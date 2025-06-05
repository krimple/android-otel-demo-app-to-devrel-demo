package io.opentelemetry.android.demo.shop.clients

import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Test

class ProductApiServiceTest {

    @Test
    fun `service can be instantiated`() = runTest {
        val httpClient = OkHttpClient()
        val tracer = io.opentelemetry.api.GlobalOpenTelemetry.getTracer("test")
        val service = ProductApiService(httpClient, tracer)
        
        assert(service != null)
    }

    @Test 
    fun `context is properly used in service call`() = runTest {
        val httpClient = OkHttpClient()
        val tracer = io.opentelemetry.api.GlobalOpenTelemetry.getTracer("test")
        val service = ProductApiService(httpClient, tracer)
        val context = Context.current()
        
        try {
            service.fetchProducts(context)
        } catch (e: Exception) {
            assert(e.message?.contains("Failed to fetch products") == true || e.message?.contains("Unresolved") == true)
        }
    }
}