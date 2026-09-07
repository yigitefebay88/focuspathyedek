package com.focuspath.app.core.domain.model

data class Task(
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val category: String = "Genel",
    val priority: Int = 1,
    val isCompleted: Boolean = false,
    val dueDate: Long,
    val rewardCoins: Int = 0,
    val energyLevel: Int = 1,
    val estimatedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val parentId: Long = 0
)
