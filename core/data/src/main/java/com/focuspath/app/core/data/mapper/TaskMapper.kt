package com.focuspath.app.core.data.mapper

import com.focuspath.app.core.data.local.TaskEntity
import com.focuspath.app.core.domain.model.Task

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        notes = notes,
        category = category,
        priority = priority,
        isCompleted = isCompleted,
        dueDate = dueDate,
        rewardCoins = rewardCoins,
        energyLevel = energyLevel,
        estimatedMinutes = estimatedMinutes,
        actualMinutes = actualMinutes,
        parentId = parentId
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        notes = notes,
        category = category,
        priority = priority,
        isCompleted = isCompleted,
        dueDate = dueDate,
        rewardCoins = rewardCoins,
        energyLevel = energyLevel,
        estimatedMinutes = estimatedMinutes,
        actualMinutes = actualMinutes,
        parentId = parentId
    )
}
