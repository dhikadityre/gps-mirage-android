package com.dhikadityre.gpsmirage.core.model

/**
 * Represents a geographic coordinate (lat/lng pair).
 */
data class Coordinate(
    val latitude: Double,
    val longitude: Double
)

enum class LocationType {
    CENTER_ANCHOR,
    MOCK_TARGET,
    NATIVE
}

/**
 * Core location domain model.
 * Used across all features — saved locations, history, generator output, etc.
 */
data class Location(
    val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: LocationType = LocationType.MOCK_TARGET,
    val radiusMeters: Double? = null,
    val parentAnchorId: Long? = null,
    val accuracy: Float = 5f,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val isFavorite: Boolean = false,
    val isGenerated: Boolean = false,
    val folderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val coordinate: Coordinate
        get() = Coordinate(latitude, longitude)

    val displayCoordinate: String
        get() = "%.6f, %.6f".format(latitude, longitude)

    val isNew: Boolean
        get() = id == 0L
}

/**
 * Composite model representing a Center Anchor and its associated child Mock targets.
 */
data class AnchorWithMocks(
    val anchor: Location,
    val mocks: List<Location>
)

