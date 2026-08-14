package com.zachvlat.howmuchgr

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.zachvlat.howmuchgr.data.CartRepository
import com.zachvlat.howmuchgr.data.WishlistRepository
import com.zachvlat.howmuchgr.network.ProductApiService

class HowmuchgrApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        WishlistRepository.init(this)
        CartRepository.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(ProductApiService.createOkHttpClient())
            .build()
    }
}
