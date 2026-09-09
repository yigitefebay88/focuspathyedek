package com.focuspath.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val score: Long = 0L,
    val level: Int = 1,
    var photoUrl: String? = null,
    val timestamp: Long = 0L,
    val sessionDuration: Int = 0,
    val isFocusing: Boolean = false,
    val latestEmoji: String? = null,
    val emojiTime: Long = 0L,
    val focusBuddyEmail: String? = null,
    val currentTaskTitle: String? = null,
    val teamId: String? = null
)
