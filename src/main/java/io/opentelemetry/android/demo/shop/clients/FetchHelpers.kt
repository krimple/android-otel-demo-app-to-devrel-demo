package io.opentelemetry.android.demo.shop.clients

import android.util.Log
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException

class FetchHelpers {
    companion object {

        private fun isBenignCancel(e: IOException): Boolean {
            if (e.message?.contains("Canceled", ignoreCase = true) == true) return true
            if (e.message?.contains("stream was reset: CANCEL", true) == true) return true
            if (e is InterruptedIOException) return true
            return e is StreamResetException && e.errorCode == ErrorCode.CANCEL
        }

        private fun logRumOrFallback(e: Throwable, req: Request) {
            val rum = OtelDemoApplication.rum
            if (rum != null) {
                OtelDemoApplication.logException(
                    rum,
                    e,
                    Attributes.of(
                        AttributeKey.stringKey("app.tracedRequest.url"),
                        req.url.toString(),
                        AttributeKey.stringKey("name"),
                        "exception",
                    ),
                    Thread.currentThread()
                )
            } else {
                Log.e("fetch", "Error", e)
            }
        }

        suspend fun executeRequestWithBaggage(
            request: Request,
            baggage: Map<String, String>
        ): String = suspendCancellableCoroutine { cont ->

            val tracer = OtelDemoApplication.getTracer()
            val span = tracer?.spanBuilder("executeRequestWithBaggage")
                ?.setSpanKind(SpanKind.INTERNAL)
                ?.startSpan()

            val client = OtelDemoApplication.getHttpClient()
            val traced = request.newBuilder().apply {
                // If these are *custom* headers, keep as-is. If you mean W3C baggage,
                // prefer OTel propagation instead of ad-hoc headers.
                baggage.forEach { (k, v) -> addHeader(k, v) }
            }.build()

            val call = client.newCall(traced)

            cont.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    try {
                        if (isBenignCancel(e)) {
                            span?.setAttribute("app.canceled", true)
                        } else {
                            span?.setStatus(StatusCode.ERROR)
                            logRumOrFallback(e, traced)
                        }
                    } finally {
                        span?.end()
                        if (cont.isActive) cont.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use { r ->
                            if (!r.isSuccessful) {
                                span?.setStatus(StatusCode.ERROR)
                                val msg = r.body?.string().orEmpty()
                                val ex = IOException("error ${r.code}: $msg")
                                logRumOrFallback(ex, traced)
                                if (cont.isActive) cont.resumeWithException(ex)
                            } else {
                                val body = r.body?.string().orEmpty()
                                if (cont.isActive) cont.resume(body)
                            }
                        }
                    } finally {
                        span?.end()
                    }
                }
            })
        }
    }
}