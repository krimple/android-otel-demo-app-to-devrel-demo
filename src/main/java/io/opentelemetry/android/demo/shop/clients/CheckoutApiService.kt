package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.*
import io.opentelemetry.android.demo.shop.ui.cart.CartViewModel
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutInfoViewModel
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import io.opentelemetry.api.common.AttributeKey.doubleKey
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey

class CheckoutApiService(
    private val tracer: Tracer
) {
    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun placeOrder(
        cartViewModel: CartViewModel,
        checkoutInfoViewModel: CheckoutInfoViewModel,
        parentContext: Context = Context.current()
    ): CheckoutResponse {
        val span = tracer.spanBuilder("checkout.place_order")
            .setParent(parentContext)
            .startSpan()
            
        return span.makeCurrent().use {
            try {
                val checkoutRequest = buildCheckoutRequest(cartViewModel, checkoutInfoViewModel)
            
            // Add checkout request attributes
            span.setAttribute(stringKey("checkout.user_id"), checkoutRequest.userId)
            span.setAttribute(stringKey("checkout.email"), checkoutRequest.email)
            span.setAttribute(stringKey("checkout.currency"), checkoutRequest.userCurrency)
            span.setAttribute(stringKey("checkout.address.city"), checkoutRequest.address.city)
            span.setAttribute(stringKey("checkout.address.state"), checkoutRequest.address.state)
            span.setAttribute(stringKey("checkout.address.country"), checkoutRequest.address.country)
            span.setAttribute(stringKey("checkout.address.zip_code"), checkoutRequest.address.zipCode)
            span.setAttribute(stringKey("checkout.credit_card.number_masked"), "****-****-****-${checkoutRequest.creditCard.creditCardNumber.takeLast(4)}")
            span.setAttribute(longKey("checkout.credit_card.expiry_month"), checkoutRequest.creditCard.creditCardExpirationMonth.toLong())
            span.setAttribute(longKey("checkout.credit_card.expiry_year"), checkoutRequest.creditCard.creditCardExpirationYear.toLong())
            
            // Add cart information
            val cartItems = cartViewModel.cartItems.value
            span.setAttribute(longKey("checkout.cart.items_count"), cartItems.size.toLong())
            span.setAttribute(doubleKey("checkout.cart.total_price"), cartViewModel.getTotalPrice())
            
            cartItems.forEachIndexed { index, item ->
                span.setAttribute(stringKey("checkout.cart.item_${index}.product_id"), item.product.id)
                span.setAttribute(stringKey("checkout.cart.item_${index}.product_name"), item.product.name)
                span.setAttribute(longKey("checkout.cart.item_${index}.quantity"), item.quantity.toLong())
                span.setAttribute(doubleKey("checkout.cart.item_${index}.unit_price"), item.product.priceValue())
                span.setAttribute(doubleKey("checkout.cart.item_${index}.total_price"), item.totalPrice())
            }
            
            val requestBody = Gson().toJson(checkoutRequest)
            val checkoutUrl = "${OtelDemoApplication.apiEndpoint}/checkout?currencyCode=USD"
            val request = Request.Builder()
                .url(checkoutUrl)
                .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val responseBody = FetchHelpers.executeRequest(request)
            val checkoutResponse = Gson().fromJson(responseBody, CheckoutResponse::class.java)
            
            // Add response attributes
            span.setAttribute(stringKey("checkout.response.order_id"), checkoutResponse.orderId)
            span.setAttribute(stringKey("checkout.response.shipping_tracking_id"), checkoutResponse.shippingTrackingId)
            span.setAttribute(doubleKey("checkout.response.shipping_cost"), checkoutResponse.shippingCost.toDouble())
            span.setAttribute(stringKey("checkout.response.shipping_cost.currency"), checkoutResponse.shippingCost.currencyCode)
            span.setAttribute(longKey("checkout.response.items_count"), checkoutResponse.items.size.toLong())
            
            var totalCost = 0.0
            checkoutResponse.items.forEachIndexed { index, item ->
                span.setAttribute(stringKey("checkout.response.item_${index}.product_id"), item.item.productId)
                span.setAttribute(longKey("checkout.response.item_${index}.quantity"), item.item.quantity.toLong())
                span.setAttribute(doubleKey("checkout.response.item_${index}.cost"), item.cost.toDouble())
                span.setAttribute(stringKey("checkout.response.item_${index}.cost.currency"), item.cost.currencyCode)
                totalCost += item.cost.toDouble()
            }
            
                span.setAttribute(doubleKey("checkout.response.total_item_cost"), totalCost)
                span.setAttribute(doubleKey("checkout.response.total_order_cost"), totalCost + checkoutResponse.shippingCost.toDouble())
                
                checkoutResponse
            } catch (e: Exception) {
                span.recordException(e)
                span.setAttribute(stringKey("error.message"), e.message ?: "Unknown error")
                throw e
            } finally {
                span.end()
            }
        }
    }

    private fun buildCheckoutRequest(
        cartViewModel: CartViewModel,
        checkoutInfoViewModel: CheckoutInfoViewModel
    ): CheckoutRequest {
        val shippingInfo = checkoutInfoViewModel.shippingInfo
        val paymentInfo = checkoutInfoViewModel.paymentInfo
        val cartItems = cartViewModel.cartItems.value

        return CheckoutRequest(
            userId = UUID.randomUUID().toString(),
            email = shippingInfo.email,
            address = CheckoutAddress(
                streetAddress = shippingInfo.streetAddress,
                state = shippingInfo.state,
                country = shippingInfo.country,
                city = shippingInfo.city,
                zipCode = shippingInfo.zipCode
            ),
            userCurrency = "USD",
            creditCard = CheckoutCreditCard(
                creditCardCvv = paymentInfo.cvv.toIntOrNull() ?: 0,
                creditCardExpirationMonth = paymentInfo.expiryMonth.toIntOrNull() ?: 1,
                creditCardExpirationYear = paymentInfo.expiryYear.toIntOrNull() ?: 2030,
                creditCardNumber = paymentInfo.creditCardNumber
            ),
            items = cartItems.map { cartItem ->
                CheckoutRequestItem(
                    productId = cartItem.product.id,
                    quantity = cartItem.quantity
                )
            }
        )
    }
}