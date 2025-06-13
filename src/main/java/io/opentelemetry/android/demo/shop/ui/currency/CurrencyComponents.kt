package io.opentelemetry.android.demo.shop.ui.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyToggle(
    currentCurrency: String,
    popularCurrencies: List<String> = listOf("USD", "EUR", "GBP", "JPY"),
    onCurrencySelected: (String) -> Unit,
    onShowAllCurrencies: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Current currency button
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentCurrency,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select currency",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Popular currencies
            popularCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = "$currency ${getCurrencySymbol(currency)}",
                            fontWeight = if (currency == currentCurrency) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    }
                )
            }
            
            HorizontalDivider()
            
            // More currencies option
            DropdownMenuItem(
                text = { Text("More currencies...") },
                onClick = {
                    onShowAllCurrencies()
                    expanded = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyBottomSheet(
    availableCurrencies: List<String>,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(availableCurrencies, searchQuery) {
        if (searchQuery.isEmpty()) {
            availableCurrencies
        } else {
            availableCurrencies.filter { currency ->
                currency.contains(searchQuery, ignoreCase = true) ||
                getCurrencyName(currency).contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Select Currency",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search currencies") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        // Currency list
        LazyColumn {
            items(filteredCurrencies) { currency ->
                CurrencyItem(
                    currency = currency,
                    isSelected = currency == selectedCurrency,
                    onClick = {
                        onCurrencySelected(currency)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun CurrencyItem(
    currency: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .then(
                if (isSelected) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    ).padding(8.dp)
                } else {
                    Modifier.padding(8.dp)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Currency code
        Text(
            text = currency,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(0.3f)
        )

        // Currency symbol
        Text(
            text = getCurrencySymbol(currency),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(0.2f)
        )

        // Currency name
        Text(
            text = getCurrencyName(currency),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.5f)
        )
    }
}

// Helper functions for currency display
private fun getCurrencySymbol(currencyCode: String): String {
    return when (currencyCode) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        "CAD" -> "C$"
        "AUD" -> "A$"
        "CHF" -> "Fr"
        "CNY" -> "¥"
        "INR" -> "₹"
        "KRW" -> "₩"
        "RUB" -> "₽"
        "BRL" -> "R$"
        "MXN" -> "MX$"
        "ZAR" -> "R"
        "SGD" -> "S$"
        "HKD" -> "HK$"
        "NOK" -> "kr"
        "SEK" -> "kr"
        "DKK" -> "kr"
        "PLN" -> "zł"
        "CZK" -> "Kč"
        "HUF" -> "Ft"
        "RON" -> "lei"
        "BGN" -> "лв"
        "HRK" -> "kn"
        "ISK" -> "kr"
        "TRY" -> "₺"
        "ILS" -> "₪"
        "PHP" -> "₱"
        "THB" -> "฿"
        "MYR" -> "RM"
        "IDR" -> "Rp"
        "NZD" -> "NZ$"
        else -> currencyCode
    }
}

private fun getCurrencyName(currencyCode: String): String {
    return when (currencyCode) {
        "USD" -> "US Dollar"
        "EUR" -> "Euro"
        "GBP" -> "British Pound"
        "JPY" -> "Japanese Yen"
        "CAD" -> "Canadian Dollar"
        "AUD" -> "Australian Dollar"
        "CHF" -> "Swiss Franc"
        "CNY" -> "Chinese Yuan"
        "INR" -> "Indian Rupee"
        "KRW" -> "South Korean Won"
        "RUB" -> "Russian Ruble"
        "BRL" -> "Brazilian Real"
        "MXN" -> "Mexican Peso"
        "ZAR" -> "South African Rand"
        "SGD" -> "Singapore Dollar"
        "HKD" -> "Hong Kong Dollar"
        "NOK" -> "Norwegian Krone"
        "SEK" -> "Swedish Krona"
        "DKK" -> "Danish Krone"
        "PLN" -> "Polish Złoty"
        "CZK" -> "Czech Koruna"
        "HUF" -> "Hungarian Forint"
        "RON" -> "Romanian Leu"
        "BGN" -> "Bulgarian Lev"
        "HRK" -> "Croatian Kuna"
        "ISK" -> "Icelandic Krona"
        "TRY" -> "Turkish Lira"
        "ILS" -> "Israeli Shekel"
        "PHP" -> "Philippine Peso"
        "THB" -> "Thai Baht"
        "MYR" -> "Malaysian Ringgit"
        "IDR" -> "Indonesian Rupiah"
        "NZD" -> "New Zealand Dollar"
        else -> currencyCode
    }
}