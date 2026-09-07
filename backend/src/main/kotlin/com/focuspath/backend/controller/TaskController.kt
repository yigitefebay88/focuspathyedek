package com.focuspath.backend.controller

import com.focuspath.backend.dto.TaskSyncRequest
import com.focuspath.backend.model.Task
import com.focuspath.backend.service.TaskService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "Task management and sync")
class TaskController(private val taskService: TaskService) {

    @GetMapping
    @Operation(summary = "Get all tasks for a user")
    fun getTasks(@RequestParam email: String): List<Task> {
        return taskService.getTasksForUser(email)
    }

    @PostMapping("/sync")
    @Operation(summary = "Bulk sync tasks for a user")
    fun syncTasks(@RequestBody request: TaskSyncRequest): List<Task> {
        return taskService.syncTasks(request)
    }
}
