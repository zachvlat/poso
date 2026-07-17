package com.zachvlat.howmuchgr.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zachvlat.howmuchgr.data.CartRepository
import com.zachvlat.howmuchgr.data.WishlistRepository
import com.zachvlat.howmuchgr.network.DailyPriceEntry
import com.zachvlat.howmuchgr.network.Product
import com.zachvlat.howmuchgr.network.RetailerPrice
import com.zachvlat.howmuchgr.ui.viewmodel.SearchViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    initialQuery: String? = null,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialQuery) {
        val q = initialQuery
        if (q != null) {
            viewModel.setQueryFromWishlist(q)
        }
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Αναζήτηση προϊόντος...") },
                singleLine = true,
                trailingIcon = {
                    if (state.products.isNotEmpty()) {
                        IconButton(onClick = viewModel::toggleWishlist) {
                            Icon(
                                imageVector = if (state.isCurrentQuerySaved)
                                    Icons.Default.Favorite
                                else
                                    Icons.Default.FavoriteBorder,
                                contentDescription = if (state.isCurrentQuerySaved)
                                    "Αφαίρεση από αγαπημένα"
                                else
                                    "Προσθήκη στα αγαπημένα",
                                tint = if (state.isCurrentQuerySaved)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                state.products.isNotEmpty() -> {
                    ProductList(
                        products = state.products,
                        onProductClick = viewModel::onProductClick
                    )
                }

                state.query.isNotBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Δεν βρέθηκαν προϊόντα")
                    }
                }
            }
        }

        state.selectedProduct?.let { product ->
            ProductDetailBottomSheet(
                product = product,
                isLoading = state.isDetailLoading,
                onDismiss = viewModel::dismissProductDetail
            )
        }
    }
}

@Composable
fun ProductList(
    products: List<Product>,
    onProductClick: (Product) -> Unit = {},
    cartToggle: Boolean = false
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product) },
                cartToggle = cartToggle
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    cartToggle: Boolean = false
) {
    var isWishlisted by remember { mutableStateOf(WishlistRepository.isSaved(product.name)) }
    val context = LocalContext.current
    val imageUrl = "https://api.posokanei.gov.gr/images/product/${product.id}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name.trim(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    var isInCart by remember { mutableStateOf(CartRepository.isInCart(product.id)) }
                    IconButton(onClick = {
                        if (cartToggle) {
                            if (isInCart) {
                                CartRepository.removeProduct(product.id)
                            } else {
                                CartRepository.addProduct(product.id)
                            }
                            isInCart = !isInCart
                        } else {
                            CartRepository.addProduct(product.id)
                            isInCart = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isInCart)
                                Icons.Default.RemoveShoppingCart
                            else
                                Icons.Default.AddShoppingCart,
                            contentDescription = if (isInCart)
                                "Αφαίρεση από καλάθι"
                            else
                                "Προσθήκη στο καλάθι",
                            tint = if (isInCart)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = {
                        if (isWishlisted) {
                            WishlistRepository.removeQuery(product.name)
                        } else {
                            WishlistRepository.addQuery(product.name)
                        }
                        isWishlisted = !isWishlisted
                    }) {
                        Icon(
                            imageVector = if (isWishlisted)
                                Icons.Default.Favorite
                            else
                                Icons.Default.FavoriteBorder,
                            contentDescription = if (isWishlisted)
                                "Αφαίρεση από αγαπημένα"
                            else
                                "Προσθήκη στα αγαπημένα",
                            tint = if (isWishlisted)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!product.brand.isNullOrBlank()) {
                Text(
                    text = product.brand.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!product.category.isNullOrBlank()) {
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            product.priceStats?.let { stats ->
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Από",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "%.2f €".format(stats.minPrice),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Έως",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "%.2f €".format(stats.maxPrice),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailBottomSheet(
    product: Product,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val context = LocalContext.current
            val imageUrl = "https://api.posokanei.gov.gr/images/product/${product.id}"

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = product.name.trim(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (!product.brand.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!product.category.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!product.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            product.priceStats?.let { stats ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Τιμές",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Ελάχιστη", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "%.2f €".format(stats.minPrice),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Μέση", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "%.2f €".format(stats.avgPrice),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Μέγιστη", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "%.2f €".format(stats.maxPrice),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                if (stats.minUnitPrice != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ελάχιστη τιμή μονάδας: %.2f €/%s".format(
                            stats.minUnitPrice,
                            product.unit ?: ""
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (product.retailerPrices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Τιμές ανά κατάστημα",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                product.retailerPrices
                    .sortedBy { it.price }
                    .forEach { rp ->
                        RetailerPriceRow(rp)
                    }
            }

            product.history?.let { history ->
                if (history.dailyPrices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ιστορικό τιμών",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    history.dateRange?.let { range ->
                        val rangeFormat = SimpleDateFormat("d MMM yyyy", Locale("el", "GR"))
                        val startFormatted = range.start?.let {
                            try { rangeFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)!!) } catch (_: Exception) { it }
                        } ?: ""
                        val endFormatted = range.end?.let {
                            try { rangeFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)!!) } catch (_: Exception) { it }
                        } ?: ""
                        Text(
                            text = "$startFormatted — $endFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var selectedRetailer by remember { mutableIntStateOf(0) }
                    val retailerKeys = history.dailyPrices.keys.toList()

                    PriceHistoryChart(
                        dailyPrices = history.dailyPrices,
                        selectedRetailerKey = retailerKeys.getOrNull(selectedRetailer),
                        selectedIndex = selectedRetailer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(retailerKeys.size) { index ->
                            val key = retailerKeys[index]
                            val color = RetailerColors[index % RetailerColors.size]
                            val displayName = product.retailerPrices
                                .find { it.retailer == key }
                                ?.retailerDisplayName ?: key
                            val isSelected = selectedRetailer == index
                            Card(
                                modifier = Modifier.clickable { selectedRetailer = index },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        color.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = displayName,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (retailerKeys.isNotEmpty()) {
                        val selectedKey = retailerKeys[selectedRetailer]
                        val displayName = product.retailerPrices
                            .find { it.retailer == selectedKey }
                            ?.retailerDisplayName ?: selectedKey

                        RetailerHistorySection(
                            retailerName = displayName,
                            entries = history.dailyPrices[selectedKey] ?: emptyList()
                        )
                    }
                }
            }
        }
    }
}

private val RetailerColors = listOf(
    Color(0xFF6750A4),
    Color(0xFF006B3F),
    Color(0xFFB3261E),
    Color(0xFF7D5260),
    Color(0xFF0061A4),
    Color(0xFF904D00),
    Color(0xFF4A6267),
    Color(0xFF5D5F5F)
)

@Composable
fun PriceHistoryChart(
    dailyPrices: Map<String, List<DailyPriceEntry>>,
    selectedRetailerKey: String?,
    selectedIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    val displayFormat = remember { SimpleDateFormat("d MMM", Locale("el", "GR")) }

    val allDates = remember(dailyPrices) {
        dailyPrices.values.flatten()
            .mapNotNull { it.date }
            .distinct()
            .sorted()
    }

    val selectedEntries = remember(dailyPrices, selectedRetailerKey) {
        selectedRetailerKey?.let { dailyPrices[it] } ?: emptyList()
    }

    if (allDates.isEmpty() || selectedEntries.isEmpty()) return

    val allPrices = remember(selectedEntries) {
        selectedEntries.mapNotNull { it.price }
    }

    if (allPrices.isEmpty()) return

    val minPrice = allPrices.min()
    val maxPrice = allPrices.max()
    val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01)

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val leftPadding = 48f
        val bottomPadding = 36f
        val topPadding = 12f
        val rightPadding = 12f

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - bottomPadding - topPadding

        val gridLines = 4
        for (i in 0..gridLines) {
            val y = topPadding + chartHeight * (1f - i.toFloat() / gridLines)
            val price = minPrice + priceRange * i.toFloat() / gridLines

            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1f
            )

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "%.2f".format(price),
                    leftPadding - 8f,
                    y + 4f,
                    android.graphics.Paint().apply {
                        textSize = 22f
                        color = android.graphics.Color.GRAY
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }
        }

        if (allDates.size >= 2) {
            val step = (allDates.size - 1) / 4.coerceAtMost(allDates.size - 1)
            for (i in allDates.indices step step.coerceAtLeast(1)) {
                val x = leftPadding + chartWidth * i.toFloat() / (allDates.size - 1)
                val date = try {
                    dateFormat.parse(allDates[i])
                } catch (_: Exception) { null }
                val label = date?.let { displayFormat.format(it) } ?: allDates[i].takeLast(5)

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        label,
                        x,
                        size.height - 8f,
                        android.graphics.Paint().apply {
                            textSize = 20f
                            color = android.graphics.Color.GRAY
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }

        val sorted = selectedEntries.sortedBy { it.date }
        if (sorted.size >= 2) {
            val color = RetailerColors[selectedIndex % RetailerColors.size]
            val path = Path()
            sorted.forEachIndexed { i, entry ->
                val dateIdx = allDates.indexOf(entry.date).coerceAtLeast(0)
                val x = leftPadding + chartWidth * dateIdx.toFloat() / (allDates.size - 1).coerceAtLeast(1)
                val y = topPadding + chartHeight * (1f - ((entry.price ?: return@forEachIndexed) - minPrice) / priceRange).toFloat()

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3f)
            )
        }
    }
}

@Composable
fun RetailerHistorySection(
    retailerName: String,
    entries: List<com.zachvlat.howmuchgr.network.DailyPriceEntry>
) {
    if (entries.isEmpty()) return

    val sorted = entries.sortedBy { it.date }
    val minPrice = sorted.minOfOrNull { it.price ?: Double.MAX_VALUE }
    val maxPrice = sorted.maxOfOrNull { it.price ?: 0.0 }

    val inputFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val outputFormat = remember { SimpleDateFormat("d MMM", Locale("el", "GR")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = retailerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "%.2f € — %.2f €".format(minPrice, maxPrice),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val recent = sorted.takeLast(7)
            recent.forEach { entry ->
                val displayDate = try {
                    val date = inputFormat.parse(entry.date)
                    date?.let { outputFormat.format(it) } ?: entry.date
                } catch (_: Exception) {
                    entry.date
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.2f €".format(entry.price),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (sorted.size > 7) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "+%d ημέρες".format(sorted.size - 7),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RetailerPriceRow(price: RetailerPrice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = price.retailerDisplayName,
                style = MaterialTheme.typography.bodySmall
            )
            if (price.isDiscount) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ΠΡΟΣΦΟΡΑ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "%.2f €".format(price.price),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
