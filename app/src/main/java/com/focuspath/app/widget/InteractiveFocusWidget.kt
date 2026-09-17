package com.focuspath.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.focuspath.app.R
import com.focuspath.app.core.data.local.AppDatabase
import com.focuspath.app.service.FocusService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class InteractiveFocusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("focuspath_prefs", Context.MODE_PRIVATE)
        val coins = prefs.getInt("user_coins", 0)
        val xp = prefs.getInt("user_xp", 0)
        
        val activeTaskTitle = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val tasks = db.taskDaoProvider().getAllTasksOnce()
            tasks.firstOrNull { !it.isCompleted }?.title ?: "Görev Yok"
        }

        val timerText = if (FocusService.isRunning) {
            formatTime(FocusService.currentTime)
        } else {
            "00:00"
        }

        val isRunning = FocusService.isRunning

        provideContent {
            GlanceTheme {
                InteractiveFocusContent(
                    timerText = timerText,
                    isRunning = isRunning,
                    activeTask = activeTaskTitle,
                    coins = coins,
                    xp = xp,
                )
            }
        }
    }

    @Composable
    private fun InteractiveFocusContent(
        timerText: String,
        isRunning: Boolean,
        activeTask: String,
        coins: Int,
        xp: Int,
    ) {
        val green = ColorProvider(androidx.compose.ui.graphics.Color(0xFF00FF41))
        val black = ColorProvider(androidx.compose.ui.graphics.Color(0xFF000000))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(black)
                .padding(12.dp)
                .cornerRadius(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FOCUS_PATH >",
                    style = TextStyle(
                        color = green,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                
                // Refresh Button
                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(actionRunCallback<RefreshAction>())
                ) {
                    Image(
                        provider = ImageProvider(android.R.drawable.stat_notify_sync),
                        contentDescription = "Refresh",
                        modifier = GlanceModifier.fillMaxSize(),
                        colorFilter = ColorFilter.tint(green)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer Section
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = timerText,
                        style = TextStyle(
                            color = green,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = activeTask.uppercase(Locale.getDefault()),
                        maxLines = 1,
                        style = TextStyle(
                            color = green,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // Controls
                Button(
                    text = if (isRunning) "STOP" else "START",
                    onClick = actionRunCallback<ToggleTimerAction>(
                        actionParametersOf(ToggleTimerAction.PARAM_ACTION to if (isRunning) FocusService.ACTION_STOP else FocusService.ACTION_START)
                    ),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = green,
                        contentColor = black
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Stats
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$coins COINS",
                    style = TextStyle(
                        color = green,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "$xp XP",
                    style = TextStyle(
                        color = green,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val s = (millis / 1000) % 60
        val m = (millis / (1000 * 60)) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}

class ToggleTimerAction : ActionCallback {
    companion object {
        val PARAM_ACTION = ActionParameters.Key<String>("action_type")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val action = parameters[PARAM_ACTION] ?: return
        val intent = Intent(context, FocusService::class.java).apply {
            this.action = action
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        
        // Update widget state immediately
        InteractiveFocusWidget().update(context, glanceId)
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        InteractiveFocusWidget().update(context, glanceId)
    }
}

class InteractiveFocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = InteractiveFocusWidget()
}
