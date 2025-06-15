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

    companion object {
        @Volatile
        private var INSTANCE: CurrencyViewModel? = null

        fun getInstance(context: Context): CurrencyViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CurrencyViewModel().also {
                    INSTANCE = it
                    it.loadCurrencies()
                }
            }
        }
    }

    init {
        // Don't auto-load currencies here anymore - controlled by singleton
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

            val currencies = currencyApiService.fetchCurrencies()
            _availableCurrencies.value = currencies
        }
    }

    fun selectCurrency(currency: String) {
        if (_availableCurrencies.value.contains(currency)) {
            _selectedCurrency.value = currency
            saveCurrency(currency)
            Log.d("otel.demo", "Selected currency: $currency")
        }
    }

    fun retryLoadCurrencies() {
        loadCurrencies()
    }
}