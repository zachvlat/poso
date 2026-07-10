package com.zachvlat.howmuchgr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zachvlat.howmuchgr.network.CategoryNode
import com.zachvlat.howmuchgr.network.Product
import com.zachvlat.howmuchgr.network.ProductApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val categories: List<CategoryNode> = emptyList(),
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTitle: String = "Κατηγορίες",
    val canGoBack: Boolean = false,
    val isProductView: Boolean = false,
    val selectedProduct: Product? = null,
    val isDetailLoading: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val apiService = ProductApiService.create()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val backStack = mutableListOf<Pair<String, List<CategoryNode>>>()

    init {
        loadRootCategories()
    }

    private fun loadRootCategories() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            try {
                val response = apiService.getCategoryTree()
                val rootCategories = response.tree.filter { !it.hidden }
                _uiState.value = HomeUiState(
                    categories = rootCategories,
                    currentTitle = "Κατηγορίες",
                    canGoBack = false,
                    isProductView = false
                )
                backStack.clear()
            } catch (e: Exception) {
                _uiState.value = HomeUiState(error = e.message ?: "Σφάλμα φόρτωσης κατηγοριών")
            }
        }
    }

    fun onCategoryClick(category: CategoryNode) {
        if (category.children.isNotEmpty()) {
            val visibleChildren = category.children.filter { !it.hidden }
            val currentState = _uiState.value
            backStack.add(currentState.currentTitle to currentState.categories)
            _uiState.value = HomeUiState(
                categories = visibleChildren,
                currentTitle = category.name,
                canGoBack = true,
                isProductView = false
            )
        } else {
            loadProducts(category)
        }
    }

    fun onBackClick() {
        val currentState = _uiState.value
        if (currentState.isProductView) {
            _uiState.value = currentState.copy(
                products = emptyList(),
                isProductView = false,
                isLoading = false,
                error = null
            )
        } else if (backStack.isNotEmpty()) {
            val (title, categories) = backStack.removeLast()
            _uiState.value = HomeUiState(
                categories = categories,
                currentTitle = title,
                canGoBack = backStack.isNotEmpty(),
                isProductView = false
            )
        }
    }

    fun retry() {
        loadRootCategories()
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

    private fun loadProducts(category: CategoryNode) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                isProductView = true,
                products = emptyList()
            )
            try {
                val response = apiService.getProductsByCategory(
                    categoryId = category.categoryId
                )
                _uiState.value = _uiState.value.copy(
                    products = response.products,
                    isLoading = false,
                    currentTitle = category.name
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Σφάλμα φόρτωσης προϊόντων",
                    isProductView = false
                )
            }
        }
    }
}
