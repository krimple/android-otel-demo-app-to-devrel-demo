package io.opentelemetry.android.demo.shop.clients

import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.util.Log
import io.honeycomb.opentelemetry.android.Honeycomb
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import kotlinx.coroutines.suspendCancellableCoroutine

class FetchHelpers {
    companion object {

        suspend fun executeRequestWithBaggage(
            request: Request,
            baggage: Map<String, String>
        ): String = suspendCancellableCoroutine<String> { cont ->

            val tracer = OtelDemoApplication.getTracer()
            val span =
                tracer?.spanBuilder("executeRequestWithBaggage")
                    ?.setSpanKind(SpanKind.CLIENT)
                    ?.startSpan()

            val client = OtelDemoApplication.getHttpClient()
            val traced = request.newBuilder().apply {
                baggage.forEach { (k, v) -> addHeader(k, v) }
            }.build()

            val call = client.newCall(traced)

            cont.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                /*
                // instead
                // using Honeycomb's helper

*/
                override fun onFailure(call: Call, e: IOException) {

                    span?.setStatus(StatusCode.ERROR)
                    if (OtelDemoApplication.rum !== null) {
                        Honeycomb.logException(
                            OtelDemoApplication.rum!!,
                            e,
                            Attributes.of(
                                AttributeKey.stringKey("app.tracedRequest.url"), traced.url.toString()
                            ),
                            Thread.currentThread()
                        )
                    } else {
                        // TODO - this is the dumb fallback
                        Log.e("sometag", "Error is", e);
                    }
                    // Record the exception the OTEL Way:
                    // span?.recordException(e)

                    span?.end()
                    if (!cont.isCompleted) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response): Unit {
                    try {
                        response.use {
                            if (!it.isSuccessful) {
                                span?.setStatus(StatusCode.ERROR)
                                val msg = it.body?.string().orEmpty()
                                // create an exception to record since this is a
                                // error state in the network stack, not a thrown
                                // exception
                                val ex = IOException("error ${it.code}: $msg")
                                if (OtelDemoApplication.rum !== null) {
                                    Honeycomb.logException(
                                        OtelDemoApplication.rum!!,
                                        ex,
                                        Attributes.of(
                                            AttributeKey.stringKey("app.tracedRequest.url"), traced.url.toString()
                                        ),
                                        Thread.currentThread()
                                    )
                                } else {
                                    // TODO - this is the dumb fallback
                                    Log.e("sometag", "Error is", ex);
                                }
                                if (!cont.isCompleted) cont.resumeWithException(ex);
                            } else {
                                val body = it.body?.string().orEmpty()
                                if (!cont.isCompleted) cont.resume(body)
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

