package com.focuspath.app.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.focuspath.app.ui.viewmodel.TaskViewModel
import java.util.Locale

@Composable
fun LofiFocusDialog(vm: TaskViewModel, isEnglish: Boolean, onDismiss: () -> Unit) {
    val timeLeft = vm.timeLeft.longValue
    val currentStation = vm.currentRadioStation.value
    val isRadioPlaying = vm.isRadioPlaying.value

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0A)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?q=80&w=1000&auto=format&fit=crop",
                contentDescription = "Lofi Background",
                modifier = Modifier.fillMaxSize().alpha(0.3f),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isEnglish) "LOFI FOCUS ROOM" else "LOFI ODAK ODASI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                // Center Content (Timer & Vibe)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎧",
                        fontSize = 56.sp
                    )
                    Spacer(Modifier.height(16.dp))

                    val mins = (timeLeft / 1000) / 60
                    val secs = (timeLeft / 1000) % 60
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isEnglish) "Immersive study & chill beats" else "Sürükleyici çalışma ve rahatlatıcı ritimler",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(32.dp))

                    // Radio controls card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentStation?.name ?: (if (isEnglish) "Lofi Focus Radio" else "Lofi Odak Radyosu"),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { vm.previousRadioStation() }) {
                                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }

                                Surface(
                                    onClick = { 
                                        if (currentStation == null) {
                                            vm.toggleRadio(vm.radioStations.first())
                                        } else {
                                            vm.pauseResumeRadio()
                                        }
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (isRadioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                IconButton(onClick = { vm.nextRadioStation() }) {
                                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }

                // Bottom Action
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (isEnglish) "RETURN TO APP" else "UYGULAMAYA DÖN",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
