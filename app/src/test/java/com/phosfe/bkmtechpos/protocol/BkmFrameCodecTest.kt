package com.phosfe.bkmtechpos.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BkmFrameCodecTest {
    @Test fun crcMatchesPublishedIeeeVector() {
        assertArrayEquals(
            byteArrayOf(0xCB.toByte(), 0xF4.toByte(), 0x39, 0x26),
            BkmIntegrity.crc32("123456789".toByteArray())
        )
    }

    @Test fun encryptedFrameRoundTrips() {
        val key = ByteArray(16) { (it + 1).toByte() }
        val cipher = TripleDesSessionCipher(key)
        val message = IsoMessage("0800", mapOf(
            3 to "930000".toByteArray(),
            11 to "000123".toByteArray(),
            12 to "235959".toByteArray(),
            13 to "0912".toByteArray(),
            43 to "SERIAL000001    PHSPOS0101350000      ".take(40).padEnd(40).toByteArray()
        ))
        val codec = BkmFrameCodec()
        val decoded = codec.decode(codec.encode(TerminalRoute(vendorId = 42, serialNumber = "SERIAL000001"), message, cipher), cipher)
        assertTrue(decoded.encrypted)
        assertEquals("SERIAL000001", decoded.route.serialNumber)
        assertEquals(message.messageType, decoded.message.messageType)
        message.fields.forEach { (field, value) -> assertArrayEquals(value, decoded.message.fields.getValue(field)) }
    }
}

