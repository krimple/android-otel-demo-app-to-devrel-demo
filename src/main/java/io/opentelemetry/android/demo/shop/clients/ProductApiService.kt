package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.honeycomb.opentelemetry.android.Honeycomb
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import okhttp3.Request
import android.util.Log

class ProductApiService(
) {
    suspend fun fetchProducts(): List<Product> {
        val tracer = GlobalOpenTelemetry.getTracer("astronomy-shop")
        Log.d("otel.demo", "Tracer obtained: $tracer")

        val span = tracer.spanBuilder("fetchProducts")
            //.setParent(parentContext)
            .startSpan()
        Log.d("otel.demo", "Span created: $span, SpanContext: ${span.spanContext}")
        span.makeCurrent().use {
            try {
                val productsUrl = "${OtelDemoApplication.apiEndpoint}/prodcts"
                Log.d("otel.demo", "Making request to: $productsUrl")
                val request = Request.Builder()
                    .url(productsUrl)
                    .get().build()

                val bodyText = FetchHelpers.executeRequest(request)
                val listType = object : TypeToken<List<Product>>() {}.type
                Log.d("otel.demo", "Request completed successfully")
                return Gson().fromJson(bodyText, listType)
            } catch (e: Exception) {
                Log.d("otel.demo", "Request failed with exception: ${e.message}")
                OtelDemoApplication.rum?.let {
                    // do I need both??
                    span.setStatus(StatusCode.ERROR)
                    span.recordException(e)
                    Honeycomb.logException(it, e)
                }
                throw e
            } finally {
                Log.d("otel.demo", "Ending span: $span")
                span?.end()
            }
        }
    }
}