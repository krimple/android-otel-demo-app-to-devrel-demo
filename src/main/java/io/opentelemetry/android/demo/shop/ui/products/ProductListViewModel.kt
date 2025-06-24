package io.opentelemetry.android.demo.shop.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.clients.ProductApiService
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProductListViewModel(
    private val productApiService: ProductApiService = ProductApiService()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()
    
    private var hasLoadedOnce = false
    
    fun refreshProducts(currencyCode: String = "USD") {
        if (hasLoadedOnce) {
            loadProducts(currencyCode, isRefresh = true)
        } else {
            loadProducts(currencyCode, isRefresh = false)
            hasLoadedOnce = true
        }
    }
    
    private fun loadProducts(currencyCode: String = "USD", isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            var span: Span? = null
            try {
                val products = productApiService.fetchProducts(currencyCode)
                _uiState.value = ProductListUiState(
                  products = products,
                  isLoading = false,
                  errorMessage = null
                )

                // tack the attributes to the nested span
                span = Span.current()

                span?.setAttribute("component", "product_list")
                span?.setAttribute("is_refresh", isRefresh)
                span?.setAttribute("currency.code", currencyCode)
                span?.setAttribute("user_action", "view_product_list")
                span?.setAttribute("products.loaded", products.size.toLong())
                span?.setAttribute("operation.result", "success")
            } catch (e: Exception) {
                _uiState.value = ProductListUiState(
                    products = emptyList(),
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load products"
                )
            }
        }
    }
}
