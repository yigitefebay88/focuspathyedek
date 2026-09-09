package com.focuspath.shared.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserResponse(
    @SerialName("email") val email: String,
    @SerialName("username") val username: String,
    @SerialName("xp") val xp: Long,
    @SerialName("coins") val coins: Int,
    @SerialName("level") val level: Int,
    @SerialName("photoUrl") val photoUrl: String?
)
