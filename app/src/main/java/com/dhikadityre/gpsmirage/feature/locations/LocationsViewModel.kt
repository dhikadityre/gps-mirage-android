package com.dhikadityre.gpsmirage.feature.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.geo.GeoEngine
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationType
import com.dhikadityre.gpsmirage.domain.folder.FolderRepository
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import com.dhikadityre.gpsmirage.domain.location.MapPreviewCoordinator
import com.dhikadityre.gpsmirage.domain.location.MockLocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val folderRepository: FolderRepository,
    private val mockLocationManager: MockLocationManager,
    private val mapPreviewCoordinator: MapPreviewCoordinator,
    private val geoEngine: GeoEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationsUiState(isLoading = true))
    val uiState: StateFlow<LocationsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                locationRepository.getByTypeFlow(LocationType.CENTER_ANCHOR),
                locationRepository.getByTypeFlow(LocationType.MOCK_TARGET),
                locationRepository.getByTypeFlow(LocationType.NATIVE),
                locationRepository.getAnchorsWithMocksFlow(),
                folderRepository.getAllFlow()
            ) { anchors, mockTargets, nativeLocs, grouped, folders ->
                val duplicates = computeDuplicates(anchors, mockTargets)
                _uiState.value.copy(
                    anchors = anchors,
                    mockTargets = mockTargets,
                    nativeLocations = nativeLocs,
                    groupedList = grouped,
                    folders = folders,
                    duplicateAnchorMap = duplicates,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.update { current ->
                    newState.copy(
                        selectedTab = current.selectedTab,
                        isFilterFavoriteOnly = current.isFilterFavoriteOnly,
                        selectedFolderId = current.selectedFolderId,
                        expandedAnchorIds = current.expandedAnchorIds,
                        searchQuery = current.searchQuery,
                        searchResults = current.searchResults,
                        isSearchActive = current.isSearchActive,
                        isSelectionMode = current.isSelectionMode,
                        selectedLocationIds = current.selectedLocationIds,
                        showBulkDeleteDialog = current.showBulkDeleteDialog,
                        deleteLinkedChildren = current.deleteLinkedChildren,
                        locationToDelete = current.locationToDelete
                    )
                }
            }
        }
    }

    private fun computeDuplicates(anchors: List<Location>, mocks: List<Location>): Map<Long, Location> {
        val result = mutableMapOf<Long, Location>()
        for (mock in mocks) {
            val matchingAnchor = anchors.firstOrNull { anchor ->
                geoEngine.distance(mock.coordinate, anchor.coordinate) <= 1.0 ||
                (Math.abs(mock.latitude - anchor.latitude) < 0.00001 && Math.abs(mock.longitude - anchor.longitude) < 0.00001)
            }
            if (matchingAnchor != null && mock.parentAnchorId != matchingAnchor.id) {
                result[mock.id] = matchingAnchor
            }
        }
        return result
    }

    fun onTabSelected(tab: LocationsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onToggleFavoriteFilter() {
        _uiState.update { it.copy(isFilterFavoriteOnly = !it.isFilterFavoriteOnly) }
    }

    fun onFolderSelected(folderId: Long?) {
        _uiState.update { it.copy(selectedFolderId = folderId) }
    }

    fun onToggleAnchorExpanded(anchorId: Long) {
        _uiState.update { state ->
            val updated = if (state.expandedAnchorIds.contains(anchorId)) {
                state.expandedAnchorIds - anchorId
            } else {
                state.expandedAnchorIds + anchorId
            }
            state.copy(expandedAnchorIds = updated)
        }
    }

    fun onPreviewLocation(target: Location, parentAnchor: Location? = null, onNavigateToMap: () -> Unit) {
        mapPreviewCoordinator.requestPreview(target, parentAnchor)
        onNavigateToMap()
    }

    fun onLinkToAnchor(mockId: Long, anchorId: Long) {
        viewModelScope.launch {
            locationRepository.updateParentAnchor(mockId, anchorId)
            val anchor = _uiState.value.anchors.find { it.id == anchorId }
            val anchorName = anchor?.name ?: "Anchor"
            _uiState.update { it.copy(userMessage = "Mock Target berhasil dikaitkan ke $anchorName") }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
        } else {
            viewModelScope.launch {
                val results = locationRepository.search(query)
                _uiState.update { it.copy(searchResults = results) }
            }
        }
    }

    fun onToggleSearch() {
        _uiState.update {
            val nextState = !it.isSearchActive
            it.copy(
                isSearchActive = nextState,
                searchQuery = if (!nextState) "" else it.searchQuery,
                searchResults = if (!nextState) emptyList() else it.searchResults
            )
        }
    }

    fun onToggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch {
            locationRepository.updateFavorite(id, !current)
        }
    }

    fun onDeleteRequest(location: Location) {
        _uiState.update { it.copy(locationToDelete = location) }
    }

    fun onDeleteConfirmed() {
        val target = _uiState.value.locationToDelete ?: return
        viewModelScope.launch {
            locationRepository.delete(target.id)
            _uiState.update { it.copy(locationToDelete = null) }
        }
    }

    fun onDeleteDismissed() {
        _uiState.update { it.copy(locationToDelete = null) }
    }

    fun enterSelectionMode(initialSelectedId: Long? = null) {
        _uiState.update {
            it.copy(
                isSelectionMode = true,
                selectedLocationIds = if (initialSelectedId != null) setOf(initialSelectedId) else emptySet()
            )
        }
    }

    fun exitSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedLocationIds = emptySet(),
                showBulkDeleteDialog = false,
                deleteLinkedChildren = false
            )
        }
    }

    fun toggleSelectLocation(id: Long, isAnchor: Boolean = false, childIds: List<Long> = emptyList()) {
        _uiState.update { state ->
            val currentSelected = state.selectedLocationIds
            val isCurrentlySelected = currentSelected.contains(id)
            val updated = if (isAnchor && childIds.isNotEmpty()) {
                val targetGroup = setOf(id) + childIds
                if (isCurrentlySelected) {
                    currentSelected - targetGroup
                } else {
                    currentSelected + targetGroup
                }
            } else {
                if (isCurrentlySelected) {
                    currentSelected - id
                } else {
                    currentSelected + id
                }
            }
            state.copy(selectedLocationIds = updated)
        }
    }

    fun toggleSelectAll(visibleIds: List<Long>, groupedChildMap: Map<Long, List<Long>> = emptyMap()) {
        _uiState.update { state ->
            val allTargetIds = mutableSetOf<Long>()
            for (id in visibleIds) {
                allTargetIds.add(id)
                groupedChildMap[id]?.let { children ->
                    allTargetIds.addAll(children)
                }
            }

            val currentSelected = state.selectedLocationIds
            val allSelected = allTargetIds.isNotEmpty() && currentSelected.containsAll(allTargetIds)
            val updated = if (allSelected) {
                currentSelected - allTargetIds
            } else {
                currentSelected + allTargetIds
            }
            state.copy(selectedLocationIds = updated)
        }
    }

    fun onRequestBulkDelete() {
        if (_uiState.value.selectedLocationIds.isEmpty()) return
        _uiState.update { it.copy(showBulkDeleteDialog = true) }
    }

    fun onDismissBulkDelete() {
        _uiState.update { it.copy(showBulkDeleteDialog = false, deleteLinkedChildren = false) }
    }

    fun onToggleDeleteLinkedChildren(checked: Boolean) {
        _uiState.update { it.copy(deleteLinkedChildren = checked) }
    }

    fun onConfirmBulkDelete() {
        val selectedIds = _uiState.value.selectedLocationIds.toList()
        if (selectedIds.isEmpty()) return
        val deleteLinked = _uiState.value.deleteLinkedChildren

        viewModelScope.launch {
            locationRepository.deleteMultiple(selectedIds, deleteLinked)
            val count = selectedIds.size
            _uiState.update {
                it.copy(
                    isSelectionMode = false,
                    selectedLocationIds = emptySet(),
                    showBulkDeleteDialog = false,
                    deleteLinkedChildren = false,
                    userMessage = "$count lokasi berhasil dihapus"
                )
            }
        }
    }

    fun onMoveToFolder(locationId: Long, folderId: Long?) {
        viewModelScope.launch {
            locationRepository.updateFolder(locationId, folderId)
        }
    }

    fun onStartMock(location: Location) {
        mockLocationManager.start(location)
    }

    fun generateSubMocks(anchor: Location, count: Int) {
        viewModelScope.launch {
            val existing = locationRepository.getChildrenOfAnchor(anchor.id)
            val newMocks = geoEngine.generateRealisticSubMocks(
                anchor = anchor,
                count = count,
                startIndex = existing.size + 1
            )
            locationRepository.saveAll(newMocks)
            _uiState.update { state ->
                state.copy(
                    expandedAnchorIds = state.expandedAnchorIds + anchor.id,
                    userMessage = "Berhasil membuat $count sub-mock untuk ${anchor.name}"
                )
            }
        }
    }
}

