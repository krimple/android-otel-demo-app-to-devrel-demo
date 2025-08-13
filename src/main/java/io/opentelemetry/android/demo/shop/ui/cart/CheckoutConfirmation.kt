package io.opentelemetry.android.demo.shop.ui.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.opentelemetry.android.demo.shop.ui.products.ProductCard
import io.honeycomb.opentelemetry.android.compose.HoneycombInstrumentedComposable
import java.util.Locale

@Composable
fun CheckoutConfirmationScreen(
    cartViewModel: CartViewModel,
    checkoutInfoViewModel: CheckoutInfoViewModel
) {
    HoneycombInstrumentedComposable(name = "CheckoutConfirmationScreen") {

    val shippingInfo = checkoutInfoViewModel.shippingInfo
    val checkoutResponse = checkoutInfoViewModel.checkoutResponse

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Your order is complete!",
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Text(
            text = "We've sent a confirmation email to ${shippingInfo.email}.",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Display API response data if available
        checkoutResponse?.let { response ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Order Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(text = "Order ID: ${response.orderId}")
                    Text(text = "Tracking ID: ${response.shippingTrackingId}")
                    Text(text = "Shipping Cost: ${response.shippingCost.formatCurrency()}")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Display ordered items from API response
            Text(
                text = "Ordered Items",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            response.items.forEach { checkoutItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = checkoutItem.item.product.name,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "Quantity: ${checkoutItem.item.quantity}")
                            Text(text = "Cost: ${checkoutItem.cost.formatCurrency()}")
                        }
                    }
                }
            }
            
            Text(
                text = "Total Cost: ${response.items.sumOf { it.cost.toDouble() }.let { "\$${"%.2f".format(it)}" }}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        } ?: run {
            // Fallback: Display cart items if no API response
            val cartItems = cartViewModel.cartItems.collectAsState().value
            cartItems.forEach { cartItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductCard(
                        product = cartItem.product,
                        onProductClick = {},
                        modifier = Modifier
                            .width(300.dp)
                            .height(170.dp),
                        isNarrow = true
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Quantity: ${cartItem.quantity}",
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                        )

                        Text(
                            text = "Total: \$${String.format(Locale.US, "%.2f", cartItem.totalPrice())}",
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(8.dp),
                        )
                    }
                }
            }

            Text(
                text = "Total Price: \$${String.format(Locale.US, "%.2f", cartViewModel.getTotalPrice())}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                textAlign = TextAlign.End
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Shipping Address",
                    fontWeight = FontWeight.Bold
                )
                
                // Use shipping address from API response if available, otherwise use form data
                val addressToShow = checkoutResponse?.shippingAddress ?: run {
                    io.opentelemetry.android.demo.shop.model.CheckoutAddress(
                        streetAddress = shippingInfo.streetAddress,
                        city = shippingInfo.city,
                        state = shippingInfo.state,
                        country = shippingInfo.country,
                        zipCode = shippingInfo.zipCode
                    )
                }
                
                Text(text = "Street: ${addressToShow.streetAddress}")
                Text(text = "City: ${addressToShow.city}")
                Text(text = "State: ${addressToShow.state}")
                Text(text = "Zip Code: ${addressToShow.zipCode}")
                Text(text = "Country: ${addressToShow.country}")
            }
        }
    }
    }
}
