package com.focuspath.app.ui.screens.task

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.focuspath.app.ui.components.CoolGoogleSignInButton
import com.focuspath.app.ui.components.ProfileImage
import com.focuspath.app.ui.theme.AccentRed
import com.focuspath.app.ui.theme.AccentYellow
import com.focuspath.app.ui.theme.TerminalGreen
import com.focuspath.app.ui.viewmodel.TaskViewModel

@Composable
fun SettingsTab(
    vm: TaskViewModel,
    lang: Map<String, String>,
    isEnglish: Boolean,
    onToggleLanguage: () -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { vm.updateProfilePicture(it) }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = lang["settings"] ?: "Settings", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

        // Premium Card
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (vm.isPremium.value) TerminalGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface), border = BorderStroke(2.dp, if (vm.isPremium.value) TerminalGreen else AccentYellow.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = lang["premiumTitle"] ?: "", style = MaterialTheme.typography.titleMedium, color = if (vm.isPremium.value) TerminalGreen else AccentYellow, fontWeight = FontWeight.Bold)
                        Text(text = if (vm.isPremium.value) (lang["premiumActive"] ?: "") else (lang["premiumDesc"] ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (!vm.isPremium.value) {
                        Button(onClick = { vm.buyPremium() }, colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)) { Text(lang["upgrade"] ?: "", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    } else {
                        Icon(Icons.Default.Verified, null, tint = TerminalGreen)
                    }
                }
            }
        }

        // Appearance
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(lang["appearance"] ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(lang["themeLabel"] ?: "", color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = vm.isDarkMode.value, onCheckedChange = { vm.toggleTheme() })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(lang["langLabel"] ?: "", color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = onToggleLanguage) { Text(if (isEnglish) "English" else "Türkçe", color = MaterialTheme.colorScheme.primary) }
                }
            }
        }

        // Notifications & Sound
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if(isEnglish) "NOTIFICATIONS & SOUND" else "BİLDİRİM VE SES", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if(isEnglish) "End Session Notification" else "Seans Bitiş Bildirimi")
                    Switch(checked = vm.isNotificationEnabled.value, onCheckedChange = { vm.setNotificationEnabled(it) })
                }
                
                Spacer(Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if(isEnglish) "Auto Do Not Disturb" else "Otomatik Rahatsız Etmeyin")
                        Text(if(isEnglish) "Enable DND when focus starts" else "Odaklanınca modu otomatik aç", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Switch(checked = vm.isAutoDndEnabled.value, onCheckedChange = { vm.setAutoDndEnabled(context, it) })
                }

                Spacer(Modifier.height(12.dp))
                Text(if(isEnglish) "AMBIENT SOUNDS (LOOP)" else "AMBİYANS SESLERİ (DÖNGÜ)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if(isEnglish) "Ambient Rain" else "Ambiyans Yağmuru")
                        Text(if(isEnglish) "Plays rain sound during focus" else "Odaklanırken yağmur sesi çalar", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Switch(checked = vm.isRainEnabled.value, onCheckedChange = { vm.toggleRain() })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if(isEnglish) "Ambient Fireplace" else "Ambiyans Şöminesi")
                        Text(if(isEnglish) "Plays crackling sound during focus" else "Odaklanırken çıtırtı sesi çalar", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Switch(checked = vm.isFireplaceEnabled.value, onCheckedChange = { vm.toggleFireplace() })
                }
                
                Spacer(Modifier.height(12.dp))
                Text(text = (if(isEnglish) "Alarm Volume: " else "Alarm Ses Seviyesi: ") + "${(vm.alarmVolume.floatValue * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Slider(value = vm.alarmVolume.floatValue, onValueChange = { vm.setAlarmVolume(it) }, valueRange = 0f..1f)
                
                Spacer(Modifier.height(12.dp))
                Text(if(isEnglish) "Alarm Sound" else "Alarm Sesi", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("default" to "🎶", "beep" to "🔔", "alarm" to "🚨").forEach { (id, icon) ->
                        Surface(onClick = { vm.setAlarmSound(id) }, shape = RoundedCornerShape(12.dp), color = if (vm.alarmSound.value == id) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), border = BorderStroke(1.dp, if (vm.alarmSound.value == id) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Gray.copy(0.2f)), modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(icon, fontSize = 20.sp) }
                        }
                    }
                }
            }
        }

        // Water Reminder
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if(isEnglish) "HEALTH & WELLNESS" else "SAĞLIK VE YAŞAM", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if(isEnglish) "Water Reminder" else "Su İçme Hatırlatıcısı")
                        Text(if(isEnglish) "Get notified to stay hydrated" else "Su içmek için bildirim al", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                  Switch(
                        checked = vm.isWaterReminderEnabled.value, 
                        onCheckedChange = { vm.setWaterReminder(it, vm.waterReminderInterval.intValue, context) }
                    )
                }

                if (vm.isWaterReminderEnabled.value) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = (if(isEnglish) "Reminder Interval: " else "Hatırlatma Aralığı: ") + "${vm.waterReminderInterval.intValue} " + (if(isEnglish) "hours" else "saat"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Slider(
                        value = vm.waterReminderInterval.intValue.toFloat(),
                        onValueChange = { vm.setWaterReminder(true, it.toInt(), context) },
                        valueRange = 1f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // Account
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(lang["account"] ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (vm.isLoggedIn.value) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileImage(
                                photoUrl = vm.userPhotoUrl.value,
                                name = vm.userName.value,
                                email = vm.userEmail.value,
                                size = 48.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(vm.userName.value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(vm.userEmail.value, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                        
                        if (vm.isUploadingProfile.value) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            TextButton(onClick = { 
                                galleryLauncher.launch("image/*")
                            }) {
                                Text(if (isEnglish) "Change" else "Değiştir", fontSize = 12.sp)
                            }
                        }
                    }
                    Button(onClick = { vm.logoutGoogle(context) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) { Text(lang["logout"] ?: "", color = Color.White) }
                } else {
                    CoolGoogleSignInButton(onClick = onLoginClick, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), text = lang["login"] ?: "Login")
                }
            }
        }

        // AI & Smart Insights
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isEnglish) "AI & Smart Insights" else "AI ve Akıllı Öngörüler",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Smart Break Suggestion
                Text(
                    text = if (isEnglish) "Smart Break Suggestion:" else "Akıllı Mola Tavsiyesi:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = vm.getSmartBreakSuggestion(isEnglish),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                
                // Distraction Analysis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Distraction Analysis" else "Dikkat Dağıtıcı Analizi",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (vm.isAnalyzingDistractions.value) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Row {
                            TextButton(onClick = { vm.analyzeDistractionsWithAi(isEnglish) }) {
                                Text(if (isEnglish) "Analyze Distractions" else "Dikkat Analizi", fontSize = 11.sp)
                            }
                            TextButton(onClick = { vm.generateWeeklyFocusReport(isEnglish) }) {
                                Text(if (isEnglish) "Weekly Report" else "Haftalık Rapor", fontSize = 11.sp)
                            }
                        }
                    }
                }
                
                vm.distractionAnalysis.value?.let { analysis ->
                    Text(
                        text = analysis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    )
                } ?: run {
                    Text(
                        text = if (isEnglish) "Log distractions to get AI tips." else "AI ipuçları için dikkat dağıtıcıları kaydedin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
