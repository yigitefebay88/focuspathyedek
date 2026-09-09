package com.focuspath.shared

import com.focuspath.shared.model.Task
import com.focuspath.shared.model.LeaderboardUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedViewModel {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardUser>> = _leaderboard.asStateFlow()

    init {
        // Demo data
        _tasks.value = listOf(
            Task(id = 1, title = "Design System Update", priority = 2, estimatedMinutes = 45),
            Task(id = 2, title = "Sync with iOS Team", priority = 1, estimatedMinutes = 30),
            Task(id = 3, title = "Fix Multiplatform Bugs", priority = 2, isCompleted = true),
            Task(id = 4, title = "Refactor Shared UI", priority = 0)
        )
        
        _leaderboard.value = listOf(
            LeaderboardUser(name = "Yigit", score = 1250, isFocusing = true),
            LeaderboardUser(name = "AI Coach", score = 9999, isFocusing = false),
            LeaderboardUser(name = "Beta Tester", score = 450)
        )
    }

    fun toggleTask(task: Task) {
        _tasks.value = _tasks.value.map {
            if (it.id == task.id) it.copy(isCompleted = !it.isCompleted) else it
        }
    }
}
