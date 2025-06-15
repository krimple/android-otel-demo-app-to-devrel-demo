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
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine

class FetchHelpers {
    companion object {
        private val tracer = OtelDemoApplication.getTracer()

        private val client = OkHttpClient()

        suspend fun executeRequest(request: Request): String = suspendCancellableCoroutine<String> { cont ->

            val span = tracer?.spanBuilder("executeRequest")?.setSpanKind(SpanKind.CLIENT)?.startSpan()
            val tracedRequest = request.newBuilder().build()

            val call = client.newCall(tracedRequest)

            cont.invokeOnCancellation {
                call.cancel()
            }

            span?.makeCurrent().use {
                span?.setAttribute("http.method", tracedRequest.method)
                span?.setAttribute("http.url", tracedRequest.url.toString())

                client.newCall(tracedRequest).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                            Log.d(
                                "FetchHelpers",
                                "SPAN ERROR: executeRequest span=$span, HTTP error ${e.message}"
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
                                    "SPAN ERROR: executeRequest span=$span, HTTP error ${response.message}"
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
}