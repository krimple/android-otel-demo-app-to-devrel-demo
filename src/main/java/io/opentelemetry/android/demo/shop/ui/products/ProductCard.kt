package io.opentelemetry.android.demo.shop.ui.products

import androidx.compose.foundation.layout.size
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.opentelemetry.android.demo.shop.clients.ImageLoader
import io.opentelemetry.android.demo.gothamFont
import io.opentelemetry.android.demo.shop.model.Product

@Composable
fun ProductCard(
    product: Product,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isNarrow: Boolean = false
) {
    val context = LocalContext.current
    val imageUrl = ImageLoader.getImageUrl(product.picture)

    val cardColors = CardColors(
        containerColor = Color.White, contentColor = Color.Black,
        disabledContentColor = Color.Black, disabledContainerColor = Color.Black
    )
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = cardColors,
        modifier = modifier
            .fillMaxSize()
            .height(200.dp)
            .wrapContentHeight()
            .padding(20.dp),
        onClick = { onProductClick(product.id) }
    ) {
        Row(modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(120.dp)
                    )
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${product.name}\n\n${product.priceUsd.formatCurrency()}",
                        fontFamily = gothamFont,
                        style = TextStyle.Default.copy(textAlign = TextAlign.Right),
                        fontSize = if (isNarrow) 13.sp else 18.sp,
                        modifier = Modifier
                            .padding(start = 15.dp, top = 15.dp)
                            .fillMaxWidth()
                    )
                }

            }
        }
    }
}
