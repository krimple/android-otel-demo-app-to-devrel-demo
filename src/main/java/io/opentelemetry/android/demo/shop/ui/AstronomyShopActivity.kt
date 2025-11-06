/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Modified from original at:
 * https://github.com/open-telemetry/opentelemetry-android/blob/main/demo-app/src/main/java/io/opentelemetry/android/demo/shop/ui/AstronomyShopActivity.kt
 * 
 * Changes: Modified to use Honeycomb SDK and add additional telemetry and features.
 */

package io.opentelemetry.android.demo.shop.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.compose.LocalOpenTelemetryRum
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.clients.CheckoutApiService
import io.opentelemetry.android.demo.shop.ui.cart.CartScreen
import io.opentelemetry.android.demo.shop.ui.cart.CartViewModel
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutConfirmationScreen
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutInfoViewModel
import io.opentelemetry.android.demo.shop.ui.cart.InfoScreen
import io.opentelemetry.android.demo.shop.ui.currency.CurrencyViewModel
import io.opentelemetry.android.demo.shop.ui.products.ProductDetails
import io.opentelemetry.android.demo.shop.ui.products.ProductList
import io.opentelemetry.android.demo.shop.ui.products.ProductListViewModel
import io.opentelemetry.android.demo.shop.ui.products.ProductDetailViewModel
import io.opentelemetry.android.demo.theme.DemoAppTheme
import io.opentelemetry.api.common.AttributeKey.doubleKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.honeycomb.opentelemetry.android.compose.HoneycombInstrumentedComposable
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement

class AstronomyShopActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalOpenTelemetryRum provides OtelDemoApplication.rum) {
                AstronomyShopScreen()
            }
        }
    }
}

@Composable
fun AstronomyShopScreen() {
    HoneycombInstrumentedComposable(name = "AstronomyShopScreen") {
        val context = LocalContext.current
        val currencyViewModel: CurrencyViewModel = viewModel()
        val checkoutApiService = remember { CheckoutApiService() }
        val astronomyShopNavController = rememberAstronomyShopNavController()
        val cartViewModel: CartViewModel = viewModel()
        val checkoutInfoViewModel: CheckoutInfoViewModel = viewModel()
        val productListViewModel: ProductListViewModel = viewModel()
        val productDetailViewModel: ProductDetailViewModel = viewModel()
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        var isPlacingOrder by remember { mutableStateOf(false) }

        DemoAppTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        BottomNavigationBar(
                            items = listOf(
                                BottomNavItem.Exit,
                                BottomNavItem.List,
                                BottomNavItem.Cart
                            ),
                            currentRoute = astronomyShopNavController.currentRoute,
                            onItemClicked = { route ->
                                astronomyShopNavController.navController.navigate(route) {
                                    popUpTo(astronomyShopNavController.navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            },
                            onExitClicked = {
                                (context as? Activity)?.finish()
                            }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = astronomyShopNavController.navController,
                        startDestination = MainDestinations.HOME_ROUTE,
                        Modifier.padding(innerPadding)
                    ) {
                        composable(BottomNavItem.List.route) {
                            ProductList(
                                productListViewModel = productListViewModel,
                                currencyViewModel = currencyViewModel,
                                onProductClick = { productId ->
                                    astronomyShopNavController.navigateToProductDetail(productId)
                                }
                            )
                        }
                        composable(BottomNavItem.Cart.route) {
                            CartScreen(
                                cartViewModel = cartViewModel,
                                onCheckoutClick = {
                                    astronomyShopNavController.navigateToCheckoutInfo()
                                },
                                onProductClick = { productId ->
                                    astronomyShopNavController.navigateToProductDetail(productId)
                                }
                            )
                        }
                        composable("${MainDestinations.PRODUCT_DETAIL_ROUTE}/{${MainDestinations.PRODUCT_ID_KEY}}") { backStackEntry ->
                            val productId =
                                backStackEntry.arguments?.getString(MainDestinations.PRODUCT_ID_KEY)
                            productId?.let { id ->
                                ProductDetails(
                                    productId = id,
                                    productDetailViewModel = productDetailViewModel,
                                    cartViewModel = cartViewModel,
                                    currencyViewModel = currencyViewModel,
                                    upPress = { astronomyShopNavController.upPress() },
                                    onProductClick = { productId ->
                                        astronomyShopNavController.navigateToProductDetail(productId)
                                    },
                                    navigateToCheckout = {
                                        astronomyShopNavController.navController.navigate(BottomNavItem.Cart.route)
                                    }
                                )
                            }
                        }
                        composable(MainDestinations.CHECKOUT_INFO_ROUTE) {
                            InfoScreen(
                                onPlaceOrderClick = {
                                    coroutineScope.launch {
                                        isPlacingOrder = true
                                        placeOrder(
                                            snackbarHostState = snackbarHostState,
                                            astronomyShopNavController = astronomyShopNavController,
                                            cartViewModel = cartViewModel,
                                            checkoutInfoViewModel = checkoutInfoViewModel,
                                            checkoutApiService = checkoutApiService,
                                            onComplete = { isPlacingOrder = false }
                                        )
                                    }
                                },
                                upPress = { astronomyShopNavController.upPress() },
                                checkoutInfoViewModel = checkoutInfoViewModel,
                                cartViewModel = cartViewModel,
                                currencyViewModel = currencyViewModel,
                                isPlacingOrder = isPlacingOrder
                            )
                        }
                        composable(MainDestinations.CHECKOUT_CONFIRMATION_ROUTE) {
                            CheckoutConfirmationScreen(
                                cartViewModel = cartViewModel,
                                checkoutInfoViewModel = checkoutInfoViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun placeOrder(
    snackbarHostState: SnackbarHostState,
    astronomyShopNavController: InstrumentedAstronomyShopNavController,
    cartViewModel: CartViewModel,
    checkoutInfoViewModel: CheckoutInfoViewModel,
    checkoutApiService: CheckoutApiService,
    onComplete: () -> Unit
){
    try {
        val checkoutResponse = checkoutApiService.placeOrder(checkoutInfoViewModel)
        checkoutInfoViewModel.updateCheckoutResponse(checkoutResponse)
        cartViewModel.clearCart()
        onComplete()
        astronomyShopNavController.navigateToCheckoutConfirmation()
    } catch (_: Exception) {
        onComplete()
        snackbarHostState.showSnackbar(
            message = "❌ Failed to place order. Please try again.",
            withDismissAction = true,
            duration = SnackbarDuration.Indefinite
        )
    }

}

// Removed generateOrderPlacedEvent function - consolidated into placeOrder span

