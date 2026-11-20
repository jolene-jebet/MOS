package com.example.mos.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RiskLevelTest {

    @Test
    fun `0_14 is LOW`() {
        assertEquals(RiskLevel.LOW, Risk(overall = 0.14).level())
    }

    @Test
    fun `0_15 is MODERATE`() {
        assertEquals(RiskLevel.MODERATE, Risk(overall = 0.15).level())
    }

    @Test
    fun `0_30 is MODERATE`() {
        assertEquals(RiskLevel.MODERATE, Risk(overall = 0.30).level())
    }

    @Test
    fun `0_31 is HIGH`() {
        assertEquals(RiskLevel.HIGH, Risk(overall = 0.31).level())
    }
}
