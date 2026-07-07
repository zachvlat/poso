package com.zachvlat.howmuchgr.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
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

    companion object {
        private const val BASE_URL = "https://api.posokanei.gov.gr/"

        fun create(): ProductApiService {
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val contentType = "application/json".toMediaType()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(ProductApiService::class.java)
        }
    }
}
