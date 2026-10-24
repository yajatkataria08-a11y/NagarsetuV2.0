package com.nagarsetu.core.data.network

import com.nagarsetu.core.data.local.PreferencesManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Automatically injects the JWT Bearer authorization header from encrypted storage
 * into outgoing requests to the NagarSetu API gateway.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val preferencesManager: PreferencesManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = preferencesManager.getAuthToken()

        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")

        if (!token.isNullOrBlank() && originalRequest.header("Authorization") == null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
