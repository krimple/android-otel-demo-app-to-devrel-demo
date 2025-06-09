/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context as OtelContext
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.honeycomb.opentelemetry.android.Honeycomb
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.clients.CheckoutApiService
import io.opentelemetry.android.demo.shop.clients.ProductApiService
import io.opentelemetry.android.demo.shop.clients.ProductCatalogClient
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.android.demo.shop.ui.cart.CartScreen
import io.opentelemetry.android.demo.shop.ui.cart.CartViewModel
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutConfirmationScreen
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutInfoViewModel
import io.opentelemetry.android.demo.shop.ui.cart.InfoScreen
import io.opentelemetry.android.demo.shop.ui.products.ProductDetails
import io.opentelemetry.android.demo.shop.ui.products.ProductList
import io.opentelemetry.android.demo.theme.DemoAppTheme
import io.opentelemetry.api.common.AttributeKey.doubleKey
import io.opentelemetry.api.common.AttributeKey.stringKey

class AstronomyShopActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstronomyShopScreen()
        }
    }
}

@Composable
fun AstronomyShopScreen() {
    val context = LocalContext.current
    val productCatalogClient = remember { ProductCatalogClient() }
    val productApiService = remember { ProductApiService() }

    val checkoutApiService = remember { CheckoutApiService() }
    var products by remember { mutableStateOf(emptyList<io.opentelemetry.android.demo.shop.model.Product>()) }
    val astronomyShopNavController = rememberAstronomyShopNavController()
    val cartViewModel: CartViewModel = viewModel()
    val checkoutInfoViewModel: CheckoutInfoViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    // TODO if we don't get a span builder, won't this cease to send data?
    // or is this a situation where the span builder would just return an
    // invalid span that won't do anything?
    LaunchedEffect(Unit) {
        val tracer = OtelDemoApplication.tracer("astronomy-shop")
        val span = tracer?.spanBuilder("loadProducts")
            ?.setAttribute("component", "astronomy_shop")
            ?.startSpan()
        try {
            span?.makeCurrent().use { scope ->
                launch(OtelContext.current().asContextElement()) {
                    try {
                        products = productApiService.fetchProducts()
                        span?.setAttribute("products.loaded", products.size.toLong())
                    } catch (e: Exception) {
                        products = ArrayList<Product>()
                    }
                }.join()
            }
        } finally {
            span?.end()
        }
    }

    DemoAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        items = listOf(BottomNavItem.Exit, BottomNavItem.List, BottomNavItem.Cart),
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
                        ProductList(products = products) { productId ->
                            astronomyShopNavController.navigateToProductDetail(productId)
                        }
                    }
                    composable(BottomNavItem.Cart.route) {
                        CartScreen(cartViewModel = cartViewModel, onCheckoutClick = {astronomyShopNavController.navigateToCheckoutInfo()},  onProductClick = { productId ->
                            astronomyShopNavController.navigateToProductDetail(productId)
                        })
                    }
                    composable("${MainDestinations.PRODUCT_DETAIL_ROUTE}/{${MainDestinations.PRODUCT_ID_KEY}}") { backStackEntry ->
                        val productId = backStackEntry.arguments?.getString(MainDestinations.PRODUCT_ID_KEY)
                        val product = products.find { it.id == productId }
                        product?.let { ProductDetails(
                            product = it,
                            cartViewModel,
                            upPress = {astronomyShopNavController.upPress()},
                            onProductClick = { productId ->
                                astronomyShopNavController.navigateToProductDetail(productId)
                            }
                        )
                        }
                    }
                    composable(MainDestinations.CHECKOUT_INFO_ROUTE) {
                        InfoScreen(
                            onPlaceOrderClick = {
                                coroutineScope.launch {
                                    val otelContext = OtelContext.current()
                                    launch(otelContext.asContextElement()) {
                                        instrumentedPlaceOrder(
                                            astronomyShopNavController = astronomyShopNavController,
                                            cartViewModel = cartViewModel,
                                            checkoutInfoViewModel = checkoutInfoViewModel,
                                            checkoutApiService = checkoutApiService
                                        )
                                    }
                                }
                            },
                            upPress = {astronomyShopNavController.upPress()},
                            checkoutInfoViewModel = checkoutInfoViewModel
                        )
                    }
                    composable(MainDestinations.CHECKOUT_CONFIRMATION_ROUTE){
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

private suspend fun instrumentedPlaceOrder(
    astronomyShopNavController: InstrumentedAstronomyShopNavController,
    cartViewModel: CartViewModel,
    checkoutInfoViewModel: CheckoutInfoViewModel,
    checkoutApiService: CheckoutApiService
){
    try {
        val checkoutResponse = checkoutApiService.placeOrder(cartViewModel, checkoutInfoViewModel)
        checkoutInfoViewModel.updateCheckoutResponse(checkoutResponse)
        generateOrderPlacedEvent(cartViewModel, checkoutInfoViewModel)
        cartViewModel.clearCart()
        astronomyShopNavController.navigateToCheckoutConfirmation()
    } catch (e: Exception) {
        // TODO: Handle error properly - for now just log and proceed with original flow

        OtelDemoApplication.rum?.let { Honeycomb.logException(it, e) }
        e.printStackTrace()
        generateOrderPlacedEvent(cartViewModel, checkoutInfoViewModel)
        cartViewModel.clearCart()
        astronomyShopNavController.navigateToCheckoutConfirmation()
    }
}

private fun generateOrderPlacedEvent(
    cartViewModel: CartViewModel,
    checkoutInfoViewModel: CheckoutInfoViewModel
) {
    // Adding span instead of log entry
    OtelDemoApplication.tracer("otel.demo.app")?.spanBuilder("orderPlaced")
        ?.setAttribute(doubleKey("order.total.value"), cartViewModel.getTotalPrice())
        ?.setAttribute(stringKey("buyer.state"), checkoutInfoViewModel.shippingInfo.state)
        ?.startSpan()
        ?.end()

    val eventBuilder = OtelDemoApplication.eventBuilder("otel.demo.app", "order.placed")
    eventBuilder
        ?.setAttribute(doubleKey("order.total.value"), cartViewModel.getTotalPrice())
        ?.setAttribute(stringKey("buyer.state"), checkoutInfoViewModel.shippingInfo.state)
        ?.emit()
}

