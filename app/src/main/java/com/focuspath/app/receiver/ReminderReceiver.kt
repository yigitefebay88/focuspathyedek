package com.focuspath.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.focuspath.app.MainActivity
import com.focuspath.app.util.ReminderUtil

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isWaterReminder = intent.getBooleanExtra("is_water_reminder", false)
        
        if (isWaterReminder) {
            val prefs = context.getSharedPreferences("focuspath_prefs", Context.MODE_PRIVATE)
            val interval = prefs.getInt("water_reminder_interval", 1)
            ReminderUtil.scheduleWaterReminder(context, interval)

            prefs.edit().putLong("last_water_reminder_time", System.currentTimeMillis()).apply()
        }

        val title = if (isWaterReminder) {
            if (java.util.Locale.getDefault().language == "tr") "Su İçme Vakti! 💧" else "Time to Drink Water! 💧"
        } else {
            intent.getStringExtra("task_title") ?: "Focuspath Hatırlatıcı"
        }
        
        val contentText = if (isWaterReminder) {
            if (java.util.Locale.getDefault().language == "tr") "Sağlığın için bir bardak su içmeyi unutma." else "Don't forget to drink a glass of water for your health."
        } else {
            title
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "focuspath_channel")
            .setSmallIcon(com.focuspath.app.R.drawable.ic_launcher_foregroundd) 
            .setContentTitle(if (isWaterReminder) title else "Görev Vakti! 🎯")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
