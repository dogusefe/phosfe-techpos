package com.phosfe.bkmtechpos.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BerTlvTest {
    @Test fun shortAndLongLengthsRoundTrip() {
        val entries = listOf(
            TlvEntry(0x01, byteArrayOf(1, 2, 3)),
            TlvEntry(0x9F33, ByteArray(180) { it.toByte() })
        )
        val decoded = BerTlv.decode(BerTlv.encode(entries))
        assertEquals(entries.map { it.tag }, decoded.map { it.tag })
        entries.zip(decoded).forEach { (expected, actual) -> assertArrayEquals(expected.value, actual.value) }
    }
}

