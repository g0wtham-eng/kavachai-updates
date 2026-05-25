package com.example.myapplication.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface FraudApiService {
    @GET("check-caller")
    suspend fun checkCaller(
        @Query("phoneNumber") phoneNumber: String
    ): FraudResponse
}

data class FraudResponse(
    val verdict: String,
    val score: Float,
    val details: String?,
    val transcript: List<String>?,
    val voiceStatus: String?,
    val originStatus: String?,
    val dbStatus: String?
)
