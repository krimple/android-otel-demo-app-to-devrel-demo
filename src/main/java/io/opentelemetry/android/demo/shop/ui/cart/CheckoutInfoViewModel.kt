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
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.OtelDemoApplication.Companion.rum
import io.opentelemetry.api.trace.StatusCode
import kotlinx.coroutines.Dispatchers
import okhttp3.Dispatcher

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
    private var hasCalculatedShipping = false

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
        // Reset calculation flag when shipping info changes
        if (!newShippingInfo.isComplete()) {
            hasCalculatedShipping = false
            shippingCost = null
        }
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

    fun calculateShippingCostIfNeeded(cartViewModel: CartViewModel, currencyCode: String = "USD") {
        // Only calculate if we haven't already calculated for the current complete shipping info
        if (hasCalculatedShipping || !shippingInfo.isComplete() || cartViewModel.cartItems.value.isEmpty()) {
            return
        }
        calculateShippingCost(cartViewModel, currencyCode)
    }

    private fun calculateShippingCost(cartViewModel: CartViewModel, currencyCode: String = "USD") {
        val tracer = OtelDemoApplication.getTracer()

        viewModelScope.launch(Dispatchers.IO) {
            if (!shippingInfo.isComplete() || cartViewModel.cartItems.value.isEmpty()) {
                val span = tracer?.spanBuilder("checkout_info_vm.skip_shipping")
                    ?.setAttribute("app.shipping.calculation.skipped", true)
                    ?.setAttribute(
                        "app.shipping.calculation.skip.reason",
                        if (!shippingInfo.isComplete()) "incomplete_shipping_info" else "empty_cart"
                    )
                    ?.setAttribute("app.user.currency", currencyCode)
                    ?.setAttribute("app.view.model", "CheckoutInfoViewModel")
                    ?.setAttribute("app.operation.type", "skip_shipping_calculation")
                    ?.startSpan()
                shippingCost = null
                span?.end()
            } else {
                val span = tracer?.spanBuilder("checkout_info_vm.calculate_shipping")
                    ?.setAttribute("app.user.currency", currencyCode)
                    ?.setAttribute("app.shipping.info.complete", shippingInfo.isComplete())
                    ?.setAttribute(
                        "app.cart.items.count",
                        cartViewModel.cartItems.value.size.toLong()
                    )
                    ?.setAttribute("app.shipping.address.city", shippingInfo.city)
                    ?.setAttribute("app.shipping.address.state", shippingInfo.state)
                    ?.setAttribute("app.shipping.address.country", shippingInfo.country)
                    ?.setAttribute("app.view.model", "CheckoutInfoViewModel")
                    ?.startSpan()
                span?.makeCurrent().use {
                    isCalculatingShipping = true
                    shippingCalculationError = null

                    try {
                        span?.setAttribute("app.shipping.calculation.started", true)
                        val cost = shippingApiService.getShippingCost(
                            cartViewModel = cartViewModel,
                            checkoutInfoViewModel = this@CheckoutInfoViewModel,
                            currencyCode = currencyCode
                        )
                        shippingCost = cost
                        span?.setAttribute("app.shipping.cost", cost.toDouble())
                        hasCalculatedShipping = true
                    } catch (e: Exception) {
                        span?.setStatus(StatusCode.ERROR)
                        OtelDemoApplication.logException(rum, e, null, Thread.currentThread())
                        shippingCalculationError = "Failed to calculate shipping: ${e.message}"
                    } finally {
                        isCalculatingShipping = false
                        span?.end()
                    }
                }
            }
        }
    }
}