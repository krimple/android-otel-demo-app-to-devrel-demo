package io.opentelemetry.android.demo.shop.clients

import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context as OtelContext
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

class ProductCatalogClient {
    private val httpClient = OkHttpClient()
    private val tracer = GlobalOpenTelemetry.getTracer("product-catalog-client")
    private val apiService = ProductApiService(httpClient, tracer)

    suspend fun getProducts(parentContext: OtelContext = OtelContext.current()): List<Product> {
        return apiService.fetchProducts(parentContext)
    }

    fun get(): List<Product> {
        return runBlocking {
            getProducts()
        }
    }

}