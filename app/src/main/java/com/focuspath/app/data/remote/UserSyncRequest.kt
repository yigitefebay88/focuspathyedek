package com.focuspath.app.data.remote

import com.google.gson.annotations.SerializedName

data class UserSyncRequest(
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("xp") val xp: Long,
    @SerializedName("coins") val coins: Int,
    @SerializedName("level") val level: Int,
    @SerializedName("photoUrl") val photoUrl: String?
)
