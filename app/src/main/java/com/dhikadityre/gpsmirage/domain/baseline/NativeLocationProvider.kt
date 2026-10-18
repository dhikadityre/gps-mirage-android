package com.dhikadityre.gpsmirage.domain.baseline

import kotlinx.coroutines.flow.Flow

data class NativeLocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double,
    val speed: Float,
    val bearing: Float,
    val time: Long = System.currentTimeMillis()
)

interface NativeLocationProvider {
    val locationFlow: Flow<NativeLocationData>
    suspend fun getLastKnownLocation(): NativeLocationData?
}
