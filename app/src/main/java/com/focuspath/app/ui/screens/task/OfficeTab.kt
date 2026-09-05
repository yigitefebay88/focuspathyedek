package com.focuspath.app.ui.screens.task

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuspath.app.ui.theme.AccentYellow
import com.focuspath.app.ui.theme.TerminalGreen
import com.focuspath.app.ui.viewmodel.TaskViewModel

@Composable
fun OfficeTab(
    vm: TaskViewModel,
    isEnglish: Boolean,
    onShowLiveSession: () -> Unit
) {
    val officeLevel = vm.officeLevel.value
    val coins = vm.userCoins.value
    val unlocked = vm.unlockedItems
    val upgradeCost = officeLevel * 500

    var officeRotationY by remember { mutableFloatStateOf(-25f) }
    var scale by remember { mutableFloatStateOf(1.1f) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "YİME CENTER", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(text = "LVL $officeLevel • $coins 🪙", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val zoom = event.calculateZoom()
                            if (event.changes.size > 1 && zoom != 1f) {
                                scale = (scale * zoom).coerceIn(0.7f, 2.0f)
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(200.dp).graphicsLayer { rotationX = 0f; rotationY = officeRotationY; scaleX = scale; scaleY = scale; cameraDistance = 25f * density }) {
                Box(modifier = Modifier.align(Alignment.Center).size(180.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(8.dp)))
                Text(text = "🏢", fontSize = 100.sp, modifier = Modifier.align(Alignment.Center).graphicsLayer { rotationX = 0f; rotationY = 0f })
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = if (isEnglish) "OFFICE UPGRADES" else "OFİS YÜKSELTMELERİ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "$upgradeCost 🪙", fontWeight = FontWeight.Bold, color = AccentYellow)
                    Button(onClick = { if (coins >= upgradeCost) vm.upgradeOffice() }, colors = ButtonDefaults.buttonColors(containerColor = if (coins >= upgradeCost) MaterialTheme.colorScheme.primary else Color.Gray)) { Text(if (isEnglish) "UPGRADE" else "YÜKSELT", color = Color.Black) }
                }
            }
        }

        Button(onClick = onShowLiveSession, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text(if (isEnglish) "JOIN ACTIVE SESSION" else "AKTİF OTURUMA KATIL", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
