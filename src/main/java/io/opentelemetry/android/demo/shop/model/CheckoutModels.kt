package io.opentelemetry.android.demo.shop.model

import com.google.gson.annotations.SerializedName

data class CheckoutRequest(
    val userId: String,
    val email: String,
    val address: CheckoutAddress,
    val userCurrency: String,
    val creditCard: CheckoutCreditCard,
    val items: List<CheckoutRequestItem>? = null  // Optional for server-side cart
)

data class CheckoutAddress(
    val streetAddress: String,
    val state: String,
    val country: String,
    val city: String,
    val zipCode: String
)

data class CheckoutCreditCard(
    val creditCardCvv: Int,
    val creditCardExpirationMonth: Int,
    val creditCardExpirationYear: Int,
    val creditCardNumber: String
)

data class CheckoutRequestItem(
    val productId: String,
    val quantity: Int
)

data class CheckoutResponse(
    val orderId: String,
    val shippingTrackingId: String,
    val shippingCost: Money,
    val shippingAddress: CheckoutAddress,
    val items: List<CheckoutItem>
)

data class CheckoutItem(
    val cost: Money,
    val item: CheckoutItemDetail
)

data class CheckoutItemDetail(
    val productId: String,
    val quantity: Int,
    val product: Product
)

data class Money(
    val currencyCode: String,
    val units: Long,
    val nanos: Long
) {
    fun toDouble(): Double {
        return units + (nanos / 1_000_000_000.0)
    }
    
    fun formatCurrency(): String {
        val value = toDouble()
        return when (currencyCode) {
            "USD" -> "$${String.format("%.2f", value)}"
            "EUR" -> "€${String.format("%.2f", value)}"
            "GBP" -> "£${String.format("%.2f", value)}"
            "JPY" -> "¥${String.format("%.0f", value)}"
            else -> "${currencyCode} ${String.format("%.2f", value)}"
        }
    }
}