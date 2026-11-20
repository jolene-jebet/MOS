package com.example.mos.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeRiskRepositoryTest {

    @Test
    fun `unknown route id falls back to route risk`() = runTest {
        val repository = FakeRiskRepository(context = null)
        val route = Route(
            id = "unknown-route",
            risk = Risk(overall = 0.33, accident = 0.5, flooding = 0.1, lighting = 0.2)
        )

        val result = repository.getRisk(route, TransportMode.WALK)

        assertEquals(route.risk, result)
    }
}
