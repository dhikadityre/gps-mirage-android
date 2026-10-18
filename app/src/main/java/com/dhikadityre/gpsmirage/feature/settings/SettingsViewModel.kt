package com.dhikadityre.gpsmirage.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.location.DeviceLocationProvider
import com.dhikadityre.gpsmirage.core.model.Coordinate
import com.dhikadityre.gpsmirage.core.model.RadiusConfig
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val deviceLocationProvider: DeviceLocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeRadiusConfig()
    }

    private fun observeRadiusConfig() {
        viewModelScope.launch {
            settingsRepository.radiusConfig.collectLatest { config ->
                _uiState.update { current ->
                    current.copy(
                        radiusConfig = config,
                        latitudeInput = config.anchor?.let { String.format(Locale.US, "%.6f", it.latitude) } ?: current.latitudeInput,
                        longitudeInput = config.anchor?.let { String.format(Locale.US, "%.6f", it.longitude) } ?: current.longitudeInput,
                        radiusInput = config.radiusMeters.toInt().toString(),
                        radiusSliderValue = config.radiusMeters.toFloat()
                    )
                }
            }
        }
    }

    fun onRadiusToggleChanged(enabled: Boolean) {
        val updated = _uiState.value.radiusConfig.copy(isEnabled = enabled)
        viewModelScope.launch {
            settingsRepository.updateRadiusConfig(updated)
        }
    }

    fun onLatitudeChanged(latitude: String) {
        _uiState.update { it.copy(latitudeInput = latitude, coordinateError = null) }
    }

    fun onLongitudeChanged(longitude: String) {
        _uiState.update { it.copy(longitudeInput = longitude, coordinateError = null) }
    }

    fun onSaveAnchor() {
        val lat = _uiState.value.latitudeInput.toDoubleOrNull()
        val lng = _uiState.value.longitudeInput.toDoubleOrNull()

        if (lat == null || lng == null) {
            _uiState.update { it.copy(coordinateError = "Koordinat harus berupa angka valid") }
            return
        }

        if (lat < -90.0 || lat > 90.0) {
            _uiState.update { it.copy(coordinateError = "Latitude harus antara -90.0 dan 90.0") }
            return
        }

        if (lng < -180.0 || lng > 180.0) {
            _uiState.update { it.copy(coordinateError = "Longitude harus antara -180.0 dan 180.0") }
            return
        }

        val anchor = Coordinate(lat, lng)
        val updated = _uiState.value.radiusConfig.copy(anchor = anchor)
        viewModelScope.launch {
            settingsRepository.updateRadiusConfig(updated)
            _uiState.update {
                it.copy(
                    coordinateError = null,
                    message = "Titik acuan berhasil disimpan"
                )
            }
        }
    }

    fun onRadiusSliderChanged(sliderValue: Float) {
        val meters = sliderValue.toDouble()
        _uiState.update {
            it.copy(
                radiusSliderValue = sliderValue,
                radiusInput = sliderValue.toInt().toString(),
                radiusError = null
            )
        }
        val updated = _uiState.value.radiusConfig.copy(radiusMeters = meters)
        viewModelScope.launch {
            settingsRepository.updateRadiusConfig(updated)
        }
    }

    fun onRadiusInputChanged(input: String) {
        _uiState.update { it.copy(radiusInput = input, radiusError = null) }
        val meters = input.toDoubleOrNull()
        if (meters != null && meters in 50.0..50000.0) {
            _uiState.update { it.copy(radiusSliderValue = meters.toFloat().coerceIn(100f, 5000f)) }
            val updated = _uiState.value.radiusConfig.copy(radiusMeters = meters)
            viewModelScope.launch {
                settingsRepository.updateRadiusConfig(updated)
            }
        } else if (meters != null && (meters < 50.0 || meters > 50000.0)) {
            _uiState.update { it.copy(radiusError = "Radius harus antara 50m dan 50.000m") }
        }
    }

    fun onUseCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetectingLocation = true, coordinateError = null) }
            val location = deviceLocationProvider.getCurrentLocation()
            if (location != null) {
                val latStr = String.format(Locale.US, "%.6f", location.latitude)
                val lngStr = String.format(Locale.US, "%.6f", location.longitude)
                val updated = _uiState.value.radiusConfig.copy(anchor = location)
                settingsRepository.updateRadiusConfig(updated)
                _uiState.update {
                    it.copy(
                        isDetectingLocation = false,
                        latitudeInput = latStr,
                        longitudeInput = lngStr,
                        coordinateError = null,
                        message = "Lokasi GPS riil perangkat berhasil diterapkan"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isDetectingLocation = false,
                        coordinateError = "Gagal mendeteksi lokasi GPS perangkat. Pastikan izin lokasi aktif."
                    )
                }
            }
        }
    }

    fun onResetToMayapadaTower() {
        val anchor = RadiusConfig.MAYAPADA_TOWER
        val latStr = String.format(Locale.US, "%.6f", anchor.latitude)
        val lngStr = String.format(Locale.US, "%.6f", anchor.longitude)
        val updated = _uiState.value.radiusConfig.copy(anchor = anchor)
        viewModelScope.launch {
            settingsRepository.updateRadiusConfig(updated)
            _uiState.update {
                it.copy(
                    latitudeInput = latStr,
                    longitudeInput = lngStr,
                    coordinateError = null,
                    message = "Titik acuan berhasil direset ke Mayapada Tower"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
