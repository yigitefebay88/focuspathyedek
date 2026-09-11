package com.focuspath.shared

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focuspath.shared.model.Task
import com.focuspath.shared.model.User
import com.focuspath.shared.model.LeaderboardUser
import com.focuspath.shared.model.WorkerInfo

enum class AppState {
    SPLASH, INTRO, MAIN
}

@Composable
fun App() {
    val viewModel = remember { SharedViewModel() }
    
    // Safety: ensure default states are used if collectAsState fails (unlikely but good for stability)
    val user by viewModel.user.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val incomingTasks by viewModel.incomingTasks.collectAsState()
    val isDailyRewardClaimed by viewModel.isDailyRewardClaimed.collectAsState()
    val loginStreak by viewModel.loginStreak.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()

    var appState by remember { mutableStateOf(AppState.SPLASH) }
    var selectedTab by remember { mutableStateOf(0) }
    var activeDialog by remember { mutableStateOf<String?>(null) }

    FocusPathTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            when (appState) {
                AppState.SPLASH -> SplashScreen { appState = AppState.INTRO }
                AppState.INTRO -> IntroScreen { appState = AppState.MAIN }
                AppState.MAIN -> {
                    MainScaffold(
                        viewModel = viewModel,
                        tasks = tasks,
                        leaderboard = leaderboard,
                        user = user,
                        isPremium = isPremium,
                        incomingTasks = incomingTasks,
                        isDailyRewardClaimed = isDailyRewardClaimed,
                        loginStreak = loginStreak,
                        selectedTab = selectedTab,
                        onTabSelect = { selectedTab = it },
                        workers = workers,
                        isFocusActive = isTimerRunning,
                        activeDialog = activeDialog,
                        onDialogChange = { activeDialog = it }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FocusPath", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = TerminalGreen)
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("BAŞLA", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun IntroScreen(onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎯", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text("Hoş Geldiniz", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("DEVAM ET", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun MainScaffold(
    viewModel: SharedViewModel,
    tasks: List<Task>,
    leaderboard: List<LeaderboardUser>,
    user: User,
    isPremium: Boolean,
    incomingTasks: List<Task>,
    isDailyRewardClaimed: Boolean,
    loginStreak: Int,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    workers: List<WorkerInfo>,
    isFocusActive: Boolean,
    activeDialog: String?,
    onDialogChange: (String?) -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A1A), contentColor = TerminalGreen) {
                val items = listOf("Ana" to Icons.Default.Home, "Görev" to Icons.Default.CheckCircle, "Ofis" to Icons.Default.Business, "Su" to Icons.Default.WaterDrop)
                items.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { onTabSelect(index) },
                        icon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = TerminalGreen, unselectedIconColor = Color.Gray, indicatorColor = Color.White.copy(0.1f))
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FocusPath", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                IconButton(onClick = { onDialogChange("PROFİL") }) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(TerminalGreen.copy(0.2f)).border(1.dp, TerminalGreen, CircleShape), contentAlignment = Alignment.Center) {
                        Text(user.name.take(1).uppercase(), color = TerminalGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Sub Nav (Quick Access)
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val buttons = listOf("GÖREV" to "QUEST", "GELEN" to "INCOMING", "OYUN" to "GAMES", "BAŞARIM" to "ACHIEVE")
                items(buttons) { (label, key) ->
                    TextButton(onClick = { onDialogChange(key) }) {
                        Text(label, color = TerminalGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> HomeTab(leaderboard, user, viewModel, onDialogChange)
                    1 -> TaskTab(tasks, onToggle = { viewModel.toggleTask(it) })
                    2 -> SharedOfficeScreen(workers = workers, officeLevel = user.level, coins = user.coins, isFocusActive = isFocusActive)
                    else -> WaterTab(viewModel)
                }
            }
        }
    }

    // Handle Dialogs
    activeDialog?.let { type ->
        Dialog(onDismissRequest = { onDialogChange(null) }) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(24.dp)),
                color = SurfaceColor,
                border = BorderStroke(1.dp, TerminalGreen.copy(0.3f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(type, style = MaterialTheme.typography.titleLarge, color = TerminalGreen, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(16.dp))
                    Text("Bu özellik iOS için optimize ediliyor.", color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { onDialogChange(null) }, modifier = Modifier.fillMaxWidth()) {
                        Text("KAPAT")
                    }
                }
            }
        }
    }

    if (!isDailyRewardClaimed && activeDialog == null) {
        LaunchedEffect(Unit) { onDialogChange("GÜNLÜK ÖDÜL") }
    }
}

@Composable
fun HomeTab(leaderboard: List<LeaderboardUser>, user: User, viewModel: SharedViewModel, onDialogChange: (String?) -> Unit) {
    val currentStation by viewModel.currentRadioStation.collectAsState()
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceColor)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hoş Geldin, ${user.name}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("${user.coins} Coin • Seviye ${user.level}", style = MaterialTheme.typography.bodySmall, color = TerminalGreen)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceColor)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📻 RADYO", style = MaterialTheme.typography.labelSmall, color = AccentYellow)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Lofi", "Jazz", "Brain", "Rain").forEach { name ->
                            val isSelected = currentStation == name
                            IconButton(
                                onClick = { if(isSelected) viewModel.stopRadio() else viewModel.playRadio(name) },
                                modifier = Modifier.background(if (isSelected) TerminalGreen.copy(0.2f) else Color.White.copy(0.05f), CircleShape)
                            ) {
                                Icon(Icons.Default.MusicNote, null, tint = if (isSelected) TerminalGreen else Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("LİDERLİK", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceColor)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    leaderboard.take(3).forEach { lbUser ->
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(TerminalGreen.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                Text(lbUser.name.take(1), color = TerminalGreen, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(lbUser.name, color = Color.White, modifier = Modifier.weight(1f))
                            Text("${lbUser.score} XP", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskTab(tasks: List<Task>, onToggle: (Task) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SharedDashboardHeader("Görevler", "Bugünkü hedeflerin") }
        items(tasks) { task ->
            SharedTaskItem(title = task.title, isCompleted = task.isCompleted, onClick = { onToggle(task) })
        }
    }
}

@Composable
fun WaterTab(viewModel: SharedViewModel) {
    val waterCups by viewModel.waterCupsDrunk.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("SU TAKİBİ", style = MaterialTheme.typography.headlineMedium, color = TerminalGreen)
        Text("$waterCups / 8 Bardak", style = MaterialTheme.typography.displaySmall, color = Color.White)
        Button(onClick = { viewModel.drinkWater() }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text("İÇTİM (+5 Coin)")
        }
    }
}
