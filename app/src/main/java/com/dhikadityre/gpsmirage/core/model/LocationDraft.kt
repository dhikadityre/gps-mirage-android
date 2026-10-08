package com.dhikadityre.gpsmirage.core.model

/**
 * A draft location used during import/parse preview — not yet persisted.
 */
data class LocationDraft(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 5f,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f
) {
    val displayCoordinate: String
        get() = "%.6f, %.6f".format(latitude, longitude)

    fun toLocation(): Location = Location(
        name = name,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        altitude = altitude,
        speed = speed,
        bearing = bearing
    )
}

/**
 * Mock location session state.
 */
data class MockSession(
    val location: Location,
    val startedAt: Long = System.currentTimeMillis(),
    val historyId: Long = 0L
)
