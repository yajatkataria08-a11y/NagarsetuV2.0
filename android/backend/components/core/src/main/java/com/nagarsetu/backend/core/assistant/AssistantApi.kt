package com.nagarsetu.backend.core.assistant

import retrofit2.Response
import retrofit2.http.*

interface GroqApiService {
    @POST("chat/completions")
    suspend fun getCompletion(
        @Header("Authorization") token: String,
        @Body request: GroqRequest
    ): Response<GroqResponse>
}

interface GeminiApiService {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun getCompletion(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}
