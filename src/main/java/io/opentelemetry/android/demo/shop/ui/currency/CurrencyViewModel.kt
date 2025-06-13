package io.opentelemetry.android.demo.shop.ui.currency

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.opentelemetry.android.demo.shop.clients.CurrencyApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class CurrencyViewModel(private val context: Context? = null) : ViewModel() {
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
        loadCurrencies()
    }

    private fun getSavedCurrency(): String {
        return context?.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
            ?.getString("selected_currency", "USD") ?: "USD"
    }

    private fun saveCurrency(currency: String) {
        context?.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("selected_currency", currency)
            ?.apply()
    }

    private fun loadCurrencies() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val currencies = currencyApiService.fetchCurrencies()
                _availableCurrencies.value = currencies
                Log.d("otel.demo", "Loaded ${currencies.size} currencies")
            } catch (e: Exception) {
                _error.value = "Failed to load currencies: ${e.message}"
                Log.e("otel.demo", "Failed to load currencies", e)
            } finally {
                _isLoading.value = false
            }
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