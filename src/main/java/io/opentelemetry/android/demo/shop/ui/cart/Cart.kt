package io.opentelemetry.android.demo.shop.ui.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.clients.ProductApiService
import io.opentelemetry.android.demo.shop.clients.ProductCatalogClient
import io.opentelemetry.android.demo.shop.clients.RecommendationService
import io.opentelemetry.android.demo.shop.ui.products.ProductCard
import io.opentelemetry.android.demo.shop.ui.products.RecommendedSection
import io.opentelemetry.api.common.AttributeKey.doubleKey
import java.util.Locale

@Composable
fun CartScreen(
    cartViewModel: CartViewModel = viewModel(),
    onCheckoutClick: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val context = LocalContext.current
    val productCatalogClient = ProductCatalogClient()
    val productApiService = ProductApiService()
    val recommendationService = remember { RecommendationService(productCatalogClient, productApiService, cartViewModel) }
    val uiState by cartViewModel.uiState.collectAsState()
    val cartItems = uiState.cartItems
    val isCartEmpty = cartItems.isEmpty()
    val isLoading = uiState.isLoading
    val errorMessage = uiState.errorMessage
    val recommendedProducts = remember { recommendationService.getRecommendedProducts() }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Error message
        errorMessage?.let { error ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "Error: $error",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                OutlinedButton(
                    onClick = { clearCart(cartViewModel) },
                    enabled = !isLoading,
                    modifier = Modifier
                ) {
                    Text("Empty Cart", color = Color.Red)
                }
            }
        }

        items(cartItems.size) { index ->
            ProductCard(product = cartItems[index].product, onProductClick = {})
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Quantity: ${cartItems[index].quantity}",
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total: \$${String.format(Locale.US, "%.2f", cartItems[index].totalPrice())}",
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total Price: \$${String.format(Locale.US, "%.2f", cartViewModel.getTotalPrice())}",
                modifier = Modifier.padding(16.dp)
            )

            Button(
                onClick = onCheckoutClick,
                enabled = !isCartEmpty && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCartEmpty || isLoading) Color.Gray else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Checkout")
            }

            Spacer(modifier = Modifier.height(32.dp))
            RecommendedSection(recommendedProducts = recommendedProducts, onProductClick = onProductClick)
        }
    }
}

private fun clearCart(cartViewModel: CartViewModel) {
    val tracer = OtelDemoApplication.getTracer()
    val span = tracer?.spanBuilder("cart.clear")
        ?.setAttribute("app.cart.total.cost", cartViewModel.getTotalPrice())
        ?.setAttribute("app.cart.items.count", cartViewModel.uiState.value.cartItems.size.toLong())
        ?.setAttribute("app.operation.type", "clear_cart")
        ?.setAttribute("app.screen.name", "cart")
        ?.setAttribute("app.interaction.type", "tap")
        ?.startSpan()
    
    cartViewModel.clearCart() // Uses default USD currency
    span?.end()
}
