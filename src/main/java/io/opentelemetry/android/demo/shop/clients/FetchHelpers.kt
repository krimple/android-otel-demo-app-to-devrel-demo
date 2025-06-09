package io.opentelemetry.android.demo.shop.clients

import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context as OtelContext
import io.opentelemetry.context.propagation.TextMapSetter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class OkHttpTextMapSetter : TextMapSetter<Request.Builder> {
    override fun set(carrier: Request.Builder?, key: String, value: String) {
        carrier?.addHeader(key, value)
    }
}

class FetchHelpers {
    companion object {
        private val TEXT_MAP_SETTER: TextMapSetter<Request.Builder> = OkHttpTextMapSetter()

        suspend fun executeRequest(request: Request): String {
            val client = OkHttpClient()
            val tracer = OtelDemoApplication.rum?.openTelemetry?.getTracer("astronomy-shop")

            val span = tracer?.spanBuilder("executeRequest")?.startSpan()
            return try {
                return span?.makeCurrent().use {
                    val result: Result<String> = suspendCoroutine { cont ->
                        val callback = object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                span?.setStatus(StatusCode.ERROR)
                                span?.recordException(e)
                                cont.resumeWithException(Exception("http error", e))
                            }

                            override fun onResponse(call: Call, response: Response) {
                                val responseBody = response.body?.string() ?: ""

                                if (response.code < 200 || response.code >= 300) {
                                    span?.setStatus(StatusCode.ERROR)
                                    val exception = Exception("error ${response.code}: $responseBody")

                                    span?.recordException(exception,
                                        Attributes.builder()
                                            .put("name", "exception")
                                            .put("foo", "bar")
                                            .build())
                                    cont.resumeWithException(exception)
                                    return
                                }

                                cont.resume(Result.success(responseBody))
                            }
                        }

                        client.newCall(request).enqueue(callback)
                    }
                    
                    result.getOrThrow()
                }
            } catch (e: Exception) {
                span?.setStatus(StatusCode.ERROR)
                span?.recordException(e)
                throw e
            } finally {
                span?.end()
            }
        }
    }
}