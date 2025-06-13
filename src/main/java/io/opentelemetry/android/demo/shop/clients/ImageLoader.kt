package io.opentelemetry.android.demo.shop.clients

import io.opentelemetry.android.demo.OtelDemoApplication

/**
 * Image URL utilities for remote image loading via Coil.
 * 
 * This replaces the previous local asset-based ImageLoader with remote URL support.
 * Images are now loaded directly by Coil's AsyncImage composables.
 */
object ImageLoader {
    
    /**
     * Constructs the full image URL from the picture field.
     * If the picture field already contains a full URL, returns it as-is.
     * Otherwise, constructs URL using the same base endpoint as the API.
     */
    fun getImageUrl(picture: String): String {
        return if (picture.startsWith("http://") || picture.startsWith("https://")) {
            picture
        } else {
            // Use the same base endpoint as the API, but replace /api with /images/products
            // This ensures images are served from the same local/remote environment as the API
            val baseUrl = OtelDemoApplication.apiEndpoint.removeSuffix("/api")
            "$baseUrl/images/products/$picture"
        }
    }
}