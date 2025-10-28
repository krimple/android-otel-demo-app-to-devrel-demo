package io.opentelemetry.android.demo.shop.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.clients.ProductApiService
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.StatusCode
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
        // User-initiated screen load - create root span without inherited context
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("screen.load_products")
            ?.setAttribute("app.operation.is.refresh", isRefresh)
            ?.setAttribute("app.screen.name", "product_list")
            ?.setAttribute("app.user.currency", currencyCode)
            ?.startSpan()
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Make this span current for API calls within this coroutine
                val scope = span?.makeCurrent()
                val products = try {
                    productApiService.fetchProducts(currencyCode)
                } finally {
                    scope?.close()
                }
                _uiState.value = ProductListUiState(
                  products = products,
                  isLoading = false,
                  errorMessage = null
                )

                span?.setAttribute("app.products.count", products.size.toLong())
                
            } catch (e: Exception) {
                _uiState.value = ProductListUiState(
                    products = emptyList(),
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load products"
                )
                
                span?.setStatus(StatusCode.ERROR)
            } finally {
                span?.end()
            }
        }
    }
}
