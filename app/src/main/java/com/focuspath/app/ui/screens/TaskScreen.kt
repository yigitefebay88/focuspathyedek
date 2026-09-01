package com.focuspath.app.ui.screens

import android.Manifest
import android.R
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.focuspath.app.data.local.TaskEntity
import com.focuspath.app.data.model.LeaderboardUser
import com.focuspath.app.receiver.ReminderReceiver
import com.focuspath.app.ui.components.AnimatedIconButton
import com.focuspath.app.ui.components.CoolGoogleSignInButton
import com.focuspath.app.ui.components.ProfileImage
import com.focuspath.app.ui.theme.*
import com.focuspath.app.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.focuspath.app.ui.viewmodel.WorkerAction
import com.focuspath.app.ui.viewmodel.WorkerInfo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.viewinterop.AndroidView
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun TaskScreen(vm: TaskViewModel, onLoginClick: () -> Unit) {

    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val taskList by vm.tasks.collectAsState(initial = emptyList())
    val allTasksList by vm.allTasks.collectAsState(initial = emptyList())
    val totalFocusMins by vm.totalFocusMinutes.collectAsState(initial = 0)
    val totalTasksDone by vm.totalTasksCompleted.collectAsState(initial = 0)
    val selectedDate by vm.selectedDate.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showCertificate by remember { mutableStateOf(false) }
    var taskInput by rememberSaveable { mutableStateOf("") }
    var taskNotes by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Genel") }
    var selectedPriority by rememberSaveable { mutableStateOf(1) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var taskFilter by rememberSaveable { mutableStateOf(0) }
    var categoryFilter by rememberSaveable { mutableStateOf("Tümü") }
    var priorityFilter by rememberSaveable { mutableStateOf(-1) }

    val chatHistory = vm.chatHistory
    val isBotTyping by vm.isBotTyping
    val chatListState = rememberLazyListState()
    val focusActive by vm.isFocusActive
    var isEnglish by rememberSaveable { mutableStateOf(false) }

    // Persistent Notification logic in ViewModel or simple UI check
    if (focusActive) {
        val currentTask = taskList.find { !it.isCompleted && it.priority == 2 } ?: taskList.firstOrNull { !it.isCompleted }
        currentTask?.let { task ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = AccentRed.copy(0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentRed)
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PriorityHigh, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(

                        text = if(isEnglish) "ACTIVE GOAL: ${task.title}" else "ŞU ANKİ HEDEF: ${task.title}",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = AccentRed
                    )
                }
            }
        }
    }

    val context = LocalContext.current

    // SES ÇALMA MANTIĞI: SoundPool Kullanımı
    LaunchedEffect(vm.workers.toList(), focusActive) {
        val isAnyTyping = vm.workers.any { it.currentAction == WorkerAction.TYPING }
        val isAnyMousing = vm.workers.any { it.currentAction == WorkerAction.MOUSE }

        vm.playKeyboardSound(isAnyTyping && focusActive)
        vm.playMouseSound(isAnyMousing && focusActive)
    }
    val prefs = remember { context.getSharedPreferences("focuspath_prefs", android.content.Context.MODE_PRIVATE) }
    
    val timerRunning = vm.timerRunning.value
    val timeLeft = vm.timeLeft.longValue
    val timeElapsed = vm.timeElapsed.longValue
    val pomodoroTotalMillis = vm.pomodoroTotalMillis.longValue
    val isPomodoroMode = vm.isPomodoroMode.value

    var selectedDay by rememberSaveable { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditDialog by remember { mutableStateOf<TaskEntity?>(null) }
    var showDirectChat by remember { mutableStateOf<String?>(null) }
    var isSortByPriority by rememberSaveable { mutableStateOf(false) }
    var showZenMode by rememberSaveable { mutableStateOf(false) }
    var showLiveSession by rememberSaveable { mutableStateOf(false) }
    var selectedFocusSound by rememberSaveable { mutableStateOf("rain") }
    var dailyFocus by rememberSaveable { mutableStateOf("") }
    
    val todayStats by vm.todayStats.collectAsState()
    val completedSessions = todayStats?.sessionsCompleted ?: 0
    val interruptedSessions = todayStats?.sessionsInterrupted ?: 0

    var quoteHistory by rememberSaveable { mutableStateOf(listOf<String>()) }
    var showQuoteHistory by remember { mutableStateOf(false) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var showWeeklyAnalytics by rememberSaveable { mutableStateOf(false) }

    val onPeerClick: (String) -> Unit = { showDirectChat = it }

    val quotes = if (isEnglish) {
        listOf(
            "Discipline is the bridge between goals and accomplishment. — Jim Rohn",
            "Focus on being productive instead of busy. — Tim Ferriss",
            "Your mind is for having ideas, not holding them. — David Allen",
            "The secret of getting ahead is getting started. — Mark Twain",
            "Small progress is still progress. — Anonymous",
            "Believing is half the battle. — Theodore Roosevelt",
            "Discipline is doing what you hate to do, but doing it like you love it. — Mike Tyson",
            "Victory belongs to the one who can say, \"Victory is mine.\" Success, however, belongs to the one who starts by saying, \"I will succeed,\" and ultimately says, \"I have succeeded.\"",
        )
    } else {
        listOf(
            "Zafer, zafer benimdir diyebilenindir. Başarı ise başaracağım diyerek başlayarak sonunda başardım diyenindir. - Mustafa Kemal Atatürk",
            "Disiplin, hedefler ile başarı arasındaki köprüdür. — Jim Rohn",
            "Meşgul olmak yerine üretken olmaya odaklan. — Tim Ferriss",
            "Zihniniz fikir üretmek içindir, onları saklamak için değil. — David Allen",
            "Öne geçmenin sırrı başlamaktır. — Mark Twain",
            "Küçük ilerleme de bir ilerlemedir. — Anonim",
            "İnanmak başarmanın yarısıdır. — Theodore Roosevelt",
            "Disiplin nefret ettiğin her şeyi yapmak ama onu seviyormuş gibi yapmaktır. — Mike Tyson"
        )
    }
    var currentQuote by remember { mutableStateOf(quotes.random()) }
    LaunchedEffect(currentQuote) {
        if (quoteHistory.isEmpty() || quoteHistory.last() != currentQuote) {
            val newList = quoteHistory.toMutableList()
            newList.add(currentQuote)
            if (newList.size > 5) newList.removeAt(0)
            quoteHistory = newList
        }
    }
    LaunchedEffect(Unit) {
        while(true) {
            delay(30000L)
            currentQuote = quotes.random()
        }
    }

    // Timer Receiver MainActivity'e taşındı (Arka plan desteği için)

    LaunchedEffect(searchQuery) { vm.setSearchQuery(searchQuery) }
    LaunchedEffect(taskFilter) { vm.setTaskFilter(taskFilter) }
    LaunchedEffect(categoryFilter) { vm.setCategoryFilter(categoryFilter) }
    LaunchedEffect(priorityFilter) { vm.setPriorityFilter(priorityFilter) }
    LaunchedEffect(isSortByPriority) {
        if (isSortByPriority) vm.setSortType(1) else vm.setSortType(0)
    }

    val lang = if (isEnglish) {
        mapOf(
            "signedInAs" to "Signed in: ",
            "tasks" to "Tasks", "ai" to "AI Core", "cal" to "Calendar",
            "add" to "New Task...", "noteHint" to "Add details...",
            "search" to "Search tasks...", "all" to "All", "active" to "Active", "completed" to "Done",
            "empty" to "No tasks found in terminal.", "emptyChat" to "YimeBot ready. Ask anything.",
            "bot" to "Command AI...", "typing" to "YimeBot is computing...",
            "modeTimer" to "Stopwatch", "modePomo" to "Pomodoro", "start" to "START", "stop" to "PAUSE",
            "quick1" to "💡 Optimize Day", "quick2" to "🚀 Code Refactor",
            "hello" to "System online.", "morningTitle" to "🌅 Good Morning", "afternoonTitle" to "☀️ Good Afternoon", "eveningTitle" to "☀️ Good Evening",
            "nightTitle" to "🌑 Good Night",
            "quote" to "Discipline is the bridge between goals and accomplishment.",
            "clearDone" to "Clear Done", "statsTotal" to "Total", "statsDone" to "Done",
            "productivityTitle" to "📈 Productivity Core", "last12Months" to "Activity",
            "categorySplit" to "Distribution",
            "settings" to "Settings", "appearance" to "System Appearance", "account" to "User Account",
            "data" to "Data & Storage", "about" to "Terminal Intel", "vibe" to "Haptic Feedback",
            "langLabel" to "Display Language", "themeLabel" to "Terminal Vibe", "logout" to "Disconnect Session",
            "login" to "Initialize Session", "clearAll" to "Wipe Database", "version" to "Build Version: 1.0.4-beta",
            "premiumTitle" to "FocusPath Premium", "premiumDesc" to "Premium is required for some mini-games and exclusive features.",
            "upgrade" to "Upgrade Now", "premiumActive" to "Premium Active ⚡",
            "leaderboard" to "GLOBAL_LEADERBOARD",
            "office" to "Office",
            "home" to "Home",
            "clearChat" to "Clear History"
        )
    } else {
        mapOf(
            "signedInAs" to "Bağlı: ",
            "tasks" to "Görevler", "ai" to "Yapay Zeka", "cal" to "Takvim",
            "add" to "Yeni Görev Ekle...", "noteHint" to "Not ekle...",
            "search" to "Görevlerde ara...", "all" to "Tümü", "active" to "Aktif", "completed" to "Tamamlanan",
            "empty" to "Terminalde görev bulunamadı.", "emptyChat" to "YimeBot aktif. Soru sorun.",
            "bot" to "YimeBot'a Sor", "typing" to "YimeBot hesaplıyor...",
            "modeTimer" to "Kronometre", "modePomo" to "Pomodoro", "start" to "BAŞLAT", "stop" to "DURDUR",
            "quick1" to "💡 Günü Planla", "quick2" to "🚀 Kod İyileştir",
            "hello" to "Sistem çevrimiçi.", "morningTitle" to "🌅 Günaydın", "afternoonTitle" to "☀️ İyi Günler", "eveningTitle" to "🌙 İyi Akşamlar",
            "nightTitle" to "🌑 İyi Geceler",
            "quote" to "Disiplin, hedefler ile başarı arasındaki köprüdür.",
            "clearDone" to "Temizle", "statsTotal" to "Toplam", "statsDone" to "Biten",
            "productivityTitle" to "📈 Üretkenlik Merkezi", "last12Months" to "Aktivite",
            "categorySplit" to "Dağılım",
            "settings" to "Ayarlar", "appearance" to "Sistem Görünümü", "account" to "Kullanıcı Hesabı",
            "data" to "Veri ve Depolama", "about" to "Terminal Bilgisi", "vibe" to "Titreşim Geri Bildirimi",
            "langLabel" to "Görüntüleme Dili", "themeLabel" to "Terminal Modu", "logout" to "Bağlantıyı Kes",
            "login" to "Sisteme Bağlan", "clearAll" to "Veritabanını Sıfırla", "version" to "Versiyon: 1.0.4-beta",
            "premiumTitle" to "FocusPath Premium", "premiumDesc" to "Bazı mini oyunlar ve özellikler için Premiumunuz olması gerekir.",
            "upgrade" to "Hemen Yükselt", "premiumActive" to "Premium Aktif ⚡",
            "leaderboard" to "LİDERLİK_TABLOSU",
            "office" to "Ofis",
            "home" to "Ana Sayfa",
            "ai" to "AI",
            "tasks" to "Görevler",
            "cal" to "Takvim",
            "clearChat" to "Geçmişi Sil"
        )
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 6..11 -> lang["morningTitle"]
        in 12..17 -> lang["afternoonTitle"]
        in 18..21 -> lang["eveningTitle"]
        else -> lang["nightTitle"]
    } ?: "⚡ ${lang["hello"]}"
    val currentMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    LaunchedEffect(selectedFocusSound) {
        mediaPlayer?.let { try { if (it.isPlaying) it.stop(); it.release() } catch (e: Exception) {} }
        mediaPlayer = null
        try {
            val resId = context.resources.getIdentifier(selectedFocusSound, "raw", context.packageName)
            if (resId != 0) {
                val player = MediaPlayer.create(context, resId)
                player?.let { it.isLooping = true; it.setVolume(0.7f, 0.7f); mediaPlayer = it; if (timerRunning) it.start() }
            }
        } catch (e: Exception) {}
    }
    DisposableEffect(Unit) { onDispose { mediaPlayer?.let { try { it.stop(); it.release() } catch (e: Exception) {} } } }
    LaunchedEffect(timerRunning, mediaPlayer) {
        val player = mediaPlayer ?: return@LaunchedEffect
        try {
            if (timerRunning) {
                if (!player.isPlaying) player.start()
            } else {
                if (player.isPlaying) player.pause()
            }
        } catch (e: Exception) {
            android.util.Log.e("FocusPath", "MediaPlayer error: ${e.message}")
        }
    }

    var showDeleteConfirm by remember { mutableStateOf<TaskEntity?>(null) }

    LaunchedEffect(Unit) {
        vm.checkAndGenerateBriefing(isEnglish)
    }

    LaunchedEffect(vm.showConfetti.value) {
        if (vm.showConfetti.value) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        val title = when(selectedTab) {
                            0 -> if(isEnglish) "Home" else "Ana Sayfa"
                            1 -> if(isEnglish) "Tasks" else "Görevler"
                            2 -> if(isEnglish) "AI Assistant" else "AI Asistan"
                            3 -> if(isEnglish) "Calendar" else "Takvim"
                            4 -> if(isEnglish) "Virtual Office" else "Sanal Ofis"
                            5 -> if(isEnglish) "Settings" else "Ayarlar"
                            else -> "FocusPath"
                        }
                        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedTab = 0 // Görevler tabına git
                        }) {
                            Icon(Icons.Default.Search, null)
                        }

                        IconButton(onClick = {
                            if (vm.hasUpdate.value) {
                                showUpdateDialog = true
                            } else {
                                Toast.makeText(context, if(isEnglish) "No new notifications." else "Yeni bildirim yok.", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            BadgedBox(
                                badge = {
                                    if (vm.hasUpdate.value) {
                                        Badge(containerColor = AccentRed)
                                    }
                                }
                            ) {
                                Icon(
                                    if(vm.hasUpdate.value) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    null,
                                    tint = if(vm.hasUpdate.value) AccentRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(onClick = {
                            selectedTab = 5 // Settings tabına git
                        }) {
                            Icon(Icons.Default.Settings, null)
                        }

                        IconButton(onClick = { if (vm.isLoggedIn.value) { vm.logoutGoogle(context) } else { onLoginClick() } }) {
                            if (vm.isLoggedIn.value) {
                                ProfileImage(
                                    photoUrl = vm.userPhotoUrl.value,
                                    name = vm.userName.value,
                                    email = vm.userEmail.value,
                                    size = 32.dp
                                )
                            } else {
                                Icon(Icons.Default.Login, null)
                            }
                        }
                    }
                )

                val terminalColor = MaterialTheme.colorScheme.primary
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val navLabels = if (isEnglish)
                            listOf("QUEST", "INCOMING", "GAMES", "ACHIEVE", "FRIENDS")
                        else
                            listOf("GÖREV", "GELEN", "OYUNLAR", "BAŞARIM", "ARKADAŞLAR")

                        val navActions = listOf(
                            { com.focuspath.app.MainActivity.showQuestDialogState.value = true },
                            { com.focuspath.app.MainActivity.showIncomingTasksDialogState.value = true },
                            { com.focuspath.app.MainActivity.showGamesDialogState.value = true },
                            { com.focuspath.app.MainActivity.showAchievementDialogState.value = true },
                            { com.focuspath.app.MainActivity.showFriendsDialogState.value = true }
                        )

                        navLabels.forEachIndexed { index, label ->
                            TextButton(
                                onClick = navActions[index],
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    color = terminalColor,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp // Extra small to fit 5 items
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip // Prevent any wrapping/extra line
                                )
                            }
                        }
                    }
                }

                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (vm.isLoggedIn.value) "${lang["signedInAs"]}${vm.userEmail.value}" else "OFFLINE MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },

        bottomBar = {
            NavigationBar {
                val tabs = listOf(
                    Triple(0, Icons.Default.Home, lang["home"] ?: ""),
                    Triple(1, Icons.Default.CheckCircle, lang["tasks"] ?: ""),
                    Triple(2, Icons.Default.SmartToy, lang["ai"] ?: ""),
                    Triple(3, Icons.Default.CalendarMonth, lang["cal"] ?: ""),
                    Triple(4, Icons.Default.Business, lang["office"] ?: "")
                )

                tabs.forEach { (index, icon, label) ->
                    val isSelected = selectedTab == index
                    val animatedScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tabScale"
                    )

                    NavigationBarItem(
                        icon = {
                            Box(modifier = Modifier.graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }) {
                                if (index == 1) {
                                    BadgedBox(badge = {
                                        val activeCount = taskList.count { !it.isCompleted }
                                        if (activeCount > 0) { Badge(containerColor = AccentRed) { Text("$activeCount") } }
                                    }) { Icon(icon, null) }
                                } else {
                                    Icon(icon, null)
                                }
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = if (label.length > 8) 9.sp else 11.sp
                                )
                            )
                        },
                        selected = isSelected,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { inner ->
        Box(modifier = Modifier.padding(inner).consumeWindowInsets(inner).imePadding().padding(16.dp)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val springSpec = spring<IntOffset>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                    val fadeSpec = tween<Float>(durationMillis = 300)
                    
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = springSpec) { width -> width } + fadeIn(animationSpec = fadeSpec)).togetherWith(
                            slideOutHorizontally(animationSpec = springSpec) { width -> -width } + fadeOut(animationSpec = fadeSpec))
                    } else {
                        (slideInHorizontally(animationSpec = springSpec) { width -> -width } + fadeIn(animationSpec = fadeSpec)).togetherWith(
                            slideOutHorizontally(animationSpec = springSpec) { width -> width } + fadeOut(animationSpec = fadeSpec))
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "tabTransition"
            ) { targetSelectedTab ->
                when (targetSelectedTab) {
                    0 -> HomeTabFull(vm, allTasksList, lang, isEnglish, totalFocusMins, completedSessions, interruptedSessions, { selectedTab = it }, { showCertificate = it })
                    1 -> TaskTabFull(vm, taskList, allTasksList, selectedDate, lang, isEnglish, greeting, currentQuote, haptic, context, isLandscape, { showClearDialog = true }, { showEditDialog = it }, { showDeleteConfirm = it }, { showReminderDialog = it }, { showQuoteHistory = true })
                    2 -> AiTabFull(vm, lang, isEnglish, context, chatHistory, isBotTyping, chatListState, taskList)
                    3 -> CalendarTabFull(vm, lang, currentMonthName, selectedDay, allTasksList, { selectedDay = it }, isEnglish, context, timerRunning, isPomodoroMode, timeLeft, timeElapsed, pomodoroTotalMillis, selectedFocusSound, completedSessions, { vm.toggleTimer(context, it) }, { vm.isPomodoroMode.value = it }, { vm.pomodoroTotalMillis.longValue = it ; vm.timeLeft.longValue = it }, { selectedFocusSound = it }, { showZenMode = true })
                    5 -> SettingsTabFull(vm, lang, isEnglish, onLoginClick, { isEnglish = !isEnglish })
                    4 -> OfficeTabFull(vm, isEnglish, context, allTasksList, completedSessions, { showLiveSession = true }) { showDirectChat = it }
                }
            }
        }
    }

    if (vm.showDailyBriefing.value) {
        DailyBriefingDialog(vm, isEnglish)
    }

    if (vm.showConfetti.value) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    KonfettiView(ctx).apply {
                        start(
                            Party(
                                speed = 0f,
                                maxSpeed = 30f,
                                damping = 0.9f,
                                spread = 360,
                                colors = listOf(0xfce18a, 0xff726d, 0xf41c0e, 0xf44336),
                                position = Position.Relative(0.5, 0.3),
                                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
                            )
                        )
                    }
                }
            )
        }
    }

    if (vm.isMonotasking.value) {
        MonotaskingDialog(vm, isEnglish)
    }

    if (vm.isDecisionSpinnerActive.value || vm.spinnerResult.value != null) {
        DecisionSpinnerDialog(vm, isEnglish)
    }

    if (vm.showCoffeeBreak.value) {
        AlertDialog(
            onDismissRequest = { vm.showCoffeeBreak.value = false },
            confirmButton = {
                Button(onClick = { vm.showCoffeeBreak.value = false }, shape = RoundedCornerShape(12.dp)) {
                    Text(if(isEnglish) "BACK TO WORK" else "ÇALIŞMAYA DÖN")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Coffee, null, tint = AccentYellow)
                    Spacer(Modifier.width(8.dp))
                    Text(if(isEnglish) "Time for a break!" else "Mola Vakti!")
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "☕",
                        fontSize = 64.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if(isEnglish) "Enjoy your coffee break." else "Kahve molasının tadını çıkar!",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if(isEnglish) "You've worked hard." else "Bugün çok iyi çalıştın.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (vm.slicedTasks.isNotEmpty() || vm.isSlicingTask.value) {
        TaskSlicerDialog(vm, isEnglish)
    }

    showDirectChat?.let { name ->
        com.focuspath.app.ui.screens.task.DirectChatDialog(
            targetName = name,
            targetEmail = vm.selectedChatUserEmail.value ?: "",
            vm = vm,
            isEnglish = isEnglish,
            onDismiss = { showDirectChat = null ; vm.selectedChatUserEmail.value = null }
        )
    }

    if (showWeeklyAnalytics) {
        com.focuspath.app.ui.screens.task.WeeklyAnalyticsDialog(vm, isEnglish) {
            showWeeklyAnalytics = false
        }
    }

    if (vm.showMysteryBox.value) {
        val reward = vm.mysteryBoxReward.value
        AlertDialog(
            onDismissRequest = { },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎁", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(if(isEnglish) "SURPRISE BOX!" else "SÜRPRİZ KUTU!") 
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(if(isEnglish) "Great focus session! You found a reward:" else "Harika odaklandın! Bir ödül buldun:", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(reward?.icon ?: "✨", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(reward?.title ?: "", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.claimMysteryBoxReward() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if(isEnglish) "COLLECT REWARD" else "ÖDÜLÜ AL")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(if(isEnglish) "System Update" else "Sistem Güncellemesi")
                }
            },
            text = {
                Column {
                    Text(
                        text = if(isEnglish) "New version ${vm.updateVersionName.value} is available!" else "Yeni sürüm ${vm.updateVersionName.value} yayında!",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(vm.updateNotes.value, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = { vm.markUpdateAsSeen() ; showUpdateDialog = false }) {
                    Text(if(isEnglish) "Got it" else "Anladım")
                }
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = null }, title = { Text(if(isEnglish) "Delete Task?" else "Görevi Sil?") }, text = { Text(if(isEnglish) "Are you sure you want to delete '${showDeleteConfirm?.title}'?" else "'${showDeleteConfirm?.title}' görevini silmek istediğinize emin misiniz?") }, confirmButton = { Button(onClick = { showDeleteConfirm?.let { vm.deleteTask(it) }; showDeleteConfirm = null }, colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) { Text(if(isEnglish) "Delete" else "Sil", color = Color.White) } }, dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text(if(isEnglish) "Cancel" else "İptal", color = Color.Gray) } })
    }
    if (showQuoteHistory) {
        AlertDialog(onDismissRequest = { showQuoteHistory = false }, title = { Text(if (isEnglish) "Quote History" else "Söz Geçmişi") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { quoteHistory.reversed().forEach { quote -> Text("• $quote", style = MaterialTheme.typography.bodySmall) } } }, confirmButton = { TextButton(onClick = { showQuoteHistory = false }) { Text("Close") } })
    }
    if (showClearDialog) {
        AlertDialog(onDismissRequest = { showClearDialog = false }, title = { Text("Tamamlananları Temizle") }, text = { Text("Tamamlanan tüm görevler kalıcı olarak silinecek. Emin misiniz?") }, confirmButton = { Button(onClick = { vm.clearCompletedTasks(); showClearDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) { Text("Temizle") } }, dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("İptal") } })
    }
    showEditDialog?.let { task ->
        var editTitle by remember { mutableStateOf(task.title) }; var editNotes by remember { mutableStateOf(task.notes) }
        var editPriority by remember { mutableIntStateOf(task.priority) }
        var editDuration by remember { mutableStateOf(task.estimatedMinutes.let { if(it == 0) "" else it.toString() }) }
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text(if(isEnglish) "Edit Task" else "Görevi Düzenle") },
            text = {
                Column {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text(if(isEnglish) "Title" else "Başlık") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = editNotes, onValueChange = { editNotes = it }, label = { Text(if(isEnglish) "Notes" else "Notlar") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editDuration, 
                        onValueChange = { if (it.all { char -> char.isDigit() }) editDuration = it }, 
                        label = { Text(if(isEnglish) "Duration (Mins)" else "Süre (Dakika)") }, 
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(if(isEnglish) "Priority" else "Öncelik", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            (if(isEnglish) "EASY" else "KOLAY") to 0,
                            (if(isEnglish) "MEDIUM" else "ORTA") to 1,
                            (if(isEnglish) "HARD" else "ZOR") to 2
                        ).forEach { (label, p) ->
                            FilterChip(
                                selected = editPriority == p,
                                onClick = { editPriority = p },
                                label = {
                                    Text(
                                        label,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.updateTask(task.copy(title = editTitle, notes = editNotes, priority = editPriority, estimatedMinutes = editDuration.toIntOrNull() ?: 0))
                        showEditDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if(isEnglish) "Save" else "Kaydet", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text(if(isEnglish) "Cancel" else "İptal")
                }
            }
        )
    }
    showReminderDialog?.let { task ->
        var reminderMinutes by remember { mutableStateOf("10") }
        AlertDialog(onDismissRequest = { showReminderDialog = null }, title = { Text("Hatırlatıcı Kur") }, text = { Column { Text("Görev: ${task.title}", color = Color.Gray, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); OutlinedTextField(value = reminderMinutes, onValueChange = { reminderMinutes = it }, placeholder = { Text("Kaç dakika sonra?", color = Color.Gray) }, singleLine = true, modifier = Modifier.fillMaxWidth()) } }, confirmButton = {
            Button(onClick = {
                val mins = reminderMinutes.toLongOrNull() ?: 10L
                val triggerTime = System.currentTimeMillis() + (mins * 60 * 1000L)
                val intent = Intent(context, ReminderReceiver::class.java).apply { putExtra("task_title", task.title) }
                val pendingIntent = PendingIntent.getBroadcast(context, task.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent) } else { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent) } } catch (e: SecurityException) { alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent) }
                showReminderDialog = null
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Ayarla", color = Color.Black) }
        })
    }
    if (showZenMode) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showZenMode = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            val infiniteTransition = rememberInfiniteTransition()
            val breathScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.3f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Reverse))
            val breathAlpha by infiniteTransition.animateFloat(initialValue = 0.1f, targetValue = 0.3f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Reverse))
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(280.dp).graphicsLayer(scaleX = breathScale, scaleY = breathScale).background(MaterialTheme.colorScheme.primary.copy(alpha = breathAlpha), CircleShape))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FOCUS MODE", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        val breathingText = when { breathScale < 1.1f -> if (isEnglish) "BREATH IN..." else "NEFES AL..." ; breathScale > 1.2f -> if (isEnglish) "BREATH OUT..." else "NEFES VER..." ; else -> if (isEnglish) "HOLD..." else "TUT..." }
                        Text(text = breathingText, style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 4.sp), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(20.dp))
                        val displayTime = if (isPomodoroMode) { val mins = (timeLeft / 1000) / 60; val secs = (timeLeft / 1000) % 60; String.format(Locale.getDefault(), "%02d:%02d", mins, secs) } else { val hours = (timeElapsed / 1000) / 3600; val mins = ((timeElapsed / 1000) % 3600) / 60; val secs = (timeElapsed / 1000) % 60; String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs) }
                        Text(text = displayTime, style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Light), color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { IconButton(onClick = { vm.toggleTimer(context, !timerRunning) }) { Icon(if (timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) } ; IconButton(onClick = { showZenMode = false }) { Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(32.dp)) } }
                    }
                }
            }
        }
    }
    if (showLiveSession) {
        val lbUsers by vm.leaderboard.collectAsState()
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showLiveSession = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var step by remember { mutableIntStateOf(if (timerRunning) 3 else -1) }
            LaunchedEffect(step) {
                if (step >= 0 && step < 3) {
                    when (step) {
                        0 -> { delay(1000); step = 1 }
                        1 -> { delay(1500); step = 2 }
                        2 -> { delay(1200); step = 3 }
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0D0D0D)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // TAM EKRAN ARKA PLAN
                    Image(
                        painter = painterResource(id = com.focuspath.app.R.drawable.ancient_library),
                        contentDescription = "Library",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    // Karartma Katmanı (Okunabilirlik için)
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Black.copy(alpha = 0.95f)))))

                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (step == -1) {
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp))
                                Spacer(Modifier.height(24.dp))
                                Text(if (isEnglish) "VIRTUAL LIBRARY" else "Sanal Kütüphane", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    text = if (isEnglish) "Focus with global peers in a quiet space." else "Dünya çapındaki üyelerle sessizce odaklan.",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                var isStrictMode by remember { mutableStateOf(prefs.getBoolean("STRICT_MODE_DEFAULT", false)) }
                                Spacer(Modifier.height(24.dp))

                                Surface(
                                    onClick = { isStrictMode = !isStrictMode },
                                    color = if(isStrictMode) MaterialTheme.colorScheme.primary.copy(0.1f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if(isStrictMode) MaterialTheme.colorScheme.primary else Color.Gray.copy(0.3f))
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if(isStrictMode) Icons.Default.Lock else Icons.Default.LockOpen, null, tint = if(isStrictMode) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(if(isEnglish) "STRICT MODE" else "SERT MOD", color = if(isStrictMode) MaterialTheme.colorScheme.primary else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(if(isEnglish) "Auto DND & App Blocker active" else "Otomatik DND ve Uygulama Engelleyici aktif", color = Color.Gray, fontSize = 9.sp)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Switch(
                                            checked = isStrictMode,
                                            onCheckedChange = { isStrictMode = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color.Transparent,
                                                uncheckedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                            ),
                                            thumbContent = if (isStrictMode) {
                                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize), tint = Color.Black) }
                                            } else null
                                        )
                                    }
                                }

                                Spacer(Modifier.height(32.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    listOf(25, 45, 60).forEach { mins ->
                                        Button(onClick = {
                                            if (isStrictMode) {
                                                vm.setAutoDndEnabled(context, true)
                                                prefs.edit().putBoolean("STRICT_MODE_DEFAULT", true).apply()
                                            }
                                            vm.pomodoroTotalMillis.longValue = mins * 60000L; vm.timeLeft.longValue = mins * 60000L;
                                            vm.isPomodoroMode.value = true; vm.toggleTimer(context, true);
                                            step = 0
                                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                                            Text("${mins}m", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(32.dp))
                                TextButton(
                                    onClick = { showLiveSession = false },
                                    modifier = Modifier.alpha(0.8f)
                                ) {
                                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isEnglish) "GO BACK" else "GERİ DÖN", color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            }
                        } else if (step < 3) {
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.HistoryEdu, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
                                Spacer(Modifier.height(32.dp))
                                Text(text = when (step) {
                                    0 -> if(isEnglish) "OPENING LIBRARY DOORS..." else "KÜTÜPHANE KAPILARI AÇILIYOR..."
                                    1 -> if(isEnglish) "FINDING QUIET DESKS..." else "SESSİZ MASALAR BULUNUYOR..."
                                    else -> if(isEnglish) "JOINING STUDY GROUP..." else "ÇALIŞMA GRUBUNA KATILINIYOR..."
                                }, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(24.dp))
                                LinearProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(240.dp).height(2.dp))
                            }
                        } else {
                            // LIBRARY VIEW
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(text = if (isEnglish) "LIBRARY CORE" else "KÜTÜPHANE MERKEZİ", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = if (isEnglish) "DEEP STUDY SESSION" else "DERİN ÇALIŞMA OTURUMU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                        if (vm.isAutoDndEnabled.value) {
                                            Spacer(Modifier.width(8.dp))
                                            Surface(color = AccentRed.copy(0.1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(0.5.dp, AccentRed.copy(alpha = 0.5f))) {
                                                Text("STRICT", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), color = AccentRed.copy(alpha = 0.8f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))) {
                                    val mins = (timeLeft / 1000) / 60; val secs = (timeLeft / 1000) % 60
                                    Text(text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs), modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                }
                            }
                            Spacer(Modifier.height(24.dp))

                            // Community Grid
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val me = lbUsers.find { it.email == vm.userEmail.value }
                                    if (me != null) { item(key = "me") { UserLiveRow(me, isEnglish, true, vm) } }
                                    items(lbUsers.filter { it.email != vm.userEmail.value }.take(15), key = { it.email }) { user ->
                                        UserLiveRow(user, isEnglish, false, vm)
                                    }
                                }
                                // Visual library effect overlay
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.2f), Color.Black.copy(0.8f))), alpha = 0.3f))
                            }

                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { vm.syncXpToFirestore(isFocusing = false); showLiveSession = false ; vm.toggleTimer(context, false) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ExitToApp, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(if (isEnglish) "LEAVE LIBRARY" else "KÜTÜPHANEDEN AYRIL", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCertificate) {
        // En güncel veriyi sağlamak için: 
        // 1. Yerel DB sum
        // 2. Bugün ViewModel'da biriken (Reaktif state)
        // 3. Cloud'dan gelen toplam (Başka cihazlar veya reinstall durumu için)
        val currentDayMins = vm.dailyFocusMinutes.intValue
        val cloudTotal = vm.totalFocusMinutesCloud.intValue
        
        val finalFocusMins = maxOf(totalFocusMins, currentDayMins, cloudTotal)
        
        com.focuspath.app.ui.screens.task.CertificateDialog(
            userName = vm.userName.value,
            totalXp = vm.userXp.value.toLong(),
            totalFocusMinutes = finalFocusMins,
            totalTasksCompleted = totalTasksDone,
            isEnglish = isEnglish,
            onDismiss = { showCertificate = false }
        )
    }
}

@Composable
fun UserLiveRow(user: LeaderboardUser, isEnglish: Boolean, isMe: Boolean, vm: TaskViewModel) {
    val displayPhoto = user.photoUrl

    Surface(
        color = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileImage(
                photoUrl = displayPhoto,
                name = user.name,
                email = user.email,
                size = 36.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.name.uppercase(),
                    color = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    if (isEnglish) "STATUS: DEEP_FOCUS" else "DURUM: DERİN_ODAK",
                    color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }

            // CANLI EMOJİ GÖSTERİMİ (Kaldırıldı)

            if (user.isFocusing) {
                Icon(Icons.Default.Bolt, null, tint = AccentYellow, modifier = Modifier.size(20.dp))
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable

private fun TaskTabFull(vm: TaskViewModel, taskList: List<TaskEntity>, allTasksList: List<TaskEntity>, selectedDate: Long, lang: Map<String, String>, isEnglish: Boolean, greeting: String, currentQuote: String, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback, context: Context, isLandscape: Boolean, onShowClearDialog: () -> Unit, onShowEditDialog: (TaskEntity) -> Unit, onShowDeleteConfirm: (TaskEntity) -> Unit, onShowReminderDialog: (TaskEntity) -> Unit, onShowQuoteHistory: () -> Unit) {
    var tabTaskInput by rememberSaveable { mutableStateOf("") }; var tabTaskNotes by rememberSaveable { mutableStateOf("") }
    var tabSelectedCategory by rememberSaveable { mutableStateOf("Genel") }; var tabSelectedPriority by rememberSaveable { mutableStateOf(1) }
    var tabDailyFocus by rememberSaveable { mutableStateOf("") }
    var tabSearchQuery by rememberSaveable { mutableStateOf("") }
    var tabTaskFilter by rememberSaveable { mutableStateOf(0) }; var tabCategoryFilter by rememberSaveable { mutableStateOf("Tümü") }; var tabPriorityFilter by rememberSaveable { mutableStateOf(-1) }

    var showTomorrowDialog by remember { mutableStateOf(false) }

    LaunchedEffect(tabSearchQuery) { vm.setSearchQuery(tabSearchQuery) }
    LaunchedEffect(tabTaskFilter) { vm.setTaskFilter(tabTaskFilter) }
    LaunchedEffect(tabCategoryFilter) { vm.setCategoryFilter(tabCategoryFilter) }
    LaunchedEffect(tabPriorityFilter) { vm.setPriorityFilter(tabPriorityFilter) }

    LaunchedEffect(vm.isFocusActive.value) {
        while(vm.isFocusActive.value) {
            vm.checkIntervalChime()
            delay(10000L)
        }
    }

    val totalCount = allTasksList.size ; val doneCount = allTasksList.count { it.isCompleted }
    val isMinimalist = vm.isMinimalistMode.value
    val onboardingTasks = vm.onboardingTasks
    
    // Smooth scrolling physics
    val flingBehavior = androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        flingBehavior = flingBehavior
    ) {
        // ONBOARDING TASKS
        if (onboardingTasks.isNotEmpty() && !isMinimalist) {
            item(key = "onboarding_section") {
                Column(modifier = Modifier.animateItemPlacement()) {
                    Text(
                        text = if(isEnglish) "🚀 STARTER MISSIONS" else "🚀 BAŞLANGIÇ GÖREVLERİ",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentYellow,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        items(onboardingTasks, key = { it.id }) { task ->
                            Card(
                                modifier = Modifier.width(180.dp).animateItemPlacement(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if(task.isCompleted) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, if(task.isCompleted) Color.Green.copy(0.3f) else AccentYellow.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if(isEnglish) task.titleEn else task.titleTr,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if(task.isCompleted) Color.Gray else Color.White,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            minLines = 2
                                        )
                                        if (task.isCompleted) {
                                            Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    if (task.isCompleted) {
                                        Text(
                                            text = if(isEnglish) "COMPLETED" else "TAMAMLANDI",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Green,
                                            fontWeight = FontWeight.Black
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("💰", fontSize = 10.sp)
                                            Text("+${task.rewardCoins}", fontSize = 10.sp, color = AccentYellow, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(8.dp))
                                            Text("⭐", fontSize = 10.sp)
                                            Text("+${task.rewardXp}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "calendar_selector") {
            if (!isMinimalist) {
                Card(modifier = Modifier.fillMaxWidth().animateItemPlacement(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.2f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        for (i in -3..3) {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, i) }
                            val dateMillis = cal.timeInMillis
                            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                            val isSelected = Calendar.getInstance().apply { timeInMillis = selectedDate }.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

                            Surface(
                                onClick = {
                                    vm.setSelectedDate(dateMillis)
                                    showTomorrowDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f)),
                                modifier = Modifier.size(width = 40.dp, height = 50.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(text = SimpleDateFormat("E", Locale.getDefault()).format(cal.time), fontSize = 8.sp, color = if (isSelected) Color.Black else Color.Gray)
                                    Text(text = "$dayNum", color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "daily_planner") {
            if (!isMinimalist) {
                Card(
                    modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if(isEnglish) "DAILY PLANNER" else "GÜNLÜK PLANLAYICI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val sum = taskList.joinToString { it.title }
                                    vm.sendAiCommand(if(isEnglish) "Plan my day with these tasks: $sum" else "Bu görevlerle günümü planla: $sum", isEnglish)
                                    Toast.makeText(context, if(isEnglish) "AI is planning your day..." else "Yapay zeka gününüzü planlıyor...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if(isEnglish) "Plan Today" else "Bugünü Planla", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showTomorrowDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentYellow.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, AccentYellow.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.EventNote, null, tint = AccentYellow.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if(isEnglish) "Remember Tomorrow" else "Yarını Hatırla", color = AccentYellow.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item(key = "greeting_card") {
            Card(modifier = Modifier.fillMaxWidth().animateItemPlacement(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = greeting, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        
                        val showInfo = vm.isLoggedIn.value || vm.userXp.value > 0
                        if (showInfo) {
                            Column {
                                val userTitle = com.focuspath.app.util.FocusRank.getTitle(vm.userXp.value.toLong(), isEnglish)
                                val statusText = if (vm.isLoggedIn.value) "${vm.userEmail.value} | $userTitle" else (if(isEnglish) "Offline | $userTitle" else "Çevrimdışı | $userTitle")
                                
                                Text(
                                    text = "$statusText / ${vm.userXp.value}", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!isMinimalist) {
                                    Spacer(Modifier.height(4.dp))
                                    val xpProgress = (vm.userXp.value % 100) / 100f
                                    LinearProgressIndicator(progress = { xpProgress }, modifier = Modifier.width(120.dp).height(2.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                }
                            }
                        }
                        if (!isMinimalist) {
                            Text(text = currentQuote, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.clickable { onShowQuoteHistory() }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(8.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Adjust, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = if(isEnglish) "TODAY'S GOAL: " else "GÜNÜN ODAĞI: ", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            BasicTextField(value = tabDailyFocus, onValueChange = { tabDailyFocus = it }, textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth(), singleLine = true, decorationBox = { inner -> if (tabDailyFocus.isEmpty()) Text(if (isEnglish) "What will you focus on?" else "Neye odaklanacaksın?", color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis); inner() })
                        }
                    }
                    if (!isMinimalist) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))) { Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(lang["statsTotal"] ?: "Toplam", fontSize = 9.sp, color = Color.Gray); Text("$totalCount", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp) } }
                            Surface(color = TerminalGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.5f))) { Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(lang["statsDone"] ?: "Biten", fontSize = 9.sp, color = Color.Gray); Text("$doneCount", color = TerminalGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp) } }
                            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) { Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocalFireDepartment, null, tint = AccentYellow, modifier = Modifier.size(14.dp)); Text("${vm.userStreak.value}d", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp) } }
                        }
                    }
                }
            }
        }

        item(key = "progress_indicator") {
            if (totalCount > 0) {
                val progress = doneCount.toFloat() / totalCount.toFloat()
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).animateItemPlacement()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("İlerleme", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                }
            }
        }

        item(key = "add_task_form") {
            Column(modifier = Modifier.animateItemPlacement()) {
                OutlinedTextField(value = tabTaskInput, onValueChange = { tabTaskInput = it }, label = { Text(if(isEnglish) "Task Title" else "Görev Başlığı") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tabTaskNotes, onValueChange = { tabTaskNotes = it }, label = { Text(if(isEnglish) "Details" else "Notlar") }, modifier = Modifier.weight(1.5f), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    OutlinedTextField(
                        value = vm.currentTaskEstimation.intValue.let { if(it == 0) "" else it.toString() },
                        onValueChange = { vm.currentTaskEstimation.intValue = it.toIntOrNull() ?: 0 },
                        label = { Text(if(isEnglish) "Mins" else "Süre") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (tabTaskInput.isNotBlank()) { vm.addTask(tabTaskInput, tabTaskNotes, tabSelectedCategory, tabSelectedPriority, selectedDate, 0, vm.currentTaskEstimation.intValue); tabTaskInput = "" ; tabTaskNotes = "" ; vm.currentTaskEstimation.intValue = 0 ; haptic.performHapticFeedback(HapticFeedbackType.LongPress) } })
                    )
                }
                Spacer(Modifier.height(6.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Genel", "İş", "Kod", "Okul").forEach { cat ->
                            FilterChip(
                                selected = tabSelectedCategory == cat,
                                onClick = { tabSelectedCategory = cat },
                                label = { Text(cat, fontSize = 10.sp) },
                                leadingIcon = null,
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("KOLAY" to 0, "ORTA" to 1, "ZOR" to 2).forEach { (label, p) ->
                                FilterChip(
                                    selected = tabSelectedPriority == p,
                                    onClick = { tabSelectedPriority = p },
                                    label = { Text(label, fontSize = 10.sp) },
                                    leadingIcon = null,
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("🪫", "🔋", "⚡").forEachIndexed { index, icon ->
                                Surface(
                                    onClick = { vm.setTaskEnergy(index) },
                                    shape = CircleShape,
                                    color = if(vm.selectedTaskEnergy.intValue == index) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    border = BorderStroke(1.dp, if(vm.selectedTaskEnergy.intValue == index) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) { Text(icon, fontSize = 14.sp) }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (tabTaskInput.isNotBlank()) { vm.addTask(tabTaskInput, tabTaskNotes, tabSelectedCategory, tabSelectedPriority, selectedDate, 0, vm.currentTaskEstimation.intValue); tabTaskInput = "" ; tabTaskNotes = "" ; vm.currentTaskEstimation.intValue = 0 ; haptic.performHapticFeedback(HapticFeedbackType.LongPress) } }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.Add, null, tint = Color.Black); Spacer(Modifier.width(8.dp)); Text("EKLE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }

        stickyHeader {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // ARA KISMI GÖRSEL İPUCU: Küçük bir aşağı ok veya belirgin border
                    OutlinedTextField(
                        value = tabSearchQuery,
                        onValueChange = { tabSearchQuery = it },
                        placeholder = { Text(lang["search"] ?: "", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (tabSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { tabSearchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                                }
                                IconButton(onClick = { if (vm.isPremium.value) vm.setSortType((vm.sortType.value + 1) % 3) }) { Icon(when(vm.sortType.collectAsState().value) { 1 -> Icons.Default.Sort ; 2 -> Icons.Default.SortByAlpha ; else -> Icons.Default.LowPriority }, null) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    // KAYDIRMA İPUCU: Liste başlarken minik bir gölge veya divider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        ScrollableTabRow(selectedTabIndex = tabTaskFilter, edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}, modifier = Modifier.weight(1f)) {
                            listOf(lang["all"] to 0, lang["active"] to 1, lang["completed"] to 2).forEach { (label, index) -> Tab(selected = tabTaskFilter == index, onClick = { tabTaskFilter = index }, text = { Text(label ?: "", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, fontWeight = if(tabTaskFilter == index) FontWeight.Bold else FontWeight.Normal) }) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (doneCount > 0) {
                                IconButton(onClick = {
                                    val summary = taskList.filter { it.isCompleted }.joinToString("\n") { "- ${it.title}" }
                                    context.startActivity(Intent.createChooser(Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "FocusPath Tamamlanan Görevlerim:\n$summary")
                                        type = "text/plain"
                                    }, null))
                                }) {
                                    Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (taskList.any { !it.isCompleted }) {
                                IconButton(onClick = {
                                    val finalIndex = (0..7).random()
                                    vm.runDecisionSpinner(taskList.filter { !it.isCompleted }, isEnglish, finalIndex)
                                }) {
                                    Icon(Icons.Default.Casino, null, tint = AccentYellow, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (doneCount > 0) {
                                TextButton(onClick = onShowClearDialog, contentPadding = PaddingValues(horizontal = 4.dp)) {
                                    Text(lang["clearDone"] ?: "Temizle", color = AccentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                }
            }
        }

        items(taskList, key = { it.id }) { task ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .pointerInput(task) {
                        detectTapGestures(onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onShowEditDialog(task) })
                    },
                border = BorderStroke(1.dp, if (task.isCompleted) Color.Gray.copy(alpha = 0.2f) else if(task.priority == 2) AccentRed.copy(0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = task.isCompleted, 
                        onCheckedChange = { 
                            vm.toggleTask(task)
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        },
                        enabled = !task.isCompleted
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = task.title, style = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (task.notes.isNotBlank()) Text(text = task.notes, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "[${task.category}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(8.dp))
                            val (priorityLabel, priorityColor) = when(task.priority) {
                                0 -> "KOLAY" to Color.Gray
                                2 -> "ZOR" to AccentRed
                                else -> "ORTA" to MaterialTheme.colorScheme.primary
                            }
                            Text(
                                text = priorityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.monotask.value = task ; vm.isMonotasking.value = true }) { Icon(Icons.Default.FilterCenterFocus, null, tint = AccentYellow, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = { if (vm.isPremium.value) { vm.sliceTaskWithAi(task.title, isEnglish) } else { Toast.makeText(context, "AI breakdown is a Premium feature.", Toast.LENGTH_SHORT).show() } }) { Icon(Icons.Default.AutoAwesome, null, tint = if(vm.isPremium.value) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = { onShowReminderDialog(task) }) { Icon(Icons.Default.Alarm, null, tint = AccentYellow, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = { onShowEditDialog(task) }) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = { onShowDeleteConfirm(task) }) { Icon(Icons.Default.Delete, null, tint = AccentRed, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) } // Bottom bar için boşluk
    }

    if (showTomorrowDialog) {
        TomorrowPlanningDialog(vm, isEnglish) { showTomorrowDialog = false }
    }
}

@Composable
private fun AiTabFull(vm: TaskViewModel, lang: Map<String, String>, isEnglish: Boolean, context: Context, chatHistory: List<String>, isBotTyping: Boolean, chatListState: LazyListState, taskList: List<TaskEntity>) {
    var aiInput by rememberSaveable { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isEnglish) "en-US" else "tr-TR")
        }
    }

    val startListening = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            isListening = true
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) vm.sendAiCommand(matches[0], isEnglish)
                }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onError(error: Int) { isListening = false }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizer.startListening(recognizerIntent)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(context, if(isEnglish) "Microphone permission required for voice input." else "Sesli girdi için mikrofon izni gereklidir.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // CHAT BOT HEADER (Profil ve Durum)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(42.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    // Yeşil Durum Simgesi
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "YimeBot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if(isEnglish) "AI Productivity Assistant" else "Yapay Zeka Asistanı",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            IconButton(onClick = { vm.resetChat() }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = AccentRed)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { val sum = taskList.joinToString { it.title }; vm.sendAiCommand(if(isEnglish) "Optimize: $sum" else "Günü planla: $sum", isEnglish) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text(lang["quick1"] ?: "", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) };
            OutlinedButton(onClick = { vm.sendAiCommand(if(isEnglish) "Code refactor advice?" else "Kod önerisi ver.", isEnglish) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text(lang["quick2"] ?: "", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }

        Card(modifier = Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (chatHistory.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if(isEnglish) "Hi, I'm YimeBot" else "Merhaba, Ben YimeBot",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = lang["emptyChat"] ?: "",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    LazyColumn(state = chatListState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chatHistory) { message ->
                            val isUser = message.startsWith("Siz: ") || message.startsWith("You: ");
                            val text = message.substringAfter(": ");
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if(!isUser) IconButton(onClick = { (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("YimeBot", text)) ; Toast.makeText(context, "Kopyalandı", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = Color.Gray) };
                                    Surface(color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (isUser) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))) {
                                        Text(text = message, modifier = Modifier.padding(8.dp), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (isBotTyping) { Spacer(Modifier.height(4.dp)); Text(lang["typing"] ?: "", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp)) }; Spacer(Modifier.height(8.dp)); Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Input",
                tint = if (isListening) AccentRed else MaterialTheme.colorScheme.primary
            )
        }
        OutlinedTextField(
            value = aiInput,
            onValueChange = { aiInput = it },
            placeholder = { Text(lang["bot"] ?: "", color = Color.Gray) },
            modifier = Modifier.weight(1f),
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (aiInput.isNotBlank() && !isBotTyping) { vm.sendAiCommand(aiInput, isEnglish); aiInput = "" } })
        );
        Button(onClick = { if (aiInput.isNotBlank() && !isBotTyping) { vm.sendAiCommand(aiInput, isEnglish); aiInput = "" } }, modifier = Modifier.padding(start = 8.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.Send, null, tint = Color.Black) } }
    }
}



@Composable
private fun CalendarTabFull(vm: TaskViewModel, lang: Map<String, String>, currentMonthName: String, selectedDay: Int, allTasksList: List<TaskEntity>, onDaySelect: (Int) -> Unit, isEnglish: Boolean, context: Context, timerRunning: Boolean, isPomodoroMode: Boolean, timeLeft: Long, timeElapsed: Long, pomodoroTotalMillis: Long, selectedFocusSound: String, completedPomodorosToday: Int, onTimerToggle: (Boolean) -> Unit, onModeToggle: (Boolean) -> Unit, onTimerSet: (Long) -> Unit, onSoundSelect: (String) -> Unit, onZenShow: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var showBrainDump by remember { mutableStateOf(false) }
    var showCertificate by remember { mutableStateOf(false) }
    var brainDumpText by remember { mutableStateOf("") }
    var showTomorrowDialog by remember { mutableStateOf(false) }

    val lbUsers by vm.leaderboard.collectAsState(); Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = currentMonthName.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(text = SimpleDateFormat("E", Locale.getDefault()).format(cal.time), fontSize = 8.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                                Text(text = "$dayNum", color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        };
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) { Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // ADHD FEATURE: Brain Dump Button
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("FOCUS TIMER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); if (completedPomodorosToday > 0) Text("${if(isEnglish) "Today:" else "Bugün:"} $completedPomodorosToday", fontSize = 10.sp, color = AccentYellow) }; Row { listOf("rain" to "🌧️", "fireplace" to "🔥").forEach { (s, i) -> IconButton({ onSoundSelect(s) }, modifier = Modifier.size(32.dp)) { Text(i, modifier = Modifier.alpha(if(selectedFocusSound == s) 1f else 0.3f)) } }; Spacer(Modifier.width(8.dp)); listOf(25, 45, 60, 90, 120).forEach { m -> TextButton({ onTimerSet(m * 60000L) }) { Text("${m}m", fontSize = 10.sp) } } } }; Spacer(Modifier.height(10.dp));
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
            Spacer(Modifier.height(10.dp)); val displayTime = if (isPomodoroMode) { val mins = (timeLeft / 1000) / 60; val secs = (timeLeft / 1000) % 60; String.format(Locale.getDefault(), "%02d:%02d", mins, secs) } else { val h = (timeElapsed / 3600000); val m = (timeElapsed % 3600000) / 60000; val s = (timeElapsed % 60000) / 1000; String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s) };

            Box(contentAlignment = Alignment.Center) {
                // ADHD FEATURE: Visual Time Timer (Eksilen Kırmızı Halkalı)
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

                // Dopamine Multiplier Ring
                val dMulti = vm.dopamineMultiplier.floatValue
                val multiplierProgress by animateFloatAsState(
                    targetValue = (dMulti - 0.5f) / 2.5f,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "DopamineMultiplier"
                )
                CircularProgressIndicator(
                    progress = { multiplierProgress },
                    modifier = Modifier.size(210.dp),
                    strokeWidth = 6.dp, // Biraz daha kalın
                    color = AccentYellow.copy(alpha = 0.9f) // Daha parlak
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(displayTime, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (dMulti > 1.0f) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.1f,
                            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse)
                        )
                        Text(
                            text = "x${String.format(Locale.getDefault(), "%.1f", dMulti)} BOOST ⚡",
                            color = AccentYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                        )
                    }

                    // ADHD FOCUS FLAME
                    if (timerRunning) {
                        val flameTransition = rememberInfiniteTransition()
                        val flameScale by flameTransition.animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
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
            };

            Spacer(Modifier.height(16.dp))

            // ADHD FEATURES: "Distraction Logger"
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (timerRunning) {
                    Button(
                        onClick = {
                            vm.logDistraction("DİKKAT DAĞILDI")
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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

                // TEST İÇİN: SEANSI YARIM BIRAKMA BUTONU
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
    };
 Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) { Column(modifier = Modifier.padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(text = lang["leaderboard"] ?: "LEADERBOARD", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontWeight = FontWeight.Bold); IconButton(onClick = { vm.fetchLeaderboard() }) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) } }; Spacer(Modifier.height(12.dp)); Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) { Text("#", modifier = Modifier.width(30.dp), color = Color.Gray, fontSize = 10.sp); Text("USER", modifier = Modifier.weight(1f), color = Color.Gray, fontSize = 10.sp); Text("UNVAN", modifier = Modifier.width(80.dp), color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center); Text("XP", modifier = Modifier.width(50.dp), color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.End) }; HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)); if (lbUsers.isEmpty()) { Text("FETCHING DATA...", modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Gray, fontSize = 11.sp) } else { lbUsers.forEachIndexed { index, user -> val isMe = user.email.equals(vm.userEmail.value, ignoreCase = true) ; Row(modifier = Modifier.fillMaxWidth().background(if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("${index + 1}", modifier = Modifier.width(30.dp), color = if (index < 3) AccentYellow.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp);                                 Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    com.focuspath.app.ui.components.ProfileImage(
                                        photoUrl = user.photoUrl,
                                        name = user.name,
                                        email = user.email,
                                        size = 20.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    val displayName = if (user.name.isNullOrBlank()) "ANONYMOUS USER" else user.name.uppercase()
                                    Text(text = displayName, color = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }; val rank = com.focuspath.app.util.FocusRank.getTitle(user.score, isEnglish); Text(rank, modifier = Modifier.width(80.dp), color = Color.Gray.copy(alpha = 0.7f), fontSize = 8.sp, textAlign = TextAlign.Center); Text("${user.score}", modifier = Modifier.width(50.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End) } } } } }
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

@Composable
private fun SettingsTabFull(vm: TaskViewModel, lang: Map<String, String>, isEnglish: Boolean, onLoginClick: () -> Unit, onToggleLanguage: () -> Unit) {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { vm.updateProfilePicture(it) }
    }

    var showNameEditDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf(vm.userName.value) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = lang["settings"] ?: "Settings", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, if (vm.isPremium.value) TerminalGreen.copy(alpha = 0.5f) else AccentYellow.copy(alpha = 0.3f))) { Column(modifier = Modifier.padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(text = lang["premiumTitle"] ?: "", style = MaterialTheme.typography.titleMedium, color = if (vm.isPremium.value) TerminalGreen.copy(alpha = 0.8f) else AccentYellow.copy(alpha = 0.8f), fontWeight = FontWeight.Bold); Text(text = if (vm.isPremium.value) (lang["premiumActive"] ?: "") else (lang["premiumDesc"] ?: ""), style = MaterialTheme.typography.bodySmall, color = Color.Gray) }; if (!vm.isPremium.value) { Button(onClick = { vm.buyPremium() }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentYellow.copy(alpha = 0.8f))) { Text(lang["upgrade"] ?: "", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } } else { Icon(Icons.Default.Verified, null, tint = TerminalGreen.copy(alpha = 0.8f)) } } } }; Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) { Column(modifier = Modifier.padding(16.dp)) { Text(lang["appearance"] ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(lang["themeLabel"] ?: ""); Switch(checked = vm.isDarkMode.value, onCheckedChange = { vm.toggleTheme() }) }; if (vm.isPremium.value) { Spacer(Modifier.height(8.dp)); Text("Terminal Renk Şeması (Premium)", style = MaterialTheme.typography.labelSmall, color = Color.Gray); Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { listOf(0 to TerminalGreen, 1 to Color(0xFFFFB000), 2 to Color(0xFF00E5FF), 3 to Color(0xFFFF5252)).forEach { (idx, color) -> Box(modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).border(width = if (vm.themeColorIndex.value == idx) 2.dp else 0.dp, color = Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp)).clickable { vm.setThemeColor(idx) }) } } }; Spacer(Modifier.height(8.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(lang["langLabel"] ?: ""); TextButton(onClick = onToggleLanguage) { Text(if (isEnglish) "English" else "Türkçe", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)) } }; Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(lang["vibe"] ?: "Haptic Feedback"); Switch(checked = vm.isHapticEnabled.value, onCheckedChange = { vm.setHapticEnabled(it) }) } } };
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if(isEnglish) "ADHD TOOLS" else "DEHB ARAÇLARI", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if(isEnglish) "Minimalist Interface" else "Sade Arayüz")
                        Text(if(isEnglish) "Reduce cognitive load by hiding stats" else "İstatistikleri gizleyerek bilişsel yükü azalt", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Switch(checked = vm.isMinimalistMode.value, onCheckedChange = { vm.setMinimalistMode(it) })
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if(isEnglish) "Interval Chimes" else "Zaman Farkındalığı Sinyalleri")
                        Text(if(isEnglish) "Subtle alert every ${vm.intervalMinutes.intValue} mins" else "Her ${vm.intervalMinutes.intValue} dakikada bir hafif uyarı", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Switch(checked = vm.isIntervalChimeEnabled.value, onCheckedChange = { vm.setIntervalChime(it, vm.intervalMinutes.intValue) })
                }

                if (vm.isIntervalChimeEnabled.value) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = (if(isEnglish) "Chime Interval: " else "Sinyal Aralığı: ") + "${vm.intervalMinutes.intValue} " + (if(isEnglish) "min" else "dk"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Slider(
                        value = vm.intervalMinutes.intValue.toFloat(),
                        onValueChange = { vm.setIntervalChime(true, it.toInt()) },
                        valueRange = 5f..60f,
                        steps = 10,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) { Column(modifier = Modifier.padding(16.dp)) { Text(if(isEnglish) "PRODUCTIVITY" else "ÜRETKENLİK", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f));
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
                Column(modifier = Modifier.weight(1f)) { 
                    Text(if(isEnglish) "App Blocker" else "Uygulama Engelleyici")
                    Text(if(isEnglish) "Block distracting apps during focus" else "Odaklanırken dikkat dağıtıcıları engelle", style = MaterialTheme.typography.labelSmall, color = Color.Gray) 
                }
                val context = LocalContext.current 
                var showBlockerDialog by remember { mutableStateOf(false) }
                var showDisclosureDialog by remember { mutableStateOf(false) }

                if (showDisclosureDialog) {
                    com.focuspath.app.ui.screens.task.AccessibilityDisclosureDialog(
                        isEnglish = isEnglish,
                        onDismiss = { showDisclosureDialog = false },
                        onAccept = {
                            showDisclosureDialog = false
                            vm.openAccessibilitySettings(context)
                        }
                    )
                }

                Button(
                    onClick = { 
                        if (vm.isAccessibilityServiceEnabled(context)) { 
                            showBlockerDialog = true 
                        } else { 
                            showDisclosureDialog = true 
                        } 
                    }, 
                    shape = RoundedCornerShape(12.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = if (vm.isAccessibilityServiceEnabled(context)) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else AccentRed.copy(alpha = 0.8f))
                ) { 
                    Text(if (vm.isAccessibilityServiceEnabled(context)) (if(isEnglish) "CONFIGURE" else "YAPILANDIR") else (if(isEnglish) "ENABLE SERVICE" else "SERVİSİ AÇ"), color = Color.Black, fontSize = 11.sp) 
                }
                if (showBlockerDialog) { 
                    com.focuspath.app.ui.screens.task.AppBlockerDialog(vm, isEnglish) { showBlockerDialog = false } 
                } 
            }
        }
    };
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) { Column(modifier = Modifier.padding(16.dp)) { Text(if(isEnglish) "NOTIFICATIONS & SOUND" else "BİLDİRİM VE SES", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if(isEnglish) "End Session Notification" else "Seans Bitiş Bildirimi"); Switch(checked = vm.isNotificationEnabled.value, onCheckedChange = { vm.setNotificationEnabled(it) }) }; Spacer(Modifier.height(8.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(if(isEnglish) "Auto Do Not Disturb" else "Otomatik Rahatsız Etmeyin"); Text(if(isEnglish) "Enable DND when focus starts" else "Odaklanınca modu otomatik aç", style = MaterialTheme.typography.labelSmall, color = Color.Gray) }; val context = LocalContext.current ; Switch(checked = vm.isAutoDndEnabled.value, onCheckedChange = { vm.setAutoDndEnabled(context, it) }) }; 
            
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

            Spacer(Modifier.height(12.dp)); Text(text = (if(isEnglish) "Alarm Volume: " else "Alarm Ses Seviyesi: ") + "${(vm.alarmVolume.floatValue * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.Gray); Slider(value = vm.alarmVolume.floatValue, onValueChange = { vm.setAlarmVolume(it) }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))); Spacer(Modifier.height(12.dp)); Text(if(isEnglish) "Alarm Sound" else "Alarm Sesi", style = MaterialTheme.typography.labelSmall, color = Color.Gray); Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("default" to "🎶", "beep" to "🔔", "alarm" to "🚨").forEach { (id, icon) -> Surface(onClick = { vm.setAlarmSound(id) }, shape = RoundedCornerShape(12.dp), color = if (vm.alarmSound.value == id) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), border = BorderStroke(1.dp, if (vm.alarmSound.value == id) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Gray.copy(0.2f)), modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Text(icon, fontSize = 20.sp, modifier = Modifier.alpha(if(vm.alarmSound.value == id) 1f else 0.5f)) } } } } } };         Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(lang["account"] ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                
                val showProfile = vm.isLoggedIn.value || vm.userPhotoUrl.value != null
                
                if (showProfile) {
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
                                size = 48.dp,
                                border = if (!vm.isLoggedIn.value) BorderStroke(1.dp, Color.Gray) else null
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(vm.userName.value, fontWeight = FontWeight.Bold, color = if (vm.isLoggedIn.value) MaterialTheme.colorScheme.onSurface else Color.Gray, fontSize = 14.sp)
                                    if (vm.isLoggedIn.value) {
                                        IconButton(onClick = { 
                                            newNameInput = vm.userName.value
                                            showNameEditDialog = true 
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Text(if (vm.isLoggedIn.value) vm.userEmail.value else (if(isEnglish) "OFFLINE SESSION" else "ÇEVRİMDIŞI OTURUM"), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                        }
                        
                        if (vm.isLoggedIn.value) {
                            if (vm.isUploadingProfile.value) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                                    Text(if (isEnglish) "Change" else "Değiştir", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Button(onClick = onLoginClick, shape = RoundedCornerShape(8.dp)) {
                                Text(lang["login"] ?: "Login", fontSize = 11.sp)
                            }
                        }
                    }
                    if (vm.isLoggedIn.value) {
                        Button(onClick = { vm.logoutGoogle(context) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.8f))) { Text(lang["logout"] ?: "", color = Color.White) }
                    }
                } else {
                    Button(onClick = onLoginClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))) { Text(lang["login"] ?: "Giriş Yap / Kayıt Ol", color = Color.Black) }
                }
            }
        }
    }

    if (showNameEditDialog) {
        AlertDialog(
            onDismissRequest = { showNameEditDialog = false },
            title = { Text(if(isEnglish) "Edit Name" else "Adı Düzenle") },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text(if(isEnglish) "Full Name" else "Ad Soyad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateUserName(newNameInput)
                    showNameEditDialog = false
                }) {
                    Text(if(isEnglish) "Save" else "Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameEditDialog = false }) {
                    Text(if(isEnglish) "Cancel" else "İptal")
                }
            }
        )
    }
}

@Composable
fun WorkerDetailDialog(vm: TaskViewModel, worker: WorkerInfo, isEnglish: Boolean, onPeerClick: (String) -> Unit, onDismiss: () -> Unit) {
    val onSendReaction: (String) -> Unit = { emoji ->
        worker.email?.let { vm.sendEmojiReaction(it, emoji) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(worker.name.uppercase(), fontWeight = FontWeight.ExtraBold)
                }
                if (worker.isFriend) {
                    Icon(Icons.Default.Star, null, tint = AccentYellow)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // FOCUS BUDDY STATUS (Eğer buddy ise)
                val isMyBuddy = vm.selectedFocusBuddy.value?.id == worker.id
                if (isMyBuddy && worker.interactionText != null) {
                    Surface(color = AccentYellow.copy(0.1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AccentYellow)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, null, tint = AccentYellow, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = if(isEnglish) "FOCUSING ON: ${worker.interactionText}" else "ODAKLANDIĞI KONU: ${worker.interactionText}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
                        }
                    }
                }

                // EMOJİ PANELİ (Dialog'un en başında, çok daha görünür)
                if (worker.email != null && !worker.isMe) {
                    Text(if(isEnglish) "ADHD TOOLS" else "DEHB ARAÇLARI", style = MaterialTheme.typography.labelSmall, color = AccentYellow, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (isMyBuddy) vm.selectedFocusBuddy.value = null
                                else vm.selectedFocusBuddy.value = worker
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if(isMyBuddy) AccentRed else Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(if(isMyBuddy) Icons.Default.LinkOff else Icons.Default.Groups, null, tint = if(isMyBuddy) Color.White else AccentYellow)
                            Spacer(Modifier.width(8.dp))
                            Text(if(isMyBuddy) (if(isEnglish) "DISCONNECT" else "BAĞI KOPAR") else "BODY DOUBLING", fontSize = 8.sp, color = Color.White)
                        }

                        if (worker.isFriend) {
                            Button(
                                onClick = {
                                    vm.selectedChatUserEmail.value = worker.email
                                    onPeerClick(worker.name)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Chat, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if(isEnglish) "MESSAGE" else "MESAJ", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // EMOJİ REAKSİYONLARI (Gelen Kutusu İçin)
                    Text(if(isEnglish) "QUICK REACTION" else "HIZLI REAKSİYON", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("🔥", "👏", "☕", "🚀", "💡", "🎮").forEach { emoji ->
                            Surface(
                                onClick = { onSendReaction(emoji); onDismiss() },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(44.dp),
                                border = BorderStroke(1.dp, Color.Gray.copy(0.3f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }

                Surface(color = MaterialTheme.colorScheme.primary.copy(0.1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text(if(isEnglish) "Role" else "Rol", style = MaterialTheme.typography.labelSmall, color = Color.Gray); Text(worker.position, fontWeight = FontWeight.Bold) }
                        Column(horizontalAlignment = Alignment.End) {
                            Box(modifier = Modifier.background(TerminalGreen.copy(0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(worker.currentAction.name, color = TerminalGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f), border = BorderStroke(1.dp, Color.Gray.copy(0.2f))) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if(isEnglish) "Work Time" else "Çalışma", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("${worker.totalWorkTime}m", fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), border = BorderStroke(1.dp, Color.Gray.copy(0.2f))) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if(isEnglish) "Productivity" else "Verim", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("%${worker.productivity}", fontWeight = FontWeight.Bold, color = TerminalGreen)
                        }
                    }
                }
                LinearProgressIndicator(progress = { (worker.xpContribution / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primary.copy(0.1f))
                Text(text = "${if(isEnglish) "XP Contribution:" else "XP Katkısı:"} ${worker.xpContribution}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("KAPAT", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun BoxScope.WorkerModel(worker: WorkerInfo, fixedRotationX: Float, selectedBuddy: WorkerInfo? = null, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()

    // Typing animation
    val typingOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f,
        animationSpec = infiniteRepeatable(animation = tween(150, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
    )

    // Head bobbing (Thinking/Idle)
    val headBob by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse)
    )

    val isTyping = worker.currentAction == WorkerAction.TYPING
    val isMouse = worker.currentAction == WorkerAction.MOUSE
    val isThinking = worker.currentAction == WorkerAction.THINKING
    val isAsking = worker.currentAction == WorkerAction.ASKING

    // Emoji Reaksiyonu Kontrolü (Anlık temizleme için)
    var emojiVisible by remember(worker.latestEmoji, worker.emojiTime) {
        mutableStateOf(worker.latestEmoji != null && (System.currentTimeMillis() - worker.emojiTime < 5000))
    }
    LaunchedEffect(worker.latestEmoji, worker.emojiTime) {
        if (emojiVisible) {
            val remaining = 5000 - (System.currentTimeMillis() - worker.emojiTime)
            if (remaining > 0) {
                kotlinx.coroutines.delay(remaining)
                emojiVisible = false
            }
        }
    }

    // Oturma mantığı: Odaklanma aktifse ve masadaysa oturur. Yürürken veya kahve molasında ayaktadır.
    val isSitting = worker.isFocusing && worker.deskId.isNotEmpty() && worker.currentAction != WorkerAction.WALKING && worker.currentAction != WorkerAction.COFFEE

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.BottomCenter)) {
        // EMOJİ BALONU (En Üstte)
        if (emojiVisible && worker.latestEmoji != null) {
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.offset(y = (-25).dp).size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = worker.latestEmoji!!, fontSize = 16.sp)
                }
            }
        }

        // FOCUS AURA (Sadece kendimiz ve odaklanıyorsak)
        if (worker.isMe && worker.isFocusing) {
            val auraTransition = rememberInfiniteTransition()
            val auraScale by auraTransition.animateFloat(initialValue = 0.8f, targetValue = 1.4f, animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse))
            val auraAlpha by auraTransition.animateFloat(initialValue = 0.1f, targetValue = 0.4f, animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse))
            
            Box(modifier = Modifier
                .size(60.dp)
                .graphicsLayer { scaleX = auraScale; scaleY = auraScale; alpha = auraAlpha; rotationX = 90f }
                .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary, Color.Transparent)), CircleShape)
            )
        }

        if (worker.interactionText != null && isAsking) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-20).dp)) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TerminalGreen),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = worker.interactionText!!,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        } else {
            // İSİM ETİKETİ (Online kullanıcılar için her zaman, botlar için sadece tıklanınca veya hep - Tercihen online vurgusu)
            val nameTagColor = if (worker.isLiveUser) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.offset(y = (-10).dp),
                border = if (worker.isFocusing) BorderStroke(1.dp, TerminalGreen) else null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) {
                    if (worker.isFocusing) {
                        Box(modifier = Modifier.size(4.dp).background(TerminalGreen, CircleShape))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = worker.name.uppercase(),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        // 3D UYUMU: Karakteri dik tutmak yerine ofis zeminine göre 3D derinlik katıyoruz.
        val isMyBuddy = selectedBuddy?.id == worker.id
        Box(modifier = Modifier
            .offset(y = if (isSitting) 18.dp else 0.dp) // Ayaktayken daha yukarıda (y=0) durur
            .size(44.dp, 54.dp)
            .then(
                if (isMyBuddy) Modifier.border(2.dp, AccentYellow, RoundedCornerShape(12.dp)) else Modifier
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .graphicsLayer {
                // Zemindeki 55 derecelik açıyı tersine çevirip biraz daha öne eğiyoruz (Monitöre bakış açısı)
                rotationX = -fixedRotationX + (if (isSitting) 5f else 0f)

                // GERÇEKÇİ YÜRÜYÜŞ: Sağa/Sola giderken tüm vücudu (ve saçın açısını) belirgin şekilde çeviriyoruz
                if (worker.currentAction == WorkerAction.WALKING) {
                    // Yürürken vücudu 50 derece çevirerek profil görünümü veriyoruz
                    rotationY = if (worker.isFacingRight) -50f else 50f
                    // Yürürken karakteri biraz daha dikleştirerek hareket ivmesi katıyoruz
                    rotationX += 10f
                } else {
                    rotationY = 0f
                }

                cameraDistance = 12f * density
                transformOrigin = TransformOrigin(0.5f, 1f) // Ayaklardan itibaren dönme
            }) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width ; val h = size.height
                val clothesColor = when(worker.name.length % 3) {
                    0 -> Color(0xFF1B5E20); 1 -> Color(0xFF0D47A1); else -> Color(0xFF37474F)
                }

                // Torso (Gövdeye hafif gölge ile derinlik katma)
                drawRoundRect(color = clothesColor, topLeft = Offset(w * 0.22f, h * 0.28f), size = Size(w * 0.56f, h * 0.38f), cornerRadius = CornerRadius(8f, 8f))

                // Legs (Ayaktayken bacaklar daha uzun)
                val legHeight = if (isSitting) h * 0.34f else h * 0.42f
                drawRoundRect(color = Color(0xFF212121), topLeft = Offset(w * 0.18f, h * 0.58f), size = Size(w * 0.28f, legHeight), cornerRadius = CornerRadius(6f, 6f))
                drawRoundRect(color = Color(0xFF212121), topLeft = Offset(w * 0.54f, h * 0.58f), size = Size(w * 0.28f, legHeight), cornerRadius = CornerRadius(6f, 6f))

                // Arms (Animated)
                val leftArmOffset = if(isTyping) typingOffset else 0f
                val rightArmOffset = if(isTyping) -typingOffset else if(isMouse) typingOffset else 0f

                drawRoundRect(color = clothesColor, topLeft = Offset(w * 0.08f, h * 0.32f + leftArmOffset), size = Size(w * 0.18f, h * 0.22f), cornerRadius = CornerRadius(4f, 4f))
                drawRoundRect(color = clothesColor, topLeft = Offset(w * 0.74f, h * 0.32f + rightArmOffset), size = Size(w * 0.18f, h * 0.22f), cornerRadius = CornerRadius(4f, 4f))

                // Head (Kafayı hafif monitöre doğru eğik çiziyoruz)
                val headY = h * 0.18f + (if(isThinking || isAsking) headBob else 0f)

                // ARKADAŞ FOTOĞRAFI VEYA STANDART KAFA
                if (worker.photoUrl == null) {
                    // Arka görünüm için ten rengi yerine saç rengini tüm kafaya yayıyoruz (Yüz görünmesin)
                    val hairColor = if (worker.name.length % 2 == 0) Color(0xFF3E2723) else Color(0xFF212121)
                    drawCircle(color = hairColor, radius = w * 0.21f, center = Offset(w * 0.50f, headY))
                    // Hair Detail (Arka saç yapısı)
                    drawArc(color = hairColor, startAngle = 0f, sweepAngle = 360f, useCenter = true, topLeft = Offset(w * 0.28f, headY - h * 0.14f), size = Size(w * 0.44f, h * 0.24f))
                }
            }

            // Arkadaş fotoğrafı varsa Canvas dışına (AsyncImage ile) kafa olarak çiz
            if (worker.photoUrl != null) {
                val headBobPx = if(isThinking || isAsking) headBob else 0f
                AsyncImage(
                    model = worker.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 2.dp + headBobPx.dp)
                        .clip(CircleShape)
                        .border(1.dp, if(worker.isFocusing) TerminalGreen else Color.Gray, CircleShape)
                        .graphicsLayer { rotationY = 180f }, // Fotoğrafı da arkaya bakıyormuş gibi hissettirmek için çevirebiliriz (Opsiyonel)
                    contentScale = ContentScale.Crop
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width ; val h = size.height
                val headY = h * 0.18f + (if(isThinking || isAsking) headBob else 0f)

                // Eyes silindi çünkü arkadan bakıyoruz

                // Status Dot
                val statusColor = when(worker.currentAction) {
                    WorkerAction.WORKING, WorkerAction.TYPING, WorkerAction.MOUSE -> TerminalGreen
                    WorkerAction.COFFEE, WorkerAction.RESTING -> AccentYellow
                    WorkerAction.ASKING -> Color.Cyan
                    else -> Color.Gray
                }
                drawCircle(color = statusColor, radius = 4f, center = Offset(w * 0.85f, headY - h * 0.1f))
            }
        }
    }
}

@Composable
fun PlantModel(level: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(24.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Saksı
            drawRoundRect(
                color = Color(0xFF5D4037),
                topLeft = Offset(w * 0.2f, h * 0.6f),
                size = Size(w * 0.6f, h * 0.35f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            
            // Bitki Büyüme Evreleri
            val plantColor = if (level > 5) Color(0xFF2E7D32) else Color(0xFF4CAF50)
            
            when {
                level <= 2 -> { // Filiz
                    drawRect(color = plantColor, topLeft = Offset(w * 0.45f, h * 0.4f), size = Size(w * 0.1f, h * 0.25f))
                    drawArc(color = plantColor, startAngle = 180f, sweepAngle = 90f, useCenter = true, topLeft = Offset(w * 0.25f, h * 0.35f), size = Size(w * 0.3f, h * 0.2f))
                }
                level <= 5 -> { // Küçük Bitki
                    drawRect(color = plantColor, topLeft = Offset(w * 0.45f, h * 0.2f), size = Size(w * 0.1f, h * 0.45f))
                    drawCircle(color = plantColor, radius = w * 0.15f, center = Offset(w * 0.35f, h * 0.4f))
                    drawCircle(color = plantColor, radius = w * 0.15f, center = Offset(w * 0.65f, h * 0.3f))
                }
                level <= 8 -> { // Çiçekli Bitki
                    drawRect(color = plantColor, topLeft = Offset(w * 0.45f, h * 0.1f), size = Size(w * 0.1f, h * 0.55f))
                    repeat(3) { i ->
                        drawCircle(color = plantColor, radius = w * 0.18f, center = Offset(w * (0.3f + i * 0.2f), h * (0.2f + i * 0.15f)))
                    }
                    // Çiçek
                    drawCircle(color = Color.Yellow, radius = w * 0.1f, center = Offset(w * 0.5f, h * 0.15f))
                }
                else -> { // Küçük Ağaç
                    drawRect(color = Color(0xFF3E2723), topLeft = Offset(w * 0.42f, h * 0.1f), size = Size(w * 0.16f, h * 0.55f))
                    repeat(5) { i ->
                        drawCircle(color = Color(0xFF1B5E20), radius = w * 0.22f, center = Offset(w * (0.2f + (i % 3) * 0.3f), h * (0.1f + (i / 2) * 0.2f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficeTabFull(vm: TaskViewModel, isEnglish: Boolean, context: Context, allTasksList: List<TaskEntity>, completedPomodorosToday: Int, onShowLiveSession: () -> Unit, onPeerClick: (String) -> Unit) {
    var showWeeklyAnalytics by rememberSaveable { mutableStateOf(false) }

    if (showWeeklyAnalytics) {
        com.focuspath.app.ui.screens.task.WeeklyAnalyticsDialog(vm, isEnglish) {
            showWeeklyAnalytics = false
        }
    }

    var showTeamDialog by remember { mutableStateOf(false) }

    if (showTeamDialog) {
        com.focuspath.app.ui.screens.task.TeamManagementDialog(vm, isEnglish) {
            showTeamDialog = false
        }
    }

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val workersCopy = vm.workers.toList()
    val isFocusActive by vm.isFocusActive // Odanın ışık durumunu bu belirler
    
    // ODA KARARTMA ANİMASYONU
    val ambientAlpha by animateFloatAsState(
        targetValue = if (isFocusActive) 0.5f else 1f,
        animationSpec = tween(1500), label = "OfficeLight"
    )

    var scale by remember { mutableFloatStateOf(0.85f) } ; val officeLevel = vm.officeLevel.value ; val coins = vm.userCoins.value ; val upgradeCost = officeLevel * 500 ; val employeeCount = 3 ; val deskCount = 3 ; val unlocked = vm.unlockedItems ; val fixedRotationX = 55f ; val fixedRotationY = 0f
    val baseFloorSize = 300f ; val dynamicFloorSize = baseFloorSize + (officeLevel - 1) * 40f ; val wallHalfSize = dynamicFloorSize / 2f ; var showLayoutDialog by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "YİME CENTER", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(text = "LVL $officeLevel • $coins 🪙", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // SİNERJİ BONUSU GÖSTERGESİ
                val isSynergy by vm.isTeamSynergyActive
                if (isSynergy) {
                    Surface(
                        color = AccentYellow.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AccentYellow.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, null, tint = AccentYellow, modifier = Modifier.size(14.dp))
                            Text("x1.2 BONUS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = AccentYellow)
                        }
                    }
                }

                // TAKIM MODU SWITCH
                if (vm.userTeam.value != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 4.dp)) {
                        Text(if(isEnglish) "TEAM" else "TAKIM", fontSize = 7.sp, color = if(vm.isTeamOfisMode.value) MaterialTheme.colorScheme.primary else Color.Gray)
                        Switch(
                            checked = vm.isTeamOfisMode.value,
                            onCheckedChange = { vm.toggleTeamOfficeMode(it) },
                            modifier = Modifier.scale(0.6f).height(20.dp)
                        )
                    }
                }
                
                IconButton(onClick = { showTeamDialog = true }) {
                    Icon(Icons.Default.Groups, null, tint = if(vm.userTeam.value != null) MaterialTheme.colorScheme.primary else Color.Gray)
                }

                IconButton(onClick = { vm.resetOfficePositions() }) {
                    Icon(Icons.Default.RestartAlt, null, tint = AccentRed)
                }
            }
        }
        if (showLayoutDialog) { AlertDialog(onDismissRequest = { showLayoutDialog = false }, title = { Text("Ofis Düzenleri") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Yazılım Ofisi" to "💻", "Tasarım Ofisi" to "🎨", "Gaming Ofisi" to "🎮", "CEO Ofisi" to "🏆").forEach { (name, emoji) -> val hasSaved = vm.prefs.getString("office_layout_$name", null) != null ; Row(modifier = Modifier.fillMaxWidth().clickable { if (hasSaved) vm.loadLayout(name) else vm.saveCurrentLayout(name) ; showLayoutDialog = false }.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Text(emoji, fontSize = 20.sp); Spacer(Modifier.width(12.dp)); Text(name, fontWeight = FontWeight.Bold) }; Text(if (hasSaved) "YÜKLE" else "KAYDET", color = if(hasSaved) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 10.sp) } } } }, confirmButton = { TextButton(onClick = { showLayoutDialog = false }) { Text("Kapat") } }) }
        Box(modifier = Modifier.fillMaxWidth().height(500.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF020202)).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> scale = (scale * zoom).coerceIn(0.6f, 2.2f) } }, contentAlignment = Alignment.Center) {
            
            // ANA 3D SAHNE - STABİL PERSPEKTİF
            Box(modifier = Modifier.size(dynamicFloorSize.dp).graphicsLayer { 
                rotationX = 55f // Sadece öne eğerek stabiliteyi sağlıyoruz
                scaleX = scale
                scaleY = scale
                alpha = ambientAlpha // ODA KARARMASI
                cameraDistance = 30f * density 
                transformOrigin = TransformOrigin.Center 
            }) {
                
                // 1. ARKA DUVAR
                Box(modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(180.dp)
                    .graphicsLayer {
                        rotationX = -90f 
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(Brush.verticalGradient(listOf(Color(0xFF151515), Color(0xFF0A0A0A))))
                    .border(1.dp, Color.White.copy(alpha = 0.05f))
                )

                // 2. SOL DUVAR
                Box(modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(180.dp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        rotationY = 90f
                        transformOrigin = TransformOrigin(1f, 0.5f)
                    }
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1E1E1E), Color(0xFF0D0D0D))))
                    .border(1.dp, Color.White.copy(alpha = 0.05f))
                )

                // 3. ZEMİN (Spot Işığı Buraya Eklenecek)
                Box(modifier = Modifier
                    .align(Alignment.Center)
                    .size(dynamicFloorSize.dp)
                    .background(Brush.radialGradient(listOf(Color(0xFF222222), Color(0xFF050505))))
                    .border(2.dp, Color.Black)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = size.width / 10
                        for (i in 0..10) {
                            drawLine(Color.White.copy(alpha = 0.04f), Offset(i * step, 0f), Offset(i * step, size.height), strokeWidth = 1.5f)
                            drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, i * step), Offset(size.width, i * step), strokeWidth = 1.5f)
                        }
                    }

                    // MASAYA SPOT IŞIĞI (Sadece odaklanırken)
                    if (isFocusActive) {
                        val myDesk = workersCopy.find { it.isMe }?.deskId
                        if (myDesk != null) {
                            val deskPos = vm.itemPositions[myDesk] ?: Offset.Zero
                            Box(modifier = Modifier
                                .offset { IntOffset(deskPos.x.toInt(), deskPos.y.toInt()) }
                                .size(200.dp)
                                .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(0.2f), Color.Transparent)))
                            )
                        }
                    }
                }
                
                // 4. KÖŞE BİRLEŞİM ÇİZGİSİ
                Box(modifier = Modifier.align(Alignment.TopStart).width(2.dp).height(180.dp).graphicsLayer { rotationX = -90f ; transformOrigin = TransformOrigin(0.5f, 1f) }.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))

                // Masalar ve Çalışanlar
                val deskPositionsList = listOf(
                    -90f to 0f, 0f to 0f, 90f to 0f
                )
                val totalWorkerCount = 3

                deskPositionsList.take(totalWorkerCount).forEachIndexed { idx, coords ->
                    key("desk_$idx") {
                        val (defaultDx, defaultDy) = coords
                        val deskId = "desk_setup_$idx"
                        val isVisible = vm.visibleItems.contains(deskId)
                        val savedDeskPos = vm.itemPositions[deskId] ?: Offset(defaultDx, defaultDy)

                        if (isVisible) {
                            // MASA VE OTURAN ÇALIŞAN ANİMASYONU
                            val animatedDeskOffset by androidx.compose.animation.core.animateIntOffsetAsState(
                                targetValue = IntOffset(
                                    (savedDeskPos.x * currentDensity.density).toInt(),
                                    (savedDeskPos.y * currentDensity.density).toInt()
                                ),
                                label = "DeskMovement"
                            )

                            Box(modifier = Modifier
                                .align(Alignment.Center)
                                .offset { animatedDeskOffset }
                                .pointerInput(deskId) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val sensitivity = 5.0f // Hassasiyeti artırdık (3.8f -> 5.0f)
                                            val currentPos = vm.itemPositions[deskId] ?: Offset(defaultDx, defaultDy)
                                            vm.updateItemPosition(
                                                id = deskId,
                                                offset = Offset(currentPos.x + (dragAmount.x * sensitivity / currentDensity.density), currentPos.y + (dragAmount.y * sensitivity / currentDensity.density)),
                                                isDragging = true,
                                                floorSize = dynamicFloorSize
                                            )
                                        },
                                        onDragEnd = {
                                            val currentPos = vm.itemPositions[deskId] ?: Offset(defaultDx, defaultDy)
                                            vm.updateItemPosition(deskId, currentPos, isDragging = false, floorSize = dynamicFloorSize)
                                        }
                                    )
                                }
                            ) {
                                // Sandalye
                                Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 45.dp)) { Box(modifier = Modifier.align(Alignment.Center).offset(y = 14.dp).size(34.dp, 34.dp)) { repeat(5) { i -> Box(modifier = Modifier.align(Alignment.Center).width(3.dp).height(16.dp).graphicsLayer { rotationZ = i * 72f }.background(Color(0xFF1A1A1A))) } }; Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 4.dp).size(6.dp, 12.dp).background(Brush.horizontalGradient(listOf(Color(0xFF222222), Color(0xFF444444), Color(0xFF111111))))); Box(modifier = Modifier.size(36.dp, 26.dp).background(Brush.verticalGradient(listOf(Color(0xFF2C2C2C), Color(0xFF121212))), RoundedCornerShape(12.dp)).border(1.2.dp, Color.Black.copy(0.6f), RoundedCornerShape(12.dp))); Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-20).dp).size(36.dp, 32.dp).graphicsLayer { rotationX = -105f; transformOrigin = TransformOrigin(0.5f, 1f) }.background(Brush.verticalGradient(listOf(Color(0xFF3A3A3A), Color(0xFF1E1E1E))), RoundedCornerShape(12.dp)).border(1.2.dp, Color.Gray.copy(0.3f), RoundedCornerShape(12.dp))) { repeat(3) { i -> Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter).offset(y = (8 + i * 8).dp).background(Color.Black.copy(0.2f))) } } }
                                // Masa
                                Box(modifier = Modifier.width(if (vm.isPremium.value) 76.dp else 70.dp).height(if (vm.isPremium.value) 36.dp else 34.dp)) {
                                    Box(modifier = Modifier.fillMaxSize().offset(y = 4.dp).background(if (vm.isPremium.value) Color(0xFF000000) else Color(0xFF3E2723), RoundedCornerShape(12.dp)));
                                    Box(modifier = Modifier.fillMaxSize().background(if (vm.isPremium.value) Brush.verticalGradient(listOf(Color(0xFF212121), Color(0xFF000000))) else Brush.verticalGradient(listOf(Color(0xFFA1887F), Color(0xFF6D4C41))), RoundedCornerShape(12.dp)).border(if (vm.isPremium.value) 1.dp else 0.5.dp, if (vm.isPremium.value) AccentYellow.copy(alpha = 0.5f) else Color(0xFFBCAAA4).copy(0.5f), RoundedCornerShape(12.dp))) {
                                        if (vm.isPremium.value) { Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter).offset(y = 2.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(0.1f), Color.Transparent)))) }

                                        // Klavye ve Mouse (Görsel)
                                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                            // Keyboard
                                            Box(modifier = Modifier.align(Alignment.CenterStart).size(width = 30.dp, height = 12.dp).background(Color(0xFF111111), RoundedCornerShape(1.dp)).border(0.5.dp, Color.Gray.copy(0.3f), RoundedCornerShape(1.dp))) {
                                                Row(modifier = Modifier.fillMaxSize().padding(1.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                                    repeat(4) { Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White.copy(0.05f))) }
                                                }
                                            }
                                            // Mouse
                                            Box(modifier = Modifier.align(Alignment.CenterEnd).offset(x = 4.dp).size(width = 6.dp, height = 9.dp).background(Color(0xFF111111), RoundedCornerShape(3.dp)).border(0.5.dp, Color.Gray.copy(0.3f), RoundedCornerShape(3.dp)))
                                        }
                                    }
                                }
                                // Monitör ve Glow
                                Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-30).dp).width(44.dp).height(32.dp)) {
                                    val isAnyWorkerFocusing = workersCopy.any { it.deskId == deskId && it.isFocusing }
                                    Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 12.dp).size(16.dp, 8.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(2.dp))); Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 4.dp).size(4.dp, 12.dp).background(Color(0xFF111111))); Box(modifier = Modifier.fillMaxSize().offset(x = 2.dp, y = 2.dp).background(Color.Black, RoundedCornerShape(4.dp))); Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp))) { Box(modifier = Modifier.fillMaxSize().padding(2.dp).background(Color.Black, RoundedCornerShape(2.dp))) { Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = if(isAnyWorkerFocusing) 0.6f else 0.3f), Color.Transparent, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))))); if(isAnyWorkerFocusing) { Text(text = workersCopy.find { it.deskId == deskId }?.monitorContent ?: "...", color = TerminalGreen.copy(0.7f), fontSize = 5.sp, modifier = Modifier.padding(2.dp).align(Alignment.Center)) } } }
                                }

                                // FOCUS TREE (Sadece kullanıcının masasında)
                                val isMyDesk = workersCopy.any { it.deskId == deskId && it.isMe }
                                if (isMyDesk) {
                                    PlantModel(level = vm.plantLevel.intValue, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-8).dp, y = (-20).dp))
                                }

                                // Çalışanlar (Masada Olanlar) - Z-INDEX: Masa karakterin arkasında kalsın (Y-sıralaması Box içinde)
                                workersCopy.filter { it.deskId == deskId && it.currentAction != WorkerAction.WALKING && it.currentAction != WorkerAction.COFFEE }.forEach { worker ->
                                    key(worker.id) {
                                        WorkerModel(worker = worker, fixedRotationX = fixedRotationX, selectedBuddy = vm.selectedFocusBuddy.value) {
                                            vm.workerDetails.value = worker
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Ofis içinde dolaşan çalışanlar - Y-Sıralaması: Y pozisyonuna göre sırala (Öndekiler daha sonra çizilsin)
                workersCopy
                    .filter { it.currentAction == WorkerAction.WALKING || it.currentAction == WorkerAction.COFFEE || (it.deskId.isEmpty() && it.currentAction != WorkerAction.WORKING) }
                    .forEach { worker ->
                        key(worker.id) {
                            // YÜRÜME ANİMASYONU: Pozisyon değişimlerini yumuşatıyoruz (Işınlanmayı engeller)
                            val animatedOffset by androidx.compose.animation.core.animateIntOffsetAsState(
                                targetValue = IntOffset(
                                    (worker.currentPos.x * currentDensity.density).toInt(),
                                    (worker.currentPos.y * currentDensity.density).toInt()
                                ),
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                ),
                                label = "WorkerMovement"
                            )

                            Box(modifier = Modifier
                                .align(Alignment.Center)
                                .offset { animatedOffset }
                            ) {
                                WorkerModel(worker = worker, fixedRotationX = fixedRotationX, selectedBuddy = vm.selectedFocusBuddy.value) {
                                    vm.workerDetails.value = worker
                                }
                            }
                        }
                    }

                val sceneItems = listOf(
                    "plant_1" to "🪴",
                    "coffee_1" to "☕",
                    "server_1" to "🖥️",
                    "glass_office_1" to "🪟",
                    "whiteboard_1" to "📋",
                    "lamp_1" to "💡",
                    "pc_1" to "💻",
                    "meeting_table_1" to "🤝",
                    "lava_lamp_1" to "🏮",
                    "arcade_1" to "🕹️",
                    "cat_1" to "🐱",
                    "robot_1" to "🤖",
                    "neon_sign_1" to "✨"
                )
                sceneItems.forEach { (id, emoji) ->
                    val isUnlocked = unlocked.contains(id) || id == "pc_1" || (id == "plant_1" && officeLevel >= 2) || (id == "glass_office_1" && officeLevel >= 3) || (id == "whiteboard_1" && officeLevel >= 4) || (id == "meeting_table_1" && officeLevel >= 4) || (id == "server_1" && officeLevel >= 5) || (id == "coffee_1" && officeLevel >= 6) || (id == "lamp_1" && officeLevel >= 7)
                    val isVisible = vm.visibleItems.contains(id) || (id == "meeting_table_1" && officeLevel >= 4 && !vm.visibleItems.contains(id))

                    if (isVisible && isUnlocked) {
                        val defaultPos = when(id) {
                            "meeting_table_1" -> Offset(-120f, 60f)
                            "coffee_1" -> Offset(120f, -60f)
                            "server_1" -> Offset(-120f, -60f)
                            else -> Offset(0f, 0f)
                        }
                        val savedPos = vm.itemPositions[id] ?: defaultPos

                        // 3D DUVAR EŞYALARI MANTIĞI (Pencere, Beyaz Tahta ve Neon Tabela duvarda durmalı)
                        val isWallItem = id == "glass_office_1" || id == "whiteboard_1" || id == "neon_sign_1"

                        Box(modifier = Modifier
                            .align(Alignment.Center)
                            .offset { 
                                if (isWallItem) {
                                    // X moves left/right. Y is fixed at back wall.
                                    IntOffset((savedPos.x * currentDensity.density).toInt(), (-dynamicFloorSize/2 * currentDensity.density).toInt())
                                } else {
                                    IntOffset((savedPos.x * currentDensity.density).toInt(), (savedPos.y * currentDensity.density).toInt())
                                }
                            }
                            .graphicsLayer {
                                if (isWallItem) {
                                    // Move UP the wall based on savedPos.y (Dragging down on floor -> moves UP on wall)
                                    translationY = (savedPos.y * currentDensity.density) - 100f // Base height offset
                                }
                            }
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .pointerInput(id) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val sensitivity = 5.0f
                                        val currentPos = vm.itemPositions[id] ?: defaultPos
                                        vm.updateItemPosition(
                                            id = id,
                                            offset = Offset(
                                                currentPos.x + (dragAmount.x * sensitivity / currentDensity.density),
                                                currentPos.y + (dragAmount.y * sensitivity / currentDensity.density)
                                            ),
                                            isDragging = true,
                                            floorSize = dynamicFloorSize
                                        )
                                    },
                                    onDragEnd = {
                                        val currentPos = vm.itemPositions[id] ?: defaultPos
                                        vm.updateItemPosition(id, currentPos, isDragging = false, floorSize = dynamicFloorSize)
                                    }
                                )
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            if (id == "server_1") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Server Gölgesi
                                    Box(modifier = Modifier.size(36.dp, 12.dp).offset(y = 55.dp).background(Color.Black.copy(0.3f), CircleShape))
                                    Box(modifier = Modifier.size(40.dp, 60.dp).background(Color(0xFF111111), RoundedCornerShape(8.dp)).border(1.dp, Color.Gray.copy(0.3f), RoundedCornerShape(8.dp))) {
                                        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            repeat(6) { i ->
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    val infiniteTransition = rememberInfiniteTransition()
                                                    val ledAlpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500 + i * 100), RepeatMode.Reverse))
                                                    Box(modifier = Modifier.size(4.dp, 2.dp).background(if(i % 2 == 0) TerminalGreen.copy(ledAlpha) else Color.Cyan.copy(ledAlpha)))
                                                    Box(modifier = Modifier.weight(1f).height(2.dp).background(Color.Gray.copy(0.2f)))
                                                }
                                            }
                                        }
                                    }
                                    Text("RACK-0${officeLevel}", fontSize = 6.sp, color = TerminalGreen.copy(0.5f))
                                }
                            } else if (id == "meeting_table_1") {
                                Box(modifier = Modifier.size(80.dp, 40.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp)).border(2.dp, Color.Gray.copy(0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Text("MEETING CENTER", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                }
                            } else if (id == "lava_lamp_1") {
                                // 3D LAVA LAMBASI VE GÜÇLÜ IŞIK
                                Box(contentAlignment = Alignment.Center) {
                                    // Işık Haresi (Glow) - DAHA GÜÇLÜ
                                    Box(modifier = Modifier.size(150.dp).background(Brush.radialGradient(listOf(Color.Red.copy(0.6f), Color.Transparent)), CircleShape))
                                    // Lamba Tabanı
                                    Box(modifier = Modifier.size(24.dp, 8.dp).offset(y = 12.dp).background(Color.DarkGray, CircleShape))
                                    Text(emoji, fontSize = 28.sp)
                                }
                            } else if (id == "neon_sign_1") {
                                // 3D NEON TABELA: Duvarda parlar
                                val flickerTransition = rememberInfiniteTransition()
                                val flickerAlpha by flickerTransition.animateFloat(
                                    initialValue = 0.8f, targetValue = 1f,
                                    animationSpec = infiniteRepeatable(tween(100), RepeatMode.Reverse)
                                )
                                
                                Box(modifier = Modifier
                                    .size(100.dp, 40.dp)
                                    .graphicsLayer {
                                        rotationX = -90f
                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                        alpha = flickerAlpha
                                    }
                                    .background(Color.Black.copy(0.8f), RoundedCornerShape(8.dp))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Neon Işığı (Glow) - DAHA GÜÇLÜ
                                    Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(0.4f), Color.Transparent))))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("FOCUS", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        Text("+5% XP BOOST", color = MaterialTheme.colorScheme.primary.copy(0.7f), fontSize = 6.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else if (id == "lamp_1") {
                                // MASA LAMBASI IŞIĞI - DAHA GÜÇLÜ
                                Box(contentAlignment = Alignment.Center) {
                                    Box(modifier = Modifier.size(120.dp).background(Brush.radialGradient(listOf(AccentYellow.copy(0.5f), Color.Transparent)), CircleShape))
                                    Text(emoji, fontSize = 24.sp)
                                }
                            } else if (id == "arcade_1") {
                                // ATARİ MAKİNESİ IŞIĞI
                                Box(contentAlignment = Alignment.Center) {
                                    Box(modifier = Modifier.size(110.dp).background(Brush.radialGradient(listOf(Color.Magenta.copy(0.3f), Color.Transparent)), CircleShape))
                                    Text(emoji, fontSize = 32.sp)
                                }
                            } else if (id == "robot_1") {
                                // ROBOT GÖZ IŞIĞI
                                Box(contentAlignment = Alignment.Center) {
                                    Box(modifier = Modifier.size(70.dp).background(Brush.radialGradient(listOf(Color.Cyan.copy(0.5f), Color.Transparent)), CircleShape))
                                    Text(emoji, fontSize = 24.sp)
                                }
                            } else if (id == "plant_1") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(modifier = Modifier.size(24.dp, 8.dp).background(Color.Black.copy(alpha = 0.2f), CircleShape)); Text(emoji, fontSize = 24.sp, modifier = Modifier.offset(y = (-20).dp)) }
                            } else {
                                // Diğer eşyalar için yer gölgesi
                                Box(modifier = Modifier.size(24.dp, 8.dp).offset(y = 12.dp).background(Color.Black.copy(0.2f), CircleShape))
                                Text(emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
        val totalTasks = allTasksList.size ; val doneTasks = allTasksList.count { it.isCompleted } ; val completionRate = if (totalTasks > 0) (doneTasks.toFloat() / totalTasks) else 0f
        Card(modifier = Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures { showWeeklyAnalytics = true } }, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))) { Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (isEnglish) "PERFORMANCE ANALYTICS" else "PERFORMANS ANALİZİ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(60.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), strokeWidth = 4.dp); Icon(Icons.Default.Timer, null, tint = AccentYellow.copy(alpha = 0.6f), modifier = Modifier.size(24.dp)) }; Spacer(Modifier.height(8.dp)); Text("$completedPomodorosToday", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)); Text(if (isEnglish) "Focus" else "Odak", fontSize = 10.sp, color = Color.Gray) }; Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = { completionRate }, modifier = Modifier.size(60.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), strokeWidth = 4.dp, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)); Text("${(completionRate * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)) }; Spacer(Modifier.height(8.dp)); Text("$doneTasks/$totalTasks", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)); Text(if (isEnglish) "Tasks" else "Görev", fontSize = 10.sp, color = Color.Gray) }; Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(60.dp), color = AccentRed.copy(alpha = 0.05f), strokeWidth = 4.dp); Icon(Icons.Default.Whatshot, null, tint = AccentRed.copy(alpha = 0.6f), modifier = Modifier.size(24.dp)) }; Spacer(Modifier.height(8.dp)); Text("${vm.userStreak.value}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)); Text(if (isEnglish) "Streak" else "Seri", fontSize = 10.sp, color = Color.Gray) } } } }
        Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) { Column(modifier = Modifier.padding(16.dp)) { Text(text = if (isEnglish) "DOPAMINE STORE" else "DOPAMİN MAĞAZASI", style = MaterialTheme.typography.titleMedium, color = AccentYellow.copy(alpha = 0.8f)); Spacer(Modifier.height(12.dp));
            val storeItems = listOf(
                Triple("lava_lamp_1", "🏮", 300),
                Triple("neon_sign_1", "✨", 700),
                Triple("arcade_1", "🕹️", 1800),
                Triple("cat_1", "🐱", 2500),
                Triple("robot_1", "🤖", 3500)
            )
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                storeItems.forEach { (id, emoji, cost) ->
                    val isOwned = unlocked.contains(id)
                    Surface(
                        onClick = { 
                            if(isOwned) vm.toggleItemVisibility(id) 
                            else vm.buyItem(id, cost) 
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if(isOwned) {
                            if (vm.visibleItems.contains(id)) MaterialTheme.colorScheme.primary.copy(0.1f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                        } else MaterialTheme.colorScheme.primary.copy(0.05f),
                        border = BorderStroke(1.dp, if(isOwned) {
                            if (vm.visibleItems.contains(id)) MaterialTheme.colorScheme.primary.copy(0.5f)
                            else Color.Gray.copy(0.3f)
                        } else AccentYellow.copy(alpha = 0.4f)),
                        modifier = Modifier.width(100.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emoji, fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            if (isOwned) {
                                val isVisible = vm.visibleItems.contains(id)
                                Text(if(isVisible) (if(isEnglish) "ACTIVE" else "AKTİF") else (if(isEnglish) "OFF" else "PASİF"), 
                                    fontSize = 9.sp, 
                                    color = if(isVisible) MaterialTheme.colorScheme.primary else Color.Gray, 
                                    fontWeight = FontWeight.Bold)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$cost", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AccentYellow)
                                    Spacer(Modifier.width(2.dp))
                                    Text("🪙", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        } }

        Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) { 
            Column(modifier = Modifier.padding(16.dp)) { 
                Text(text = if (isEnglish) "UNLOCKED ASSETS" else "AÇILAN EŞYALAR", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                Spacer(Modifier.height(8.dp))
                
                val otherItems = listOf(
                    "pc_1" to "💻", "plant_1" to "🪴", "glass_office_1" to "🪟", "whiteboard_1" to "📋", 
                    "server_1" to "🖥️", "coffee_1" to "☕", "lamp_1" to "💡",
                    "arcade_1" to "🕹️", "cat_1" to "🐱", "robot_1" to "🤖", "lava_lamp_1" to "🏮", "neon_sign_1" to "✨"
                )

                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // MASALARI TEK BİR BUTONDA TOPLADIK (SADECE AÇIK OLANLARI GÖSTER)
                    val totalAllowedDesks = (officeLevel * 3).coerceAtMost(12)
                    val activeDesks = (0 until totalAllowedDesks).filter { vm.visibleItems.contains("desk_setup_$it") }.size

                    Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(0.1f), RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🪑", fontSize = 18.sp)
                            Text(text = "$activeDesks/$totalAllowedDesks", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        }
                    }

                    otherItems.forEach { (id, emoji) ->
                        val isUnlocked = unlocked.contains(id) || id.startsWith("desk_setup_") || id == "pc_1" || (id == "plant_1" && officeLevel >= 2) || (id == "glass_office_1" && officeLevel >= 3) || (id == "whiteboard_1" && officeLevel >= 4) || (id == "server_1" && officeLevel >= 5) || (id == "coffee_1" && officeLevel >= 6) || (id == "lamp_1" && officeLevel >= 7)
                        if (isUnlocked) { 
                            val isVisibleInScene = vm.visibleItems.contains(id) 
                            Box(modifier = Modifier.size(48.dp).background(if (isVisibleInScene) MaterialTheme.colorScheme.primary.copy(0.1f) else Color.Gray.copy(0.05f), RoundedCornerShape(12.dp)).border(1.dp, if (isVisibleInScene) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Gray.copy(0.2f), RoundedCornerShape(12.dp)).clickable { val defaultPos = if (id.startsWith("desk_setup_")) Offset(0f, -30f) else Offset(0f, 0f) ; vm.toggleItemVisibility(id, defaultPos) }, contentAlignment = Alignment.Center) { 
                                Text(text = emoji, fontSize = 24.sp, modifier = Modifier.alpha(if (isVisibleInScene) 1f else 0.4f))
                                if (!isVisibleInScene) Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(12.dp).align(Alignment.TopEnd).padding(2.dp)) 
                                else Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(12.dp).align(Alignment.TopEnd).padding(2.dp)) 
                            } 
                        } else { 
                            Surface(shape = RoundedCornerShape(12.dp), color = Color.Gray.copy(alpha = 0.1f), border = BorderStroke(1.dp, Color.Gray.copy(0.3f)), modifier = Modifier.size(48.dp)) { 
                                Box(contentAlignment = Alignment.Center) { Text(text = "🔒", fontSize = 18.sp) } 
                            } 
                        }
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))) { Column(modifier = Modifier.padding(16.dp)) { Text(text = if (isEnglish) "OFFICE UPGRADES" else "OFİS YÜKSELTMELERİ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("Cost", fontSize = 12.sp, color = Color.Gray); Text("$upgradeCost 🪙", fontWeight = FontWeight.Bold, color = AccentYellow.copy(alpha = 0.9f)) }; Button(onClick = { if (coins >= upgradeCost) vm.upgradeOffice() }, colors = ButtonDefaults.buttonColors(containerColor = if (coins >= upgradeCost) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f))) { Text(if (isEnglish) "UPGRADE" else "YÜKSELT", color = Color.Black) } } } }
        Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) { Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$deskCount", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp); Text(if (isEnglish) "Desks" else "Masa", fontSize = 11.sp, color = Color.Gray) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$employeeCount", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp); Text(if (isEnglish) "Staff" else "Çalışan", fontSize = 11.sp, color = Color.Gray) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$officeLevel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp); Text(if (isEnglish) "Level" else "Seviye", fontSize = 11.sp, color = Color.Gray) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$coins", fontWeight = FontWeight.Bold, color = AccentYellow, fontSize = 18.sp); Text("Coins", fontSize = 11.sp, color = Color.Gray) } } }
        Button(onClick = onShowLiveSession, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(if (isEnglish) "JOIN ACTIVE SESSION" else "AKTİF OTURUMA KATIL", color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Spacer(Modifier.height(32.dp))
    }

    vm.workerDetails.value?.let { worker ->
        WorkerDetailDialog(
            vm = vm,
            worker = worker,
            isEnglish = isEnglish,
            onPeerClick = onPeerClick
        ) {
            vm.workerDetails.value = null
        }
    }
}


@Composable
fun MonotaskingDialog(vm: TaskViewModel, isEnglish: Boolean) {
    val task = vm.monotask.value ?: return
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { vm.isMonotasking.value = false },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    if(isEnglish) "MONOTASKING MODE" else "TEK GÖREV MODU",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(48.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (task.notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(text = task.notes, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(64.dp))

                Button(
                    onClick = { vm.toggleTask(task) ; vm.isMonotasking.value = false },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !task.isCompleted
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if(isEnglish) "COMPLETE TASK" else "GÖREVİ TAMAMLA", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { vm.isMonotasking.value = false },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(if(isEnglish) "EXIT" else "ÇIK", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DailyBriefingDialog(vm: TaskViewModel, isEnglish: Boolean) {
    AlertDialog(
        onDismissRequest = { vm.showDailyBriefing.value = false },
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbSunny, null, tint = AccentYellow)
                Spacer(Modifier.width(8.dp))
                Text(if (isEnglish) "Morning Briefing" else "Sabah Brifingi")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (vm.isBriefingLoading.value) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(if(isEnglish) "YimeBot is analyzing your day..." else "YimeBot gününü analiz ediyor...", fontSize = 12.sp, color = Color.Gray)
                } else {
                    Text(
                        text = vm.dailyBriefingText.value ?: "",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 20.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { vm.showDailyBriefing.value = false }) {
                Text(if (isEnglish) "LET'S WORK!" else "BAŞLAYALIM!")
            }
        }
    )
}

@Composable
fun DecisionSpinnerDialog(vm: TaskViewModel, isEnglish: Boolean) {
    val taskList by vm.tasks.collectAsState(initial = emptyList())
    val rotation = remember { Animatable(0f) }
    val resultTask = vm.spinnerResult.value
    val resultAction = vm.spinnerAction.value
    val isActive = vm.isDecisionSpinnerActive.value

    // Çarkın duracağı rastgele açıyı ViewModel üzerinden veya tıklama anında yönetmeliyiz
    // Senkronizasyon için tıklama anındaki randomDegrees'i kullanacağız
    LaunchedEffect(isActive) {
        if (isActive) {
            // ViewModel'deki bekleme süresiyle uyumlu (3.2s)
            val actionPhrasesCount = 8
            val segmentAngle = 360f / actionPhrasesCount

            // ViewModel'deki finalIndex'e karşılık gelen açıyı hesapla
            val actionPhrases = if (isEnglish) {
                listOf("DO IT NOW!", "POSTPONE", "5 MIN BREAK", "FOCUS!", "DEEP WORK", "QUICK WIN", "SKIP IT", "JUST START")
            } else {
                listOf("ŞİMDİ YAP!", "ERTELE", "5 DK MOLA", "ODAKLAN!", "DERİN ÇALIŞ", "HIZLI BİTİR", "PAS GEÇ", "SADECE BAŞLA")
            }
            val targetIdx = actionPhrases.indexOf(resultAction)

            // pointerPos = 270. finalIndex = (270 - finalDegrees) / segmentAngle
            // finalDegrees = 270 - (targetIdx * segmentAngle) - (segmentAngle/2)
            val targetAngle = (270f - (targetIdx * segmentAngle) - (segmentAngle / 2f) + 360f) % 360f

            rotation.animateTo(
                targetValue = rotation.value + 1440f + targetAngle,
                animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing)
            )
        } else if (resultAction == null) {
            rotation.snapTo(0f)
        }
    }

    AlertDialog(
        onDismissRequest = { vm.spinnerResult.value = null ; vm.spinnerAction.value = null },
        title = { Text(if(isEnglish) "Decision Spinner" else "Karar Çarkı", color = AccentYellow, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        shape = RoundedCornerShape(12.dp),
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if(isEnglish) "Let the fate decide your next task!" else "Kaderin bir sonraki görevini seçmesine izin ver!",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                    // THE WHEEL
                    val activeTasks = taskList.filter { !it.isCompleted }
                    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation.value }) {
                        val canvasSize = size.minDimension
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = canvasSize / 2

                        val segmentCount = 8
                        val sweepAngle = 360f / segmentCount
                        val colors = listOf(
                            Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
                            Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4),
                            Color(0xFF00BCD4), Color(0xFF009688)
                        )

                        val actionPhrases = if (isEnglish) {
                            listOf("DO IT NOW!", "POSTPONE", "5 MIN BREAK", "FOCUS!", "DEEP WORK", "QUICK WIN", "SKIP IT", "JUST START")
                        } else {
                            listOf("ŞİMDİ YAP!", "ERTELE", "5 DK MOLA", "ODAKLAN!", "DERİN ÇALIŞ", "HIZLI BİTİR", "PAS GEÇ", "SADECE BAŞLA")
                        }

                        for (i in 0 until segmentCount) {
                            val startAngle = i * sweepAngle
                            drawArc(
                                color = colors[i % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                size = Size(canvasSize, canvasSize),
                                topLeft = Offset((size.width - canvasSize) / 2, (size.height - canvasSize) / 2)
                            )

                            // Yazı çizme mantığı (Eylem cümleleri)
                            val phrase = actionPhrases[i % actionPhrases.size]
                            val angleRad = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                            val textRadius = radius * 0.65f
                            val x = center.x + Math.cos(angleRad).toFloat() * textRadius
                            val y = center.y + Math.sin(angleRad).toFloat() * textRadius

                            drawContext.canvas.nativeCanvas.apply {
                                save()
                                translate(x, y)
                                rotate(startAngle + sweepAngle / 2 + 90f)
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 22f // Daha okunaklı boyut
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    isAntiAlias = true
                                }
                                drawText(phrase, 0f, 0f, paint)
                                restore()
                            }
                        }

                        // Wheel border
                        drawCircle(
                            color = Color.White.copy(alpha = 0.2f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    // Center point
                    Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(12.dp), border = BorderStroke(2.dp, Color.Black)) {}

                    // Pointer (Fixed at top)
                    Icon(
                        Icons.Default.ArrowDropDown,
                        null,
                        tint = AccentYellow,
                        modifier = Modifier.size(48.dp).align(Alignment.TopCenter).offset(y = (-20).dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                AnimatedVisibility(visible = !isActive && resultTask != null) {
                    Card(
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (resultAction != null) {
                                Text(
                                    text = resultAction,
                                    color = AccentYellow,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(if(isEnglish) "FOR TASK:" else "GÖREV:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(resultTask?.title ?: "", fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.spinnerResult.value = null ; vm.spinnerAction.value = null },
                enabled = !isActive,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if(isEnglish) "LET'S GO!" else "HADİ BAŞLAYALIM!", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun TomorrowPlanningDialog(vm: TaskViewModel, isEnglish: Boolean, onDismiss: () -> Unit) {
    val allTasks by vm.allTasks.collectAsState(initial = emptyList())
    val selectedDate by vm.selectedDate.collectAsState()
    val context = LocalContext.current

    val targetCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val targetDateStr = SimpleDateFormat("dd MMMM", Locale.getDefault()).format(targetCal.time).uppercase()
    val dayNameStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(targetCal.time).uppercase()

    val targetTasks = allTasks.filter { isSameDay(it.dueDate, selectedDate) }
    var newTaskTitle by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Event, 
                                null, 
                                tint = MaterialTheme.colorScheme.primary, 
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if(isEnglish) "DAILY PLAN" else "GÜNLÜK PLAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                        }
                        Text(
                            text = targetDateStr,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dayNameStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // TASK LIST
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp),
                    color = Color.Black.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    if (targetTasks.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Inbox, null, modifier = Modifier.size(40.dp), tint = Color.Gray.copy(alpha = 0.2f))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if(isEnglish) "No plans yet." else "Henüz bir plan yok.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(targetTasks, key = { it.id }) { task ->
                                Surface(
                                    modifier = Modifier.animateItem(),
                                    color = if (task.isCompleted) Color.Gray.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(0.5.dp, if (task.isCompleted) Color.Transparent else Color.Gray.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Circle,
                                            contentDescription = null,
                                            tint = if (task.isCompleted) TerminalGreen else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = task.title,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        IconButton(
                                            onClick = { vm.deleteTask(task) }, 
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, null, tint = AccentRed.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // QUICK ADD AREA
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if(isEnglish) "QUICK ADD" else "HIZLI EKLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if(isEnglish) "COMMAND" else "KOMUT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 8.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    placeholder = { Text(if(isEnglish) "What needs to be done?" else "Ne yapılması gerekiyor?", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    leadingIcon = {
                        Text(">", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                    },
                    trailingIcon = {
                        if (newTaskTitle.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val taskId = System.currentTimeMillis()
                                    vm.addTask(newTaskTitle, "", "Genel", 1, selectedDate)

                                    val reminderCal = Calendar.getInstance().apply {
                                        timeInMillis = selectedDate
                                        set(Calendar.HOUR_OF_DAY, 9)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                    }
                                    
                                    // Eğer seçilen saat geçmişse, 10 dakika sonraya kur (Bugün için)
                                    val finalTriggerTime = if (reminderCal.timeInMillis <= System.currentTimeMillis()) {
                                        System.currentTimeMillis() + (10 * 60 * 1000L) 
                                    } else {
                                        reminderCal.timeInMillis
                                    }
                                    
                                    // ID çakışmasını önlemek için güvenli bir integer oluştur
                                    val safeId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                                    setTaskAlarm(context, newTaskTitle, safeId, finalTriggerTime)
                                    
                                    newTaskTitle = ""
                                },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newTaskTitle.isNotBlank()) {
                            val taskId = System.currentTimeMillis()
                            vm.addTask(newTaskTitle, "", "Genel", 1, selectedDate)

                            val reminderCal = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                            
                            val finalTriggerTime = if (reminderCal.timeInMillis <= System.currentTimeMillis()) {
                                System.currentTimeMillis() + (10 * 60 * 1000L)
                            } else {
                                reminderCal.timeInMillis
                            }

                            val safeId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                            setTaskAlarm(context, newTaskTitle, safeId, finalTriggerTime)
                            
                            newTaskTitle = ""
                        }
                    })
                )

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(if (isEnglish) "DONE" else "TAMAM", fontWeight = FontWeight.Black, color = Color.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

private fun setTaskAlarm(context: Context, title: String, id: Int, triggerTime: Long) {
    val intent = Intent(context, ReminderReceiver::class.java).apply { 
        putExtra("task_title", title) 
    }
    // FLAG_MUTABLE ekledik çünkü bazı Android sürümlerinde Intent ekstraları için gerekli olabiliyor
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }
    
    val pendingIntent = PendingIntent.getBroadcast(context, id, intent, flags)
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
    } catch (e: Exception) {
        // Herhangi bir hata durumunda en temel yöntemle kurmaya çalış
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
}


@Composable
fun TaskSlicerDialog(vm: TaskViewModel, isEnglish: Boolean) {
    AlertDialog(
        onDismissRequest = { vm.slicedTasks.clear() },
        shape = RoundedCornerShape(12.dp),
        title = { Text(if(isEnglish) "AI Task Slicer" else "AI Görev Parçalayıcı", color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                if (vm.isSlicingTask.value) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(if(isEnglish) "Slicing with ADHD brain in mind..." else "DEHB dostu adımlara bölünüyor...", fontSize = 10.sp)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(vm.slicedTasks) { step ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.SubdirectoryArrowRight, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(Modifier.width(8.dp))
                                Text(step, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.slicedTasks.clear() }) { Text("OK") }
        }
    )
}

private fun isSameMonth(millis1: Long, millis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
}

private fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTabFull(vm: TaskViewModel, allTasks: List<TaskEntity>, lang: Map<String, String>, isEnglish: Boolean, totalFocusMinutes: Int, completedSessions: Int, interruptedSessions: Int, onTabChange: (Int) -> Unit, onShowCertificate: (Boolean) -> Unit) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val leaderboard by vm.leaderboard.collectAsState()
    
    val onlineCount = leaderboard.count { it.isFocusing }
    val top3 = leaderboard.take(3)
    
    val totalCoins = vm.userCoins.value
    val totalXp = vm.userXp.value
    
    // Bugünün başlangıcını bul (00:00:00)
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val upcomingTasks = allTasks.filter { !it.isCompleted && it.dueDate >= startOfToday }.sortedBy { it.dueDate }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "home_header") {
            Row(modifier = Modifier.fillMaxWidth().animateItemPlacement(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val showHomeProfile = vm.isLoggedIn.value || vm.userPhotoUrl.value != null
                    if (showHomeProfile) {
                        ProfileImage(
                            photoUrl = vm.userPhotoUrl.value,
                            name = vm.userName.value,
                            email = vm.userEmail.value,
                            size = 32.dp,
                            border = if (!vm.isLoggedIn.value) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)) else null
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = lang["home"] ?: "Ana Sayfa",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Surface(
                    color = TerminalGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(TerminalGreen, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if(isEnglish) "$onlineCount ONLINE" else "$onlineCount AKTİF",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // DAILY CHALLENGE CARD
        val yesterdayMins = vm.yesterdayFocusMins.intValue
        val targetMins = vm.todayChallengeTarget.intValue
        if (yesterdayMins > 0) {
            item(key = "daily_challenge") {
                Card(
                    modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentYellow.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, AccentYellow.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯", fontSize = 24.sp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "DAILY CHALLENGE" else "GÜNLÜK MEYDAN OKUMA",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentYellow,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEnglish) 
                                    "Yesterday you focused for $yesterdayMins min. Can we beat $targetMins min today?" 
                                    else "Dün $yesterdayMins dakika odaklandın. Bugün $targetMins dakikayı geçebilir miyiz?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        item(key = "home_leaderboard") {
            Card(
                modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var lbTabState by remember { mutableIntStateOf(0) }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if(isEnglish) "LEADERBOARD" else "LİDERLİK TABLOSU", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Row {
                            TextButton(onClick = { lbTabState = 0 }) {
                                Text(if(isEnglish) "PLAYERS" else "OYUNCULAR", fontSize = 10.sp, color = if(lbTabState == 0) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
                            TextButton(onClick = { lbTabState = 1 }) {
                                Text(if(isEnglish) "TEAMS" else "TAKIMLAR", fontSize = 10.sp, color = if(lbTabState == 1) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    if (lbTabState == 0) {
                        // OYUNCULAR (TOP 3)
                        if (vm.isLeaderboardLoading.value && top3.isEmpty()) {
                            Text(if(isEnglish) "Loading players..." else "Oyuncular yükleniyor...", fontSize = 12.sp, color = Color.Gray)
                        } else if (top3.isEmpty()) {
                            Text(if(isEnglish) "No data found." else "Henüz oyuncu yok.", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                top3.forEachIndexed { index, user ->
                                    val displayPhoto = user.photoUrl
                                    
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(contentAlignment = Alignment.Center) {
                                            ProfileImage(
                                                photoUrl = displayPhoto,
                                                name = user.name,
                                                email = user.email,
                                                size = 48.dp,
                                                border = BorderStroke(
                                                    width = 2.dp,
                                                    color = when(index) {
                                                        0 -> Color(0xFFFFD700) // Gold
                                                        1 -> Color(0xFFC0C0C0) // Silver
                                                        else -> Color(0xFFCD7F32) // Bronze
                                                    }
                                                )
                                            )
                                            if (user.isFocusing) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .align(Alignment.BottomEnd)
                                                        .background(TerminalGreen, CircleShape)
                                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = user.name.split(" ").firstOrNull() ?: "User",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${user.score} XP",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 8.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // TAKIMLAR (TOP 3)
                        val teamLb by vm.teamLeaderboard.collectAsState()
                        val top3Teams = teamLb.take(3)
                        
                        if (top3Teams.isEmpty()) {
                            Text(if(isEnglish) "No teams found." else "Henüz takım bulunamadı.", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                top3Teams.forEachIndexed { index, team ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            modifier = Modifier.size(48.dp),
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.4f),
                                            border = BorderStroke(
                                                width = 2.dp,
                                                color = when(index) {
                                                    0 -> Color(0xFFFFD700) 
                                                    1 -> Color(0xFFC0C0C0) 
                                                    else -> Color(0xFFCD7F32) 
                                                }
                                            )
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(team.name.take(1).uppercase(), fontWeight = FontWeight.Black, color = Color.White)
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = team.name.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${team.totalTeamXp} XP",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 8.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "home_rewards") {
            Card(
                modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if(isEnglish) "REWARDS & PROGRESS" else "ÖDÜLLER VE İLERLEME", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🪙", fontSize = 24.sp)
                            Text("$totalCoins", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(if(isEnglish) "Coins" else "Coin", fontSize = 10.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⭐", fontSize = 24.sp)
                            Text("${com.focuspath.app.util.FocusRank.getTitle(totalXp.toLong(), isEnglish)} / $totalXp", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text(if(isEnglish) "Title / Level" else "Şirket Unvanı / Seviyesi", fontSize = 10.sp, color = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            
                            val isCertificateUnlocked = totalXp >= 500
                            
                            TextButton(
                                onClick = { 
                                    if(isCertificateUnlocked) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onShowCertificate(true) 
                                    }
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp),
                                enabled = true
                            ) {
                                Icon(
                                    Icons.Default.Verified, 
                                    null, 
                                    modifier = Modifier.size(14.dp), 
                                    tint = if(isCertificateUnlocked) com.focuspath.app.util.FocusRank.getColor(totalXp.toLong()) else Color.Gray
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if(isCertificateUnlocked) (if(isEnglish) "View Certificate" else "Sertifikayı Gör") 
                                           else (if(isEnglish) "$totalXp/500 XP" else "$totalXp/500 XP"), 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = if(isCertificateUnlocked) com.focuspath.app.util.FocusRank.getColor(totalXp.toLong()) else Color.Gray
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥", fontSize = 24.sp)
                            Text("${vm.userStreak.value}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(if(isEnglish) "Streak" else "Seri", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        item(key = "home_sessions") {
            Card(
                modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if(isEnglish) "FOCUS SESSIONS (TODAY)" else "ODAKLANMA SEANSLARI (BUGÜN)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(color = TerminalGreen.copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                                Text("$completedSessions", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = TerminalGreen, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }
                            Text(if(isEnglish) "COMPLETED" else "TAMAMLANAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(color = AccentRed.copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                                Text("$interruptedSessions", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = AccentRed, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }
                            Text(if(isEnglish) "INTERRUPTED" else "YARIM KALAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    
                    if (interruptedSessions > 0) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if(isEnglish) "You have interrupted sessions." else "Yarım kalan seansların var.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { 
                                    vm.startRecoverySession(context)
                                    onTabChange(3) // Takvim/Timer tabına at
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    if(isEnglish) "RECOVER" else "TELAFİ ET", 
                                    color = MaterialTheme.colorScheme.primary, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "home_reminders") {
            Card(
                modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if(isEnglish) "REMINDERS & UPCOMING" else "HATIRLATMALAR VE SIRADAKİLER", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Icon(Icons.Default.Notifications, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    }
                    Spacer(Modifier.height(12.dp))
                    
                    if (upcomingTasks.isEmpty()) {
                        Text(if(isEnglish) "No upcoming reminders." else "Yaklaşan hatırlatıcı yok.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        upcomingTasks.take(5).forEach { task ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(if(task.priority == 2) AccentRed else MaterialTheme.colorScheme.primary, CircleShape))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(task.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.dueDate))
                                    Text(timeStr, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "home_motivation") {
            val quotes = listOf(
                "Focus on being productive instead of busy.",
                "Your focus determines your reality.",
                "Work hard in silence, let success be your noise.",
                "Deep work is the superpower of the 21st century.",
                "The only way to do great work is to love what you do."
            )
            val quotesTr = listOf(
                "Meşgul olmak yerine üretken olmaya odaklan.",
                "Odak noktan gerçekliğini belirler.",
                "Sessizce sıkı çalış, bırak gürültüyü başarın yapsın.",
                "Derin çalışma, 21. yüzyılın süper gücüdür.",
                "Harika işler yapmanın tek yolu, yaptığınız işi sevmektir."
            )

            val hourIndex = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) % quotes.size
            val currentQuoteText = if (isEnglish) quotes[hourIndex] else quotesTr[hourIndex]

            Card(
                modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if(isEnglish) "DAILY MOTIVATION" else "GÜNLÜK MOTİVASYON",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AsyncImage(
                        model = com.focuspath.app.R.drawable.ancient_library,
                        contentDescription = "Focus Motivation",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = currentQuoteText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontWeight = FontWeight.Bold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
        item(key = "home_spacer") { Spacer(Modifier.height(80.dp).animateItemPlacement()) }
    }
}





