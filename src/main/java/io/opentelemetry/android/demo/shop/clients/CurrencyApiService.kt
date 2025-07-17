package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.session.SessionManager
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import okhttp3.Request
import android.util.Log
import io.opentelemetry.api.trace.Span
import io.opentelemetry.extension.kotlin.asContextElement

class CurrencyApiService {
    private val sessionManager = SessionManager.getInstance()
    suspend fun fetchCurrencies(): List<String> {
        Log.d("otel.demo", "Fetching available currencies")

        val currencyUrl = "${OtelDemoApplication.apiEndpoint}/currency"
        val request = Request.Builder()
            .url(currencyUrl)
            .get().build()

        try {

            // make the actual call
            val baggageHeaders = mapOf("Baggage" to "session.id=${sessionManager.currentSessionId}")
            val bodyText = FetchHelpers.executeRequestWithBaggage(request, baggageHeaders)

            val listType = object : TypeToken<List<String>>() {}.type
            val currencies = Gson().fromJson<List<String>>(bodyText, listType)

            // post-call enrichment of OK HTTP span
            val currentSpan = Span.current()
            if (currentSpan.isRecording) {
                currentSpan.setAttribute("app.operation.type", "fetch_currencies")
                currentSpan.setAttribute("app.currencies.count", currencies.size.toLong())
                currentSpan.updateName("CurrencyAPIService.fetchCurrencies") // Change from "executeRequest" to business name
            }
            return currencies
        } catch (e: Exception) {
            val currentSpan = Span.current()
            if (currentSpan.isRecording) {
                currentSpan.setAttribute("app.operation.type", "fetch_currencies")
                currentSpan.updateName("CurrencyAPIService.fetchCurrencies")
                // Note: FetchHelpers already set ERROR status and recorded the HTTP exception
            }
            throw e
        }
    }
}