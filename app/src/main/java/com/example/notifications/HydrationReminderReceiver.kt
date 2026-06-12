package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HydrationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == "com.example.drinkplus.ACTION_REMIND") {
            // Trigger the notification
            HydrationNotificationHelper.showHydrationReminder(context)

            // Reschedule the next alarm based on user preference
            scheduleNextAlarm(context)
        } else if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Devices restarted, restore alarms
            scheduleNextAlarm(context)
        }
    }

    private fun scheduleNextAlarm(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeUser = db.drinkDao().getActiveUser()
                if (activeUser != null && activeUser.reminderIntervalMinutes > 0) {
                    HydrationNotificationHelper.scheduleReminder(
                        context,
                        activeUser.reminderIntervalMinutes
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
