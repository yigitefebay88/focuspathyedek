package com.focuspath.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuspath.app.R
import com.focuspath.app.data.local.TaskDao
import com.focuspath.app.data.local.TaskEntity
import com.focuspath.app.data.local.FocusHistoryEntity
import com.focuspath.app.data.model.LeaderboardUser
import com.focuspath.app.data.model.Team
import com.focuspath.app.data.remote.FocusPathApiService
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.Query as FirestoreQuery
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import java.text.SimpleDateFormat
import javax.inject.Inject
import com.focuspath.app.billing.BillingProvider
import com.focuspath.app.service.FocusService
import com.google.ai.client.generativeai.type.FunctionCallPart
import kotlinx.coroutines.delay

enum class WorkerAction { 
    IDLE, WORKING, TYPING, MOUSE, THINKING, RESTING, WALKING, COFFEE, MEETING, COMPLETED, ASKING 
}

data class WorkerInfo(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val position: String = "Developer",
    val deskId: String,
    var currentAction: WorkerAction = WorkerAction.IDLE,
    var currentPos: Offset = Offset.Zero,
    var targetPos: Offset = Offset.Zero,
    var isFocusing: Boolean = false,
    var lastActionTime: Long = System.currentTimeMillis(),
    var totalWorkTime: Long = 0L,
    var productivity: Int = (80..100).random(),
    var monitorContent: String = "Terminal",
    var xpContribution: Int = 0,
    var photoUrl: String? = null,
    var isFriend: Boolean = false,
    var isFacingRight: Boolean = true,
    var movementCount: Int = 0,
    var roomId: Int = 0,
    var interactionText: String? = null,
    var email: String? = null,
    val isLiveUser: Boolean = false,
    val lastUpdate: Long = System.currentTimeMillis(),
    var latestEmoji: String? = null,
    var emojiTime: Long = 0L,
    val isMe: Boolean = false
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val application: Application,
    private val taskDao: TaskDao,
    private val generativeModel: GenerativeModel,
    val prefs: android.content.SharedPreferences,
    private val apiService: FocusPathApiService,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {

    var billingProvider: BillingProvider? = null

    val isLoggedIn = mutableStateOf(false)
    val userEmail = mutableStateOf("")
    val userName = mutableStateOf(prefs.getString("user_name", "ANONYMOUS") ?: "ANONYMOUS")
    val userPhotoUrl = mutableStateOf<String?>(prefs.getString("user_photo_url", null))
    val userStreak = mutableStateOf(5)
    val userCoins = mutableStateOf(prefs.getInt("user_coins", 0))
    val lifetimeCoins = mutableStateOf(prefs.getInt("lifetime_coins", 0))
    val officeLevel = mutableStateOf(prefs.getInt("office_level", 1))
    val blockedApps = mutableStateListOf<String>().apply {
        addAll(prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet())
    }
    val isFocusActive = mutableStateOf(prefs.getBoolean("is_focus_active", false))
    
    // TIMER STATE (Moved from TaskScreen to ViewModel for performance and persistence)
    val timerRunning = mutableStateOf(false)
    val timeLeft = mutableLongStateOf(25 * 60 * 1000L)
    val timeElapsed = mutableLongStateOf(0L)
    val pomodoroTotalMillis = mutableLongStateOf(25 * 60 * 1000L)
    val isPomodoroMode = mutableStateOf(true)
    private var timerSyncJob: kotlinx.coroutines.Job? = null

    val brainDumpNotes = mutableStateListOf<String>()
    val selectedFocusBuddy = mutableStateOf<WorkerInfo?>(null)
    val buddyCurrentTask = mutableStateOf<String?>(null)

    // ADHD FEATURES STATE
    val isMinimalistMode = mutableStateOf(prefs.getBoolean("adhd_minimalist_mode", false))
    val isIntervalChimeEnabled = mutableStateOf(prefs.getBoolean("adhd_interval_chime", false))
    val intervalMinutes = mutableIntStateOf(prefs.getInt("adhd_interval_minutes", 15))
    val lastChimeTime = mutableLongStateOf(0L)
    
    val selectedTaskEnergy = mutableIntStateOf(1) // 0: Düşük, 1: Normal, 2: Yüksek
    val isDecisionSpinnerActive = mutableStateOf(false)
    val spinnerResult = mutableStateOf<TaskEntity?>(null)
    val spinnerAction = mutableStateOf<String?>(null)

    val isSlicingTask = mutableStateOf(false)
    val slicedTasks = mutableStateListOf<String>()

    val isMonotasking = mutableStateOf(false)
    val monotask = mutableStateOf<TaskEntity?>(null)
    val showConfetti = mutableStateOf(false)

    // ADHD NEW STATE
    val distractionLog = mutableStateListOf<String>()
    val distractionAnalysis = mutableStateOf<String?>(null)
    val isAnalyzingDistractions = mutableStateOf(false)
    val isUploadingProfile = mutableStateOf(false)
    val dopamineMultiplier = mutableFloatStateOf(1.0f)
    val currentTaskEstimation = mutableIntStateOf(0)

    private var liveFocusUpdateJob: kotlinx.coroutines.Job? = null

    val isLeaderboardLoading = mutableStateOf(false)
    
    private val _teamLeaderboard = MutableStateFlow<List<Team>>(emptyList())
    val teamLeaderboard: StateFlow<List<Team>> = _teamLeaderboard.asStateFlow()
    
    // TEAM SYSTEM STATE
    val userTeam = mutableStateOf<Team?>(null)
    val teamMembers = mutableStateListOf<LeaderboardUser>()
    val isTeamLoading = mutableStateOf(false)
    val isTeamOfisMode = mutableStateOf(prefs.getBoolean("is_team_office_mode", false))
    private var teamListener: com.google.firebase.firestore.ListenerRegistration? = null

    val dailyBriefingText = mutableStateOf<String?>(null)
    val isBriefingLoading = mutableStateOf(false)
    val showDailyBriefing = mutableStateOf(false)

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val todayStr = sdf.format(Date())

    fun checkAndGenerateBriefing(isEnglish: Boolean) {
        val lastDate = prefs.getString("last_briefing_date", "")
        if (lastDate == todayStr) return // Bugün zaten gösterildi

        viewModelScope.launch {
            showDailyBriefing.value = true
            isBriefingLoading.value = true
            try {
                val tasks = taskDao.getAllTasksOnce().filter { isSameDay(it.dueDate, System.currentTimeMillis()) && !it.isCompleted }
                if (tasks.isEmpty()) {
                    dailyBriefingText.value = if (isEnglish) "You have no tasks for today. A perfect day to plan something new!" else "Bugün için planlanmış görevin yok. Yeni bir şeyler planlamak için harika bir gün!"
                } else {
                    val taskListStr = tasks.joinToString { it.title }
                    val prompt = if (isEnglish) {
                        "Good morning! Here are today's tasks: $taskListStr. Give a brief, motivating 2-sentence summary/advice for the day."
                    } else {
                        "Günaydın! Bugünün görevleri: $taskListStr. Bu liste için kısa, motive edici 2 cümlelik bir sabah brifingi/tavsiyesi ver."
                    }
                    val response = withContext(Dispatchers.IO) { generativeModel.generateContent(prompt) }
                    dailyBriefingText.value = response.text ?: (if(isEnglish) "Ready to start?" else "Başlamaya hazır mısın?")
                }
                prefs.edit().putString("last_briefing_date", todayStr).apply()
            } catch (e: Exception) {
                dailyBriefingText.value = if(isEnglish) "System online. Let's make today productive!" else "Sistem çevrimiçi. Bugün verimli bir gün olsun!"
            } finally {
                isBriefingLoading.value = false
            }
        }
    }

    fun getWeeklyHistory(): Flow<List<FocusHistoryEntity>> {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        return taskDao.getFocusHistory(sdf.format(cal.time))
    }

    private fun updateTodayHistory(focusMins: Int = 0, tasksDone: Int = 0, sessionsComp: Int = 0, sessionsInt: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = taskDao.getFocusHistoryByDate(todayStr) ?: FocusHistoryEntity(todayStr)
            taskDao.insertFocusHistory(current.copy(
                totalFocusMinutes = current.totalFocusMinutes + focusMins,
                tasksCompleted = current.tasksCompleted + tasksDone,
                sessionsCompleted = current.sessionsCompleted + sessionsComp,
                sessionsInterrupted = current.sessionsInterrupted + sessionsInt
            ))
        }
    }

    fun recordSessionResult(completed: Boolean) {
        if (completed) {
            updateTodayHistory(sessionsComp = 1)
        } else {
            updateTodayHistory(sessionsInt = 1)
        }
    }

    fun addBrainDumpNote(note: String) {
        if (note.isNotBlank()) {
            brainDumpNotes.add(note)
            addTask(title = "[ZİHİN] $note", notes = "Odaklanma sırasında akla geldi.", category = "Zihin", priority = 0)
        }
    }

    // NEW ADHD METHODS
    fun logDistraction(reason: String) {
        distractionLog.add(reason)
        // Dikkati toplamak için çarpanı düşür
        dopamineMultiplier.floatValue = (dopamineMultiplier.floatValue - 0.1f).coerceAtLeast(0.5f)
    }

    fun applyDopamineBoost() {
        dopamineMultiplier.floatValue = (dopamineMultiplier.floatValue + 0.2f).coerceAtMost(3.0f)
    }

    fun updateActualTaskTime(taskId: Long, minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getAllTasksOnce().find { it.id == taskId }
            if (task != null) {
                taskDao.insertTask(task.copy(actualMinutes = task.actualMinutes + minutes))
            }
        }
    }


    // ADHD METHODS
    fun sliceTaskWithAi(taskTitle: String, isEnglish: Boolean) {
        val apiKey = com.focuspath.app.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            slicedTasks.clear()
            slicedTasks.add(if(isEnglish) "AI features require a GEMINI_API_KEY in local.properties" else "AI özellikleri için local.properties içinde GEMINI_API_KEY gereklidir")
            return
        }

        viewModelScope.launch {
            isSlicingTask.value = true
            slicedTasks.clear()
            try {
                val prompt = if (isEnglish) {
                    "Break down the following task into 5 clear, actionable sub-tasks. Output only the sub-tasks, one per line. Task: $taskTitle"
                } else {
                    "$taskTitle görevini 5 net ve uygulanabilir alt göreve böl. Sadece alt görevleri her satıra bir tane gelecek şekilde yaz."
                }
                val response = withContext(Dispatchers.IO) { generativeModel.generateContent(prompt) }
                android.util.Log.d("FocusPathAI", "Response received: ${response.text}")
                
                val text = response.text
                if (text.isNullOrBlank()) {
                    throw Exception(if(isEnglish) "AI returned an empty response" else "AI boş cevap döndürdü")
                }

                text.split("\n").forEach { line ->
                    if (line.isNotBlank()) {
                        val cleaned = line.replace(Regex("^[0-9.\\-* ]+"), "").trim()
                        if (cleaned.isNotEmpty()) slicedTasks.add(cleaned)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusPathAI", "Slice task error: ${e.message}", e)
                val userError = when {
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true || 
                    e.cause?.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    e.toString().contains("UnknownHostException", ignoreCase = true) -> 
                        if(isEnglish) "Network error. Please check your internet connection." else "İnternet bağlantısı yok. Lütfen bağlantınızı kontrol edin."
                    
                    e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true -> 
                        if(isEnglish) "Invalid API Key. Please check your local.properties." else "Geçersiz API Anahtarı. Lütfen local.properties dosyasını kontrol edin."
                    
                    e.message?.contains("unexpected", ignoreCase = true) == true -> {
                        val detail = e.cause?.message ?: e.message
                        if(isEnglish) "AI Service Error: $detail" else "AI Servis Hatası: $detail"
                    }
                    e.message?.contains("404") == true -> if(isEnglish) "Model Not Found" else "Model Bulunamadı"
                    else -> if(isEnglish) "Error: ${e.localizedMessage}" else "Hata: ${e.localizedMessage}"
                }
                slicedTasks.add(userError)
            } finally {
                isSlicingTask.value = false
            }
        }
    }

    fun runDecisionSpinner(tasks: List<TaskEntity>, isEnglish: Boolean, finalActionIndex: Int) {
        if (tasks.isEmpty()) return
        
        val actionPhrases = if (isEnglish) {
            listOf("DO IT NOW!", "POSTPONE", "5 MIN BREAK", "FOCUS!", "DEEP WORK", "QUICK WIN", "SKIP IT", "JUST START")
        } else {
            listOf("ŞİMDİ YAP!", "ERTELE", "5 DK MOLA", "ODAKLAN!", "DERİN ÇALIŞ", "HIZLI BİTİR", "PAS GEÇ", "SADECE BAŞLA")
        }
        
        // Önce sonuçları belirle (Animasyon hesaplaması için gerekli)
        spinnerAction.value = actionPhrases.getOrNull(finalActionIndex % actionPhrases.size)
        spinnerResult.value = tasks.random()

        viewModelScope.launch {
            isDecisionSpinnerActive.value = true
            // Animasyon süresine uygun bekleme (3 saniye + biraz pay)
            delay(3200)
            isDecisionSpinnerActive.value = false
        }
    }

    fun setIntervalChime(enabled: Boolean, mins: Int) {
        isIntervalChimeEnabled.value = enabled
        intervalMinutes.value = mins
        prefs.edit().apply {
            putBoolean("adhd_interval_chime", enabled)
            putInt("adhd_interval_minutes", mins)
        }.apply()
    }

    fun checkIntervalChime() {
        if (!isIntervalChimeEnabled.value || !isFocusActive.value) return
        val now = System.currentTimeMillis()
        if (lastChimeTime.longValue == 0L) {
            lastChimeTime.longValue = now
            return
        }
        if (now - lastChimeTime.longValue >= intervalMinutes.intValue * 60 * 1000L) {
            playTickSound()
            lastChimeTime.longValue = now
        }
    }

    fun setTaskEnergy(energy: Int) {
        selectedTaskEnergy.intValue = energy
    }

    fun analyzeDistractionsWithAi(isEnglish: Boolean) {
        if (distractionLog.isEmpty()) {
            distractionAnalysis.value = if (isEnglish) "No distractions logged yet. Stay focused!" else "Henüz dikkat dağıtıcı loglanmadı. Odaklanmaya devam!"
            return
        }

        viewModelScope.launch {
            isAnalyzingDistractions.value = true
            try {
                val logStr = distractionLog.joinToString(", ")
                val prompt = if (isEnglish) {
                    "Here is a log of my distractions during focus time: $logStr. Analyze these patterns and give me 3 actionable, empathetic tips to stay focused, specifically for someone with ADHD traits. Keep it brief."
                } else {
                    "Odaklanma sırasında dikkatimi dağıtan şeyler şunlar: $logStr. Bu kalıpları analiz et ve odaklanmamı sağlayacak, ADHD özellikleri gösteren biri için uygun, empatik ve uygulanabilir 3 kısa ipucu ver."
                }
                val response = withContext(Dispatchers.IO) { generativeModel.generateContent(prompt) }
                distractionAnalysis.value = response.text
            } catch (e: Exception) {
                distractionAnalysis.value = if (isEnglish) "AI could not analyze logs right now." else "AI şu an analiz yapamıyor."
            } finally {
                isAnalyzingDistractions.value = false
            }
        }
    }

    fun generateWeeklyFocusReport(isEnglish: Boolean) {
        viewModelScope.launch {
            isAnalyzingDistractions.value = true
            try {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                val history = taskDao.getFocusHistoryOnce(sdf.format(cal.time))
                
                if (history.isEmpty()) {
                    distractionAnalysis.value = if (isEnglish) "No history found for the last 7 days." else "Son 7 güne ait veri bulunamadı."
                    return@launch
                }
                
                val statsSummary = history.joinToString("\n") { 
                    "Date: ${it.date}, Mins: ${it.totalFocusMinutes}, Tasks: ${it.tasksCompleted}" 
                }
                
                val prompt = if (isEnglish) {
                    "Analyze my weekly productivity: \n$statsSummary\n Give me a brief, one-paragraph summary of my progress and 2 tips to improve next week. Be encouraging."
                } else {
                    "Haftalık verimliliğimi analiz et: \n$statsSummary\n İlerlemem hakkında tek paragraflık kısa bir özet ve haftaya iyileştirmek için 2 ipucu ver. Teşvik edici ol."
                }
                
                val response = withContext(Dispatchers.IO) { generativeModel.generateContent(prompt) }
                distractionAnalysis.value = response.text
            } catch (e: Exception) {
                distractionAnalysis.value = if (isEnglish) "Report generation failed." else "Rapor oluşturulamadı."
            } finally {
                isAnalyzingDistractions.value = false
            }
        }
    }

    fun getSmartBreakSuggestion(isEnglish: Boolean): String {
        val energy = selectedTaskEnergy.intValue
        return when {
            energy <= 2 -> if (isEnglish) "Low energy. Take a 15-min walk or a quick nap." else "Enerjin düşük. 15 dakikalık bir yürüyüş yap veya kısa bir şekerleme yap."
            energy <= 4 -> if (isEnglish) "Moderate energy. 5-min stretching or deep breathing." else "Orta seviye enerji. 5 dakika esneme veya derin nefes egzersizi yap."
            else -> if (isEnglish) "High energy! Maybe a quick water break then continue." else "Enerjin yüksek! Hızlı bir su molası ver ve devam et."
        }
    }
    val hasUpdate = mutableStateOf(false)
    val updateNotes = mutableStateOf("")
    val updateVersionName = mutableStateOf("")

    private fun checkRemoteUpdate() {
        // Fetch from Firestore config to make it dynamic
        firestore.collection("app_config").document("update_info").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val remoteVersionCode = doc.getLong("version_code")?.toInt() ?: 0
                    val remoteNotes = doc.getString("notes") ?: ""
                    val remoteVersionName = doc.getString("version_name") ?: "Yeni Sürüm"
                    val currentVersionCode = com.focuspath.app.BuildConfig.VERSION_CODE
                    
                    if (remoteVersionCode > currentVersionCode) {
                        val lastSeenVersion = prefs.getInt("last_seen_update_version", 0)
                        if (remoteVersionCode > lastSeenVersion) {
                            hasUpdate.value = true
                            updateNotes.value = remoteNotes
                            updateVersionName.value = remoteVersionName
                        }
                    }
                }
            }.addOnFailureListener {
                updateNotes.value = "• Sistem iyileştirmeleri."
            }
    }

    fun markUpdateAsSeen() {
        hasUpdate.value = false
        firestore.collection("app_config").document("update_info").get().addOnSuccessListener { doc ->
            val remoteVersionCode = doc.getLong("version_code")?.toInt() ?: 0
            if (remoteVersionCode > 0) {
                prefs.edit().putInt("last_seen_update_version", remoteVersionCode).apply()
            }
        }
    }

    fun resetUpdateNotification() {
        prefs.edit().putInt("last_seen_update_version", 0).apply()
        checkRemoteUpdate()
    }

    val unlockedItems = mutableStateListOf<String>().apply {
        addAll(prefs.getStringSet("unlocked_items", setOf("desk_1", "pc_1")) ?: setOf("desk_1", "pc_1"))
    }

    // GÖRÜNÜR EŞYALAR LİSTESİ (Sahnede olanlar)
    val visibleItems = mutableStateListOf<String>().apply {
        val saved = prefs.getStringSet("visible_items", null)
        if (saved != null) {
            addAll(saved)
        } else {
            // İlk açılışta varsayılanları ekle
            addAll(listOf("desk_setup_0"))
        }
    }

    private val _leaderboard = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardUser>> = combine(
        _leaderboard,
        snapshotFlow { userPhotoUrl.value }
    ) { users, localPhoto ->
        users.map { user ->
            if (user.email == userEmail.value && user.email.isNotBlank()) {
                user.copy(photoUrl = localPhoto)
            } else {
                user
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ofis Eşya Pozisyonları - SÜRÜM 2 (Oda Bazlı Düzenleme)
    val itemPositions = mutableStateMapOf<String, Offset>().apply {
        val saved = prefs.getString("office_layout_v2", null)
        if (saved != null) {
            try {
                saved.split("|").forEach { pair ->
                    val parts = pair.split(":")
                    if (parts.size == 2) {
                        val coords = parts[1].split(",")
                        if (coords.size == 2) {
                            put(parts[0], Offset(coords[0].toFloat(), coords[1].toFloat()))
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    // ÇALIŞAN SİSTEMİ
    val workers = mutableStateListOf<WorkerInfo>()
    val friendsList = mutableStateListOf<LeaderboardUser>()
    private val liveUsers = mutableStateMapOf<String, WorkerInfo>()
    val workerDetails = mutableStateOf<WorkerInfo?>(null)

    private var soundPool: SoundPool? = null
    private var keyboardSoundId: Int = 0
    private var mouseSoundId: Int = 0
    private var rainSoundId: Int = 0
    private var fireplaceSoundId: Int = 0
    private var dragonSoundId: Int = 0
    private var keyboardStreamId: Int = 0
    private var mouseStreamId: Int = 0
    private var rainStreamId: Int = 0
    private var fireplaceStreamId: Int = 0

    val isRainEnabled = mutableStateOf(prefs.getBoolean("is_rain_enabled", false))
    val isFireplaceEnabled = mutableStateOf(prefs.getBoolean("is_fireplace_enabled", false))

    init {
        setupSoundPool()
        checkRemoteUpdate()
        firebaseAuth.currentUser?.let { user ->
            isLoggedIn.value = true
            userEmail.value = user.email?.lowercase() ?: ""
            
            // Sadece yerelde yoksa Firebase Auth'tan al (Stale veriyi önlemek için)
            if (userName.value == "ANONYMOUS") {
                userName.value = user.displayName ?: "ANONYMOUS"
            }
            if (userPhotoUrl.value == null) {
                userPhotoUrl.value = user.photoUrl?.toString()
            }

            fetchLeaderboard()
            fetchUserDataFromFirestore()
            fetchUserTeam() // TAKIM VERİSİNİ ÇEK
            syncXpToFirestore() // Hemen senkronize et ki leaderboard boş kalmasın
            startFriendRequestListener()
            startFriendsListener()
            startLiveFocusListener()
        }
        
        // Arkadaş listesi güncellendiğinde ofisi tazele
        viewModelScope.launch {
            leaderboard.collect {
                updateFriendWorkers()
            }
        }

        // DÜZELTME: İlk açılışta masaları kayıtlara ekle (SADECE 3 MASA)
        if (!prefs.contains("office_layout_v2")) {
            val defaultDesks = listOf(
                -90f to 0f, 0f to 0f, 90f to 0f
            )
            defaultDesks.forEachIndexed { i, (x, y) ->
                itemPositions["desk_setup_$i"] = Offset(x, y)
            }
            saveLayoutToPrefs("v2")
        }

        initializeWorkers()
        startWorkerSimulation()
        syncTimerWithService()
    }

    fun updateUserName(newName: String) {
        if (newName.isNotBlank()) {
            userName.value = newName
            prefs.edit().putString("user_name", newName).apply()
            syncProfileToFirestore()
            syncXpToFirestore() // Liderlik tablosundaki adı da güncelle
        }
    }

    fun updateProfilePicture(uri: android.net.Uri) {
        val user = firebaseAuth.currentUser ?: return
        
        viewModelScope.launch {
            try {
                // 1. YEREL KOPYALAMA (Anında Kalıcılık)
                val localPath = withContext(Dispatchers.IO) {
                    val localFile = java.io.File(application.filesDir, "profile_pic_${user.uid}.jpg")
                    val inputStream = application.contentResolver.openInputStream(uri)
                    val outputStream = java.io.FileOutputStream(localFile)
                    
                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    localFile.absolutePath
                }

                // UI ve Cache'i anında yerel dosya ile güncelle
                // Cache busting için zaman damgası ekliyoruz
                val timestamp = System.currentTimeMillis()
                val cacheBustedPath = "$localPath?t=$timestamp"
                
                userPhotoUrl.value = cacheBustedPath
                prefs.edit()
                    .putString("user_photo_url", localPath)
                    .putLong("profile_last_local_update", timestamp)
                    .apply()

                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "Profil fotoğrafı güncellendi.", Toast.LENGTH_SHORT).show()
                }

                // 2. ARKA PLAN SENKRONİZASYONU (Firebase)
                isUploadingProfile.value = true
                
                launch(Dispatchers.IO) {
                    try {
                        val localFile = java.io.File(localPath)
                        val bytes = localFile.readBytes()
                        if (bytes.isNotEmpty()) {
                            val fileName = "profiles/${user.uid}.jpg"
                            val ref = storage.reference.child(fileName)
                            ref.putBytes(bytes).await()
                            
                            val downloadUrl = ref.downloadUrl.await()
                            val finalPhotoUrl = downloadUrl.toString()

                            // Auth Profilini Güncelle
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setPhotoUri(android.net.Uri.parse(finalPhotoUrl))
                                .build()
                            user.updateProfile(profileUpdates).await()

                            // Firestore Güncelle (Kullanıcı profili)
                            val profileMap = mapOf(
                                "photoUrl" to finalPhotoUrl,
                                "last_sync" to System.currentTimeMillis()
                            )
                            firestore.collection("users").document(user.uid)
                                .set(profileMap, com.google.firebase.firestore.SetOptions.merge()).await()
                            
                            // Auth nesnesini tazele ki yeni URL görünsün
                            user.reload().await()
                            
                            // Liderlik tablosunu tamamen güncelle
                            syncXpToFirestore() 

                            android.util.Log.d("FocusPathAuth", "Firebase senkronizasyonu başarılı.")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FocusPathAuth", "Arka plan yükleme hatası: ${e.message}")
                    } finally {
                        withContext(Dispatchers.Main) {
                            isUploadingProfile.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusPathAuth", "Yerel kayıt hatası", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "Resim kaydedilemedi.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun syncTimerWithService() {
        val targetEnd = prefs.getLong("TIMER_TARGET_END", 0L)
        val isServiceRunning = FocusService.isRunning
        val hasActiveSession = targetEnd > System.currentTimeMillis() && isServiceRunning
        
        if (hasActiveSession) {
            timerRunning.value = true
            isPomodoroMode.value = prefs.getBoolean("TIMER_IS_POMODORO", true)
            pomodoroTotalMillis.longValue = prefs.getLong("TIMER_INITIAL_DURATION", 25 * 60 * 1000L)
            timeLeft.longValue = targetEnd - System.currentTimeMillis()
        }

        timerSyncJob?.cancel()
        timerSyncJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (timerRunning.value) {
                    if (isPomodoroMode.value) {
                        timeLeft.longValue = FocusService.currentTime
                        if (!FocusService.isRunning) {
                            timerRunning.value = false
                        }
                    } else {
                        timeElapsed.longValue = FocusService.currentTime
                    }
                }
            }
        }
    }

    fun toggleTimer(context: Context, running: Boolean) {
        timerRunning.value = running
        val intent = Intent(context, FocusService::class.java).apply {
            action = if (running) FocusService.ACTION_START else FocusService.ACTION_STOP
            putExtra(FocusService.EXTRA_IS_POMODORO, isPomodoroMode.value)
            putExtra(FocusService.EXTRA_DURATION, pomodoroTotalMillis.longValue)
        }
        
        if (running) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            if (isPomodoroMode.value && timeLeft.longValue > 1000) {
                recordSessionResult(false)
            }
            context.stopService(intent)
        }
        
        setFocusActive(running, if(running) System.currentTimeMillis() + pomodoroTotalMillis.longValue else 0L)
    }

    private fun setupSoundPool() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, _ ->
            // Sesler yüklendiğinde çalmaya hazır olduklarını biliyoruz
            updateAmbientSounds()
        }

        keyboardSoundId = soundPool?.load(application, com.focuspath.app.R.raw.keyboard_tap, 1) ?: 0
        mouseSoundId = soundPool?.load(application, com.focuspath.app.R.raw.mouse_click, 1) ?: 0
        rainSoundId = soundPool?.load(application, com.focuspath.app.R.raw.rain, 1) ?: 0
        fireplaceSoundId = soundPool?.load(application, com.focuspath.app.R.raw.fireplace, 1) ?: 0
        dragonSoundId = soundPool?.load(application, com.focuspath.app.R.raw.dragon_correct, 1) ?: 0
    }

    fun toggleRain() {
        isRainEnabled.value = !isRainEnabled.value
        prefs.edit().putBoolean("is_rain_enabled", isRainEnabled.value).apply()
        updateAmbientSounds()
    }

    fun toggleFireplace() {
        isFireplaceEnabled.value = !isFireplaceEnabled.value
        prefs.edit().putBoolean("is_fireplace_enabled", isFireplaceEnabled.value).apply()
        updateAmbientSounds()
    }

    fun updateAmbientSounds() {
        val sp = soundPool ?: return
        val isActive = isFocusActive.value

        // RAIN
        if (isRainEnabled.value && isActive) {
            if (rainStreamId == 0 && rainSoundId != 0) {
                rainStreamId = sp.play(rainSoundId, 0.45f, 0.45f, 1, -1, 1.0f)
            } else if (rainStreamId != 0) {
                sp.resume(rainStreamId)
            }
        } else {
            if (rainStreamId != 0) {
                sp.pause(rainStreamId)
            }
        }

        // FIREPLACE
        if (isFireplaceEnabled.value && isActive) {
            if (fireplaceStreamId == 0 && fireplaceSoundId != 0) {
                fireplaceStreamId = sp.play(fireplaceSoundId, 0.55f, 0.55f, 1, -1, 1.0f)
            } else if (fireplaceStreamId != 0) {
                sp.resume(fireplaceStreamId)
            }
        } else {
            if (fireplaceStreamId != 0) {
                sp.pause(fireplaceStreamId)
            }
        }
    }

    fun playKeyboardSound(play: Boolean) {
        if (play && isFocusActive.value) {
            if (keyboardStreamId == 0) {
                keyboardStreamId = soundPool?.play(keyboardSoundId, 0.25f, 0.25f, 1, -1, 1.0f) ?: 0
            }
        } else {
            if (keyboardStreamId != 0) {
                soundPool?.stop(keyboardStreamId)
                keyboardStreamId = 0
            }
        }
    }

    fun playMouseSound(play: Boolean) {
        if (play && isFocusActive.value) {
            if (mouseStreamId == 0) {
                mouseStreamId = soundPool?.play(mouseSoundId, 0.2f, 0.2f, 1, -1, 1.0f) ?: 0
            }
        } else {
            if (mouseStreamId != 0) {
                soundPool?.stop(mouseStreamId)
                mouseStreamId = 0
            }
        }
    }

    fun playTickSound() {
        if (dragonSoundId != 0) {
            // Yeni dragon sesini çalıyoruz
            soundPool?.play(dragonSoundId, 1.0f, 1.0f, 2, 0, 1.0f)
        }
    }

    private var liveFocusRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var lastLiveFocusUpdate = 0L

    fun startLiveFocusListener() {
        val currentEmail = userEmail.value
        if (currentEmail.isBlank()) return

        liveFocusRegistration?.remove()
        liveFocusRegistration = firestore.collection("live_focus")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                // THROW AWAY UPDATES IF TOO FREQUENT (Anti-Overheating)
                // EXCEPT if there's an emoji update
                val now = System.currentTimeMillis()
                val hasEmojiUpdate = snapshot?.documentChanges?.any { 
                    val data = it.document.data
                    val emojiTime = data["emojiTime"] as? Long ?: 0L
                    (now - emojiTime) < 5000 
                } ?: false

                if (!hasEmojiUpdate && now - lastLiveFocusUpdate < 3000) return@addSnapshotListener
                if (!hasEmojiUpdate) lastLiveFocusUpdate = now

                snapshot?.let { querySnapshot ->
                    val now = System.currentTimeMillis()
                    val remoteUsers = querySnapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val email = data["email"] as? String ?: ""
                        
                        val lastUpd = data["lastUpdate"] as? Long ?: 0L
                        if (now - lastUpd > 60000) return@mapNotNull null // 1 dakikadan eski verileri gösterme
                        
                        val isMe = (email == currentEmail)
                        
                        WorkerInfo(
                            id = if (isMe) "me" else "live_$email",
                            name = (if (isMe) userName.value else data["name"] as? String) ?: "Peer",
                            photoUrl = (if (isMe) userPhotoUrl.value else data["photoUrl"] as? String),
                            email = email,
                            deskId = "live_desk_${email.hashCode() % 3}", 
                            isFocusing = true,
                            currentAction = WorkerAction.WORKING,
                            isLiveUser = true,
                            lastUpdate = lastUpd,
                            latestEmoji = data["latestEmoji"] as? String,
                            emojiTime = (data["emojiTime"] as? Long) ?: 0L,
                            isMe = isMe
                        )
                    }
                    
                    updateWorkersWithLivePeers(remoteUsers)
                }
            }
    }


    private fun updateWorkersWithLivePeers(livePeers: List<WorkerInfo>) {
        val names = listOf("Alex", "Jordan", "Casey", "Riley", "Taylor", "Morgan", "Quinn", "Skyler", "Charlie", "Avery", "Jamie", "Dakota")
        val positions = listOf("Senior Dev", "Lead Designer", "Security Expert", "DevOps Ninja", "AI Architect", "Data Scientist")
        val monitorContents = listOf("Terminal", "VS Code", "Figma", "Dashboard", "Logcat", "Kibana", "Jira", "Grafana")

        val currentEmail = userEmail.value
        
        // 1. SADECE GÖRÜNÜR OLAN MASALARI AL
        val activeDeskIds = visibleItems.filter { it.startsWith("desk_setup_") }
        if (activeDeskIds.isEmpty()) {
            workers.clear()
            return
        }

        // 2. ÖNCE KENDİMİ AL (Eğer odaklanıyorsam)
        val me = livePeers.find { it.isMe }
        
        // 3. BODY DOUBLING ARKADAŞINI BUL
        val buddyEmail = selectedFocusBuddy.value?.email
        val buddyUser = leaderboard.value.find { it.email == buddyEmail }
        
        // 4. ARKADAŞLARI AL (isFocusing olanlar, buddy hariç)
        val activeFriends = leaderboard.value.filter { it.email != currentEmail && it.email != buddyEmail && it.isFocusing }
        
        val isTeamMode = isTeamOfisMode.value
        val myTeamId = userTeam.value?.id

        // 5. KALAN YERLERE DÜNYADAN CANLI KİŞİLERİ AL (Team Mode Açıksa sadece takım arkadaşlarını al)
        val availableLivePeers = if (isTeamMode && myTeamId != null) {
            livePeers.filter { lp -> 
                !lp.isMe && lp.email != buddyEmail && 
                leaderboard.value.find { it.email == lp.email }?.teamId == myTeamId 
            }
        } else if (isTeamMode) {
            emptyList()
        } else {
            livePeers.filter { lp -> !lp.isMe && lp.email != buddyEmail && !activeFriends.any { it.email == lp.email } }
        }
        
        val priorityList = mutableListOf<WorkerInfo>()
        if (me != null) priorityList.add(me)
        
        // BODY DOUBLING ARKADAŞINI BAŞA EKLE (Kendimizden hemen sonra)
        if (buddyUser != null) {
            // Team modundaysak ve buddy takımda değilse göstermeyebiliriz (Tercihe bağlı)
            val showBuddy = !isTeamMode || buddyUser.teamId == myTeamId
            if (showBuddy) {
                priorityList.add(WorkerInfo(
                    id = "buddy_${buddyUser.email}",
                    name = buddyUser.name,
                    photoUrl = buddyUser.photoUrl,
                    email = buddyUser.email,
                    deskId = "",
                    isFocusing = buddyUser.isFocusing,
                    currentAction = if (buddyUser.isFocusing) WorkerAction.WORKING else WorkerAction.IDLE,
                    isFriend = true,
                    isLiveUser = true,
                    latestEmoji = buddyUser.latestEmoji,
                    emojiTime = buddyUser.emojiTime,
                    interactionText = buddyUser.currentTaskTitle // Arkadaşın neye odaklandığını göster
                ))
            }
            buddyCurrentTask.value = buddyUser.currentTaskTitle
        } else {
            buddyCurrentTask.value = null
        }

        activeFriends.forEach { f ->
            val showFriend = !isTeamMode || f.teamId == myTeamId
            if (showFriend) {
                priorityList.add(WorkerInfo(
                    id = "friend_${f.email}",
                    name = f.name,
                    photoUrl = f.photoUrl,
                    email = f.email,
                    deskId = "",
                    isFocusing = true,
                    currentAction = WorkerAction.WORKING,
                    isFriend = true,
                    isLiveUser = true,
                    latestEmoji = f.latestEmoji,
                    emojiTime = f.emojiTime
                ))
            }
        }
        priorityList.addAll(availableLivePeers)

        val finalWorkers = mutableListOf<WorkerInfo>()

        activeDeskIds.forEachIndexed { i, deskId ->
            val indexInDefault = deskId.removePrefix("desk_setup_").toIntOrNull() ?: 0
            val deskOffset = when(indexInDefault) {
                0 -> Offset(-90f, 0f)
                1 -> Offset(0f, 0f)
                else -> Offset(90f, 0f)
            }
            val spawnPos = itemPositions[deskId] ?: deskOffset

            val topPeer = priorityList.getOrNull(i)
            
            val worker = if (topPeer != null) {
                topPeer.copy(
                    deskId = deskId,
                    currentPos = spawnPos,
                    targetPos = spawnPos,
                    name = if (topPeer.isMe) "${topPeer.name} (Siz)" else topPeer.name
                )
            } else {
                // BOT OLUŞTUR
                val seed = i.toLong() + 200L
                val botRandom = Random(seed)
                WorkerInfo(
                    id = "bot_$i",
                    name = names.getOrElse(i % names.size) { "Expert $i" },
                    position = positions[botRandom.nextInt(positions.size)],
                    deskId = deskId,
                    currentPos = spawnPos,
                    targetPos = spawnPos,
                    monitorContent = monitorContents[botRandom.nextInt(monitorContents.size)],
                    currentAction = if (isFocusActive.value) WorkerAction.WORKING else WorkerAction.IDLE,
                    isFocusing = isFocusActive.value
                )
            }
            finalWorkers.add(worker)
        }

        workers.clear()
        workers.addAll(finalWorkers)
    }

    private fun updateLiveFocusStatus(active: Boolean) {
        val user = firebaseAuth.currentUser ?: return
        val email = user.email ?: return
        
        val docRef = firestore.collection("live_focus").document(email)
        if (active) {
            // Yerel dosya yolunu değil, Firebase'deki uzak URL'i gönder
            val remoteUrl = user.photoUrl?.toString() ?: ""
            
            val status = mapOf(
                "email" to email,
                "name" to userName.value,
                "photoUrl" to remoteUrl,
                "lastUpdate" to System.currentTimeMillis()
            )
            docRef.set(status, com.google.firebase.firestore.SetOptions.merge())
        } else {
            docRef.delete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        friendRequestRegistration?.remove()
        liveFocusRegistration?.remove()
        updateLiveFocusStatus(false)
        soundPool?.release()
        soundPool = null
    }

    private fun updateFriendWorkers() {
        // Bu fonksiyonun işlevini updateWorkersWithLivePeers devraldı.
        // Canlı veri geldiğinde orası her şeyi priority bazlı güncelliyor.
    }

    private fun initializeWorkers() {
        val names = listOf("Alex", "Jordan", "Casey", "Riley", "Taylor", "Morgan", "Quinn", "Skyler", "Charlie", "Avery", "Jamie", "Dakota")
        val positions = listOf("Senior Dev", "Lead Designer", "Security Expert", "DevOps Ninja", "AI Architect", "Data Scientist")
        val monitorContents = listOf("Terminal", "VS Code", "Figma", "Dashboard", "Logcat", "Kibana", "Jira", "Grafana")
        
        workers.clear()
        
        // LEADERBOARD'DAN RASTGELE KİŞİLERİ SEÇ
        val allLbUsers = leaderboard.value.filter { it.email != userEmail.value }.shuffled()
        
        // SADECE GÖRÜNÜR OLAN MASALARI BUL
        val activeDeskIds = visibleItems.filter { it.startsWith("desk_setup_") }
        
        // Oda merkezi (Tek oda)
        val roomBase = Offset(0f, 0f)

        activeDeskIds.forEachIndexed { i, deskId ->
            val indexInDefault = deskId.removePrefix("desk_setup_").toIntOrNull() ?: 0
            val seed = i.toLong() + 100L
            val botRandom = Random(seed)
            val lbUser = allLbUsers.getOrNull(i)
            
            val workerName = lbUser?.name ?: names.getOrElse(i % names.size) { "Expert $i" }
            val workerPhoto = lbUser?.photoUrl
            val workerPosition = if (lbUser != null) "Global Peer" else positions[botRandom.nextInt(positions.size)]
            val workerMonitor = if (lbUser != null && lbUser.isFocusing) "Focusing..." else monitorContents[botRandom.nextInt(monitorContents.size)]
            
            // Oda içindeki varsayılan masa pozisyonu (Yatay genişlik: 90f)
            val deskOffset = when(indexInDefault) {
                0 -> Offset(-90f, 0f)
                1 -> Offset(0f, 0f)
                else -> Offset(90f, 0f)
            }
            val defaultPos = roomBase + deskOffset
            
            val spawnPos = itemPositions[deskId] ?: defaultPos
            
            workers.add(
                WorkerInfo(
                    name = workerName,
                    position = workerPosition,
                    deskId = deskId,
                    currentPos = spawnPos,
                    targetPos = spawnPos,
                    monitorContent = workerMonitor,
                    currentAction = WorkerAction.WORKING,
                    photoUrl = workerPhoto,
                    isFriend = lbUser != null,
                    isFocusing = lbUser?.isFocusing ?: false,
                    roomId = 0,
                    email = lbUser?.email
                )
            )
        }
    }

    private fun startWorkerSimulation() {
        // Ana davranış döngüsü
        viewModelScope.launch {
            while (true) {
                delay(2000L + (0..3000).random())
                val size = workers.size
                for (i in 0 until size) {
                    if (i < workers.size) {
                        updateWorkerBehavior(i)
                    }
                }
            }
        }
        
        // Hareket döngüsü (Yürüme animasyonu için)
        viewModelScope.launch {
            while (true) {
                delay(30L)
                val currentSize = workers.size
                for (i in 0 until currentSize) {
                    if (i >= workers.size) continue
                    val worker = try { workers[i] } catch (e: Exception) { continue }
                    if (worker.currentAction == WorkerAction.WALKING) {
                        val newPos = moveTowards(worker.currentPos, worker.targetPos, 1.8f)
                        val facingRight = worker.targetPos.x > worker.currentPos.x
                        
                        if (newPos != worker.currentPos || facingRight != worker.isFacingRight) {
                            if (i < workers.size) {
                                workers[i] = workers[i].copy(
                                    currentPos = newPos,
                                    isFacingRight = facingRight
                                )
                            }
                        }
                        if (newPos == worker.targetPos) {
                            val finalAction = when {
                                worker.targetPos.x == 120f -> WorkerAction.COFFEE
                                worker.targetPos.x == -120f -> WorkerAction.MEETING
                                else -> WorkerAction.IDLE
                            }
                            if (i < workers.size) {
                                workers[i] = workers[i].copy(currentAction = finalAction, isFacingRight = true)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateWorkerBehavior(idx: Int) {
        val worker = workers[idx]
        if (worker.currentAction == WorkerAction.WALKING) return
        
        val isFocusActive = this.isFocusActive.value
        val rand = (0..100).random()
        val hasDesk = worker.deskId.isNotEmpty()
        val currentDeskPos = if (hasDesk) (itemPositions[worker.deskId] ?: worker.currentPos) else worker.currentPos

        val nextAction = when {
            isFocusActive && hasDesk -> {
                when {
                    rand < 12 -> { // Soru sorma ihtimalini %12 yaptık (Daha görünür olması için)
                        val questionsList = listOf(
                            "Bug'ı çözemedim, yardım?", "API dokümanı nerede?", "Kod incelemesi lazım.", 
                            "Bu logic doğru mu?", "Kahve isteyen var mı?", "PR bekliyor!",
                            "Fix this bug?", "Need help with API", "Code review anyone?", "PR is ready!"
                        )
                        workers[idx] = workers[idx].copy(interactionText = questionsList.random())
                        WorkerAction.ASKING
                    }
                    rand < 45 -> { workers[idx] = workers[idx].copy(interactionText = null); WorkerAction.TYPING }
                    rand < 75 -> { workers[idx] = workers[idx].copy(interactionText = null); WorkerAction.MOUSE }
                    rand < 90 -> { workers[idx] = workers[idx].copy(interactionText = null); WorkerAction.WORKING }
                    else -> { workers[idx] = workers[idx].copy(interactionText = null); WorkerAction.THINKING }
                }
            }
            !isFocusActive && hasDesk -> {
                if (worker.movementCount >= 2) {
                    if (kotlin.math.abs(worker.currentPos.x - currentDeskPos.x) > 5f || kotlin.math.abs(worker.currentPos.y - currentDeskPos.y) > 5f) {
                        startWalking(idx, currentDeskPos)
                        return
                    }
                    WorkerAction.IDLE
                } else {
                    when {
                        rand < 10 -> WorkerAction.TYPING
                        rand < 20 -> WorkerAction.MOUSE
                        rand < 30 -> WorkerAction.THINKING
                        rand < 45 -> WorkerAction.RESTING
                        rand < 60 -> { incrementMovement(idx); startWalking(idx, Offset(120f, -60f)); return }
                        rand < 75 -> { incrementMovement(idx); startWalking(idx, Offset(-120f, 60f)); return }
                        rand < 85 -> {
                            incrementMovement(idx)
                            val wanderOffset = Offset((-100..100).random().toFloat(), (-80..80).random().toFloat())
                            startWalking(idx, wanderOffset)
                            return
                        }
                        else -> {
                            if (kotlin.math.abs(worker.currentPos.x - currentDeskPos.x) > 30f || kotlin.math.abs(worker.currentPos.y - currentDeskPos.y) > 30f) {
                                if ((0..1).random() == 0) { startWalking(idx, currentDeskPos); return }
                            }
                            WorkerAction.IDLE
                        }
                    }
                }
            }
            else -> {
                when {
                    rand < 30 -> WorkerAction.THINKING
                    rand < 60 -> WorkerAction.IDLE
                    rand < 70 && worker.movementCount < 2 -> {
                        incrementMovement(idx)
                        val randomArea = if((0..1).random() == 0) Offset(120f, -60f) else Offset(-120f, 60f)
                        startWalking(idx, randomArea)
                        return
                    }
                    else -> WorkerAction.RESTING
                }
            }
        }
        
        workers[idx] = worker.copy(
            currentAction = nextAction, 
            isFocusing = isFocusActive && hasDesk,
            lastActionTime = System.currentTimeMillis()
        )
        
        if (isFocusActive && hasDesk && (nextAction == WorkerAction.TYPING || nextAction == WorkerAction.MOUSE)) {
            workers[idx].xpContribution += 1
        }
    }

    private fun incrementMovement(idx: Int) {
        workers[idx] = workers[idx].copy(movementCount = workers[idx].movementCount + 1)
    }

    private fun startWalking(idx: Int, target: Offset) {
        workers[idx] = workers[idx].copy(currentAction = WorkerAction.WALKING, targetPos = target)
    }

    private fun moveTowards(current: Offset, target: Offset, speed: Float): Offset {
        val dx = target.x - current.x
        val dy = target.y - current.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        if (distance <= speed) return target
        return Offset(current.x + (dx / distance) * speed, current.y + (dy / distance) * speed)
    }

    fun syncTasksFromCloud() {
        val email = userEmail.value
        if (email.isBlank()) return
        
        firestore.collection("users").document(email).collection("tasks").get().addOnSuccessListener { snapshot ->
            viewModelScope.launch(Dispatchers.IO) {
                snapshot.documents.forEach { doc ->
                    val task = doc.toObject(TaskEntity::class.java)
                    if (task != null) {
                        val fixedTask = if (task.id == 0L) {
                            task.copy(id = doc.id.toLongOrNull() ?: System.currentTimeMillis())
                        } else task
                        taskDao.insertTask(fixedTask)
                    }
                }
            }
        }
    }

    private fun fetchUserDataFromFirestore() {
        val user = firebaseAuth.currentUser ?: return
        val uid = user.uid
        val email = user.email ?: ""

        val docRefByUid = firestore.collection("users").document(uid)
        val docRefByEmail = if (email.isNotBlank()) firestore.collection("users").document(email) else null

        docRefByUid.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                applyUserDataDoc(doc)
            } else if (docRefByEmail != null) {
                docRefByEmail.get().addOnSuccessListener { emailDoc ->
                    if (emailDoc.exists()) {
                        applyUserDataDoc(emailDoc)
                    }
                }
            }
        }
        syncTasksFromCloud()
    }

    private fun applyUserDataDoc(doc: com.google.firebase.firestore.DocumentSnapshot) {
        val cloudXp = doc.getLong("user_xp")?.toInt() ?: 0
        if (cloudXp > userXp.value) {
            userXp.value = cloudXp
            prefs.edit().putInt("user_xp", cloudXp).apply()
        }
        val cloudCoins = doc.getLong("user_coins")?.toInt() ?: 0
        if (cloudCoins > userCoins.value) {
            userCoins.value = cloudCoins
            prefs.edit().putInt("user_coins", cloudCoins).apply()
        }
        val cloudLifetimeCoins = doc.getLong("lifetime_coins")?.toInt() ?: 0
        if (cloudLifetimeCoins > lifetimeCoins.value) {
            lifetimeCoins.value = cloudLifetimeCoins
            prefs.edit().putInt("lifetime_coins", cloudLifetimeCoins).apply()
        }
        val cloudOfficeLevel = doc.getLong("office_level")?.toInt() ?: 1
        if (cloudOfficeLevel > officeLevel.value) {
            officeLevel.value = cloudOfficeLevel
            prefs.edit().putInt("office_level", cloudOfficeLevel).apply()
        }
        val cloudUnlocked = doc.get("unlocked_items") as? List<String>
        if (cloudUnlocked != null) {
            cloudUnlocked.forEach { item ->
                if (!unlockedItems.contains(item)) {
                    unlockedItems.add(item)
                }
            }
            prefs.edit().putStringSet("unlocked_items", unlockedItems.toSet()).apply()
        }
        
        // Photo and Name sync
        doc.getString("photoUrl")?.let { cloudPhoto ->
            if (cloudPhoto.isNotBlank()) {
                val cloudSyncTime = doc.getLong("last_sync") ?: 0L
                val localUpdateTime = prefs.getLong("profile_last_local_update", 0L)
                
                // Sadece buluttaki veri daha yeniyse (veya yerelde hiç yoksa) kabul et
                if (cloudSyncTime > localUpdateTime || userPhotoUrl.value == null) {
                    userPhotoUrl.value = cloudPhoto
                    prefs.edit().putString("user_photo_url", cloudPhoto).apply()
                }
            }
        }
        doc.getString("name")?.let { cloudName ->
            if (cloudName.isNotBlank() && cloudName != "ANONYMOUS") {
                userName.value = cloudName
                prefs.edit().putString("user_name", cloudName).apply()
            }
        }
    }

    private fun syncProfileToFirestore() {
        val user = firebaseAuth.currentUser ?: return
        // Yerel dosya yolunu değil, Firebase'deki uzak URL'i gönder
        val remoteUrl = user.photoUrl?.toString() ?: ""
        
        val profileMap = mapOf(
            "uid" to user.uid,
            "name" to userName.value,
            "email" to (user.email?.lowercase() ?: ""),
            "photoUrl" to remoteUrl,
            "user_xp" to userXp.value,
            "user_coins" to userCoins.value,
            "lifetime_coins" to lifetimeCoins.value,
            "office_level" to officeLevel.value,
            "unlocked_items" to unlockedItems.toList(),
            "last_sync" to System.currentTimeMillis()
        )
        // Hem UID hem Email ile dokümanı güncelleyelim ki uyuşmazlıklar tamamen ortadan kalksın
        firestore.collection("users").document(user.uid).set(profileMap, com.google.firebase.firestore.SetOptions.merge())
        val email = user.email
        if (!email.isNullOrBlank()) {
            firestore.collection("users").document(email).set(profileMap, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow(0)
    val sortType: StateFlow<Int> = _sortType.asStateFlow()

    private val _taskFilter = MutableStateFlow(0)
    val taskFilter: StateFlow<Int> = _taskFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow("Tümü")
    private val _priorityFilter = MutableStateFlow(-1)
    val priorityFilter: StateFlow<Int> = _priorityFilter.asStateFlow()

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    
    val totalFocusMinutes: Flow<Int> = taskDao.getTotalFocusMinutes().map { it ?: 0 }
    val totalTasksCompleted: Flow<Int> = taskDao.getTotalTasksCompleted().map { it ?: 0 }

    val tasks: Flow<List<TaskEntity>> = combine(
        allTasks, _searchQuery, _sortType, _taskFilter, _categoryFilter, _priorityFilter, _selectedDate
    ) { flowArray ->
        val tasks = flowArray[0] as List<TaskEntity>
        val query = flowArray[1] as String
        val sort = flowArray[2] as Int
        val filter = flowArray[3] as Int
        val category = flowArray[4] as String
        val priorityF = flowArray[5] as Int
        val sDate = flowArray[6] as Long

        val filtered = tasks.filter { task ->
            val matchesSearch = query.isBlank() || task.title.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) { 1 -> !task.isCompleted; 2 -> task.isCompleted; else -> true }
            val matchesCategory = category == "Tümü" || task.category == category
            val matchesPriority = priorityF == -1 || task.priority == priorityF
            val matchesDate = isSameDay(task.dueDate, sDate)
            matchesSearch && matchesFilter && matchesCategory && matchesPriority && matchesDate
        }
        when (sort) {
            1 -> filtered.sortedByDescending { it.priority }
            2 -> filtered.sortedBy { it.title.lowercase() }
            3 -> filtered.sortedBy { it.isCompleted }
            else -> filtered
        }
    }

    private fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun setSelectedDate(millis: Long) { _selectedDate.value = millis }

    val isDarkMode = mutableStateOf(prefs.getBoolean("is_dark_mode", true))
    val isHapticEnabled = mutableStateOf(prefs.getBoolean("is_haptic_enabled", true))
    val isPremium = mutableStateOf(prefs.getBoolean("is_premium", false))
    val themeColorIndex = mutableStateOf(prefs.getInt("theme_color_index", 0))

    val isNotificationEnabled = mutableStateOf(prefs.getBoolean("is_notification_enabled", true))
    val alarmSound = mutableStateOf(prefs.getString("alarm_sound", "default") ?: "default")
    val alarmVolume = mutableFloatStateOf(prefs.getFloat("alarm_volume", 0.5f))
    val isAutoDndEnabled = mutableStateOf(prefs.getBoolean("is_auto_dnd_enabled", false))
    
    val currentLayoutName = mutableStateOf(prefs.getString("active_layout_name", "Standart Ofis") ?: "Standart Ofis")

    val showReviewDialog = mutableStateOf(false)
    private var hasSeenReview = prefs.getBoolean("has_seen_review", false)

    fun incrementWinCount() {
        if (!hasSeenReview) {
            showReviewDialog.value = true
            hasSeenReview = true
            prefs.edit().putBoolean("has_seen_review", true).apply()
        }
    }

    val userXp = mutableStateOf(prefs.getInt("user_xp", 0))
    val userLevel = derivedStateOf { (userXp.value / 100) + 1 }
    val userRank = derivedStateOf {
        when {
            userXp.value < 100 -> "ROOT_INIT"
            userXp.value < 500 -> "SYSTEM_USER"
            userXp.value < 2000 -> "POWER_USER"
            else -> "SUDO_ADMIN"
        }
    }

    val chatHistory = mutableStateListOf<String>()
    val isBotTyping = mutableStateOf(false)
    val selectedChatUser = mutableStateOf<String?>(null)
    val selectedChatUserEmail = mutableStateOf<String?>(null) // Gerçek e-posta üzerinden mesajlaşma için
    
    private val _directMessages = mutableStateListOf<DirectMessage>()
    val directMessages: List<DirectMessage> get() = _directMessages.sortedBy { it.timestamp }

    fun toggleTeamOfficeMode(enabled: Boolean) {
        isTeamOfisMode.value = enabled
        prefs.edit().putBoolean("is_team_office_mode", enabled).apply()
        // Ofisi hemen güncellemek için live listener'ı tetikleyebiliriz
        val currentPeers = workers.toList()
        updateWorkersWithLivePeers(currentPeers)
    }

    fun createTeam(teamName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser ?: return
        val email = user.email?.lowercase() ?: return
        isTeamLoading.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val teamId = UUID.randomUUID().toString()
                val inviteCode = (1..6).map { (('A'..'Z') + ('0'..'9')).random() }.joinToString("")
                
                val newTeam = Team(
                    id = teamId,
                    name = teamName,
                    inviteCode = inviteCode,
                    creatorUid = user.uid,
                    memberEmails = listOf(email)
                )
                
                firestore.collection("teams").document(teamId).set(newTeam).await()
                
                // Kullanıcı profilini güncelle (Hem UID hem Email dökümanını güncelle)
                val updateMap = mapOf("teamId" to teamId)
                firestore.collection("users").document(user.uid).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                if (user.email != null) {
                    firestore.collection("users").document(user.email!!.lowercase()).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                }
                firestore.collection("leaderboard").document(user.uid).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                
                withContext(Dispatchers.Main) {
                    isTeamLoading.value = false
                    fetchUserTeam()
                    onSuccess()
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusPathTeam", "Create Team Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    isTeamLoading.value = false
                    onError(e.localizedMessage ?: "Takım oluşturulamadı")
                }
            }
        }
    }

    fun joinTeam(inviteCode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser ?: return
        val email = user.email?.lowercase() ?: return
        val cleanCode = inviteCode.trim().uppercase()
        isTeamLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val teamSnapshot = firestore.collection("teams")
                    .whereEqualTo("inviteCode", cleanCode)
                    .get().await()

                if (teamSnapshot.isEmpty) {
                    throw Exception("Geçersiz davet kodu")
                }

                val teamDoc = teamSnapshot.documents[0]
                val team = teamDoc.toObject(Team::class.java) ?: throw Exception("Takım verisi okunamadı")
                
                if (team.memberEmails.contains(email)) {
                    throw Exception("Zaten bu takımdasınız")
                }

                val updatedMembers = team.memberEmails.toMutableList().apply { add(email) }
                firestore.collection("teams").document(team.id).update("memberEmails", updatedMembers).await()

                // Kullanıcı profilini güncelle (Hem UID hem Email dökümanını güncelle)
                val updateMap = mapOf("teamId" to team.id)
                firestore.collection("users").document(user.uid).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                if (user.email != null) {
                    firestore.collection("users").document(user.email!!.lowercase()).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                }
                firestore.collection("leaderboard").document(user.uid).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()

                withContext(Dispatchers.Main) {
                    isTeamLoading.value = false
                    fetchUserTeam()
                    onSuccess()
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusPathTeam", "Join Team Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    isTeamLoading.value = false
                    onError(e.localizedMessage ?: "Takıma katılım başarısız")
                }
            }
        }
    }

    fun leaveTeam(onSuccess: () -> Unit) {
        val user = firebaseAuth.currentUser ?: return
        val email = user.email?.lowercase() ?: return
        val currentTeamId = userTeam.value?.id ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val teamDoc = firestore.collection("teams").document(currentTeamId).get().await()
                val team = teamDoc.toObject(Team::class.java)
                
                if (team != null) {
                    val updatedMembers = team.memberEmails.filter { it != email }
                    if (updatedMembers.isEmpty()) {
                        firestore.collection("teams").document(currentTeamId).delete().await()
                    } else {
                        firestore.collection("teams").document(currentTeamId).update("memberEmails", updatedMembers).await()
                    }
                }

                // Kullanıcı profilinden takım ID'sini kaldır
                val updateMap = mutableMapOf<String, Any?>("teamId" to null)
                firestore.collection("users").document(user.uid).update(updateMap).await()
                firestore.collection("leaderboard").document(user.uid).update(updateMap).await()

                withContext(Dispatchers.Main) {
                    userTeam.value = null
                    teamMembers.clear()
                    onSuccess()
                }
            } catch (e: Exception) {}
        }
    }

    fun fetchUserTeam() {
        val user = firebaseAuth.currentUser ?: return
        
        firestore.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            val teamId = doc.getString("teamId")
            if (teamId != null) {
                listenToTeam(teamId)
            }
        }
    }

    private fun listenToTeam(teamId: String) {
        teamListener?.remove()
        teamListener = firestore.collection("teams").document(teamId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                val team = snapshot?.toObject(Team::class.java)
                userTeam.value = team
                
                if (team != null) {
                    // Takım üyelerini getir
                    firestore.collection("leaderboard")
                        .whereIn("email", team.memberEmails)
                        .addSnapshotListener { lbSnapshot, _ ->
                            val members = lbSnapshot?.documents?.mapNotNull { it.toObject(LeaderboardUser::class.java) } ?: emptyList()
                            teamMembers.clear()
                            teamMembers.addAll(members)
                        }
                }
            }
    }

    fun fetchLeaderboard() {
        isLeaderboardLoading.value = true
        
        // Timeout için bir mekanizma ekleyelim
        val timeoutJob = viewModelScope.launch {
            kotlinx.coroutines.delay(10000) // 10 saniye sonra hala yükleniyorsa zorla kapat
            if (isLeaderboardLoading.value) {
                isLeaderboardLoading.value = false
                android.util.Log.w("FocusPathFirestore", "Leaderboard fetch timeout")
            }
        }

        // KİŞİSEL LİDERLİK TABLOSU
        firestore.collection("leaderboard")
            .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, e ->
                timeoutJob.cancel()
                isLeaderboardLoading.value = false
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { it.toObject(LeaderboardUser::class.java) }
                    _leaderboard.value = users
                    initializeWorkers()
                }
            }
        
        // TAKIM LİDERLİK TABLOSU
        firestore.collection("teams")
            .orderBy("totalTeamXp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val teams = snapshot.documents.mapNotNull { it.toObject(Team::class.java) }
                    _teamLeaderboard.value = teams
                }
            }
        
        // Incoming Messages Listener
        listenForIncomingMessages()
    }

    private fun listenForIncomingMessages() {
        val email = userEmail.value
        if (email.isBlank()) return
        
        // DÜZELTME: Composite Index hatasını önlemek için orderBy'ı sunucuda değil, in-memory yapıyoruz
        firestore.collection("messages")
            .whereEqualTo("to", email)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val msg = change.document.toObject(DirectMessage::class.java)
                        if (!_directMessages.any { it.id == msg.id }) {
                            _directMessages.add(msg)
                            // Gelen mesaj bildirimi - Sadece son 10 saniye içinde gönderilmişse göster (Spam engelleme)
                            if (System.currentTimeMillis() - msg.timestamp < 10000) {
                                viewModelScope.launch(Dispatchers.Main) {
                                    val toastMsg = if (msg.text.contains("Sana bir tepki gönderdi:")) {
                                        val emoji = msg.text.substringAfterLast(": ").trim()
                                        if (msg.from == userEmail.value) {
                                            "Kendine $emoji gönderdin!"
                                        } else {
                                            "${msg.fromName} sana $emoji gönderdi!"
                                        }
                                    } else {
                                        "${msg.fromName}: ${msg.text}"
                                    }
                                    Toast.makeText(application, toastMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
            
        firestore.collection("messages")
            .whereEqualTo("from", email)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val msg = change.document.toObject(DirectMessage::class.java)
                        if (!_directMessages.any { it.id == msg.id }) {
                            _directMessages.add(msg)
                        }
                    }
                }
            }
    }

    fun sendEmojiReaction(targetEmail: String, emoji: String) {
        if (targetEmail.isBlank()) return
        val cleanTargetEmail = targetEmail.trim().lowercase()
        val now = System.currentTimeMillis()
        
        // 1. Liderlik tablosunu güncelle (Ofis karakterleri için)
        firestore.collection("leaderboard")
            .whereEqualTo("email", cleanTargetEmail)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    doc.reference.update("latestEmoji", emoji, "emojiTime", now)
                }
            }
            
        // 2. Canlı Ofis durumunu güncelle (Anlık görünmesi için)
        firestore.collection("live_focus").document(cleanTargetEmail)
            .set(mapOf("latestEmoji" to emoji, "emojiTime" to now), com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener {
                android.util.Log.e("FocusPathEmoji", "Live focus update failed: ${it.message}")
            }
            
        // 3. Mesaj olarak gönder (Gelen kutusuna düşmesi için asıl yöntem)
        val targetUser = _leaderboard.value.find { it.email.lowercase() == cleanTargetEmail }
        val targetName = targetUser?.name ?: "Arkadaş"
        sendDirectMessage(cleanTargetEmail, targetName, "Sana bir tepki gönderdi: $emoji")
    }

    fun sendDirectMessage(toEmail: String, toName: String, content: String) {
        val fromEmail = userEmail.value.lowercase()
        val fromName = userName.value
        val cleanToEmail = toEmail.trim().lowercase()
        if (fromEmail.isBlank() || cleanToEmail.isBlank()) return
        
        val msgId = UUID.randomUUID().toString()
        val msg = DirectMessage(
            id = msgId,
            from = fromEmail,
            fromName = fromName,
            to = toEmail,
            toName = toName,
            text = content,
            timestamp = System.currentTimeMillis()
        )
        
        firestore.collection("messages").document(msgId).set(msg)
    }

    private fun syncTaskToFirestore(task: TaskEntity) {
        if (!isLoggedIn.value || userEmail.value.isBlank()) return
        firestore.collection("users").document(userEmail.value).collection("tasks").document(task.id.toString()).set(task)
    }

    private fun deleteTaskFromFirestore(taskId: Long) {
        if (!isLoggedIn.value || userEmail.value.isBlank()) return
        firestore.collection("users").document(userEmail.value).collection("tasks").document(taskId.toString()).delete()
    }

    fun syncXpToFirestore(sessionDuration: Int = 0, isFocusing: Boolean = false) {
        val user = firebaseAuth.currentUser ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTasks = taskDao.getAllTasksOnce()
                val activeTask = currentTasks.find { !it.isCompleted && it.priority == 2 } ?: currentTasks.firstOrNull { !it.isCompleted }

                // Yerel dosya yolunu değil, Firebase'deki uzak URL'i gönder
                val remoteUrl = user.photoUrl?.toString() ?: ""

                val updateMap = mutableMapOf<String, Any>(
                    "uid" to user.uid,
                    "email" to (user.email?.lowercase() ?: ""),
                    "score" to userXp.value.toLong(),
                    "photoUrl" to remoteUrl,
                    "timestamp" to System.currentTimeMillis(),
                    "sessionDuration" to sessionDuration,
                    "isFocusing" to isFocusing,
                    "focusBuddyEmail" to (selectedFocusBuddy.value?.email ?: ""),
                    "currentTaskTitle" to (if (isFocusing) activeTask?.title ?: "" else "")
                )
                
                if (userName.value != "ANONYMOUS" && userName.value.isNotBlank()) { 
                    updateMap["name"] = userName.value 
                } else if (!user.displayName.isNullOrBlank()) { 
                    updateMap["name"] = user.displayName!! 
                }

                firestore.collection("leaderboard").document(user.uid)
                    .set(updateMap, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                
                android.util.Log.d("FocusPathFirestore", "Leaderboard sync successful")
            } catch (e: Exception) {
                android.util.Log.e("FocusPathFirestore", "Leaderboard sync error: ${e.message}")
            }
        }
    }

    private var chatSession = generativeModel.startChat()

    fun sendAiCommand(prompt: String, isEnglish: Boolean) {
        val target = selectedChatUser.value
        val prefix = if (target != null) "@$target: " else ""
        
        chatHistory.add("${if (isEnglish) "You: " else "Siz: "}$prefix$prompt")
        
        // Eğer bir kullanıcıya mesaj atılıyorsa, otomatik cevap verme (gerçek kişi cevaplar)
        if (target != null) return

        isBotTyping.value = true
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { chatSession.sendMessage(prompt) }
                chatHistory.add("YimeBot: ${response.text ?: "..."}")
            } catch (e: Exception) { 
                android.util.Log.e("FocusPathAI", "Chat AI error: ${e.message}", e)
                val msg = e.toString()
                
                // HER DURUMDA ÇEVRİMDIŞI YANIT VERMEYİ DENE (Kullanıcı Deneyimi İçin)
                val localTasks = taskDao.getAllTasksOnce()
                val localResponse = getLocalAiResponse(prompt, isEnglish, localTasks)
                
                // Eğer yerel sistemde bu prompt için bir cevap varsa onu ver
                if (!localResponse.contains("Çevrimdışı Mod: Bu komutu şu an", ignoreCase = true) && 
                    !localResponse.contains("Offline Mode: I cannot process", ignoreCase = true)) {
                    chatHistory.add("YimeBot (Offline): $localResponse")
                } else {
                    // Yerel cevap yoksa hatayı göster
                    val errorMsg = when {
                        msg.contains("404") -> if(isEnglish) "Model Not Found" else "Model Bulunamadı"
                        msg.contains("401") || msg.contains("API_KEY_INVALID") || msg.contains("invalid api key", ignoreCase = true) -> if(isEnglish) "Invalid API Key" else "Geçersiz API Anahtarı"
                        msg.contains("location is not supported") -> if(isEnglish) "Region Not Supported" else "Bölge Desteklenmiyor"
                        else -> "Detail: ${e.localizedMessage ?: msg}"
                    }
                    chatHistory.add("YimeBot: ERROR ($errorMsg)")
                }
            }
            finally { isBotTyping.value = false }
        }
    }

    private fun getLocalAiResponse(prompt: String, isEnglish: Boolean, tasks: List<TaskEntity>): String {
        val now = System.currentTimeMillis()
        val todayTasks = tasks.filter { isSameDay(it.dueDate, now) }
        val activeTasks = todayTasks.filter { !it.isCompleted }
        val completedToday = todayTasks.count { it.isCompleted }
        val totalToday = todayTasks.size
        
        val lowerPrompt = prompt.lowercase()

        return when {
            // 1. İlerleme ve Durum Raporu
            lowerPrompt.contains("status") || lowerPrompt.contains("progress") || 
            lowerPrompt.contains("durum") || lowerPrompt.contains("neler var") || lowerPrompt.contains("analiz") -> {
                if (totalToday == 0) {
                    if (isEnglish) "You haven't added any tasks for today yet. Let's start by planning one!"
                    else "Bugün için henüz bir görev eklememişsin. Bir plan yaparak başlayalım mı?"
                } else {
                    val percent = if (totalToday > 0) (completedToday * 100) / totalToday else 0
                    if (isEnglish) {
                        "Progress Report: You've completed $completedToday out of $totalToday tasks today ($percent%). " +
                        if (percent >= 100) "Legendary! You crushed the day!" else "Keep going, you're doing great!"
                    } else {
                        "İlerleme Raporu: Bugün $totalToday görevden $completedToday tanesini bitirdin (%$percent). " +
                        if (percent >= 100) "Efsanesin! Bugünün hakkını verdin!" else "Harika gidiyorsun, durmak yok!"
                    }
                }
            }
            
            // 2. Enerji Odaklı Öneriler
            lowerPrompt.contains("tired") || lowerPrompt.contains("energy") || 
            lowerPrompt.contains("yorgun") || lowerPrompt.contains("enerji") || lowerPrompt.contains("mod") || lowerPrompt.contains("halim yok") -> {
                val energy = selectedTaskEnergy.intValue
                if (energy <= 2) {
                    val easyTask = activeTasks.find { it.priority == 0 }
                    if (isEnglish) "Your energy is low. Maybe try a 'Quick Win'? ${easyTask?.let { "Starting with '${it.title}' might help." } ?: "Just take a 5-min break."}"
                    else "Enerjin düşük görünüyor. Belki bir 'Hızlı Galibiyet' iyi gelir? ${easyTask?.let { "'${it.title}' görevine küçük bir adım atmaya ne dersin?" } ?: "Sadece 5 dakikalık bir mola ver."}"
                } else {
                    val bigTask = activeTasks.find { it.priority == 2 }
                    if (isEnglish) "Energy levels are high! Perfect time to tackle your biggest challenge: ${bigTask?.title ?: "any high-priority task"}."
                    else "Enerjin yüksek! En büyük zorluğun olan '${bigTask?.title ?: "yüksek öncelikli bir görev"}' için harika bir zaman."
                }
            }

            // 3. Odaklanma Asistanı
            lowerPrompt.contains("focus") || lowerPrompt.contains("timer") || 
            lowerPrompt.contains("odak") || lowerPrompt.contains("pomodoro") || lowerPrompt.contains("başla") -> {
                if (timerRunning.value) {
                    val mins = (timeLeft.longValue / 1000) / 60
                    if (isEnglish) "You are currently in a focus session! $mins minutes left. Stay in the zone, I'm right here with you."
                    else "Şu an odaklanma seansındasın! Yaklaşık $mins dakikan kaldı. Bölgeyi terk etme, ben yanındayım."
                } else {
                    val suggest = if (activeTasks.isNotEmpty()) activeTasks.first().title else "something small"
                    if (isEnglish) "Ready for deep work? I suggest a 25-min Pomodoro for '$suggest'."
                    else "Derin çalışma için hazır mısın? '$suggest' için 25 dakikalık bir Pomodoro öneririm."
                }
            }

            // 4. Motivasyon ve Destek
            lowerPrompt.contains("motivate") || lowerPrompt.contains("bored") || 
            lowerPrompt.contains("destek") || lowerPrompt.contains("sıkıldım") || lowerPrompt.contains("gaz ver") -> {
                val quotes = if (isEnglish) listOf(
                    "Action is the antidote to anxiety.",
                    "Done is better than perfect.",
                    "Small steps lead to big changes.",
                    "Focus on one thing at a time."
                ) else listOf(
                    "Eylem, kaygının panzehiridir.",
                    "Tamamlanmış, mükemmelden iyidir.",
                    "Küçük adımlar büyük değişimlere yol açar.",
                    "Aynı anda sadece tek bir şeye odaklan."
                )
                if (isEnglish) "Yime Motivation: ${quotes.random()} You've got this!"
                else "Yime Motivasyon: ${quotes.random()} Bunu yapabilirsin!"
            }

            // 5. Kodlama ve Teknik İpuçları
            lowerPrompt.contains("code") || lowerPrompt.contains("kod") || lowerPrompt.contains("yazılım") -> {
                val tips = if (isEnglish) listOf(
                    "Try the Pomodoro technique to avoid burnout.",
                    "Modular code is easier to debug. Break down your functions.",
                    "Don't forget to write unit tests for critical business logic.",
                    "Keep your Composable functions small and focused.",
                    "Believing is half of success.",
                    "Nothing is impossible."
                ) else listOf(
                    "Tükenmişliği önlemek için Pomodoro tekniğini kullan.",
                    "Modüler kodun hata ayıklaması daha kolaydır. Fonksiyonlarını parçala.",
                    "Kritik iş mantıkları için unit test yazmayı unutma.",
                    "Composable fonksiyonlarını küçük ve tek bir işe odaklı tut.",
                    "İnanmak başarmanın yarısıdır.",
                    "İmkansız diye bir şey yoktur."
                )
                tips.random()
            }

            // 6. Planlama ve Optimizasyon (Geliştirilmiş Suggestion)
            lowerPrompt.contains("optimize") || lowerPrompt.contains("plan") || 
            lowerPrompt.contains("suggestion") || lowerPrompt.contains("öneri") || lowerPrompt.contains("ne yapayım") -> {
                if (activeTasks.isEmpty()) {
                    if (isEnglish) "Offline Mode: I checked your list, you should create a task first."
                    else "Çevrimdışı Mod: Görev listene baktım, henüz bugüne ait aktif bir görevin yok. Yeni bir hedef belirlemeye ne dersin?"
                } else {
                    val sorted = activeTasks.sortedByDescending { it.priority }
                    val topTask = sorted.first().title
                    val totalMins = activeTasks.sumOf { 
                        if (it.estimatedMinutes > 0) it.estimatedMinutes 
                        else when(it.priority) {
                            2 -> 60
                            0 -> 15
                            else -> 30
                        }
                    }
                    val taskCount = activeTasks.size

                    if (isEnglish) {
                        "Offline Mode: You have $taskCount tasks left today. I suggest starting with '$topTask'. " +
                        (if (totalMins > 0) "Total estimated time: $totalMins mins. " else "") +
                        "Let's focus!"
                    } else {
                        "Çevrimdışı Mod: Bugün için kalan $taskCount görevin var. Analizime göre; önce '$topTask' görevine odaklanmalısın. " +
                        (if (totalMins > 0) "Yaklaşık $totalMins dakikalık bir işin var. " else "") +
                        "Hadi başlayalım!"
                    }
                }
            }

            else -> {
                if (isEnglish) "I'm offline, but I can help with 'status', 'energy', 'focus', 'motivation' or 'planning'!"
                else "Şu an çevrimdışıyım ama 'durum', 'enerji', 'odak', 'motivasyon' veya 'planlama' konularında sana yardımcı olabilirim!"
            }
        }
    }

    fun resetChat() { chatHistory.clear() ; chatSession = generativeModel.startChat() ; isBotTyping.value = false }
    fun suggestPriorityTask(taskList: List<TaskEntity>, isEnglish: Boolean) {
        if (taskList.isEmpty()) return
        val tasksString = taskList.filter { !it.isCompleted }.joinToString { it.title }
        if (tasksString.isBlank()) return
        sendAiCommand("Suggestion for: $tasksString", isEnglish)
    }

    fun addTask(title: String, notes: String, category: String, priority: Int, dueDate: Long = System.currentTimeMillis(), rewardCoins: Int = 0, estimation: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = TaskEntity(
                title = title, 
                notes = notes, 
                category = category, 
                priority = priority, 
                dueDate = dueDate, 
                rewardCoins = rewardCoins,
                energyLevel = selectedTaskEnergy.intValue,
                estimatedMinutes = estimation
            )
            val generatedId = taskDao.insertTask(task)
            syncTaskToFirestore(task.copy(id = generatedId))
            updateWidgets()
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            taskDao.updateTask(updatedTask)
            syncTaskToFirestore(updatedTask)
            if (updatedTask.isCompleted) { 
                val xpBase = 10
                val coinBase = task.rewardCoins.coerceAtLeast(5)
                
                addXp((xpBase * dopamineMultiplier.floatValue).toInt())
                addCoins((coinBase * dopamineMultiplier.floatValue).toInt())
                
                // Haftalık karne için kaydet
                updateTodayHistory(tasksDone = 1)
                
                applyDopamineBoost()
                
                // DOPAMINE REWARD
                viewModelScope.launch(Dispatchers.Main) {
                    showConfetti.value = true
                    delay(3000)
                    showConfetti.value = false
                }
            }
            updateWidgets()
        }
    }


    private fun updateWidgets() {
        viewModelScope.launch {
            try {
                com.focuspath.app.widget.TaskWidget().updateAll(application)
            } catch (e: Exception) {
                android.util.Log.e("FocusPath", "Widget update error", e)
            }
        }
    }

    fun addXp(amount: Int) { 
        val bonus = if (isTeamSynergyActive.value) (amount * 0.2f).toInt() else 0
        val finalAmount = amount + bonus
        
        userXp.value += finalAmount 
        prefs.edit().putInt("user_xp", userXp.value).apply() 
        syncXpToFirestore() 
        syncProfileToFirestore() 
        
        // Takım XP'sini de güncelle
        updateTeamXp(finalAmount)
    }

    val isTeamSynergyActive = derivedStateOf {
        val myTeamId = userTeam.value?.id
        if (myTeamId == null || !isFocusActive.value) false
        else teamMembers.any { it.email != userEmail.value && it.isFocusing }
    }

    private fun updateTeamXp(amount: Int) {
        val teamId = userTeam.value?.id ?: return
        val currentEmail = userEmail.value
        
        firestore.runTransaction { transaction ->
            val teamRef = firestore.collection("teams").document(teamId)
            val team = transaction.get(teamRef).toObject(Team::class.java) ?: return@runTransaction
            
            val newWeeklyXp = team.currentWeeklyXp + amount
            val newTotalXp = team.totalTeamXp + amount
            
            // MVP Güncelleme (Bu hafta en çok katkı sağlayan)
            // Not: Basitleştirmek için o an XP kazananı MVP adayı olarak kontrol ediyoruz
            // Gerçek MVP mantığı için üyelerin katkılarını ayrı bir map'te tutmak daha iyi olur
            // Ancak şu anki yapıda "Son büyük katkıyı yapan" veya "Lider" gibi davranabilir
            
            val updates = mutableMapOf<String, Any>(
                "currentWeeklyXp" to newWeeklyXp,
                "totalTeamXp" to newTotalXp
            )
            
            // ROZET KONTROLÜ
            val newBadges = team.badges.toMutableList()
            if (newTotalXp >= 1000 && !newBadges.contains("startup")) {
                newBadges.add("startup") // "Garaj Ruhu" Rozeti
            }
            if (newTotalXp >= 10000 && !newBadges.contains("unicorn")) {
                newBadges.add("unicorn") // "Unicorn" Rozeti
            }
            if (team.memberEmails.size >= 5 && !newBadges.contains("social")) {
                newBadges.add("social") // "Kalabalık Ekip" Rozeti
            }
            
            if (newBadges.size > team.badges.size) {
                updates["badges"] = newBadges
            }
            
            transaction.update(teamRef, updates)
        }.addOnSuccessListener {
            android.util.Log.d("FocusPathTeam", "Team XP and Badges updated")
        }
    }

    fun recordFocusSession(minutes: Int) {
        val currentFocus = prefs.getInt("DAILY_FOCUS_CURRENT", 0) ; val newFocus = currentFocus + minutes
        prefs.edit().putInt("DAILY_FOCUS_CURRENT", newFocus).apply()
        if (newFocus >= 30 && !prefs.getBoolean("DAILY_FOCUS_DONE", false)) { prefs.edit().putBoolean("DAILY_FOCUS_DONE", true).apply() ; addCoins(50) }
        if (isLoggedIn.value) { firestore.collection("users").document(userEmail.value).update("total_focus_minutes", com.google.firebase.firestore.FieldValue.increment(minutes.toLong())) }
        
        // Haftalık karne için kaydet
        updateTodayHistory(focusMins = minutes)
    }

    fun setMinimalistMode(enabled: Boolean) {
        isMinimalistMode.value = enabled
        prefs.edit().putBoolean("adhd_minimalist_mode", enabled).apply()
    }

    fun setFocusActive(active: Boolean, timerEnd: Long = 0L) {
        if (isFocusActive.value == active && timerEnd == 0L) return
        isFocusActive.value = active 
        prefs.edit().putBoolean("is_focus_active", active).apply() 
        syncXpToFirestore(isFocusing = active) 
        updateLiveFocusStatus(active)
        
        // Multi-device sync: Timer bitiş süresini Firestore'a yaz
        if (active && timerEnd > 0) {
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid)
                    .update("timer_target_end", timerEnd)
            }
        }
        
        // Periyodik canlılık güncellemesi
        liveFocusUpdateJob?.cancel()
        if (active) {
            liveFocusUpdateJob = viewModelScope.launch(Dispatchers.IO) {
                while (true) {
                    delay(30000L) // 30 saniyede bir güncelle
                    updateLiveFocusStatus(true)
                }
            }
        }
        
        updateAmbientSounds() // Ambient sesleri güncelle

        viewModelScope.launch {
            val size = workers.size
            for (i in 0 until size) {
                if (i < workers.size) {
                    workers[i] = workers[i].copy(movementCount = 0)
                    updateWorkerBehavior(i)
                }
            }
        }
    }

    fun toggleBlockedApp(packageName: String) { if (blockedApps.contains(packageName)) blockedApps.remove(packageName) else blockedApps.add(packageName) ; prefs.edit().putStringSet("blocked_apps", blockedApps.toSet()).apply() }
    fun deleteTask(task: TaskEntity) { 
        viewModelScope.launch(Dispatchers.IO) { 
            // 1. Önce yerelden sil
            taskDao.deleteTask(task) 
            // 2. Buluttan sil
            deleteTaskFromFirestore(task.id)
            
            // 3. Güvenlik önlemi: Eğer ID 0 ise bulutta "0" isimli dokümanı da silmeyi dene
            if (task.id == 0L) {
                deleteTaskFromFirestore(0L)
            }
            updateWidgets()
        } 
    }
    fun updateTask(task: TaskEntity) { viewModelScope.launch(Dispatchers.IO) { taskDao.updateTask(task) ; syncTaskToFirestore(task) } }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortType(type: Int) { _sortType.value = type }
    fun setTaskFilter(filter: Int) { _taskFilter.value = filter }
    fun setCategoryFilter(category: String) { _categoryFilter.value = category }
    fun setPriorityFilter(priority: Int) { _priorityFilter.value = priority }
    fun clearCompletedTasks() { 
        viewModelScope.launch(Dispatchers.IO) { 
            val completedTasks = taskDao.getAllTasksOnce().filter { it.isCompleted }
            taskDao.deleteCompletedTasks() 
            completedTasks.forEach { deleteTaskFromFirestore(it.id) }
        } 
    }
    fun addCoins(amount: Int) { 
        userCoins.value += amount 
        if (amount > 0) {
            lifetimeCoins.value += amount
            prefs.edit().putInt("lifetime_coins", lifetimeCoins.value).apply()
        }
        prefs.edit().putInt("user_coins", userCoins.value).apply() 
        syncProfileToFirestore() 
    }
    fun upgradeOffice() { 
        val cost = officeLevel.value * 500 
        if (userCoins.value >= cost) { 
            addCoins(-cost) 
            officeLevel.value += 1 
            prefs.edit().putInt("office_level", officeLevel.value).apply() 
            syncProfileToFirestore() 
            initializeWorkers()
        } 
    }
    fun setHapticEnabled(enabled: Boolean) { isHapticEnabled.value = enabled ; prefs.edit().putBoolean("is_haptic_enabled", enabled).apply() }
    fun setThemeColor(index: Int) { themeColorIndex.value = index ; prefs.edit().putInt("theme_color_index", index).apply() }
    fun buyPremium() { billingProvider?.startPurchaseFlow() }
    fun toggleTheme() { isDarkMode.value = !isDarkMode.value ; prefs.edit().putBoolean("is_dark_mode", isDarkMode.value).apply() }

    fun buyItem(id: String, cost: Int) {
        if (userCoins.value >= cost && !unlockedItems.contains(id)) {
            addCoins(-cost)
            unlockedItems.add(id)
            prefs.edit().putStringSet("unlocked_items", unlockedItems.toSet()).apply()
            // Hemen ofise ekle
            toggleItemVisibility(id, Offset(0f, 0f))
        }
    }

    fun loginGoogle(context: Context, idToken: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                android.util.Log.d("FocusPathAuth", "Firebase'e giriş yapılıyor...")
                val authResult = firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
                val user = authResult.user ?: throw Exception("Kullanıcı bilgisi alınamadı")
                
                android.util.Log.d("FocusPathAuth", "Giriş başarılı: ${user.email}")
                isLoggedIn.value = true 
                userEmail.value = user.email ?: "" 
                userName.value = user.displayName ?: "ANONYMOUS" 
                userPhotoUrl.value = user.photoUrl?.toString()
                prefs.edit().apply {
                    putString("user_name", userName.value)
                    putString("user_photo_url", userPhotoUrl.value)
                }.apply()
                
                syncXpToFirestore() 
                fetchLeaderboard() 
                startFriendRequestListener()
                onSuccess(user.email ?: "")
            } catch (e: Exception) { 
                android.util.Log.e("FocusPathAuth", "Firebase login error", e)
                onError(e.localizedMessage ?: "Error") 
            }
        }
    }

    fun logoutGoogle(context: Context) { 
        viewModelScope.launch { 
            friendRequestRegistration?.remove()
            firebaseAuth.signOut() 
            isLoggedIn.value = false 
            userEmail.value = "" 
            userName.value = "ANONYMOUS"
            userPhotoUrl.value = null
            prefs.edit().apply {
                remove("user_name")
                remove("user_photo_url")
            }.apply()
        } 
    }

    fun loginEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) { 
        viewModelScope.launch { 
            try { 
                android.util.Log.d("FocusPathAuth", "Email login starting for: $email")
                val result = firebaseAuth.signInWithEmailAndPassword(email, pass).await() 
                val user = result.user
                
                if (user != null) {
                    isLoggedIn.value = true 
                    userEmail.value = user.email ?: "" 
                    userName.value = user.displayName ?: user.email?.split("@")?.get(0) ?: "User"
                    userPhotoUrl.value = user.photoUrl?.toString()
                    
                    prefs.edit().apply {
                        putString("user_name", userName.value)
                        putString("user_photo_url", userPhotoUrl.value)
                    }.apply()
                    
                    android.util.Log.d("FocusPathAuth", "Email login successful: ${user.email}")
                    
                    syncXpToFirestore() 
                    fetchLeaderboard() 
                    fetchUserDataFromFirestore()
                    startFriendRequestListener()
                    onSuccess()
                } else {
                    throw Exception("Kullanıcı oluşturulamadı.")
                }
            } catch (e: Exception) { 
                android.util.Log.e("FocusPathAuth", "Email login error", e)
                val errorMessage = when {
                    e.message?.contains("password") == true || e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Hatalı e-posta veya şifre."
                    e.message?.contains("user") == true -> "Kullanıcı bulunamadı."
                    e.message?.contains("network") == true -> "Ağ hatası. İnternetinizi kontrol edin."
                    else -> e.localizedMessage ?: "Giriş yapılamadı."
                }
                onError(errorMessage)
            } 
        } 
    }

    fun registerEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) { 
        viewModelScope.launch { 
            try { 
                val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await() 
                val user = result.user
                isLoggedIn.value = true 
                userEmail.value = user?.email ?: "" 
                userName.value = user?.email?.split("@")?.get(0) ?: "User"
                userPhotoUrl.value = user?.photoUrl?.toString()

                prefs.edit().apply {
                    putString("user_name", userName.value)
                    putString("user_photo_url", userPhotoUrl.value)
                }.apply()
                
                // Yeni kullanıcı için Firestore dokümanı oluştur
                syncProfileToFirestore()
                syncXpToFirestore() 
                fetchLeaderboard() 
                startFriendRequestListener()
                onSuccess() 
            } catch (e: Exception) { 
                android.util.Log.e("FocusPathAuth", "Registration error", e)
                onError(e.localizedMessage ?: "Kayıt yapılamadı") 
            } 
        } 
    }
    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) { viewModelScope.launch { try { firebaseAuth.sendPasswordResetEmail(email).await() ; onSuccess() } catch (e: Exception) { onError(e.localizedMessage ?: "Error") } } }

    fun sendFriendRequest(targetEmail: String, targetName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUser = firebaseAuth.currentUser ?: return
        val myDisplayName = if (userName.value != "ANONYMOUS" && userName.value.isNotBlank()) userName.value else currentUser.displayName ?: "Kullanıcı"
        
        val myData = mapOf(
            "uid" to currentUser.uid,
            "name" to myDisplayName,
            "email" to (currentUser.email?.lowercase() ?: ""),
            "score" to userXp.value.toLong(),
            "photoUrl" to (userPhotoUrl.value ?: "")
        )
        
        firestore.collection("users")
            .document(targetEmail.lowercase())
            .collection("friend_requests")
            .document(currentUser.uid)
            .set(myData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "Hata oluştu") }
    }

    fun acceptFriendRequest(reqUser: LeaderboardUser, onSuccess: () -> Unit) {
        val currentUser = firebaseAuth.currentUser ?: return
        val currentEmail = currentUser.email ?: return
        val db = firestore
        val userDocRef = db.collection("users").document(currentEmail.lowercase())

        val friendData = mapOf(
            "uid" to reqUser.uid,
            "name" to reqUser.name,
            "email" to reqUser.email.lowercase(),
            "score" to reqUser.score,
            "photoUrl" to (reqUser.photoUrl ?: "")
        )

        val myDisplayName = if (userName.value != "ANONYMOUS" && userName.value.isNotBlank()) userName.value else currentUser.displayName ?: "Kullanıcı"
        val myData = mapOf(
            "uid" to currentUser.uid,
            "name" to myDisplayName,
            "email" to currentEmail.lowercase(),
            "score" to userXp.value.toLong(),
            "photoUrl" to (userPhotoUrl.value ?: "")
        )

        userDocRef.collection("friends").document(reqUser.uid).set(friendData)
            .addOnSuccessListener {
                db.collection("users").document(reqUser.email.lowercase()).collection("friends").document(currentUser.uid).set(myData)
                    .addOnSuccessListener {
                        userDocRef.collection("friend_requests").document(reqUser.uid).delete()
                        onSuccess()
                    }
            }
    }

    fun removeFriend(friendUid: String, friendEmail: String, onSuccess: () -> Unit) {
        val currentUser = firebaseAuth.currentUser ?: return
        val currentEmail = currentUser.email ?: return
        val db = firestore
        
        // 1. Kendi listemden sil
        db.collection("users").document(currentEmail.lowercase())
            .collection("friends").document(friendUid).delete()
            .addOnSuccessListener {
                // 2. Karşı tarafın listesinden beni sil
                db.collection("users").document(friendEmail.lowercase())
                    .collection("friends").document(currentUser.uid).delete()
                    .addOnSuccessListener {
                        onSuccess()
                    }
            }
    }

    private var friendsRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    fun startFriendsListener() {
        val email = userEmail.value
        if (email.isBlank()) return
        
        friendsRegistration?.remove()
        friendsRegistration = firestore.collection("users").document(email.lowercase())
            .collection("friends")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val list = snapshot?.documents?.mapNotNull { it.toObject(LeaderboardUser::class.java) } ?: emptyList()
                friendsList.clear()
                friendsList.addAll(list)
            }
    }

    private var friendRequestRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    fun startFriendRequestListener() {
        val email = userEmail.value
        if (email.isBlank()) return
        
        friendRequestRegistration?.remove()
        friendRequestRegistration = firestore.collection("users").document(email)
            .collection("friend_requests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val senderName = change.document.getString("name") ?: "Bir kullanıcı"
                        showLocalNotification("Yeni Arkadaş İsteği", "$senderName sana arkadaşlık isteği gönderdi!")
                    }
                }
            }
    }

    private fun showLocalNotification(title: String, message: String) {
        val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "focuspath_notifications"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "FocusPath Bildirimleri", android.app.NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(application, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabled = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled?.contains("${context.packageName}/com.focuspath.app.service.FocusBlockerService") == true
    }

    fun openAccessibilitySettings(context: Context) { context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }

    fun updateItemPosition(id: String, offset: Offset, isDragging: Boolean = true, floorSize: Float = 300f) {
        val collisionThreshold = 45f
        val isColliding = itemPositions.entries.any { 
            it.key != id && 
            kotlin.math.abs(it.value.x - offset.x) < collisionThreshold && 
            kotlin.math.abs(it.value.y - offset.y) < collisionThreshold 
        }

        if (!isColliding) {
            val limit = (floorSize / 2f) - 40f 
            val boundedOffset = Offset(
                offset.x.coerceIn(-limit, limit),
                offset.y.coerceIn(-limit, limit)
            )
            itemPositions[id] = boundedOffset
            if (!isDragging) { saveLayoutToPrefs("v2") }
        }
    }

    private fun saveLayoutToPrefs(slotName: String) {
        val savedString = itemPositions.entries.joinToString("|") { "${it.key}:${it.value.x},${it.value.y}" }
        val key = if (slotName == "current") "office_layout_v2" else "office_layout_$slotName"
        prefs.edit().putString(key, savedString).apply()
    }

    fun toggleItemVisibility(id: String, defaultPos: Offset = Offset(0f, 0f)) {
        if (visibleItems.contains(id)) {
            visibleItems.remove(id)
        } else {
            visibleItems.add(id)
            if (!itemPositions.containsKey(id)) {
                itemPositions[id] = defaultPos
            }
        }
        prefs.edit().putStringSet("visible_items", visibleItems.toSet()).apply()
        saveLayoutToPrefs("current")
        
        // Masa ekleme/çıkarma yapıldığında çalışanları güncelle
        if (id.startsWith("desk_setup_")) {
            initializeWorkers()
        }
    }

    fun resetOfficePositions() { 
        currentLayoutName.value = "Standart Ofis"
        itemPositions.clear() 
        visibleItems.clear()
        
        val defaultDesks = listOf(
            -90f to 0f, 0f to 0f, 90f to 0f
        )
        val deskCount = 3

        defaultDesks.forEachIndexed { i, (x, y) ->
            val deskId = "desk_setup_$i"
            itemPositions[deskId] = Offset(x, y)
            visibleItems.add(deskId)
        }
        
        prefs.edit().putStringSet("visible_items", visibleItems.toSet()).apply()
        saveLayoutToPrefs("v2")
    }

    fun saveCurrentLayout(slotName: String) { saveLayoutToPrefs("current") }
    fun loadLayout(slotName: String) {}

    fun setNotificationEnabled(enabled: Boolean) { isNotificationEnabled.value = enabled ; prefs.edit().putBoolean("is_notification_enabled", enabled).apply() }
    fun setAlarmSound(sound: String) { alarmSound.value = sound ; prefs.edit().putString("alarm_sound", sound).apply() }
    fun setAlarmVolume(volume: Float) { alarmVolume.floatValue = volume ; prefs.edit().putFloat("alarm_volume", volume).apply() }
    fun setAutoDndEnabled(context: android.content.Context, enabled: Boolean) {
        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (enabled && !nm.isNotificationPolicyAccessGranted) { context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) ; return }
        isAutoDndEnabled.value = enabled ; prefs.edit().putBoolean("is_auto_dnd_enabled", enabled).apply()
    }

    fun getInstalledApps(): List<AppInfo> {
        val pm = application.packageManager
        val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
        return apps.filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }.map { AppInfo(name = it.loadLabel(pm).toString(), packageName = it.packageName, icon = it.loadIcon(pm)) }.sortedBy { it.name }
    }
}

data class DirectMessage(
    val id: String = "",
    val from: String = "",
    val fromName: String = "",
    val to: String = "",
    val toName: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

data class AppInfo(val name: String, val packageName: String, val icon: android.graphics.drawable.Drawable)
