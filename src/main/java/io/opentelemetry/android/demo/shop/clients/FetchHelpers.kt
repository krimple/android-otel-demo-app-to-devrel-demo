package io.opentelemetry.android.demo.shop.clients

import android.icu.util.TimeUnit
import android.util.Log
import coil3.network.HttpException
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter
import java.io.IOException
import java.io.InterruptedIOException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
import kotlin.coroutines.resumeWithException

class BaggageInterceptor(
    private val otelRum: OpenTelemetryRum,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val sessionId = otelRum.rumSessionId
        // TODO: This should listen to SessionObserver, but the SessionManager isn't exposed by the SDK?
        val baggage: Baggage = Baggage.builder()
            .put("session.id", sessionId)
            .build()

        val baggageStr = mutableListOf<String>()
        baggage.forEach { key, entry ->
            baggageStr.add("${key}=${entry.value}")
        }

        val newRequest = chain.request().newBuilder()
            .addHeader("baggage", baggageStr.joinToString())
            .build()

        return chain.proceed(newRequest)
    }
}

class OkHttpTextMapSetter : TextMapSetter<Request.Builder> {
    override fun set(carrier: Request.Builder?, key: String, value: String) {
        carrier?.addHeader(key, value)
    }
}

class FetchHelpers {

    companion object {
        private val TEXT_MAP_SETTER: TextMapSetter<Request.Builder> = OkHttpTextMapSetter();

        private fun isBenignCancel(e: IOException): Boolean {
            if (e.message?.contains("Canceled", ignoreCase = true) == true) return true
            if (e.message?.contains("stream was reset: CANCEL", true) == true) return true
            if (e is InterruptedIOException) return true
            return e is StreamResetException && e.errorCode == ErrorCode.CANCEL
        }

        private fun logRumOrFallback(e: Throwable, req: Request) {
            if (e is IOException && isBenignCancel(e)) {
                // don't bother marking with an error in the root span
                // this is a spurious error and we don't want to report it
                return
            }
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

        suspend fun executeRequest(request: Request): String {
            val okClientBuilder = OkHttpClient.Builder()
            OtelDemoApplication.rum?.let { okClientBuilder.addInterceptor(BaggageInterceptor(it)) }
            // add ridiculously long timeouts - because, demo
            val client = okClientBuilder
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            return suspendCancellableCoroutine { cont ->
                val callback = object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        logRumOrFallback(e, request)
                        cont.resumeWithException(e)
                        return
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use { r ->
                            if (!response.isSuccessful) {
                                val msg = r.body?.string().orEmpty()
                                val ex = IOException("error ${r.code}: $msg")
                                logRumOrFallback(ex, call.request())
                                if (cont.isActive) cont.resumeWithException(ex)
                            } else {
                                val body = r.body?.string().orEmpty()
                                if (cont.isActive) cont.resume(body) {}
                            }
                        }
                    }
                }

                val requestWithHeadersBuilder = request.newBuilder()
                OtelDemoApplication.rum?.openTelemetry?.propagators?.textMapPropagator?.inject(
                    Context.current(),
                    requestWithHeadersBuilder,
                    TEXT_MAP_SETTER
                )
                val requestWithHeaders = requestWithHeadersBuilder.build()
                client.newCall(requestWithHeaders).enqueue(callback)
            }
        }
    }
}