package com.dhikadityre.gpsmirage.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.dhikadityre.gpsmirage.core.model.ReminderAlarm
import com.dhikadityre.gpsmirage.domain.alarm.AlarmTimeCalculator
import com.dhikadityre.gpsmirage.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ReminderAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context?
) {
    constructor() : this(null)

    private val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    open fun scheduleAlarm(alarm: ReminderAlarm) {
        val ctx = context ?: return
        if (!alarm.isEnabled || alarmManager == null) return

        val triggerMillis = AlarmTimeCalculator.calculateNextTriggerMillis(
            nowMillis = System.currentTimeMillis(),
            hour = alarm.hour,
            minute = alarm.minute,
            daysOfWeek = alarm.daysOfWeek
        )

        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REMINDER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            ctx,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    AlarmManagerCompat.setExactAndAllowWhileIdle(
                        alarmManager,
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    AlarmManagerCompat.setAndAllowWhileIdle(
                        alarmManager,
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            AlarmManagerCompat.setAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    open fun cancelAlarm(alarmId: Long) {
        val ctx = context ?: return
        if (alarmManager == null) return

        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REMINDER_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            ctx,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    open fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }
}
