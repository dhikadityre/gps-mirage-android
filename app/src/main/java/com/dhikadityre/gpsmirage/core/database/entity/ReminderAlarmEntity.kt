package com.dhikadityre.gpsmirage.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dhikadityre.gpsmirage.core.model.ReminderAlarm

@Entity(tableName = "reminder_alarms")
data class ReminderAlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "title", defaultValue = "")
    val title: String = "",
    val label: String,
    val hour: Int,
    val minute: Int,
    @ColumnInfo(name = "days_of_week")
    val daysOfWeek: Int,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean,
    @ColumnInfo(name = "is_delete_after_ringing", defaultValue = "0")
    val isDeleteAfterRinging: Boolean = false,
    @ColumnInfo(name = "sound_uri", defaultValue = "NULL")
    val soundUri: String? = null,
    @ColumnInfo(name = "snooze_minutes", defaultValue = "5")
    val snoozeMinutes: Int = 5,
    @ColumnInfo(name = "is_vibrate", defaultValue = "1")
    val isVibrate: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    fun toDomain(): ReminderAlarm = ReminderAlarm(
        id = id,
        title = title,
        label = label,
        hour = hour,
        minute = minute,
        daysOfWeek = ReminderAlarm.bitmaskToDays(daysOfWeek),
        isEnabled = isEnabled,
        isDeleteAfterRinging = isDeleteAfterRinging,
        soundUri = soundUri,
        snoozeMinutes = snoozeMinutes,
        isVibrate = isVibrate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(alarm: ReminderAlarm): ReminderAlarmEntity = ReminderAlarmEntity(
            id = alarm.id,
            title = alarm.title,
            label = alarm.label,
            hour = alarm.hour,
            minute = alarm.minute,
            daysOfWeek = ReminderAlarm.daysToBitmask(alarm.daysOfWeek),
            isEnabled = alarm.isEnabled,
            isDeleteAfterRinging = alarm.isDeleteAfterRinging,
            soundUri = alarm.soundUri,
            snoozeMinutes = alarm.snoozeMinutes,
            isVibrate = alarm.isVibrate,
            createdAt = alarm.createdAt,
            updatedAt = alarm.updatedAt
        )
    }
}
