// OWNER: C — TODO
package com.example.mos.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.delay

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
fun Risk.level(): RiskLevel = when {
    overall < 0.15 -> RiskLevel.LOW
    overall <= 0.30 -> RiskLevel.MODERATE
    else -> RiskLevel.HIGH
}
// RemoteRiskRepository (AI server via Retrofit) goes here later
