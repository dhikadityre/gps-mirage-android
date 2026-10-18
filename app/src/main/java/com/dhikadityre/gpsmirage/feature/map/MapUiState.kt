package com.dhikadityre.gpsmirage.feature.map

import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.MapLayerStyle
import com.dhikadityre.gpsmirage.core.model.RadiusConfig

enum class MapSelectionMode {
    CENTER_PIN,
    TAP_TO_SELECT
}

data class MapPreviewSnapshot(
    val previousRadiusConfig: RadiusConfig,
    val previousCoordinate: Coordinate?,
    val previousLocationName: String,
    val previousResolvedAddress: String?,
    val previousCenterAnchor: Coordinate?
)

data class MapUiState(
    val selectedCoordinate: Coordinate? = RadiusConfig.MAYAPADA_TOWER,
    val selectedLocationName: String = "",
    val isResolvingAddress: Boolean = true,
    val resolvedShortAddress: String? = null,
    val isLoading: Boolean = false,
    val isMockActive: Boolean = false,
    val activeMockLocation: Location? = null,
    val recentLocations: List<Location> = emptyList(),
    val favoriteLocations: List<Location> = emptyList(),
    val isQuickLocationsVisible: Boolean = false,
    val radiusConfig: RadiusConfig = RadiusConfig(),
    val distanceToAnchorMeters: Double? = 0.0,
    val isWithinRadius: Boolean = true,
    val selectionMode: MapSelectionMode = MapSelectionMode.CENTER_PIN,
    val isPositionLocked: Boolean = false,
    val isRewindEnabled: Boolean = false,
    val previewSnapshot: MapPreviewSnapshot? = null,
    val previewingLocationName: String? = null,
    val mapLayerStyle: MapLayerStyle = MapLayerStyle.GOOGLE_ROADMAP,
    val error: String? = null,
    val event: MapEvent? = null
)

sealed interface MapEvent {
    data class ShowSnackbar(val message: String) : MapEvent
    object NavigateToSetupGuide : MapEvent
    object MockStarted : MapEvent
    object MockStopped : MapEvent
    data class AnimateCamera(val coordinate: Coordinate) : MapEvent
}
