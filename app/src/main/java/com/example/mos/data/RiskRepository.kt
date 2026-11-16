// OWNER: C — TODO
package com.example.mos.data

interface RiskRepository {
    suspend fun getRisk(route: Route, mode: TransportMode): Risk
}

class FakeRiskRepository : RiskRepository {
    override suspend fun getRisk(route: Route, mode: TransportMode): Risk {
        // TODO: read fake_data.json
        return Risk(overall = 0.5, accident = 0.5, flooding = 0.5, lighting = 0.5)
    }
}

// RemoteRiskRepository (AI server via Retrofit) goes here later
