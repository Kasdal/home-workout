package com.example.workoutapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun `formatKg drops trailing zero`() {
        assertEquals("20 kg", formatKg(20f))
        assertEquals("20", formatKg(20f, withUnit = false))
    }

    @Test
    fun `formatKg keeps one decimal for fractional values`() {
        assertEquals("12.5 kg", formatKg(12.5f))
        assertEquals("12.5", formatKg(12.5f, withUnit = false))
    }

    @Test
    fun `formatKg handles zero and large whole numbers`() {
        assertEquals("0 kg", formatKg(0f))
        assertEquals("1440 kg", formatKg(1440f))
    }

    @Test
    fun `formatDuration renders sub-minute values as seconds`() {
        assertEquals("0s", formatDuration(0L))
        assertEquals("45s", formatDuration(45L))
    }

    @Test
    fun `formatDuration renders minutes and mixed values`() {
        assertEquals("3m", formatDuration(180L))
        assertEquals("3m 20s", formatDuration(200L))
    }

    @Test
    fun `formatDuration renders hours and clamps negative input`() {
        assertEquals("1h", formatDuration(3600L))
        assertEquals("1h 5m", formatDuration(3900L))
        assertEquals("0s", formatDuration(-30L))
    }
}
