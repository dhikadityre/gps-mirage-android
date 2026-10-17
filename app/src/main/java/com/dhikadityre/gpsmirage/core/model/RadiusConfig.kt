package com.dhikadityre.gpsmirage.core.model

/**
 * Domain model representing geofence radius configuration.
 *
 * @property isEnabled Whether radius-constrained selection is active.
 * @property anchor The center reference coordinate for the radius boundary, or null if unconfigured.
 * @property radiusMeters The allowed selection radius distance in meters (default: 500.0m).
 */
data class RadiusConfig(
    val isEnabled: Boolean = false,
    val anchor: Coordinate? = null,
    val radiusMeters: Double = 500.0
)
