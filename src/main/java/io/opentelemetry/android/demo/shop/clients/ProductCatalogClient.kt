package io.opentelemetry.android.demo.shop.clients

import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.context.Context as OtelContext
import kotlinx.coroutines.runBlocking


class ProductCatalogClient {
   private val productApiService = ProductApiService()

    suspend fun getProducts(currencyCode: String = "USD", parentContext: OtelContext = OtelContext.current()): List<Product> {
        return productApiService.fetchProducts(currencyCode)
    }

    suspend fun getProduct(productId: String, currencyCode: String = "USD", parentContext: OtelContext = OtelContext.current()): Product {
        return productApiService.fetchProduct(productId, currencyCode)
    }
}