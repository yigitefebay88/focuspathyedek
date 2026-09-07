package com.focuspath.app.ui.screens.task

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuspath.app.core.data.local.TaskEntity
import com.focuspath.app.core.data.local.HabitEntity
import com.focuspath.app.ui.components.ProfileImage
import com.focuspath.app.ui.screens.TomorrowPlanningDialog
import com.focuspath.app.ui.theme.AccentRed
import com.focuspath.app.ui.theme.AccentYellow
import com.focuspath.app.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarTab(
    vm: TaskViewModel,
    lang: Map<String, String>,
    currentMonthName: String,
    selectedDay: Int,
    allTasksList: List<TaskEntity>,
    onDaySelect: (Int) -> Unit,
    isEnglish: Boolean,
    context: Context,
    timerRunning: Boolean,
    isPomodoroMode: Boolean,
    timeLeft: Long,
    timeElapsed: Long,
    pomodoroTotalMillis: Long,
    selectedFocusSound: String,
    completedPomodorosToday: Int,
    onTimerToggle: (Boolean) -> Unit,
    onModeToggle: (Boolean) -> Unit,
    onTimerSet: (Long) -> Unit,
    onSoundSelect: (String) -> Unit,
    onZenShow: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showBrainDump by remember { mutableStateOf(false) }
    var brainDumpText by remember { mutableStateOf("") }
    var showTomorrowDialog by remember { mutableStateOf(false) }

    val lbUsers by vm.leaderboard.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentMonthName.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    for (i in -3..3) {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, i) }
                        val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                        val dateMillis = cal.timeInMillis
                        val isSelected = dayNum == selectedDay

                        Surface(
                            onClick = {
                                onDaySelect(dayNum)
                                vm.setSelectedDate(dateMillis)
                                showTomorrowDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)),
                            modifier = Modifier.size(width = 42.dp, height = 54.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = SimpleDateFormat("E", Locale.getDefault()).format(cal.time),
                                    fontSize = 8.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                                Text(
                                    text = "$dayNum",
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (timerRunning) {
                    Button(
                        onClick = { showBrainDump = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.Psychology, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("ZİHNİNİ BOŞALT (BRAIN DUMP)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("FOCUS TIMER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        if (completedPomodorosToday > 0) {
                            Text("${if(isEnglish) "Today:" else "Bugün:"} $completedPomodorosToday", fontSize = 10.sp, color = AccentYellow)
                        }
                    }
                    Row {
                        listOf("rain" to "🌧️", "fireplace" to "🔥").forEach { (s, i) ->
                            IconButton({ onSoundSelect(s) }, modifier = Modifier.size(32.dp)) {
                                Text(i, modifier = Modifier.alpha(if(selectedFocusSound == s) 1f else 0.3f))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        listOf(25, 45, 60, 90, 120).forEach { m ->
                            TextButton({ onTimerSet(m * 60000L) }) {
                                Text("${m}m", fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                var customMinutes by remember(pomodoroTotalMillis) { mutableFloatStateOf((pomodoroTotalMillis / 60000).toFloat()) }
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if(isEnglish) "Custom Duration:" else "Özel Süre:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(text = "${customMinutes.toInt()} ${if(isEnglish) "min" else "dk"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = customMinutes,
                        onValueChange = { customMinutes = it ; onTimerSet(it.toLong() * 60000L) },
                        valueRange = 1f..180f,
                        steps = 179,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(24.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                val displayTime = if (isPomodoroMode) {
                    val mins = (timeLeft / 1000) / 60
                    val secs = (timeLeft / 1000) % 60
                    String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                } else {
                    val h = (timeElapsed / 3600000)
                    val m = (timeElapsed % 3600000) / 60000
                    val s = (timeElapsed % 60000) / 1000
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
                }

                Box(contentAlignment = Alignment.Center) {
                    val progress = if (isPomodoroMode) timeLeft.toFloat() / (pomodoroTotalMillis) else 1f

                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(180.dp),
                        strokeWidth = 8.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(180.dp),
                        strokeWidth = 8.dp,
                        color = if (progress < 0.2f) AccentRed.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )

                    val dMulti = vm.dopamineMultiplier.floatValue
                    val multiplierProgress by animateFloatAsState(
                        targetValue = (dMulti - 0.5f) / 2.5f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "DopamineMultiplier"
                    )
                    CircularProgressIndicator(
                        progress = { multiplierProgress },
                        modifier = Modifier.size(210.dp),
                        strokeWidth = 6.dp,
                        color = AccentYellow.copy(alpha = 0.9f)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(displayTime, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (dMulti > 1.0f) {
                            val infiniteTransition = rememberInfiniteTransition(label = "boost")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                                label = "boostScale"
                            )
                            Text(
                                text = "x${String.format(Locale.getDefault(), "%.1f", dMulti)} BOOST ⚡",
                                color = AccentYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                            )
                        }

                        if (timerRunning) {
                            val flameTransition = rememberInfiniteTransition(label = "flame")
                            val flameScale by flameTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                                label = "flameScale"
                            )
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                null,
                                tint = if (dMulti > 1.5f) Color.Cyan else AccentYellow,
                                modifier = Modifier
                                    .size(32.dp)
                                    .graphicsLayer(scaleX = flameScale, scaleY = flameScale)
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (timerRunning) {
                        Button(
                            onClick = {
                                vm.logDistraction("DİKKAT DAĞILDI")
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(0.1f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Warning, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("⚠️ DİKKATİM DAĞILDI (ODAK BOZULDU)", fontSize = 10.sp, color = AccentRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) { 
                    Button(
                        onClick = { onTimerToggle(!timerRunning) }, 
                        shape = RoundedCornerShape(12.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = if(timerRunning) AccentRed else MaterialTheme.colorScheme.primary)
                    ) { 
                        Text(if(timerRunning) (lang["stop"] ?: "DURAKLAT") else (lang["start"] ?: "BAŞLAT"), color = Color.Black) 
                    }

                    if (timerRunning || (isPomodoroMode && timeLeft < pomodoroTotalMillis)) {
                        IconButton(
                            onClick = { vm.abandonSession(context) },
                            modifier = Modifier
                                .background(Color.Red.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, Color.Red.copy(alpha = 0.3f), CircleShape)
                        ) { 
                            Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(20.dp)) 
                        }
                    }

                    IconButton({ onModeToggle(!isPomodoroMode) }) { 
                        Icon(if(isPomodoroMode) Icons.Default.Timer else Icons.Default.HourglassEmpty, null, tint = MaterialTheme.colorScheme.primary) 
                    }
                    
                    if(vm.isPremium.value) {
                        IconButton(onZenShow) { 
                            Icon(Icons.Default.FilterCenterFocus, null, tint = AccentYellow) 
                        }
                    }
                } 
            } 
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = lang["leaderboard"] ?: "LEADERBOARD", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { vm.fetchLeaderboard() }) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Text("#", modifier = Modifier.width(30.dp), color = Color.Gray, fontSize = 10.sp)
                    Text("USER", modifier = Modifier.weight(1f), color = Color.Gray, fontSize = 10.sp)
                    Text("UNVAN", modifier = Modifier.width(80.dp), color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
                    Text("XP", modifier = Modifier.width(50.dp), color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                if (lbUsers.isEmpty()) {
                    Text("FETCHING DATA...", modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Gray, fontSize = 11.sp)
                } else {
                    lbUsers.forEachIndexed { index, user ->
                        val isMe = user.email.equals(vm.userEmail.value, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}",
                                modifier = Modifier.width(30.dp),
                                color = if (index < 3) AccentYellow.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                ProfileImage(
                                    photoUrl = user.photoUrl,
                                    name = user.name,
                                    email = user.email,
                                    size = 20.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                val displayName = if (user.name.isNullOrBlank()) "ANONYMOUS USER" else user.name.uppercase()
                                Text(text = displayName, color = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            val rank = com.focuspath.app.util.FocusRank.getTitle(user.score, isEnglish)
                            Text(rank, modifier = Modifier.width(80.dp), color = Color.Gray.copy(alpha = 0.7f), fontSize = 8.sp, textAlign = TextAlign.Center)
                            Text("${user.score}", modifier = Modifier.width(50.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }

    if (showBrainDump) {
        AlertDialog(
            onDismissRequest = { showBrainDump = false },
            title = { Text("Zihni Temizle", color = AccentYellow) },
            shape = RoundedCornerShape(12.dp),
            text = {
                Column {
                    Text("Aklına takılan alakasız düşünceyi buraya yaz ve odaklanmaya geri dön. Biz onu senin için saklayacağız.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = brainDumpText,
                        onValueChange = { brainDumpText = it },
                        placeholder = { Text("Örn: Sütü almayı unutma...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addBrainDumpNote(brainDumpText)
                    brainDumpText = ""
                    showBrainDump = false
                }) { Text("KAYDET VE UNUT") }
            },
            dismissButton = { TextButton(onClick = { showBrainDump = false }) { Text("İPTAL") } }
        )
    }

    if (showTomorrowDialog) {
        TomorrowPlanningDialog(vm, isEnglish) { showTomorrowDialog = false }
    }
}
