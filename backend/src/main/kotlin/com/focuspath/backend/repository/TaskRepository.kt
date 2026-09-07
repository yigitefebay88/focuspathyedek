package com.focuspath.backend.repository

import com.focuspath.backend.model.Task
import com.focuspath.backend.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : JpaRepository<Task, Long> {
    fun findAllByUser(user: User): List<Task>
}
