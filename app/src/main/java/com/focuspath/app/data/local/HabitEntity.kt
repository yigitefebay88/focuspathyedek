package com.focuspath.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val iconName: String = "Star",
    val colorHex: String = "#FF6200EE",
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: Long = 0, // YYYYMMDD formatında tutmak daha kolay olabilir veya millis
    val createdAt: Long = System.currentTimeMillis()
)
