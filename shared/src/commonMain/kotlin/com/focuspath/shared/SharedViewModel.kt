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
    private val _user = MutableStateFlow(User(name = "Kullanıcı", coins = 120, level = 2, xp = 350))
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

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _incomingTasks = MutableStateFlow(
        listOf(
            Task(id = 101, title = "Kod İncelemesi (Peer Review)", category = "İş", priority = 1)
        )
    )
    val incomingTasks: StateFlow<List<Task>> = _incomingTasks.asStateFlow()

    private val _isDailyRewardClaimed = MutableStateFlow(false)
    val isDailyRewardClaimed: StateFlow<Boolean> = _isDailyRewardClaimed.asStateFlow()

    private val _loginStreak = MutableStateFlow(3)
    val loginStreak: StateFlow<Int> = _loginStreak.asStateFlow()

    private val _workers = MutableStateFlow(
        listOf(
            WorkerInfo(id = "1", name = "Ahmet", position = "Developer", isMe = true),
            WorkerInfo(id = "2", name = "Elif", position = "Designer")
        )
    )
    val workers: StateFlow<List<WorkerInfo>> = _workers.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _currentRadioStation = MutableStateFlow<String?>(null)
    val currentRadioStation: StateFlow<String?> = _currentRadioStation.asStateFlow()

    private val _waterCupsDrunk = MutableStateFlow(4)
    val waterCupsDrunk: StateFlow<Int> = _waterCupsDrunk.asStateFlow()

    fun toggleTask(task: Task) {
        _tasks.update { currentList ->
            currentList.map {
                if (it.id == task.id) it.copy(isCompleted = !it.isCompleted) else it
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
        addXpAndCoins(20, 50)
    }

    fun playRadio(station: String) {
        _currentRadioStation.value = station
    }

    fun stopRadio() {
        _currentRadioStation.value = null
    }

    fun drinkWater() {
        _waterCupsDrunk.update { minOf(it + 1, 8) }
        _user.update { it.copy(coins = it.coins + 5) }
    }
}
