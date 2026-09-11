package com.phosfe.bkmtechpos.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PackedDecimalTest {
    @Test fun evenDigitsRoundTrip() {
        val encoded = PackedDecimal.encode("1298")
        assertArrayEquals(byteArrayOf(0x12, 0x98.toByte()), encoded)
        assertEquals("1298", PackedDecimal.decode(encoded, 4))
    }

    @Test fun oddDigitsUseLeadingZero() {
        val encoded = PackedDecimal.encode("123")
        assertArrayEquals(byteArrayOf(0x01, 0x23), encoded)
        assertEquals("123", PackedDecimal.decode(encoded, 3))
    }

    @Test fun trackDataUsesDSeparatorAndRightFiller() {
        val encoded = PackedDecimal.encodeTrack("5400000000000001=29122010000000000000")
        assertEquals("5400000000000001=29122010000000000000", PackedDecimal.decodeTrack(encoded, 37))
    }
}

