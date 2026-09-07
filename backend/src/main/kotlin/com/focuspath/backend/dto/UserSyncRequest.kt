package com.focuspath.backend.dto

data class UserSyncRequest(
    val email: String,
    val username: String,
    val xp: Long,
    val coins: Int,
    val level: Int,
    val photoUrl: String?
)
