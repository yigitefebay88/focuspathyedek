package com.focuspath.app.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val notes: String = "",
    val category: String = "Genel",
    val priority: Int = 1,
    var isCompleted: Boolean = false,
    val dueDate: Long = System.currentTimeMillis(),
    val rewardCoins: Int = 0,
    val energyLevel: Int = 1,
    val estimatedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val parentId: Long = 0
)
