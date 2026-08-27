package com.focuspath.app

import android.R
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.focuspath.app.billing.BillingManager
import com.focuspath.app.billing.BillingProvider
import com.focuspath.app.ui.screens.SplashScreen
import com.focuspath.app.ui.screens.TaskScreen
import com.focuspath.app.ui.theme.FocusPathTypography
import com.focuspath.app.ui.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- UI İÇİN VERİ MODELLERİ ---
data class DailyQuestItem(
    val title: String,
    val target: Int,
    val current: Int,
    val rewardCoins: Int,
    val isCompleted: Boolean
)

data class AchievementItem(
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val icon: String
)

@AndroidEntryPoint

class MainActivity : ComponentActivity(), BillingProvider {

    private lateinit var billingManager: BillingManager
    private lateinit var vm: TaskViewModel

    // Pencerelerin uygulamanın her yerinden tetiklenebilmesi için statik state'ler
    companion object {
        var showQuestDialogState = mutableStateOf(false)
        var showAchievementDialogState = mutableStateOf(false)
        var showFriendsDialogState = mutableStateOf(false)
        var showIncomingTasksDialogState = mutableStateOf(false)
        var showGamesDialogState = mutableStateOf(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            vm = androidx.lifecycle.viewmodel.compose.viewModel()
            vm.billingProvider = this

            billingManager = BillingManager(this) {
                vm.isPremium.value = true
                vm.addXp(500)
                getSharedPreferences("focuspath_prefs", MODE_PRIVATE).edit().putBoolean("is_premium", true).apply()
            }

            var showSplash by remember { mutableStateOf(true) }
            var showIntro by remember { mutableStateOf(false) }
            var showAuthDialog by remember { mutableStateOf(false) }

            // --- YEREL DIALOG VE GÖREV STATE'LERİ ---
            val prefs = getSharedPreferences("focuspath_prefs", Context.MODE_PRIVATE)

            // Günlük Ödül State'leri
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            // Günlük Challenge State'leri
            val lastQuestDate = prefs.getString("LAST_QUEST_DATE", "") ?: ""
            if (lastQuestDate != todayStr) {
                // Yeni bir gün başladı, değerleri sıfırla
                prefs.edit()
                    .putString("LAST_QUEST_DATE", todayStr)
                    .putInt("DAILY_FOCUS_CURRENT", 0)
                    .putBoolean("DAILY_FOCUS_DONE", false)
                    .putBoolean("DAILY_REWARD_CLAIMED_TODAY", false) // Ödül her gün sıfırlanır
                    .apply()
            }
            
            // Verileri oku
            val claimedToday = prefs.getBoolean("DAILY_REWARD_CLAIMED_TODAY", false)
            val currentStreak = prefs.getInt("LOGIN_STREAK", 1)
            val showDailyRewardDialog = remember { mutableStateOf(!claimedToday) }
            val focusCurrent = prefs.getInt("DAILY_FOCUS_CURRENT", 0)
            val focusDone = prefs.getBoolean("DAILY_FOCUS_DONE", false)

            // --- BİLDİRİM İZNİ (Android 13+) ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted -> }
                LaunchedEffect(Unit) {
                    val permission = android.Manifest.permission.POST_NOTIFICATIONS
                    if (androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        launcher.launch(permission)
                    }
                }
            }

            // Bildirim İzni ve Diğer Başlangıçlar...
            
            // DEĞERLENDİRME DİALOGU KONTROLÜ
            if (vm.showReviewDialog.value) {
                com.focuspath.app.ui.screens.games.ReviewDialog(
                    onDismiss = { vm.showReviewDialog.value = false },
                    onRate = {
                        vm.showReviewDialog.value = false
                        val uri = android.net.Uri.parse("market://details?id=${this@MainActivity.packageName}")
                        val goToMarket = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        try {
                            startActivity(goToMarket)
                        } catch (e: Exception) {
                            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=${this@MainActivity.packageName}")))
                        }
                    }
                )
            }

            val darkTheme = vm.isDarkMode.value
            val terminalColor = when(vm.themeColorIndex.value) {
                1 -> Color(0xFFFFB74D) // Muted Amber
                2 -> Color(0xFF4DD0E1) // Muted Cyan
                3 -> Color(0xFFE57373) // Muted Red
                else -> Color(0xFF81C784) // Muted Emerald Green
            }

            MaterialTheme(
                colorScheme = if (darkTheme) {
                    darkColorScheme(
                        primary = terminalColor,
                        background = Color(0xFF121212),
                        surface = Color(0xFF1E1E1E),
                        onSurface = Color(0xFFE0E0E0),
                        onSurfaceVariant = Color(0xFF9E9E9E)
                    )
                } else {
                    lightColorScheme(primary = terminalColor, background = Color(0xFFF5F5F5), surface = Color.White, onSurface = Color(0xFF333333))
                },
                typography = FocusPathTypography
            ) {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = Color.Transparent.toArgb()
                        window.navigationBarColor = Color.Transparent.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (showSplash) {
                        SplashScreen(terminalColor, onStartClick = { 
                            showSplash = false 
                            showIntro = true
                        })
                    } else if (showIntro) {
                        com.focuspath.app.ui.screens.IntroScreen(terminalColor, onFinish = { showIntro = false })
                    } else {
                        // ANA EKRAN
                        Box(modifier = Modifier.fillMaxSize()) {
                            TaskScreen(
                                vm = vm,
                                onLoginClick = {
                                    showAuthDialog = true
                                }
                            )
                        }
                    }
                }

                // Auth Dialog
                if (showAuthDialog) {
                    com.focuspath.app.ui.screens.task.AuthDialog(
                        vm = vm,
                        lang = if (vm.isLoggedIn.value) mapOf("signedInAs" to "Giriş yapıldı: ") else mapOf(),
                        onDismiss = { showAuthDialog = false }
                    )
                }

                // 1. GÜNLÜK ÖDÜL POPUP
                if (showDailyRewardDialog.value && !claimedToday && !showSplash) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text("🎁 Günlük Ödül!") },
                        text = {
                            Text("Tebrikler! $currentStreak. gün serisindesin.\n\nBugünün ödülünü toplayarak serini devam ettir, 20 altın kazan!")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    prefs.edit()
                                        .putString("LAST_LOGIN_DATE", todayStr)
                                        .putBoolean("DAILY_REWARD_CLAIMED_TODAY", true)
                                        .putInt("LOGIN_STREAK", currentStreak + 1)
                                        .apply()

                                    vm.addCoins(20)
                                    showDailyRewardDialog.value = false
                                    Toast.makeText(this@MainActivity, "Ödül alındı! (+20 Altın)", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = terminalColor)
                            ) {
                                Text("Ödülü Al", color = Color.Black)
                            }
                        }
                    )
                }

                // 3. OYUNLAR DİALOG
                if (showGamesDialogState.value) {
                    com.focuspath.app.ui.screens.games.GamesDialog(
                        vm = vm,
                        onDismiss = { showGamesDialogState.value = false }
                    )
                }

                // 2. GÜNLÜK CHALLENGE DIALOG
                if (showQuestDialogState.value) {
                    val quests = listOf(
                        DailyQuestItem("30 Dakika Odaklan", 30, focusCurrent, 50, focusDone)
                    )

                    Dialog(onDismissRequest = { showQuestDialogState.value = false }) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(450.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, terminalColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // ARKA PLAN GÖRSELİ (Hedef/Başarı Temalı)
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1552664730-d307ca884978?q=80&w=1000&auto=format&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().alpha(0.4f),
                                    contentScale = ContentScale.Crop
                                )

                                // Karartma Gradyanı
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))

                                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                    // HEADER
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🎯", fontSize = 24.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "GÜNLÜK CHALLENGE",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = terminalColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        items(quests) { quest ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text(quest.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    LinearProgressIndicator(
                                                        progress = { (quest.current.toFloat() / quest.target).coerceIn(0f, 1f) },
                                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                                        color = terminalColor,
                                                        trackColor = Color.White.copy(alpha = 0.1f)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("${quest.current}/${quest.target} dk", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                                                        Surface(
                                                            color = if (quest.isCompleted) Color.Green.copy(0.1f) else terminalColor.copy(0.1f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text(
                                                                if (quest.isCompleted) "✅ TAMAMLANDI" else "💰 +${quest.rewardCoins} COIN",
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = if (quest.isCompleted) Color.Green else terminalColor,
                                                                fontWeight = FontWeight.Black
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { showQuestDialogState.value = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Kapat", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. ACHIEVEMENT (BAŞARIMLAR) DIALOG
                if (showAchievementDialogState.value) {
                    val userCoinsVal = vm.userCoins.value
                    val userXpVal = vm.userXp.value

                    val achievements = listOf(
                        AchievementItem("İlk Adım", "Uygulamaya giriş yap ve ilk seansını başlat.", true, "🏆"),
                        AchievementItem("Para Avcısı", "Toplamda 100 altına ulaş.", userCoinsVal >= 100, "💰"),
                        AchievementItem("Deneyimli Hacker", "500 Şirket Puanına ulaş.", userXpVal >= 500, "💻"),
                        AchievementItem("Sistem Yetkilisi", "Kurucu Ortak rütbesine yüksel.", userXpVal >= 2000, "👑")
                    )

                    Dialog(onDismissRequest = { showAchievementDialogState.value = false }) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, terminalColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // ARKA PLAN GÖRSELİ (Kupa/Başarı Temalı)
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1578262825743-a4e402caab76?q=80&w=1000&auto=format&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize().alpha(0.35f),
                                    contentScale = ContentScale.Crop
                                )

                                // Karartma Gradyanı
                                Box(modifier = Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))

                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                                    // HEADER
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🏅", fontSize = 24.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "BAŞARIMLAR",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = terminalColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        achievements.forEach { item ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (item.isUnlocked) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.2f)
                                                ),
                                                border = BorderStroke(0.5.dp, if (item.isUnlocked) terminalColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        color = if (item.isUnlocked) terminalColor.copy(0.1f) else Color.Gray.copy(0.1f),
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(item.icon, fontSize = 24.sp, modifier = Modifier.alpha(if(item.isUnlocked) 1f else 0.4f))
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Column {
                                                        Text(
                                                            item.title,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (item.isUnlocked) Color.White else Color.Gray
                                                        )
                                                        Text(
                                                            item.description,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (item.isUnlocked) Color.LightGray else Color.Gray.copy(alpha = 0.7f)
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            if (item.isUnlocked) "KİLİT AÇILDI ✅" else "KİLİTLİ 🔒",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Black,
                                                            color = if (item.isUnlocked) Color.Green else Color.Red.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { showAchievementDialogState.value = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Kapat", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. ARKADAŞ SİSTEMİ DİALOGU (İstekler, Arkadaşlar ve Karşılıklı Görev Gönderme)
                if (showFriendsDialogState.value) {
                    var searchEmail by remember { mutableStateOf("") }
                    var searchResultUser by remember { mutableStateOf<com.focuspath.app.data.model.LeaderboardUser?>(null) }
                    var statusMessage by remember { mutableStateOf("") }
                    var myFriendsList by remember { mutableStateOf<List<com.focuspath.app.data.model.LeaderboardUser>>(emptyList()) }
                    var incomingRequests by remember { mutableStateOf<List<com.focuspath.app.data.model.LeaderboardUser>>(emptyList()) }

                    var selectedFriendForTask by remember { mutableStateOf<com.focuspath.app.data.model.LeaderboardUser?>(null) }
                    var taskTitleInput by remember { mutableStateOf("") }
                    var taskRewardInput by remember { mutableStateOf("50") }

                    LaunchedEffect(Unit) {
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val uid = currentUser.uid
                            val email = currentUser.email

                            // Hem UID hem de Email bazlı dokümanları dinleyelim (uyuşmazlık olmasın diye)
                            val userDocRefs = mutableListOf(db.collection("users").document(uid))
                            if (!email.isNullOrBlank()) {
                                userDocRefs.add(db.collection("users").document(email))
                            }

                            userDocRefs.forEach { userDocRef ->
                                userDocRef.collection("friends")
                                    .addSnapshotListener { friendsSnapshot, _ ->
                                        val friendUids = friendsSnapshot?.documents?.mapNotNull { it.id } ?: emptyList()
                                        
                                        if (friendUids.isNotEmpty()) {
                                            db.collection("leaderboard")
                                                .whereIn("uid", friendUids)
                                                .addSnapshotListener { lbSnapshot, _ ->
                                                    val list = lbSnapshot?.documents?.mapNotNull { 
                                                        it.toObject(com.focuspath.app.data.model.LeaderboardUser::class.java) 
                                                    } ?: emptyList()
                                                    if (list.isNotEmpty()) {
                                                        myFriendsList = list
                                                    }
                                                }
                                        }
                                    }

                                userDocRef.collection("friend_requests")
                                    .addSnapshotListener { snapshot, _ ->
                                        val reqs = snapshot?.documents?.mapNotNull { it.toObject(com.focuspath.app.data.model.LeaderboardUser::class.java) } ?: emptyList()
                                        if (reqs.isNotEmpty()) {
                                            incomingRequests = reqs
                                        }
                                    }
                            }
                        }
                    }

                    Dialog(onDismissRequest = { showFriendsDialogState.value = false }) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(550.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, terminalColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // ARKA PLAN GÖRSELİ (Sosyal/Arkadaş Temalı)
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=1000&auto=format&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().alpha(0.2f),
                                    contentScale = ContentScale.Crop
                                )

                                // Karartma Gradyanı
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))

                                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                    // HEADER
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("👥", fontSize = 24.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "ARKADAŞ SİSTEMİ",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = terminalColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = searchEmail,
                                        onValueChange = { searchEmail = it },
                                        label = { Text("E-posta ile Ara", color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = terminalColor,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.LightGray
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            if (searchEmail.isNotBlank()) {
                                                val cleanEmail = searchEmail.trim().lowercase()
                                                statusMessage = "Aranıyor..."
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                    .collection("leaderboard")
                                                    .whereEqualTo("email", cleanEmail)
                                                    .get()
                                                    .addOnSuccessListener { documents ->
                                                        if (!documents.isEmpty) {
                                                            val doc = documents.documents[0]
                                                            val user = doc.toObject(com.focuspath.app.data.model.LeaderboardUser::class.java)
                                                            searchResultUser = user
                                                            statusMessage = "Kullanıcı bulundu!"
                                                        } else {
                                                            searchResultUser = null
                                                            statusMessage = "Kullanıcı bulunamadı! (Kullanıcının sistemde kaydı olmayabilir)"
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        statusMessage = "Hata: ${it.localizedMessage}"
                                                    }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = terminalColor),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Kullanıcı Ara", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    if (statusMessage.isNotEmpty()) {
                                        Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = terminalColor, modifier = Modifier.padding(top = 4.dp))
                                    }

                                    searchResultUser?.let { foundUser ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                                            border = BorderStroke(0.5.dp, terminalColor.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(foundUser.name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                                    Text("XP: ${foundUser.score}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                                }
                                                Button(
                                                    onClick = {
                                                        vm.sendFriendRequest(
                                                            targetEmail = foundUser.email,
                                                            targetName = foundUser.name,
                                                            onSuccess = {
                                                                Toast.makeText(this@MainActivity, "İstek gönderildi!", Toast.LENGTH_SHORT).show()
                                                                searchResultUser = null
                                                                searchEmail = ""
                                                            },
                                                            onError = { msg ->
                                                                Toast.makeText(this@MainActivity, "Hata: $msg", Toast.LENGTH_LONG).show()
                                                            }
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = terminalColor),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Ekle", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (incomingRequests.isNotEmpty()) {
                                        Text("📩 GELEN İSTEKLER", style = MaterialTheme.typography.labelSmall, color = terminalColor, fontWeight = FontWeight.Bold)
                                        LazyColumn(modifier = Modifier.height(100.dp)) {
                                            items(incomingRequests) { reqUser ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(reqUser.name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                                        Button(
                                                            onClick = {
                                                                vm.acceptFriendRequest(
                                                                    reqUser = reqUser,
                                                                    onSuccess = {
                                                                        Toast.makeText(this@MainActivity, "Artık arkadaşsınız!", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                )
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text("Kabul", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Text("👥 ARKADAŞLARIM", style = MaterialTheme.typography.labelSmall, color = Color.LightGray, fontWeight = FontWeight.Bold)
                                    
                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        items(myFriendsList) { friend ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        val isOnline = (System.currentTimeMillis() - friend.timestamp) < 5 * 60 * 1000
                                                        Text(friend.name, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isOnline) Color.Green else Color.Gray))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(if (isOnline) "Online" else "Offline", style = MaterialTheme.typography.labelSmall, color = if (isOnline) Color.Green.copy(0.7f) else Color.Gray)
                                                        }
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Button(
                                                            onClick = { selectedFriendForTask = friend },
                                                            colors = ButtonDefaults.buttonColors(containerColor = terminalColor),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("🎯 Görev", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                                                        }
                                                        
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        
                                                        IconButton(
                                                            onClick = {
                                                                vm.removeFriend(friend.uid, friend.email) {
                                                                    Toast.makeText(this@MainActivity, "Arkadaş silindi.", Toast.LENGTH_SHORT).show()
                                                                }
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Sil",
                                                                tint = Color.Red.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { showFriendsDialogState.value = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Kapat", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    selectedFriendForTask?.let { targetFriend ->
                        AlertDialog(
                            onDismissRequest = { selectedFriendForTask = null },
                            title = { Text("🎯 ${targetFriend.name} adlı kullanıcıya görev yaz") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = taskTitleInput,
                                        onValueChange = { taskTitleInput = it },
                                        label = { Text("Görev Açıklaması (Örn: 30 dk kod yaz)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = taskRewardInput,
                                        onValueChange = { taskRewardInput = it },
                                        label = { Text("Ödül (Coin)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (taskTitleInput.isNotBlank()) {
                                            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                            val taskData = mapOf(
                                                "title" to taskTitleInput,
                                                "reward" to (taskRewardInput.toIntOrNull() ?: 50),
                                                "senderName" to (currentUser?.displayName ?: "Bir Arkadaş"),
                                                "timestamp" to System.currentTimeMillis()
                                            )

                                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("users")
                                                .document(targetFriend.email)
                                                .collection("incoming_tasks")
                                                .add(taskData)
                                                .addOnSuccessListener {
                                                    Toast.makeText(this@MainActivity, "Görev başarıyla gönderildi!", Toast.LENGTH_SHORT).show()
                                                    selectedFriendForTask = null
                                                    taskTitleInput = ""
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(this@MainActivity, "Görev gönderilemedi.", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = terminalColor)
                                ) {
                                    Text("Gönder", color = Color.Black)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { selectedFriendForTask = null }) {
                                    Text("İptal", color = Color.Gray)
                                }
                            }
                        )
                    }
                }

                // 5. ARKADAŞLARDAN GELEN GÖREVLER DİALOGU
                if (showIncomingTasksDialogState.value) {
                    var incomingTasksList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
                    var taskDocumentIds by remember { mutableStateOf<List<String>>(emptyList()) }

                    LaunchedEffect(Unit) {
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null && currentUser.email != null) {
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(currentUser.email!!)
                                .collection("incoming_tasks")
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val list = mutableListOf<Map<String, Any>>()
                                    val ids = mutableListOf<String>()
                                    for (doc in snapshot.documents) {
                                        doc.data?.let {
                                            list.add(it)
                                            ids.add(doc.id)
                                        }
                                    }
                                    incomingTasksList = list
                                    taskDocumentIds = ids
                                }
                        }
                    }

                    Dialog(onDismissRequest = { showIncomingTasksDialogState.value = false }) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, terminalColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // ARKA PLAN GÖRSELİ
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1516533075015-a3838414c3cb?q=80&w=1000&auto=format&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().alpha(0.15f),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Karartma Gradyanı
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))

                                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                    // HEADER
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Inbox, null, tint = terminalColor)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "GELEN GÖREVLER", 
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = terminalColor
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (incomingTasksList.isEmpty()) {
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("📭", fontSize = 48.sp)
                                                Spacer(Modifier.height(8.dp))
                                                Text("Şu an bekleyen görev yok.", color = Color.Gray)
                                            }
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            items(incomingTasksList.indices.toList()) { index ->
                                                val task = incomingTasksList[index]
                                                val docId = taskDocumentIds[index]
                                                val title = task["title"] as? String ?: "Görev"
                                                val reward = (task["reward"] as? Long)?.toInt() ?: 50
                                                val sender = task["senderName"] as? String ?: "Bir Arkadaş"

                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text("GÖNDEREN: $sender", style = MaterialTheme.typography.labelSmall, color = terminalColor, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text("💰", fontSize = 12.sp)
                                                                Spacer(Modifier.width(4.dp))
                                                                Text("+$reward Coin", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                                            }
                                                            Button(
                                                                onClick = {
                                                                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                                                    if (currentUser != null && currentUser.email != null) {
                                                                        vm.addTask(
                                                                            title = "Arkadaştan: $title",
                                                                            notes = "Gönderen: $sender",
                                                                            category = "Arkadaş",
                                                                            priority = 2,
                                                                            rewardCoins = reward
                                                                        )

                                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                                            .collection("users")
                                                                            .document(currentUser.email!!)
                                                                            .collection("incoming_tasks")
                                                                            .document(docId)
                                                                            .delete()
                                                                            .addOnSuccessListener {
                                                                                Toast.makeText(this@MainActivity, "Görev kabul edildi!", Toast.LENGTH_SHORT).show()
                                                                                incomingTasksList = incomingTasksList.filterIndexed { i, _ -> i != index }
                                                                                taskDocumentIds = taskDocumentIds.filterIndexed { i, _ -> i != index }
                                                                            }
                                                                    }
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = terminalColor),
                                                                shape = RoundedCornerShape(12.dp)
                                                            ) {
                                                                Text("Kabul Et", color = Color.Black, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Button(
                                        onClick = { showIncomingTasksDialogState.value = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Kapat", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun startPurchaseFlow() {
        billingManager.startPurchase()
    }
}
@Preview(showBackground = true, showSystemUi = true, name = "FocusPath Theme")
@Composable
private fun FocusPathThemePreview() {
    val terminalColor = Color(0xFF81C784)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = terminalColor,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onSurface = Color(0xFFE0E0E0),
            onSurfaceVariant = Color(0xFF9E9E9E)
        ),
        typography = FocusPathTypography
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "FocusPath",
                    style = MaterialTheme.typography.headlineMedium,
                    color = terminalColor
                )
                Text(
                    "Preview — ViewModel / Firebase yok",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = terminalColor)
                ) {
                    Text("Örnek Buton", color = Color.Black)
                }
                LinearProgressIndicator(
                    progress = { 0.45f },
                    modifier = Modifier.fillMaxWidth(),
                    color = terminalColor
                )
            }
        }
    }


    }
