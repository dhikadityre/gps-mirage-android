package com.dhikadityre.gpsmirage.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.dhikadityre.gpsmirage.core.model.Coordinate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface DeviceLocationProvider {
    /**
     * Attempts to acquire current physical device GPS/network location.
     * Returns null if permissions are absent, providers disabled, or location unavailable.
     */
    suspend fun getCurrentLocation(): Coordinate?
}

@Singleton
class DeviceLocationProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceLocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Coordinate? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // 1. Try getCurrentLocation on API 30+ (Android R+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val candidate = suspendCancellableCoroutine<Location?> { cont ->
                    val signal = android.os.CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    lm.getCurrentLocation(
                        LocationManager.GPS_PROVIDER,
                        signal,
                        ContextCompat.getMainExecutor(context)
                    ) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                }
                if (candidate != null) {
                    return Coordinate(candidate.latitude, candidate.longitude)
                }
            } catch (_: SecurityException) {
                // Permission denied
            } catch (_: Exception) {
                // Fallback to last known location
            }
        }

        // 2. Fallback to getLastKnownLocation from available providers
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (lm.isProviderEnabled(provider)) {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || loc.time > bestLocation.time) {
                            bestLocation = loc
                        }
                    }
                }
            } catch (_: SecurityException) {
                // Permission denied
            } catch (_: Exception) {
                // Provider unavailable
            }
        }

        return bestLocation?.let { Coordinate(it.latitude, it.longitude) }
    }
}
