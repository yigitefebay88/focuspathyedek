package com.focuspath.app.ui.screens.task

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.focuspath.app.ui.theme.AccentRed
import com.focuspath.app.ui.theme.TerminalGreen
import com.focuspath.app.ui.viewmodel.TaskViewModel

@Composable
fun AiTab(
    vm: TaskViewModel,
    lang: Map<String, String>,
    isEnglish: Boolean,
    context: Context
) {
    var aiInput by rememberSaveable { mutableStateOf("") }
    val chatHistory = vm.chatHistory
    val isBotTyping by vm.isBotTyping
    val chatListState = rememberLazyListState()

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            chatListState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val target = vm.selectedChatUser.value
        if (target != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = if(isEnglish) "Chatting with: $target" else "$target ile mesajlaşıyorsun", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.selectedChatUser.value = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (chatHistory.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(lang["emptyChat"] ?: "", color = Color.Gray, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(state = chatListState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chatHistory) { message ->
                            val isUser = message.startsWith("Siz: ") || message.startsWith("You: ")
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                                Surface(
                                    color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isUser) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                                ) {
                                    Text(text = message, modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                }
                            }
                        }
                        if (isBotTyping) {
                            item { Text(lang["typing"] ?: "", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isEnglish) "en-US" else "tr-TR")
                        }
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                                speechRecognizer.setRecognitionListener(object : RecognitionListener {
                                    override fun onReadyForSpeech(params: Bundle?) {}
                                    override fun onBeginningOfSpeech() {}
                                    override fun onRmsChanged(rmsdB: Float) {}
                                    override fun onBufferReceived(buffer: ByteArray?) {}
                                    override fun onEndOfSpeech() {}
                                    override fun onError(error: Int) {}
                                    override fun onResults(results: Bundle?) {
                                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                        if (!matches.isNullOrEmpty()) vm.sendAiCommand(matches[0], isEnglish)
                                    }
                                    override fun onPartialResults(partialResults: Bundle?) {}
                                    override fun onEvent(eventType: Int, params: Bundle?) {}
                                })
                                speechRecognizer.startListening(intent)
                            } catch (e: Exception) { Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show() }
                        } else { Toast.makeText(context, "Mikrofon izni gerekli.", Toast.LENGTH_SHORT).show() }
                    }) { Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary) }
                    OutlinedTextField(value = aiInput, onValueChange = { aiInput = it }, placeholder = { Text(lang["bot"] ?: "", color = Color.Gray) }, modifier = Modifier.weight(1f), singleLine = true)
                    if (chatHistory.isNotEmpty()) {
                        IconButton(onClick = { vm.resetChat() }) { Icon(Icons.Default.DeleteSweep, null, tint = AccentRed) }
                    }
                    com.focuspath.app.ui.components.BouncyScaleButton(
                        onClick = { if (aiInput.isNotBlank() && !isBotTyping) { vm.sendAiCommand(aiInput, isEnglish); aiInput = "" } },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.Black)
                    }
                }
            }
        }
    }
}
