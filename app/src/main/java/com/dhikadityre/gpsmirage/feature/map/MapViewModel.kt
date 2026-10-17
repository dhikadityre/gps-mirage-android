package com.dhikadityre.gpsmirage.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.History
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.domain.history.HistoryRepository
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import com.dhikadityre.gpsmirage.domain.location.MockLocationManager
import com.dhikadityre.gpsmirage.domain.location.MockStartResult
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val mockLocationManager: MockLocationManager,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var activeHistorySessionId: Long? = null

    init {
        observeMockState()
        observeRadiusConfig()
        loadRecentLocations()
        loadFavoriteLocations()
    }

    private fun observeRadiusConfig() {
        viewModelScope.launch {
            settingsRepository.radiusConfig.collectLatest { config ->
                _uiState.update { it.copy(radiusConfig = config) }
            }
        }
    }

    private fun observeMockState() {
        viewModelScope.launch {
            mockLocationManager.isRunning.collectLatest { running ->
                _uiState.update { it.copy(isMockActive = running) }
            }
        }
        viewModelScope.launch {
            mockLocationManager.activeLocation.collectLatest { loc ->
                _uiState.update { it.copy(activeMockLocation = loc) }
            }
        }
    }

    private fun loadRecentLocations() {
        viewModelScope.launch {
            try {
                val recent = locationRepository.getRecent(5)
                _uiState.update { it.copy(recentLocations = recent) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun loadFavoriteLocations() {
        viewModelScope.launch {
            locationRepository.getFavoritesFlow()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collectLatest { favorites ->
                    _uiState.update { it.copy(favoriteLocations = favorites) }
                }
        }
    }

    fun onMapTap(lat: Double, lng: Double) {
        _uiState.update { ui ->
            ui.copy(
                selectedCoordinate = Coordinate(latitude = lat, longitude = lng),
                selectedLocationName = ""
            )
        }
    }

    fun onStartMock() {
        val coordinate = _uiState.value.selectedCoordinate ?: return
        val name = _uiState.value.selectedLocationName.ifBlank { "Unnamed Location" }

        val location = Location(
            name = name,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude
        )

        executeStartMock(location)
    }

    private fun executeStartMock(location: Location) {
        _uiState.update { it.copy(isLoading = true) }
        when (val result = mockLocationManager.start(location)) {
            is MockStartResult.Success -> {
                viewModelScope.launch {
                    val historyId = historyRepository.startSession(
                        History(
                            locationName = location.name,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            startedAt = System.currentTimeMillis()
                        )
                    )
                    activeHistorySessionId = historyId
                    _uiState.update { ui ->
                        ui.copy(
                            isLoading = false,
                            event = MapEvent.MockStarted
                        )
                    }
                }
            }
            is MockStartResult.NotConfigured -> {
                _uiState.update { ui ->
                    ui.copy(isLoading = false, event = MapEvent.NavigateToSetupGuide)
                }
            }
            is MockStartResult.PermissionDenied -> {
                _uiState.update { ui ->
                    ui.copy(
                        isLoading = false,
                        event = MapEvent.ShowSnackbar("Permission denied for Mock Location")
                    )
                }
            }
            is MockStartResult.Error -> {
                _uiState.update { ui ->
                    ui.copy(
                        isLoading = false,
                        event = MapEvent.ShowSnackbar(result.message)
                    )
                }
            }
        }
    }

    fun onStopMock() {
        _uiState.update { it.copy(isLoading = true) }
        mockLocationManager.stop()

        activeHistorySessionId?.let { id ->
            val now = System.currentTimeMillis()
            viewModelScope.launch {
                historyRepository.stopSession(id, now, 0L)
                activeHistorySessionId = null
            }
        }

        _uiState.update { ui ->
            ui.copy(
                isLoading = false,
                event = MapEvent.MockStopped
            )
        }
    }

    fun onSaveLocation(name: String) {
        val coordinate = _uiState.value.selectedCoordinate ?: return
        val locationName = name.ifBlank { "Unnamed Location" }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                locationRepository.save(
                    Location(
                        name = locationName,
                        latitude = coordinate.latitude,
                        longitude = coordinate.longitude
                    )
                )
                loadRecentLocations()
                _uiState.update { ui ->
                    ui.copy(
                        isLoading = false,
                        event = MapEvent.ShowSnackbar("Location saved: $locationName")
                    )
                }
            } catch (e: Exception) {
                _uiState.update { ui ->
                    ui.copy(
                        isLoading = false,
                        event = MapEvent.ShowSnackbar(e.message ?: "Failed to save location")
                    )
                }
            }
        }
    }

    fun onQuickLocationTap(location: Location) {
        _uiState.update { ui ->
            ui.copy(
                selectedCoordinate = Coordinate(location.latitude, location.longitude),
                selectedLocationName = location.name,
                isQuickLocationsVisible = false
            )
        }
        executeStartMock(location)
    }

    fun toggleQuickLocations() {
        _uiState.update { it.copy(isQuickLocationsVisible = !it.isQuickLocationsVisible) }
    }

    fun clearEvent() {
        _uiState.update { it.copy(event = null) }
    }
}
