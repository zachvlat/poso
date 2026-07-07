package com.zachvlat.howmuchgr.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private data class NavTab(
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    NavTab("Αναζήτηση", Icons.Default.Search),
    NavTab("Αγαπημένα", Icons.Default.Favorite)
)

@Composable
fun MainScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingQuery by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> {
                SearchScreen(
                    modifier = Modifier.padding(innerPadding),
                    initialQuery = pendingQuery
                )
                pendingQuery = null
            }

            1 -> {
                WishlistScreen(
                    modifier = Modifier.padding(innerPadding),
                    onQuerySelected = { query ->
                        pendingQuery = query
                        selectedTab = 0
                    }
                )
            }
        }
    }
}
