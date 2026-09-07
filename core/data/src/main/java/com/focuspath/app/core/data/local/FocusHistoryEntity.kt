package com.focuspath.app.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_history")
data class FocusHistoryEntity(
    @PrimaryKey val date: String,
    val totalFocusMinutes: Int = 0,
    val tasksCompleted: Int = 0,
    val sessionsCompleted: Int = 0,
    val sessionsInterrupted: Int = 0,
    val categoryDistribution: String = ""
)
