package com.nagarsetu.predictive.data.api

import com.nagarsetu.predictive.domain.model.Forecast
import com.nagarsetu.predictive.domain.model.RAGResult
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the NagarSetu prediction micro-service.
 *
 * Fix R1: Return types were `List<Any>` and `Any`, which Retrofit/Gson cannot
 * deserialise at runtime (throws JsonParseException). Replaced with the proper
 * domain model classes already defined in PredictionModels.kt.
 *
 * Note: the remote JSON shape must match [Forecast] and [RAGResult].
 * Until the backend is deployed, PredictiveRepository's local seed data
 * (weeklyForecast / queryRag) continues to serve as the offline fallback.
 */
interface PredictiveApiService {

    companion object {
        const val BASE_URL = "https://api.nagarsetu.in/predict/v1/"
    }

    /**
     * Returns a 7-day risk forecast for the given coordinates.
     * Maps directly to [Forecast] — ensure backend JSON keys match field names.
     */
    @GET("forecast")
    suspend fun getWeeklyForecast(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): List<Forecast>

    /**
     * Queries the civic-knowledge RAG endpoint.
     * Maps directly to [RAGResult].
     */
    @GET("rag")
    suspend fun queryCivicKnowledge(
        @Query("q") query: String
    ): RAGResult
}
