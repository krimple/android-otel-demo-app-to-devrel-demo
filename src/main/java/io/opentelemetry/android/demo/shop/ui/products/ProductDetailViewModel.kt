package io.opentelemetry.android.demo.shop.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.clients.ProductApiService
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.api.trace.StatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProductDetailViewModel(
    private val productApiService: ProductApiService = ProductApiService()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()
    
    fun loadProduct(productId: String, currencyCode: String = "USD") {
        // User-initiated screen load - create root span
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("screen.load_product_detail")
            ?.setAttribute("app.screen.name", "product_detail")
            ?.setAttribute("app.product.id", productId)
            ?.setAttribute("app.user.currency", currencyCode)
            ?.startSpan()
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Make this span current for API calls within this coroutine
                val scope = span?.makeCurrent()
                val product = try {
                    productApiService.fetchProduct(productId, currencyCode)
                } finally {
                    scope?.close()
                }
                
                _uiState.value = ProductDetailUiState(
                    product = product,
                    isLoading = false,
                    errorMessage = null
                )
                
                span?.setAttribute("app.product.name", product.name)
                span?.setAttribute("app.product.price.usd", product.priceValue())
                
            } catch (e: Exception) {
                span?.setStatus(StatusCode.ERROR)
                _uiState.value = ProductDetailUiState(
                    product = null,
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load product"
                )
            } finally {
                span?.end()
            }
        }
    }
    
    fun refreshProduct(productId: String, currencyCode: String = "USD") {
        loadProduct(productId, currencyCode)
    }
}
