package io.opentelemetry.android.demo.shop.ui.products

import androidx.compose.foundation.layout.size
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            .height(140.dp)
            .wrapContentHeight()
            .padding(8.dp),
        onClick = { onProductClick(product.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .build(),
                contentDescription = product.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    fontFamily = gothamFont,
                    fontSize = if (isNarrow) 12.sp else 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black
                )
                
                Text(
                    text = product.description,
                    fontFamily = gothamFont,
                    fontSize = if (isNarrow) 10.sp else 12.sp,
                    color = Color.Gray,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = if (isNarrow) 14.sp else 16.sp
                )
                
                Text(
                    text = product.priceUsd.formatCurrency(),
                    fontFamily = gothamFont,
                    fontSize = if (isNarrow) 12.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF007AFF),
                    maxLines = 1
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF007AFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to cart",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "View details",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
