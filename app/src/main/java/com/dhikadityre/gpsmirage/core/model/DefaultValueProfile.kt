package com.dhikadityre.gpsmirage.core.model

import com.dhikadityre.gpsmirage.core.geo.UnitConverter

/**
 * Domain model representing a Default Value Profile for GPS Mirage.
 * Presets pre-fill and autocalibrate coordinates, radius, accuracy, altitude, speed,
 * and compass bearing behavior across the application.
 */
data class DefaultValueProfile(
    val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val accuracyMeters: Double,
    val altitudeMeters: Double,
    val speedKmh: Double,
    val bearingDegrees: Double,
    val useDeviceSensor: Boolean = false,
    val isActive: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val accuracyFeet: Double
        get() = UnitConverter.metersToFeet(accuracyMeters)

    val altitudeFeet: Double
        get() = UnitConverter.metersToFeet(altitudeMeters)

    val coordinate: Coordinate
        get() = Coordinate(latitude, longitude)

    companion object {
        /**
         * System default preset 'original':
         * - Name: original
         * - Latitude: -6.213647
         * - Longitude: 106.819969
         * - Radius: 50.0 meters
         * - Accuracy: 78.0 ft (~23.77m)
         * - Altitude: 89.0 ft (~27.13m)
         * - Speed: 0.0 km/h
         * - Bearing: 0.0° (Utara)
         * - Sensor Kompas: true
         */
        val ORIGINAL = DefaultValueProfile(
            id = 1L,
            name = "original",
            latitude = -6.213647,
            longitude = 106.819969,
            radiusMeters = 50.0,
            accuracyMeters = UnitConverter.feetToMeters(78.0),
            altitudeMeters = UnitConverter.feetToMeters(89.0),
            speedKmh = 0.0,
            bearingDegrees = 0.0,
            useDeviceSensor = true,
            isActive = true,
            isDefault = true
        )
    }
}
