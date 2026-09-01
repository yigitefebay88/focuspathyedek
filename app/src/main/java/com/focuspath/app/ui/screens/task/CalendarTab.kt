package com.focuspath.app.ui.screens.task

import androidx.compose.foundation.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.focuspath.app.ui.components.ProfileImage
import com.focuspath.app.ui.theme.AccentYellow
import com.focuspath.app.ui.viewmodel.TaskViewModel
import com.focuspath.app.data.model.LeaderboardUser
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarTab(
    vm: TaskViewModel,
    lang: Map<String, String>,
    allTasksList: List<com.focuspath.app.data.local.TaskEntity>
) {
    val lbUsers by vm.leaderboard.collectAsState()
    val calendar = Calendar.getInstance()
    val currentMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    
    var selectedUserForProfile by remember { mutableStateOf<LeaderboardUser?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simple Calendar Placeholder
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = currentMonthName.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        // Leaderboard
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = lang["leaderboard"] ?: "LEADERBOARD", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { vm.fetchLeaderboard() }) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(12.dp))
                if (lbUsers.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) {
                            com.focuspath.app.ui.components.ShimmerBox(
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            )
                        }
                    }
                } else {
                    lbUsers.forEachIndexed { index, user ->
                        val isMe = user.email.equals(vm.userEmail.value, ignoreCase = true)
                        val displayPhoto = user.photoUrl
                        
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = if (isMe) 1.02f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseScale"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                                .background(
                                    if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedUserForProfile = user }
                                .padding(vertical = 12.dp, horizontal = 8.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", modifier = Modifier.width(30.dp), color = if (index < 3) AccentYellow else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                ProfileImage(
                                    photoUrl = displayPhoto,
                                    name = user.name,
                                    email = user.email,
                                    size = 20.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = user.name.uppercase(), color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("${user.score}", modifier = Modifier.width(50.dp), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }

    selectedUserForProfile?.let { user ->
        UserProfileDialog(
            user = user,
            vm = vm,
            onDismiss = { selectedUserForProfile = null }
        )
    }
}

@Composable
fun UserProfileDialog(
    user: LeaderboardUser,
    vm: TaskViewModel,
    onDismiss: () -> Unit
) {
    val isMe = user.email == vm.userEmail.value
    val isFriend = vm.friendsList.any { it.email == user.email || it.uid == user.uid }
    val context = androidx.compose.ui.platform.LocalContext.current
    val displayPhoto = user.photoUrl

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileImage(
                    photoUrl = displayPhoto, 
                    name = user.name, 
                    email = user.email,
                    size = 48.dp
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(user.name.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("LEVEL ${(user.score / 100) + 1}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL XP", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text("${user.score}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("STATUS", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    val statusText = if (user.isFocusing) "FOCUSING 🎯" else "IDLE ☕"
                    Text(statusText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (user.isFocusing) Color.Green else Color.Gray)
                }

                if (user.isFocusing && !user.currentTaskTitle.isNullOrBlank()) {
                    Column {
                        Text("CURRENT TASK", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(user.currentTaskTitle, style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isMe && !isFriend) {
                    Button(
                        onClick = {
                            vm.sendFriendRequest(
                                targetEmail = user.email,
                                targetName = user.name,
                                targetUid = user.uid,
                                onSuccess = {
                                    android.widget.Toast.makeText(context, "İstek gönderildi!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("EKLE", color = Color.Black)
                    }
                }
                
                if (!isMe) {
                    Button(
                        onClick = {
                            // Mesajlaşma ekranını veya dialogunu açmak için ViewModel'i kullanabiliriz
                            vm.selectedChatUser.value = user.name
                            vm.selectedChatUserEmail.value = user.email
                            onDismiss()
                            // Burada ana ekranda AI/Chat tabına geçiş tetiklenebilir
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("MESAJ", color = Color.Black)
                    }
                }
                
                TextButton(onClick = onDismiss) {
                    Text("KAPAT")
                }
            }
        }
    )
}

