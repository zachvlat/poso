package com.zachvlat.howmuchgr.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SharedCartPayload(
    val quantities: Map<String, Int>,
    val stores: Map<String, String> = emptyMap()
)

object CartRepository {
    private const val PREFS_NAME = "cart"
    private const val KEY_QUANTITIES = "cart_quantities"
    private const val KEY_STORES = "selected_stores"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getQuantities(): Map<String, Int> {
        val raw = prefs.getString(KEY_QUANTITIES, "{}") ?: "{}"
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun getProductIds(): List<String> = getQuantities().keys.toList()

    fun addProduct(productId: String) {
        val map = getQuantities().toMutableMap()
        map[productId] = (map[productId] ?: 0) + 1
        save(map)
    }

    fun decrementProduct(productId: String) {
        val map = getQuantities().toMutableMap()
        val current = map[productId] ?: return
        if (current <= 1) {
            map.remove(productId)
            clearSelectedStore(productId)
        } else {
            map[productId] = current - 1
        }
        save(map)
    }

    fun removeProduct(productId: String) {
        val map = getQuantities().toMutableMap()
        map.remove(productId)
        clearSelectedStore(productId)
        save(map)
    }

    fun getQuantity(productId: String): Int = getQuantities()[productId] ?: 0

    fun isInCart(productId: String): Boolean = getQuantity(productId) > 0

    fun getSelectedStores(): Map<String, String> {
        val raw = prefs.getString(KEY_STORES, "{}") ?: "{}"
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun setSelectedStore(productId: String, retailerKey: String) {
        val map = getSelectedStores().toMutableMap()
        map[productId] = retailerKey
        saveStores(map)
    }

    fun clearSelectedStore(productId: String) {
        val map = getSelectedStores().toMutableMap()
        if (map.remove(productId) != null) {
            saveStores(map)
        }
    }

    fun getSharePayload(): String? {
        val quantities = getQuantities()
        if (quantities.isEmpty()) return null
        return json.encodeToString(
            SharedCartPayload(
                quantities = quantities,
                stores = getSelectedStores()
            )
        )
    }

    fun replaceCart(payload: String): Boolean {
        return try {
            val parsed = json.decodeFromString<SharedCartPayload>(payload)
            val quantities = parsed.quantities
                .filterValues { it > 0 }
                .toMap()
            if (quantities.isEmpty()) return false
            val stores = parsed.stores.filterKeys { it in quantities }
            save(quantities)
            saveStores(stores)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun save(map: Map<String, Int>) {
        prefs.edit().putString(KEY_QUANTITIES, json.encodeToString(map)).apply()
    }

    private fun saveStores(map: Map<String, String>) {
        prefs.edit().putString(KEY_STORES, json.encodeToString(map)).apply()
    }
}
