package com.zachvlat.howmuchgr.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object PriceCheckScheduler {
    private const val WORK_NAME = "price_check"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<PriceCheckWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayToNextRun(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun initialDelayToNextRun(): Long {
        val now = Calendar.getInstance()
        val nextNine = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!nextNine.after(now)) {
            nextNine.add(Calendar.DAY_OF_YEAR, 1)
        }
        return nextNine.timeInMillis - now.timeInMillis
    }
}