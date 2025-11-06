package io.opentelemetry.android.demo.shop.clients

import com.google.gson.Gson
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.model.*
import io.opentelemetry.android.demo.shop.session.SessionManager
import io.opentelemetry.api.trace.StatusCode
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class CartApiService {
    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun addItem(productId: String, quantity: Int) {
        val sessionManager = SessionManager.getInstance()
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("cart_api_service.addItem")
            ?.setAttribute("app.product.id", productId)
            ?.setAttribute("app.cart.item.quantity", quantity.toLong())
            ?.startSpan()

        try {
            span?.makeCurrent().use {
                val addItemRequest = AddItemRequest(
                    userId = sessionManager.currentSessionId,
                    item = CartItemRequest(productId = productId, quantity = quantity)
                )

                val requestBody = Gson().toJson(addItemRequest)
                val cartUrl = "${OtelDemoApplication.apiEndpoint}/cart?sessionId=${sessionManager.currentSessionId}"
                val request = Request.Builder()
                    .url(cartUrl)
                    .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                FetchHelpers.executeRequest(request)
            }
        } catch (e: Exception) {
            span?.setStatus(StatusCode.ERROR)
            if (OtelDemoApplication.rum !== null) {
                OtelDemoApplication.logException(
                    OtelDemoApplication.rum!!,
                    e,
                    null,
                    Thread.currentThread()
                )
            }
            throw e
        } finally {
            span?.end()
        }
    }

    suspend fun getCart(): ServerCart? {
        val sessionManager = SessionManager.getInstance()
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("cart_api_service.get_cart")
            ?.startSpan()

        var serverCart: ServerCart? = null

        try {
            span?.makeCurrent().use {
                val cartUrl = "${OtelDemoApplication.apiEndpoint}/cart?sessionId=${sessionManager.currentSessionId}"
                val request = Request.Builder()
                    .url(cartUrl)
                    .get()
                    .build()

                val bodyText = FetchHelpers.executeRequest(request)
                
                serverCart = Gson().fromJson(bodyText, ServerCart::class.java)
                span?.setAttribute("app.cart.items.count", serverCart.items.size.toLong())
                span?.setAttribute("app.cart.total.items", serverCart.getTotalItemCount().toLong())
                return serverCart
            }
        } catch (e: IOException) {
            // Handle 404 as empty cart (new session)
            if (e.message?.contains("404") == true) {
                span?.setAttribute("app.cart.empty.reason", "new_session")
                ServerCart(emptyList())
            } else {
                span?.setStatus(StatusCode.ERROR)
                if (OtelDemoApplication.rum !== null) {
                    OtelDemoApplication.logException(
                        OtelDemoApplication.rum!!,
                        e,
                        null,
                        Thread.currentThread()
                    )
                }
                throw e
            }
        } catch (e: Exception) {
            span?.setStatus(StatusCode.ERROR)
            if (OtelDemoApplication.rum !== null) {
                OtelDemoApplication.logException(
                    OtelDemoApplication.rum!!,
                    e,
                    null,
                    Thread.currentThread()
                )
            }
            throw e
        } finally {
            span?.end()
        }
        return serverCart
    }

    suspend fun emptyCart() {
        val sessionManager = SessionManager.getInstance()
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("cart_api_service.empty_cart")
            ?.startSpan()

        try {
            span?.makeCurrent().use {
                val cartUrl = "${OtelDemoApplication.apiEndpoint}/cart?sessionId=${sessionManager.currentSessionId}"
                val request = Request.Builder()
                    .url(cartUrl)
                    .delete()
                    .build()

                val baggageHeaders = mapOf("Baggage" to "session.id=${sessionManager.currentSessionId}")
                FetchHelpers.executeRequest(request)
            }
        } catch (e: Exception) {
            span?.setStatus(StatusCode.ERROR)
            if (OtelDemoApplication.rum !== null) {
                OtelDemoApplication.logException(
                    OtelDemoApplication.rum!!,
                    e,
                    null,
                    Thread.currentThread()
                )
            }
            throw e
        } finally {
            span?.end()
        }
    }
}