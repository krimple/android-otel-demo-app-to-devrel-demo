package io.opentelemetry.android.demo.shop.model

import io.opentelemetry.android.demo.shop.ui.cart.CartItem

/**
 * Request model for adding items to the server-side cart
 */
data class AddItemRequest(
    val userId: String,
    val item: CartItemRequest
)

/**
 * Request model for cart item data sent to server
 */
data class CartItemRequest(
    val productId: String,
    val quantity: Int
)

/**
 * Request model for emptying the server-side cart
 */
data class EmptyCartRequest(
    val userId: String
)

/**
 * Response model representing the server-side cart
 */
data class ServerCart(
    val items: List<ServerCartItem> = emptyList()
) {
    /**
     * Converts server cart items to UI cart items using provided products
     */
    fun toCartItems(products: List<Product>): List<CartItem> {
        return items.mapNotNull { serverItem ->
            products.find { it.id == serverItem.productId }?.let { product ->
                CartItem(product = product, quantity = serverItem.quantity)
            }
        }
    }
    
    /**
     * Gets the total number of items in the cart
     */
    fun getTotalItemCount(): Int {
        return items.sumOf { it.quantity }
    }
}

/**
 * Response model representing an individual cart item from the server
 */
data class ServerCartItem(
    val productId: String,
    val quantity: Int
) {
    /**
     * Converts server cart item to UI cart item using provided product
     */
    fun toCartItem(product: Product): CartItem {
        return CartItem(product = product, quantity = quantity)
    }
}

/**
 * Extension function to convert UI CartItem to server request format
 */
fun CartItem.toServerCartItem(): CartItemRequest {
    return CartItemRequest(
        productId = product.id,
        quantity = quantity
    )
}