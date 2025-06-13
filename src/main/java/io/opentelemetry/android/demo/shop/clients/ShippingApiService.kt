package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.*
import io.opentelemetry.android.demo.shop.ui.cart.CartViewModel
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutInfoViewModel
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log

class ShippingApiService {
    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun getShippingCost(
        cartViewModel: CartViewModel,
        checkoutInfoViewModel: CheckoutInfoViewModel,
        currencyCode: String = "USD",
        parentContext: Context = Context.current()
    ): Money {
        val tracer = OtelDemoApplication.tracer("astronomy-shop")
        Log.d("otel.demo", "Getting shipping cost preview for ${cartViewModel.cartItems.value.size} items")

        val span = tracer?.spanBuilder("getShippingCost")
            ?.setParent(parentContext)
            ?.setAttribute("currency.code", currencyCode)
            ?.setAttribute("shipping.items.count", cartViewModel.cartItems.value.size.toLong())
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

                val responseBody = FetchHelpers.executeRequest(request)
                val checkoutResponse = Gson().fromJson(responseBody, CheckoutResponse::class.java)
                
                Log.d("otel.demo", "Shipping preview completed - cost: ${checkoutResponse.shippingCost.formatCurrency()}")
                
                span?.setAttribute("shipping.cost.value", checkoutResponse.shippingCost.toDouble())
                span?.setAttribute("shipping.cost.currency", checkoutResponse.shippingCost.currencyCode)
                span?.setAttribute("shipping.preview", true)
                
                checkoutResponse.shippingCost
            }
        } catch (e: Exception) {
            span?.setStatus(StatusCode.ERROR)
            span?.recordException(e)
            span?.setAttribute("shipping.calculation.failed", true)
            span?.setAttribute("shipping.calculation.error.type", e.javaClass.simpleName)
            span?.setAttribute("shipping.calculation.error.message", e.message ?: "Unknown error")
            span?.setAttribute("shipping.calculation.request.url", checkoutUrl)
            span?.setAttribute("shipping.calculation.fallback.cost", 0.0)
            Log.d("otel.demo", "Shipping preview request failed, returning zero cost fallback")
            // Return zero cost as fallback
            Money(currencyCode = currencyCode, units = 0, nanos = 0)
        } finally {
            Log.d("otel.demo", "Ending shipping preview span: $span")
            span?.end()
        }
    }
}