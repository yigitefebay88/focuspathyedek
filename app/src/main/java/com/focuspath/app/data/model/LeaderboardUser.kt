package com.focuspath.app.data.model

import com.google.firebase.firestore.PropertyName

data class LeaderboardUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val score: Long = 0L,
    val photoUrl: String? = null,
    val timestamp: Long = 0L,
    val sessionDuration: Int = 0,
    @get:PropertyName("focusing") @set:PropertyName("focusing") var isFocusing: Boolean = false,
    val latestEmoji: String? = null,
    val emojiTime: Long = 0L,
    val focusBuddyEmail: String? = null,
    val currentTaskTitle: String? = null
)
