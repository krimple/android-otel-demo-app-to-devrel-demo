package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.api.trace.StatusCode
import okhttp3.Request
import android.util.Log

class ProductApiService(
) {
    suspend fun fetchProducts(): List<Product> {
        val tracer = OtelDemoApplication.rum?.openTelemetry?.getTracer("astronomy-shop")
        Log.d("otel.demo", "Tracer obtained: $tracer")

        val span = tracer?.spanBuilder("fetchProducts")?.startSpan()
        Log.d("otel.demo", "Span created: $span");
        return try {
            span?.makeCurrent().use {
                val productsUrl = "${OtelDemoApplication.apiEndpoint}/products"
                Log.d("otel.demo", "Making request to: $productsUrl")
                val request = Request.Builder()
                    .url(productsUrl)
                    .get().build()

                val bodyText = FetchHelpers.executeRequest(request)
                val listType = object : TypeToken<List<Product>>() {}.type
                Log.d("otel.demo", "Request completed successfully")
                // implict return is last statement in method in Kotlin so it is for the try
                Gson().fromJson<List<Product>>(bodyText, listType)
            }
        } catch (e: Exception) {
            Log.d("otel.demo", "Request failed with exception: ${e.message}")
            // do I need both??
            span?.setStatus(StatusCode.ERROR)
            span?.recordException(e)
            // TODO - do we need this call?
            // Honeycomb.logException(, e)
            throw e
        } finally {
            Log.d("otel.demo", "Ending span: $span")
            span?.end()
        }
    }
}