package com.focuspath.app.data.remote

import com.google.gson.annotations.SerializedName

data class TaskDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("notes") val notes: String,
    @SerializedName("category") val category: String,
    @SerializedName("priority") val priority: Int,
    @SerializedName("isCompleted") val isCompleted: Boolean,
    @SerializedName("dueDate") val dueDate: Long,
    @SerializedName("parentId") val parentId: Long
)
