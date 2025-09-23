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
import kotlinx.coroutines.suspendCancellableCoroutine

class FetchHelpers {
    companion object {

        suspend fun executeRequestWithBaggage(request: Request, baggageHeaders: Map<String, String>): String = suspendCancellableCoroutine<String> { cont ->

            val tracer = OtelDemoApplication.getTracer()
            val span = tracer?.spanBuilder("executeRequestWithBaggage")?.setSpanKind(SpanKind.CLIENT)?.startSpan()
            val client = OtelDemoApplication.getHttpClient()

            // Add baggage headers to the request
            val requestBuilder = request.newBuilder()
            baggageHeaders.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }
            val tracedRequest = requestBuilder.build()

            val call = client.newCall(tracedRequest)

            cont.invokeOnCancellation {
                call.cancel()
                span?.end()
            }

            client.newCall(tracedRequest).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    span?.setStatus(StatusCode.ERROR)
                    span?.recordException(e)
                    span?.end()
                    cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response): Unit {
                    if (!response.isSuccessful) {
                        span?.setStatus(StatusCode.ERROR)
                        val ex =
                            IOException("error ${response.code}: ${response.body?.string()}")
                        span?.recordException(ex)
                        span?.end()
                        cont.resumeWithException(ex)
                    } else {
                        val responseBody = response.body?.string() ?: ""
                        span?.end()
                        cont.resume(responseBody)
                    }
                }
            })
        }
    }
}
