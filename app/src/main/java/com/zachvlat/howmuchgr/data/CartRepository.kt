package com.zachvlat.howmuchgr.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CartRepository {
    private const val PREFS_NAME = "cart"
    private const val KEY_QUANTITIES = "cart_quantities"

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
        } else {
            map[productId] = current - 1
        }
        save(map)
    }

    fun removeProduct(productId: String) {
        val map = getQuantities().toMutableMap()
        map.remove(productId)
        save(map)
    }

    fun getQuantity(productId: String): Int = getQuantities()[productId] ?: 0

    fun isInCart(productId: String): Boolean = getQuantity(productId) > 0

    private fun save(map: Map<String, Int>) {
        prefs.edit().putString(KEY_QUANTITIES, json.encodeToString(map)).apply()
    }
}
