package com.nagarsetu.core.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SendGridService {
    @POST("v3/mail/send")
    suspend fun sendEmail(
        @Header("Authorization") authHeader: String,
        @Body request: SendGridRequest
    ): Response<Unit>
}

data class SendGridRequest(
    val personalizations: List<Personalization>,
    val from: EmailUser,
    val subject: String,
    val content: List<Content>
)

data class Personalization(
    val to: List<EmailUser>
)

data class EmailUser(
    val email: String,
    val name: String? = null
)

data class Content(
    val type: String,
    val value: String
)
