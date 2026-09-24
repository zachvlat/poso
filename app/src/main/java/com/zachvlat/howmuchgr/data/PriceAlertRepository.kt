package com.zachvlat.howmuchgr.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class StoredPrice(
    val productId: String,
    val name: String,
    val price: Double
)

object PriceAlertRepository {
    private const val PREFS_NAME = "price_alerts"
    private const val KEY_PRICES = "saved_prices"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getPrices(): Map<String, StoredPrice> {
        val raw = prefs.getString(KEY_PRICES, "{}") ?: "{}"
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun upsert(price: StoredPrice) {
        val map = getPrices().toMutableMap()
        map[price.productId] = price
        prefs.edit().putString(KEY_PRICES, json.encodeToString(map)).apply()
    }
}