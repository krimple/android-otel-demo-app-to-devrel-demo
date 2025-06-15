package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import okhttp3.Request
import android.util.Log
import io.opentelemetry.extension.kotlin.asContextElement

class CurrencyApiService {
    companion object {
        private val tracer = OtelDemoApplication.getTracer()
    }

    suspend fun fetchCurrencies(): List<String> {
        Log.d("otel.demo", "Fetching available currencies")

        val span = tracer?.spanBuilder("fetchCurrencies")?.startSpan()
        val currencies = span?.makeCurrent().use {
            try {
                val currencyUrl = "${OtelDemoApplication.apiEndpoint}/currency"
                Log.d("otel.demo", "Making request to: $currencyUrl")
                val request = Request.Builder()
                    .url(currencyUrl)
                    .get().build()

                // make the actual call
                val bodyText = FetchHelpers.executeRequest(request)

                val listType = object : TypeToken<List<String>>() {}.type
                val currencies = Gson().fromJson<List<String>>(bodyText, listType)

                Log.d("otel.demo", "Currency request completed successfully")
                span?.setAttribute( "currencies.count", currencies.joinToString(","))
                currencies
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
        return currencies
    }
}