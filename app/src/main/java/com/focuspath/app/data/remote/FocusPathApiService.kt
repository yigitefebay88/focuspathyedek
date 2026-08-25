package com.focuspath.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FocusPathApiService {
    @GET("tasks")
    suspend fun getTasks(@Query("email") email: String): List<TaskDto>

    @POST("sync")
    suspend fun syncTask(@Body task: TaskDto): Response<Unit>
}
