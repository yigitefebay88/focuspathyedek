package com.focuspath.app.core.data.repository

import com.focuspath.app.core.data.local.TaskDao
import com.focuspath.app.core.data.mapper.toDomain
import com.focuspath.app.core.data.mapper.toEntity
import com.focuspath.app.core.domain.model.Task
import com.focuspath.app.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTaskById(id: Long): Task? {
        return taskDao.getAllTasksOnce().find { it.id == id }?.toDomain()
    }

    override suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }
}
