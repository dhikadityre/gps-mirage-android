package com.dhikadityre.gpsmirage.core.geo

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `metersToFeet converts accurately`() {
        assertEquals(3.28084, UnitConverter.metersToFeet(1.0), 0.0001)
        // 23.7744 meters should be ~78 ft
        assertEquals(78.0, UnitConverter.metersToFeet(23.7744), 0.01)
        // 27.1272 meters should be ~89 ft
        assertEquals(89.0, UnitConverter.metersToFeet(27.1272), 0.01)
    }

    @Test
    fun `feetToMeters converts accurately`() {
        assertEquals(1.0, UnitConverter.feetToMeters(3.28084), 0.0001)
        // 78 ft should be ~23.77 meters
        assertEquals(23.77, UnitConverter.feetToMeters(78.0), 0.01)
        // 89 ft should be ~27.13 meters
        assertEquals(27.13, UnitConverter.feetToMeters(89.0), 0.01)
    }

    @Test
    fun `bidirectional roundtrip preserves values`() {
        val originalMeters = 50.0
        val toFeet = UnitConverter.metersToFeet(originalMeters)
        val backToMeters = UnitConverter.feetToMeters(toFeet)
        assertEquals(originalMeters, backToMeters, 0.0001)

        val originalFeet = 150.0
        val toMeters = UnitConverter.feetToMeters(originalFeet)
        val backToFeet = UnitConverter.metersToFeet(toMeters)
        assertEquals(originalFeet, backToFeet, 0.0001)
    }

    @Test
    fun `normalizeBearing bounds bearing within 0 to 360`() {
        assertEquals(0.0, UnitConverter.normalizeBearing(0.0), 0.0001)
        assertEquals(0.0, UnitConverter.normalizeBearing(360.0), 0.0001)
        assertEquals(10.5, UnitConverter.normalizeBearing(370.5), 0.0001)
        assertEquals(350.0, UnitConverter.normalizeBearing(-10.0), 0.0001)
        assertEquals(5.0, UnitConverter.normalizeBearing(725.0), 0.0001)
    }

    @Test
    fun `formatValue formats to two decimal places cleanly`() {
        assertEquals("23.77", UnitConverter.formatTwoDecimals(23.7744))
        assertEquals("78.00", UnitConverter.formatTwoDecimals(78.0))
    }
}
