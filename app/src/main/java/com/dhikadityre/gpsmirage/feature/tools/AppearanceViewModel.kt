package com.dhikadityre.gpsmirage.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.model.AppearanceConfig
import com.dhikadityre.gpsmirage.core.model.AppLanguage
import com.dhikadityre.gpsmirage.core.model.AppThemeMode
import com.dhikadityre.gpsmirage.core.model.MapLayerStyle
import com.dhikadityre.gpsmirage.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppearanceUiState(
    val config: AppearanceConfig = AppearanceConfig(),
    val message: String? = null
)

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppearanceUiState())
    val uiState: StateFlow<AppearanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.appearanceConfig.collectLatest { config ->
                _uiState.update { it.copy(config = config) }
            }
        }
    }

    fun setMapStyle(style: MapLayerStyle) {
        viewModelScope.launch {
            settingsRepository.updateMapStyle(style)
        }
    }

    fun setShowStatusBarNotification(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowStatusBarNotification(show)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.updateLanguage(language)
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(themeMode)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
