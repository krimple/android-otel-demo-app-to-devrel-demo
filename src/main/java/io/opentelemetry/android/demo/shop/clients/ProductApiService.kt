package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.android.demo.shop.model.ProductDeserializationWrapper
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.Context as OtelContext
import io.opentelemetry.context.propagation.TextMapSetter
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OkHttpTextMapSetter : TextMapSetter<Request.Builder> {
    override fun set(carrier: Request.Builder?, key: String, value: String) {
        carrier?.addHeader(key, value)
    }
}
class ProductApiService(
    private val httpClient: OkHttpClient,
    private val tracer: Tracer
) {
    companion object {
        private const val PRODUCTS_API_URL = "https://www.zurelia.honeydemo.io/api/products"
        private val TEXT_MAP_SETTER: TextMapSetter<Request.Builder> = OkHttpTextMapSetter();
    }

    suspend fun fetchProducts(parentContext: Context = Context.current()): List<Product> {
        val clientBuilder = OkHttpClient.Builder()

        // the actual request to execute
        val requestBuilder = Request.Builder()
            .url(PRODUCTS_API_URL)
            .get()
        val request = requestBuilder.build()
        val client = clientBuilder.build()
/*
        return withContext(Dispatchers.IO + parentContext.asContextElement()) {
            val span = tracer.spanBuilder("product_api.fetch_products")
                .setParent(parentContext)
                .setAttribute("http.url", PRODUCTS_API_URL)
                .startSpan()
 */

//            span.makeCurrent().use {

        val result: Result<String> = suspendCoroutine { cont ->
            val callback = object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resume(Result.failure(Exception("http error", e)))
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string() ?: ""

                    // did it fail?
                    if (response.code < 200 || response.code >= 300) {
                        val exception =
                            Exception("error ${response.code}: $responseBody")
                        cont.resume(Result.failure(exception))
                        return
                    }

                    // it worked, give us the body
                    cont.resume(Result.success(responseBody))
                }
            }

            // set up our call - we need a new request builder
            val builder = request.newBuilder()
            // Inject OpenTelemetry context into headers
            OtelDemoApplication.rum?.openTelemetry?.propagators?.textMapPropagator?.inject(
                OtelContext.current(),
                builder,
                TEXT_MAP_SETTER
            );

            val requestWithHeaders = builder.build()
            client.newCall(requestWithHeaders).enqueue(callback)

        }
        val bodyText = result.getOrThrow();
        val parsedBody = Gson().fromJson(
            bodyText,
            ProductDeserializationWrapper::class.java
        )
        return parsedBody.products
    }
}