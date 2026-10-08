package com.dhikadityre.gpsmirage.feature.map

import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.Location

data class MapUiState(
    val selectedCoordinate: Coordinate? = null,
    val selectedLocationName: String = "",
    val isLoading: Boolean = false,
    val isMockActive: Boolean = false,
    val activeMockLocation: Location? = null,
    val recentLocations: List<Location> = emptyList(),
    val favoriteLocations: List<Location> = emptyList(),
    val isQuickLocationsVisible: Boolean = false,
    val error: String? = null,
    val event: MapEvent? = null
)

sealed interface MapEvent {
    data class ShowSnackbar(val message: String) : MapEvent
    object NavigateToSetupGuide : MapEvent
    object MockStarted : MapEvent
    object MockStopped : MapEvent
}
