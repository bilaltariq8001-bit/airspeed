package com.velocity.speedometer.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedSmootherTest {

    @Test
    fun `rejects points with accuracy worse than 30 meters`() {
        val smoother = SpeedSmoother()
        assertNull(smoother.update(rawSpeedMps = 15f, accuracyMeters = 35f, timestampMs = 1_000L))
        assertNull(smoother.update(rawSpeedMps = 15f, accuracyMeters = 0f, timestampMs = 1_500L))
    }

    @Test
    fun `clamps stationary noise below threshold to zero`() {
        val smoother = SpeedSmoother()
        val result = smoother.update(rawSpeedMps = 0.2f, accuracyMeters = 5f, timestampMs = 1_000L)
        assertNotNull(result)
        assertEquals(0f, result!!, 0.001f)
    }

    @Test
    fun `rejects physically impossible instantaneous speed jump`() {
        val smoother = SpeedSmoother()
        val initial = smoother.update(rawSpeedMps = 10f, accuracyMeters = 5f, timestampMs = 1_000L)
        assertNotNull(initial)

        // +60 m/s (+216 km/h) in 0.5 seconds is impossible acceleration
        val spike = smoother.update(rawSpeedMps = 70f, accuracyMeters = 5f, timestampMs = 1_500L)
        assertNull(spike)
    }

    @Test
    fun `smooths noisy GPS jitter around steady speed`() {
        val smoother = SpeedSmoother()
        val samples = listOf(20f, 23f, 17f, 22f, 18f, 21f, 19f)
        var lastEstimate = 0f
        samples.forEachIndexed { idx, raw ->
            val est = smoother.update(
                rawSpeedMps = raw,
                accuracyMeters = 6f,
                timestampMs = 1_000L + idx * 500L
            )
            assertNotNull(est)
            lastEstimate = est!!
        }
        assertTrue(lastEstimate in 18.5f..21.5f)
    }
}
