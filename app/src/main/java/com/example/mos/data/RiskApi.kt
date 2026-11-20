// OWNER: C — RiskApi: Retrofit client for the AI risk server (POST /predict)
package com.example.mos.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Change this when pointing at a different server.
// 10.0.2.2 is the Android emulator's alias for the host machine's localhost.
const val RISK_API_BASE_URL = "http://10.0.2.2:8000/"

// Request: coordinates are [lat, lng] pairs taken from the route's stops.
data class RiskPredictRequest(
    val coordinates: List<List<Double>>,
    val mode: String,
    val hour: Int
)

// Response: all fields are 0.0–1.0, matching Risk's scale directly.
data class RiskPredictResponse(
    val overall: Double = 0.0,
    val accident: Double = 0.0,
    val flooding: Double = 0.0,
    val lighting: Double = 0.0
)

interface RiskApiService {
    @POST("predict")
    suspend fun predict(@Body request: RiskPredictRequest): RiskPredictResponse
}

fun createRiskApiService(baseUrl: String = RISK_API_BASE_URL): RiskApiService {
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RiskApiService::class.java)
}
