package com.zachvlat.howmuchgr.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WishlistRepository {
    private const val PREFS_NAME = "wishlist"
    private const val KEY_QUERIES = "saved_queries"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getQueries(): List<String> {
        val raw = prefs.getString(KEY_QUERIES, "[]") ?: "[]"
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addQuery(query: String) {
        val list = getQueries().toMutableList()
        if (query !in list) {
            list.add(query)
            save(list)
        }
    }

    fun removeQuery(query: String) {
        val list = getQueries().toMutableList()
        if (list.remove(query)) {
            save(list)
        }
    }

    fun isSaved(query: String): Boolean = query in getQueries()

    private fun save(queries: List<String>) {
        prefs.edit().putString(KEY_QUERIES, json.encodeToString(queries)).apply()
    }
}
