package com.dhikadityre.gpsmirage.domain.alarm

import com.dhikadityre.gpsmirage.core.model.ReminderAlarm
import kotlinx.coroutines.flow.Flow

interface ReminderAlarmRepository {
    fun getAllAlarms(): Flow<List<ReminderAlarm>>
    suspend fun getAlarmById(id: Long): ReminderAlarm?
    suspend fun getActiveAlarms(): List<ReminderAlarm>
    suspend fun saveAlarm(alarm: ReminderAlarm): Long
    suspend fun deleteAlarm(id: Long)
    suspend fun toggleAlarm(id: Long, isEnabled: Boolean)
    suspend fun setAllAlarmsEnabled(isEnabled: Boolean)
    suspend fun rescheduleAllActiveAlarms()
    suspend fun count(): Int
}
