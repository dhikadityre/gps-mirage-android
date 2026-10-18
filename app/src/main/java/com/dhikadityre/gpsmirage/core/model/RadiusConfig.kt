package com.dhikadityre.gpsmirage.core.model

/**
 * Domain model representing geofence radius configuration.
 *
 * @property isEnabled Whether radius-constrained selection is active.
 * @property anchor The center reference coordinate for the radius boundary, or null if unconfigured.
 * @property radiusMeters The allowed selection radius distance in meters (default: 500.0m).
 */
data class RadiusConfig(
    val isEnabled: Boolean = true,
    val anchor: Coordinate? = MAYAPADA_TOWER,
    val radiusMeters: Double = 500.0
) {
    companion object {
        val MAYAPADA_TOWER = Coordinate(latitude = -6.213726, longitude = 106.819897)
    }
}
