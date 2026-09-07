package com.focuspath.backend.service

import com.focuspath.backend.dto.TaskSyncRequest
import com.focuspath.backend.model.Task
import com.focuspath.backend.model.User
import com.focuspath.backend.repository.TaskRepository
import com.focuspath.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) {

    fun getTasksForUser(email: String): List<Task> {
        val user = userRepository.findByEmail(email).orElse(null) ?: return emptyList()
        return taskRepository.findAllByUser(user)
    }

    @Transactional
    fun syncTasks(request: TaskSyncRequest): List<Task> {
        val user = userRepository.findByEmail(request.email).orElseGet {
            userRepository.save(User(email = request.email))
        }

        val existingTasks = taskRepository.findAllByUser(user)
        val existingTaskMap = existingTasks.filter { it.localId != null }.associateBy { it.localId }

        val tasksToSave = request.tasks.map { dto ->
            val existing = existingTaskMap[dto.id]
            Task(
                id = existing?.id,
                localId = dto.id,
                title = dto.title,
                notes = dto.notes,
                category = dto.category,
                priority = dto.priority,
                isCompleted = dto.isCompleted,
                dueDate = Instant.ofEpochMilli(dto.dueDate),
                parentId = dto.parentId,
                user = user
            )
        }

        return taskRepository.saveAll(tasksToSave)
    }
}
