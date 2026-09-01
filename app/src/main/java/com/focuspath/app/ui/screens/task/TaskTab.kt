package com.focuspath.app.ui.screens.task

import androidx.compose.foundation.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuspath.app.data.local.TaskEntity
import com.focuspath.app.ui.theme.AccentRed
import com.focuspath.app.ui.theme.AccentYellow
import com.focuspath.app.ui.theme.TerminalGreen
import com.focuspath.app.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.content.Intent
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTab(
    vm: TaskViewModel,
    taskList: List<TaskEntity>,
    allTasksList: List<TaskEntity>,
    selectedDate: Long,
    lang: Map<String, String>,
    isEnglish: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: Context,
    onEditTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onReminderTask: (TaskEntity) -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onShowClearDialog: () -> Unit,
    onShowQuoteHistory: () -> Unit
) {
    var taskInput by remember { mutableStateOf("") }
    var taskNotes by remember { mutableStateOf("") }
    var taskDuration by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Genel") }
    var selectedPriority by remember { mutableStateOf(1) }
    var searchQuery by remember { mutableStateOf("") }
    var taskFilter by remember { mutableStateOf(0) }
    var categoryFilter by remember { mutableStateOf("Tümü") }
    var priorityFilter by remember { mutableStateOf(-1) }
    var dailyFocus by remember { mutableStateOf("") }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val totalCount = allTasksList.size
    val doneCount = allTasksList.count { it.isCompleted }

    val filteredTasks = taskList

    LaunchedEffect(searchQuery) { vm.setSearchQuery(searchQuery) }
    LaunchedEffect(taskFilter) { vm.setTaskFilter(taskFilter) }
    LaunchedEffect(categoryFilter) { vm.setCategoryFilter(categoryFilter) }
    LaunchedEffect(priorityFilter) { vm.setPriorityFilter(priorityFilter) }

    val taskInputSection = @Composable {
        Column {
            // DATE PICKER ROW
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (i in -3..3) {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, i) }
                        val dateMillis = cal.timeInMillis
                        val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                        val isSelected = Calendar.getInstance().apply { timeInMillis = selectedDate }.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

                        Surface(
                            onClick = { vm.setSelectedDate(dateMillis) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f)),
                            modifier = Modifier.size(width = 40.dp, height = 50.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = SimpleDateFormat("E", Locale.getDefault()).format(cal.time), fontSize = 8.sp, color = if (isSelected) Color.Black else Color.Gray)
                                Text(text = "$dayNum", color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // STATUS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Hello", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        if (vm.isLoggedIn.value) {
                            Column {
                                Text(text = "${vm.userEmail.value} | LVL ${vm.userLevel.value}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                Spacer(Modifier.height(4.dp))
                                val xpProgress = (vm.userXp.value % 100) / 100f
                                LinearProgressIndicator(progress = { xpProgress }, modifier = Modifier.width(120.dp).height(2.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("> FOCUS: ", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            BasicTextField(value = dailyFocus, onValueChange = { dailyFocus = it }, textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium), cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> if (dailyFocus.isEmpty()) Text(if (isEnglish) "SET DAILY INTENTION..." else "BUGÜNKÜ ODAĞINI BELİRLE...", color = Color.Gray, style = MaterialTheme.typography.bodySmall); inner() })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(lang["statsTotal"] ?: "Total", fontSize = 9.sp, color = Color.Gray)
                                Text("$totalCount", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Surface(color = TerminalGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.5f))) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(lang["statsDone"] ?: "Done", fontSize = 9.sp, color = Color.Gray)
                                Text("$doneCount", color = TerminalGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = taskInput, 
                onValueChange = { taskInput = it }, 
                label = { Text(if(isEnglish) "Task Title" else "Görev Başlığı") }, 
                modifier = Modifier.fillMaxWidth(), 
                maxLines = 2, 
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = taskNotes, 
                onValueChange = { taskNotes = it }, 
                label = { Text(if(isEnglish) "Details (Optional)" else "Notlar (Opsiyonel)") }, 
                modifier = Modifier.fillMaxWidth(), 
                maxLines = 3, 
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = taskDuration, 
                onValueChange = { if (it.all { char -> char.isDigit() }) taskDuration = it }, 
                label = { Text(if(isEnglish) "Duration (Mins)" else "Süre (Dakika)") }, 
                modifier = Modifier.fillMaxWidth(), 
                singleLine = true, 
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), 
                keyboardActions = KeyboardActions(onDone = { 
                    if (taskInput.isNotBlank()) { 
                        vm.addTask(taskInput, taskNotes, selectedCategory, selectedPriority, selectedDate, 0, taskDuration.toIntOrNull() ?: 0)
                        taskInput = ""; taskNotes = ""; taskDuration = ""; haptic.performHapticFeedback(HapticFeedbackType.LongPress) 
                    } 
                })
            )
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Genel", "İş", "Kod", "Okul").forEach { cat -> FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat, fontSize = 10.sp) }, leadingIcon = null, modifier = Modifier.height(32.dp)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Kolay" to 0, "Orta" to 1, "Zor" to 2).forEach { (label, p) ->
                            FilterChip(selected = selectedPriority == p, onClick = { selectedPriority = p }, label = { Text(label, fontSize = 10.sp) }, leadingIcon = null, modifier = Modifier.height(32.dp))
                        }
                    }
                }
                com.focuspath.app.ui.components.BouncyScaleButton(
                    onClick = { 
                        if (taskInput.isNotBlank()) { 
                            vm.addTask(taskInput, taskNotes, selectedCategory, selectedPriority, selectedDate, 0, taskDuration.toIntOrNull() ?: 0)
                            taskInput = ""
                            taskNotes = ""
                            taskDuration = ""
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress) 
                        } 
                    }
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.Black)
                    Spacer(Modifier.width(4.dp))
                    Text("EKLE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    val taskListSection = @Composable {
        Column {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text(lang["search"] ?: "", color = Color.Gray) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ScrollableTabRow(selectedTabIndex = taskFilter, edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}, modifier = Modifier.weight(1f)) {
                    listOf(lang["all"] to 0, lang["active"] to 1, lang["completed"] to 2).forEach { (label, index) ->
                        Tab(selected = taskFilter == index, onClick = { taskFilter = index }, text = { Text(label ?: "", color = if (taskFilter == index) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 12.sp) })
                    }
                }
                if (doneCount > 0) {
                    TextButton(onClick = { onShowClearDialog() }) { Text(lang["clearDone"] ?: "Clear", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(6.dp))
            if (filteredTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(lang["empty"] ?: "", color = Color.Gray) }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredTasks, key = { it.id }) { task ->
                        val animatedElevation by animateDpAsState(
                            targetValue = if (task.isCompleted) 0.dp else 4.dp,
                            label = "cardElevation"
                        )
                        val animatedScale by animateFloatAsState(
                            targetValue = if (task.isCompleted) 0.98f else 1f,
                            label = "cardScale"
                        )
                        val glowAlpha by animateFloatAsState(
                            targetValue = if (task.isCompleted) 0.2f else 0f,
                            animationSpec = tween(500),
                            label = "glowAlpha"
                        )
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { 
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }
                                .background(
                                    if (task.isCompleted) TerminalGreen.copy(alpha = glowAlpha) 
                                    else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .animateItem()
                                .pointerInput(task) { 
                                    detectTapGestures(onLongPress = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onEditTask(task) 
                                    }) 
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (task.isCompleted) 
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) 
                                else 
                                    MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
                            border = BorderStroke(
                                1.dp, 
                                if (task.isCompleted) Color.Gray.copy(alpha = 0.2f) 
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                val animatedCheckScale by animateFloatAsState(
                                    targetValue = if (task.isCompleted) 1.2f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
                                    label = "checkScale"
                                )
                                
                                Box(modifier = Modifier.graphicsLayer { scaleX = animatedCheckScale; scaleY = animatedCheckScale }) {
                                    Checkbox(
                                        checked = task.isCompleted, 
                                        onCheckedChange = { vm.toggleTask(task) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = TerminalGreen,
                                            uncheckedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title, 
                                        modifier = Modifier.basicMarquee(),
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Bold
                                        ),
                                        color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (task.notes.isNotBlank()) {
                                        Text(
                                            text = task.notes, 
                                            style = MaterialTheme.typography.bodySmall, 
                                            color = Color.Gray.copy(alpha = 0.8f)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "[${task.category}]", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        val priorityLabel = when(task.priority) {
                                            0 -> if(isEnglish) "KOLAY" else "KOLAY"
                                            2 -> if(isEnglish) "ZOR" else "ZOR"
                                            else -> if(isEnglish) "ORTA" else "ORTA"
                                        }
                                        val priorityColor = when(task.priority) {
                                            0 -> Color.Gray
                                            2 -> AccentRed
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        Text(text = priorityLabel, style = MaterialTheme.typography.labelSmall, color = priorityColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onReminderTask(task) }) { 
                                        Icon(Icons.Default.Alarm, null, tint = AccentYellow.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) 
                                    }
                                    IconButton(onClick = { onDeleteTask(task) }) { 
                                        Icon(Icons.Default.Delete, null, tint = AccentRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) 
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) { taskInputSection() }
            Box(modifier = Modifier.weight(1.2f)) { taskListSection() }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                taskInputSection()
            }
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.weight(1f)) { 
                taskListSection() 
            }
        }
    }



}


