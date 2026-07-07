package com.zachvlat.howmuchgr.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.zachvlat.howmuchgr.data.WishlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WishlistUiState(
    val queries: List<String> = emptyList()
)

class WishlistViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WishlistUiState())
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = WishlistUiState(queries = WishlistRepository.getQueries())
    }

    fun removeQuery(query: String) {
        WishlistRepository.removeQuery(query)
        refresh()
    }
}
