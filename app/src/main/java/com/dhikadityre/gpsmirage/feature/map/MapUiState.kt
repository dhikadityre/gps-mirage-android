package com.dhikadityre.gpsmirage.feature.map

import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.RadiusConfig

data class MapUiState(
    val selectedCoordinate: Coordinate? = null,
    val selectedLocationName: String = "",
    val isResolvingAddress: Boolean = false,
    val resolvedShortAddress: String? = null,
    val isLoading: Boolean = false,
    val isMockActive: Boolean = false,
    val activeMockLocation: Location? = null,
    val recentLocations: List<Location> = emptyList(),
    val favoriteLocations: List<Location> = emptyList(),
    val isQuickLocationsVisible: Boolean = false,
    val radiusConfig: RadiusConfig = RadiusConfig(),
    val distanceToAnchorMeters: Double? = null,
    val isWithinRadius: Boolean = true,
    val error: String? = null,
    val event: MapEvent? = null
)

sealed interface MapEvent {
    data class ShowSnackbar(val message: String) : MapEvent
    object NavigateToSetupGuide : MapEvent
    object MockStarted : MapEvent
    object MockStopped : MapEvent
}
