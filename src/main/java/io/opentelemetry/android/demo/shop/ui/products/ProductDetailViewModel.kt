package io.opentelemetry.android.demo.shop.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.clients.ProductApiService
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.api.common.AttributeKey.stringKey
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
    
    fun loadProduct(productId: String) {
        val tracer = OtelDemoApplication.tracer("product-detail")
        val span = tracer?.spanBuilder("loadProductDetail")
            ?.setAttribute(stringKey("component"), "product_detail")
            ?.setAttribute(stringKey("product.id"), productId)
            ?.startSpan()
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                span?.makeCurrent().use {
                    val product = productApiService.fetchProduct(productId)
                    _uiState.value = ProductDetailUiState(
                        product = product,
                        isLoading = false,
                        errorMessage = null
                    )
                    span?.setAttribute(stringKey("product.name"), product.name)
                    span?.setAttribute("product.price", product.priceValue())
                }
            } catch (e: Exception) {
                span?.setStatus(StatusCode.ERROR)
                span?.recordException(e)
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
    
    fun refreshProduct(productId: String) {
        loadProduct(productId)
    }
}
