package com.dhikadityre.gpsmirage.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.geo.UnitConverter
import com.dhikadityre.gpsmirage.core.model.DefaultValueProfile
import com.dhikadityre.gpsmirage.core.sensor.CompassSensorManager
import com.dhikadityre.gpsmirage.domain.preset.DefaultProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DefaultProfileFormState(
    val id: Long = 0L,
    val name: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val radiusMeters: String = "50",
    val accuracyMeters: String = "23.77",
    val accuracyFeet: String = "78.00",
    val altitudeMeters: String = "27.13",
    val altitudeFeet: String = "89.00",
    val speedKmh: String = "0",
    val bearingDegrees: String = "0.0",
    val useDeviceSensor: Boolean = true,
    val isDefault: Boolean = false,
    val error: String? = null
)

data class DefaultValuesUiState(
    val profiles: List<DefaultValueProfile> = emptyList(),
    val activeProfile: DefaultValueProfile? = null,
    val currentSensorBearing: Float = 0f,
    val isCompassAvailable: Boolean = false,
    val formState: DefaultProfileFormState? = null,
    val userMessage: String? = null
)

@HiltViewModel
class DefaultValuesViewModel @Inject constructor(
    private val repository: DefaultProfileRepository,
    private val compassSensorManager: CompassSensorManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DefaultValuesUiState(isCompassAvailable = compassSensorManager.isCompassAvailable())
    )
    val uiState: StateFlow<DefaultValuesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultSeeded()
        }
        viewModelScope.launch {
            repository.allProfiles.collect { profiles ->
                _uiState.update { it.copy(profiles = profiles) }
            }
        }
        viewModelScope.launch {
            repository.activeProfile.collect { active ->
                _uiState.update { it.copy(activeProfile = active) }
            }
        }
        if (compassSensorManager.isCompassAvailable()) {
            viewModelScope.launch {
                compassSensorManager.observeBearing().collect { bearing ->
                    _uiState.update { it.copy(currentSensorBearing = bearing) }
                }
            }
        }
    }

    fun openCreateDialog() {
        val sensorBearing = _uiState.value.currentSensorBearing
        _uiState.update {
            it.copy(
                formState = DefaultProfileFormState(
                    id = 0L,
                    name = "",
                    latitude = "-6.213647",
                    longitude = "106.819969",
                    radiusMeters = "50",
                    accuracyMeters = "23.77",
                    accuracyFeet = "78.00",
                    altitudeMeters = "27.13",
                    altitudeFeet = "89.00",
                    speedKmh = "0",
                    bearingDegrees = if (it.isCompassAvailable) {
                        UnitConverter.formatTwoDecimals(sensorBearing.toDouble())
                    } else "0.0",
                    useDeviceSensor = it.isCompassAvailable,
                    isDefault = false
                )
            )
        }
    }

    fun openEditDialog(profile: DefaultValueProfile) {
        _uiState.update {
            it.copy(
                formState = DefaultProfileFormState(
                    id = profile.id,
                    name = profile.name,
                    latitude = profile.latitude.toString(),
                    longitude = profile.longitude.toString(),
                    radiusMeters = profile.radiusMeters.toInt().toString(),
                    accuracyMeters = UnitConverter.formatTwoDecimals(profile.accuracyMeters),
                    accuracyFeet = UnitConverter.formatTwoDecimals(profile.accuracyFeet),
                    altitudeMeters = UnitConverter.formatTwoDecimals(profile.altitudeMeters),
                    altitudeFeet = UnitConverter.formatTwoDecimals(profile.altitudeFeet),
                    speedKmh = profile.speedKmh.toInt().toString(),
                    bearingDegrees = UnitConverter.formatTwoDecimals(profile.bearingDegrees),
                    useDeviceSensor = profile.useDeviceSensor,
                    isDefault = profile.isDefault
                )
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(formState = null) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(formState = it.formState?.copy(name = name, error = null)) }
    }

    fun onLatitudeChanged(lat: String) {
        _uiState.update { it.copy(formState = it.formState?.copy(latitude = lat, error = null)) }
    }

    fun onLongitudeChanged(lng: String) {
        _uiState.update { it.copy(formState = it.formState?.copy(longitude = lng, error = null)) }
    }

    fun onRadiusChanged(radius: String) {
        _uiState.update { it.copy(formState = it.formState?.copy(radiusMeters = radius, error = null)) }
    }

    fun onAccuracyMetersChanged(metersStr: String) {
        val meters = metersStr.toDoubleOrNull()
        val feetStr = if (meters != null && meters >= 0) {
            UnitConverter.formatTwoDecimals(UnitConverter.metersToFeet(meters))
        } else {
            _uiState.value.formState?.accuracyFeet ?: ""
        }
        _uiState.update {
            it.copy(formState = it.formState?.copy(accuracyMeters = metersStr, accuracyFeet = feetStr, error = null))
        }
    }

    fun onAccuracyFeetChanged(feetStr: String) {
        val feet = feetStr.toDoubleOrNull()
        val metersStr = if (feet != null && feet >= 0) {
            UnitConverter.formatTwoDecimals(UnitConverter.feetToMeters(feet))
        } else {
            _uiState.value.formState?.accuracyMeters ?: ""
        }
        _uiState.update {
            it.copy(formState = it.formState?.copy(accuracyFeet = feetStr, accuracyMeters = metersStr, error = null))
        }
    }

    fun onAltitudeMetersChanged(metersStr: String) {
        val meters = metersStr.toDoubleOrNull()
        val feetStr = if (meters != null) {
            UnitConverter.formatTwoDecimals(UnitConverter.metersToFeet(meters))
        } else {
            _uiState.value.formState?.altitudeFeet ?: ""
        }
        _uiState.update {
            it.copy(formState = it.formState?.copy(altitudeMeters = metersStr, altitudeFeet = feetStr, error = null))
        }
    }

    fun onAltitudeFeetChanged(feetStr: String) {
        val feet = feetStr.toDoubleOrNull()
        val metersStr = if (feet != null) {
            UnitConverter.formatTwoDecimals(UnitConverter.feetToMeters(feet))
        } else {
            _uiState.value.formState?.altitudeMeters ?: ""
        }
        _uiState.update {
            it.copy(formState = it.formState?.copy(altitudeFeet = feetStr, altitudeMeters = metersStr, error = null))
        }
    }

    fun onSpeedChanged(speed: String) {
        _uiState.update { it.copy(formState = it.formState?.copy(speedKmh = speed, error = null)) }
    }

    fun onBearingChanged(bearing: String) {
        _uiState.update { it.copy(formState = it.formState?.copy(bearingDegrees = bearing, error = null)) }
    }

    fun onUseDeviceSensorToggled(enabled: Boolean) {
        val currentSensor = _uiState.value.currentSensorBearing
        _uiState.update { state ->
            val updatedBearing = if (enabled) {
                UnitConverter.formatTwoDecimals(currentSensor.toDouble())
            } else {
                state.formState?.bearingDegrees ?: "0.0"
            }
            state.copy(
                formState = state.formState?.copy(
                    useDeviceSensor = enabled,
                    bearingDegrees = updatedBearing,
                    error = null
                )
            )
        }
    }

    fun saveForm() {
        val form = _uiState.value.formState ?: return

        if (form.name.isBlank()) {
            _uiState.update { it.copy(formState = it.formState?.copy(error = "Nama preset tidak boleh kosong")) }
            return
        }
        val lat = form.latitude.toDoubleOrNull()
        if (lat == null || lat < -90.0 || lat > 90.0) {
            _uiState.update { it.copy(formState = it.formState?.copy(error = "Latitude harus di antara -90 dan 90")) }
            return
        }
        val lng = form.longitude.toDoubleOrNull()
        if (lng == null || lng < -180.0 || lng > 180.0) {
            _uiState.update { it.copy(formState = it.formState?.copy(error = "Longitude harus di antara -180 dan 180")) }
            return
        }
        val radius = form.radiusMeters.toDoubleOrNull()
        if (radius == null || radius <= 0) {
            _uiState.update { it.copy(formState = it.formState?.copy(error = "Radius harus berupa angka positif")) }
            return
        }
        val accuracy = form.accuracyMeters.toDoubleOrNull() ?: 23.77
        val altitude = form.altitudeMeters.toDoubleOrNull() ?: 27.13
        val speed = form.speedKmh.toDoubleOrNull() ?: 0.0
        val bearing = form.bearingDegrees.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            val profile = DefaultValueProfile(
                id = form.id,
                name = form.name.trim(),
                latitude = lat,
                longitude = lng,
                radiusMeters = radius,
                accuracyMeters = accuracy,
                altitudeMeters = altitude,
                speedKmh = speed,
                bearingDegrees = UnitConverter.normalizeBearing(bearing),
                useDeviceSensor = form.useDeviceSensor,
                isDefault = form.isDefault
            )
            repository.saveProfile(profile)
            _uiState.update {
                it.copy(
                    formState = null,
                    userMessage = "Preset '${profile.name}' berhasil disimpan"
                )
            }
        }
    }

    fun setActiveProfile(id: Long) {
        viewModelScope.launch {
            repository.setActiveProfile(id)
            _uiState.update { it.copy(userMessage = "Preset aktif berhasil diubah") }
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            repository.deleteProfile(id)
            _uiState.update { it.copy(userMessage = "Preset berhasil dihapus") }
        }
    }

    fun resetOriginalPreset() {
        viewModelScope.launch {
            repository.resetOriginalPreset()
            _uiState.update { it.copy(userMessage = "Preset 'original' berhasil dikembalikan ke pengaturan awal") }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
