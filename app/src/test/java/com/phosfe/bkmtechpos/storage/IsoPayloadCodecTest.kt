package com.phosfe.bkmtechpos.storage

import com.phosfe.bkmtechpos.protocol.IsoMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class IsoPayloadCodecTest {
    @Test fun roundTripPreservesBinaryFields() {
        val original = IsoMessage("0420", mapOf(
            2 to "5400000000000001".toByteArray(),
            11 to "123456".toByteArray(),
            55 to byteArrayOf(0, 1, 2, 0xFF.toByte()),
            63 to byteArrayOf(0x0C, 0, 1, 7)
        ))

        val restored = IsoPayloadCodec.decode(IsoPayloadCodec.encode(original))

        assertEquals(original.messageType, restored.messageType)
        assertEquals(original.fields.keys, restored.fields.keys)
        original.fields.forEach { (number, value) -> assertArrayEquals(value, restored.fields[number]) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun corruptedEnvelopeIsRejected() {
        IsoPayloadCodec.decode(ByteArray(12))
    }
}
