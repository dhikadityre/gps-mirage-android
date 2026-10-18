package com.dhikadityre.gpsmirage.data.repository

import com.dhikadityre.gpsmirage.core.database.dao.ReminderAlarmDao
import com.dhikadityre.gpsmirage.core.database.entity.ReminderAlarmEntity
import com.dhikadityre.gpsmirage.core.model.ReminderAlarm
import com.dhikadityre.gpsmirage.data.alarm.ReminderAlarmManager
import com.dhikadityre.gpsmirage.domain.alarm.ReminderAlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderAlarmRepositoryImpl @Inject constructor(
    private val dao: ReminderAlarmDao,
    private val alarmManager: ReminderAlarmManager
) : ReminderAlarmRepository {

    override fun getAllAlarms(): Flow<List<ReminderAlarm>> {
        return dao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAlarmById(id: Long): ReminderAlarm? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getActiveAlarms(): List<ReminderAlarm> {
        return dao.getEnabledAlarms().map { it.toDomain() }
    }

    override suspend fun saveAlarm(alarm: ReminderAlarm): Long {
        val now = System.currentTimeMillis()
        val entity = ReminderAlarmEntity.fromDomain(
            alarm.copy(
                createdAt = if (alarm.id == 0L) now else alarm.createdAt,
                updatedAt = now
            )
        )
        val id = if (alarm.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            alarm.id
        }

        val savedAlarm = alarm.copy(id = id)
        if (savedAlarm.isEnabled) {
            alarmManager.scheduleAlarm(savedAlarm)
        } else {
            alarmManager.cancelAlarm(id)
        }
        return id
    }

    override suspend fun deleteAlarm(id: Long) {
        alarmManager.cancelAlarm(id)
        dao.delete(id)
    }

    override suspend fun toggleAlarm(id: Long, isEnabled: Boolean) {
        dao.updateEnabled(id, isEnabled)
        val alarm = dao.getById(id)?.toDomain()
        if (alarm != null) {
            if (isEnabled) {
                alarmManager.scheduleAlarm(alarm)
            } else {
                alarmManager.cancelAlarm(id)
            }
        }
    }

    override suspend fun setAllAlarmsEnabled(isEnabled: Boolean) {
        val allAlarms = dao.getAllList()
        dao.setAllEnabled(isEnabled)
        if (isEnabled) {
            rescheduleAllActiveAlarms()
        } else {
            for (item in allAlarms) {
                alarmManager.cancelAlarm(item.id)
            }
        }
    }

    override suspend fun rescheduleAllActiveAlarms() {
        val activeAlarms = dao.getEnabledAlarms().map { it.toDomain() }
        for (alarm in activeAlarms) {
            alarmManager.scheduleAlarm(alarm)
        }
    }

    override suspend fun count(): Int {
        return dao.count()
    }
}
