package io.opentelemetry.android.demo.shop.clients

import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.context.Context as OtelContext
import io.opentelemetry.context.propagation.TextMapSetter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OkHttpTextMapSetter : TextMapSetter<Request.Builder> {
    override fun set(carrier: Request.Builder?, key: String, value: String) {
        carrier?.addHeader(key, value)
    }
}

class FetchHelpers {
    companion object {
        private val TEXT_MAP_SETTER: TextMapSetter<Request.Builder> = OkHttpTextMapSetter()

        suspend fun executeRequest(client: OkHttpClient, request: Request): String {
            
            val result: Result<String> = suspendCoroutine { cont ->
                val callback = object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        cont.resume(Result.failure(Exception("http error", e)))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string() ?: ""

                        if (response.code < 200 || response.code >= 300) {
                            val exception = Exception("error ${response.code}: $responseBody")
                            cont.resume(Result.failure(exception))
                            return
                        }

                        cont.resume(Result.success(responseBody))
                    }
                }

                val builder = request.newBuilder()
                OtelDemoApplication.rum?.openTelemetry?.propagators?.textMapPropagator?.inject(
                    OtelContext.current(),
                    builder,
                    TEXT_MAP_SETTER
                )

                val requestWithHeaders = builder.build()
                client.newCall(requestWithHeaders).enqueue(callback)
            }
            
            return result.getOrThrow()
        }
    }
}