package com.focuspath.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val xp: Int = 0,
    val coins: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val isPremium: Boolean = false
)
