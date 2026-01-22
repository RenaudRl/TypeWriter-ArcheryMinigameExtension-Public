package com.btc.shops.service

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceholderServiceTest {
    @Test
    fun `format default pattern`() {
        val millis = (((1 * 24 + 2) * 60 + 3) * 60 + 4) * 1000L
        assertEquals("1j:02h:03m:04s", formatDuration(millis))
    }

    @Test
    fun `format custom pattern`() {
        val millis = (65 * 1000L)
        assertEquals("01:05", formatDuration(millis, "{m}:{s}"))
    }
}

