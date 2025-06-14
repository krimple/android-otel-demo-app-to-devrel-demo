package io.opentelemetry.android.demo.shop.clients

import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.common.Attributes
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
import kotlin.coroutines.suspendCoroutine

class FetchHelpers {
    companion object {
        suspend fun executeRequest(request: Request): String {
            val tracer = OtelDemoApplication.rum
                ?.openTelemetry?.getTracer("astronomy-shop") ?: return ""

            val span = tracer.spanBuilder("executeRequest")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan()

            val scope = span.makeCurrent()

            return suspendCoroutine { cont ->
                val client = OkHttpClient()
                val tracedRequest = request.newBuilder().build()

                span.setAttribute("http.method", tracedRequest.method)
                span.setAttribute("http.url", tracedRequest.url.toString())

                client.newCall(tracedRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        span.setStatus(StatusCode.ERROR)
                        span.recordException(e)
                        cont.resumeWithException(e)
                        scope.close()
                        span.end()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string() ?: ""
                        span.setAttribute("http.status_code", response.code.toString())

                        if (!response.isSuccessful) {
                            val ex = IOException("error ${response.code}: $body")
                            span.setStatus(StatusCode.ERROR)
                            span.recordException(ex)
                            cont.resumeWithException(ex)
                        } else {
                            cont.resume(body)
                        }

                        scope.close()
                        span.end()
                    }
                })
            }
        }
    }
}