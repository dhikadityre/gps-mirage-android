package com.dhikadityre.gpsmirage.feature.tools

import com.dhikadityre.gpsmirage.core.model.ReminderAlarm

data class AlarmSchedulerUiState(
    val alarms: List<ReminderAlarm> = emptyList(),
    val isMasterEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val dialogAlarm: ReminderAlarm? = null,
    val isDialogOpen: Boolean = false,
    val alarmToDelete: ReminderAlarm? = null,
    val userMessage: String? = null,
    val canScheduleExact: Boolean = true,
    val hasNotificationPermission: Boolean = true
)
