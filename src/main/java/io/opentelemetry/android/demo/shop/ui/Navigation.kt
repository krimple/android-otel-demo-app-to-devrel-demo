package io.opentelemetry.android.demo.shop.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.*
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.common.AttributeKey.stringKey


sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    data object Exit : BottomNavItem("home", Icons.AutoMirrored.Filled.ExitToApp, "Exit")
    data object List : BottomNavItem("prod-list", Icons.AutoMirrored.Filled.List, "List")
    data object Cart : BottomNavItem("cart", Icons.Filled.ShoppingCart, "Cart")
}

object MainDestinations {
    const val HOME_ROUTE = "prod-list"
    const val PRODUCT_DETAIL_ROUTE = "product"
    const val PRODUCT_ID_KEY = "productId"
    const val CHECKOUT_INFO_ROUTE = "checkout-info"
    const val CHECKOUT_CONFIRMATION_ROUTE = "checkout-confirmation"
}

@Composable
fun rememberAstronomyShopNavController(navController: NavHostController = rememberNavController())
        : InstrumentedAstronomyShopNavController = remember(navController)
{
    InstrumentedAstronomyShopNavController(AstronomyShopNavController(navController))
}

@Stable
class AstronomyShopNavController(
    val navController: NavHostController,
) {
    val currentRoute: String?
        get() = navController.currentDestination?.route

    fun upPress() {
        navController.navigateUp()
    }

    fun navigateToProductDetail(productId: String) {
        navController.navigate("${MainDestinations.PRODUCT_DETAIL_ROUTE}/$productId")
    }

    fun navigateToCheckoutInfo(){
        navController.navigate(MainDestinations.CHECKOUT_INFO_ROUTE)
    }

    fun navigateToCheckoutConfirmation(){
        navController.navigate(MainDestinations.CHECKOUT_CONFIRMATION_ROUTE)
    }
}

class InstrumentedAstronomyShopNavController(
    private val delegate : AstronomyShopNavController
){
    val navController: NavHostController
        get() = delegate.navController

    val currentRoute: String?
        get() = delegate.currentRoute

    fun upPress() {
        delegate.upPress()
    }

    fun navigateToProductDetail(productId: String) {
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("navigation")
            ?.setAttribute("app.screen.name", "product_detail")
            ?.setAttribute("app.operation.type", "navigate")
            ?.setAttribute("app.product.id", productId)
            ?.setAttribute("app.interaction.type", "tap")
            ?.startSpan()
        
        delegate.navigateToProductDetail(productId)
        span?.end()
    }

    fun navigateToCheckoutInfo() {
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("navigation")
            ?.setAttribute("app.screen.name", "checkout_info")
            ?.setAttribute("app.operation.type", "navigate")
            ?.setAttribute("app.interaction.type", "tap")
            ?.startSpan()
        
        delegate.navigateToCheckoutInfo()
        span?.end()
    }

    fun navigateToCheckoutConfirmation() {
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("navigation")
            ?.setAttribute("app.screen.name", "checkout_confirmation")
            ?.setAttribute("app.operation.type", "navigate")
            ?.setAttribute("app.interaction.type", "tap")
            ?.startSpan()
        
        delegate.navigateToCheckoutConfirmation()
        span?.end()
    }
}

@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClicked: (String) -> Unit,
    onExitClicked: () -> Unit
) {
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (item.route == BottomNavItem.Exit.route) {
                        onExitClicked()
                    } else {
                        onItemClicked(item.route)
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}