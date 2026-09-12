package com.phosfe.bkmtechpos.security

import com.phosfe.bkmtechpos.protocol.BkmTag
import com.phosfe.bkmtechpos.protocol.BkmTlv
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class RsaRemoteLoadingFieldsTest {
    @Test fun rsaMethodAndTr31IndicatorAppearInTheirRespectiveSteps() {
        val first = BkmTlv.decode(RsaRemoteLoadingFields.firstRequest(ByteArray(8) { it.toByte() }))
        assertEquals(listOf(0x01, 0x20), first.map { it.id })
        assertArrayEquals(byteArrayOf(0, 0), first.last().value)

        val second = BkmTlv.decode(RsaRemoteLoadingFields.secondRequest(ByteArray(8), ByteArray(128)))
        assertEquals(listOf(0x04, 0x1A, 0x33), second.map { it.id })
        assertArrayEquals(byteArrayOf(0x01, 0x42), second.last().value)
    }

    @Test fun responsesKeepChallengesBlocksAndKcvsSeparate() {
        val first = RsaRemoteLoadingFields.firstReply(BkmTlv.encode(listOf(
            BkmTag(0x02, ByteArray(8) { 2 }),
            BkmTag(0x03, ByteArray(16) { 3 } + ByteArray(8) { 4 })
        )))
        assertArrayEquals(ByteArray(8) { 2 }, first.hostChallenge)
        assertArrayEquals(ByteArray(16) { 3 }, first.encryptedMsk)
        assertArrayEquals(ByteArray(8) { 4 }, first.encryptedTerminalChallenge)

        val second = RsaRemoteLoadingFields.secondReply(BkmTlv.encode(listOf(
            BkmTag(0x17, ByteArray(8) { 7 }),
            BkmTag(0x37, byteArrayOf(1, 2, 3) + "B0080K0TB00N0000".toByteArray()),
            BkmTag(0x38, byteArrayOf(1, 4, 5, 6) + "B0080P0TB00N0000".toByteArray())
        )))
        assertArrayEquals(byteArrayOf(1, 2, 3), second.terminalMasterKeyKcv)
        assertEquals(1, second.pinKeyIndicator)
        assertArrayEquals(byteArrayOf(4, 5, 6), second.pinKeyKcv)
        assertEquals("B0080P0TB00N0000", second.pinKeyBlock.toString(Charsets.US_ASCII))
    }

    @Test(expected = IllegalArgumentException::class)
    fun truncatedKeyBlockIsRejected() {
        RsaRemoteLoadingFields.secondReply(BkmTlv.encode(listOf(
            BkmTag(0x17, ByteArray(8)), BkmTag(0x37, byteArrayOf(1, 2, 3)),
            BkmTag(0x38, byteArrayOf(1, 2, 3, 4, 5))
        )))
    }
}
