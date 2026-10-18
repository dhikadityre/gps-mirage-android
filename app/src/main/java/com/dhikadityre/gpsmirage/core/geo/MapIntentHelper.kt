package com.dhikadityre.gpsmirage.core.geo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object MapIntentHelper {

    /**
     * Launch Google Maps or external map app to inspect the specified coordinate.
     * Targets `com.google.android.apps.maps` with safe fallbacks to generic geo-intent or browser.
     */
    fun launchMaps(context: Context, latitude: Double, longitude: Double, label: String? = null) {
        val queryParam = if (!label.isNullOrBlank()) {
            "$latitude,$longitude(${URLEncoder.encode(label, "UTF-8")})"
        } else {
            "$latitude,$longitude"
        }
        val uriString = "geo:$latitude,$longitude?q=$queryParam"
        val mapUri = Uri.parse(uriString)

        try {
            val gmapsIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            context.startActivity(gmapsIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW, mapUri)
                context.startActivity(genericIntent)
            } catch (_: Exception) {
                try {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                    )
                    context.startActivity(browserIntent)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
