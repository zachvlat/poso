package com.zachvlat.howmuchgr.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(
    val page: Int = 1,
    @SerialName("page_size")
    val pageSize: Int = 15,
    @SerialName("sort_by")
    val sortBy: String = "name",
    @SerialName("sort_order")
    val sortOrder: String = "asc",
    val title: String
)

@Serializable
data class SearchResponse(
    val products: List<Product>
)

@Serializable
data class Product(
    val id: String,
    val name: String,
    val brand: String? = null,
    val images: List<String> = emptyList(),
    val category: String? = null,
    @SerialName("category_ids")
    val categoryIds: List<String> = emptyList(),
    val subcategory: String? = null,
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("has_image")
    val hasImage: Boolean = false,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("image_version")
    val imageVersion: String? = null,
    val unit: String? = null,
    @SerialName("unit_quantity")
    val unitQuantity: Double? = null,
    @SerialName("private_label")
    val privateLabel: Boolean = false,
    @SerialName("price_stats")
    val priceStats: PriceStats? = null,
    val retailers: List<String> = emptyList(),
    @SerialName("retailer_prices")
    val retailerPrices: List<RetailerPrice> = emptyList(),
    @SerialName("available_countries")
    val availableCountries: List<String> = emptyList(),
    @SerialName("is_international")
    val isInternational: Boolean = false
)

@Serializable
data class PriceStats(
    @SerialName("min_price")
    val minPrice: Double? = null,
    @SerialName("max_price")
    val maxPrice: Double? = null,
    @SerialName("avg_price")
    val avgPrice: Double? = null,
    @SerialName("retailer_count")
    val retailerCount: Int? = null,
    @SerialName("min_unit_price")
    val minUnitPrice: Double? = null,
    @SerialName("last_computed")
    val lastComputed: String? = null
)

@Serializable
data class RetailerPrice(
    val retailer: String,
    @SerialName("retailer_display_name")
    val retailerDisplayName: String,
    @SerialName("retailer_name")
    val retailerName: String = "",
    val price: Double? = null,
    @SerialName("price_normalized")
    val priceNormalized: Double? = null,
    @SerialName("is_discount")
    val isDiscount: Boolean = false,
    @SerialName("discount_percentage")
    val discountPercentage: Double? = null,
    @SerialName("last_updated")
    val lastUpdated: String? = null,
    val country: String? = null
)
