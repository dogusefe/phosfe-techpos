package com.phosfe.bkmtechpos.transaction

import com.phosfe.bkmtechpos.host.IsoExchange
import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.terminal.RotatingStan
import com.phosfe.bkmtechpos.terminal.TerminalClock
import com.phosfe.bkmtechpos.terminal.TerminalProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AuthorizationProtocolTest {
    private val request = AuthorizationRequestFactory(
        TerminalProfile("SERIAL000001", "PHS", "POS", "0100", "3500", 12),
        RotatingStan(99),
        TerminalClock(Clock.fixed(Instant.parse("2026-09-12T14:22:33Z"), ZoneOffset.UTC))
    ).create(
        AuthorizationInput(
            kind = AuthorizationKind.SALE,
            amountMinor = 1_250,
            entryMode = "0110",
            conditionCode = "00",
            terminalId = "TERM0001",
            merchantId = "MERCHANT0000001",
            pan = "5400000000000001",
            batchTrace = BatchTrace(1, 2)
        )
    )

    @Test fun approvedTransactionKeepsReversalUntilCardCompletion() {
        val journal = MemoryJournal()
        val coordinator = AuthorizationCoordinator(
            IsoExchange { authorizationResponse(it, "00") },
            journal
        )
        val result = coordinator.authorize(request, "5400000000000001")
        assertEquals(true, result.reply.approved)
        assertNotNull(result.completionToken)
        assertNotNull(journal.pending())
        coordinator.confirmCardCompletion(result.completionToken!!)
        assertNull(journal.pending())
    }

    @Test fun declinedTransactionClearsReversalImmediately() {
        val journal = MemoryJournal()
        val result = AuthorizationCoordinator(IsoExchange { authorizationResponse(it, "05") }, journal)
            .authorize(request, "5400000000000001")
        assertEquals(false, result.reply.approved)
        assertNull(result.completionToken)
        assertNull(journal.pending())
    }

    @Test fun transportFailureLeavesDurableReversalAndBlocksNextTransaction() {
        val journal = MemoryJournal()
        val coordinator = AuthorizationCoordinator(IsoExchange { throw IOException("timeout") }, journal)
        try {
            coordinator.authorize(request, "5400000000000001")
            throw AssertionError("Expected transport failure")
        } catch (_: IOException) {
            // Expected.
        }
        assertNotNull(journal.pending())
        try {
            coordinator.authorize(request, "5400000000000001")
            throw AssertionError("Pending reversal should block a new authorization")
        } catch (_: IllegalStateException) {
            // Expected.
        }
    }

    private fun authorizationResponse(request: IsoMessage, code: String): IsoMessage = IsoMessage("0210", mapOf(
        3 to request.fields.getValue(3),
        11 to request.fields.getValue(11),
        12 to request.fields.getValue(12),
        13 to request.fields.getValue(13),
        37 to "123456789012".toByteArray(),
        38 to "ABC123".toByteArray(),
        39 to code.toByteArray(),
        41 to request.fields.getValue(41),
        42 to request.fields.getValue(42)
    ))

    private class MemoryJournal : ReversalJournal {
        private var value: PendingReversal? = null
        override fun pending(): PendingReversal? = value
        override fun save(entry: PendingReversal) {
            check(value == null)
            value = entry
        }
        override fun clear(expectedId: String) {
            require(value?.id == expectedId)
            value = null
        }
    }
}

