package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.android.demo.shop.session.SessionManager
import io.opentelemetry.api.trace.StatusCode
import okhttp3.Request

class ProductApiService(
) {
    suspend fun fetchProducts(currencyCode: String = "USD"): List<Product> {
        val tracer = OtelDemoApplication.getTracer()

        val span = tracer?.spanBuilder("product_api_service.fetch_products")
            ?.setAttribute("app.user.currency", currencyCode)
            ?.startSpan()
        return try {
            span?.makeCurrent().use {
                val productsUrl = "${OtelDemoApplication.apiEndpoint}/products?currencyCode=$currencyCode"
                val request = Request.Builder()
                    .url(productsUrl)
                    .get().build()

                val bodyText = FetchHelpers.executeRequest(request)
                val listType = object : TypeToken<List<Product>>() {}.type

                // implict return is last statement in method in Kotlin so it is for the try
                Gson().fromJson<List<Product>>(bodyText, listType)
            }
        } catch (e: Exception) {
            span?.setStatus(StatusCode.ERROR, "Failed to fetch products");

            OtelDemoApplication.logException(
                OtelDemoApplication.rum!!,
                e,
                null,
                Thread.currentThread()
            )
            throw e
        } finally {
            span?.end()
        }
    }

    suspend fun fetchProduct(productId: String, currencyCode: String = "USD"): Product {
        val tracer = OtelDemoApplication.getTracer()

        val span = tracer?.spanBuilder("product_api_service.fetch_product")
            ?.setAttribute("app.product.id", productId)
            ?.setAttribute("app.user.currency", currencyCode)
            ?.startSpan()

        return try {
            span?.makeCurrent().use {
                val productUrl = "${OtelDemoApplication.apiEndpoint}/products/$productId?currencyCode=$currencyCode"
                val request = Request.Builder()
                    .url(productUrl)
                    .get().build()

                val bodyText = FetchHelpers.executeRequest(request)
                Gson().fromJson(bodyText, Product::class.java)
            }
        } catch (e: Exception) {
            // mark the span in error
            span?.setStatus(StatusCode.ERROR)
            throw e;
        } finally {
            span?.end()
        }
    }
}