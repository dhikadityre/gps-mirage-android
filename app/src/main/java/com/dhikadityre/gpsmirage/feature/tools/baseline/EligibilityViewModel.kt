package com.dhikadityre.gpsmirage.feature.tools.baseline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.domain.baseline.EligibilityReport
import com.dhikadityre.gpsmirage.domain.baseline.MockEligibilityChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EligibilityUiState(
    val report: EligibilityReport? = null,
    val isChecking: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class EligibilityViewModel @Inject constructor(
    private val eligibilityChecker: MockEligibilityChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(EligibilityUiState(isChecking = true))
    val uiState: StateFlow<EligibilityUiState> = _uiState.asStateFlow()

    init {
        refreshEligibility()
    }

    fun refreshEligibility() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true) }
            val result = eligibilityChecker.checkEligibility()
            _uiState.update {
                it.copy(
                    report = result,
                    isChecking = false,
                    message = if (result.isFullyEligible) "Sistem Eligible & Siap Fake GPS" else "Sistem Belum Siap (Perlu Tindakan)"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
