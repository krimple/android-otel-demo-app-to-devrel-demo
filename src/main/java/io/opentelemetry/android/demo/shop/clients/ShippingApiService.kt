package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.*
import io.opentelemetry.android.demo.shop.session.SessionManager
import io.opentelemetry.android.demo.shop.ui.cart.CartViewModel
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutInfoViewModel
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log

class ShippingApiService {
    private val sessionManager = SessionManager.getInstance()
    
    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun getShippingCost(
        cartViewModel: CartViewModel,
        checkoutInfoViewModel: CheckoutInfoViewModel,
        currencyCode: String = "USD",
        parentContext: Context = Context.current()
    ): Money {
        val tracer = OtelDemoApplication.getTracer()
        Log.d("otel.demo", "Getting shipping cost preview for ${cartViewModel.cartItems.value.size} items")

        val span = tracer?.spanBuilder("ShippingAPIService.getShippingCost")
            ?.setParent(parentContext)
            ?.setAttribute("app.user.currency", currencyCode)
            ?.setAttribute("app.cart.items.count", cartViewModel.cartItems.value.size.toLong())
            ?.setAttribute("app.operation.type", "calculate_shipping")
            ?.startSpan()

        // Use checkout API as preview to get shipping cost
        val shippingInfo = checkoutInfoViewModel.shippingInfo
        val cartItems = cartViewModel.cartItems.value
        val checkoutUrl = "${OtelDemoApplication.apiEndpoint}/checkout?currencyCode=$currencyCode"

        return try {
            span?.makeCurrent().use {
                // Create a preview checkout request (we won't actually process payment)
                val checkoutRequest = CheckoutRequest(
                    userId = "shipping-preview",
                    email = shippingInfo.email.ifEmpty { "preview@example.com" },
                    address = CheckoutAddress(
                        streetAddress = shippingInfo.streetAddress.ifEmpty { "Preview St" },
                        state = shippingInfo.state,
                        country = shippingInfo.country,
                        city = shippingInfo.city,
                        zipCode = shippingInfo.zipCode
                    ),
                    userCurrency = currencyCode,
                    creditCard = CheckoutCreditCard(
                        creditCardCvv = 123,
                        creditCardExpirationMonth = 12,
                        creditCardExpirationYear = 2030,
                        creditCardNumber = "4111111111111111"
                    ),
                    items = cartItems.map { cartItem ->
                        CheckoutRequestItem(
                            productId = cartItem.product.id,
                            quantity = cartItem.quantity
                        )
                    }
                )

                val requestBody = Gson().toJson(checkoutRequest)
                Log.d("otel.demo", "Making shipping preview request to: $checkoutUrl")

                val request = Request.Builder()
                    .url(checkoutUrl)
                    .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val baggageHeaders = mapOf("Baggage" to "session.id=${sessionManager.currentSessionId}")
                val responseBody = FetchHelpers.executeRequestWithBaggage(request, baggageHeaders)
                val checkoutResponse = Gson().fromJson(responseBody, CheckoutResponse::class.java)
                
                Log.d("otel.demo", "Shipping preview completed - cost: ${checkoutResponse.shippingCost.formatCurrency()}")
                
                span?.setAttribute("app.shipping.cost", checkoutResponse.shippingCost.toDouble())
                
                checkoutResponse.shippingCost
            }
        } catch (e: Exception) {
            span?.setStatus(StatusCode.ERROR)
            span?.recordException(e)
            Log.d("otel.demo", "Shipping preview request failed, returning zero cost fallback")
            // Return zero cost as fallback
            Money(currencyCode = currencyCode, units = 0, nanos = 0)
        } finally {
            Log.d("otel.demo", "Ending shipping preview span: $span")
            span?.end()
        }
    }
}