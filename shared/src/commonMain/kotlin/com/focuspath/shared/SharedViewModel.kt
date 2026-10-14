package com.focuspath.shared

import com.focuspath.shared.model.LeaderboardUser
import com.focuspath.shared.model.Task
import com.focuspath.shared.model.User
import com.focuspath.shared.model.WorkerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SharedViewModel {
    private val _user = MutableStateFlow(User(name = "Yigit", coins = 250, level = 3, xp = 520))
    val user: StateFlow<User> = _user.asStateFlow()

    private val _tasks = MutableStateFlow(
        listOf(
            Task(id = 1, title = "Kotlin Multiplatform Projesini Tamamla", category = "İş", priority = 2, isCompleted = false),
            Task(id = 2, title = "30 Dakika Odaklanma Seansı", category = "Sağlık", priority = 1, isCompleted = true),
            Task(id = 3, title = "Su İç (2 Bardak)", category = "Sağlık", priority = 0, isCompleted = false)
        )
    )
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _leaderboard = MutableStateFlow(
        listOf(
            LeaderboardUser(name = "Ahmet Yılmaz", score = 1250, level = 5),
            LeaderboardUser(name = "Zeynep Kaya", score = 980, level = 4),
            LeaderboardUser(name = "Mehmet Demir", score = 820, level = 3)
        )
    )
    val leaderboard: StateFlow<List<LeaderboardUser>> = _leaderboard.asStateFlow()

    private val _incomingTasks = MutableStateFlow(
        listOf(
            Task(id = 101, title = "Kod İncelemesi (Peer Review)", category = "İş", priority = 1)
        )
    )
    val incomingTasks: StateFlow<List<Task>> = _incomingTasks.asStateFlow()

    private val _isDailyRewardClaimed = MutableStateFlow(false)
    val isDailyRewardClaimed: StateFlow<Boolean> = _isDailyRewardClaimed.asStateFlow()

    private val _loginStreak = MutableStateFlow(4)
    val loginStreak: StateFlow<Int> = _loginStreak.asStateFlow()

    private val _workers = MutableStateFlow(
        listOf(
            WorkerInfo(id = "1", name = "Ahmet", position = "Developer", isMe = true, isFocusing = true),
            WorkerInfo(id = "2", name = "Elif", position = "Designer", isFocusing = false),
            WorkerInfo(id = "3", name = "Can", position = "Tester", isFocusing = true)
        )
    )
    val workers: StateFlow<List<WorkerInfo>> = _workers.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _timeLeft = MutableStateFlow(25 * 60 * 1000L)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _currentRadioStation = MutableStateFlow<String?>(null)
    val currentRadioStation: StateFlow<String?> = _currentRadioStation.asStateFlow()

    private val _waterCupsDrunk = MutableStateFlow(5)
    val waterCupsDrunk: StateFlow<Int> = _waterCupsDrunk.asStateFlow()

    private val _officeLevel = MutableStateFlow(2)
    val officeLevel: StateFlow<Int> = _officeLevel.asStateFlow()

    fun toggleTask(task: Task) {
        _tasks.update { currentList ->
            currentList.map {
                if (it.id == task.id) {
                    val nextState = !it.isCompleted
                    if (nextState) addXpAndCoins(30, 15)
                    it.copy(isCompleted = nextState)
                } else it
            }
        }
    }

    fun addTask(title: String, category: String, priority: Int) {
        val newTask = Task(
            id = ((_tasks.value.maxOfOrNull { it.id } ?: 0L) + 1),
            title = title,
            category = category,
            priority = priority,
            isCompleted = false
        )
        _tasks.update { listOf(newTask) + it }
    }

    fun deleteTask(task: Task) {
        _tasks.update { list -> list.filter { it.id != task.id } }
    }

    fun completeIncomingTask(task: Task) {
        _incomingTasks.update { list -> list.filter { it.id != task.id } }
        addXpAndCoins(50, 50)
    }

    fun addXpAndCoins(xp: Int, coins: Int) {
        _user.update { u ->
            val newXp = u.xp + xp
            val newCoins = u.coins + coins
            val newLevel = if (newXp >= u.level * 200) u.level + 1 else u.level
            u.copy(xp = newXp, coins = newCoins, level = newLevel)
        }
    }

    fun claimDailyReward() {
        _isDailyRewardClaimed.value = true
        _loginStreak.update { it + 1 }
        addXpAndCoins(25, 50)
    }

    fun toggleTimer() {
        _isTimerRunning.update { !it }
    }

    fun playRadio(station: String) {
        _currentRadioStation.value = station
    }

    fun stopRadio() {
        _currentRadioStation.value = null
    }

    fun drinkWater() {
        _waterCupsDrunk.update { minOf(it + 1, 8) }
        addXpAndCoins(10, 5)
    }

    fun upgradeOffice() {
        val cost = _officeLevel.value * 500
        if (_user.value.coins >= cost) {
            _user.update { it.copy(coins = it.coins - cost) }
            _officeLevel.update { it + 1 }
        }
    }
}
