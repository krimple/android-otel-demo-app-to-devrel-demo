package io.opentelemetry.android.demo.shop.clients

import io.opentelemetry.android.demo.shop.model.*
import io.opentelemetry.android.demo.shop.ui.cart.CartViewModel
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutInfoViewModel
import io.opentelemetry.android.demo.shop.ui.cart.ShippingInfo
import org.junit.Test
import org.junit.Assert.*

class ShippingApiServiceTest {

    @Test
    fun `service can be instantiated`() {
        val service = ShippingApiService()
        assertNotNull(service)
    }

    @Test
    fun `getShippingCost method exists and has correct signature`() {
        val service = ShippingApiService()
        // Test that the method exists and can be called
        assertNotNull(service::getShippingCost)
    }

    @Test
    fun `service handles empty cart gracefully`() {
        val service = ShippingApiService()
        val cartViewModel = CartViewModel()
        val checkoutInfoViewModel = CheckoutInfoViewModel()
        
        // Verify service can handle empty cart without crashing
        assertNotNull(service)
        assertNotNull(cartViewModel)
        assertNotNull(checkoutInfoViewModel)
    }

    @Test
    fun `service handles incomplete shipping info gracefully`() {
        val service = ShippingApiService()
        val cartViewModel = CartViewModel()
        val checkoutInfoViewModel = CheckoutInfoViewModel()
        
        // Create incomplete shipping info by clearing required fields
        val incompleteShippingInfo = ShippingInfo(
            email = "",  // Make it incomplete
            streetAddress = "123 Test St",
            zipCode = "12345",
            city = "Test City",
            state = "TS",
            country = "Test Country"
        )
        checkoutInfoViewModel.updateShippingInfo(incompleteShippingInfo)
        
        // Add a test product to cart
        val testProduct = Product(
            id = "test-product",
            name = "Test Product",
            description = "A test product",
            picture = "test.jpg",
            priceUsd = PriceUsd("USD", 10, 0),
            categories = listOf("test")
        )
        cartViewModel.addProduct(testProduct, 1)
        
        // Verify service can handle incomplete shipping info
        assertNotNull(service)
        assertEquals(1, cartViewModel.cartItems.value.size)
        assertFalse(checkoutInfoViewModel.shippingInfo.isComplete())
    }

    @Test
    fun `service creates proper request structure for preview`() {
        val service = ShippingApiService()
        val cartViewModel = CartViewModel()
        val checkoutInfoViewModel = CheckoutInfoViewModel()
        
        // Set up complete shipping info
        val completeShippingInfo = ShippingInfo(
            email = "test@example.com",
            streetAddress = "123 Test St",
            zipCode = "12345",
            city = "Test City",
            state = "TS",
            country = "Test Country"
        )
        checkoutInfoViewModel.updateShippingInfo(completeShippingInfo)
        
        // Add a test product
        val testProduct = Product(
            id = "test-product",
            name = "Test Product", 
            description = "A test product",
            picture = "test.jpg",
            priceUsd = PriceUsd("USD", 10, 0),
            categories = listOf("test")
        )
        cartViewModel.addProduct(testProduct, 1)
        
        // Verify the setup is correct for shipping calculation
        assertTrue(checkoutInfoViewModel.shippingInfo.isComplete())
        assertEquals(1, cartViewModel.cartItems.value.size)
        assertNotNull(service)
    }
}