package com.zachvlat.howmuchgr.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zachvlat.howmuchgr.MainActivity
import com.zachvlat.howmuchgr.R
import com.zachvlat.howmuchgr.data.PriceAlertRepository
import com.zachvlat.howmuchgr.data.StoredPrice
import com.zachvlat.howmuchgr.data.WishlistRepository
import com.zachvlat.howmuchgr.network.Product
import com.zachvlat.howmuchgr.network.ProductApiService
import com.zachvlat.howmuchgr.network.SearchRequest

class PriceCheckWorker(
    appContext: android.content.Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val queries = WishlistRepository.getQueries()
        if (queries.isEmpty()) return Result.success()

        val apiService = ProductApiService.create()
        val drops = mutableListOf<Pair<StoredPrice, Double>>()
        val seen = mutableSetOf<String>()

        for (query in queries) {
            try {
                val response = apiService.searchProducts(SearchRequest(title = query))
                for (product in response.products) {
                    if (product.id in seen) continue
                    seen.add(product.id)

                    val currentPrice = lowestPrice(product)
                    if (currentPrice == null || currentPrice <= 0.0) continue

                    val stored = PriceAlertRepository.getPrices()[product.id]
                    if (stored != null && stored.price > currentPrice) {
                        val dropPercent = (stored.price - currentPrice) / stored.price * 100.0
                        drops.add(stored.copy(price = currentPrice) to dropPercent)
                    }

                    PriceAlertRepository.upsert(
                        StoredPrice(
                            productId = product.id,
                            name = product.name,
                            price = currentPrice
                        )
                    )
                }
            } catch (_: Exception) {
                // Ignore failing queries and check the rest
            }
        }

        if (drops.isNotEmpty()) {
            sendNotification(drops)
        }
        return Result.success()
    }

    private fun lowestPrice(product: Product): Double? {
        return product.priceStats?.minPrice
            ?: product.retailerPrices
                .filter { it.price != null }
                .minOfOrNull { it.price!! }
    }

    private fun sendNotification(drops: List<Pair<StoredPrice, Double>>) {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (drops.size == 1) "Πτώση τιμής" else "Πτώση τιμών"
        val lines = drops.joinToString("\n") { (item, drop) ->
            "%s: -%.0f%%".format(item.name.take(45), drop)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(lines.substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "price_drops"
        private const val NOTIFICATION_ID = 1001

        fun createNotificationChannel(context: android.content.Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Πτώσεις τιμών",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}