package com.dhikadityre.gpsmirage.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhikadityre.gpsmirage.core.model.ReminderAlarm
import com.dhikadityre.gpsmirage.data.alarm.ReminderAlarmManager
import com.dhikadityre.gpsmirage.domain.alarm.ReminderAlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmSchedulerViewModel @Inject constructor(
    private val repository: ReminderAlarmRepository,
    private val alarmManager: ReminderAlarmManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmSchedulerUiState())
    val uiState: StateFlow<AlarmSchedulerUiState> = _uiState.asStateFlow()

    init {
        checkSystemCapabilities()
        observeAlarms()
    }

    private fun checkSystemCapabilities() {
        val canExact = alarmManager.canScheduleExact()
        _uiState.update { it.copy(canScheduleExact = canExact) }
    }

    fun updatePermissionStatus(hasNotification: Boolean, canExact: Boolean) {
        _uiState.update {
            it.copy(
                hasNotificationPermission = hasNotification,
                canScheduleExact = canExact
            )
        }
    }

    private fun observeAlarms() {
        viewModelScope.launch {
            repository.getAllAlarms().collect { alarms ->
                val anyEnabled = alarms.isEmpty() || alarms.any { it.isEnabled }
                _uiState.update { state ->
                    state.copy(
                        alarms = alarms,
                        isMasterEnabled = anyEnabled,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleMaster(enableAll: Boolean) {
        viewModelScope.launch {
            repository.setAllAlarmsEnabled(enableAll)
            _uiState.update {
                it.copy(
                    isMasterEnabled = enableAll,
                    userMessage = if (enableAll) "Semua alarm diaktifkan" else "Semua alarm dinonaktifkan"
                )
            }
        }
    }

    fun toggleAlarm(alarmId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(alarmId, isEnabled)
        }
    }

    fun openCreateDialog() {
        _uiState.update {
            it.copy(
                dialogAlarm = ReminderAlarm(
                    label = "",
                    hour = 8,
                    minute = 0,
                    daysOfWeek = setOf(1, 2, 3, 4, 5) // Default Senin - Jumat
                ),
                isDialogOpen = true
            )
        }
    }

    fun openEditDialog(alarm: ReminderAlarm) {
        _uiState.update {
            it.copy(
                dialogAlarm = alarm,
                isDialogOpen = true
            )
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(
                dialogAlarm = null,
                isDialogOpen = false
            )
        }
    }

    fun saveAlarm(label: String, hour: Int, minute: Int, daysOfWeek: Set<Int>) {
        val currentDialogAlarm = _uiState.value.dialogAlarm ?: return
        viewModelScope.launch {
            val trimmedLabel = label.trim().ifEmpty { "Pengingat GPS Mirage" }
            val alarmToSave = currentDialogAlarm.copy(
                label = trimmedLabel,
                hour = hour.coerceIn(0, 23),
                minute = minute.coerceIn(0, 59),
                daysOfWeek = daysOfWeek,
                isEnabled = true
            )

            repository.saveAlarm(alarmToSave)
            _uiState.update {
                it.copy(
                    dialogAlarm = null,
                    isDialogOpen = false,
                    userMessage = "Jadwal alarm berhasil disimpan"
                )
            }
        }
    }

    fun requestDelete(alarm: ReminderAlarm) {
        _uiState.update { it.copy(alarmToDelete = alarm) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(alarmToDelete = null) }
    }

    fun confirmDelete() {
        val toDelete = _uiState.value.alarmToDelete ?: return
        viewModelScope.launch {
            repository.deleteAlarm(toDelete.id)
            _uiState.update {
                it.copy(
                    alarmToDelete = null,
                    userMessage = "Alarm berhasil dihapus"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
