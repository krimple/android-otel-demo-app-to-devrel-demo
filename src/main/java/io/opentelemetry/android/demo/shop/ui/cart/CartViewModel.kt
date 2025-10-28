package io.opentelemetry.android.demo.shop.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.android.demo.shop.model.Money
import io.opentelemetry.android.demo.shop.clients.CartApiService
import io.opentelemetry.android.demo.shop.clients.ProductApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.OtelDemoApplication.Companion.rum
import io.opentelemetry.api.trace.StatusCode

data class CartItem(
    val product: Product,
    var quantity: Int
) {
    fun totalPrice() = product.priceValue() * quantity
}

data class CartUiState(
    val cartItems: List<CartItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CartViewModel(
    private val cartApiService: CartApiService = CartApiService(),
    private val productApiService: ProductApiService = ProductApiService()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState
    
    val cartItems: StateFlow<List<CartItem>> = _uiState.map { it.cartItems }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )
    
    fun refreshCart(currencyCode: String = "USD") {
        loadCart(currencyCode)
    }
    
    private fun loadCart(currencyCode: String = "USD") {
        // User-initiated screen load - create root span
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("screen.load_cart")
            ?.setAttribute("app.screen.name", "cart")
            ?.setAttribute("app.user.currency", currencyCode)
            ?.startSpan()
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // Make this span current for API calls within this coroutine
                val scope = span?.makeCurrent()
                val (serverCart, products) = try {
                    val serverCart = cartApiService.getCart()
                    val products = productApiService.fetchProducts(currencyCode)
                    Pair(serverCart, products)
                } finally {
                    scope?.close()
                }
                val cartItems = serverCart.toCartItems(products)
                
                _uiState.value = CartUiState(
                    cartItems = cartItems,
                    isLoading = false,
                    errorMessage = null
                )
                
                span?.setAttribute("app.cart.items.count", cartItems.size.toLong())
                span?.setAttribute("app.cart.total.cost", getTotalPrice())
                
            } catch (e: Exception) {
                _uiState.value = CartUiState(
                    cartItems = emptyList(),
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load cart"
                )
                
                span?.setStatus(StatusCode.ERROR)
                OtelDemoApplication.logException(rum, e, null, Thread.currentThread())
            } finally {
                span?.end()
            }
        }
    }

    fun addProduct(product: Product, quantity: Int, currencyCode: String = "USD") {
        // User-initiated cart action - create root span
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("user.add_to_cart")
            ?.setAttribute("app.product.id", product.id)
            ?.setAttribute("app.product.name", product.name)
            ?.setAttribute("app.product.price.usd", product.priceValue())
            ?.setAttribute("app.cart.item.quantity", quantity.toLong())
            ?.setAttribute("app.user.currency", currencyCode)
            ?.startSpan()
        
        viewModelScope.launch {
            // Prevent concurrent executions of addProduct
            if (_uiState.value.isLoading) {
                span?.setAttribute("app.operation.status", "skipped_already_loading")
                span?.end()
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Check for demo scenarios
                val currentCartItems = _uiState.value.cartItems
                val currentExplorascopes = currentCartItems
                    .filter { it.product.name.contains("National Park Foundation Explorascope") }
                    .sumOf { it.quantity }
                val totalExplorascopes = currentExplorascopes + quantity
                
                if (totalExplorascopes == 10) {
                    // mark the span in error (for Honeycomb)
                    span?.setStatus(StatusCode.ERROR)
                    // create an exception and send to a Honeycomb trace-participating log message
                    var exception = Exception("The application crashed - unknown error.");
                    if (OtelDemoApplication.rum != null) {
                        OtelDemoApplication.logException(
                            OtelDemoApplication.rum!!,
                            exception,
                            null,
                            Thread.currentThread())
                    }
                    throw exception;
                } else if (totalExplorascopes == 9) {
                    // TODO - more interesting hang scenario with backend delay
                    span?.setAttribute("app.demo.trigger", "hang")
                }
                
                if (product.name.contains("The Comet Book")) {
                    span?.setAttribute("app.demo.trigger", "slow_animation")
                }
                
                // Make this span current for API calls within this coroutine
                val scope = span?.makeCurrent()
                val (serverCart, products) = try {
                    // Add to server-side cart
                    cartApiService.addItem(product.id, quantity)
                    
                    // Get updated cart state
                    val serverCart = cartApiService.getCart()
                    val products = productApiService.fetchProducts(currencyCode)
                    Pair(serverCart, products)
                } finally {
                    scope?.close()
                }
                val cartItems = serverCart.toCartItems(products)
                
                _uiState.value = CartUiState(
                    cartItems = cartItems,
                    isLoading = false,
                    errorMessage = null
                )
                
                // Add debug telemetry
                span?.setAttribute("app.cart.final.items.count", cartItems.size.toLong())
                span?.setAttribute("app.cart.final.total.cost", getTotalPrice())
                cartItems.forEachIndexed { index, item ->
                    span?.setAttribute("app.cart.final.item_${index}.product_id", item.product.id)
                    span?.setAttribute("app.cart.final.item_${index}.quantity", item.quantity.toLong())
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to add product to cart"
                )
                
                span?.setStatus(StatusCode.ERROR)
            } finally {
                span?.end()
            }
        }
    }

    fun getTotalPrice(): Double {
        return _uiState.value.cartItems.sumOf { it.totalPrice() }
    }

    fun getTotalPriceFormatted(currencyCode: String): String {
        val total = getTotalPrice()
        val totalMoney = Money(
            currencyCode = currencyCode,
            units = total.toLong(),
            nanos = ((total - total.toLong()) * 1_000_000_000).toLong()
        )
        return totalMoney.formatCurrency()
    }

    fun clearCart(currencyCode: String = "USD") {
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("user.clear_cart")
            ?.setAttribute("app.cart.total.cost", getTotalPrice())
            ?.setAttribute("app.cart.items.count", _uiState.value.cartItems.size.toLong())
            ?.setAttribute("app.user.currency", currencyCode)
            ?.startSpan()
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Make this span current for API calls within this coroutine
                val scope = span?.makeCurrent()
                try {
                    cartApiService.emptyCart()
                } finally {
                    scope?.close()
                }
                
                _uiState.value = CartUiState(
                    cartItems = emptyList(),
                    isLoading = false,
                    errorMessage = null
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to clear cart"
                )
                
                span?.setStatus(StatusCode.ERROR)
            } finally {
                span?.end()
            }
        }
    }

}
