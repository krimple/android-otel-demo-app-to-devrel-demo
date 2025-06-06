package io.opentelemetry.android.demo.shop.model

import com.google.gson.annotations.SerializedName

data class CheckoutRequest(
    val userId: String,
    val email: String,
    val address: CheckoutAddress,
    val userCurrency: String,
    val creditCard: CheckoutCreditCard
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
        return "$${String.format("%.2f", toDouble())}"
    }
}