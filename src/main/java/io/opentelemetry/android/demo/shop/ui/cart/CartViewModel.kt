package io.opentelemetry.android.demo.shop.ui.cart

import androidx.lifecycle.ViewModel
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.android.demo.shop.model.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.common.AttributeKey.doubleKey
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey

data class CartItem(
    val product: Product,
    var quantity: Int
) {
    fun totalPrice() = product.priceValue() * quantity
}

class CartViewModel : ViewModel() {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    fun addProduct(product: Product, quantity: Int) {
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("CartViewModel.addProduct")
            ?.setAttribute("app.product.id", product.id)
            ?.setAttribute("app.product.name", product.name)
            ?.setAttribute("app.product.price.usd", product.priceValue())
            ?.setAttribute("app.cart.item.quantity", quantity.toLong())
            ?.setAttribute("app.operation.type", "add_product")
            ?.setAttribute("app.view.model", "CartViewModel")
            ?.startSpan()
        
        try {
            _cartItems.value = _cartItems.value.toMutableList().apply {
                val index = indexOfFirst { it.product.id == product.id }
                if (index >= 0) {
                    val oldQuantity = this[index].quantity
                    this[index] = this[index].copy(quantity = oldQuantity + quantity)
                    span?.setAttribute("app.cart.previous.item.count", oldQuantity.toLong())
                    span?.setAttribute("app.cart.new.item.count", (oldQuantity + quantity).toLong())
                } else {
                    add(CartItem(product, quantity))
                    span?.setAttribute("app.cart.previous.item.count", 0L)
                    span?.setAttribute("app.cart.new.item.count", quantity.toLong())
                }
            }
            
            // Check for demo scenarios
            val totalExplorascopes = _cartItems.value
                .filter { it.product.name.contains("National Park Foundation Explorascope") }
                .sumOf { it.quantity }
            
            if (totalExplorascopes == 10) {
                span?.setAttribute("app.demo.trigger", "crash")
            } else if (totalExplorascopes == 9) {
                span?.setAttribute("app.demo.trigger", "hang")
            }
            
            if (product.name.contains("The Comet Book")) {
                span?.setAttribute("app.demo.trigger", "slow_animation")
            }
            
            span?.setAttribute("app.cart.total.cost", getTotalPrice())
            span?.setAttribute("app.cart.items.count", _cartItems.value.size.toLong())
            span?.setAttribute("app.operation.status", "success")
        } catch (e: Exception) {
            span?.setAttribute("app.operation.status", "failed")
            span?.recordException(e)
            throw e
        } finally {
            span?.end()
        }
    }

    fun getTotalPrice(): Double {
        return _cartItems.value.sumOf { it.totalPrice() }
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

    fun clearCart() {
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("CartViewModel.clearCart")
            ?.setAttribute("app.cart.total.cost", getTotalPrice())
            ?.setAttribute("app.cart.items.count", _cartItems.value.size.toLong())
            ?.setAttribute("app.operation.type", "clear_cart")
            ?.setAttribute("app.view.model", "CartViewModel")
            ?.startSpan()
        
        try {
            _cartItems.value = emptyList()
            span?.setAttribute("app.operation.status", "success")
        } catch (e: Exception) {
            span?.setAttribute("app.operation.status", "failed")
            span?.recordException(e)
            throw e
        } finally {
            span?.end()
        }
    }

}
