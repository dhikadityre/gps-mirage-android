package com.dhikadityre.gpsmirage

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class GpsMirageApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configure OSMDroid internal storage for full Marshmallow (API 23) compatibility
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }
}
