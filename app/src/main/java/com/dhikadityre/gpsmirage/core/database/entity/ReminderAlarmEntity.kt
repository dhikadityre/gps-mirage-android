package com.dhikadityre.gpsmirage.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dhikadityre.gpsmirage.core.model.ReminderAlarm

@Entity(tableName = "reminder_alarms")
data class ReminderAlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val label: String,
    val hour: Int,
    val minute: Int,
    @ColumnInfo(name = "days_of_week")
    val daysOfWeek: Int,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    fun toDomain(): ReminderAlarm = ReminderAlarm(
        id = id,
        label = label,
        hour = hour,
        minute = minute,
        daysOfWeek = ReminderAlarm.bitmaskToDays(daysOfWeek),
        isEnabled = isEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(alarm: ReminderAlarm): ReminderAlarmEntity = ReminderAlarmEntity(
            id = alarm.id,
            label = alarm.label,
            hour = alarm.hour,
            minute = alarm.minute,
            daysOfWeek = ReminderAlarm.daysToBitmask(alarm.daysOfWeek),
            isEnabled = alarm.isEnabled,
            createdAt = alarm.createdAt,
            updatedAt = alarm.updatedAt
        )
    }
}
