// OWNER: C — RiskApi: Retrofit client for the AI risk server (POST /predict)
package com.example.mos.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Paste your Render link here. Keep the trailing "/".
// For local testing on the emulator use "http://10.0.2.2:8000/" (needs cleartext, see 6.4).
const val RISK_API_BASE_URL = "https://semproject-mlengine-render.onrender.com/"

data class RiskPredictRequest(val coordinates: List<List<Double>>, val mode: String, val hour: Int)

data class RiskPredictResponse(
    val overall: Double = 0.0, val accident: Double = 0.0,
    val flooding: Double = 0.0, val lighting: Double = 0.0
)

data class RiskHealthResponse(val status: String = "")

interface RiskApiService {
    @POST("predict")
    suspend fun predict(@Body request: RiskPredictRequest): RiskPredictResponse

    @GET("health")
    suspend fun health(): RiskHealthResponse
}

fun createRiskApiService(baseUrl: String = RISK_API_BASE_URL): RiskApiService {
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RiskApiService::class.java)
}