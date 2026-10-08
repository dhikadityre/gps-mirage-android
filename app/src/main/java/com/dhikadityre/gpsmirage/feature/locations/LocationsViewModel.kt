package com.dhikadityre.gpsmirage.feature.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.domain.folder.FolderRepository
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import com.dhikadityre.gpsmirage.domain.location.MockLocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val folderRepository: FolderRepository,
    private val mockLocationManager: MockLocationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationsUiState(isLoading = true))
    val uiState: StateFlow<LocationsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            locationRepository.getAllFlow().collect { locs ->
                _uiState.update { it.copy(allLocations = locs, isLoading = false) }
            }
        }
        viewModelScope.launch {
            locationRepository.getFavoritesFlow().collect { favs ->
                _uiState.update { it.copy(favorites = favs) }
            }
        }
        viewModelScope.launch {
            folderRepository.getAllFlow().collect { folders ->
                _uiState.update { it.copy(folders = folders) }
            }
        }
    }

    fun onTabSelected(tab: LocationsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
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

    fun onMoveToFolder(locationId: Long, folderId: Long?) {
        viewModelScope.launch {
            locationRepository.updateFolder(locationId, folderId)
        }
    }

    fun onStartMock(location: Location) {
        mockLocationManager.start(location)
    }
}
