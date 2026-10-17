package com.dhikadityre.gpsmirage.feature.settings

import com.dhikadityre.gpsmirage.core.model.RadiusConfig

data class SettingsUiState(
    val radiusConfig: RadiusConfig = RadiusConfig(),
    val latitudeInput: String = "",
    val longitudeInput: String = "",
    val radiusInput: String = "500",
    val radiusSliderValue: Float = 500f,
    val isDetectingLocation: Boolean = false,
    val coordinateError: String? = null,
    val radiusError: String? = null,
    val message: String? = null
)
