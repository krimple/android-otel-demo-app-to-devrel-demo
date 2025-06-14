package io.opentelemetry.android.demo.shop.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.opentelemetry.android.demo.shop.ui.components.UpPressButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import io.opentelemetry.android.demo.shop.ui.currency.CurrencyViewModel

@Composable
fun InfoField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    )
}

@Composable
fun InfoFieldsSection(
    fields: List<Triple<String, String, (String) -> Unit>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        fields.forEach { (label, value, onValueChange) ->
            InfoField(
                value = value,
                onValueChange = onValueChange,
                label = label,
                keyboardType = when (label) {
                    "Zip Code", "Credit Card Number", "Month", "Year", "CVV" -> KeyboardType.Number
                    else -> KeyboardType.Text
                },
                isPassword = label == "CVV"
            )
        }
    }
}

@Composable
fun InfoScreen(
    onPlaceOrderClick: () -> Unit,
    upPress: () -> Unit,
    checkoutInfoViewModel: CheckoutInfoViewModel,
    cartViewModel: CartViewModel,
    currencyViewModel: CurrencyViewModel
) {
    val selectedCurrency by currencyViewModel.selectedCurrency.collectAsState()
    
    val shippingInfo = checkoutInfoViewModel.shippingInfo
    val paymentInfo = checkoutInfoViewModel.paymentInfo
    val shippingCost = checkoutInfoViewModel.shippingCost
    val isCalculatingShipping = checkoutInfoViewModel.isCalculatingShipping
    val shippingCalculationError = checkoutInfoViewModel.shippingCalculationError

    val focusManager = LocalFocusManager.current
    val canProceed = checkoutInfoViewModel.canProceedToCheckout()
    
    // Calculate shipping when shipping info is complete
    LaunchedEffect(shippingInfo, selectedCurrency) {
        checkoutInfoViewModel.calculateShippingCostIfNeeded(cartViewModel, selectedCurrency)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clickable { focusManager.clearFocus() }
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(title = "Shipping Address")

            InfoFieldsSection(
                fields = listOf(
                    Triple("E-mail Address", shippingInfo.email) { checkoutInfoViewModel.updateShippingInfo(shippingInfo.copy(email = it)) },
                    Triple("Street Address", shippingInfo.streetAddress) { checkoutInfoViewModel.updateShippingInfo(shippingInfo.copy(streetAddress = it)) },
                    Triple("Zip Code", shippingInfo.zipCode) { checkoutInfoViewModel.updateShippingInfo(shippingInfo.copy(zipCode = it)) },
                    Triple("City", shippingInfo.city) { checkoutInfoViewModel.updateShippingInfo(shippingInfo.copy(city = it)) },
                    Triple("State", shippingInfo.state) { checkoutInfoViewModel.updateShippingInfo(shippingInfo.copy(state = it)) },
                    Triple("Country", shippingInfo.country) { checkoutInfoViewModel.updateShippingInfo(shippingInfo.copy(country = it)) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Shipping Cost Section
            if (shippingInfo.isComplete()) {
                SectionHeader(title = "Order Summary")
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Cart total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:")
                            Text(cartViewModel.getTotalPriceFormatted(selectedCurrency))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Shipping cost
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Shipping:")
                            when {
                                isCalculatingShipping -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                shippingCalculationError != null -> {
                                    Text(
                                        text = "Error calculating",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                shippingCost != null -> {
                                    Text(shippingCost.formatCurrency())
                                }
                                else -> {
                                    Text("Calculating...")
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = formatTotalCost(cartViewModel, checkoutInfoViewModel, selectedCurrency),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            SectionHeader(title = "Payment Method")

            InfoFieldsSection(
                fields = listOf(
                    Triple("Credit Card Number", paymentInfo.creditCardNumber) { checkoutInfoViewModel.updatePaymentInfo(paymentInfo.copy(creditCardNumber = it)) },
                    Triple("Month", paymentInfo.expiryMonth) { checkoutInfoViewModel.updatePaymentInfo(paymentInfo.copy(expiryMonth = it)) },
                    Triple("Year", paymentInfo.expiryYear) { checkoutInfoViewModel.updatePaymentInfo(paymentInfo.copy(expiryYear = it)) },
                    Triple("CVV", paymentInfo.cvv) { checkoutInfoViewModel.updatePaymentInfo(paymentInfo.copy(cvv = it)) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onPlaceOrderClick()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canProceed
            ) {
                Text("Place Order")
            }
        }

        UpPressButton(
            upPress = upPress,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )
    }
}

// Helper functions for formatting prices
private fun formatTotalCost(
    cartViewModel: CartViewModel,
    checkoutInfoViewModel: CheckoutInfoViewModel,
    currencyCode: String
): String {
    val cartTotal = cartViewModel.getTotalPrice()
    val shippingCost = checkoutInfoViewModel.shippingCost?.toDouble() ?: 0.0
    val total = cartTotal + shippingCost
    
    // Create a Money object to use the formatCurrency function
    val totalMoney = io.opentelemetry.android.demo.shop.model.Money(
        currencyCode = currencyCode,
        units = total.toLong(),
        nanos = ((total - total.toLong()) * 1_000_000_000).toLong()
    )
    
    return totalMoney.formatCurrency()
}