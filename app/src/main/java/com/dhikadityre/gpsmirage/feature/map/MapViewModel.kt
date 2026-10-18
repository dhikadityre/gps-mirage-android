package com.dhikadityre.gpsmirage.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.geo.GeoEngine
import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.History
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.RadiusConfig
import com.dhikadityre.gpsmirage.domain.history.HistoryRepository
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import com.dhikadityre.gpsmirage.domain.location.MockLocationManager
import com.dhikadityre.gpsmirage.domain.location.MockStartResult
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.dhikadityre.gpsmirage.core.geo.ReverseGeocoder
import com.dhikadityre.gpsmirage.domain.location.RewindCoordinator

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val mockLocationManager: MockLocationManager,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val geoEngine: GeoEngine,
    private val reverseGeocoder: ReverseGeocoder,
    private val rewindCoordinator: RewindCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var activeHistorySessionId: Long? = null
    private var addressResolutionJob: Job? = null

    init {
        rewindCoordinator.setCenterAnchor(RadiusConfig.MAYAPADA_TOWER)
        observeMockState()
        observeRadiusConfig()
        loadRecentLocations()
        loadFavoriteLocations()
        resolveAddress(RadiusConfig.MAYAPADA_TOWER, withDelay = false)
    }

    private fun calculateRadiusValidation(
        coordinate: Coordinate?,
        config: RadiusConfig
    ): Pair<Double?, Boolean> {
        if (coordinate == null) return null to true
        if (!config.isEnabled || config.anchor == null) {
            return null to true
        }
        val distance = geoEngine.distance(config.anchor, coordinate)
        val isWithin = distance <= config.radiusMeters
        return distance to isWithin
    }

    private fun observeRadiusConfig() {
        viewModelScope.launch {
            settingsRepository.radiusConfig.collectLatest { config ->
                config.anchor?.let { rewindCoordinator.setCenterAnchor(it) }
                _uiState.update { ui ->
                    val (dist, isWithin) = calculateRadiusValidation(ui.selectedCoordinate, config)
                    ui.copy(
                        radiusConfig = config,
                        distanceToAnchorMeters = dist,
                        isWithinRadius = isWithin,
                        isRewindEnabled = rewindCoordinator.isRewindAvailable()
                    )
                }
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

    fun setSelectionMode(mode: MapSelectionMode) {
        _uiState.update { it.copy(selectionMode = mode) }
    }

    fun togglePositionLock() {
        val newLocked = !_uiState.value.isPositionLocked
        if (newLocked) {
            _uiState.value.selectedCoordinate?.let { rewindCoordinator.onLockPosition(it) }
        } else {
            rewindCoordinator.onUnlockPosition()
        }
        _uiState.update {
            it.copy(
                isPositionLocked = newLocked,
                isRewindEnabled = rewindCoordinator.isRewindAvailable()
            )
        }
    }

    fun clearSelectedCoordinate() {
        addressResolutionJob?.cancel()
        _uiState.update {
            it.copy(
                selectedCoordinate = null,
                selectedLocationName = "",
                resolvedShortAddress = null,
                isResolvingAddress = false,
                distanceToAnchorMeters = null,
                isWithinRadius = true
            )
        }
    }

    fun onMapCenterMoved(lat: Double, lng: Double) {
        val currentState = _uiState.value
        if (currentState.isPositionLocked || currentState.selectionMode != MapSelectionMode.CENTER_PIN) {
            return
        }

        // Guard: Ignore uninitialized / default (0.0, 0.0) coordinates from OSMDroid
        if (Math.abs(lat) < 0.00001 && Math.abs(lng) < 0.00001) {
            return
        }

        val selected = Coordinate(latitude = lat, longitude = lng)
        rewindCoordinator.onMapMoved(selected)
        val (dist, isWithin) = calculateRadiusValidation(selected, currentState.radiusConfig)

        _uiState.update { ui ->
            ui.copy(
                selectedCoordinate = selected,
                selectedLocationName = "",
                resolvedShortAddress = null,
                isResolvingAddress = true,
                distanceToAnchorMeters = dist,
                isWithinRadius = isWithin,
                isRewindEnabled = rewindCoordinator.isRewindAvailable()
            )
        }

        resolveAddress(selected, withDelay = true)
    }

    fun onMapTap(lat: Double, lng: Double) {
        val currentState = _uiState.value
        if (currentState.isPositionLocked || currentState.selectionMode != MapSelectionMode.TAP_TO_SELECT) {
            return
        }

        // Guard: Ignore uninitialized (0.0, 0.0) coordinates
        if (Math.abs(lat) < 0.00001 && Math.abs(lng) < 0.00001) {
            return
        }

        val selected = Coordinate(latitude = lat, longitude = lng)
        rewindCoordinator.onMapMoved(selected)
        val (dist, isWithin) = calculateRadiusValidation(selected, currentState.radiusConfig)

        _uiState.update { ui ->
            ui.copy(
                selectedCoordinate = selected,
                selectedLocationName = "",
                resolvedShortAddress = null,
                isResolvingAddress = true,
                distanceToAnchorMeters = dist,
                isWithinRadius = isWithin,
                isRewindEnabled = rewindCoordinator.isRewindAvailable()
            )
        }

        resolveAddress(selected, withDelay = false)
    }

    private fun resolveAddress(coordinate: Coordinate, withDelay: Boolean = true) {
        addressResolutionJob?.cancel()
        addressResolutionJob = viewModelScope.launch {
            if (withDelay) {
                delay(400L)
            }
            val result = reverseGeocoder.getShortAddress(coordinate)
            _uiState.update { ui ->
                if (ui.selectedCoordinate == coordinate) {
                    ui.copy(
                        isResolvingAddress = false,
                        resolvedShortAddress = result?.shortAddress,
                        selectedLocationName = result?.shortAddress ?: ""
                    )
                } else {
                    ui
                }
            }
        }
    }

    fun onStartMock() {
        val coordinate = _uiState.value.selectedCoordinate ?: return
        if (!_uiState.value.isWithinRadius && _uiState.value.radiusConfig.isEnabled) {
            _uiState.update {
                it.copy(event = MapEvent.ShowSnackbar("Lokasi berada di luar batas radius"))
            }
            return
        }
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
                rewindCoordinator.onMockStarted(Coordinate(location.latitude, location.longitude))
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
                            isRewindEnabled = rewindCoordinator.isRewindAvailable(),
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
        addressResolutionJob?.cancel()
        val coordinate = Coordinate(location.latitude, location.longitude)
        val (dist, isWithin) = calculateRadiusValidation(coordinate, _uiState.value.radiusConfig)
        _uiState.update { ui ->
            ui.copy(
                selectedCoordinate = coordinate,
                selectedLocationName = location.name,
                resolvedShortAddress = location.name,
                isResolvingAddress = false,
                distanceToAnchorMeters = dist,
                isWithinRadius = isWithin,
                isQuickLocationsVisible = false
            )
        }
        if (!isWithin && _uiState.value.radiusConfig.isEnabled) {
            _uiState.update {
                it.copy(event = MapEvent.ShowSnackbar("Lokasi berada di luar batas radius"))
            }
            return
        }
        executeStartMock(location)
    }

    fun toggleQuickLocations() {
        _uiState.update { it.copy(isQuickLocationsVisible = !it.isQuickLocationsVisible) }
    }

    fun onRewindClick() {
        val target = rewindCoordinator.executeRewindStep() ?: return
        val currentLocked = _uiState.value.isPositionLocked
        val shouldUnlock = target.requiresUnlock && currentLocked
        if (shouldUnlock) {
            rewindCoordinator.onUnlockPosition()
        }
        val targetCoord = target.coordinate
        rewindCoordinator.onMapMoved(targetCoord)
        val (dist, isWithin) = calculateRadiusValidation(targetCoord, _uiState.value.radiusConfig)

        _uiState.update { ui ->
            ui.copy(
                selectedCoordinate = targetCoord,
                selectedLocationName = "",
                resolvedShortAddress = null,
                isResolvingAddress = true,
                distanceToAnchorMeters = dist,
                isWithinRadius = isWithin,
                isPositionLocked = if (shouldUnlock) false else currentLocked,
                isRewindEnabled = rewindCoordinator.isRewindAvailable(),
                event = MapEvent.AnimateCamera(targetCoord)
            )
        }
        resolveAddress(targetCoord, withDelay = false)
    }

    fun onRecenterToAnchor() {
        val anchor = _uiState.value.radiusConfig.anchor ?: RadiusConfig.MAYAPADA_TOWER
        rewindCoordinator.onMapMoved(anchor)
        val (dist, isWithin) = calculateRadiusValidation(anchor, _uiState.value.radiusConfig)
        _uiState.update { ui ->
            ui.copy(
                selectedCoordinate = anchor,
                selectedLocationName = "",
                resolvedShortAddress = null,
                isResolvingAddress = true,
                distanceToAnchorMeters = dist,
                isWithinRadius = isWithin,
                isRewindEnabled = rewindCoordinator.isRewindAvailable(),
                event = MapEvent.AnimateCamera(anchor)
            )
        }
        resolveAddress(anchor, withDelay = false)
    }

    fun clearEvent() {
        _uiState.update { it.copy(event = null) }
    }
}
