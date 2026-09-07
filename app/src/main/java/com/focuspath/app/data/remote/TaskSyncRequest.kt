package com.focuspath.app.data.remote

import com.google.gson.annotations.SerializedName

data class TaskSyncRequest(
    @SerializedName("email") val email: String,
    @SerializedName("tasks") val tasks: List<TaskDto>
)
