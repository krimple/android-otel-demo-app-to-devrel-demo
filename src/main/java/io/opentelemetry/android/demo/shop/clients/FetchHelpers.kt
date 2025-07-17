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
            val client = OkHttpClient()

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

            // span?.setAttribute("http.method", tracedRequest.method)
            // span?.setAttribute("http.url", tracedRequest.url.toString())
            // span?.setAttribute("baggage.headers.count", baggageHeaders.size.toLong())

            client.newCall(tracedRequest).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.d(
                        "FetchHelpers",
                        "SPAN ERROR: executeRequestWithBaggage span=$span, HTTP error ${e.message}"
                    )
                    span?.setStatus(StatusCode.ERROR)
                    span?.recordException(e)
                    span?.end()
                    cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response): Unit {
                    if (!response.isSuccessful) {
                        Log.d(
                            "FetchHelpers",
                            "SPAN ERROR: executeRequestWithBaggage span=$span, HTTP error ${response.message}"
                        )
                        span?.setStatus(StatusCode.ERROR)
                        val ex =
                            IOException("error ${response.code}: ${response.body?.string()}")
                        span?.recordException(ex)
                        span?.end()
                        cont.resumeWithException(ex)
                    } else {
                        span?.end()
                        cont.resume(response.body?.string() ?: "")
                    }
                }
            })
        }
    }
}
