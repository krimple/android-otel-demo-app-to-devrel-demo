package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import io.opentelemetry.android.demo.shop.model.*
import io.opentelemetry.android.demo.shop.ui.cart.CartViewModel
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutInfoViewModel
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class CheckoutApiService(
    private val tracer: Tracer
) {
    companion object {
        private const val CHECKOUT_API_URL = "https://www.zurelia.honeydemo.io/api/checkout?currencyCode=USD"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun placeOrder(
        cartViewModel: CartViewModel,
        checkoutInfoViewModel: CheckoutInfoViewModel,
        parentContext: Context = Context.current()
    ): CheckoutResponse {
        val checkoutRequest = buildCheckoutRequest(cartViewModel, checkoutInfoViewModel)
        
        val requestBody = Gson().toJson(checkoutRequest)
        val request = Request.Builder()
            .url(CHECKOUT_API_URL)
            .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val responseBody = FetchHelpers.executeRequest(request)
        return Gson().fromJson(responseBody, CheckoutResponse::class.java)
    }

    private fun buildCheckoutRequest(
        cartViewModel: CartViewModel,
        checkoutInfoViewModel: CheckoutInfoViewModel
    ): CheckoutRequest {
        val shippingInfo = checkoutInfoViewModel.shippingInfo
        val paymentInfo = checkoutInfoViewModel.paymentInfo

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
            )
        )
    }
}