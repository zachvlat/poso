package com.zachvlat.howmuchgr.ui.screen

import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.activity.compose.BackHandler
import coil.compose.rememberAsyncImagePainter
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zachvlat.howmuchgr.network.CategoryNode
import com.zachvlat.howmuchgr.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    BackHandler(enabled = state.canGoBack) {
        viewModel.onBackClick()
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.canGoBack) {
                    IconButton(onClick = viewModel::onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Πίσω"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = state.currentTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoading && !state.isProductView -> {
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            IconButton(onClick = viewModel::retry) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Επανάληψη"
                                )
                            }
                        }
                    }
                }

                state.isProductView -> {
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (state.products.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Δεν βρέθηκαν προϊόντα",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        ProductList(
                            products = state.products,
                            onProductClick = viewModel::onProductClick
                        )
                    }
                }

                else -> {
                    CategoryGrid(
                        categories = state.categories,
                        onCategoryClick = viewModel::onCategoryClick
                    )
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
private fun CategoryGrid(
    categories: List<CategoryNode>,
    onCategoryClick: (CategoryNode) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.categoryId }) { category ->
            CategoryCard(
                category = category,
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: CategoryNode,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val imagePainter = if (category.imageUrl != null) {
        rememberAsyncImagePainter(
            model = coil.request.ImageRequest.Builder(context)
                .data(category.imageUrl)
                .crossfade(true)
                .build(),
            error = rememberVectorPainter(image = Icons.Default.Folder),
            placeholder = rememberVectorPainter(image = Icons.Default.Folder)
        )
    } else {
        rememberVectorPainter(image = Icons.Default.Folder)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = imagePainter,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (category.imageUrl == null) MaterialTheme.colorScheme.primary else Color.Unspecified
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            if (category.totalProductCount != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${category.totalProductCount} προϊόντα",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        }
    }
}
