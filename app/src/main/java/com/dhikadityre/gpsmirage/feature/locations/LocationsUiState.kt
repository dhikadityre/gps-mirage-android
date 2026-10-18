package com.dhikadityre.gpsmirage.feature.locations

import com.dhikadityre.gpsmirage.core.model.AnchorWithMocks
import com.dhikadityre.gpsmirage.core.model.Folder
import com.dhikadityre.gpsmirage.core.model.Location

enum class LocationsTab {
    ANCHORS,
    MOCK_TARGETS,
    GROUPED
}

data class LocationsUiState(
    val selectedTab: LocationsTab = LocationsTab.ANCHORS,
    val anchors: List<Location> = emptyList(),
    val mockTargets: List<Location> = emptyList(),
    val groupedList: List<AnchorWithMocks> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val isFilterFavoriteOnly: Boolean = false,
    val selectedFolderId: Long? = null,
    val expandedAnchorIds: Set<Long> = emptySet(),
    val duplicateAnchorMap: Map<Long, Location> = emptyMap(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Location> = emptyList(),
    val isSearchActive: Boolean = false,
    val locationToDelete: Location? = null,
    val userMessage: String? = null,
    val error: String? = null
)

