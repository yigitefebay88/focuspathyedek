package com.focuspath.app.di

import android.content.Context
import com.focuspath.app.core.data.local.AppDatabase
import com.focuspath.app.core.data.local.TaskDao
import com.focuspath.app.data.remote.FocusPathApiService
import com.focuspath.app.core.domain.usecase.GetFocusRankUseCase
import com.focuspath.app.core.domain.usecase.CalculateDopamineUseCase
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideTaskDao(@ApplicationContext context: Context): TaskDao {
        return AppDatabase.getDatabase(context).taskDaoProvider()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): android.content.SharedPreferences {
        return context.getSharedPreferences("focuspath_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        val apiKey = com.focuspath.app.BuildConfig.GEMINI_API_KEY

        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
            )
        )
    }

    @Provides
    @Singleton
    fun provideGetFocusRankUseCase(): GetFocusRankUseCase = GetFocusRankUseCase()

    @Provides
    @Singleton
    fun provideCalculateDopamineUseCase(): CalculateDopamineUseCase = CalculateDopamineUseCase()

    @Provides
    @Singleton
    fun provideFocusPathApiService(): FocusPathApiService {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return retrofit2.Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/") 
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .client(client)
            .build()
            .create(FocusPathApiService::class.java)
    }
}
