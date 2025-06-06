package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.android.demo.shop.model.ProductDeserializationWrapper
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import okhttp3.Request

class ProductApiService(
    private val httpClient: okhttp3.OkHttpClient,
    private val tracer: Tracer
) {
    companion object {
        private const val PRODUCTS_API_URL = "https://www.zurelia.honeydemo.io/api/products"
    }

    suspend fun fetchProducts(parentContext: Context = Context.current()): List<Product> {
        val request = Request.Builder()
            .url(PRODUCTS_API_URL)
            .get()
            .build()

        val bodyText = FetchHelpers.executeRequest(request)
        val parsedBody = Gson().fromJson(
            bodyText,
            ProductDeserializationWrapper::class.java
        )
        return parsedBody.products
    }
}