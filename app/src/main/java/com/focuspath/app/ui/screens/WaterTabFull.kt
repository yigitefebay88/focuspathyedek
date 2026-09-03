package com.focuspath.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuspath.app.ui.viewmodel.TaskViewModel

@Composable
fun WaterTabFull(
    vm: TaskViewModel,
    isEnglish: Boolean
) {
    val context = LocalContext.current
    val waterCups = vm.waterCupsDrunk.intValue
    val targetCups = 8
    val progress = (waterCups.toFloat() / targetCups).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "water")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = if (isEnglish) "WATER REMINDER" else "SU HATIRLATICI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        // Water Progress Circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(Color.Blue.copy(alpha = 0.1f))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background Circle
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(4.dp, Color.Blue.copy(alpha = 0.2f))
            ) {}

            // Progress Fill (Simple version)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))
                        )
                    )
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "$waterCups / $targetCups",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isEnglish) "Cups Today" else "Bugünkü Bardak",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Button(
            onClick = { vm.drinkWater() },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
        ) {
            Icon(Icons.Default.LocalDrink, null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isEnglish) "I DRANK WATER (+5 Coin)" else "SU İÇTİM (+5 Coin)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        if (isEnglish) "Reminder Settings" else "Hatırlatıcı Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isEnglish) "Active" else "Aktif")
                        Text(
                            if (isEnglish) "Remind me to drink water" else "Su içmemi hatırlat",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = vm.isWaterReminderEnabled.value,
                        onCheckedChange = { vm.setWaterReminder(it, vm.waterReminderInterval.intValue, context) }
                    )
                }

                if (vm.isWaterReminderEnabled.value) {
                    Column {
                        Text(
                            text = (if (isEnglish) "Interval: " else "Aralık: ") + "${vm.waterReminderInterval.intValue} " + (if (isEnglish) "hours" else "saat"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = vm.waterReminderInterval.intValue.toFloat(),
                            onValueChange = { vm.setWaterReminder(true, it.toInt(), context) },
                            valueRange = 1f..12f,
                            steps = 11
                        )
                    }
                }
            }
        }

        // Motivation Text
        Text(
            text = if (isEnglish) 
                "Drinking water improves focus and reduces fatigue." 
            else 
                "Su içmek odaklanmayı artırır ve yorgunluğu azaltır.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
