package com.focuspath.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.focuspath.app.service.FocusService
import java.util.Locale

class PomoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        val terminalGreen = Color(0xFF00FF41)
        val isRunning = FocusService.isRunning
        val currentTime = FocusService.currentTime
        
        val timeStr = formatTime(currentTime)
        val statusStr = if (isRunning) "ACTIVE" else "IDLE"

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(8.dp)
                .appWidgetBackground(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FOCUS STATUS: $statusStr",
                style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 10.sp)
            )
            
            Text(
                text = timeStr,
                style = TextStyle(
                    color = ColorProvider(terminalGreen),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    private fun formatTime(millis: Long): String {
        val s = (millis / 1000) % 60 ; val m = (millis / (1000 * 60)) % 60 ; val h = (millis / (1000 * 60 * 60))
        return if (h > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}

class PomoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PomoWidget()
}
