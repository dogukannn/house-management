package com.dunettrpg

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DuneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
