package com.velocity.speedometer.domain.util

import com.velocity.speedometer.domain.model.GpsSignalQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun `haversine calculates accurate distance between known coordinates`() {
        // ~111.19 km per degree of latitude at equator
        val dist = GeoUtils.haversineDistanceMeters(0.0, 0.0, 1.0, 0.0)
        assertEquals(111_195.0, dist, 250.0)
    }

    @Test
    fun `classifies GPS accuracy thresholds correctly`() {
        assertEquals(GpsSignalQuality.GOOD, GeoUtils.classifyGpsQuality(4.5f))
        assertEquals(GpsSignalQuality.MODERATE, GeoUtils.classifyGpsQuality(15f))
        assertEquals(GpsSignalQuality.MODERATE, GeoUtils.classifyGpsQuality(30f))
        assertEquals(GpsSignalQuality.POOR_OR_NONE, GeoUtils.classifyGpsQuality(31f))
        assertEquals(GpsSignalQuality.POOR_OR_NONE, GeoUtils.classifyGpsQuality(null))
    }

    @Test
    fun `maps bearing degrees to eight compass directions`() {
        assertNull(GeoUtils.bearingToCardinal(null))
        assertEquals(GeoUtils.CardinalDirection.N, GeoUtils.bearingToCardinal(0f))
        assertEquals(GeoUtils.CardinalDirection.N, GeoUtils.bearingToCardinal(355f))
        assertEquals(GeoUtils.CardinalDirection.NE, GeoUtils.bearingToCardinal(45f))
        assertEquals(GeoUtils.CardinalDirection.E, GeoUtils.bearingToCardinal(90f))
        assertEquals(GeoUtils.CardinalDirection.SE, GeoUtils.bearingToCardinal(135f))
        assertEquals(GeoUtils.CardinalDirection.S, GeoUtils.bearingToCardinal(180f))
        assertEquals(GeoUtils.CardinalDirection.SW, GeoUtils.bearingToCardinal(225f))
        assertEquals(GeoUtils.CardinalDirection.W, GeoUtils.bearingToCardinal(270f))
        assertEquals(GeoUtils.CardinalDirection.NW, GeoUtils.bearingToCardinal(315f))
    }

    @Test
    fun `isReasonableMove rejects teleportation spikes`() {
        assertTrue(GeoUtils.isReasonableMove(distanceMeters = 30.0, deltaTimeMs = 1_000L))
        assertFalse(GeoUtils.isReasonableMove(distanceMeters = 5_000.0, deltaTimeMs = 1_000L))
    }
}
