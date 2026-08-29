package com.focuspath.app.ui.screens.task

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.focuspath.app.R
import com.focuspath.app.data.local.TaskEntity
import com.focuspath.app.data.model.LeaderboardUser
import com.focuspath.app.receiver.ReminderReceiver
import com.focuspath.app.ui.components.CoolGoogleSignInButton
import com.focuspath.app.ui.components.ProfileImage
import com.focuspath.app.ui.theme.AccentRed
import com.focuspath.app.ui.theme.AccentYellow
import com.focuspath.app.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import com.focuspath.app.ui.theme.TerminalGreen
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.border
import java.util.*
import com.focuspath.app.data.local.FocusHistoryEntity

@Composable
fun WeeklyAnalyticsDialog(vm: TaskViewModel, isEnglish: Boolean, onDismiss: () -> Unit) {
    val history by vm.getWeeklyHistory().collectAsState(initial = emptyList())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(if (isEnglish) "WEEKLY REPORT" else "HAFTALIK KARNE")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (history.isEmpty()) {
                    Text(
                        if (isEnglish) "No data for this week yet. Start focusing!" else "Bu hafta için henüz veri yok. Odaklanmaya başla!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    val totalMins = history.sumOf { it.totalFocusMinutes }
                    val totalTasks = history.sumOf { it.tasksCompleted }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if(isEnglish) "Focus" else "Odak", fontSize = 10.sp)
                                Text("${totalMins}m", fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if(isEnglish) "Tasks" else "Görev", fontSize = 10.sp)
                                Text("$totalTasks", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Bar Chart
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val maxFocus = history.maxOf { it.totalFocusMinutes }.coerceAtLeast(1)
                            history.takeLast(7).forEach { day ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val barHeight = (day.totalFocusMinutes.toFloat() / maxFocus) * 100
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height(barHeight.dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    )
                                    Text(day.date.takeLast(2), fontSize = 8.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = if(isEnglish) "💡 Pro Tip: Consistency is key to building focus habits." else "💡 Tavsiye: Odaklanma alışkanlığı için süreklilik en önemli kuraldır.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary.copy(0.7f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(if(isEnglish) "GOT IT" else "ANLADIM") }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    task: TaskEntity,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if(isEnglish) "Delete Task?" else "Görevi Sil?") },
        text = { Text(if(isEnglish) "Are you sure you want to delete '${task.title}'?" else "'${task.title}' görevini silmek istediğinize emin misiniz?") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) { Text(if(isEnglish) "Delete" else "Sil", color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if(isEnglish) "Cancel" else "İptal", color = Color.Gray) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Int) -> Unit
) {
    var editTitle by remember { mutableStateOf(task.title) }
    var editNotes by remember { mutableStateOf(task.notes) }
    var editPriority by remember { mutableIntStateOf(task.priority) }
    var editDuration by remember { mutableStateOf(task.estimatedMinutes.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Görevi Düzenle", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column {
                OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = editNotes, onValueChange = { editNotes = it }, label = { Text("Notlar") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = editDuration, onValueChange = { editDuration = it }, label = { Text("Tahmini Süre (Dk)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                
                Spacer(Modifier.height(12.dp))
                Text("Öncelik", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Kolay" to 0, "Orta" to 1, "Zor" to 2).forEach { (label, p) ->
                        FilterChip(
                            selected = editPriority == p,
                            onClick = { editPriority = p },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(editTitle, editNotes, editPriority, editDuration.toIntOrNull() ?: 0) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal", color = Color.Gray) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ReminderDialog(
    task: TaskEntity,
    context: Context,
    onDismiss: () -> Unit
) {
    var reminderMinutes by remember { mutableStateOf("10") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hatırlatıcı Kur", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column {
                Text("Görev: ${task.title}", color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = reminderMinutes, onValueChange = { reminderMinutes = it }, placeholder = { Text("Kaç dakika sonra?", color = Color.Gray) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val mins = reminderMinutes.toLongOrNull() ?: 10L
                val triggerTime = System.currentTimeMillis() + (mins * 60 * 1000L)
                val intent = Intent(context, ReminderReceiver::class.java).apply { 
                    putExtra("task_title", task.title) 
                }
                
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                
                // Task ID'yi PendingIntent için güvenli bir integer'a çevir (Overflow önleme)
                val safeId = (task.id % Int.MAX_VALUE).toInt()
                val pendingIntent = PendingIntent.getBroadcast(context, safeId, intent, flags)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                    Toast.makeText(context, "$mins dakika sonra hatırlandı.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Toast.makeText(context, "Hatırlatıcı kuruldu.", Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Ayarla", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal", color = Color.Gray) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ZenModeDialog(
    timerRunning: Boolean,
    isPomodoroMode: Boolean,
    timeLeft: Long,
    timeElapsed: Long,
    isEnglish: Boolean,
    onToggleTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("ZEN MODE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                Spacer(Modifier.height(20.dp))
                val displayTime = if (isPomodoroMode) {
                    val mins = (timeLeft / 1000) / 60
                    val secs = (timeLeft / 1000) % 60
                    String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                } else {
                    val hours = (timeElapsed / 1000) / 3600
                    val mins = ((timeElapsed / 1000) % 3600) / 60
                    val secs = (timeElapsed / 1000) % 60
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs)
                }
                Text(text = displayTime, style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Light), color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(60.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    IconButton(onClick = onToggleTimer) { Icon(if (timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun LiveSessionDialog(
    lbUsers: List<LeaderboardUser>,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.95f)) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                // KÜTÜPHANE GÖRSELİ
                Card(
                    modifier = Modifier.fillMaxWidth().height(140.dp).padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Box {
                        AsyncImage(
                            model = R.drawable.ancient_library,
                            contentDescription = "Library",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
                    }
                }

                Text("LIVE GLOBAL FOCUS ROOM", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(32.dp))
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(lbUsers.take(10)) { user ->
                        Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                ProfileImage(
                                    photoUrl = user.photoUrl,
                                    name = user.name,
                                    email = null,
                                    size = 32.dp
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(if (isEnglish) "STATUS: DEEP_FOCUS" else "Durum: DERİN_ODAK", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                                }
                                Icon(Icons.Default.Bolt, null, tint = AccentYellow, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AccentRed), modifier = Modifier.fillMaxWidth()) { Text(if (isEnglish) "DISCONNECT" else "BAĞLANTIYI KES", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun AuthDialog(
    vm: TaskViewModel,
    lang: Map<String, String>,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf(vm.prefs.getString("saved_email", "") ?: "") }
    var password by remember { mutableStateOf(vm.prefs.getString("saved_password", "") ?: "") }
    var rememberMe by remember { mutableStateOf(vm.prefs.getBoolean("remember_me", false)) }
    var isRegister by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf("") }

    var isResetPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isResetPassword) "Şifre Sıfırla" else if (isRegister) "Kayıt Ol" else "Giriş Yap", color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth())
                if (!isResetPassword) {
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Şifre") }, modifier = Modifier.fillMaxWidth(), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                    
                    if (!isRegister) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }.padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                            Text("Beni Hatırla", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                
                if (error.isNotEmpty()) { Text(error, color = Color.Red, fontSize = 12.sp) }
                if (infoMessage.isNotEmpty()) { Text(infoMessage, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { isRegister = !isRegister; isResetPassword = false; error = ""; infoMessage = "" }) { 
                        Text(if (isRegister) "Giriş Yap" else "Kayıt Ol", fontSize = 12.sp) 
                    }
                    if (!isResetPassword) {
                        TextButton(onClick = {
                            isResetPassword = true; error = ""; infoMessage = ""
                        }) { Text("Şifremi Unuttum", fontSize = 12.sp) }
                    } else {
                        TextButton(onClick = {
                            isResetPassword = false; error = ""; infoMessage = ""
                        }) { Text("Geri Dön", fontSize = 12.sp) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (email.isBlank()) {
                    error = "Lütfen e-posta adresinizi girin."
                    return@Button
                }
                if (isResetPassword) {
                    vm.resetPassword(email, onSuccess = { 
                        infoMessage = "Sıfırlama bağlantısı gönderildi. Lütfen e-postanızı (ve Spam/Gereksiz klasörünü) kontrol edin."; error = ""
                    }, onError = { error = it })
                } else {
                    if (password.isBlank()) {
                        error = "Lütfen şifrenizi girin."
                        return@Button
                    }
                    if (isRegister) {
                        vm.registerEmail(email, password, onSuccess = { onDismiss() }, onError = { error = it })
                    } else {
                        vm.loginEmail(email, password, saveCredentials = rememberMe, onSuccess = { onDismiss() }, onError = { error = it })
                    }
                }
            }) { Text(if (isResetPassword) "Gönder" else if (isRegister) "Kayıt Ol" else "Giriş Yap") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun DirectChatDialog(
    targetName: String,
    targetEmail: String,
    vm: TaskViewModel,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    var chatInput by remember { mutableStateOf("") }
    val directMessages = vm.directMessages.filter { 
        (it.from == vm.userEmail.value && it.to == targetEmail) || 
        (it.to == vm.userEmail.value && it.from == targetEmail)
    }.sortedBy { it.timestamp }
    
    val listState = rememberLazyListState()

    LaunchedEffect(directMessages.size) {
        if (directMessages.isNotEmpty()) listState.animateScrollToItem(directMessages.size - 1)
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().height(450.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(text = targetName.uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(0.2f))

                // Chat Messages
                LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(directMessages) { msg ->
                        val isUser = msg.from == vm.userEmail.value
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                            Surface(
                                color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.5.dp, if(isUser) MaterialTheme.colorScheme.primary else Color.Gray.copy(0.3f))
                            ) {
                                Text(text = msg.text, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Input Area
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if(isEnglish) "Ask something..." else "Bir şey sor...", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { 
                            if (chatInput.isNotBlank()) {
                                vm.sendDirectMessage(targetEmail, targetName, chatInput)
                                chatInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun AppBlockerDialog(
    vm: TaskViewModel,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    var installedApps by remember { mutableStateOf(emptyList<com.focuspath.app.ui.viewmodel.AppInfo>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        installedApps = vm.getInstalledApps()
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEnglish) "Block Distracting Apps" else "Dikkat Dağıtıcıları Engelle", color = MaterialTheme.colorScheme.primary) },
        text = {
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column {
                    Text(
                        if (isEnglish) "Selected apps will be blocked during focus sessions." 
                        else "Seçilen uygulamalar odaklanma sırasında engellenecektir.",
                        fontSize = 12.sp, color = Color.Gray
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (isEnglish) "Search apps..." else "Uygulama ara...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filteredApps) { app ->
                            val isBlocked = vm.blockedApps.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📱", fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                Text(app.name, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                Switch(
                                    checked = isBlocked,
                                    onCheckedChange = { vm.toggleBlockedApp(app.packageName) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (isEnglish) "DONE" else "TAMAM")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun AccessibilityDisclosureDialog(
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(if (isEnglish) "Accessibility Permission" else "Erişilebilirlik İzni")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isEnglish)
                        "FocusPath uses the Accessibility Service to help you stay focused by blocking distracting apps during your focus sessions."
                    else
                        "FocusPath, odaklanma seanslarınız sırasında dikkat dağıtıcı uygulamaları engelleyerek odaklanmanıza yardımcı olmak için Erişilebilirlik Hizmetini kullanır.",
                    fontSize = 14.sp
                )
                Text(
                    text = if (isEnglish)
                        "How it works:\n• It detects which app is in the foreground.\n• If a blocked app is opened during focus, it returns you to FocusPath.\n• No personal or sensitive data is collected or shared."
                    else
                        "Nasıl çalışır:\n• Hangi uygulamanın ön planda olduğunu tespit eder.\n• Odaklanma sırasında engellenmiş bir uygulama açılırsa sizi FocusPath'e geri döndürür.\n• Hiçbir kişisel veya hassas veri toplanmaz veya paylaşılmaz.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text(if (isEnglish) "ACCEPT & ENABLE" else "KABUL ET VE AÇ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "NOT NOW" else "ŞİMDİ DEĞİL", color = Color.Gray)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun TeamManagementDialog(
    vm: TaskViewModel,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    var teamNameInput by remember { mutableStateOf("") }
    var joinCodeInput by remember { mutableStateOf("") }
    val userTeam by vm.userTeam
    val teamMembers = vm.teamMembers
    val isTeamLoading by vm.isTeamLoading
    val context = androidx.compose.ui.platform.LocalContext.current
    val terminalColor = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp, max = 650.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, terminalColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // background Image
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?q=80&w=1000&auto=format&fit=crop",
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().alpha(0.2f),
                    contentScale = ContentScale.Crop
                )

                // dark Gradient
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    // HEADER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, null, tint = terminalColor, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (isEnglish) "TEAM CORE" else "TAKIM MERKEZİ",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isTeamLoading) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = terminalColor)
                        }
                    } else if (userTeam == null) {
                        // NO TEAM STATE
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // CREATE TEAM CARD
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                                border = BorderStroke(0.5.dp, terminalColor.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🚀", fontSize = 20.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (isEnglish) "Establish New Team" else "Yeni Takım Kur",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = teamNameInput,
                                        onValueChange = { teamNameInput = it },
                                        placeholder = { Text(if (isEnglish) "Startup Name..." else "Girişim Adı...", color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = terminalColor,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.LightGray
                                        )
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (teamNameInput.isNotBlank()) {
                                                vm.createTeam(
                                                    teamNameInput,
                                                    onSuccess = { Toast.makeText(context, "Takım başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show() },
                                                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = terminalColor),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(if (isEnglish) "INITIALIZE TEAM" else "TAKIMI BAŞLAT", color = Color.Black, fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            // JOIN TEAM CARD
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🔑", fontSize = 20.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (isEnglish) "Join Existing HQ" else "Mevcut Takıma Katıl",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = joinCodeInput,
                                        onValueChange = { if (it.length <= 6) joinCodeInput = it.uppercase() },
                                        placeholder = { Text("######", color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, letterSpacing = 4.sp, fontWeight = FontWeight.Black),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentYellow,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedTextColor = AccentYellow,
                                            unfocusedTextColor = Color.LightGray
                                        )
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (joinCodeInput.length == 6) {
                                                vm.joinTeam(
                                                    joinCodeInput,
                                                    onSuccess = { Toast.makeText(context, "Takıma katıldınız!", Toast.LENGTH_SHORT).show() },
                                                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(if (isEnglish) "ENTER HQ" else "TAKIMA KATIL", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // ACTIVE TEAM STATE
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // TEAM HEADER CARD
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = terminalColor.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, terminalColor.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column {
                                            Text(
                                                userTeam!!.name.uppercase(),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Black,
                                                color = terminalColor
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(if (isEnglish) "HQ CODE: " else "TAKIM KODU: ", fontSize = 11.sp, color = Color.Gray)
                                                Text(userTeam!!.inviteCode, fontWeight = FontWeight.Black, color = AccentYellow, letterSpacing = 1.sp)
                                                IconButton(
                                                    onClick = { 
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        val clip = android.content.ClipData.newPlainText("Team Code", userTeam!!.inviteCode)
                                                        clipboard.setPrimaryClip(clip)
                                                        Toast.makeText(context, "Kod kopyalandı!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                        
                                        // Level Badge
                                        Surface(
                                            color = Color.Black.copy(0.4f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, terminalColor.copy(0.3f))
                                        ) {
                                            Text(
                                                "LVL ${(userTeam!!.totalTeamXp / 5000 + 1)}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = terminalColor
                                            )
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(16.dp))
                                    
                                    // PROGRESS
                                    val progress = (userTeam!!.currentWeeklyXp.toFloat() / userTeam!!.weeklyXpGoal).coerceIn(0f, 1f)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isEnglish) "WEEKLY SPRINT" else "HAFTALIK HEDEF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("%${(progress * 100).toInt()}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = terminalColor)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                        color = terminalColor,
                                        trackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${userTeam!!.currentWeeklyXp} XP", fontSize = 10.sp, color = Color.LightGray)
                                        Text("${userTeam!!.weeklyXpGoal} XP", fontSize = 10.sp, color = Color.Gray)
                                    }

                                    // BADGES
                                    if (userTeam!!.badges.isNotEmpty()) {
                                        Spacer(Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            userTeam!!.badges.forEach { badge ->
                                                val (emoji, label) = when (badge) {
                                                    "startup" -> "🚀" to (if (isEnglish) "Startup" else "Girişim")
                                                    "unicorn" -> "🦄" to "Unicorn"
                                                    "social" -> "🤝" to (if (isEnglish) "Social" else "Sosyal")
                                                    else -> "🏅" to "Badge"
                                                }
                                                Surface(
                                                    color = Color.White.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(0.5.dp, AccentYellow.copy(0.3f))
                                                ) {
                                                    Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Text(emoji, fontSize = 10.sp)
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(label, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // MEMBERS LIST
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isEnglish) "OPERATIVES" else "EKİP ÜYELERİ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(teamMembers) { member ->
                                        val isMvp = teamMembers.maxByOrNull { it.score }?.email == member.email && member.score > 0
                                        Surface(
                                            color = Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = if (isMvp) BorderStroke(1.dp, AccentYellow.copy(0.3f)) else null
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    ProfileImage(photoUrl = member.photoUrl, name = member.name, size = 32.dp)
                                                    if (isMvp) {
                                                        Text("👑", fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp))
                                                    }
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        member.name, 
                                                        fontSize = 14.sp, 
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isMvp) AccentYellow else Color.White
                                                    )
                                                    val status = if (isMvp) (if (isEnglish) "TEAM MVP" else "TAKIM LİDERİ") 
                                                                else (if (isEnglish) "Field Agent" else "Saha Ajanı")
                                                    Text(status, fontSize = 9.sp, color = if (isMvp) AccentYellow.copy(0.7f) else Color.Gray)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("${member.score} XP", fontSize = 12.sp, fontWeight = FontWeight.Black, color = terminalColor)
                                                    if (member.isFocusing) {
                                                        Text(if (isEnglish) "FOCUSING" else "ODAKLANIYOR", fontSize = 7.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // LEAVE BUTTON
                            TextButton(
                                onClick = { vm.leaveTeam { Toast.makeText(context, "Takımdan ayrıldınız.", Toast.LENGTH_SHORT).show() } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(contentColor = AccentRed.copy(0.7f))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isEnglish) "LEAVE TEAM" else "TAKIMDAN AYRIL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEnglish) "MINIMIZE" else "KAPAT", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
