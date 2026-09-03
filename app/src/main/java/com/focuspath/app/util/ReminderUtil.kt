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
        
        // Exact alarm is better for water re█▏  16 GB
        //pulling 41926ed5f140:  75% ▕█████████████     ▏ 896 MB/1.2 GB   89 MB/s      3sminder, but requires permission check for Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    intervalHours * 3600 * 1000L,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                intervalHours * 3600 * 1000L,
                pendingIntent
            )
        }
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
