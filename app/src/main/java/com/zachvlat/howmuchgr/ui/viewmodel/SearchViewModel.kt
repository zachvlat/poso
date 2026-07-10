package com.zachvlat.howmuchgr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zachvlat.howmuchgr.data.WishlistRepository
import com.zachvlat.howmuchgr.network.Product
import com.zachvlat.howmuchgr.network.ProductApiService
import com.zachvlat.howmuchgr.network.SearchRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCurrentQuerySaved: Boolean = false,
    val selectedProduct: Product? = null,
    val isDetailLoading: Boolean = false
)

class SearchViewModel : ViewModel() {

    private val apiService = ProductApiService.create()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            isCurrentQuerySaved = WishlistRepository.isSaved(query)
        )
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            if (query.isNotBlank()) {
                searchProducts(query)
            } else {
                _uiState.value = _uiState.value.copy(products = emptyList(), error = null)
            }
        }
    }

    fun setQueryFromWishlist(query: String) {
        onQueryChanged(query)
    }

    fun toggleWishlist() {
        val q = _uiState.value.query
        if (q.isBlank() || _uiState.value.products.isEmpty()) return

        if (WishlistRepository.isSaved(q)) {
            WishlistRepository.removeQuery(q)
        } else {
            WishlistRepository.addQuery(q)
        }
        _uiState.value = _uiState.value.copy(
            isCurrentQuerySaved = !_uiState.value.isCurrentQuerySaved
        )
    }

    fun onProductClick(product: Product) {
        _uiState.value = _uiState.value.copy(selectedProduct = product, isDetailLoading = true)
        viewModelScope.launch {
            try {
                val detailed = apiService.getProductById(product.id)
                _uiState.value = _uiState.value.copy(
                    selectedProduct = detailed,
                    isDetailLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    selectedProduct = product,
                    isDetailLoading = false
                )
            }
        }
    }

    fun dismissProductDetail() {
        _uiState.value = _uiState.value.copy(selectedProduct = null, isDetailLoading = false)
    }

    private suspend fun searchProducts(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            val request = SearchRequest(title = query)
            val response = apiService.searchProducts(request)
            _uiState.value = _uiState.value.copy(
                products = response.products,
                isLoading = false,
                isCurrentQuerySaved = WishlistRepository.isSaved(query)
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Unknown error"
            )
        }
    }
}
