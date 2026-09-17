package com.focuspath.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.MediaPlayer
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
import com.focuspath.app.core.data.local.TaskDao
import com.focuspath.app.core.data.local.TaskEntity
import com.focuspath.app.core.data.local.HabitEntity
import com.focuspath.app.core.data.local.FocusHistoryEntity
import com.focuspath.shared.model.LeaderboardUser
import com.focuspath.app.data.model.Team
import com.focuspath.app.data.remote.FocusPathApiService
import com.focuspath.app.data.remote.TaskSyncRequest
import com.focuspath.app.data.remote.UserSyncRequest
import com.focuspath.app.data.remote.TaskDto
import com.focuspath.app.core.domain.usecase.GetFocusRankUseCase
import com.focuspath.app.core.domain.usecase.CalculateDopamineUseCase
import com.focuspath.app.core.domain.repository.TaskRepository
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
import com.google.gson.Gson
import java.text.SimpleDateFormat
import javax.inject.Inject
import com.focuspath.app.billing.BillingProvider
import com.focuspath.app.service.FocusService
import com.focuspath.app.util.ReminderUtil
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

data class OnboardingTask(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val rewardCoins: Int,
    val rewardXp: Int,
    var isCompleted: Boolean = false
)

data class MysteryBoxReward(
    val type: String, // "COINS", "PLANT_XP", "ITEM"
    val amount: Int = 0,
    val itemId: String? = null,
    val title: String,
    val icon: String
)

data class RadioStation(
    val name: String,
    val url: String,
    val icon: String,
    val isLocal: Boolean = false,
    val resId: Int = 0
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
    private val storage: FirebaseStorage,
    private val getFocusRankUseCase: GetFocusRankUseCase,
    private val calculateDopamineUseCase: CalculateDopamineUseCase,
    private val taskRepository: TaskRepository
) : ViewModel() {

    // --- PROPERTY DEFINITIONS ---

    var billingProvider: BillingProvider? = null

    val isLoggedIn = mutableStateOf(false)
    val userEmail = mutableStateOf("")
    val userName = mutableStateOf(prefs.getString("user_name", "ANONYMOUS") ?: "ANONYMOUS")
    val userPhotoUrl = mutableStateOf<String?>(prefs.getString("user_photo_url", null))
    private val _localUserXp = MutableStateFlow(prefs.getInt("user_xp", 0).toLong())
    private val _localUserPhoto = MutableStateFlow(prefs.getString("user_photo_url", null))
    private val _localUserName = MutableStateFlow(prefs.getString("user_name", "ANONYMOUS") ?: "ANONYMOUS")
    val userStreak = mutableStateOf(5)
    val userCoins = mutableStateOf(prefs.getInt("user_coins", 0))
    val lifetimeCoins = mutableStateOf(prefs.getInt("lifetime_coins", 0))
    val officeLevel = mutableStateOf(prefs.getInt("office_level", 1))
    val dailyFocusMinutes = mutableIntStateOf(prefs.getInt("DAILY_FOCUS_CURRENT", 0))
    val totalFocusMinutesCloud = mutableIntStateOf(prefs.getInt("total_focus_minutes_cloud", 0))
    val blockedApps = mutableStateListOf<String>().apply {
        addAll(prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet())
    }
    val isFocusActive = mutableStateOf(false)
    
    val timerRunning = mutableStateOf(false)
    val isTimerPaused = mutableStateOf(false)
    val timeLeft = mutableLongStateOf(25 * 60 * 1000L)
    val timeElapsed = mutableLongStateOf(0L)
    val pomodoroTotalMillis = mutableLongStateOf(25 * 60 * 1000L)
    val isPomodoroMode = mutableStateOf(true)
    private var timerSyncJob: kotlinx.coroutines.Job? = null

    val brainDumpNotes = mutableStateListOf<String>()
    val selectedFocusBuddy = mutableStateOf<WorkerInfo?>(null)
    val buddyCurrentTask = mutableStateOf<String?>(null)

    val isMinimalistMode = mutableStateOf(prefs.getBoolean("adhd_minimalist_mode", false))
    val isIntervalChimeEnabled = mutableStateOf(prefs.getBoolean("adhd_interval_chime", false))
    val intervalMinutes = mutableIntStateOf(prefs.getInt("adhd_interval_minutes", 15))
    val lastChimeTime = mutableLongStateOf(0L)
    
    val selectedTaskEnergy = mutableIntStateOf(1)
    val isDecisionSpinnerActive = mutableStateOf(false)
    val spinnerResult = mutableStateOf<TaskEntity?>(null)
    val spinnerAction = mutableStateOf<String?>(null)

    val isSlicingTask = mutableStateOf(false)
    val slicedTasks = mutableStateListOf<String>()

    val isMonotasking = mutableStateOf(false)
    val monotask = mutableStateOf<TaskEntity?>(null)
    val showConfetti = mutableStateOf(false)

    val allHabits = taskDao.getAllHabits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distractionLog = mutableStateListOf<String>()
    val distractionAnalysis = mutableStateOf<String?>(null)
    val isAnalyzingDistractions = mutableStateOf(false)
    val isUploadingProfile = mutableStateOf(false)
    val dopamineMultiplier = mutableFloatStateOf(1.0f)
    val currentTaskEstimation = mutableIntStateOf(0)

    private var presenceHeartbeatJob: kotlinx.coroutines.Job? = null

    val isLeaderboardLoading = mutableStateOf(false)
    val onboardingTasks = mutableStateListOf<OnboardingTask>()

    val plantLevel = mutableIntStateOf(prefs.getInt("plant_level", 1))
    val plantGrowthXp = mutableIntStateOf(prefs.getInt("plant_growth_xp", 0))
    val lastPlantCheckTime = mutableLongStateOf(prefs.getLong("last_plant_check", System.currentTimeMillis()))
    
    private val _teamLeaderboard = MutableStateFlow<List<Team>>(emptyList())
    val teamLeaderboard: StateFlow<List<Team>> = combine(
        _teamLeaderboard,
        snapshotFlow { userTeam.value }
    ) { remoteTeams, localTeam ->
        val list = remoteTeams.toMutableList()
        localTeam?.let { lt ->
            if (list.none { it.id == lt.id }) {
                list.add(lt)
            }
        }
        list.sortedByDescending { it.totalTeamXp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val userTeam = mutableStateOf<Team?>(null)
    val teamMembers = mutableStateListOf<LeaderboardUser>()
    val isTeamLoading = mutableStateOf(false)
    val isTeamOfisMode = mutableStateOf(prefs.getBoolean("is_team_office_mode", false))
    private var teamListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var teamMembersListener: com.google.firebase.firestore.ListenerRegistration? = null

    val showMysteryBox = mutableStateOf(false)
    val mysteryBoxReward = mutableStateOf<MysteryBoxReward?>(null)
    val showCoffeeBreak = mutableStateOf(false)

    val dailyBriefingText = mutableStateOf<String?>(null)
    val isBriefingLoading = mutableStateOf(false)
    val showDailyBriefing = mutableStateOf(false)
    val showMindfulnessBreak = mutableStateOf(false)
    
    val yesterdayFocusMins = mutableIntStateOf(0)
    val todayChallengeTarget = mutableIntStateOf(0)

    val aiHistoryInsight = mutableStateOf<String?>(null)
    val isAnalyzingHistory = mutableStateOf(false)

    val dopamineMenu = mutableStateListOf<DopamineItem>()
    val showDopamineMenu = mutableStateOf(false)

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val todayStr: String get() = synchronized(sdf) { sdf.format(Date()) }

    val isWaterReminderEnabled = mutableStateOf(prefs.getBoolean("is_water_reminder_enabled", false))
    val waterReminderInterval = mutableIntStateOf(prefs.getInt("water_reminder_interval", 1))
    val waterCupsDrunk = mutableIntStateOf(0)
    val isWaterRewardAvailable = mutableStateOf(false)
    private var lastWaterRewardTime = prefs.getLong("last_water_reward_time", 0L)
    private var lastWaterReminderTime = prefs.getLong("last_water_reminder_time", 0L)

    val todayStats = getWeeklyHistory().map { list ->
        val now = todayStr
        val found = list.find { it.date == now }
        android.util.Log.d("FocusPathStats", "todayStats sync: ${if (found != null) "Found ${found.sessionsCompleted} sessions" else "No data for $now"}")
        found
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isRecoverMode = mutableStateOf(false)
    val hasUpdate = mutableStateOf(false)
    val updateNotes = mutableStateOf("")
    val updateVersionName = mutableStateOf("")

    val unlockedItems = mutableStateListOf<String>().apply {
        addAll(prefs.getStringSet("unlocked_items", setOf("desk_1", "pc_1")) ?: setOf("desk_1", "pc_1"))
    }

    val visibleItems = mutableStateListOf<String>().apply {
        val saved = prefs.getStringSet("visible_items", null)
        if (saved != null) {
            addAll(saved)
        } else {
            addAll(listOf("desk_setup_0"))
        }
    }

    private val _leaderboard = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardUser>> = combine(
        _leaderboard,
        _localUserXp,
        _localUserPhoto,
        _localUserName
    ) { users, localXp, localPhoto, localName ->
        try {
            val currentUid = firebaseAuth.currentUser?.uid
            val currentEmail = userEmail.value
            
            var foundMe = false
            val mappedUsers = users.map { user ->
                val isMe = (currentUid != null && user.uid == currentUid) || 
                           (user.email.isNotBlank() && user.email.equals(currentEmail, ignoreCase = true))
                
                if (isMe) {
                    foundMe = true
                    user.copy(photoUrl = localPhoto, score = localXp, name = localName)
                } else {
                    user
                }
            }.toMutableList()

            if (!foundMe && currentUid != null) {
                mappedUsers.add(LeaderboardUser(
                    uid = currentUid,
                    name = localName,
                    email = currentEmail,
                    score = localXp,
                    photoUrl = localPhoto
                ))
            }

            mappedUsers.sortedByDescending { it.score }
        } catch (e: Exception) {
            users
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val workers = mutableStateListOf<WorkerInfo>()
    val friendsList = mutableStateListOf<LeaderboardUser>()
    private val liveUsers = mutableStateMapOf<String, WorkerInfo>()
    val workerDetails = mutableStateOf<WorkerInfo?>(null)

    private var soundPool: SoundPool? = null
    private var keyboardSoundId: Int = 0
    private var mouseSoundId: Int = 0
    private var dragonSoundId: Int = 0
    private var keyboardStreamId: Int = 0
    private var mouseStreamId: Int = 0

    private var rainPlayer: MediaPlayer? = null
    private var fireplacePlayer: MediaPlayer? = null
    private var radioPlayer: MediaPlayer? = null
    private var radioJob: kotlinx.coroutines.Job? = null

    val isRainEnabled = mutableStateOf(prefs.getBoolean("is_rain_enabled", false))
    val isFireplaceEnabled = mutableStateOf(prefs.getBoolean("is_fireplace_enabled", false))
    
    val currentRadioStation = mutableStateOf<RadioStation?>(null)
    val isRadioPlaying = mutableStateOf(false)
    val isRadioLoading = mutableStateOf(false)

    private val simulatedBots = mutableStateListOf<LeaderboardUser>()
    private var botSimulationJob: kotlinx.coroutines.Job? = null

    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
        when (key) {
            "user_xp" -> {
                val newXp = sharedPrefs.getInt("user_xp", userXp.value)
                if (userXp.value != newXp) {
                    userXp.value = newXp
                    android.util.Log.d("FocusPathStats", "XP Synced from Prefs: $newXp")
                }
            }
            "user_coins" -> {
                val newCoins = sharedPrefs.getInt("user_coins", userCoins.value)
                if (userCoins.value != newCoins) {
                    userCoins.value = newCoins
                    android.util.Log.d("FocusPathStats", "Coins Synced from Prefs: $newCoins")
                }
            }
            "last_water_reminder_time" -> {
                val lastTime = sharedPrefs.getLong("last_water_reminder_time", 0L)
                val now = System.currentTimeMillis()
                isWaterRewardAvailable.value = lastTime > 0 && (now - lastTime < 2 * 60 * 60 * 1000L)
            }
            "office_level" -> officeLevel.value = sharedPrefs.getInt("office_level", officeLevel.value)
            "is_premium" -> isPremium.value = sharedPrefs.getBoolean("is_premium", isPremium.value)
        }
    }

    val radioStations = listOf(
        RadioStation("Lofi Focus", "", "🎧", isLocal = true, resId = com.focuspath.app.R.raw.lofi),
        RadioStation("Chill Out", "", "☕", isLocal = true, resId = com.focuspath.app.R.raw.chill),
        RadioStation("Deep Zen", "", "🧠", isLocal = true, resId = com.focuspath.app.R.raw.zen),
        RadioStation("Rainy Mood", "", "🌧️", isLocal = true, resId = com.focuspath.app.R.raw.rain),
        RadioStation("Fireplace", "", "🔥", isLocal = true, resId = com.focuspath.app.R.raw.fireplace)
    )

    private var liveFocusRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var friendsRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var friendRequestRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var lastLiveFocusUpdate = 0L

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
        allTasks.distinctUntilChanged(), _searchQuery, _sortType, _taskFilter, _categoryFilter, _priorityFilter, _selectedDate
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
            val isMainTask = task.parentId == 0L
            matchesSearch && matchesFilter && matchesCategory && matchesPriority && matchesDate && isMainTask
        }
        when (sort) {
            1 -> filtered.sortedByDescending { it.priority }
            2 -> filtered.sortedBy { it.title.lowercase() }
            3 -> filtered.sortedBy { it.isCompleted }
            else -> filtered
        }
    }

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
    val selectedChatUserEmail = mutableStateOf<String?>(null)
    
    private val _directMessages = mutableStateListOf<DirectMessage>()
    val directMessages: List<DirectMessage> get() = _directMessages.sortedBy { it.timestamp }

    private var messageIncomingRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var messageOutgoingRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var chatSession = generativeModel.startChat()
    
    val isTeamSynergyActive = derivedStateOf {
        val myTeamId = userTeam.value?.id
        if (myTeamId == null || !isFocusActive.value) false
        else teamMembers.any { it.email != userEmail.value && it.isFocusing }
    }

    val isBreakingTask = mutableStateOf(false)

    // --- MEMBER FUNCTIONS ---

    fun drinkWater() {
        val now = System.currentTimeMillis()
        val reminderWindow = 2 * 60 * 60 * 1000L
        val dailyLimit = 8

        if (waterCupsDrunk.intValue >= dailyLimit) {
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(application, "Günlük hedefine zaten ulaştın! (8/8) 💧", Toast.LENGTH_SHORT).show()
            }
            return
        }

        lastWaterReminderTime = prefs.getLong("last_water_reminder_time", 0L)

        waterCupsDrunk.intValue += 1
        prefs.edit().putInt("water_cups_drunk_$todayStr", waterCupsDrunk.intValue).apply()
        playWaterSound()

        if (lastWaterReminderTime > 0 && now - lastWaterReminderTime <= reminderWindow) {
            prefs.edit().putLong("last_water_reminder_time", 0L).apply()
            lastWaterReminderTime = 0
            isWaterRewardAvailable.value = false

            lastWaterRewardTime = now
            prefs.edit().putLong("last_water_reward_time", now).apply()

            addCoins(5)
            addXp(5)

            viewModelScope.launch(Dispatchers.Main) {
                showConfetti.value = true
                delay(2000)
                showConfetti.value = false
            }
        } else {
            viewModelScope.launch(Dispatchers.Main) {
                val message = if (lastWaterReminderTime == 0L) 
                    "Su içtin! 💧 (Hatırlatıcı bekleyerek coin kazanabilirsin)" 
                else 
                    "Hatırlatıcı süresi dolmuş! 💧"
                Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playWaterSound() {
        playTickSound()
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val soundUrl = "https://assets.mixkit.co/active_storage/sfx/2571/2571-preview.mp3"
                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                )
                mp.setDataSource(soundUrl)
                mp.setOnPreparedListener { it.start() }
                mp.setOnCompletionListener { it.release() }
                mp.setOnErrorListener { player, _, _ ->
                    player.release()
                    true
                }
                mp.prepareAsync()
            } catch (e: Exception) {
                android.util.Log.e("FocusPathWater", "Sound setup error: ${e.message}")
            }
        }
    }

    fun checkAndGenerateBriefing(isEnglish: Boolean) {
        val lastDate = prefs.getString("last_briefing_date", "")
        if (lastDate == todayStr) return

        viewModelScope.launch {
            showDailyBriefing.value = true
            isBriefingLoading.value = true
            try {
                val tasks = taskDao.getAllTasksOnce().filter { isSameDay(it.dueDate, System.currentTimeMillis()) && !it.isCompleted && it.parentId == 0L }
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
        val start = synchronized(sdf) { sdf.format(cal.time) }
        return taskDao.getFocusHistory(start)
    }

    fun analyzeFocusHistoryWithAi(isEnglish: Boolean) {
        if (isAnalyzingHistory.value) return
        isAnalyzingHistory.value = true
        
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -14) }
                val start = synchronized(sdf) { sdf.format(cal.time) }
                val history = taskDao.getFocusHistoryOnce(start)
                
                if (history.isEmpty()) {
                    aiHistoryInsight.value = if (isEnglish) "Not enough data for analysis yet." else "Analiz için henüz yeterli veri yok."
                    return@launch
                }

                val historyStr = history.joinToString("\n") { 
                    "Date: ${it.date}, Focus: ${it.totalFocusMinutes}m, Tasks: ${it.tasksCompleted}, Sessions: ${it.sessionsCompleted}, Interrupted: ${it.sessionsInterrupted}"
                }

                val prompt = if (isEnglish) {
                    "As a productivity coach for someone with ADHD, analyze this 14-day focus history and give 3 specific, motivating, and actionable insights to improve their performance:\n$historyStr\nBe concise and friendly."
                } else {
                    "ADHD odaklı bir verimlilik koçu olarak, şu 14 günlük odaklanma geçmişini analiz et ve performansı artırmak için 3 tane somut, motive edici ve uygulanabilir tavsiye ver:\n$historyStr\nKısa ve samimi ol."
                }

                val response = withContext(Dispatchers.IO) { generativeModel.generateContent(prompt) }
                aiHistoryInsight.value = response.text ?: (if(isEnglish) "Keep up the good work!" else "Harika çalışmaya devam et!")
            } catch (e: Exception) {
                android.util.Log.e("FocusPathAI", "Analysis error: ${e.message}")
                aiHistoryInsight.value = if(isEnglish) "Unable to connect to coach." else "Koç ile bağlantı kurulamadı."
            } finally {
                isAnalyzingHistory.value = false
            }
        }
    }

    private fun updateTodayHistory(focusMins: Int = 0, tasksDone: Int = 0, sessionsComp: Int = 0, sessionsInt: Int = 0) {
        val dateToUpdate = todayStr 
        android.util.Log.d("FocusPathStats", "Updating history for $dateToUpdate: mins=$focusMins, tasks=$tasksDone, sessions=$sessionsComp")
        
        viewModelScope.launch(Dispatchers.IO) {
            val updated = taskDao.updateFocusHistoryAtomic(dateToUpdate, focusMins, tasksDone, sessionsComp, sessionsInt)
            if (updated == 0) {
                android.util.Log.d("FocusPathStats", "Row not found, inserting new row for $dateToUpdate")
                taskDao.insertFocusHistory(FocusHistoryEntity(
                    date = dateToUpdate,
                    totalFocusMinutes = focusMins,
                    tasksCompleted = tasksDone,
                    sessionsCompleted = sessionsComp,
                    sessionsInterrupted = sessionsInt
                ))
            } else {
                android.util.Log.d("FocusPathStats", "Atomic update successful for $dateToUpdate")
            }
        }
    }

    fun startRecoverySession(context: Context) {
        isRecoverMode.value = true
        isPomodoroMode.value = true
        pomodoroTotalMillis.longValue = 5 * 60000L
        timeLeft.longValue = 5 * 60000L
        toggleTimer(context, true)
        
        Toast.makeText(application, "Telafi Modu: 5 Dakika Odaklan!", Toast.LENGTH_SHORT).show()
    }

    fun recoverSession() {
        val date = todayStr
        viewModelScope.launch(Dispatchers.IO) {
            val updated = taskDao.recoverInterruptedSession(date)
            if (updated > 0) {
                addXp(10)
                addCoins(10)
                
                viewModelScope.launch(Dispatchers.Main) {
                    isRecoverMode.value = false
                    Toast.makeText(application, "Seans Başarıyla Telafi Edildi! 🛡️", Toast.LENGTH_SHORT).show()
                    playTickSound()
                    showConfetti.value = true
                    delay(3000)
                    showConfetti.value = false
                }
            }
        }
    }

    fun recordSessionResult(completed: Boolean) {
        if (!timerRunning.value && !isTimerPaused.value && !completed) return
        
        android.util.Log.d("FocusPathStats", "recordSessionResult entry: completed=$completed")
        
        timerRunning.value = false
        isTimerPaused.value = false
        setFocusActive(false)

        if (completed) {
            if (isRecoverMode.value) {
                recoverSession()
                return
            }

            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(application, "Seans Tamamlandı! 🎉 +1", Toast.LENGTH_SHORT).show()
                playTickSound()
            }

            val activeMonotask = monotask.value
            updateTodayHistory(sessionsComp = 1)
            
            if (isPomodoroMode.value) {
                addXp(10)
                addCoins(10)
                completeOnboardingTask("first_focus")

                val totalMins = (pomodoroTotalMillis.longValue / 60000).toInt()
                if (totalMins >= 20 || (0..100).random() < totalMins * 4) {
                    generateMysteryBox()
                } else {
                    showMindfulnessBreak.value = true
                }

                viewModelScope.launch(Dispatchers.Main) {
                    showConfetti.value = true
                    delay(4000)
                    showConfetti.value = false
                }
            }

            if (activeMonotask != null && !activeMonotask.isCompleted) {
                toggleTask(activeMonotask)
            } else {
                updateTodayHistory(tasksDone = 1)
            }
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

    fun logDistraction(reason: String) {
        distractionLog.add(reason)
        dopamineMultiplier.floatValue = calculateDopamineUseCase.calculateNewMultiplierAfterDistraction(dopamineMultiplier.floatValue)
    }

    fun applyDopamineBoost() {
        dopamineMultiplier.floatValue = calculateDopamineUseCase.calculateNewMultiplierAfterBoost(dopamineMultiplier.floatValue)
    }

    fun updateActualTaskTime(taskId: Long, minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getAllTasksOnce().find { it.id == taskId }
            if (task != null) {
                taskDao.insertTask(task.copy(actualMinutes = task.actualMinutes + minutes))
            }
        }
    }

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
                
                val text = response.text
                if (text.isNullOrBlank()) {
                    throw Exception(if(isEnglish) "AI returned an empty response" else "AI boş cevap döndürdü")
                }
                
                text.lines().filter { it.isNotBlank() }.take(5).forEach {
                    slicedTasks.add(it.trim().removePrefix("-").removePrefix("•").trim())
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusPathAI", "Slice error: ${e.message}")
                val msg = e.toString()
                val errorMsg = when {
                    msg.contains("401") || msg.contains("API_KEY_INVALID") -> 
                        if(isEnglish) "Invalid AI Key. Check local.properties" else "AI Anahtarı Geçersiz. local.properties dosyasını kontrol edin."
                    else -> e.localizedMessage ?: "AI Error"
                }
                slicedTasks.add("ERROR: $errorMsg")
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
        
        spinnerAction.value = actionPhrases.getOrNull(finalActionIndex % actionPhrases.size)
        spinnerResult.value = tasks.random()

        viewModelScope.launch {
            isDecisionSpinnerActive.value = true
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

    private fun startUserEnvironment(user: com.google.firebase.auth.FirebaseUser) {
        isLoggedIn.value = true
        userEmail.value = user.email?.lowercase() ?: ""

        if (userName.value == "ANONYMOUS") {
            userName.value = user.displayName ?: "ANONYMOUS"
            _localUserName.value = userName.value
        }
        if (userPhotoUrl.value == null) {
            userPhotoUrl.value = user.photoUrl?.toString()
        }

        fetchUserDataFromFirestore()
        syncWithBackend()

        viewModelScope.launch {
            delay(2500)
            fetchLeaderboard()
            fetchUserTeam()
            syncXpToFirestore()
            startFriendRequestListener()
            startFriendsListener()
            startLiveFocusListener()
            startPresenceHeartbeat()
        }
    }

    fun syncWithBackend() {
        val user = firebaseAuth.currentUser ?: return
        val email = user.email?.lowercase() ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userRequest = UserSyncRequest(
                    email = email,
                    username = userName.value,
                    xp = userXp.value.toLong(),
                    coins = userCoins.value,
                    level = officeLevel.value,
                    photoUrl = userPhotoUrl.value
                )
                apiService.syncUser(userRequest)

                val localTasks = taskDao.getAllTasksOnce()
                val taskDtos = localTasks.map { entity ->
                    TaskDto(
                        id = entity.id,
                        title = entity.title,
                        notes = entity.notes,
                        category = entity.category,
                        priority = entity.priority,
                        isCompleted = entity.isCompleted,
                        dueDate = entity.dueDate,
                        parentId = entity.parentId
                    )
                }
                val taskRequest = TaskSyncRequest(email = email, tasks = taskDtos)
                val response = apiService.syncTasks(taskRequest)
                
                if (response.isSuccessful) {
                    android.util.Log.d("FocusPathBackend", "Backend sync successful")
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusPathBackend", "Backend sync failed: ${e.message}")
            }
        }
    }

    private fun checkRemoteUpdate() {
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

    private fun autoLogin() {
        val savedEmail = prefs.getString("saved_email", null)
        val savedPass = prefs.getString("saved_password", null)
        val rememberMe = prefs.getBoolean("remember_me", false)
        
        if (rememberMe && !savedEmail.isNullOrBlank() && !savedPass.isNullOrBlank() && firebaseAuth.currentUser == null) {
            loginEmail(savedEmail, savedPass, true, {}, {})
        }
    }

    private fun loadDopamineMenu() {
        dopamineMenu.clear()
        dopamineMenu.addAll(listOf(
            DopamineItem(titleTr = "Derin nefes al (2 dk)", titleEn = "Deep breathing (2 min)", category = "Appetizer", icon = "🌬️", actionType = "BREATHING"),
            DopamineItem(titleTr = "Pencereden dışarı bak", titleEn = "Look out the window", category = "Appetizer", icon = "🪟"),
            DopamineItem(titleTr = "Kısa bir yürüyüş", titleEn = "Take a short walk", category = "Main", icon = "🚶"),
            DopamineItem(titleTr = "10 sayfa kitap oku", titleEn = "Read 10 pages", category = "Main", icon = "📚"),
            DopamineItem(titleTr = "Masayı topla", titleEn = "Tidy up your desk", category = "Main", icon = "🧹"),
            DopamineItem(titleTr = "En sevdiğin şarkıyı dinle", titleEn = "Listen to your favorite song", category = "Dessert", icon = "🎵"),
            DopamineItem(titleTr = "Bir meyve ye", titleEn = "Eat a piece of fruit", category = "Dessert", icon = "🍎")
        ))
    }

    fun completeDopamineActivity(item: DopamineItem, isEnglish: Boolean, context: Context) {
        viewModelScope.launch {
            if (item.actionType == "BREATHING") {
                isPomodoroMode.value = true
                pomodoroTotalMillis.longValue = 2 * 60000L
                timeLeft.longValue = 2 * 60000L
                toggleTimer(context, true)
            }
            showDopamineMenu.value = false
            val msg = if(isEnglish) "Enjoy your '${item.titleEn}' break!" else "'${item.titleTr}' molasının tadını çıkar!"
            Toast.makeText(application, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchYesterdayStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(cal.time)
            val history = taskDao.getFocusHistoryByDate(yesterdayStr)
            
            yesterdayFocusMins.intValue = history?.totalFocusMinutes ?: 0
            todayChallengeTarget.intValue = yesterdayFocusMins.intValue + 1
        }
    }

    private fun checkPlantHealth() {
        val now = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000L
        val diff = now - lastPlantCheckTime.longValue
        
        if (diff > 2 * dayInMillis) {
            val levelsToDrop = (diff / (2 * dayInMillis)).toInt()
            plantLevel.intValue = (plantLevel.intValue - levelsToDrop).coerceAtLeast(1)
            prefs.edit().putInt("plant_level", plantLevel.intValue).apply()
        }
        lastPlantCheckTime.longValue = now
        prefs.edit().putLong("last_plant_check", now).apply()
    }

    private fun growPlant(minutes: Int) {
        val growthAmount = minutes
        plantGrowthXp.intValue += growthAmount
        
        val xpNeeded = plantLevel.intValue * 100
        if (plantGrowthXp.intValue >= xpNeeded) {
            plantGrowthXp.intValue -= xpNeeded
            plantLevel.intValue = (plantLevel.intValue + 1).coerceAtMost(10)
            prefs.edit().putInt("plant_level", plantLevel.intValue).apply()
        }
        prefs.edit().putInt("plant_growth_xp", plantGrowthXp.intValue).apply()
        syncProfileToFirestore()
    }

    fun generateMysteryBox() {
        val rand = (0..100).random()
        val reward = when {
            rand < 50 -> MysteryBoxReward("COINS", 50, null, "50 Altın", "💰")
            rand < 80 -> MysteryBoxReward("COINS", 150, null, "150 Altın", "💰")
            rand < 95 -> MysteryBoxReward("PLANT_XP", 200, null, "Bitki Süper Gübresi", "🧪")
            else -> {
                val items = listOf("lava_lamp_1", "arcade_1", "cat_1", "robot_1", "neon_sign_1")
                val item = items.random()
                MysteryBoxReward("ITEM", 0, item, "Nadir Eşya: $item", "🎁")
            }
        }
        mysteryBoxReward.value = reward
        showMysteryBox.value = true
    }

    fun claimMysteryBoxReward() {
        val reward = mysteryBoxReward.value ?: return
        when (reward.type) {
            "COINS" -> addCoins(reward.amount)
            "PLANT_XP" -> {
                plantGrowthXp.intValue += reward.amount
                val xpNeeded = plantLevel.intValue * 100
                if (plantGrowthXp.intValue >= xpNeeded) {
                    plantGrowthXp.intValue -= xpNeeded
                    plantLevel.intValue = (plantLevel.intValue + 1).coerceAtMost(10)
                }
                prefs.edit().putInt("plant_growth_xp", plantGrowthXp.intValue).apply()
                prefs.edit().putInt("plant_level", plantLevel.intValue).apply()
            }
            "ITEM" -> {
                reward.itemId?.let { id ->
                    if (!unlockedItems.contains(id)) {
                        unlockedItems.add(id)
                        prefs.edit().putStringSet("unlocked_items", unlockedItems.toSet()).apply()
                    }
                }
            }
        }
        showMysteryBox.value = false
        mysteryBoxReward.value = null
    }

    private fun loadOnboardingTasks() {
        val tasks = listOf(
            OnboardingTask("profile_pic", "Profil fotoğrafı yükle", "Upload profile picture", 20, 50),
            OnboardingTask("first_focus", "İlk 5 dakikalık odaklanmanı yap", "Complete first 5-min focus", 30, 100),
            OnboardingTask("add_friend", "Bir arkadaş ekle", "Add a friend", 25, 75),
            OnboardingTask("join_team", "Bir takıma katıl veya oluştur", "Join or create a team", 50, 150)
        )
        
        tasks.forEach { task ->
            task.isCompleted = prefs.getBoolean("onboarding_${task.id}", false)
        }
        
        onboardingTasks.clear()
        onboardingTasks.addAll(tasks)
    }

    fun completeOnboardingTask(id: String) {
        val idx = onboardingTasks.indexOfFirst { it.id == id }
        if (idx == -1) return
        
        val task = onboardingTasks[idx]
        val alreadyDoneInPrefs = prefs.getBoolean("onboarding_$id", false)
        
        if (task.isCompleted && alreadyDoneInPrefs) return

        if (!alreadyDoneInPrefs) {
            prefs.edit().putBoolean("onboarding_$id", true).apply()
            addCoins(task.rewardCoins)
            addXp(task.rewardXp)
        }
        
        if (!task.isCompleted) {
            onboardingTasks[idx] = task.copy(isCompleted = true)
        }
        
        if (onboardingTasks.all { it.isCompleted }) {
            viewModelScope.launch(Dispatchers.Main) {
                playTickSound()
                showConfetti.value = true
                delay(3000)
                showConfetti.value = false
            }
        }
    }

    fun updateUserName(newName: String) {
        if (newName.isNotBlank()) {
            userName.value = newName
            _localUserName.value = newName
            prefs.edit().putString("user_name", newName).apply()
            syncProfileToFirestore()
            syncXpToFirestore()
        }
    }

    fun updateProfilePicture(uri: android.net.Uri) {
        val user = firebaseAuth.currentUser
        val userId = user?.uid ?: "local_user"
        
        viewModelScope.launch {
            try {
                val localPath = withContext(Dispatchers.IO) {
                    val localFile = java.io.File(application.filesDir, "profile_pic_$userId.jpg")
                    val inputStream = application.contentResolver.openInputStream(uri)
                    val outputStream = java.io.FileOutputStream(localFile)
                    inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
                    localFile.absolutePath
                }

                val displayPath = "file://$localPath?t=${System.currentTimeMillis()}"
                withContext(Dispatchers.Main) {
                    userPhotoUrl.value = displayPath
                    _localUserPhoto.value = displayPath
                    initializeWorkers()
                    Toast.makeText(application, "Profil fotoğrafı güncellendi.", Toast.LENGTH_SHORT).show()
                    completeOnboardingTask("profile_pic")
                }
                
                prefs.edit()
                    .putString("user_photo_url", displayPath)
                    .putLong("profile_last_local_update", System.currentTimeMillis())
                    .apply()

                if (user != null) {
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
                                val finalPhotoUrl = "${downloadUrl}?t=${System.currentTimeMillis()}"
                                
                                syncXpToFirestore(forcedPhotoUrl = finalPhotoUrl) 
                                if (isFocusActive.value) {
                                    updateLiveFocusStatus(active = true, forcedPhotoUrl = finalPhotoUrl)
                                }

                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setPhotoUri(android.net.Uri.parse(finalPhotoUrl))
                                    .build()
                                user.updateProfile(profileUpdates).await()

                                withContext(Dispatchers.Main) {
                                    userPhotoUrl.value = finalPhotoUrl
                                    _localUserPhoto.value = finalPhotoUrl
                                    syncProfileToFirestore()
                                }
                            }
                        } finally {
                            withContext(Dispatchers.Main) { isUploadingProfile.value = false }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusPathAuth", "Profil güncelleme hatası: ${e.message}")
            }
        }
    }

    private fun syncTimerWithService() {
        val targetEnd = prefs.getLong("TIMER_TARGET_END", 0L)
        val isServiceRunning = FocusService.isRunning
        val hasActiveSession = targetEnd > System.currentTimeMillis() && isServiceRunning
        
        if (hasActiveSession) {
            timerRunning.value = true
            isFocusActive.value = true
            isPomodoroMode.value = prefs.getBoolean("TIMER_IS_POMODORO", true)
            pomodoroTotalMillis.longValue = prefs.getLong("TIMER_INITIAL_DURATION", 25 * 60 * 1000L)
            timeLeft.longValue = if (isPomodoroMode.value) (targetEnd - System.currentTimeMillis()) else FocusService.currentTime
            updateAmbientSounds()
        } else {
            isFocusActive.value = false
            timerRunning.value = false
            updateAmbientSounds()
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
                            isFocusActive.value = false
                            updateAmbientSounds()
                        }
                    } else {
                        timeElapsed.longValue = FocusService.currentTime
                    }
                }
            }
        }
    }

    fun toggleTimer(context: Context, running: Boolean) {
        if (running) {
            val isResume = isTimerPaused.value
            timerRunning.value = true
            isTimerPaused.value = false
            
            val intent = Intent(context, FocusService::class.java).apply {
                action = if (isResume) FocusService.ACTION_RESUME else FocusService.ACTION_START
                putExtra(FocusService.EXTRA_IS_POMODORO, isPomodoroMode.value)
                putExtra(FocusService.EXTRA_DURATION, if (isResume) timeLeft.longValue else pomodoroTotalMillis.longValue)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            setFocusActive(true, if(isResume) System.currentTimeMillis() + timeLeft.longValue else System.currentTimeMillis() + pomodoroTotalMillis.longValue)
        } else {
            timerRunning.value = false
            isTimerPaused.value = true
            
            val intent = Intent(context, FocusService::class.java).apply { action = FocusService.ACTION_PAUSE }
            context.startService(intent)
            setFocusActive(false)
        }
    }

    fun abandonSession(context: Context) {
        if (isPomodoroMode.value && (timerRunning.value || isTimerPaused.value)) {
            recordSessionResult(false)
        }
        
        timerRunning.value = false
        isTimerPaused.value = false
        timeLeft.longValue = pomodoroTotalMillis.longValue
        
        val intent = Intent(context, FocusService::class.java).apply { action = FocusService.ACTION_STOP }
        context.stopService(intent)
        setFocusActive(false)
    }

    private fun setupSoundPool() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, _ -> updateAmbientSounds() }

        keyboardSoundId = soundPool?.load(application, R.raw.keyboard_tap, 1) ?: 0
        mouseSoundId = soundPool?.load(application, R.raw.mouse_click, 1) ?: 0
        dragonSoundId = soundPool?.load(application, R.raw.dragon_correct, 1) ?: 0
        
        try {
            rainPlayer = MediaPlayer.create(application, R.raw.rain).apply {
                isLooping = true
                setVolume(0.45f, 0.45f)
            }
        } catch (e: Exception) {}

        try {
            fireplacePlayer = MediaPlayer.create(application, R.raw.fireplace).apply {
                isLooping = true
                setVolume(0.55f, 0.55f)
            }
        } catch (e: Exception) {}
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
        try {
            if (isRainEnabled.value) {
                if (rainPlayer == null) {
                    rainPlayer = MediaPlayer.create(application, R.raw.rain).apply {
                        isLooping = true
                        setVolume(0.45f, 0.45f)
                    }
                }
                if (rainPlayer?.isPlaying == false) rainPlayer?.start()
            } else {
                if (rainPlayer?.isPlaying == true) rainPlayer?.pause()
            }
        } catch (e: Exception) {}

        try {
            if (isFireplaceEnabled.value) {
                if (fireplacePlayer == null) {
                    fireplacePlayer = MediaPlayer.create(application, R.raw.fireplace).apply {
                        isLooping = true
                        setVolume(0.55f, 0.55f)
                    }
                }
                if (fireplacePlayer?.isPlaying == false) fireplacePlayer?.start()
            } else {
                if (fireplacePlayer?.isPlaying == true) fireplacePlayer?.pause()
            }
        } catch (e: Exception) {}
    }

    fun playKeyboardSound(play: Boolean) {
        if (play && isFocusActive.value) {
            if (keyboardStreamId == 0) keyboardStreamId = soundPool?.play(keyboardSoundId, 0.25f, 0.25f, 1, -1, 1.0f) ?: 0
        } else {
            if (keyboardStreamId != 0) { soundPool?.stop(keyboardStreamId) ; keyboardStreamId = 0 }
        }
    }

    fun playMouseSound(play: Boolean) {
        if (play && isFocusActive.value) {
            if (mouseStreamId == 0) mouseStreamId = soundPool?.play(mouseSoundId, 0.2f, 0.2f, 1, -1, 1.0f) ?: 0
        } else {
            if (mouseStreamId != 0) { soundPool?.stop(mouseStreamId) ; mouseStreamId = 0 }
        }
    }

    fun toggleRadio(station: RadioStation) {
        if (currentRadioStation.value == station) pauseResumeRadio() else startRadio(station)
    }

    fun pauseResumeRadio() {
        try {
            radioPlayer?.let {
                if (it.isPlaying) { it.pause() ; isRadioPlaying.value = false } else { it.start() ; isRadioPlaying.value = true }
            } ?: currentRadioStation.value?.let { startRadio(it) }
        } catch (e: Exception) {}
    }

    fun nextRadioStation() {
        val currentIndex = radioStations.indexOf(currentRadioStation.value)
        val nextIndex = if (currentIndex == -1 || currentIndex == radioStations.size - 1) 0 else currentIndex + 1
        startRadio(radioStations[nextIndex])
    }

    fun previousRadioStation() {
        val currentIndex = radioStations.indexOf(currentRadioStation.value)
        val prevIndex = if (currentIndex <= 0) radioStations.size - 1 else currentIndex - 1
        startRadio(radioStations[prevIndex])
    }

    private fun startRadio(station: RadioStation) {
        stopRadio()
        currentRadioStation.value = station
        isRadioLoading.value = true
        radioJob = viewModelScope.launch(Dispatchers.Main) {
            var player: MediaPlayer? = null
            try {
                if (station.isLocal) {
                    player = MediaPlayer.create(application, station.resId)
                    player.isLooping = true
                    player.start()
                    radioPlayer = player
                    isRadioPlaying.value = true
                    isRadioLoading.value = false
                } else {
                    player = MediaPlayer()
                    player.setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
                    radioPlayer = player
                    player.setOnPreparedListener { it.start() ; isRadioPlaying.value = true ; isRadioLoading.value = false }
                    player.setOnErrorListener { _, _, _ -> isRadioLoading.value = false ; isRadioPlaying.value = false ; stopRadio() ; true }
                    viewModelScope.launch { delay(20000) ; if (isRadioLoading.value && radioPlayer == player) stopRadio() }
                    withContext(Dispatchers.IO) { player.setDataSource(station.url) ; player.prepareAsync() }
                }
            } catch (e: Exception) { isRadioLoading.value = false ; stopRadio() }
        }
    }

    fun stopRadio() {
        radioJob?.cancel() ; radioJob = null
        radioPlayer?.let { try { if (it.isPlaying) it.stop() ; it.release() } catch (e: Exception) {} }
        radioPlayer = null ; currentRadioStation.value = null ; isRadioPlaying.value = false ; isRadioLoading.value = false
    }

    fun playTickSound() {
        if (dragonSoundId != 0) soundPool?.play(dragonSoundId, 1.0f, 1.0f, 5, 0, 1.0f)
    }

    fun startLiveFocusListener() {
        val currentEmail = userEmail.value
        if (currentEmail.isBlank()) return
        liveFocusRegistration?.remove()
        liveFocusRegistration = firestore.collection("live_focus")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val now = System.currentTimeMillis()
                val hasUrgentUpdate = snapshot?.documentChanges?.any { 
                    val data = it.document.data
                    val emojiTime = data["emojiTime"] as? Long ?: 0L
                    val hasEmoji = data["latestEmoji"] != null && Math.abs(now - emojiTime) < 10000
                    val isPhotoChange = it.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED && (data.containsKey("photoUrl") || data.containsKey("photo_url"))
                    hasEmoji || isPhotoChange
                } ?: false

                if (!hasUrgentUpdate && now - lastLiveFocusUpdate < 2000) return@addSnapshotListener
                if (!hasUrgentUpdate) lastLiveFocusUpdate = now

                snapshot?.let { querySnapshot ->
                    val remoteUsers = querySnapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val email = data["email"] as? String ?: ""
                        val lastUpd = data["lastUpdate"] as? Long ?: 0L
                        if (now - lastUpd > 60000) return@mapNotNull null
                        val isMe = email.equals(currentEmail, ignoreCase = true)
                        val remotePhoto = (data["photoUrl"] as? String) ?: (data["photo_url"] as? String)
                        WorkerInfo(
                            id = if (isMe) "me" else "live_$email",
                            name = (if (isMe) userName.value else data["name"] as? String) ?: "Peer",
                            photoUrl = if (isMe) userPhotoUrl.value else remotePhoto,
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
        val currentEmail = userEmail.value
        val activeDeskIds = visibleItems.filter { it.startsWith("desk_setup_") }
        if (activeDeskIds.isEmpty()) { workers.clear() ; return }

        val me = livePeers.find { it.isMe }
        val buddyEmail = selectedFocusBuddy.value?.email
        val buddyUser = leaderboard.value.find { it.email == buddyEmail }
        val activeFriends = leaderboard.value.filter { it.email != currentEmail && it.email != buddyEmail && it.isFocusing }
        val isTeamMode = isTeamOfisMode.value
        val myTeamId = userTeam.value?.id

        val availableLivePeers = if (isTeamMode && myTeamId != null) {
            livePeers.filter { lp -> !lp.isMe && lp.email != buddyEmail && leaderboard.value.find { it.email == lp.email }?.teamId == myTeamId }
        } else if (isTeamMode) {
            emptyList()
        } else {
            livePeers.filter { lp -> !lp.isMe && lp.email != buddyEmail && !activeFriends.any { it.email == lp.email } }
        }
        
        val priorityList = mutableListOf<WorkerInfo>()
        if (me != null) priorityList.add(me)
        
        if (buddyUser != null) {
            val showBuddy = !isTeamMode || buddyUser.teamId == myTeamId
            if (showBuddy) {
                val liveBuddy = livePeers.find { it.email == buddyUser.email }
                priorityList.add(WorkerInfo(
                    id = "buddy_${buddyUser.email}",
                    name = buddyUser.name,
                    photoUrl = liveBuddy?.photoUrl ?: buddyUser.photoUrl,
                    email = buddyUser.email,
                    deskId = "",
                    isFocusing = buddyUser.isFocusing,
                    currentAction = if (buddyUser.isFocusing) WorkerAction.WORKING else WorkerAction.IDLE,
                    isFriend = true,
                    isLiveUser = true,
                    latestEmoji = liveBuddy?.latestEmoji ?: buddyUser.latestEmoji,
                    emojiTime = if (liveBuddy != null) liveBuddy.emojiTime else buddyUser.emojiTime,
                    interactionText = buddyUser.currentTaskTitle
                ))
            }
            buddyCurrentTask.value = buddyUser.currentTaskTitle
        } else {
            buddyCurrentTask.value = null
        }

        activeFriends.forEach { f ->
            if (!isTeamMode || f.teamId == myTeamId) {
                val liveData = livePeers.find { it.email == f.email }
                priorityList.add(WorkerInfo(id = "friend_${f.email}", name = f.name, photoUrl = liveData?.photoUrl ?: f.photoUrl, email = f.email, deskId = "", isFocusing = true, currentAction = WorkerAction.WORKING, isFriend = true, isLiveUser = true, latestEmoji = liveData?.latestEmoji ?: f.latestEmoji, emojiTime = if (liveData != null) liveData.emojiTime else f.emojiTime))
            }
        }
        priorityList.addAll(availableLivePeers)

        val finalWorkers = mutableListOf<WorkerInfo>()
        activeDeskIds.forEachIndexed { i, deskId ->
            val topPeer = priorityList.getOrNull(i) ?: return@forEachIndexed
            val indexInDefault = deskId.removePrefix("desk_setup_").toIntOrNull() ?: 0
            val deskOffset = when(indexInDefault) { 0 -> Offset(-90f, 0f) ; 1 -> Offset(0f, 0f) ; else -> Offset(90f, 0f) }
            val spawnPos = itemPositions[deskId] ?: deskOffset
            finalWorkers.add(topPeer.copy(deskId = deskId, currentPos = spawnPos, targetPos = spawnPos, name = if (topPeer.isMe) "${topPeer.name} (Siz)" else topPeer.name))
        }
        workers.clear() ; workers.addAll(finalWorkers)
    }

    private fun updateLiveFocusStatus(active: Boolean, forcedPhotoUrl: String? = null) {
        val email = userEmail.value.lowercase()
        if (email.isBlank()) return
        
        val docRef = firestore.collection("live_focus").document(email)
        if (active) {
            val user = firebaseAuth.currentUser
            val currentPhoto = userPhotoUrl.value
            var remoteUrl = when {
                !forcedPhotoUrl.isNullOrBlank() -> forcedPhotoUrl
                currentPhoto?.startsWith("http") == true || currentPhoto?.startsWith("data:image") == true -> currentPhoto
                else -> user?.photoUrl?.toString() ?: ""
            }
            if (remoteUrl.isBlank() || remoteUrl == "null") {
                val finalName = if (userName.value != "ANONYMOUS" && userName.value.isNotBlank()) userName.value else user?.displayName ?: email.substringBefore("@")
                remoteUrl = "https://ui-avatars.com/api/?name=${finalName.replace(" ", "+")}&background=0D8ABC&color=fff"
            }
            val finalUrl = if (remoteUrl.contains("ui-avatars.com")) remoteUrl else { 
                val cleanUrl = if (remoteUrl.contains("?t=")) remoteUrl.substringBefore("?t=") else remoteUrl
                "$cleanUrl?t=${System.currentTimeMillis()}" 
            }
            val status = mapOf(
                "email" to email,
                "name" to userName.value,
                "photoUrl" to finalUrl,
                "photo_url" to finalUrl,
                "lastUpdate" to System.currentTimeMillis()
            )
            docRef.set(status, com.google.firebase.firestore.SetOptions.merge())
        } else {
            docRef.delete()
        }
    }

    private fun startPresenceHeartbeat() {
        presenceHeartbeatJob?.cancel()
        if (userEmail.value.isBlank()) return
        
        presenceHeartbeatJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val focusing = isFocusActive.value
                    syncXpToFirestore(isFocusing = focusing)
                    if (focusing) updateLiveFocusStatus(true)
                    delay(if (focusing) 30000L else 180000L)
                } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e ; delay(10000L) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        friendRequestRegistration?.remove()
        liveFocusRegistration?.remove()
        teamListener?.remove()
        teamMembersListener?.remove()
        updateLiveFocusStatus(false)
        presenceHeartbeatJob?.cancel()
        soundPool?.release() ; soundPool = null
        try { rainPlayer?.release() ; rainPlayer = null ; fireplacePlayer?.release() ; fireplacePlayer = null ; radioPlayer?.release() ; radioPlayer = null } catch (e: Exception) {}
    }

    private fun updateFriendWorkers() {}

    private fun initializeWorkers() {
        workers.clear()
        val activeUsers = leaderboard.value.filter { it.uid != "me" && it.isFocusing }.sortedBy { it.uid }
        val activeDeskIds = visibleItems.filter { it.startsWith("desk_setup_") }.sorted()
        val roomBase = Offset(0f, 0f)
        activeDeskIds.forEachIndexed { i, deskId ->
            val lbUser = activeUsers.getOrNull(i) ?: return@forEachIndexed
            val indexInDefault = deskId.removePrefix("desk_setup_").toIntOrNull() ?: 0
            val deskOffset = when(indexInDefault) { 0 -> Offset(-90f, 0f) ; 1 -> Offset(0f, 0f) ; else -> Offset(90f, 0f) }
            val spawnPos = itemPositions[deskId] ?: (roomBase + deskOffset)
            val taskText = if (!lbUser.currentTaskTitle.isNullOrBlank()) lbUser.currentTaskTitle else "Focusing..."
            workers.add(WorkerInfo(id = "init_${lbUser.uid}", name = lbUser.name, deskId = deskId, currentPos = spawnPos, targetPos = spawnPos, monitorContent = taskText!!, currentAction = WorkerAction.WORKING, photoUrl = lbUser.photoUrl, isFriend = true, isFocusing = true, roomId = 0, email = lbUser.email))
        }
    }

    private fun startWorkerSimulation() {
        viewModelScope.launch { while (true) { delay(2000L + (0..3000).random()) ; val size = workers.size ; for (i in 0 until size) { if (i < workers.size) updateWorkerBehavior(i) } } }
        viewModelScope.launch {
            while (true) {
                delay(60L)
                val currentSize = workers.size
                if (!workers.any { it.currentAction == WorkerAction.WALKING }) continue
                for (i in 0 until currentSize) {
                    if (i >= workers.size) continue
                    val worker = try { workers[i] } catch (e: Exception) { continue }
                    if (worker.currentAction == WorkerAction.WALKING) {
                        val newPos = moveTowards(worker.currentPos, worker.targetPos, 1.8f)
                        val facingRight = worker.targetPos.x > worker.currentPos.x
                        if (newPos != worker.currentPos || facingRight != worker.isFacingRight) { if (i < workers.size) workers[i] = workers[i].copy(currentPos = newPos, isFacingRight = facingRight) }
                        if (newPos == worker.targetPos) { val finalAction = when { worker.targetPos.x == 120f -> WorkerAction.COFFEE ; worker.targetPos.x == -120f -> WorkerAction.MEETING ; else -> WorkerAction.IDLE } ; if (i < workers.size) workers[i] = workers[i].copy(currentAction = finalAction, isFacingRight = true) }
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
            isFocusActive && hasDesk -> when { rand < 12 -> { val questionsList = listOf("Bug'ı çözemedim, yardım?", "API dokümanı nerede?", "PR bekliyor!", "Fix this bug?", "PR is ready!") ; workers[idx] = workers[idx].copy(interactionText = questionsList.random()) ; WorkerAction.ASKING } ; rand < 45 -> { workers[idx] = workers[idx].copy(interactionText = null) ; WorkerAction.TYPING } ; rand < 75 -> { workers[idx] = workers[idx].copy(interactionText = null) ; WorkerAction.MOUSE } ; rand < 90 -> { workers[idx] = workers[idx].copy(interactionText = null) ; WorkerAction.WORKING } ; else -> { workers[idx] = workers[idx].copy(interactionText = null) ; WorkerAction.THINKING } }
            !isFocusActive && hasDesk -> if (worker.movementCount >= 2) { if (Math.abs(worker.currentPos.x - currentDeskPos.x) > 5f || Math.abs(worker.currentPos.y - currentDeskPos.y) > 5f) { startWalking(idx, currentDeskPos) ; return } ; WorkerAction.IDLE } else when { rand < 10 -> WorkerAction.TYPING ; rand < 20 -> WorkerAction.MOUSE ; rand < 60 -> { incrementMovement(idx) ; startWalking(idx, Offset(120f, -60f)) ; return } ; rand < 75 -> { incrementMovement(idx) ; startWalking(idx, Offset(-120f, 60f)) ; return } ; else -> { if (Math.abs(worker.currentPos.x - currentDeskPos.x) > 30f) { if ((0..1).random() == 0) { startWalking(idx, currentDeskPos) ; return } } ; WorkerAction.IDLE } }
            else -> when { rand < 30 -> WorkerAction.THINKING ; rand < 60 -> WorkerAction.IDLE ; else -> WorkerAction.RESTING }
        }
        workers[idx] = worker.copy(currentAction = nextAction, isFocusing = isFocusActive && hasDesk, lastActionTime = System.currentTimeMillis())
        if (isFocusActive && hasDesk && (nextAction == WorkerAction.TYPING || nextAction == WorkerAction.MOUSE)) workers[idx].xpContribution += 1
    }

    private fun incrementMovement(idx: Int) { workers[idx] = workers[idx].copy(movementCount = workers[idx].movementCount + 1) }

    private fun startWalking(idx: Int, target: Offset) { workers[idx] = workers[idx].copy(currentAction = WorkerAction.WALKING, targetPos = target) }

    private fun moveTowards(current: Offset, target: Offset, speed: Float): Offset {
        val dx = target.x - current.x ; val dy = target.y - current.y
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (distance <= speed) return target
        return Offset(current.x + (dx / distance) * speed, current.y + (dy / distance) * speed)
    }

    fun syncTasksFromCloud(email: String? = null, uid: String? = null) {
        val finalEmail = (email ?: userEmail.value).lowercase()
        val finalUid = uid ?: firebaseAuth.currentUser?.uid
        if (finalEmail.isBlank() && finalUid.isNullOrBlank()) return
        if (finalEmail.isNotBlank()) firestore.collection("users").document(finalEmail).collection("tasks").get().addOnSuccessListener { processTaskSnapshot(it) }
        if (!finalUid.isNullOrBlank()) firestore.collection("users").document(finalUid).collection("tasks").get().addOnSuccessListener { processTaskSnapshot(it) }
    }

    private fun processTaskSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot) {
        viewModelScope.launch(Dispatchers.IO) { snapshot.documents.forEach { doc -> val task = doc.toObject(TaskEntity::class.java) ; if (task != null) taskDao.insertTask(if (task.id == 0L) task.copy(id = doc.id.toLongOrNull() ?: System.currentTimeMillis()) else task) } }
    }

    private fun fetchUserDataFromFirestore() {
        val email = userEmail.value.lowercase()
        if (email.isBlank()) return

        val user = firebaseAuth.currentUser
        val docRefByEmail = firestore.collection("users").document(email)
        val docRefByUid = if (user != null) firestore.collection("users").document(user.uid) else null

        docRefByEmail.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                applyUserDataDoc(doc)
                syncTasksFromCloud(email, user?.uid)
            } else docRefByUid?.get()?.addOnSuccessListener {
                if (it.exists()) {
                    applyUserDataDoc(it)
                    syncTasksFromCloud(email, user?.uid)
                }
            }
        }
    }

    private fun applyUserDataDoc(doc: com.google.firebase.firestore.DocumentSnapshot) {
        doc.getLong("user_xp")?.toInt()?.let { if (it > userXp.value) { userXp.value = it ; _localUserXp.value = it.toLong() ; prefs.edit().putInt("user_xp", it).apply() } }
        doc.getLong("user_coins")?.toInt()?.let { if (it > userCoins.value) { userCoins.value = it ; prefs.edit().putInt("user_coins", it).apply() } }
        doc.getLong("lifetime_coins")?.toInt()?.let { if (it > lifetimeCoins.value) lifetimeCoins.value = it }
        doc.getLong("office_level")?.toInt()?.let { if (it > officeLevel.value) officeLevel.value = it }
        doc.getBoolean("is_premium")?.let { if (it) isPremium.value = true }
        (doc.get("unlocked_items") as? List<String>)?.forEach { if (!unlockedItems.contains(it)) unlockedItems.add(it) }
        doc.getLong("total_focus_minutes")?.toInt()?.let { if (it > 0) totalFocusMinutesCloud.intValue = it }
        doc.getString("photoUrl")?.let { if (it.isNotBlank()) { val localUpdateTime = prefs.getLong("profile_last_local_update", 0L) ; if (System.currentTimeMillis() - localUpdateTime > 60000) { userPhotoUrl.value = it ; _localUserPhoto.value = it } } }
        doc.getString("name")?.let { if (it.isNotBlank() && it != "ANONYMOUS") { userName.value = it ; _localUserName.value = it } }
    }

    fun syncProfileToFirestore() {
        val email = userEmail.value.lowercase()
        if (email.isBlank()) return

        val user = firebaseAuth.currentUser
        val currentPhoto = userPhotoUrl.value
        if (currentPhoto?.startsWith("file://") == true) return
        
        val remoteUrl = if (currentPhoto?.startsWith("http") == true) currentPhoto else user?.photoUrl?.toString() ?: ""
        val profileMap = mutableMapOf(
            "uid" to (user?.uid ?: "guest_${email.hashCode()}"),
            "name" to userName.value,
            "email" to email,
            "photoUrl" to remoteUrl,
            "user_xp" to userXp.value,
            "user_coins" to userCoins.value,
            "office_level" to officeLevel.value,
            "unlocked_items" to unlockedItems.toList(),
            "is_premium" to isPremium.value,
            "last_sync" to System.currentTimeMillis()
        )
        
        // Sync to Firestore using email as document ID for persistence across guest sessions
        firestore.collection("users").document(email).set(profileMap, com.google.firebase.firestore.SetOptions.merge())
        // Also sync to UID if available
        user?.let { firestore.collection("users").document(it.uid).set(profileMap, com.google.firebase.firestore.SetOptions.merge()) }
        
        syncWithBackend()
    }

    private fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun setSelectedDate(millis: Long) { _selectedDate.value = millis }
    fun getSubTasks(parentId: Long): Flow<List<TaskEntity>> = taskDao.getSubTasks(parentId)

    fun incrementWinCount() { if (!hasSeenReview) { showReviewDialog.value = true ; hasSeenReview = true ; prefs.edit().putBoolean("has_seen_review", true).apply() } }

    fun toggleTeamOfficeMode(enabled: Boolean) { isTeamOfisMode.value = enabled ; prefs.edit().putBoolean("is_team_office_mode", enabled).apply() ; updateWorkersWithLivePeers(workers.toList()) }

    fun createTeam(teamName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser
        val email = user?.email?.lowercase() ?: "local_user@focuspath.local"
        isTeamLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val teamId = UUID.randomUUID().toString()
                val inviteCode = (1..6).map { (('A'..'Z') + ('0'..'9')).random() }.joinToString("")
                val newTeam = Team(id = teamId, name = teamName, inviteCode = inviteCode, creatorUid = user?.uid ?: "local_uid", memberEmails = listOf(email))
                
                if (user != null) {
                    firestore.collection("teams").document(teamId).set(newTeam).await()
                    val updateMap = mapOf("teamId" to teamId)
                    firestore.collection("users").document(user.uid).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                    if (user.email != null) firestore.collection("users").document(user.email!!.lowercase()).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                } else {
                    val teamJson = Gson().toJson(newTeam)
                    prefs.edit().putString("local_team_data", teamJson).putString("local_team_id", teamId).apply()
                }

                withContext(Dispatchers.Main) { 
                    isTeamLoading.value = false 
                    if (user != null) fetchUserTeam() else {
                        userTeam.value = newTeam
                        teamMembers.clear()
                        teamMembers.add(LeaderboardUser(uid = "me", name = userName.value, email = email, score = userXp.value.toLong()))
                    }
                    onSuccess() 
                    completeOnboardingTask("join_team") 
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { isTeamLoading.value = false ; onError(e.localizedMessage ?: "Error") } }
        }
    }

    fun joinTeam(inviteCode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = firebaseAuth.currentUser
        val email = user?.email?.lowercase() ?: "local_user@focuspath.local"
        isTeamLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (user != null) {
                    val teamSnapshot = firestore.collection("teams").whereEqualTo("inviteCode", inviteCode.trim().uppercase()).get().await()
                    if (teamSnapshot.isEmpty) throw Exception("Invalid code")
                    val teamDoc = teamSnapshot.documents[0]
                    val team = teamDoc.toObject(Team::class.java) ?: throw Exception("Error")
                    if (team.memberEmails.contains(email)) throw Exception("Already in team")
                    val updatedMembers = team.memberEmails.toMutableList().apply { add(email) }
                    firestore.collection("teams").document(team.id).update("memberEmails", updatedMembers).await()
                    val updateMap = mapOf("teamId" to team.id)
                    firestore.collection("users").document(user.uid).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                    if (user.email != null) firestore.collection("users").document(user.email!!.lowercase()).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                    withContext(Dispatchers.Main) { isTeamLoading.value = false ; fetchUserTeam() ; onSuccess() ; completeOnboardingTask("join_team") }
                } else {
                    // Local Mode: Just simulate joining if the code is correct-looking
                    if (inviteCode.length != 6) throw Exception("Invalid code")
                    val simulatedTeam = Team(id = "local_join", name = "Local HQ", inviteCode = inviteCode, memberEmails = listOf(email))
                    prefs.edit().putString("local_team_data", Gson().toJson(simulatedTeam)).apply()
                    withContext(Dispatchers.Main) {
                        isTeamLoading.value = false
                        userTeam.value = simulatedTeam
                        teamMembers.clear()
                        teamMembers.add(LeaderboardUser(uid = "me", name = userName.value, email = email, score = userXp.value.toLong()))
                        onSuccess()
                        completeOnboardingTask("join_team")
                    }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { isTeamLoading.value = false ; onError(e.localizedMessage ?: "Error") } }
        }
    }

    fun leaveTeam(onSuccess: () -> Unit) {
        val user = firebaseAuth.currentUser
        val email = user?.email?.lowercase() ?: "local_user@focuspath.local"
        val currentTeamId = userTeam.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (user != null) {
                    val teamDoc = firestore.collection("teams").document(currentTeamId).get().await()
                    if (teamDoc.exists()) {
                        val team = teamDoc.toObject(Team::class.java)
                        if (team != null) {
                            val updatedMembers = team.memberEmails.filter { it.lowercase() != email }
                            firestore.collection("teams").document(currentTeamId).update("memberEmails", updatedMembers).await()
                        }
                    }
                    teamListener?.remove() ; teamMembersListener?.remove()
                    val updateMap = mapOf<String, Any?>("teamId" to null)
                    firestore.collection("users").document(user.uid).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                    if (user.email != null) firestore.collection("users").document(user.email!!.lowercase()).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                } else {
                    prefs.edit().remove("local_team_data").remove("local_team_id").apply()
                }
                withContext(Dispatchers.Main) { userTeam.value = null ; teamMembers.clear() ; onSuccess() }
            } catch (e: Exception) {
                if (user == null) {
                    prefs.edit().remove("local_team_data").remove("local_team_id").apply()
                    withContext(Dispatchers.Main) { userTeam.value = null ; teamMembers.clear() ; onSuccess() }
                }
            }
        }
    }

    fun fetchUserTeam() {
        val user = firebaseAuth.currentUser ?: return
        firestore.collection("users").document(user.uid).get().addOnSuccessListener { doc -> doc.getString("teamId")?.let { listenToTeam(it) } }
    }

    private fun listenToTeam(teamId: String) {
        teamListener?.remove()
        teamListener = firestore.collection("teams").document(teamId).addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            val team = snapshot?.toObject(Team::class.java)
            userTeam.value = team
            if (team != null) {
                completeOnboardingTask("join_team")
                teamMembersListener?.remove()
                if (team.memberEmails.isNotEmpty()) {
                    teamMembersListener = firestore.collection("leaderboard").whereIn("email", team.memberEmails).addSnapshotListener { lbSnapshot, _ ->
                        val members = lbSnapshot?.documents?.mapNotNull { doc -> doc.toObject(LeaderboardUser::class.java)?.copy(uid = doc.id) } ?: emptyList()
                        teamMembers.clear() ; teamMembers.addAll(members)
                    }
                }
            }
        }
    }

    fun fetchLeaderboard() {
        isLeaderboardLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getLeaderboard()
                if (response.isSuccessful && response.body() != null) {
                    _leaderboard.value = response.body()!!.map { LeaderboardUser(uid = it.email, email = it.email, name = it.username, score = it.xp, level = it.level) }
                    initializeWorkers()
                } else fetchLeaderboardFromFirestore()
            } catch (e: Exception) { fetchLeaderboardFromFirestore() } finally { isLeaderboardLoading.value = false }
        }
        firestore.collection("teams").orderBy("totalTeamXp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(10).addSnapshotListener { snapshot, _ -> if (snapshot != null) _teamLeaderboard.value = snapshot.documents.mapNotNull { it.toObject(Team::class.java) } }
        listenForIncomingMessages()
    }

    private fun fetchLeaderboardFromFirestore() {
        firestore.collection("leaderboard").limit(50).get().addOnSuccessListener { snapshot ->
            val users = snapshot.documents.mapNotNull { it.toObject(LeaderboardUser::class.java)?.copy(uid = it.id) }.sortedByDescending { it.score }
            if (users.isEmpty()) fetchLeaderboardLocal() else { _leaderboard.value = users ; initializeWorkers() }
        }
    }

    fun fetchLeaderboardLocal() {
        val me = LeaderboardUser(uid = "me", name = userName.value + " (Siz)", score = userXp.value.toLong(), photoUrl = userPhotoUrl.value, level = userLevel.value)
        _leaderboard.value = (simulatedBots + me).sortedByDescending { it.score }
        initializeWorkers()
    }

    private fun startBotSimulation() {
        if (botSimulationJob != null) return
        
        val botTasks = listOf(
            "Kod İnceleme", "UI Tasarımı", "Veritabanı Optimizasyonu", 
            "Müşteri Sunumu", "E-postaları Yanıtla", "Yeni Özellik Geliştirme",
            "Bug Fix", "Dökümantasyon Yazımı", "Ekip Toplantısı", "Kahve Molası"
        )
        
        val botMessages = listOf(
            "Harika gidiyorsun, devam et!", 
            "Bugün çok üretkeniz!", 
            "Biraz mola vermeyi unutma.", 
            "Odaklanmak harika hissettiriyor.", 
            "Hadi şu görevleri bitirelim!",
            "Seninle çalışmak motive edici."
        )

        if (simulatedBots.isEmpty()) {
            val names = listOf("Arda Yılmaz", "Zeynep Kaya", "Can Demir", "Elif Şahin", "Mert Aydın")
            val colors = listOf("0D8ABC", "6495ED", "20B2AA", "F08080", "FFA07A")
            names.forEachIndexed { i, name ->
                simulatedBots.add(LeaderboardUser(
                    uid = "bot_$i",
                    name = name,
                    score = (800..3500).random().toLong(),
                    level = (8..35).random(),
                    isFocusing = (0..1).random() == 1,
                    currentTaskTitle = if ((0..1).random() == 1) botTasks.random() else "",
                    photoUrl = "https://ui-avatars.com/api/?name=${name.replace(" ", "+")}&background=${colors[i]}&color=fff",
                    latestEmoji = null
                ))
            }
        }

        botSimulationJob = viewModelScope.launch {
            while (true) {
                delay(30000L) // 30 saniyede bir aksiyon şansı
                
                var changed = false
                val currentTime = System.currentTimeMillis()
                
                for (i in simulatedBots.indices) {
                    val bot = simulatedBots[i]
                    
                    // 1. Odaklanma durumu değişimi (%15 ihtimal)
                    val toggleFocus = (1..100).random() <= 15
                    var newIsFocusing = bot.isFocusing
                    var newTitle = bot.currentTaskTitle
                    var newEmoji = bot.latestEmoji
                    var newEmojiTime = bot.emojiTime
                    
                    if (toggleFocus) {
                        newIsFocusing = !newIsFocusing
                        newTitle = if (newIsFocusing) botTasks.random() else ""
                        newEmoji = if (newIsFocusing) "🚀" else "☕"
                        newEmojiTime = currentTime
                        changed = true
                    }
                    
                    // 2. Rastgele Emoji Tepkisi (%10 ihtimal)
                    if (!toggleFocus && (1..100).random() <= 10) {
                        val emojis = listOf("🔥", "⚡", "💪", "🎯", "👏", "⭐")
                        newEmoji = emojis.random()
                        newEmojiTime = currentTime
                        changed = true
                    }
                    
                    // 3. Mesaj Gönderme (%5 ihtimal, sadece odaklanıyorken ve kullanıcıya)
                    if (newIsFocusing && (1..100).random() <= 5) {
                        val msg = DirectMessage(
                            id = UUID.randomUUID().toString(),
                            from = "bot_${i}@focuspath.local",
                            fromName = bot.name,
                            to = userEmail.value,
                            toName = userName.value,
                            text = botMessages.random(),
                            timestamp = currentTime
                        )
                        withContext(Dispatchers.Main) {
                            if (!_directMessages.any { it.text == msg.text && it.fromName == msg.fromName }) {
                                _directMessages.add(msg)
                            }
                        }
                    }
                    
                    // 4. Skor artışı (Sadece odaklanıyorsa)
                    var newScore = bot.score
                    if (newIsFocusing) {
                        newScore += (2..6).random()
                        changed = true
                    }
                    
                    val newLevel = (newScore / 100).toInt() + 1
                    
                    if (changed) {
                        simulatedBots[i] = bot.copy(
                            score = newScore, 
                            level = newLevel, 
                            isFocusing = newIsFocusing,
                            currentTaskTitle = newTitle,
                            latestEmoji = newEmoji,
                            emojiTime = newEmojiTime
                        )
                    }
                }
                
                if (changed || prefs.getBoolean("is_local_mode", false)) {
                    val me = LeaderboardUser(uid = "me", name = userName.value + " (Siz)", score = userXp.value.toLong(), photoUrl = userPhotoUrl.value, level = userLevel.value)
                    _leaderboard.value = (simulatedBots + me).sortedByDescending { it.score }
                    
                    withContext(Dispatchers.Main) {
                        initializeWorkers()
                    }
                }
            }
        }
    }

    private fun listenForIncomingMessages() {
        val email = userEmail.value
        if (email.isBlank()) return
        messageIncomingRegistration?.remove() ; messageOutgoingRegistration?.remove()
        messageIncomingRegistration = firestore.collection("messages").whereEqualTo("to", email).addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { change -> if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) { val msg = change.document.toObject(DirectMessage::class.java) ; if (!_directMessages.any { it.id == msg.id }) { _directMessages.add(msg) ; if (System.currentTimeMillis() - msg.timestamp < 10000) showLocalNotification("FocusPath Sosyal", "${msg.fromName}: ${msg.text}") } } }
        }
        messageOutgoingRegistration = firestore.collection("messages").whereEqualTo("from", email).addSnapshotListener { snapshot, _ -> snapshot?.documentChanges?.forEach { change -> if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) { val msg = change.document.toObject(DirectMessage::class.java) ; if (!_directMessages.any { it.id == msg.id }) _directMessages.add(msg) } } }
    }

    fun sendEmojiReaction(targetEmail: String, emoji: String) {
        if (targetEmail.isBlank()) return
        val cleanTargetEmail = targetEmail.trim().lowercase() ; val now = System.currentTimeMillis()
        firestore.collection("live_focus").document(cleanTargetEmail).set(mapOf("latestEmoji" to emoji, "emojiTime" to now), com.google.firebase.firestore.SetOptions.merge())
        val workerIdx = workers.indexOfFirst { it.email?.lowercase() == cleanTargetEmail }
        if (workerIdx != -1) workers[workerIdx] = workers[workerIdx].copy(latestEmoji = emoji, emojiTime = now)
        sendDirectMessage(cleanTargetEmail, "Arkadaş", "Sana bir tepki gönderdi: $emoji")
    }

    fun sendDirectMessage(toEmail: String, toName: String, content: String) {
        val fromEmail = userEmail.value.lowercase()
        val fromName = if (userName.value.isBlank() || userName.value == "ANONYMOUS") userEmail.value.split("@").first() else userName.value
        val msgId = UUID.randomUUID().toString()
        val msg = DirectMessage(id = msgId, from = fromEmail, fromName = fromName, to = toEmail, toName = toName, text = content, timestamp = System.currentTimeMillis())
        firestore.collection("messages").document(msgId).set(msg)
    }

    private fun syncTaskToFirestore(task: TaskEntity) {
        val email = userEmail.value.lowercase()
        if (!isLoggedIn.value || email.isBlank()) return
        firestore.collection("users").document(email).collection("tasks").document(task.id.toString()).set(task)
        syncWithBackend()
    }

    private fun deleteTaskFromFirestore(taskId: Long) {
        val email = userEmail.value.lowercase()
        if (!isLoggedIn.value || email.isBlank()) return
        firestore.collection("users").document(email).collection("tasks").document(taskId.toString()).delete()
    }

    fun syncXpToFirestore(sessionDuration: Int = 0, isFocusing: Boolean = false, forcedPhotoUrl: String? = null) {
        val email = userEmail.value.lowercase()
        if (email.isBlank()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTasks = taskDao.getAllTasksOnce()
                val activeTask = currentTasks.find { !it.isCompleted && it.priority == 2 } ?: currentTasks.firstOrNull { !it.isCompleted }
                val finalName = if (userName.value != "ANONYMOUS" && userName.value.isNotBlank()) userName.value else email.substringBefore("@")
                
                val currentPhoto = userPhotoUrl.value
                var remoteUrl = when { 
                    !forcedPhotoUrl.isNullOrBlank() -> forcedPhotoUrl 
                    currentPhoto?.startsWith("http") == true -> currentPhoto 
                    else -> firebaseAuth.currentUser?.photoUrl?.toString() ?: "" 
                }
                
                if (remoteUrl.isBlank()) {
                    remoteUrl = "https://ui-avatars.com/api/?name=${finalName.replace(" ", "+")}&background=0D8ABC&color=fff"
                }

                val user = firebaseAuth.currentUser
                val myUid = user?.uid ?: "guest_${email.hashCode()}"
                
                val updateMap = mutableMapOf<String, Any>(
                    "uid" to myUid,
                    "email" to email,
                    "score" to userXp.value.toLong(),
                    "photoUrl" to remoteUrl,
                    "name" to finalName,
                    "timestamp" to System.currentTimeMillis(),
                    "focusing" to isFocusing,
                    "currentTaskTitle" to (if (isFocusing) activeTask?.title ?: "" else "")
                )
                
                firestore.collection("leaderboard").document(myUid).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (e: Exception) {}
        }
    }

    fun sendAiCommand(prompt: String, isEnglish: Boolean) {
        val target = selectedChatUser.value
        val prefix = if (target != null) "@$target: " else ""
        chatHistory.add("${if (isEnglish) "You: " else "Siz: "}$prefix$prompt")
        if (target != null) return
        isBotTyping.value = true
        viewModelScope.launch {
            try { 
                val apiKey = com.focuspath.app.BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey.contains("YOUR_KEY")) throw Exception("API key is missing")

                val response = withContext(Dispatchers.IO) { chatSession.sendMessage(prompt) } 
                val responseText = response.text
                if (responseText.isNullOrBlank()) {
                    chatHistory.add("YimeBot: ${if (isEnglish) "I couldn't generate a response. Please try a different prompt." else "Bir yanıt oluşturamadım. Lütfen farklı bir şey sorun."}")
                } else {
                    chatHistory.add("YimeBot: $responseText")
                }
            }
            catch (e: Exception) { 
                android.util.Log.e("YimeBot", "AI Error, falling back to local mode", e)
                val tasks = taskDao.getAllTasksOnce()
                val localResponse = getLocalAiResponse(prompt, isEnglish, tasks)
                chatHistory.add("YimeBot: $localResponse")
                
                // Re-initialize session on error to prevent sticky broken state
                try { chatSession = generativeModel.startChat() } catch (ex: Exception) {}
            }
            finally { isBotTyping.value = false }
        }
    }

    private fun getLocalAiResponse(prompt: String, isEnglish: Boolean, tasks: List<TaskEntity>): String {
        val p = prompt.lowercase(Locale.getDefault())
        val openTasks = tasks.filter { !it.isCompleted }
        
        return when {
            p.contains("selam") || p.contains("merhaba") || p.contains("hi") || p.contains("hello") || p.contains("hey") -> 
                if (isEnglish) "Hi! I'm YimeBot. I'm currently in offline mode but I can help with your tasks." 
                else "Selam! Ben YimeBot. Şu an çevrimdışı moddayım ama görevlerinle ilgili yardımcı olabilirim."
            
            p.contains("plan") -> {
                if (openTasks.isEmpty()) {
                    if (isEnglish) "You have no tasks to plan today. Why not add some goals first?"
                    else "Bugün planlayacak bir görevin yok. Önce birkaç hedef eklemeye ne dersin?"
                } else {
                    val high = openTasks.filter { it.priority == 2 }
                    val medium = openTasks.filter { it.priority == 1 }
                    val low = openTasks.filter { it.priority == 0 }
                    
                    val plan = StringBuilder()
                    if (isEnglish) plan.append("Here is your local plan (Offline Mode):\n\n")
                    else plan.append("İşte yerel planın (Çevrimdışı Mod):\n\n")
                    
                    if (high.isNotEmpty()) {
                        plan.append(if(isEnglish) "🔥 HIGH PRIORITY:\n" else "🔥 YÜKSEK ÖNCELİK:\n")
                        high.forEach { plan.append("- ${it.title}\n") }
                        plan.append("\n")
                    }
                    if (medium.isNotEmpty()) {
                        plan.append(if(isEnglish) "⚡ MEDIUM PRIORITY:\n" else "⚡ ORTA ÖNCELİK:\n")
                        medium.forEach { plan.append("- ${it.title}\n") }
                        plan.append("\n")
                    }
                    if (low.isNotEmpty()) {
                        plan.append(if(isEnglish) "🌱 LOW PRIORITY:\n" else "🌱 DÜŞÜK ÖNCELİK:\n")
                        low.forEach { plan.append("- ${it.title}\n") }
                    }
                    
                    plan.append(if(isEnglish) "\nI suggest tackling high-priority tasks first when your energy is highest!" 
                                else "\nEnerjin en yüksekken önce yüksek öncelikli görevleri bitirmeni öneririm!")
                    
                    plan.toString()
                }
            }

            p.contains("görev") || p.contains("yapılacak") || p.contains("task") || p.contains("todo") || p.contains("list") -> {
                if (openTasks.isEmpty()) {
                    if (isEnglish) "You have no active tasks! Time to relax or add a new one."
                    else "Hiç aktif görevin yok! Dinlenebilir veya yeni bir tane ekleyebilirsin."
                } else {
                    val taskList = openTasks.take(3).joinToString(", ") { it.title }
                    val count = openTasks.size
                    if (isEnglish) "You have $count tasks left. Some of them: $taskList."
                    else "Şu an $count görevin var. Bazıları: $taskList."
                }
            }
            
            p.contains("nasılsın") || p.contains("how are you") ->
                if (isEnglish) "I'm doing great, focusing on your productivity!"
                else "Harikayım, senin üretkenliğine odaklanmış durumdayım!"

            p.contains("yardım") || p.contains("help") ->
                if (isEnglish) "I can list your tasks or chat about your productivity. In offline mode, my answers are limited."
                else "Görevlerini listeleyebilir veya üretkenliğin hakkında sohbet edebiliriz. Çevrimdışı modda cevaplarım kısıtlıdır."
                
            else -> if (isEnglish) 
                "I'm in offline mode (Invalid API Key). I can only answer basic questions about your tasks." 
                else "Çevrimdışı moddayım (Geçersiz API Anahtarı). Sadece görevlerinle ilgili temel soruları yanıtlayabilirim."
        }
    }

    fun planDayWithAi(isEnglish: Boolean) {
        viewModelScope.launch {
            val tasks = taskDao.getAllTasksOnce().filter { !it.isCompleted }
            val taskSummary = tasks.joinToString { it.title }
            if (taskSummary.isBlank()) {
                chatHistory.add("YimeBot: ${if (isEnglish) "You have no active tasks to plan!" else "Planlayacak aktif bir görevin yok!"}")
                return@launch
            }
            val prompt = if (isEnglish) {
                "Plan my day with these tasks: $taskSummary. Order them logically and suggest focus durations."
            } else {
                "Bu görevlerle günümü planla: $taskSummary. Mantıklı bir sıraya koy ve odaklanma süreleri öner."
            }
            sendAiCommand(prompt, isEnglish)
        }
    }

    fun resetChat() { chatHistory.clear() ; chatSession = generativeModel.startChat() ; isBotTyping.value = false }
    fun suggestPriorityTask(taskList: List<TaskEntity>, isEnglish: Boolean) {
        val tasksString = taskList.filter { !it.isCompleted }.joinToString { it.title }
        if (tasksString.isNotBlank()) sendAiCommand("Suggestion for: $tasksString", isEnglish)
    }

    fun addTask(title: String, notes: String, category: String, priority: Int, dueDate: Long = System.currentTimeMillis(), rewardCoins: Int = 0, estimation: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = TaskEntity(title = title, notes = notes, category = category, priority = priority, dueDate = dueDate, rewardCoins = rewardCoins, energyLevel = selectedTaskEnergy.intValue, estimatedMinutes = estimation)
            val generatedId = taskDao.insertTask(task)
            syncTaskToFirestore(task.copy(id = generatedId))
            updateWidgets()
        }
    }

    fun toggleTask(task: TaskEntity) {
        if (task.isCompleted) return 
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = task.copy(isCompleted = true)
            taskDao.updateTask(updatedTask)
            syncTaskToFirestore(updatedTask)
            if (task.parentId == 0L) { taskDao.getSubTasksOnce(task.id).forEach { if (!it.isCompleted) { val updatedSub = it.copy(isCompleted = true) ; taskDao.updateTask(updatedSub) ; syncTaskToFirestore(updatedSub) } } }
            addXp(5) ; addCoins(task.rewardCoins.coerceAtLeast(5))
            updateTodayHistory(tasksDone = 1)
            applyDopamineBoost()
            viewModelScope.launch(Dispatchers.Main) { playTickSound() ; showConfetti.value = true ; delay(3000) ; showConfetti.value = false }
            updateWidgets()
        }
    }

    private fun updateWidgets() {
        viewModelScope.launch { try { com.focuspath.app.widget.TaskWidget().updateAll(application) } catch (e: Exception) {} }
    }

    fun addXp(amount: Int) { 
        val bonus = if (isTeamSynergyActive.value) 0.2f else 0f
        val finalAmount = (amount * (1f + bonus)).toInt()
        userXp.value += finalAmount ; _localUserXp.value = userXp.value.toLong() ; prefs.edit().putInt("user_xp", userXp.value).apply() 
        syncXpToFirestore() ; syncProfileToFirestore() ; updateTeamXp(finalAmount)
    }

    private fun updateTeamXp(amount: Int) {
        val team = userTeam.value ?: return
        val user = firebaseAuth.currentUser
        
        if (user != null) {
            firestore.runTransaction { transaction ->
                val teamRef = firestore.collection("teams").document(team.id)
                val remoteTeam = transaction.get(teamRef).toObject(Team::class.java) ?: return@runTransaction
                transaction.update(teamRef, mapOf("currentWeeklyXp" to remoteTeam.currentWeeklyXp + amount, "totalTeamXp" to remoteTeam.totalTeamXp + amount))
            }
        } else {
            // Local mode: update local team data
            val updatedTeam = team.copy(
                currentWeeklyXp = team.currentWeeklyXp + amount,
                totalTeamXp = team.totalTeamXp + amount
            )
            userTeam.value = updatedTeam
            prefs.edit().putString("local_team_data", Gson().toJson(updatedTeam)).apply()
        }
    }

    fun recordFocusSession(minutes: Int) {
        dailyFocusMinutes.intValue += minutes ; growPlant(minutes)
        val newFocus = prefs.getInt("DAILY_FOCUS_CURRENT", 0) + minutes ; prefs.edit().putInt("DAILY_FOCUS_CURRENT", newFocus).apply()
        if (newFocus >= 30 && !prefs.getBoolean("DAILY_FOCUS_DONE", false)) { prefs.edit().putBoolean("DAILY_FOCUS_DONE", true).apply() ; addCoins(50) }
        updateTodayHistory(focusMins = minutes)
    }

    fun setMinimalistMode(enabled: Boolean) { isMinimalistMode.value = enabled ; prefs.edit().putBoolean("adhd_minimalist_mode", enabled).apply() }

    fun setFocusActive(active: Boolean, timerEnd: Long = 0L) {
        isFocusActive.value = active ; prefs.edit().putBoolean("is_focus_active", active).apply() 
        syncXpToFirestore(isFocusing = active) ; updateLiveFocusStatus(active)
        if (active && timerEnd > 0) firebaseAuth.currentUser?.let { firestore.collection("users").document(it.uid).update("timer_target_end", timerEnd) }
        startPresenceHeartbeat() ; updateAmbientSounds()
        viewModelScope.launch { for (i in 0 until workers.size) { if (i < workers.size) { workers[i] = workers[i].copy(movementCount = 0) ; updateWorkerBehavior(i) } } }
    }

    fun toggleBlockedApp(packageName: String) { if (blockedApps.contains(packageName)) blockedApps.remove(packageName) else blockedApps.add(packageName) ; prefs.edit().putStringSet("blocked_apps", blockedApps.toSet()).apply() }
    fun deleteTask(task: TaskEntity) { viewModelScope.launch(Dispatchers.IO) { taskDao.deleteTask(task) ; if (task.parentId == 0L) taskDao.deleteSubTasks(task.id) ; deleteTaskFromFirestore(task.id) ; updateWidgets() } }
    fun updateTask(task: TaskEntity) { viewModelScope.launch(Dispatchers.IO) { taskDao.updateTask(task) ; syncTaskToFirestore(task) } }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortType(type: Int) { _sortType.value = type }
    fun setTaskFilter(filter: Int) { _taskFilter.value = filter }
    fun setCategoryFilter(category: String) { _categoryFilter.value = category }
    fun setPriorityFilter(priority: Int) { _priorityFilter.value = priority }
    fun clearCompletedTasks() { viewModelScope.launch(Dispatchers.IO) { val completed = taskDao.getAllTasksOnce().filter { it.isCompleted } ; taskDao.deleteCompletedTasks() ; completed.forEach { deleteTaskFromFirestore(it.id) } } }
    fun addCoins(amount: Int) { userCoins.value += amount ; if (amount > 0) { lifetimeCoins.value += amount ; prefs.edit().putInt("lifetime_coins", lifetimeCoins.value).apply() } ; prefs.edit().putInt("user_coins", userCoins.value).apply() ; syncProfileToFirestore() }
    fun upgradeOffice() { val cost = officeLevel.value * 500 ; if (userCoins.value >= cost) { addCoins(-cost) ; officeLevel.value += 1 ; prefs.edit().putInt("office_level", officeLevel.value).apply() ; syncProfileToFirestore() ; initializeWorkers() } }
    fun setHapticEnabled(enabled: Boolean) { isHapticEnabled.value = enabled ; prefs.edit().putBoolean("is_haptic_enabled", enabled).apply() }
    fun setThemeColor(index: Int) { themeColorIndex.value = index ; prefs.edit().putInt("theme_color_index", index).apply() }
    fun buyPremium() { billingProvider?.startPurchaseFlow() }
    fun toggleTheme() { isDarkMode.value = !isDarkMode.value ; prefs.edit().putBoolean("is_dark_mode", isDarkMode.value).apply() }

    fun buyItem(id: String, cost: Int) {
        if (unlockedItems.contains(id)) return
        if (userCoins.value >= cost) { addCoins(-cost) ; unlockedItems.add(id) ; prefs.edit().putStringSet("unlocked_items", unlockedItems.toSet()).apply() ; toggleItemVisibility(id, Offset(0f, 0f)) ; viewModelScope.launch(Dispatchers.Main) { Toast.makeText(application, "Yeni eşya kilidi açıldı! 🎁", Toast.LENGTH_SHORT).show() } }
    }

    fun loginGoogle(context: Context, idToken: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val authResult = firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
                val user = authResult.user ?: throw Exception("Login failed")
                
                isLoggedIn.value = true
                userEmail.value = user.email?.lowercase() ?: ""
                userName.value = user.displayName ?: "ANONYMOUS"
                userPhotoUrl.value = user.photoUrl?.toString()
                
                // Sync internal states
                _localUserName.value = userName.value
                _localUserPhoto.value = userPhotoUrl.value
                _localUserXp.value = userXp.value.toLong()

                prefs.edit().apply {
                    putString("user_name", userName.value)
                    putString("user_photo_url", userPhotoUrl.value)
                    putLong("profile_last_local_update", 0L)
                }.apply()
                
                fetchUserDataFromFirestore()
                fetchLeaderboard()
                startFriendRequestListener()
                startFriendsListener()
                startLiveFocusListener()
                startPresenceHeartbeat()
                onSuccess(user.email ?: "")
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error")
            }
        }
    }

    fun logoutGoogle(context: Context) {
        viewModelScope.launch {
            val isLocalModeBeforeLogout = prefs.getBoolean("is_local_mode", false)
            val savedXpBeforeLogout = userXp.value
            val savedCoinsBeforeLogout = userCoins.value
            val savedLifetimeCoinsBeforeLogout = lifetimeCoins.value
            val savedEmailBeforeLogout = userEmail.value

            firebaseAuth.signOut()
            isLoggedIn.value = false
            userEmail.value = ""
            userName.value = "ANONYMOUS"
            userPhotoUrl.value = null
            _localUserPhoto.value = null
            _localUserName.value = "ANONYMOUS"
            
            // Eğer misafir modundaysa, XP ve Coin'leri temizleme, yerel hafızada koru!
            if (isLocalModeBeforeLogout) {
                userXp.value = savedXpBeforeLogout
                _localUserXp.value = savedXpBeforeLogout.toLong()
                userCoins.value = savedCoinsBeforeLogout
                lifetimeCoins.value = savedLifetimeCoinsBeforeLogout
                
                val wasRememberMe = prefs.getBoolean("remember_me", false)
                val savedPassword = prefs.getString("saved_password", "")
                
                prefs.edit().apply {
                    putBoolean("is_local_mode", false)
                    // Kritik verileri saklamaya devam et, silme!
                    putInt("user_xp", savedXpBeforeLogout)
                    putInt("user_coins", savedCoinsBeforeLogout)
                    putInt("lifetime_coins", savedLifetimeCoinsBeforeLogout)
                    putString("saved_email", savedEmailBeforeLogout)
                    
                    if (wasRememberMe && !savedPassword.isNullOrBlank()) {
                        putString("saved_password", savedPassword)
                        putBoolean("remember_me", true)
                    }
                }.apply()
                
                android.util.Log.d("FocusPathAuth", "Guest logout completed. Progress saved locally.")
            } else {
                // Gerçek Google kullanıcısıysa eski temizlik mantığı aynen çalışsın
                userXp.value = 0
                _localUserXp.value = 0
                userCoins.value = 0
                lifetimeCoins.value = 0
                
                val wasRememberMe = prefs.getBoolean("remember_me", false)
                val savedEmail = prefs.getString("saved_email", "")
                val savedPassword = prefs.getString("saved_password", "")
                
                prefs.edit().apply {
                    remove("user_name")
                    remove("user_photo_url")
                    remove("user_xp")
                    remove("user_coins")
                    remove("lifetime_coins")
                    putBoolean("is_local_mode", false)
                    
                    if (wasRememberMe && !savedEmail.isNullOrBlank() && !savedPassword.isNullOrBlank()) {
                        putString("saved_email", savedEmail)
                        putString("saved_password", savedPassword)
                        putBoolean("remember_me", true)
                    } else {
                        remove("saved_email")
                        remove("saved_password")
                        putBoolean("remember_me", false)
                    }
                }.apply()
            }
            
            // Refresh leaderboard to clear 'me' entry or reset to generic
            fetchLeaderboardLocal()
        }
    }

    fun loginEmail(email: String, pass: String, saveCredentials: Boolean = false, onSuccess: () -> Unit, onError: (String) -> Unit) { 
        viewModelScope.launch { 
            try { 
                val result = firebaseAuth.signInWithEmailAndPassword(email.trim().lowercase(), pass.trim()).await() 
                val user = result.user
                if (user != null) { handleSuccessfulLogin(user, email.trim().lowercase(), pass.trim(), saveCredentials) ; onSuccess() } else throw Exception("Error")
            } catch (e: Exception) { loginLocal(email.trim().lowercase(), pass.trim(), saveCredentials) ; onSuccess() } 
        } 
    }

    private fun handleSuccessfulLogin(user: com.google.firebase.auth.FirebaseUser?, email: String, pass: String, saveCredentials: Boolean) {
        if (saveCredentials) {
            prefs.edit().apply {
                putString("saved_email", email)
                putString("saved_password", pass)
                putBoolean("remember_me", true)
            }.apply()
        }
        
        isLoggedIn.value = true
        userEmail.value = email.lowercase()
        userName.value = user?.displayName ?: email.split("@").get(0)
        userPhotoUrl.value = user?.photoUrl?.toString()
        
        // Update local states for flows/combine
        _localUserName.value = userName.value
        _localUserPhoto.value = userPhotoUrl.value
        _localUserXp.value = userXp.value.toLong()

        prefs.edit().apply {
            putString("user_name", userName.value)
            putString("user_photo_url", userPhotoUrl.value)
        }.apply()
        
        fetchUserDataFromFirestore()
        fetchLeaderboard()
        startFriendRequestListener()
        startFriendsListener()
        startLiveFocusListener()
        startPresenceHeartbeat()
    }

    fun loginLocal(email: String, pass: String, saveCredentials: Boolean) {
        val cleanEmail = email.trim().lowercase()
        if (saveCredentials) {
            prefs.edit().apply {
                putString("saved_email", cleanEmail)
                putString("saved_password", pass)
                putBoolean("remember_me", true)
            }.apply()
        }
        
        isLoggedIn.value = true
        userEmail.value = cleanEmail
        val nameFromEmail = cleanEmail.split("@").getOrNull(0) ?: "User"
        userName.value = prefs.getString("user_name", nameFromEmail) ?: nameFromEmail
        userPhotoUrl.value = prefs.getString("user_photo_url", null)
        
        // Sync local states used in combine/flows
        _localUserName.value = userName.value
        _localUserPhoto.value = userPhotoUrl.value
        _localUserXp.value = userXp.value.toLong()
        
        prefs.edit().apply {
            putString("user_name", userName.value)
            putBoolean("is_local_mode", true)
        }.apply()
        
        // Firebase Anonim Giriş Entegrasyonu (Misafir kullanıcı verilerini Firebase'e kaydeder)
        viewModelScope.launch {
            try {
                if (firebaseAuth.currentUser == null) {
                    firebaseAuth.signInAnonymously().await()
                    android.util.Log.d("FocusPathAuth", "Firebase Anonymous Login Success! UID: ${firebaseAuth.currentUser?.uid}")
                }
                // Misafir kullanıcının verilerini bulutla senkronize etmeye başla
                syncProfileToFirestore()
                fetchUserDataFromFirestore()
                fetchLeaderboard()
            } catch (e: Exception) {
                android.util.Log.e("FocusPathAuth", "Firebase Anonymous Login Failed: ${e.message}")
                fetchLeaderboardLocal()
            }
        }
        
        viewModelScope.launch {
            delay(1000)
            initializeWorkers()
            startFriendRequestListener()
            startFriendsListener()
            startLiveFocusListener()
            startPresenceHeartbeat()
            listenForIncomingMessages()
        }
    }

    fun registerEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) { 
        viewModelScope.launch { 
            try { 
                val result = firebaseAuth.createUserWithEmailAndPassword(email.trim().lowercase(), pass.trim()).await() 
                val user = result.user
                isLoggedIn.value = true ; userEmail.value = user?.email?.lowercase() ?: "" ; userName.value = user?.email?.split("@")?.get(0) ?: "User"
                prefs.edit().apply { putString("user_name", userName.value) ; putString("user_photo_url", userPhotoUrl.value) }.apply()
                syncProfileToFirestore() ; syncXpToFirestore() ; fetchLeaderboard() ; startFriendRequestListener() ; startFriendsListener() ; startLiveFocusListener() ; startPresenceHeartbeat() ; onSuccess() 
            } catch (e: Exception) { onError(e.localizedMessage ?: "Error") } 
        } 
    }

    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) { viewModelScope.launch { try { firebaseAuth.sendPasswordResetEmail(email).await() ; onSuccess() } catch (e: Exception) { onError(e.localizedMessage ?: "Error") } } }

    fun sendFriendRequest(targetEmail: String, targetName: String, targetUid: String? = null, onSuccess: () -> Unit, onError: (String) -> Unit) {
        android.util.Log.d("FocusPathFriend", "sendFriendRequest called for: $targetEmail")
        
        val myEmail = userEmail.value.lowercase()
        val cleanTargetEmail = targetEmail.trim().lowercase()
        
        if (myEmail.isBlank()) {
            onError("İstek göndermek için geçerli bir e-posta adresiniz olmalı.")
            return
        }

        if (myEmail == cleanTargetEmail) {
            onError("Kendinizi arkadaş olarak ekleyemezsiniz.")
            return
        }
        
        val myUid = firebaseAuth.currentUser?.uid ?: "local_${myEmail.hashCode()}"
        
        val myData = mapOf(
            "uid" to myUid,
            "name" to userName.value,
            "email" to myEmail,
            "score" to userXp.value.toLong(),
            "photoUrl" to (userPhotoUrl.value ?: ""),
            "timestamp" to System.currentTimeMillis()
        )
        
        android.util.Log.d("FocusPathFriend", "Sending request from $myEmail to $cleanTargetEmail")
        
        firestore.collection("users")
            .document(cleanTargetEmail)
            .collection("friend_requests")
            .document(myUid)
            .set(myData)
            .addOnSuccessListener {
                android.util.Log.d("FocusPathFriend", "Request sent successfully")
                completeOnboardingTask("add_friend")
                onSuccess()
            }
            .addOnFailureListener {
                android.util.Log.e("FocusPathFriend", "Request failed", it)
                onError("İstek gönderilemedi: ${it.localizedMessage}")
            }
    }

    fun acceptFriendRequest(reqUser: LeaderboardUser, onSuccess: () -> Unit) {
        val myEmail = userEmail.value.lowercase()
        if (myEmail.isBlank()) return
        
        android.util.Log.d("FocusPathFriend", "acceptFriendRequest from: ${reqUser.email}")
        
        val myUid = firebaseAuth.currentUser?.uid ?: "local_${myEmail.hashCode()}"
        
        val friendData = mapOf(
            "uid" to reqUser.uid,
            "name" to reqUser.name,
            "email" to reqUser.email.lowercase(),
            "score" to reqUser.score,
            "photoUrl" to (reqUser.photoUrl ?: "")
        )
        val myData = mapOf(
            "uid" to myUid,
            "name" to userName.value,
            "email" to myEmail,
            "score" to userXp.value.toLong(),
            "photoUrl" to (userPhotoUrl.value ?: "")
        )
        
        val myDocId = firebaseAuth.currentUser?.uid ?: myEmail
        
        firestore.collection("users").document(myDocId).collection("friends").document(reqUser.uid).set(friendData)
            .addOnSuccessListener {
                firestore.collection("users").document(reqUser.uid).collection("friends").document(myUid).set(myData)
                    .addOnSuccessListener {
                        firestore.collection("users").document(myDocId).collection("friend_requests").document(reqUser.uid).delete()
                        completeOnboardingTask("add_friend")
                        onSuccess()
                        android.util.Log.d("FocusPathFriend", "Friend request accepted and synced")
                    }
            }
            .addOnFailureListener {
                android.util.Log.e("FocusPathFriend", "Accept request failed", it)
            }
    }

    fun removeFriend(friendUid: String, friendEmail: String, onSuccess: () -> Unit) {
        val currentUser = firebaseAuth.currentUser ?: return
        firestore.collection("users").document(currentUser.uid).collection("friends").document(friendUid).delete().addOnSuccessListener { firestore.collection("users").document(friendUid).collection("friends").document(currentUser.uid).delete().addOnSuccessListener { onSuccess() } }
    }

    fun startFriendsListener() {
        val email = userEmail.value
        if (email.isBlank()) return
        friendsRegistration?.remove()
        friendsRegistration = firestore.collection("users").document(email.lowercase()).collection("friends").addSnapshotListener { snapshot, e -> if (snapshot != null) { val list = snapshot.documents.mapNotNull { it.toObject(LeaderboardUser::class.java) } ; friendsList.clear() ; friendsList.addAll(list) ; if (list.isNotEmpty()) completeOnboardingTask("add_friend") } }
    }

    fun startFriendRequestListener() {
        val email = userEmail.value
        if (email.isBlank()) return
        friendRequestRegistration?.remove()
        friendRequestRegistration = firestore.collection("users").document(email.lowercase()).collection("friend_requests").addSnapshotListener { snapshot, e -> snapshot?.documentChanges?.forEach { if (it.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) showLocalNotification("Yeni Arkadaş İsteği", "Yeni bir istek aldın!") } }
    }

    private fun showLocalNotification(title: String, message: String) {
        val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "focuspath_notifications"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) nm.createNotificationChannel(android.app.NotificationChannel(channelId, "FocusPath", android.app.NotificationManager.IMPORTANCE_HIGH))
        nm.notify(System.currentTimeMillis().toInt(), androidx.core.app.NotificationCompat.Builder(application, channelId).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(message).setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build())
    }

    fun updateItemPosition(id: String, offset: Offset, isDragging: Boolean = true, floorSize: Float = 300f) {
        val limit = (floorSize / 2f) - 40f 
        itemPositions[id] = Offset(offset.x.coerceIn(-limit, limit), offset.y.coerceIn(-limit, limit))
        if (!isDragging) saveLayoutToPrefs("v2")
    }

    private fun saveLayoutToPrefs(slotName: String) {
        prefs.edit().putString("office_layout_$slotName", itemPositions.entries.joinToString("|") { "${it.key}:${it.value.x},${it.value.y}" }).apply()
    }

    fun toggleItemVisibility(id: String, defaultPos: Offset = Offset(0f, 0f)) {
        if (visibleItems.contains(id)) visibleItems.remove(id) else { visibleItems.add(id) ; if (!itemPositions.containsKey(id)) itemPositions[id] = defaultPos }
        prefs.edit().putStringSet("visible_items", visibleItems.toSet()).apply()
        saveLayoutToPrefs("current") ; if (id.startsWith("desk_setup_")) initializeWorkers()
    }

    fun resetOfficePositions() { 
        currentLayoutName.value = "Standart Ofis" ; itemPositions.clear() ; visibleItems.clear()
        listOf(-90f to 0f, 0f to 0f, 90f to 0f).forEachIndexed { i, (x, y) -> val deskId = "desk_setup_$i" ; itemPositions[deskId] = Offset(x, y) ; visibleItems.add(deskId) }
        prefs.edit().putStringSet("visible_items", visibleItems.toSet()).apply() ; saveLayoutToPrefs("v2")
    }

    fun saveCurrentLayout(slotName: String) { saveLayoutToPrefs("current") }
    fun loadLayout(slotName: String) {}

    fun setNotificationEnabled(enabled: Boolean) { isNotificationEnabled.value = enabled ; prefs.edit().putBoolean("is_notification_enabled", enabled).apply() }
    fun setAlarmSound(sound: String) { alarmSound.value = sound ; prefs.edit().putString("alarm_sound", sound).apply() }
    fun setAlarmVolume(volume: Float) { alarmVolume.floatValue = volume ; prefs.edit().putFloat("alarm_volume", volume).apply() }
    fun setWaterReminder(enabled: Boolean, interval: Int, context: Context) { isWaterReminderEnabled.value = enabled ; waterReminderInterval.intValue = interval ; prefs.edit().putBoolean("is_water_reminder_enabled", enabled).putInt("water_reminder_interval", interval).apply() ; if (enabled) ReminderUtil.scheduleWaterReminder(context, interval) else ReminderUtil.cancelWaterReminder(context) }
    fun setAutoDndEnabled(context: Context, enabled: Boolean) { isAutoDndEnabled.value = enabled ; prefs.edit().putBoolean("is_auto_dnd_enabled", enabled).apply() }

    fun getInstalledApps(): List<AppInfo> {
        val pm = application.packageManager
        return pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA).filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }.map { AppInfo(name = it.loadLabel(pm).toString(), packageName = it.packageName, icon = it.loadIcon(pm)) }.sortedBy { it.name }
    }

    fun addHabit(title: String, description: String = "", icon: String = "Star", color: String = "#FF6200EE") {
        viewModelScope.launch { taskDao.insertHabit(HabitEntity(title = title, description = description, iconName = icon, colorHex = color)) }
    }

    fun toggleHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis() ; val habitSdf = SimpleDateFormat("yyyyMMdd", Locale.US)
            val habitToday = habitSdf.format(Date(now)).toLong()
            if (habit.lastCompletedDate > 0 && habitSdf.format(Date(habit.lastCompletedDate)).toLong() == habitToday) return@launch
            val newStreak = if (habit.lastCompletedDate > 0 && habitSdf.format(Date(now - 86400000L)).toLong() == habitSdf.format(Date(habit.lastCompletedDate)).toLong()) habit.streak + 1 else 1
            taskDao.updateHabit(habit.copy(streak = newStreak, longestStreak = if (newStreak > habit.longestStreak) newStreak else habit.longestStreak, lastCompletedDate = now))
            if (prefs.getInt("habits_rewarded_count_$todayStr", 0) < 5) { addXp(15) ; addCoins(5) ; prefs.edit().putInt("habits_rewarded_count_$todayStr", prefs.getInt("habits_rewarded_count_$todayStr", 0) + 1).apply() ; showConfetti.value = true ; delay(2000) ; showConfetti.value = false }
        }
    }

    fun deleteHabit(habit: HabitEntity) { viewModelScope.launch { taskDao.deleteHabit(habit) } }

    fun breakTaskWithAi(parentTask: TaskEntity, isEnglish: Boolean) {
        if (isBreakingTask.value) return
        isBreakingTask.value = true
        viewModelScope.launch {
            try {
                val prompt = if (isEnglish) "Break down into 5 sub-tasks: '${parentTask.title}'" else "'${parentTask.title}' görevini 5 alt göreve böl."
                val response = withContext(Dispatchers.IO) { generativeModel.generateContent(prompt) }
                response.text?.split("\n")?.filter { it.isNotBlank() }?.take(5)?.forEach { taskDao.insertTask(TaskEntity(title = it.trim(), parentId = parentTask.id, category = parentTask.category, priority = parentTask.priority, dueDate = parentTask.dueDate)) }
                withContext(Dispatchers.Main) { Toast.makeText(application, "Done!", Toast.LENGTH_SHORT).show() }
            } finally { isBreakingTask.value = false }
        }
    }

    // --- INIT BLOCK ---

    init {
        waterCupsDrunk.intValue = prefs.getInt("water_cups_drunk_$todayStr", 0)
        val lastTime = prefs.getLong("last_water_reminder_time", 0L)
        isWaterRewardAvailable.value = lastTime > 0 && (System.currentTimeMillis() - lastTime < 2 * 60 * 60 * 1000L)

        loadOnboardingTasks()
        
        viewModelScope.launch {
            delay(1500)
            setupSoundPool()
            checkRemoteUpdate()
        }
        
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null && !isLoggedIn.value) {
                startUserEnvironment(user)
            }
        }
        
        firebaseAuth.currentUser?.let { user ->
            if (!isLoggedIn.value) {
                startUserEnvironment(user)
            }
        } ?: run {
            if (prefs.getBoolean("is_local_mode", false)) {
                isLoggedIn.value = true
                userEmail.value = prefs.getString("saved_email", "") ?: ""
                userName.value = prefs.getString("user_name", "User") ?: "User"
                fetchLeaderboardLocal()
                
                val localTeamJson = prefs.getString("local_team_data", null)
                if (localTeamJson != null) {
                    try {
                        val team = Gson().fromJson(localTeamJson, Team::class.java)
                        userTeam.value = team
                        teamMembers.clear()
                        teamMembers.add(LeaderboardUser(uid = "me", name = userName.value, email = userEmail.value, score = userXp.value.toLong()))
                    } catch (e: Exception) {}
                }
            }
        }
        
        viewModelScope.launch {
            leaderboard.collect {
                updateFriendWorkers()
            }
        }

        if (!prefs.contains("office_layout_v2")) {
            listOf(-90f to 0f, 0f to 0f, 90f to 0f).forEachIndexed { i, (x, y) ->
                itemPositions["desk_setup_$i"] = Offset(x, y)
            }
            saveLayoutToPrefs("v2")
        }

        initializeWorkers()
        startWorkerSimulation()
        syncTimerWithService()
        checkPlantHealth()
        fetchYesterdayStats()
        updateAmbientSounds()
        loadDopamineMenu()
        autoLogin()
        startBotSimulation()
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

data class DopamineItem(
    val id: String = UUID.randomUUID().toString(),
    val titleTr: String,
    val titleEn: String,
    val category: String, 
    val icon: String,
    val actionType: String? = null 
)
