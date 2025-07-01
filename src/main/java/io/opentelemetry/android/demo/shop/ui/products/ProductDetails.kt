package io.opentelemetry.android.demo.shop.ui.products

import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import io.opentelemetry.android.demo.shop.clients.ImageLoader
import io.opentelemetry.android.demo.gothamFont
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.android.demo.shop.ui.components.QuantityChooser
import io.opentelemetry.android.demo.shop.ui.cart.CartViewModel
import io.opentelemetry.android.demo.shop.ui.components.UpPressButton
import io.opentelemetry.android.demo.shop.clients.ProductApiService
import io.opentelemetry.android.demo.shop.clients.ProductCatalogClient
import io.opentelemetry.android.demo.shop.clients.RecommendationService
import io.opentelemetry.android.demo.shop.ui.components.SlowCometAnimation
import io.opentelemetry.android.demo.shop.ui.components.ConfirmPopup
import io.opentelemetry.android.demo.shop.ui.currency.CurrencyViewModel
import io.opentelemetry.android.demo.shop.ui.currency.CurrencyToggle
import io.opentelemetry.android.demo.shop.ui.currency.CurrencyBottomSheet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetails(
    productId: String,
    productDetailViewModel: ProductDetailViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel(),
    currencyViewModel: CurrencyViewModel,
    onProductClick: (String) -> Unit,
    upPress: () -> Unit
) {
    val context = LocalContext.current
    var quantity by remember { mutableIntStateOf(1) }
    var slowRender by remember { mutableStateOf(false) }
    var showCurrencyBottomSheet by remember { mutableStateOf(false) }

    val uiState by productDetailViewModel.uiState.collectAsState()
    val selectedCurrency by currencyViewModel.selectedCurrency.collectAsState()
    val availableCurrencies by currencyViewModel.availableCurrencies.collectAsState()

    // Load product when productId or currency changes
    LaunchedEffect(productId, selectedCurrency) {
        productDetailViewModel.loadProduct(productId, selectedCurrency)
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
                        text = "Error loading product",
                        fontFamily = gothamFont,
                        fontSize = 20.sp,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Unknown error",
                        fontFamily = gothamFont,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { productDetailViewModel.refreshProduct(productId, selectedCurrency) }
                    ) {
                        Text("Retry")
                    }
                }
            }

            uiState.product != null -> {
                val product = uiState.product!!
                val imageUrl = ImageLoader.getImageUrl(product.picture)

                // Initialize recommendation service with the loaded product
                val productCatalogClient = ProductCatalogClient()
                val productApiService = ProductApiService()
                val recommendationService = remember(product) {
                    RecommendationService(productCatalogClient, productApiService, cartViewModel)
                }
                val recommendedProducts = remember(product) {
                    recommendationService.getRecommendedProducts(product)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Product name and currency toggle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = product.name,
                            fontFamily = gothamFont,
                            fontSize = 24.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = product.description,
                        color = Color.Gray,
                        textAlign = TextAlign.Justify,
                        fontFamily = gothamFont,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = product.priceUsd.formatCurrency(),
                        fontFamily = gothamFont,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    QuantityChooser(quantity = quantity, onQuantityChange = { quantity = it })
                    Spacer(modifier = Modifier.height(16.dp))
                    AddToCartButton(
                        cartViewModel = cartViewModel,
                        product = product,
                        quantity = quantity,
                        currencyCode = selectedCurrency,
                        onSlowRenderChange = { slowRender = it }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    RecommendedSection(recommendedProducts = recommendedProducts, onProductClick = onProductClick)
                }
            }
        }

        UpPressButton(
            upPress = upPress,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )
        if (slowRender) {
            SlowCometAnimation(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            )
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

@Composable
fun AddToCartButton(
    cartViewModel: CartViewModel,
    product: Product,
    quantity: Int,
    currencyCode: String,
    onSlowRenderChange: (Boolean) -> Unit
) {
    var showCrashPopup by remember { mutableStateOf(false) }
    var showANRPopup by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (product.id == "OLJCESPC7Z") {
                if (quantity == 10) showCrashPopup = true
                if (quantity == 9) showANRPopup = true
            } else {
                if (product.id == "HQTGWGPNH4") {
                    onSlowRenderChange(true)
                }
            }
            cartViewModel.addProduct(product, quantity, currencyCode)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(text = "Add to Cart")
    }

    if (showCrashPopup) {
        ConfirmPopup(
            text = "This will crash the app",
            onConfirm = {
                multiThreadCrashing()
            },
            onDismiss = {
                showCrashPopup = false
            }
        )
    }
    if (showANRPopup) {
        ConfirmPopup(
            text = "This will freeze the app",
            onConfirm = {
                appFreezing()
            },
            onDismiss = {
                showCrashPopup = false
            }
        )
    }
}

fun multiThreadCrashing(numThreads : Int = 4) {
    val latch = CountDownLatch(1)

    for (i in 0..numThreads) {
        val thread = Thread {
            try {
                if (latch.await(10, TimeUnit.SECONDS)) {
                    throw IllegalStateException("Failure from thread ${Thread.currentThread().name}")
                }
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
        thread.name = "crash-thread-$i"
        thread.start()
    }

    try {
        Thread.sleep(100)
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        return
    }
    latch.countDown()
}

fun appFreezing(){
    try {
        for (i in 0 .. 20) {
            Thread.sleep(1_000)
        }
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }

}
