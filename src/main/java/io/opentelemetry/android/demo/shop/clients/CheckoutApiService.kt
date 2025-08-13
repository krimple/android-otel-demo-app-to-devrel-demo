package io.opentelemetry.android.demo.shop.clients

import android.content.Context.MODE_PRIVATE
import com.google.gson.Gson
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.*
import io.opentelemetry.android.demo.shop.ui.cart.CartViewModel
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutInfoViewModel
import io.opentelemetry.android.demo.shop.session.SessionManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import io.opentelemetry.api.common.AttributeKey.doubleKey
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.StatusCode
import android.util.Log

class CheckoutApiService(
) {
    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
    
    private val sessionManager = SessionManager.getInstance()

    suspend fun placeOrder(
        cartViewModel: CartViewModel,
        checkoutInfoViewModel: CheckoutInfoViewModel,
    ): CheckoutResponse {
        val currencyCode = OtelDemoApplication.getInstance().getSharedPreferences("currency_prefs", MODE_PRIVATE)
            ?.getString("selected_currency", "USD") ?: "USD"
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("app.checkout.place_order")
            ?.startSpan()

        span?.setAttribute(stringKey("currency.code"), currencyCode)

        // TODO - if no span, then what?
        return span?.makeCurrent().use {
            try {
                val checkoutRequest = buildCheckoutRequest(checkoutInfoViewModel, currencyCode)

                // Add checkout request attributes
                span?.setAttribute(stringKey("app.checkout.user_id"), checkoutRequest.userId)
                span?.setAttribute(stringKey("app.checkout.email"), checkoutRequest.email)
                span?.setAttribute(stringKey("app.checkout.currency"), checkoutRequest.userCurrency)
                span?.setAttribute(stringKey("app.checkout.address.city"), checkoutRequest.address.city)
                span?.setAttribute(stringKey("app.checkout.address.state"), checkoutRequest.address.state)
                span?.setAttribute(stringKey("app.checkout.address.country"), checkoutRequest.address.country)
                span?.setAttribute(stringKey("app.checkout.address.zip_code"), checkoutRequest.address.zipCode)
                span?.setAttribute(stringKey("app.checkout.credit_card.number_masked"), "****-****-****-${checkoutRequest.creditCard.creditCardNumber.takeLast(4)}")
                span?.setAttribute(longKey("app.checkout.credit_card.expiry_month"), checkoutRequest.creditCard.creditCardExpirationMonth.toLong())
                span?.setAttribute(longKey("app.checkout.credit_card.expiry_year"), checkoutRequest.creditCard.creditCardExpirationYear.toLong())

                // Note: Cart items now come from server-side cart via sessionId
                span?.setAttribute("app.checkout.cart.source", "server_side")

                val requestBody = Gson().toJson(checkoutRequest)
                val checkoutUrl = "${OtelDemoApplication.apiEndpoint}/checkout?currencyCode=$currencyCode&sessionId=${sessionManager.currentSessionId}"
                val request = Request.Builder()
                    .url(checkoutUrl)
                    .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val baggageHeaders = mapOf("Baggage" to "session.id=${sessionManager.currentSessionId}")

                val responseBody = FetchHelpers.executeRequestWithBaggage(request, baggageHeaders)
                val checkoutResponse = Gson().fromJson(responseBody, CheckoutResponse::class.java)

                // Add response attributes
                span?.setAttribute(stringKey("app.checkout.response.order_id"), checkoutResponse.orderId)
                span?.setAttribute(stringKey("app.checkout.response.shipping_tracking_id"), checkoutResponse.shippingTrackingId)
                span?.setAttribute(doubleKey("app.checkout.response.shipping_cost"), checkoutResponse.shippingCost.toDouble())
                span?.setAttribute(stringKey("app.checkout.response.shipping_cost.currency"), checkoutResponse.shippingCost.currencyCode)
                span?.setAttribute(longKey("app.checkout.response.items_count"), checkoutResponse.items.size.toLong())

                var totalCost = 0.0
                checkoutResponse.items.forEachIndexed { index, item ->
                    span?.setAttribute(stringKey("app.checkout.response.item_${index}.product_id"), item.item.productId)
                    span?.setAttribute(longKey("app.checkout.response.item_${index}.quantity"), item.item.quantity.toLong())
                    span?.setAttribute(doubleKey("app.checkout.response.item_${index}.cost"), item.cost.toDouble())
                    span?.setAttribute(stringKey("app.checkout.response.item_${index}.cost.currency"), item.cost.currencyCode)
                    totalCost += item.cost.toDouble()
                }

                span?.setAttribute(doubleKey("app.checkout.response.total_item_cost"), totalCost)
                span?.setAttribute(
                    doubleKey("app.checkout.response.order.total"),
                    totalCost + checkoutResponse.shippingCost.toDouble())

                // Reset session after successful checkout
                sessionManager.resetSession()
                span?.setAttribute("app.checkout.session.reset", true)

                // return this response
                checkoutResponse
            } catch (e: Exception) {
                Log.d("CheckoutApiService", "SPAN ERROR: checkout.place_order span=$span, exception=${e.message}")
                span?.recordException(e)
                span?.setStatus(StatusCode.ERROR)
                span?.setAttribute(stringKey("error.message"), e.message ?: "Unknown error")
                throw e
            } finally {
                span?.end()
            }
        }
    }

    private fun buildCheckoutRequest(
        checkoutInfoViewModel: CheckoutInfoViewModel,
        currencyCode: String = "USD"
    ): CheckoutRequest {
        val shippingInfo = checkoutInfoViewModel.shippingInfo
        val paymentInfo = checkoutInfoViewModel.paymentInfo

        return CheckoutRequest(
            userId = sessionManager.currentSessionId,
            email = shippingInfo.email,
            address = CheckoutAddress(
                streetAddress = shippingInfo.streetAddress,
                state = shippingInfo.state,
                country = shippingInfo.country,
                city = shippingInfo.city,
                zipCode = shippingInfo.zipCode
            ),
            userCurrency = currencyCode,
            creditCard = CheckoutCreditCard(
                creditCardCvv = paymentInfo.cvv.toIntOrNull() ?: 0,
                creditCardExpirationMonth = paymentInfo.expiryMonth.toIntOrNull() ?: 1,
                creditCardExpirationYear = paymentInfo.expiryYear.toIntOrNull() ?: 2030,
                creditCardNumber = paymentInfo.creditCardNumber
            )
            // Note: items removed - server will get them from cart using sessionId
        )
    }
}