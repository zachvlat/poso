package com.zachvlat.howmuchgr

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.zachvlat.howmuchgr.data.CartRepository
import com.zachvlat.howmuchgr.data.PriceAlertRepository
import com.zachvlat.howmuchgr.data.WishlistRepository
import com.zachvlat.howmuchgr.network.ProductApiService
import com.zachvlat.howmuchgr.worker.PriceCheckScheduler
import com.zachvlat.howmuchgr.worker.PriceCheckWorker

class HowmuchgrApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        WishlistRepository.init(this)
        CartRepository.init(this)
        PriceAlertRepository.init(this)
        PriceCheckWorker.createNotificationChannel(this)
        PriceCheckScheduler.schedule(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(ProductApiService.createOkHttpClient())
            .build()
    }
}
