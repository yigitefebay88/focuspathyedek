package com.focuspath.app.service

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.focuspath.app.MainActivity
import com.focuspath.app.R
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.*
import java.util.*

class FocusService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private var timerJob: Job? = null
    private var isPomodoro = true
    private var timeLeftMillis = 0L
    private var timeElapsedMillis = 0L
    private var completionPlayer: android.media.MediaPlayer? = null
    
    companion object {
        const val CHANNEL_ID = "focus_service_channel"
        const val NOTIFICATION_ID = 101
        
        // Actions
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        
        // Extras
        const val EXTRA_IS_POMODORO = "EXTRA_IS_POMODORO"
        const val EXTRA_DURATION = "EXTRA_DURATION"
        
        var isRunning = false
        var currentTime = 0L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("focuspath_prefs", android.content.Context.MODE_PRIVATE)
        
        // KRİTİK: startForegroundService() çağrısından sonra ilk fırsatta çağrılmalı.
        startForeground(NOTIFICATION_ID, createNotification("Sistem Hazırlanıyor..."))

        when (intent?.action) {
            ACTION_START -> {
                isPomodoro = intent.getBooleanExtra(EXTRA_IS_POMODORO, true)
                val duration = intent.getLongExtra(EXTRA_DURATION, 25 * 60 * 1000L)
                
                // KALICI KAYIT: Bitiş zamanını kaydet
                val targetEndTime = System.currentTimeMillis() + duration
                prefs.edit()
                    .putLong("TIMER_TARGET_END", targetEndTime)
                    .putBoolean("TIMER_IS_POMODORO", isPomodoro)
                    .putLong("TIMER_INITIAL_DURATION", duration)
                    .apply()

                toggleDnd(true)
                startFocus(duration)
            }
            ACTION_STOP -> {
                prefs.edit().remove("TIMER_TARGET_END").apply() // Kaydı temizle
                toggleDnd(false)
                completionPlayer?.let { try { if(it.isPlaying) it.stop(); it.release() } catch(e: Exception) {} }
                completionPlayer = null
                stopFocus()
            }
            ACTION_PAUSE -> {
                prefs.edit().remove("TIMER_TARGET_END").apply()
                isRunning = false
                timerJob?.cancel()
                updateNotification("DURAKLATILDI - " + formatTime(currentTime))
            }
            ACTION_RESUME -> {
                val duration = intent.getLongExtra(EXTRA_DURATION, timeLeftMillis)
                val targetEndTime = System.currentTimeMillis() + duration
                prefs.edit()
                    .putLong("TIMER_TARGET_END", targetEndTime)
                    .apply()
                startFocus(duration)
            }
            null -> {
                // Servis sistem tarafından yeniden başlatıldıysa (Sticky)
                val targetEnd = prefs.getLong("TIMER_TARGET_END", 0L)
                if (targetEnd > System.currentTimeMillis()) {
                    isPomodoro = prefs.getBoolean("TIMER_IS_POMODORO", true)
                    val remaining = targetEnd - System.currentTimeMillis()
                    startFocus(remaining)
                } else {
                    stopFocus()
                }
            }
        }
        return START_STICKY
    }

    private fun startFocus(duration: Long) {
        timerJob?.cancel() // Mevcut varsa iptal et
        isRunning = true
        timeLeftMillis = duration
        timeElapsedMillis = 0L
        currentTime = if (isPomodoro) timeLeftMillis else 0L
        
        timerJob = serviceScope.launch {
            var lastRecordedMinute = if (isPomodoro) (timeLeftMillis / 60000) else 0L
            
            while (isRunning) {
                delay(1000)
                if (isPomodoro) {
                    timeLeftMillis -= 1000
                    currentTime = timeLeftMillis
                    
                    val currentMinute = timeLeftMillis / 60000
                    if (currentMinute < lastRecordedMinute) {
                        sendUpdateBroadcast(1)
                        lastRecordedMinute = currentMinute
                    }

                    if (timeLeftMillis <= 0) {
                        getSharedPreferences("focuspath_prefs", android.content.Context.MODE_PRIVATE)
                            .edit().remove("TIMER_TARGET_END").apply()
                        isRunning = false
                        
                        sendCompletionBroadcast()
                        showFinalNotification()
                        toggleDnd(false)
                        break
                    }
                } else {
                    timeElapsedMillis += 1000
                    currentTime = timeElapsedMillis
                    
                    val elapsedMinutes = timeElapsedMillis / 60000
                    if (elapsedMinutes > lastRecordedMinute) {
                        sendUpdateBroadcast(1)
                        lastRecordedMinute = elapsedMinutes
                    }
                }
                
                // Widget'ı güncelle
                if (currentTime % 10000 == 0L) {
                    serviceScope.launch {
                        try {
                            com.focuspath.app.widget.PomoWidget().updateAll(applicationContext)
                        } catch (e: Exception) {}
                    }
                }
                
                updateNotification(formatTime(currentTime))
            }
        }
    }

    private fun stopFocus() {
        isRunning = false
        timerJob?.cancel()
        stopForeground(true)
        stopSelf()
    }

    private fun toggleDnd(enable: Boolean) {
        val prefs = getSharedPreferences("focuspath_prefs", android.content.Context.MODE_PRIVATE)
        val isAutoDnd = prefs.getBoolean("is_auto_dnd_enabled", false)
        if (!isAutoDnd) return

        val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
                if (enable) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                else nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (e: Exception) {}
    }

    private fun createNotification(content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FocusPath - Derin Odak")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(time: String) {
        val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, createNotification("Kalan Süre: $time"))
    }

    private fun showFinalNotification() {
        val prefs = getSharedPreferences("focuspath_prefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_notification_enabled", true)) return

        val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
        val finalNotification = NotificationCompat.Builder(this, "focuspath_channel") 
            .setContentTitle("Seans Tamamlandı! 🎉")
            .setContentText("Harika bir iş çıkardın. Dinlenmeyi hak ettin!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .build()
            
        nm.notify(NOTIFICATION_ID + 1, finalNotification)

        val volume = prefs.getFloat("alarm_volume", 0.5f)
        val soundType = prefs.getString("alarm_sound", "default") ?: "default"
        playSoftCompletionSound(volume, soundType)
    }

    private fun playSoftCompletionSound(volume: Float, soundType: String) {
        try {
            completionPlayer?.let { try { it.stop(); it.release() } catch(e: Exception) {} }
            val alarmUri = when(soundType) {
                "beep" -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                "alarm" -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                else -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            }
            completionPlayer = android.media.MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).build())
                setVolume(volume, volume) 
                prepare()
                start()
            }
            completionPlayer?.setOnCompletionListener { 
                try { it.release() } catch(e: Exception) {}
                if (completionPlayer == it) completionPlayer = null
            }
            
            // 10 saniye sonra otomatik durdur
            serviceScope.launch {
                delay(10000)
                completionPlayer?.let {
                    try {
                        if (it.isPlaying) it.stop()
                        it.release()
                    } catch(e: Exception) {}
                    completionPlayer = null
                }
            }
        } catch (e: Exception) {}
    }

    private fun formatTime(millis: Long): String {
        val s = (millis / 1000) % 60 ; val m = (millis / (1000 * 60)) % 60 ; val h = (millis / (1000 * 60 * 60))
        return if (h > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Focus Timer Service", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun sendUpdateBroadcast(minutes: Int) {
        sendBroadcast(Intent("com.focuspath.TIMER_UPDATE").apply { putExtra("minutes", minutes) })
    }

    private fun sendCompletionBroadcast() {
        val intent = Intent("com.focuspath.TIMER_FINISHED").apply {
            setPackage(packageName) // Sadece bu uygulama yakalasın
        }
        sendBroadcast(intent)
        android.util.Log.d("FocusService", "Completion Broadcast Sent")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        toggleDnd(false)
        completionPlayer?.let { try { if(it.isPlaying) it.stop(); it.release() } catch(e: Exception) {} }
        completionPlayer = null
        serviceJob.cancel()
    }
}
