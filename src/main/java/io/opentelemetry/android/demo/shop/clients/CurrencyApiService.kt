package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.session.SessionManager
import okhttp3.Request
import android.util.Log
import io.opentelemetry.api.trace.Span

class CurrencyApiService {
    suspend fun fetchCurrencies(): List<String> {
        Log.d("otel.demo", "Fetching available currencies")

        val currencyUrl = "${OtelDemoApplication.apiEndpoint}/currency"
        val request = Request.Builder()
            .url(currencyUrl)
            .get().build()

        try {

            // make the actual call
            val bodyText = FetchHelpers.executeRequest(request)

            val listType = object : TypeToken<List<String>>() {}.type
            val currencies = Gson().fromJson<List<String>>(bodyText, listType)

            // post-call enrichment of span
            val currentSpan = Span.current()
            if (currentSpan.isRecording) {
                currentSpan.setAttribute("app.currencies.count", currencies.size.toLong())
            }
            return currencies
        } catch (e: Exception) {
            // TODO FIX THIS SLOP!
            val currentSpan = Span.current()
            if (currentSpan.isRecording) {
                currentSpan.setAttribute("app.operation.type", "fetch_currencies")
                // Note: FetchHelpers already set ERROR status and recorded the HTTP exception
            }
            throw e
        }
    }
}