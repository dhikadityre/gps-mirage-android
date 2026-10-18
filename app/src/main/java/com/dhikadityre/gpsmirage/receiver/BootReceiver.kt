package com.dhikadityre.gpsmirage.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dhikadityre.gpsmirage.domain.alarm.ReminderAlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ReminderAlarmRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.rescheduleAllActiveAlarms()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
