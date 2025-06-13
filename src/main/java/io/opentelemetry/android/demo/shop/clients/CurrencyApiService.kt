package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import okhttp3.Request
import android.util.Log

class CurrencyApiService {
    suspend fun fetchCurrencies(parentContext: Context = Context.current()): List<String> {
        val tracer = OtelDemoApplication.tracer("astronomy-shop")
        Log.d("otel.demo", "Fetching available currencies")

        val span = tracer?.spanBuilder("fetchCurrencies")
            ?.setParent(parentContext)
            ?.startSpan()

        return try {
            span?.makeCurrent().use {
                val currencyUrl = "${OtelDemoApplication.apiEndpoint}/currency"
                Log.d("otel.demo", "Making request to: $currencyUrl")
                val request = Request.Builder()
                    .url(currencyUrl)
                    .get().build()

                val bodyText = FetchHelpers.executeRequest(request)
                val listType = object : TypeToken<List<String>>() {}.type
                Log.d("otel.demo", "Currency request completed successfully")
                Gson().fromJson<List<String>>(bodyText, listType)
            }
        } catch (e: Exception) {
            Log.d("otel.demo", "Currency request failed with exception: ${e.message}")
            span?.setStatus(StatusCode.ERROR)
            span?.recordException(e)
            throw e
        } finally {
            Log.d("otel.demo", "Ending currency span: $span")
            span?.end()
        }
    }
}