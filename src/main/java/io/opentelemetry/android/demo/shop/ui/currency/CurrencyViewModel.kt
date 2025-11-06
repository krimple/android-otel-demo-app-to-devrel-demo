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
import kotlinx.coroutines.Dispatchers

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
        try {
            loadCurrencies()
        } catch (e: Exception) {
            Log.d("otel.demo", "Failed to load currencies during initialization", e)
            _error.value = "Failed to initialize: ${e.message}"
        }
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
            // TODO - what, claude??
            // Ignore save errors in tests
            Log.w("CurrencyViewModel", "Could not save currency preference: ${e.message}")
        }
    }

    private fun loadCurrencies() {
        if (_availableCurrencies.value.isNotEmpty() || _isLoading.value) {
            Log.d("otel.demo", "Currencies already loaded or loading, skipping duplicate call")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null

            try {
                // TODO - could this be a problem with our traces?
                // No outer span - just let the API call create its own single-span trace
                val currencies = currencyApiService.fetchCurrencies()
                // HACK - add currency that doesn't exist
                val currenciesWithTau = currencies + "TAU"
                _availableCurrencies.value = currenciesWithTau
                _isLoading.value = false

            } catch (e: Exception) {
                Log.e("otel.demo", "Failed to load currencies: ${e.message}", e)
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
                currentSpan.setAttribute("app.user.currency", currency)
            }

            Log.d("otel.demo", "Selected currency: $currency")
        } else {
            // Add rejection context to current span if available
            val currentSpan = Span.current()
            if (currentSpan.isRecording) {
                currentSpan.setAttribute("app.user.currency", currency)
                currentSpan.setStatus(StatusCode.ERROR, "no currency found")
            }
        }
    }

    fun retryLoadCurrencies() {
        loadCurrencies()
    }
}