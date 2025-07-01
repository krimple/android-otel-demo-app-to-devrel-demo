package io.opentelemetry.android.demo.shop.model

import io.opentelemetry.android.demo.shop.ui.cart.CartItem
import org.junit.Test
import org.junit.Assert.*

class ServerCartModelsTest {

    private val testProduct = Product(
        id = "test-product-1",
        name = "Test Product",
        description = "A test product",
        picture = "test.jpg",
        priceUsd = PriceUsd("USD", 10, 500000000), // $10.50
        categories = listOf("test")
    )

    private val testProduct2 = Product(
        id = "test-product-2",
        name = "Another Test Product",
        description = "Another test product",
        picture = "test2.jpg",
        priceUsd = PriceUsd("USD", 25, 0), // $25.00
        categories = listOf("test")
    )

    @Test
    fun serverCartItem_toCartItem_convertsCorrectly() {
        val serverCartItem = ServerCartItem(
            productId = "test-product-1",
            quantity = 3
        )

        val cartItem = serverCartItem.toCartItem(testProduct)

        assertEquals("Product should match", testProduct, cartItem.product)
        assertEquals("Quantity should match", 3, cartItem.quantity)
    }

    @Test
    fun cartItem_toServerCartItem_convertsCorrectly() {
        val cartItem = CartItem(
            product = testProduct,
            quantity = 2
        )

        val serverCartItem = cartItem.toServerCartItem()

        assertEquals("Product ID should match", "test-product-1", serverCartItem.productId)
        assertEquals("Quantity should match", 2, serverCartItem.quantity)
    }

    @Test
    fun serverCart_toCartItems_convertsAllItems() {
        val serverCart = ServerCart(
            items = listOf(
                ServerCartItem("test-product-1", 2),
                ServerCartItem("test-product-2", 1)
            )
        )

        val products = listOf(testProduct, testProduct2)
        val cartItems = serverCart.toCartItems(products)

        assertEquals("Should convert all items", 2, cartItems.size)
        
        val item1 = cartItems.find { it.product.id == "test-product-1" }
        assertNotNull("Should find first product", item1)
        assertEquals("First item quantity should match", 2, item1!!.quantity)
        
        val item2 = cartItems.find { it.product.id == "test-product-2" }
        assertNotNull("Should find second product", item2)
        assertEquals("Second item quantity should match", 1, item2!!.quantity)
    }

    @Test
    fun serverCart_toCartItems_skipsItemsWithoutMatchingProducts() {
        val serverCart = ServerCart(
            items = listOf(
                ServerCartItem("test-product-1", 2),
                ServerCartItem("non-existent-product", 1),
                ServerCartItem("test-product-2", 3)
            )
        )

        val products = listOf(testProduct, testProduct2)
        val cartItems = serverCart.toCartItems(products)

        assertEquals("Should only convert items with matching products", 2, cartItems.size)
        assertTrue("Should contain first product", 
            cartItems.any { it.product.id == "test-product-1" })
        assertTrue("Should contain second product", 
            cartItems.any { it.product.id == "test-product-2" })
        assertFalse("Should not contain non-existent product", 
            cartItems.any { it.product.id == "non-existent-product" })
    }

    @Test
    fun serverCart_getTotalItemCount_calculatesCorrectly() {
        val emptyCart = ServerCart(emptyList())
        assertEquals("Empty cart should have 0 items", 0, emptyCart.getTotalItemCount())

        val cartWithItems = ServerCart(
            items = listOf(
                ServerCartItem("test-product-1", 2),
                ServerCartItem("test-product-2", 3),
                ServerCartItem("test-product-3", 1)
            )
        )
        assertEquals("Cart should have total of all quantities", 6, cartWithItems.getTotalItemCount())
    }

    @Test
    fun addItemRequest_createsCorrectStructure() {
        val request = AddItemRequest(
            userId = "session-123",
            item = CartItemRequest("test-product-1", 2)
        )

        assertEquals("User ID should match", "session-123", request.userId)
        assertEquals("Product ID should match", "test-product-1", request.item.productId)
        assertEquals("Quantity should match", 2, request.item.quantity)
    }

    @Test
    fun emptyCartRequest_createsCorrectStructure() {
        val request = EmptyCartRequest(userId = "session-123")
        
        assertEquals("User ID should match", "session-123", request.userId)
    }
}