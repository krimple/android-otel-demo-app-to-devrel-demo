package io.opentelemetry.android.demo.shop.ui.currency

import android.content.Context
import androidx.lifecycle.ViewModel
import io.opentelemetry.android.demo.shop.clients.CurrencyApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import androidx.lifecycle.viewModelScope
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.extension.kotlin.asContextElement

class CurrencyViewModel() : ViewModel() {
    private val currencyApiService = CurrencyApiService()

    private val _availableCurrencies = MutableStateFlow<List<String>>(emptyList())
    val availableCurrencies: StateFlow<List<String>> = _availableCurrencies.asStateFlow()

    private val _selectedCurrency = MutableStateFlow(getSavedCurrency())
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Load currencies when ViewModel is created
        loadCurrencies()
    }

    private fun getSavedCurrency(): String {
        return try {
            val prefs = OtelDemoApplication.getInstance()
            prefs.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
                ?.getString("selected_currency", "USD") ?: "USD"
        } catch (e: Exception) {
            // Fallback for tests or when OtelDemoApplication is not initialized
            "USD"
        }
    }

    private fun saveCurrency(currency: String) {
        try {
            val prefs = OtelDemoApplication.getInstance()
            val currencyPrefs = prefs.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
            currencyPrefs.edit().putString("selected_currency", currency).apply()
        } catch (e: Exception) {
            // Ignore save errors in tests
            Log.w("CurrencyViewModel", "Could not save currency preference: ${e.message}")
        }
    }

    private fun loadCurrencies() {
        if (_availableCurrencies.value.isNotEmpty() || _isLoading.value) {
            Log.d("otel.demo", "Currencies already loaded or loading, skipping duplicate call")
            return
        }

        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("loadCurrencies")
            ?.setAttribute(stringKey("component"), "currency_viewmodel")
            ?.setAttribute(stringKey("user_action"), "load_available_currencies")
            ?.setAttribute(stringKey("operation.type"), "currency_fetch")
            ?.startSpan()

        Log.d("otel.demo", "SPAN CREATED: loadCurrencies span=$span, tracer=$tracer")

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                span?.makeCurrent().use {
                    val currencies = currencyApiService.fetchCurrencies()
                    _availableCurrencies.value = currencies
                    _isLoading.value = false

                    // Add success attributes to span
                    span?.setAttribute(longKey("currencies.count"), currencies.size.toLong())
                    span?.setAttribute(stringKey("currencies.loaded"), currencies.joinToString(","))
                    span?.setAttribute(stringKey("operation.result"), "success")

                    Log.d("otel.demo", "Currencies loaded successfully: ${currencies.size} currencies")
                }
            } catch (e: Exception) {
                Log.e("otel.demo", "Failed to load currencies: ${e.message}", e)

                span?.setStatus(StatusCode.ERROR)
                span?.recordException(e)
                span?.setAttribute(stringKey("operation.result"), "error")
                span?.setAttribute(stringKey("error.type"), e.javaClass.simpleName)

                _isLoading.value = false
                _error.value = e.message ?: "Failed to load currencies"
            } finally {
                Log.d("otel.demo", "SPAN ENDED: loadCurrencies span=$span")
                span?.end()
            }
        }
    }

    fun selectCurrency(currency: String) {
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("selectCurrency")
            ?.setAttribute(stringKey("component"), "currency_viewmodel")
            ?.setAttribute(stringKey("user_action"), "select_currency")
            ?.setAttribute(stringKey("currency.requested"), currency)
            ?.setAttribute(stringKey("currency.previous"), _selectedCurrency.value)
            ?.startSpan()

        Log.d("otel.demo", "SPAN CREATED: selectCurrency span=$span, currency=$currency")

        try {
            span?.makeCurrent().use {
                if (_availableCurrencies.value.contains(currency)) {
                    _selectedCurrency.value = currency
                    saveCurrency(currency)

                    span?.setAttribute(stringKey("currency.selected"), currency)
                    span?.setAttribute(stringKey("operation.result"), "success")
                    span?.setAttribute(stringKey("currency.change.applied"), "true")

                    Log.d("otel.demo", "Selected currency: $currency")
                } else {
                    span?.setAttribute(stringKey("operation.result"), "rejected")
                    span?.setAttribute(stringKey("currency.change.applied"), "false")
                    span?.setAttribute(stringKey("rejection.reason"), "currency_not_available")

                    Log.w("otel.demo", "Currency $currency not available in loaded currencies")
                }
            }
        } catch (e: Exception) {
            Log.e("otel.demo", "Failed to select currency: ${e.message}", e)

            span?.setStatus(StatusCode.ERROR)
            span?.recordException(e)
            span?.setAttribute(stringKey("operation.result"), "error")
            span?.setAttribute(stringKey("error.type"), e.javaClass.simpleName)
        } finally {
            Log.d("otel.demo", "SPAN ENDED: selectCurrency span=$span")
            span?.end()
        }
    }

    fun retryLoadCurrencies() {
        loadCurrencies()
    }
}