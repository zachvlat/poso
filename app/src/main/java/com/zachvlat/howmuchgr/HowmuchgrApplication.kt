package com.zachvlat.howmuchgr

import android.app.Application
import com.zachvlat.howmuchgr.data.CartRepository
import com.zachvlat.howmuchgr.data.WishlistRepository

class HowmuchgrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WishlistRepository.init(this)
        CartRepository.init(this)
    }
}
