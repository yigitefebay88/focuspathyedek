package com.focuspath.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.DELETE

import com.google.gson.annotations.SerializedName

import com.focuspath.shared.model.UserResponse

interface FocusPathApiService {
    @GET("api/v1/tasks")
    suspend fun getTasks(@Query("email") email: String): List<TaskDto>

    @POST("api/v1/tasks/sync")
    suspend fun syncTasks(@Body request: TaskSyncRequest): Response<List<TaskDto>>

    @POST("api/v1/users/sync")
    suspend fun syncUser(@Body request: UserSyncRequest): Response<Unit>

    @GET("api/v1/users/leaderboard")
    suspend fun getLeaderboard(): Response<List<UserResponse>>
}
