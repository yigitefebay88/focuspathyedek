package com.focuspath.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()

    // FOCUS HISTORY
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusHistory(history: FocusHistoryEntity)

    @Query("SELECT * FROM focus_history WHERE date >= :startDate")
    fun getFocusHistory(startDate: String): Flow<List<FocusHistoryEntity>>

    @Query("SELECT * FROM focus_history WHERE date >= :startDate")
    suspend fun getFocusHistoryOnce(startDate: String): List<FocusHistoryEntity>

    @Query("SELECT * FROM focus_history WHERE date = :date LIMIT 1")
    suspend fun getFocusHistoryByDate(date: String): FocusHistoryEntity?

    @Query("SELECT SUM(totalFocusMinutes) FROM focus_history")
    fun getTotalFocusMinutes(): Flow<Int?>

    @Query("SELECT SUM(tasksCompleted) FROM focus_history")
    fun getTotalTasksCompleted(): Flow<Int?>
}
