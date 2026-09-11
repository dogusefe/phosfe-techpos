package com.phosfe.bkmtechpos.terminal

import com.phosfe.bkmtechpos.protocol.BkmTag
import com.phosfe.bkmtechpos.protocol.BkmTlv
import com.phosfe.bkmtechpos.protocol.IsoMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class HandshakeProtocolTest {
    private val profile = TerminalProfile("SERIAL000001", "PHS", "POS", "0100", "3500", 12)
    private val clock = TerminalClock(Clock.fixed(Instant.parse("2026-09-12T14:22:33Z"), ZoneOffset.UTC))

    @Test fun requestContainsSpecFieldsAndParameterVersions() {
        val request = HandshakeRequestFactory(profile, RotatingStan(41), clock).create(
            HandshakeReason.PERIODIC,
            listOf(ParameterVersion(0x0102, 0x03040506))
        )
        assertEquals("0800", request.messageType)
        assertEquals("710001", request.text(3))
        assertEquals("000042", request.text(11))
        assertEquals("142233", request.text(12))
        assertEquals("0912", request.text(13))
        assertEquals(40, request.fields.getValue(43).size)
        assertArrayEquals(
            byteArrayOf(1, 1, 2, 3, 4, 5, 6),
            BkmTlv.find(request.fields[63], 0x07)
        )
    }

    @Test fun successfulReplyParsesHostSignals() {
        val request = HandshakeRequestFactory(profile, RotatingStan(), clock)
            .create(HandshakeReason.PARAMETERS_ACTIVATED, emptyList())
        val response = IsoMessage("0810", mapOf(
            3 to request.fields.getValue(3),
            11 to request.fields.getValue(11),
            12 to request.fields.getValue(12),
            13 to request.fields.getValue(13),
            37 to "123456789012".toByteArray(),
            39 to "00".toByteArray(),
            48 to BkmTlv.encode(listOf(BkmTag(0x0D, byteArrayOf(2, 1))))
        ))
        val reply = HandshakeResponseParser.parse(request, response)
        assertEquals("123456789012", reply.referenceNumber)
        assertEquals(DeferredAction.BEFORE_NEXT_TRANSACTION, reply.signals.parameterDownload)
        assertEquals(DeferredAction.AFTER_NEXT_TRANSACTION, reply.signals.keyExchange)
    }

    @Test fun stanNeverProducesZero() {
        val source = RotatingStan(999_999)
        assertEquals("000001", source.next())
    }
}

