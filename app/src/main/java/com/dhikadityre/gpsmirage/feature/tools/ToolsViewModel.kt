package com.dhikadityre.gpsmirage.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.domain.location.MapResetCoordinator
import com.dhikadityre.gpsmirage.domain.location.MockLocationManager
import com.dhikadityre.gpsmirage.domain.location.ResetMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolsUiState(
    val isMockRunning: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val mockLocationManager: MockLocationManager,
    private val mapResetCoordinator: MapResetCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ToolsUiState())
    val uiState: StateFlow<ToolsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mockLocationManager.isRunning.collect { running ->
                _uiState.update { it.copy(isMockRunning = running) }
            }
        }
    }

    fun onSoftReset() {
        mockLocationManager.stop()
        mapResetCoordinator.requestReset(ResetMode.SOFT)
        _uiState.update {
            it.copy(message = "Soft Reset Berhasil: Mock dihentikan & GPS fisik pulih")
        }
    }

    fun onHardReset() {
        mockLocationManager.stop()
        mapResetCoordinator.requestReset(ResetMode.HARD)
        _uiState.update {
            it.copy(message = "Hard Reset Berhasil: Mock dihentikan, GPS fisik pulih, & Map kembali ke kondisi awal")
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
