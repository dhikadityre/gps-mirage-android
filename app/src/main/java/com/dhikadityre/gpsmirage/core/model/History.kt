package com.dhikadityre.gpsmirage.core.model

/**
 * History entry — automatically created when a mock location session starts/stops.
 * locationId is nullable: history persists even if the saved location is deleted later.
 */
data class History(
    val id: Long = 0L,
    val locationId: Long? = null,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val startedAt: Long,
    val stoppedAt: Long? = null,
    val durationMs: Long? = null
) {
    val displayCoordinate: String
        get() = "%.6f, %.6f".format(latitude, longitude)

    val isActive: Boolean
        get() = stoppedAt == null
}
