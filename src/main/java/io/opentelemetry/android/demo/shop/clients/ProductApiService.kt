package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import okhttp3.Request
import android.util.Log

class ProductApiService(
) {
    suspend fun fetchProducts(currencyCode: String = "USD"): List<Product> {
        val tracer = OtelDemoApplication.getTracer()
        Log.d("otel.demo", "Tracer obtained: $tracer")

        val span = tracer?.spanBuilder("ProductAPIService.fetchProducts")
            ?.setAttribute("app.user.currency", currencyCode)
            ?.setAttribute("app.operation.type", "fetch_products")
            ?.startSpan()
        Log.d("otel.demo", "Span created: $span")
        return try {
            span?.makeCurrent().use {
                val productsUrl = "${OtelDemoApplication.apiEndpoint}/products?currencyCode=$currencyCode"
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
            throw e
        } finally {
            Log.d("otel.demo", "Ending span: $span")
            span?.end()
        }
    }

    suspend fun fetchProduct(productId: String, currencyCode: String = "USD"): Product {
        val tracer = OtelDemoApplication.getTracer()
        Log.d("otel.demo", "Fetching individual product: $productId")

        val span = tracer?.spanBuilder("ProductAPIService.fetchProduct")
            ?.setAttribute("app.product.id", productId)
            ?.setAttribute("app.user.currency", currencyCode)
            ?.setAttribute("app.operation.type", "fetch_product")
            ?.startSpan()

        return try {
            span?.makeCurrent().use {
                val productUrl = "${OtelDemoApplication.apiEndpoint}/products/$productId?currencyCode=$currencyCode"
                Log.d("otel.demo", "Making request to: $productUrl")
                val request = Request.Builder()
                    .url(productUrl)
                    .get().build()

                val bodyText = FetchHelpers.executeRequest(request)
                Log.d("otel.demo", "Individual product request completed successfully")
                Gson().fromJson(bodyText, Product::class.java)
            }
        } catch (e: Exception) {
            Log.d("otel.demo", "Individual product request failed: ${e.message}")
            span?.setStatus(StatusCode.ERROR)
            span?.recordException(e)
            throw e
        } finally {
            Log.d("otel.demo", "Ending individual product span: $span")
            span?.end()
        }
    }
}