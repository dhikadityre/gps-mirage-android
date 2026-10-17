package com.dhikadityre.gpsmirage.core.geo

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.GeocodeResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AndroidReverseGeocoderImpl(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ReverseGeocoder {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, Dispatchers.IO)

    override suspend fun getShortAddress(coordinate: Coordinate): GeocodeResult? {
        if (!Geocoder.isPresent()) {
            return null
        }

        return withTimeoutOrNull(TIMEOUT_MS) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = fetchAddresses(geocoder, coordinate)
                val primary = addresses?.firstOrNull() ?: return@withTimeoutOrNull null

                val shortName = AddressFormatter.formatShortAddress(
                    thoroughfare = primary.thoroughfare,
                    subThoroughfare = primary.subThoroughfare,
                    featureName = primary.featureName,
                    subLocality = primary.subLocality,
                    locality = primary.locality,
                    firstAddressLine = if (primary.maxAddressLineIndex >= 0) primary.getAddressLine(0) else null
                )

                if (shortName != null) {
                    val fullAddress = if (primary.maxAddressLineIndex >= 0) primary.getAddressLine(0) else null
                    GeocodeResult(shortAddress = shortName, fullAddress = fullAddress)
                } else {
                    null
                }
            } catch (_: IOException) {
                null
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun fetchAddresses(
        geocoder: Geocoder,
        coordinate: Coordinate
    ): List<Address>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                try {
                    geocoder.getFromLocation(
                        coordinate.latitude,
                        coordinate.longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) {
                                    continuation.resume(addresses)
                                }
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        } else {
            withContext(ioDispatcher) {
                @Suppress("DEPRECATION")
                try {
                    geocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 1)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    companion object {
        private const val TIMEOUT_MS = 5000L
    }
}
