package com.focuspath.shared

import com.focuspath.shared.model.Task
import com.focuspath.shared.model.LeaderboardUser
import com.focuspath.shared.model.User
import com.focuspath.shared.model.WorkerInfo
import com.focuspath.shared.model.WorkerAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedViewModel {
    private val _user = MutableStateFlow(User(name = "mesutbay03", level = 1094, xp = 1094, coins = 737))
    val user: StateFlow<User> = _user.asStateFlow()

    private val _lifetimeCoins = MutableStateFlow(1250)
    val lifetimeCoins: StateFlow<Int> = _lifetimeCoins.asStateFlow()

    private val _waterCupsDrunk = MutableStateFlow(3)
    val waterCupsDrunk: StateFlow<Int> = _waterCupsDrunk.asStateFlow()

    private val _isWaterRewardAvailable = MutableStateFlow(true)
    val isWaterRewardAvailable: StateFlow<Boolean> = _isWaterRewardAvailable.asStateFlow()

    private val _isWaterReminderEnabled = MutableStateFlow(true)
    val isWaterReminderEnabled: StateFlow<Boolean> = _isWaterReminderEnabled.asStateFlow()

    private val _waterReminderInterval = MutableStateFlow(2)
    val waterReminderInterval: StateFlow<Int> = _waterReminderInterval.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _incomingTasks = MutableStateFlow<List<Task>>(emptyList())
    val incomingTasks: StateFlow<List<Task>> = _incomingTasks.asStateFlow()

    private val _yesterdayFocusMins = MutableStateFlow(45)
    val yesterdayFocusMins: StateFlow<Int> = _yesterdayFocusMins.asStateFlow()

    private val _todayChallengeTarget = MutableStateFlow(60)
    val todayChallengeTarget: StateFlow<Int> = _todayChallengeTarget.asStateFlow()

    private val _loginStreak = MutableStateFlow(3)
    val loginStreak: StateFlow<Int> = _loginStreak.asStateFlow()

    private val _isDailyRewardClaimed = MutableStateFlow(false)
    val isDailyRewardClaimed: StateFlow<Boolean> = _isDailyRewardClaimed.asStateFlow()

    private val _successSessions = MutableStateFlow(0)
    val successSessions: StateFlow<Int> = _successSessions.asStateFlow()

    private val _failedSessions = MutableStateFlow(0)
    val failedSessions: StateFlow<Int> = _failedSessions.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardUser>> = _leaderboard.asStateFlow()
    
    private val _workers = MutableStateFlow<List<WorkerInfo>>(emptyList())
    val workers: StateFlow<List<WorkerInfo>> = _workers.asStateFlow()

    private val _timeLeft = MutableStateFlow(1500L) 
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()
    
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    init {
        _tasks.value = listOf(
            Task(id = 1, title = "Design System Update", priority = 2, estimatedMinutes = 45),
            Task(id = 2, title = "Sync with iOS Team", priority = 1, estimatedMinutes = 30),
            Task(id = 3, title = "Fix Multiplatform Bugs", priority = 2, isCompleted = true),
            Task(id = 4, title = "Refactor Shared UI", priority = 0)
        )
        
        _leaderboard.value = listOf(
            LeaderboardUser(uid = "1", name = "Yigit", score = 1250, isFocusing = true),
            LeaderboardUser(uid = "2", name = "AI Coach", score = 9999, isFocusing = false),
            LeaderboardUser(uid = "3", name = "Beta Tester", score = 450)
        )
        
        _workers.value = listOf(
            WorkerInfo(id = "w1", name = "Yigit", isMe = true, isFocusing = true, x = 0f, y = 0f),
            WorkerInfo(id = "w2", name = "Buddy", isFocusing = true, x = -100f, y = 50f),
            WorkerInfo(id = "w3", name = "Intern", isFocusing = false, x = 100f, y = 50f)
        )

        _incomingTasks.value = listOf(
            Task(id = 101, title = "30 dk Kitap Oku", priority = 1),
            Task(id = 102, title = "Suyu İhmal Etme", priority = 0)
        )
    }

    fun toggleTask(task: Task) {
        _tasks.value = _tasks.value.map {
            if (it.id == task.id) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    fun completeIncomingTask(task: Task) {
        _incomingTasks.value = _incomingTasks.value.filter { it.id != task.id }
        _user.value = _user.value.copy(xp = _user.value.xp + 50, coins = _user.value.coins + 50)
    }

    fun drinkWater() {
        _waterCupsDrunk.value += 1
        _user.value = _user.value.copy(coins = _user.value.coins + 5)
    }

    fun setWaterReminder(enabled: Boolean, interval: Int) {
        _isWaterReminderEnabled.value = enabled
        _waterReminderInterval.value = interval
    }

    fun claimDailyReward() {
        _isDailyRewardClaimed.value = true
        _user.value = _user.value.copy(coins = _user.value.coins + 20)
        _loginStreak.value += 1
    }

    val radioStations = listOf("Lofi", "Jazz", "Nature", "White Noise")
    private val _currentRadioStation = MutableStateFlow<String?>(null)
    val currentRadioStation: StateFlow<String?> = _currentRadioStation.asStateFlow()

    fun playRadio(station: String) {
        _currentRadioStation.value = station
    }

    fun stopRadio() {
        _currentRadioStation.value = null
    }

    fun toggleTimer() {
        _isTimerRunning.value = !_isTimerRunning.value
    }
}
