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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.focuspath.app.billing.BillingManager
import com.focuspath.app.billing.BillingProvider
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.focuspath.app.ui.screens.SplashScreen
import com.focuspath.app.ui.screens.TaskScreen
import com.focuspath.app.ui.theme.FocusPathTypography
import com.focuspath.app.ui.viewmodel.TaskViewModel
import com.focuspath.app.util.UpdateManager
import com.focuspath.shared.FocusPathTheme
import com.focuspath.shared.DarkBackground
import com.focuspath.shared.SharedTaskItem
import com.focuspath.shared.SharedDashboardHeader
import com.focuspath.shared.getPlatform
import com.focuspath.shared.model.LeaderboardUser
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
    private lateinit var updateManager: UpdateManager
    private lateinit var vm: TaskViewModel

    companion object {
        var showQuestDialogState = mutableStateOf(false)
        var showAchievementDialogState = mutableStateOf(false)
        var showFriendsDialogState = mutableStateOf(false)
        var showIncomingTasksDialogState = mutableStateOf(false)
        var showGamesDialogState = mutableStateOf(false)
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, 
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
            })
    }

    fun showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd?.show(this)
            loadInterstitialAd() 
        }
    }

    private var timerReceiver: android.content.BroadcastReceiver? = null

    private val googleSignInLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("FocusPathAuth", "onActivityResult: ${result.resultCode}")
        
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                android.util.Log.d("FocusPathAuth", "Google hesabı seçildi: ${account.email}")
                
                if (idToken != null) {
                    vm.loginGoogle(
                        context = this,
                        idToken = idToken,
                        onSuccess = { email ->
                            Toast.makeText(this, "Hoş geldin, $email", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            android.util.Log.e("FocusPathAuth", "Firebase hatası: $error")
                            Toast.makeText(this, "Firebase Bağlantı Hatası: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    android.util.Log.e("FocusPathAuth", "ID Token null döndü!")
                    Toast.makeText(this, "Google servisi kimlik doğrulayamadı (Token Error).", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                val statusCode = e.statusCode
                android.util.Log.e("FocusPathAuth", "Google Hatası Kod: $statusCode")
                
                val errorMeaning = when(statusCode) {
                    10 -> "Hata 10 (DEVELOPER_ERROR): Uygulamanın SHA-1 kodu Firebase'de kayıtlı değil. Google Play Console'dan SHA-1'i alıp Firebase'e ekleyin."
                    7 -> "İnternet bağlantısı yok."
                    12500 -> "Google Play Servisleri güncel değil veya yapılandırma hatalı."
                    else -> "Giriş Hatası (Kod: $statusCode). Lütfen internetinizi kontrol edin."
                }
                Toast.makeText(this, errorMeaning, Toast.LENGTH_LONG).show()
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            android.util.Log.d("FocusPathAuth", "Giriş kullanıcı tarafından iptal edildi.")
        } else {
            Toast.makeText(this, "Giriş başarısız oldu (Kod: ${result.resultCode})", Toast.LENGTH_SHORT).show()
        }
    }

    private var interstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        MobileAds.initialize(this) { status ->
            android.util.Log.d("FocusPathAds", "AdMob Initialization Status: Done")
            com.focuspath.app.ui.components.AdMobRewardedManager.loadAd(this)
        }
        loadInterstitialAd()

        logAppSignature()
        
        updateManager = UpdateManager(this)
        updateManager.checkForUpdates()

        vm = androidx.lifecycle.ViewModelProvider(this)[TaskViewModel::class.java]
        enableEdgeToEdge()
        
        android.util.Log.d("FocusPathKMP", "KMP Platform: ${ getPlatform().name}")
        
        val filter = android.content.IntentFilter().apply {
            addAction("com.focuspath.TIMER_UPDATE")
            addAction("com.focuspath.TIMER_FINISHED")
        }
        timerReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: android.content.Intent?) {
                android.util.Log.d("FocusPathReceiver", "Broadcast Received: ${intent?.action}")
                if (intent?.action == "com.focuspath.TIMER_UPDATE") {
                    val mins = intent.getIntExtra("minutes", 1)
                    vm.recordFocusSession(mins)
                } else if (intent?.action == "com.focuspath.TIMER_FINISHED") {
                    android.util.Log.d("FocusPathReceiver", "Timer Finished, calling recordSessionResult")
                    vm.recordSessionResult(true)
                    if (!vm.isPremium.value) {
                        showInterstitialAd()
                    }
                }
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this, 
            timerReceiver!!, 
            filter, 
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            FocusPathTheme {
                vm.billingProvider = this

            billingManager = BillingManager(this) {
                val p = getSharedPreferences("focuspath_prefs", MODE_PRIVATE)
                val wasPremium = p.getBoolean("is_premium", false)
                vm.isPremium.value = true
                if (!wasPremium) {
                    vm.addXp(500)
                }
                p.edit().putBoolean("is_premium", true).apply()
            }

            var showSplash by remember { mutableStateOf(true) }
            var showIntro by remember { mutableStateOf(false) }
            var showAuthDialog by remember { mutableStateOf(false) }

            val prefs = getSharedPreferences("focuspath_prefs", Context.MODE_PRIVATE)

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val lastQuestDate = prefs.getString("LAST_QUEST_DATE", "") ?: ""
            if (lastQuestDate != todayStr) {
                prefs.edit()
                    .putString("LAST_QUEST_DATE", todayStr)
                    .putInt("DAILY_FOCUS_CURRENT", 0)
                    .putBoolean("DAILY_FOCUS_DONE", false)
                    .putBoolean("DAILY_REWARD_CLAIMED_TODAY", false) 
                    .apply()
            }
            
            val claimedToday = prefs.getBoolean("DAILY_REWARD_CLAIMED_TODAY", false)
            val currentStreak = prefs.getInt("LOGIN_STREAK", 1)
            val showDailyRewardDialog = remember { mutableStateOf(!claimedToday) }
            val focusCurrent = prefs.getInt("DAILY_FOCUS_CURRENT", 0)
            val focusDone = prefs.getBoolean("DAILY_FOCUS_DONE", false)

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
                1 -> Color(0xFFFFB74D) 
                2 -> Color(0xFF4DD0E1) 
                3 -> Color(0xFFE57373) 
                else -> Color(0xFF81C784) 
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

                if (showAuthDialog) {
                    com.focuspath.app.ui.screens.task.AuthDialog(
                        vm = vm,
                        lang = if (vm.isLoggedIn.value) mapOf("signedInAs" to "Giriş yapıldı: ") else mapOf(),
                        onGoogleSignIn = {
                            showAuthDialog = false
                            signInWithGoogle()
                        },
                        onDismiss = { showAuthDialog = false }
                    )
                }

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

                if (showGamesDialogState.value) {
                    com.focuspath.app.ui.screens.games.GamesDialog(
                        vm = vm,
                        onDismiss = { showGamesDialogState.value = false }
                    )
                }

                if (showQuestDialogState.value) {
                    val quests by remember {
                        derivedStateOf {
                            listOf(
                                DailyQuestItem("30 Dakika Odaklan", 30, focusCurrent, 50, focusDone)
                            )
                        }
                    }

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
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1552664730-d307ca884978?q=80&w=1000&auto=format&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().alpha(0.4f),
                                    contentScale = ContentScale.Crop
                                )

                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))

                                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
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

                if (showAchievementDialogState.value) {
                    val achievements by remember {
                        derivedStateOf {
                            val lifetimeCoinsVal = vm.lifetimeCoins.value
                            val userXpVal = vm.userXp.value

                            listOf(
                                AchievementItem("İlk Adım", "Uygulamaya giriş yap ve ilk seansını başlat.", true, "🏆"),
                                AchievementItem("Para Avcısı", "Toplamda 100 altına ulaş.", lifetimeCoinsVal >= 100, "💰"),
                                AchievementItem("Deneyimli Hacker", "500 Şirket Puanına ulaş.", userXpVal >= 500, "💻"),
                                AchievementItem("Sistem Yetkilisi", "Kurucu Ortak rütbesine yüksel.", userXpVal >= 2000, "👑")
                            )
                        }
                    }

                    Dialog(onDismissRequest = { showAchievementDialogState.value = false }) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, terminalColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1578262825743-a4e402caab76?q=80&w=1000&auto=format&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize().alpha(0.35f),
                                    contentScale = ContentScale.Crop
                                )

                                Box(modifier = Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))

                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
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

                if (showFriendsDialogState.value) {
                    var searchEmail by remember { mutableStateOf("") }
                    var searchResultUser by remember { mutableStateOf<LeaderboardUser?>(null) }
                    var statusMessage by remember { mutableStateOf("") }
                    var myFriendsList by remember { mutableStateOf<List<LeaderboardUser>>(emptyList()) }
                    var incomingRequests by remember { mutableStateOf<List<LeaderboardUser>>(emptyList()) }

                    var selectedFriendForTask by remember { mutableStateOf<LeaderboardUser?>(null) }
                    var taskTitleInput by remember { mutableStateOf("") }
                    var taskRewardInput by remember { mutableStateOf("50") }

                    LaunchedEffect(Unit) {
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val uid = currentUser.uid
                            val email = currentUser.email

                            val userDocRefs = mutableListOf(db.collection("users").document(uid))
                            if (!email.isNullOrBlank()) {
                                userDocRefs.add(db.collection("users").document(email.lowercase()))
                            }

                            userDocRefs.forEach { userDocRef ->
                                userDocRef.collection("friends")
                                    .addSnapshotListener { friendsSnapshot, _ ->
                                        val list = friendsSnapshot?.documents?.mapNotNull { 
                                            it.toObject(LeaderboardUser::class.java) 
                                        } ?: emptyList()
                                        if (list.isNotEmpty()) {
                                            val current = myFriendsList.toMutableList()
                                            list.forEach { newUser ->
                                                if (current.none { it.uid == newUser.uid }) {
                                                    current.add(newUser)
                                                }
                                            }
                                            myFriendsList = current
                                        }
                                    }

                                userDocRef.collection("friend_requests")
                                    .addSnapshotListener { snapshot, _ ->
                                        val reqs = snapshot?.documents?.mapNotNull { it.toObject(LeaderboardUser::class.java) } ?: emptyList()
                                        if (reqs.isNotEmpty()) {
                                            val current = incomingRequests.toMutableList()
                                            reqs.forEach { newReq ->
                                                if (current.none { it.uid == newReq.uid }) {
                                                    current.add(newReq)
                                                }
                                            }
                                            incomingRequests = current
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
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=1000&auto=format&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().alpha(0.2f),
                                    contentScale = ContentScale.Crop
                                )

                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))

                                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("👥", fontSize = 24.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "ARKADAŞ SİSTEMİ",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = terminalColor,
                                            maxLines = 1,
                                            softWrap = false
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
                                            if (vm.isLoggedIn.value) {
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
                                                                val user = doc.toObject(LeaderboardUser::class.java)
                                                                if (user != null) {
                                                                    val photoVal = doc.get("photoUrl") ?: doc.get("photo_url") ?: doc.get("photo") ?: doc.get("image")
                                                                    val photoStr = photoVal?.toString()?.trim()
                                                                    if (!photoStr.isNullOrBlank() && photoStr.startsWith("http")) {
                                                                        user.photoUrl = photoStr
                                                                    }
                                                                    
                                                                    val emailVal = doc.getString("email") ?: cleanEmail
                                                                    val populatedUser = user.copy(
                                                                        uid = doc.id,
                                                                        email = emailVal
                                                                    )
                                                                    searchResultUser = populatedUser
                                                                    statusMessage = "Kullanıcı bulundu!"
                                                                } else {
                                                                    searchResultUser = null
                                                                    statusMessage = "Kullanıcı verisi okunamadı."
                                                                }
                                                            } else {
                                                                // Leaderboard'da yoksa users koleksiyonuna bak (Yerel moddaki kullanıcılar için)
                                                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                                    .collection("users")
                                                                    .whereEqualTo("email", cleanEmail)
                                                                    .get()
                                                                    .addOnSuccessListener { userDocs ->
                                                                        if (!userDocs.isEmpty) {
                                                                            val doc = userDocs.documents[0]
                                                                            val user = doc.toObject(LeaderboardUser::class.java)
                                                                            if (user != null) {
                                                                                // Yerel modda XP 'user_xp' olarak kaydedilmiş olabilir
                                                                                val xpVal = doc.get("score") ?: doc.get("user_xp") ?: 0L
                                                                                val xpLong = when(xpVal) {
                                                                                    is Long -> xpVal
                                                                                    is Int -> xpVal.toLong()
                                                                                    else -> 0L
                                                                                }
                                                                                
                                                                                val photoVal = doc.get("photoUrl") ?: doc.get("photo_url") ?: doc.get("photo") ?: doc.get("image")
                                                                                val photoStr = photoVal?.toString()?.trim()
                                                                                if (!photoStr.isNullOrBlank() && photoStr.startsWith("http")) {
                                                                                    user.photoUrl = photoStr
                                                                                }
                                                                                
                                                                                val emailVal = doc.getString("email") ?: cleanEmail
                                                                                val populatedUser = user.copy(
                                                                                    uid = doc.id,
                                                                                    email = emailVal,
                                                                                    score = xpLong
                                                                                )
                                                                                searchResultUser = populatedUser
                                                                                statusMessage = "Kullanıcı bulundu!"
                                                                            } else {
                                                                                searchResultUser = null
                                                                                statusMessage = "Kullanıcı verisi okunamadı."
                                                                            }
                                                                        } else {
                                                                            searchResultUser = null
                                                                            statusMessage = "Kullanıcı bulunamadı! (Kullanıcının sistemde kaydı olmayabilir)"
                                                                        }
                                                                    }
                                                                    .addOnFailureListener {
                                                                        statusMessage = "Hata: ${it.localizedMessage}"
                                                                    }
                                                            }
                                                        }
                                                        .addOnFailureListener {
                                                            statusMessage = "Hata: ${it.localizedMessage}"
                                                        }
                                                }
                                            } else {
                                                Toast.makeText(this@MainActivity, "Arkadaş aramak için lütfen giriş yapın veya misafir oturumu açın.", Toast.LENGTH_LONG).show()
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
                                                            targetUid = foundUser.uid,
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
                                                        val diff = kotlin.math.abs(System.currentTimeMillis() - friend.timestamp)
                                                        val isOnline = diff < 15 * 60 * 1000
                                                        android.util.Log.d("FocusPathPresence", "Friend ${friend.name} status check: diff=${diff/1000}s, isOnline=$isOnline")
                                                        
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
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1516533075015-a3838414c3cb?q=80&w=1000&auto=format&fit=crop",
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().alpha(0.15f),
                                    contentScale = ContentScale.Crop
                                )
                                
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))

                                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
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
}

    private fun signInWithGoogle() {
        val webClientId = "208111707825-5fr1genn4gdi6kptalfq6tv2i87pfffs.apps.googleusercontent.com"
        
        android.util.Log.d("FocusPathAuth", "Google Sign-in başlatılıyor. Client ID: $webClientId")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun logAppSignature() {
        try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            }
            
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            } ?: emptyArray()

            for (signature in signatures) {
                val md = java.security.MessageDigest.getInstance("SHA-1")
                val digest = md.digest(signature.toByteArray())
                val sha1 = digest.joinToString(":") { String.format("%02X", it) }
                android.util.Log.e("AppSignature", "Kritik Bilgi - Firebase'e eklenmesi gereken SHA-1: $sha1")
            }
        } catch (e: Exception) {
            android.util.Log.e("AppSignature", "İmza alınamadı", e)
        }
    }

    override fun onResume() {
        super.onResume()
        updateManager.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateManager.onDestroy()
        timerReceiver?.let { unregisterReceiver(it) }
        timerReceiver = null
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
