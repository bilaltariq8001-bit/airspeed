package com.velocity.speedometer.domain.util

import com.velocity.speedometer.domain.model.SpeedUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedConverterTest {

    @Test
    fun `converts mps to kmh mph and knots accurately`() {
        val mps = 10f // 36 km/h
        assertEquals(36f, SpeedConverter.fromMps(mps, SpeedUnit.KMH), 0.01f)
        assertEquals(22.369f, SpeedConverter.fromMps(mps, SpeedUnit.MPH), 0.05f)
        assertEquals(19.438f, SpeedConverter.fromMps(mps, SpeedUnit.KNOTS), 0.05f)
    }

    @Test
    fun `negative speed clamps to zero`() {
        assertEquals(0f, SpeedConverter.fromMps(-5f, SpeedUnit.KMH), 0.001f)
        assertEquals(0, SpeedConverter.fromMpsRounded(-1f, SpeedUnit.MPH))
    }

    @Test
    fun `round trip toMps and fromMps preserves value`() {
        val originalKmh = 120f
        val mps = SpeedConverter.toMps(originalKmh, SpeedUnit.KMH)
        val back = SpeedConverter.fromMps(mps, SpeedUnit.KMH)
        assertEquals(originalKmh, back, 0.01f)
    }

    @Test
    fun `formats distance and duration across units`() {
        assertEquals("450 m", SpeedConverter.formatDistance(450.0, SpeedUnit.KMH))
        assertEquals("1.50 km", SpeedConverter.formatDistance(1500.0, SpeedUnit.KMH))
        assertEquals("1.00 mi", SpeedConverter.formatDistance(1609.344, SpeedUnit.MPH))
        assertEquals("1.00 nm", SpeedConverter.formatDistance(1852.0, SpeedUnit.KNOTS))

        assertEquals("05:07", SpeedConverter.formatDuration(307_000L))
        assertEquals("01:02:03", SpeedConverter.formatDuration(3_723_000L))
    }
}
