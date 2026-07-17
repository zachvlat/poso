package com.zachvlat.howmuchgr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zachvlat.howmuchgr.data.CartRepository
import com.zachvlat.howmuchgr.network.Product
import com.zachvlat.howmuchgr.network.ProductApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartItem(
    val product: Product,
    val cheapestPrice: Double,
    val storeName: String,
    val quantity: Int
)

data class StoreTotal(
    val storeName: String,
    val total: Double
)

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val storeTotals: List<StoreTotal> = emptyList(),
    val grandTotal: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CartViewModel : ViewModel() {

    private val apiService = ProductApiService.create()

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val productCache = mutableMapOf<String, Product>()

    fun refresh() {
        val quantities = CartRepository.getQuantities()
        if (quantities.isEmpty()) {
            productCache.clear()
            _uiState.value = CartUiState()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            for (id in quantities.keys) {
                if (id !in productCache) {
                    try {
                        productCache[id] = apiService.getProductById(id)
                    } catch (_: Exception) {}
                }
            }
            rebuildState()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun increment(productId: String) {
        CartRepository.addProduct(productId)
        rebuildState()
    }

    fun decrement(productId: String) {
        CartRepository.decrementProduct(productId)
        rebuildState()
    }

    fun removeProduct(productId: String) {
        CartRepository.removeProduct(productId)
        productCache.remove(productId)
        rebuildState()
    }

    private fun rebuildState() {
        val quantities = CartRepository.getQuantities()
        if (quantities.isEmpty()) {
            productCache.clear()
            _uiState.value = CartUiState()
            return
        }

        val items = mutableListOf<CartItem>()
        for ((id, qty) in quantities) {
            val product = productCache[id] ?: continue
            val cheapest = product.retailerPrices
                .filter { it.price != null }
                .minByOrNull { it.price!! }
            if (cheapest != null) {
                items.add(
                    CartItem(
                        product = product,
                        cheapestPrice = cheapest.price!!,
                        storeName = cheapest.retailerDisplayName,
                        quantity = qty
                    )
                )
            }
        }
        _uiState.value = buildState(items)
    }

    private fun buildState(items: List<CartItem>): CartUiState {
        val storeMap = mutableMapOf<String, Double>()
        for (item in items) {
            storeMap[item.storeName] = (storeMap[item.storeName] ?: 0.0) + item.cheapestPrice * item.quantity
        }
        val storeTotals = storeMap.map { (name, total) ->
            StoreTotal(storeName = name, total = total)
        }.sortedByDescending { it.total }
        val grandTotal = items.sumOf { it.cheapestPrice * it.quantity }
        return CartUiState(
            items = items,
            storeTotals = storeTotals,
            grandTotal = grandTotal
        )
    }
}
