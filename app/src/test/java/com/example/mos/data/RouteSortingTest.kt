package com.example.mos.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteSortingTest {

    @Test
    fun `routes sort by risk overall ascending`() {
        val routes = listOf(
            Route(id = "a", risk = Risk(overall = 0.40)),
            Route(id = "b", risk = Risk(overall = 0.12)),
            Route(id = "c", risk = Risk(overall = 0.28))
        )

        val sorted = routes.sortedBy { it.risk.overall }

        assertEquals(listOf(0.12, 0.28, 0.40), sorted.map { it.risk.overall })
    }
}
