package com.focuspath.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focuspath.app.util.ReminderUtil

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val prefs = context.getSharedPreferences("focuspath_prefs", Context.MODE_PRIVATE)
            val isWaterReminderEnabled = prefs.getBoolean("is_water_reminder_enabled", false)
            val waterReminderInterval = prefs.getInt("water_reminder_interval", 1)

            if (isWaterReminderEnabled) {
                ReminderUtil.scheduleWaterReminder(context, waterReminderInterval)
            }
        }
    }
}
