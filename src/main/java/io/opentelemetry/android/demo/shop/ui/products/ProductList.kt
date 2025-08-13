package io.opentelemetry.android.demo.shop.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.opentelemetry.android.demo.shop.ui.currency.CurrencyViewModel
import io.opentelemetry.android.demo.shop.ui.currency.CurrencyToggle
import io.opentelemetry.android.demo.shop.ui.currency.CurrencyBottomSheet
import io.honeycomb.opentelemetry.android.compose.HoneycombInstrumentedComposable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductList(
    productListViewModel: ProductListViewModel = viewModel(),
    currencyViewModel: CurrencyViewModel,
    onProductClick: (String) -> Unit
) {
    HoneycombInstrumentedComposable(name = "ProductListScreen") {
        val uiState by productListViewModel.uiState.collectAsState()
        val selectedCurrency by currencyViewModel.selectedCurrency.collectAsState()
        val availableCurrencies by currencyViewModel.availableCurrencies.collectAsState()

        var showCurrencyBottomSheet by remember { mutableStateOf(false) }

        // Refresh products when currency changes or when navigating back to this screen
        LaunchedEffect(selectedCurrency) {
            productListViewModel.refreshProducts(selectedCurrency)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header with currency selection
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Products",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Button(
                        onClick = { productListViewModel.refreshProducts(selectedCurrency) },
                        enabled = !uiState.isLoading
                    ) {
                        Text("Refresh")
                    }
                }

                // Currency selection row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Currency:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    CurrencyToggle(
                        currentCurrency = selectedCurrency,
                        onCurrencySelected = { currency ->
                            currencyViewModel.selectCurrency(currency)
                        },
                        onShowAllCurrencies = {
                            showCurrencyBottomSheet = true
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    val errorMessage = uiState.errorMessage
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error loading products",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Button(
                            onClick = { productListViewModel.refreshProducts(selectedCurrency) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.products.size) { index ->
                            Row {
                                ProductCard(uiState.products[index], onProductClick = onProductClick)
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier.height(50.dp)
                            )
                        }
                    }
                }
            }

            // Currency bottom sheet
            if (showCurrencyBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showCurrencyBottomSheet = false }
                ) {
                    CurrencyBottomSheet(
                        availableCurrencies = availableCurrencies,
                        selectedCurrency = selectedCurrency,
                        onCurrencySelected = { currency ->
                            currencyViewModel.selectCurrency(currency)
                        },
                        onDismiss = { showCurrencyBottomSheet = false }
                    )
                }
            }
        }
    }
}
