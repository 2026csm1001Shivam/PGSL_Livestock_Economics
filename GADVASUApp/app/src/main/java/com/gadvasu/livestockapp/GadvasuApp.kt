package com.gadvasu.livestockapp

import android.app.Application
import com.gadvasu.livestockapp.data.FarmRepository

class GadvasuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FarmRepository.init(this)
    }
}
