// OWNER: C — TODO
package com.example.mos.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.Calendar

interface RiskRepository {
    suspend fun getRisk(route: Route, mode: TransportMode): Risk
}

class FakeRiskRepository(private val context: Context? = null) : RiskRepository {

    // Loaded once, then reused for every lookup.
    private var loaded = false
    private var cachedRoot: FakeDataRoot? = null

    private fun loadRoot(): FakeDataRoot? {
        if (loaded) return cachedRoot
        loaded = true
        cachedRoot = if (context == null) {
            null
        } else {
            try {
                val json = context.assets.open("fake_data.json").bufferedReader().use { it.readText() }
                Gson().fromJson(json, FakeDataRoot::class.java)
            } catch (_: Exception) {
                null
            }
        }
        return cachedRoot
    }

    override suspend fun getRisk(route: Route, mode: TransportMode): Risk {
        delay(300)
        val match = loadRoot()?.routes?.find { it.id == route.id }
        return match?.risk ?: route.risk
    }
}

// Calls the AI risk server. Not wired into MainActivity yet — FakeRiskRepository is still active.
class RemoteRiskRepository(private val api: RiskApiService) : RiskRepository {
    override suspend fun getRisk(route: Route, mode: TransportMode): Risk {
        return try {
            val request = RiskPredictRequest(
                coordinates = route.stops.map { listOf(it.lat, it.lng) },
                mode = mode.name,
                hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            )
            val response = api.predict(request)
            Risk(
                overall = response.overall,
                accident = response.accident,
                flooding = response.flooding,
                lighting = response.lighting
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("MOS-Risk", "getRisk failed for route ${route.id}", e)
            route.risk
        }
    }
}
fun Risk.level(): RiskLevel = when {
    overall < 0.15 -> RiskLevel.LOW
    overall <= 0.30 -> RiskLevel.MODERATE
    else -> RiskLevel.HIGH
}
