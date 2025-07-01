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

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currencies = currencyApiService.fetchCurrencies()
                _availableCurrencies.value = currencies
                _isLoading.value = false

                // Add attributes to the current span (created by fetchCurrencies)
                val currentSpan = Span.current()
                if (currentSpan.isRecording) {
                    currentSpan.setAttribute("app.view.model", "CurrencyViewModel")
                    currentSpan.setAttribute("app.operation.type", "load_available_currencies")
                    currentSpan.setAttribute("app.currencies.count", currencies.size.toLong())
                }

                Log.d("otel.demo", "Currencies loaded successfully: ${currencies.size} currencies")
            } catch (e: Exception) {
                Log.e("otel.demo", "Failed to load currencies: ${e.message}", e)

                // Add error context to current span
                val currentSpan = Span.current()
                if (currentSpan.isRecording) {
                    currentSpan.setAttribute("app.view.model", "CurrencyViewModel")
                    currentSpan.setAttribute("app.operation.type", "load_available_currencies")
                }

                _isLoading.value = false
                _error.value = e.message ?: "Failed to load currencies"
            }
        }
    }

    fun selectCurrency(currency: String) {
        if (_availableCurrencies.value.contains(currency)) {
            _selectedCurrency.value = currency
            saveCurrency(currency)

            // Add attributes to current span if available
            val currentSpan = Span.current()
            if (currentSpan.isRecording) {
                currentSpan.setAttribute("app.view.model", "CurrencyViewModel")
                currentSpan.setAttribute("app.operation.type", "select_currency")
                currentSpan.setAttribute("app.user.currency", currency)
            }

            Log.d("otel.demo", "Selected currency: $currency")
        } else {
            // Add rejection context to current span if available
            val currentSpan = Span.current()
            if (currentSpan.isRecording) {
                currentSpan.setAttribute("app.view.model", "CurrencyViewModel")
                currentSpan.setAttribute("app.operation.type", "select_currency")
                currentSpan.setAttribute("app.user.currency", currency)
            }

            Log.w("otel.demo", "Currency $currency not available in loaded currencies")
        }
    }

    fun retryLoadCurrencies() {
        loadCurrencies()
    }
}