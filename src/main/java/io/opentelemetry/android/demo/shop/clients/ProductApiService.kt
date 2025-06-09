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

class ProductApiService(
    private val httpClient: okhttp3.OkHttpClient,
    private val tracer: Tracer
) {
    suspend fun fetchProducts(parentContext: Context = Context.current()): List<Product> {
        val tracer = GlobalOpenTelemetry.getTracer("astronomy-shop")

        val span = tracer.spanBuilder("fetchProducts")
            .setParent(parentContext)
            .startSpan()
        span.makeCurrent()
        try {
            val productsUrl = "${OtelDemoApplication.apiEndpoint}/prodcts"
            val request = Request.Builder()
                .url(productsUrl)
                .get()
                .build()

            val bodyText = FetchHelpers.executeRequest(request)
            val listType = object : TypeToken<List<Product>>() {}.type
            return Gson().fromJson(bodyText, listType)
        } catch (e: Exception) {
            OtelDemoApplication.rum?.let {
                // do I need both??
                span.setStatus(StatusCode.ERROR)
                span.recordException(e)
                Honeycomb.logException(it, e)
            }
            throw e
        } finally {
            span?.end()
        }
    }
}