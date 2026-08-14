package com.zachvlat.howmuchgr.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

interface ProductApiService {

    @Headers(
        "Accept: application/json",
        "X-App-Version: 1.0.0",
        "X-Platform: flutter-web"
    )
    @POST("products/search")
    suspend fun searchProducts(@Body request: SearchRequest): SearchResponse

    @Headers(
        "Accept: application/json",
        "X-App-Version: 1.0.0",
        "X-Platform: flutter-web"
    )
    @GET("meta/categories/tree")
    suspend fun getCategoryTree(
        @Query("include_counts") includeCounts: Boolean = true,
        @Query("include_hidden") includeHidden: Boolean = true
    ): CategoryTreeResponse

    @Headers(
        "Accept: application/json",
        "X-App-Version: 1.0.0",
        "X-Platform: flutter-web"
    )
    @GET("products/{id}")
    suspend fun getProductById(
        @retrofit2.http.Path("id") id: String,
        @Query("sort_retailers") sortRetailers: String = "asc",
        @Query("countries") countries: String = "GR",
        @Query("include_tax") includeTax: Boolean = true,
        @Query("include_history") includeHistory: Boolean = true
    ): Product

    @Headers(
        "Accept: application/json",
        "X-App-Version: 1.0.0",
        "X-Platform: flutter-web"
    )
    @GET("products")
    suspend fun getProductsByCategory(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 99,
        @Query("sort_by") sortBy: String = "unit_price",
        @Query("sort_order") sortOrder: String = "asc",
        @Query("category") categoryId: String,
        @Query("countries") countries: String = "GR"
    ): ProductsResponse

    companion object {
        private const val BASE_URL = "https://api.posokanei.gov.gr/"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

        fun create(): ProductApiService {
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

            val contentType = "application/json".toMediaType()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(createOkHttpClient())
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(ProductApiService::class.java)
        }

        fun createOkHttpClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            return OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .build()
                    )
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
}
