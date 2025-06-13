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
        val tracer = OtelDemoApplication.tracer("cart.operations")
        val span = tracer?.spanBuilder("cart.add_product")
            ?.setAttribute(stringKey("product.id"), product.id)
            ?.setAttribute(stringKey("product.name"), product.name)
            ?.setAttribute(doubleKey("product.price"), product.priceValue())
            ?.setAttribute(longKey("product.quantity_added"), quantity.toLong())
            ?.startSpan()
        
        try {
            _cartItems.value = _cartItems.value.toMutableList().apply {
                val index = indexOfFirst { it.product.id == product.id }
                if (index >= 0) {
                    val oldQuantity = this[index].quantity
                    this[index] = this[index].copy(quantity = oldQuantity + quantity)
                    span?.setAttribute(longKey("product.previous_quantity"), oldQuantity.toLong())
                    span?.setAttribute(longKey("product.new_quantity"), (oldQuantity + quantity).toLong())
                } else {
                    add(CartItem(product, quantity))
                    span?.setAttribute(longKey("product.previous_quantity"), 0L)
                    span?.setAttribute(longKey("product.new_quantity"), quantity.toLong())
                }
            }
            
            span?.setAttribute(doubleKey("cart.total_price"), getTotalPrice())
            span?.setAttribute(longKey("cart.total_items"), _cartItems.value.size.toLong())
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
        val tracer = OtelDemoApplication.tracer("cart.operations")
        val span = tracer?.spanBuilder("cart.clear")
            ?.setAttribute(doubleKey("cart.total_price_before_clear"), getTotalPrice())
            ?.setAttribute(longKey("cart.items_count_before_clear"), _cartItems.value.size.toLong())
            ?.startSpan()
        
        try {
            _cartItems.value = emptyList()
            span?.setAttribute(doubleKey("cart.total_price_after_clear"), 0.0)
            span?.setAttribute(longKey("cart.items_count_after_clear"), 0L)
        } finally {
            span?.end()
        }
    }

}
