package com.focuspath.backend.dto

data class TaskSyncRequest(
    val email: String,
    val tasks: List<TaskDto>
)

data class TaskDto(
    val id: Long?,
    val title: String,
    val notes: String,
    val category: String,
    val priority: Int,
    val isCompleted: Boolean,
    val dueDate: Long,
    val parentId: Long
)
