package io.opentelemetry.android.demo.shop.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.clients.ProductApiService
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
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
    
    fun refreshProducts() {
        if (hasLoadedOnce) {
            loadProducts(isRefresh = true)
        } else {
            loadProducts(isRefresh = false)
            hasLoadedOnce = true
        }
    }
    
    private fun loadProducts(isRefresh: Boolean = false) {
        val tracer = OtelDemoApplication.tracer("astronomy-shop")
        val span = tracer?.spanBuilder("loadProducts")
            ?.setAttribute(stringKey("component"), "product_list")
            ?.setAttribute("is_refresh", isRefresh)
            ?.setAttribute(stringKey("user_action"), "view_product_list")
            ?.startSpan()
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                span?.makeCurrent().use {
                    val currentContext = Context.current()
                    val products = productApiService.fetchProducts(currentContext)
                    _uiState.value = ProductListUiState(
                        products = products,
                        isLoading = false,
                        errorMessage = null
                    )
                    span?.setAttribute(longKey("products.loaded"), products.size.toLong())
                    span?.setAttribute(stringKey("operation.result"), "success")
                }
            } catch (e: Exception) {
                span?.setStatus(StatusCode.ERROR)
                span?.recordException(e)
                _uiState.value = ProductListUiState(
                    products = emptyList(),
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load products"
                )
            } finally {
                span?.end()
            }
        }
    }
}
