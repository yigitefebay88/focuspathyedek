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

    activeDialog?.let { type ->
        Dialog(onDismissRequest = { onDialogChange(null) }) {
            Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(RoundedCornerShape(24.dp)),
                color = SurfaceColor,
                border = BorderStroke(1.dp, TerminalGreen.copy(0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (type) {
                                "QUEST" -> "YENİ GÖREV EKLE"
                                "INCOMING" -> "GELEN GÖREVLER"
                                "GAMES" -> "MİNİ OYUNLAR"
                                "ACHIEVE" -> "BAŞARIMLAR"
                                "GÜNLÜK ÖDÜL" -> "GÜNLÜK ÖDÜL 🎉"
                                "PROFİL" -> "PROFİL & AYARLAR"
                                else -> type
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TerminalGreen,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(onClick = { onDialogChange(null) }) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    when (type) {
                        "QUEST" -> AddQuestDialogContent(viewModel) { onDialogChange(null) }
                        "INCOMING" -> IncomingTasksDialogContent(incomingTasks, viewModel)
                        "GAMES" -> MiniGamesDialogContent(viewModel)
                        "ACHIEVE" -> AchievementsDialogContent(user)
                        "GÜNLÜK ÖDÜL" -> DailyRewardDialogContent(viewModel, loginStreak) { onDialogChange(null) }
                        "PROFİL" -> ProfileDialogContent(user, viewModel)
                        else -> {
                            Text("Bu özellik iOS için aktif edildi.", color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { onDialogChange(null) }, modifier = Modifier.fillMaxWidth()) {
                                Text("TAMAM")
                            }
                        }
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
fun AddQuestDialogContent(viewModel: SharedViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Genel") }
    var selectedPriority by remember { mutableStateOf(1) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Görev Adı", color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TerminalGreen,
                unfocusedBorderColor = Color.Gray.copy(0.3f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Kategori", color = Color.Gray, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Genel", "İş", "Ders", "Sağlık").forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    onClick = { selectedCategory = cat },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) TerminalGreen.copy(0.2f) else Color.White.copy(0.05f),
                    border = BorderStroke(1.dp, if (isSelected) TerminalGreen else Color.Transparent)
                ) {
                    Text(cat, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (isSelected) TerminalGreen else Color.Gray, fontSize = 12.sp)
                }
            }
        }

        Text("Öncelik", color = Color.Gray, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Düşük", 1 to "Orta", 2 to "Yüksek").forEach { (level, name) ->
                val isSelected = selectedPriority == level
                Surface(
                    onClick = { selectedPriority = level },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) TerminalGreen.copy(0.2f) else Color.White.copy(0.05f),
                    border = BorderStroke(1.dp, if (isSelected) TerminalGreen else Color.Transparent)
                ) {
                    Text(name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (isSelected) TerminalGreen else Color.Gray, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (title.isNotBlank()) {
                    viewModel.addTask(title, selectedCategory, selectedPriority)
                    onDismiss()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("GÖREVİ OLUŞTUR", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun IncomingTasksDialogContent(incomingTasks: List<Task>, viewModel: SharedViewModel) {
    if (incomingTasks.isEmpty()) {
        Text("Şu an bekleyen arkadaş görevi yok.", color = Color.Gray, fontSize = 14.sp)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 260.dp)) {
            items(incomingTasks) { task ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("+50 XP • +50 Coin", color = TerminalGreen, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.completeIncomingTask(task) },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen)
                        ) {
                            Text("TAMAMLA", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniGamesDialogContent(viewModel: SharedViewModel) {
    var gameScore by remember { mutableStateOf(0) }
    var gameActive by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Refleks ve Odak Testi", style = MaterialTheme.typography.bodyMedium, color = Color.White)
        
        Card(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            colors = CardDefaults.cardColors(containerColor = if (gameActive) TerminalGreen.copy(0.2f) else Color.White.copy(0.05f)),
            border = BorderStroke(1.dp, if (gameActive) TerminalGreen else Color.Gray.copy(0.2f))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (gameActive) {
                    Button(
                        onClick = {
                            gameScore += 10
                            viewModel.addXpAndCoins(10, 5)
                            gameActive = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen)
                    ) {
                        Text("ŞİMDİ DOKUN! (+10 XP)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(onClick = { gameActive = true }) {
                        Text("TESTİ BAŞLAT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text("Kazanılan Puan: $gameScore XP", color = AccentYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AchievementsDialogContent(user: User) {
    val badges = listOf(
        "🎯" to "İlk Adım" to "İlk görevini oluştur",
        "⚡" to "Odak Ustası" to "100 dakika odaklan",
        "💧" to "Su Gurusu" to "10 bardak su iç",
        "🏢" to "Şirket Kurucusu" to "Ofisini 2. seviyeye çıkar"
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 260.dp)) {
        items(badges) { (item, desc) ->
            val (icon, title) = item
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(desc, color = Color.Gray, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.CheckCircle, null, tint = TerminalGreen, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun DailyRewardDialogContent(viewModel: SharedViewModel, streak: Int, onClaim: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("🔥 $streak Günlük Giriş Serisi!", style = MaterialTheme.typography.titleLarge, color = AccentYellow, fontWeight = FontWeight.Bold)
        Text("Bugünkü ödülünüzü alarak odaklanmaya devam edin.", color = Color.White, textAlign = TextAlign.Center, fontSize = 13.sp)
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = TerminalGreen.copy(0.1f)), border = BorderStroke(1.dp, TerminalGreen)) {
                Text("+50 COIN 🪙", modifier = Modifier.padding(16.dp), color = TerminalGreen, fontWeight = FontWeight.Bold)
            }
            Card(colors = CardDefaults.cardColors(containerColor = AccentYellow.copy(0.1f)), border = BorderStroke(1.dp, AccentYellow)) {
                Text("+20 XP ⚡", modifier = Modifier.padding(16.dp), color = AccentYellow, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = {
                viewModel.claimDailyReward()
                onClaim()
            },
            colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ÖDÜLÜ AL", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ProfileDialogContent(user: User, viewModel: SharedViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(TerminalGreen.copy(0.2f)).border(2.dp, TerminalGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(user.name.take(1).uppercase(), color = TerminalGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(user.name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Seviye ${user.level} • ${user.xp} XP", color = TerminalGreen, fontSize = 12.sp)
            }
        }

        HorizontalDivider(color = Color.White.copy(0.1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Toplam Coin", color = Color.White)
            Text("${user.coins} 🪙", color = AccentYellow, fontWeight = FontWeight.Bold)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Su Hatırlatıcısı", color = Color.White)
            Switch(checked = true, onCheckedChange = {})
        }
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
