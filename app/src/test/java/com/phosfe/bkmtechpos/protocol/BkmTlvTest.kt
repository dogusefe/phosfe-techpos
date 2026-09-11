package com.phosfe.bkmtechpos.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BkmTlvTest {
    @Test fun field48And63FormatUsesTwoByteLength() {
        val value = ByteArray(300) { it.toByte() }
        val wire = BkmTlv.encode(listOf(BkmTag(0x23, value)))
        assertEquals(0x23, wire[0].toInt() and 0xFF)
        assertEquals(0x01, wire[1].toInt() and 0xFF)
        assertEquals(0x2C, wire[2].toInt() and 0xFF)
        assertArrayEquals(value, BkmTlv.decode(wire).single().value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun truncatedValueIsRejected() {
        BkmTlv.decode(byteArrayOf(0x0D, 0x00, 0x02, 0x01))
    }
}

