package com.dhikadityre.gpsmirage.feature.locations

import com.dhikadityre.gpsmirage.core.model.Folder
import com.dhikadityre.gpsmirage.core.model.Location

enum class LocationsTab { ALL, FAVORITES, FOLDERS }

data class LocationsUiState(
    val selectedTab: LocationsTab = LocationsTab.ALL,
    val allLocations: List<Location> = emptyList(),
    val favorites: List<Location> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Location> = emptyList(),
    val isSearchActive: Boolean = false,
    val locationToDelete: Location? = null,
    val error: String? = null
)
