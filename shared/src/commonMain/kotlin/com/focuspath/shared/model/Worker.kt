package com.focuspath.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class WorkerAction {
    IDLE, WORKING, TYPING, MOUSE, THINKING, RESTING, WALKING, COFFEE, MEETING, COMPLETED, ASKING
}

@Serializable
data class WorkerInfo(
    val id: String,
    val name: String,
    val position: String = "Intern",
    val deskId: String = "",
    val currentAction: WorkerAction = WorkerAction.IDLE,
    val x: Float = 0f,
    val y: Float = 0f,
    val isFocusing: Boolean = false,
    val totalWorkTime: Long = 0,
    val productivity: Int = 100,
    val monitorContent: String = "...",
    val photoUrl: String? = null,
    val isMe: Boolean = false,
    val latestEmoji: String? = null,
    val emojiTime: Long = 0,
    val isFacingRight: Boolean = true,
    val interactionText: String? = null,
    val isLiveUser: Boolean = false
)
