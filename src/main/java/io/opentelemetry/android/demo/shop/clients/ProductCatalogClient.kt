package io.opentelemetry.android.demo.shop.clients

import android.content.Context
import com.google.gson.Gson
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.android.demo.shop.model.ProductDeserializationWrapper
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context as OtelContext
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

const val PRODUCTS_FILE = "products.json"

class ProductCatalogClient(private val context: Context) {
    private val httpClient = OkHttpClient()
    private val tracer = GlobalOpenTelemetry.getTracer("product-catalog-client")
    private val apiService = ProductApiService(httpClient, tracer)

    suspend fun getProducts(parentContext: OtelContext = OtelContext.current()): List<Product> {
        return try {
            apiService.fetchProducts(parentContext)
        } catch (e: Exception) {
            android.util.Log.w("ProductCatalogClient", "Failed to fetch from API, using fallback", e)
            getFallbackProducts()
        }
    }

    fun get(): List<Product> {
        return runBlocking {
            getProducts()
        }
    }

    private fun getFallbackProducts(): List<Product> {
        val input = context.assets.open(PRODUCTS_FILE)
        val jsonStr = input.bufferedReader()
        val wrapper = Gson().fromJson(jsonStr, ProductDeserializationWrapper::class.java)
        return wrapper.products
    }
}