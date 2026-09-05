package com.focuspath.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.focuspath.app.receiver.ReminderReceiver

object ReminderUtil {
    const val WATER_REMINDER_ID = 1001

    fun scheduleWaterReminder(context: Context, intervalHours: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("is_water_reminder", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, WATER_REMINDER_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (intervalHours * 3600 * 1000L)
        
        // Use inexact repeating to save battery. Water reminder doesn't need second-precision.
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            intervalHours * 3600 * 1000L,
            pendingIntent
        )
    }

    fun cancelWaterReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("is_water_reminder", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, WATER_REMINDER_ID, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
