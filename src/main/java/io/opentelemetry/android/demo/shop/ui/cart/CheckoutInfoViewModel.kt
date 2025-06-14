package io.opentelemetry.android.demo.shop.ui.cart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.opentelemetry.android.demo.shop.clients.ShippingApiService
import io.opentelemetry.android.demo.shop.model.CheckoutResponse
import io.opentelemetry.android.demo.shop.model.Money
import kotlinx.coroutines.launch
import android.util.Log
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.trace.StatusCode

data class ShippingInfo(
    var email: String = "someone@example.com",
    var streetAddress: String = "1600 Amphitheatre Parkway",
    var zipCode: String = "94043",
    var city: String = "Mountain View",
    var state: String = "CA",
    var country: String = "United States"
) {
    fun isComplete(): Boolean {
        return arrayOf(email, streetAddress, zipCode, city, state, country)
            .all { it.isNotBlank() }
    }
}

data class PaymentInfo(
    var creditCardNumber: String = "4432-8015-6152-0454",
    var expiryMonth: String = "01",
    var expiryYear: String = "2030",
    var cvv: String = "137"
) {
    fun isComplete(): Boolean {
        return arrayOf(creditCardNumber, expiryMonth, expiryYear, cvv)
            .all { it.isNotBlank() }
    }
}

class CheckoutInfoViewModel : ViewModel() {
    private val shippingApiService = ShippingApiService()

    var shippingInfo by mutableStateOf(ShippingInfo())
        private set

    var paymentInfo by mutableStateOf(PaymentInfo())
        private set

    var checkoutResponse by mutableStateOf<CheckoutResponse?>(null)
        private set

    var shippingCost by mutableStateOf<Money?>(null)
        private set

    var isCalculatingShipping by mutableStateOf(false)
        private set

    var shippingCalculationError by mutableStateOf<String?>(null)
        private set

    fun updateShippingInfo(newShippingInfo: ShippingInfo) {
        shippingInfo = newShippingInfo
    }

    fun updatePaymentInfo(newPaymentInfo: PaymentInfo) {
        paymentInfo = newPaymentInfo
    }

    fun updateCheckoutResponse(response: CheckoutResponse) {
        checkoutResponse = response
    }

    fun canProceedToCheckout(): Boolean {
        return shippingInfo.isComplete() && paymentInfo.isComplete()
    }

    fun calculateShippingCost(cartViewModel: CartViewModel, currencyCode: String = "USD") {
        val tracer = OtelDemoApplication.tracer("astronomy-shop")
        val span = tracer?.spanBuilder("calculateShippingCost")
            ?.setAttribute("currency.code", currencyCode)
            ?.setAttribute("shipping.info.complete", shippingInfo.isComplete())
            ?.setAttribute("cart.items.count", cartViewModel.cartItems.value.size.toLong())
            ?.setAttribute("shipping.info.email", shippingInfo.email)
            ?.setAttribute("shipping.info.city", shippingInfo.city)
            ?.setAttribute("shipping.info.state", shippingInfo.state)
            ?.setAttribute("shipping.info.country", shippingInfo.country)
            ?.setAttribute("shipping.info.zip_code", shippingInfo.zipCode)
            ?.startSpan()
        
        if (!shippingInfo.isComplete() || cartViewModel.cartItems.value.isEmpty()) {
            span?.setAttribute("shipping.calculation.skipped", true)
            span?.setAttribute("shipping.calculation.skip_reason", 
                if (!shippingInfo.isComplete()) "incomplete_shipping_info" else "empty_cart")
            span?.end()
            shippingCost = null
            return
        }

        viewModelScope.launch {
            span?.makeCurrent().use {
                isCalculatingShipping = true
                shippingCalculationError = null
                
                try {
                    span?.setAttribute("shipping.calculation.started", true)
                    val cost = shippingApiService.getShippingCost(
                        cartViewModel = cartViewModel,
                        checkoutInfoViewModel = this@CheckoutInfoViewModel,
                        currencyCode = currencyCode
                    )
                    shippingCost = cost
                    span?.setAttribute("shipping.calculation.success", true)
                    span?.setAttribute("shipping.cost.calculated.value", cost.toDouble())
                    span?.setAttribute("shipping.cost.calculated.currency", cost.currencyCode)
                } catch (e: Exception) {
                    span?.setStatus(StatusCode.ERROR)
                    span?.recordException(e)
                    span?.setAttribute("shipping.calculation.failed", true)
                    span?.setAttribute("shipping.calculation.error.type", e.javaClass.simpleName)
                    span?.setAttribute("shipping.calculation.error.message", e.message ?: "Unknown error")
                    shippingCalculationError = "Failed to calculate shipping: ${e.message}"
                } finally {
                    isCalculatingShipping = false
                    span?.end()
                }
            }
        }
    }

    fun getTotalCost(cartViewModel: CartViewModel): Double {
        val cartTotal = cartViewModel.getTotalPrice()
        val shipping = shippingCost?.toDouble() ?: 0.0
        return cartTotal + shipping
    }
}