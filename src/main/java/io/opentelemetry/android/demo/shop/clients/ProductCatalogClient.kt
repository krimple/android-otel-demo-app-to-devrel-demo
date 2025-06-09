package io.opentelemetry.android.demo.shop.clients

import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.context.Context as OtelContext
import kotlinx.coroutines.runBlocking

import okhttp3.OkHttpClient

class ProductCatalogClient {
   private val productApiService = ProductApiService()

    suspend fun getProducts(): List<Product> {
        return productApiService.fetchProducts()
    }
}