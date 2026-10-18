package com.dhikadityre.gpsmirage.feature.tools.baseline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.core.model.LocationType
import com.dhikadityre.gpsmirage.domain.baseline.NativeLocationData
import com.dhikadityre.gpsmirage.domain.baseline.NativeLocationProvider
import com.dhikadityre.gpsmirage.domain.location.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class NativeGpsUiState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float = 0f,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val radiusMeters: Double = 500.0,
    val isAcquiring: Boolean = true,
    val isSaved: Boolean = false,
    val showSaveDialog: Boolean = false,
    val saveLocationName: String = "",
    val message: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class NativeGpsViewModel @Inject constructor(
    private val nativeLocationProvider: NativeLocationProvider,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NativeGpsUiState())
    val uiState: StateFlow<NativeGpsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val last = nativeLocationProvider.getLastKnownLocation()
            if (last != null) {
                applyLocationData(last)
            }
            nativeLocationProvider.locationFlow.collect { data ->
                applyLocationData(data)
            }
        }
    }

    private fun applyLocationData(data: NativeLocationData) {
        _uiState.update {
            it.copy(
                latitude = data.latitude,
                longitude = data.longitude,
                accuracy = data.accuracy,
                altitude = data.altitude,
                speed = data.speed,
                bearing = data.bearing,
                isAcquiring = false
            )
        }
    }

    fun onRadiusChanged(radius: Double) {
        _uiState.update { it.copy(radiusMeters = radius) }
    }

    fun onOpenSaveDialog() {
        val lat = _uiState.value.latitude
        val lng = _uiState.value.longitude
        if (lat == null || lng == null) {
            _uiState.update { it.copy(errorMessage = "Belum ada koordinat fisik yang terdeteksi.") }
            return
        }
        val timestamp = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
        _uiState.update {
            it.copy(
                showSaveDialog = true,
                saveLocationName = "Native GPS ($timestamp)"
            )
        }
    }

    fun onDismissSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = false) }
    }

    fun onSaveLocationNameChanged(name: String) {
        _uiState.update { it.copy(saveLocationName = name) }
    }

    fun onConfirmSaveLocation() {
        val state = _uiState.value
        val lat = state.latitude ?: return
        val lng = state.longitude ?: return
        val name = state.saveLocationName.ifBlank { "Native GPS Benchmark" }

        viewModelScope.launch {
            val location = Location(
                name = name,
                latitude = lat,
                longitude = lng,
                type = LocationType.NATIVE,
                accuracy = state.accuracy,
                altitude = state.altitude,
                speed = state.speed,
                bearing = state.bearing,
                radiusMeters = state.radiusMeters
            )
            locationRepository.save(location)
            _uiState.update {
                it.copy(
                    isSaved = true,
                    showSaveDialog = false,
                    message = "Lokasi Native \"$name\" berhasil disimpan!"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }
}
