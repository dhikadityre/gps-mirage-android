package com.dhikadityre.gpsmirage.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dhikadityre.dev.tools.R
import com.dhikadityre.gpsmirage.MainActivity
import com.dhikadityre.gpsmirage.data.alarm.ReminderAlarmManager
import com.dhikadityre.gpsmirage.domain.alarm.ReminderAlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ReminderAlarmRepository

    @Inject
    lateinit var alarmManager: ReminderAlarmManager

    companion object {
        const val ACTION_REMINDER_ALARM = "com.dhikadityre.gpsmirage.ACTION_REMINDER_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"

        const val CHANNEL_ID = "gps_mirage_reminder_channel"
        const val NOTIFICATION_ID_BASE = 2000
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER_ALARM) return

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Pengingat GPS Mirage"

        showNotification(context, alarmId, alarmLabel)

        if (alarmId > 0) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarm = repository.getAlarmById(alarmId)
                    if (alarm != null && alarm.isEnabled) {
                        if (alarm.daysOfWeek.isNotEmpty()) {
                            // Jadwalkan kemunculan alarm berulang berikutnya
                            alarmManager.scheduleAlarm(alarm)
                        } else {
                            // Alarm sekali saja: otomatis dinonaktifkan setelah berbunyi
                            repository.toggleAlarm(alarm.id, false)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context, alarmId: Long, label: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Buat notification channel untuk Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengingat Jadwal GPS Mirage",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat untuk membuka dan mengaktifkan GPS Mirage"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // PendingIntent untuk membuka MainActivity saat notifikasi atau aksi ditekan
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            if (alarmId > 0) alarmId.toInt() else 0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(if (label.isNotBlank()) label else "Pengingat GPS Mirage")
            .setContentText("Waktunya membuka dan menjalankan aplikasi GPS Mirage.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_launcher,
                "Buka Aplikasi",
                openAppPendingIntent
            )
            .build()

        val notificationId = if (alarmId > 0) (NOTIFICATION_ID_BASE + alarmId).toInt() else NOTIFICATION_ID_BASE
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Ditangani jika izin POST_NOTIFICATIONS belum diberikan
        }
    }
}
