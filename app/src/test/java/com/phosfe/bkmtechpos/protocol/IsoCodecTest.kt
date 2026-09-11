package com.phosfe.bkmtechpos.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class IsoCodecTest {
    private val codec = IsoCodec()

    @Test fun authorizationMessageRoundTrips() {
        val original = IsoMessage(
            messageType = "0200",
            fields = mapOf(
                3 to "000000".toByteArray(),
                4 to "000000001250".toByteArray(),
                11 to "123456".toByteArray(),
                12 to "142233".toByteArray(),
                13 to "0912".toByteArray(),
                22 to "0710".toByteArray(),
                25 to "00".toByteArray(),
                41 to "TERM0001".toByteArray(),
                42 to "MERCHANT0000001".toByteArray(),
                49 to "0949".toByteArray()
            )
        )
        val decoded = codec.decode(codec.encode(original))
        assertEquals(original.messageType, decoded.messageType)
        assertEquals(original.fields.keys, decoded.fields.keys)
        original.fields.forEach { (field, value) -> assertArrayEquals(value, decoded.fields.getValue(field)) }
    }

    @Test fun binaryTlvFieldRoundTrips() {
        val tlv = BkmTlv.encode(listOf(BkmTag(0x1F, byteArrayOf(0x01, 0x02))))
        val original = IsoMessage("0800", mapOf(
            3 to "810000".toByteArray(),
            11 to "000001".toByteArray(),
            12 to "120000".toByteArray(),
            13 to "0912".toByteArray(),
            43 to "SERIAL123456    PHSPOS0101350000      ".take(40).padEnd(40).toByteArray(),
            63 to tlv
        ))
        val decoded = codec.decode(codec.encode(original))
        assertArrayEquals(tlv, decoded.fields.getValue(63))
    }

    @Test fun oddLengthPanUsesRightFillerAndRoundTrips() {
        val message = IsoMessage("0200", mapOf(
            2 to "1234567890123".toByteArray(),
            3 to "000000".toByteArray(),
            4 to "000000000100".toByteArray()
        ))
        assertArrayEquals(message.fields.getValue(2), codec.decode(codec.encode(message)).fields.getValue(2))
    }
}
