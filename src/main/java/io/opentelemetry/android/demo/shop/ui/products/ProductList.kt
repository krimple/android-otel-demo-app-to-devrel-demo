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
import io.opentelemetry.android.demo.shop.model.Product

@Composable
fun ProductList(
    productListViewModel: ProductListViewModel = viewModel(),
    onProductClick: (String) -> Unit
) {
    val uiState by productListViewModel.uiState.collectAsState()

    // Refresh products when navigating back to this screen
    LaunchedEffect(Unit) {
        productListViewModel.refreshProducts()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Pull-to-refresh header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Products",
                style = MaterialTheme.typography.headlineMedium
            )

            Button(
                onClick = { productListViewModel.refreshProducts() },
                enabled = !uiState.isLoading
            ) {
                Text("Refresh")
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
                        onClick = { productListViewModel.refreshProducts() },
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
    }
}
