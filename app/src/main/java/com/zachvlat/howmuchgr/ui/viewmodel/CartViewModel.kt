package com.zachvlat.howmuchgr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zachvlat.howmuchgr.data.CartRepository
import com.zachvlat.howmuchgr.network.Product
import com.zachvlat.howmuchgr.network.ProductApiService
import com.zachvlat.howmuchgr.network.RetailerPrice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartItem(
    val product: Product,
    val price: Double,
    val storeName: String,
    val storeKey: String,
    val quantity: Int,
    val availableStores: List<RetailerPrice>
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

    fun selectStore(productId: String, retailerKey: String) {
        CartRepository.setSelectedStore(productId, retailerKey)
        rebuildState()
    }

    fun getSharePayload(): String? = CartRepository.getSharePayload()

    fun importCart(payload: String): Boolean {
        if (!CartRepository.replaceCart(payload)) return false
        productCache.clear()
        refresh()
        return true
    }

    private fun rebuildState() {
        val quantities = CartRepository.getQuantities()
        if (quantities.isEmpty()) {
            productCache.clear()
            _uiState.value = CartUiState()
            return
        }

        val selectedStores = CartRepository.getSelectedStores()
        val items = mutableListOf<CartItem>()
        for ((id, qty) in quantities) {
            val product = productCache[id] ?: continue
            val availableStores = product.retailerPrices.filter { it.price != null }
            if (availableStores.isEmpty()) continue
            val selectedKey = selectedStores[id]
            val chosen = availableStores.firstOrNull { it.retailer == selectedKey }
                ?: availableStores.minByOrNull { it.price!! }!!
            items.add(
                CartItem(
                    product = product,
                    price = chosen.price!!,
                    storeName = chosen.retailerDisplayName,
                    storeKey = chosen.retailer,
                    quantity = qty,
                    availableStores = availableStores
                )
            )
        }
        _uiState.value = buildState(items)
    }

    private fun buildState(items: List<CartItem>): CartUiState {
        val storeMap = mutableMapOf<String, Double>()
        for (item in items) {
            storeMap[item.storeName] = (storeMap[item.storeName] ?: 0.0) + item.price * item.quantity
        }
        val storeTotals = storeMap.map { (name, total) ->
            StoreTotal(storeName = name, total = total)
        }.sortedByDescending { it.total }
        val grandTotal = items.sumOf { it.price * it.quantity }
        return CartUiState(
            items = items,
            storeTotals = storeTotals,
            grandTotal = grandTotal
        )
    }
}
