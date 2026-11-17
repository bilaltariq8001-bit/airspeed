package com.velocity.speedometer

import android.app.Application
import com.google.android.gms.maps.MapsApiSettings
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class VelocityApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure osmdroid cache inside app-private cache directory (no storage permission needed)
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val osmBase = File(cacheDir, "osmdroid")
        osmConfig.osmdroidBasePath = osmBase
        osmConfig.osmdroidTileCache = File(osmBase, "tiles")

        // Configure Google Maps Platform usage attribution ID when API key is configured
        if (BuildConfig.MAPS_API_KEY.isNotBlank()) {
            runCatching {
                MapsApiSettings.addInternalUsageAttributionId(
                    this,
                    "gmp_mcp_codeassist_v1_aistudio"
                )
            }
        }
    }
}
