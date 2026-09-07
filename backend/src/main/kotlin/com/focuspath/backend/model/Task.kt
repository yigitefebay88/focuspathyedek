package com.focuspath.backend.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "tasks")
data class Task(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val localId: Long? = null, // ID from Android App

    val title: String,
    val notes: String,
    val category: String,
    val priority: Int,
    val isCompleted: Boolean,
    val dueDate: Instant,
    val parentId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    var user: User? = null
)
