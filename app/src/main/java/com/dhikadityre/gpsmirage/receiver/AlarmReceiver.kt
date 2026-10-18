package com.dhikadityre.gpsmirage.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
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
        const val ACTION_SNOOZE_ALARM = "com.dhikadityre.gpsmirage.ACTION_SNOOZE_ALARM"
        const val ACTION_TEST_ALARM = "com.dhikadityre.gpsmirage.ACTION_TEST_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_TITLE = "extra_alarm_title"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_ALARM_SOUND_URI = "extra_alarm_sound_uri"
        const val EXTRA_ALARM_IS_VIBRATE = "extra_alarm_is_vibrate"
        const val EXTRA_ALARM_SNOOZE_MINUTES = "extra_alarm_snooze_minutes"
        const val EXTRA_ALARM_IS_DELETE_AFTER_RINGING = "extra_alarm_is_delete_after_ringing"

        const val CHANNEL_ID_PREFIX = "gps_mirage_reminder_"
        const val NOTIFICATION_ID_BASE = 2000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val title = intent.getStringExtra(EXTRA_ALARM_TITLE).orEmpty()
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL).orEmpty()
        val soundUri = intent.getStringExtra(EXTRA_ALARM_SOUND_URI)
        val isVibrate = intent.getBooleanExtra(EXTRA_ALARM_IS_VIBRATE, true)
        val snoozeMinutes = intent.getIntExtra(EXTRA_ALARM_SNOOZE_MINUTES, 5)
        val isDeleteAfterRinging = intent.getBooleanExtra(EXTRA_ALARM_IS_DELETE_AFTER_RINGING, false)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = if (alarmId > 0) (NOTIFICATION_ID_BASE + alarmId).toInt() else NOTIFICATION_ID_BASE

        when (action) {
            ACTION_SNOOZE_ALARM -> {
                notificationManager.cancel(notificationId)
                if (snoozeMinutes > 0) {
                    alarmManager.scheduleSnoozeAlarm(
                        alarmId = alarmId,
                        title = title,
                        label = label,
                        soundUri = soundUri,
                        isVibrate = isVibrate,
                        snoozeMinutes = snoozeMinutes,
                        isDeleteAfterRinging = isDeleteAfterRinging
                    )
                }
            }
            ACTION_TEST_ALARM -> {
                showNotification(
                    context = context,
                    alarmId = -1L,
                    title = title,
                    label = label,
                    soundUri = soundUri,
                    isVibrate = isVibrate,
                    snoozeMinutes = snoozeMinutes,
                    isDeleteAfterRinging = false
                )
            }
            ACTION_REMINDER_ALARM -> {
                showNotification(
                    context = context,
                    alarmId = alarmId,
                    title = title,
                    label = label,
                    soundUri = soundUri,
                    isVibrate = isVibrate,
                    snoozeMinutes = snoozeMinutes,
                    isDeleteAfterRinging = isDeleteAfterRinging
                )

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
                                    if (alarm.isDeleteAfterRinging) {
                                        repository.deleteAlarm(alarm.id)
                                    } else {
                                        repository.toggleAlarm(alarm.id, false)
                                    }
                                }
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        alarmId: Long,
        title: String,
        label: String,
        soundUri: String?,
        isVibrate: Boolean,
        snoozeMinutes: Int,
        isDeleteAfterRinging: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundKey = when {
            soundUri == "silent" -> "silent"
            soundUri.isNullOrEmpty() -> "default"
            else -> soundUri.hashCode().toString()
        }
        val vibrateKey = if (isVibrate) "vib" else "novib"
        val channelId = "${CHANNEL_ID_PREFIX}${soundKey}_${vibrateKey}"

        val resolvedSoundUri: Uri? = when {
            soundUri == "silent" -> null
            !soundUri.isNullOrEmpty() -> Uri.parse(soundUri)
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        // Buat notification channel untuk Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Pengingat Jadwal GPS Mirage",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi pengingat untuk membuka dan mengaktifkan GPS Mirage"
                    enableLights(true)
                    enableVibration(isVibrate)
                    if (isVibrate) {
                        vibrationPattern = longArrayOf(0, 300, 200, 300)
                    }
                    if (resolvedSoundUri != null) {
                        val audioAttrs = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build()
                        setSound(resolvedSoundUri, audioAttrs)
                    } else {
                        setSound(null, null)
                    }
                }
                notificationManager.createNotificationChannel(channel)
            }
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

        val contentTitle = when {
            title.isNotBlank() -> title
            label.isNotBlank() -> label
            else -> context.getString(R.string.alarm_notification_default_title)
        }

        val contentBody = when {
            title.isNotBlank() && label.isNotBlank() -> label
            else -> context.getString(R.string.alarm_notification_default_body)
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(contentTitle)
            .setContentText(contentBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_launcher,
                context.getString(R.string.alarm_notification_open_app),
                openAppPendingIntent
            )

        if (resolvedSoundUri != null) {
            notificationBuilder.setSound(resolvedSoundUri)
        }
        if (isVibrate) {
            notificationBuilder.setVibrate(longArrayOf(0, 300, 200, 300))
        }

        if (snoozeMinutes > 0) {
            val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_SNOOZE_ALARM
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_ALARM_TITLE, title)
                putExtra(EXTRA_ALARM_LABEL, label)
                putExtra(EXTRA_ALARM_SOUND_URI, soundUri)
                putExtra(EXTRA_ALARM_IS_VIBRATE, isVibrate)
                putExtra(EXTRA_ALARM_SNOOZE_MINUTES, snoozeMinutes)
                putExtra(EXTRA_ALARM_IS_DELETE_AFTER_RINGING, isDeleteAfterRinging)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                if (alarmId > 0) (alarmId + 88888).toInt() else 88888,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val snoozeText = String.format(context.getString(R.string.alarm_action_snooze), snoozeMinutes)
            notificationBuilder.addAction(R.drawable.ic_launcher, snoozeText, snoozePendingIntent)
        }

        val notificationId = if (alarmId > 0) (NOTIFICATION_ID_BASE + alarmId).toInt() else NOTIFICATION_ID_BASE
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
        } catch (_: SecurityException) {
            // Ditangani jika izin POST_NOTIFICATIONS belum diberikan
        }
    }
}
