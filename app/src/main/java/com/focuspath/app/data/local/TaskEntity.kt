package com.focuspath.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val notes: String = "",
    val category: String = "Genel",
    val priority: Int = 1,
    @get:PropertyName("completed") @set:PropertyName("completed") var isCompleted: Boolean = false,
    val dueDate: Long = System.currentTimeMillis(),
    val rewardCoins: Int = 0,
    val energyLevel: Int = 1, // 0: Düşük, 1: Normal, 2: Yüksek
    val estimatedMinutes: Int = 0,
    val actualMinutes: Int = 0
)
