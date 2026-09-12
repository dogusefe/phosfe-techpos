package com.phosfe.bkmtechpos.settlement

import com.phosfe.bkmtechpos.host.IsoExchange
import com.phosfe.bkmtechpos.protocol.BkmTlv
import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.terminal.RotatingStan
import com.phosfe.bkmtechpos.terminal.TerminalClock
import com.phosfe.bkmtechpos.terminal.TerminalProfile
import com.phosfe.bkmtechpos.transaction.PendingReversal
import com.phosfe.bkmtechpos.transaction.ReversalJournal
import com.phosfe.bkmtechpos.transaction.TransactionGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SettlementProtocolTest {
    private val clock = TerminalClock(Clock.fixed(Instant.parse("2026-09-12T14:22:33Z"), ZoneOffset.UTC))
    private val profile = TerminalProfile("SERIAL000001", "PHS", "POS", "0100", "3500", 12)
    private val totals = BatchSettlement(7, listOf(
        CurrencySettlement(
            "949",
            AmountAggregate(2, 12_500),
            AmountAggregate(1, 500),
            AmountAggregate(0, 0),
            AmountAggregate(0, 0)
        )
    ))

    @Test fun primarySettlementContainsTotalsAndCapabilities() {
        val request = SettlementRequestFactory(profile, RotatingStan(), clock)
            .create(ReconciliationStep.PRIMARY, totals, byteArrayOf(0x40, 0, 0, 0, 0))
        assertEquals("0500", request.messageType)
        assertEquals("910000", request.text(3))
        assertEquals(0x11, BkmTlv.decode(request.fields.getValue(63))[0].id)
        assertEquals(0x23, BkmTlv.decode(request.fields.getValue(63))[1].id)
    }

    @Test fun mismatchUploadsEveryApprovedRecordThenRunsSecondarySettlement() {
        val ledger = MemoryLedger(listOf(approvedTransaction()))
        val processingCodes = mutableListOf<String>()
        val exchange = IsoExchange { request ->
            processingCodes += request.text(3)!!
            if (request.messageType == "0320") {
                assertEquals("000001", request.text(11))
                assertEquals("120000", request.text(12))
                assertEquals("0912", request.text(13))
            }
            when (request.messageType) {
                "0500" -> settlementReply(request, if (request.text(3) == "910000") "95" else "00")
                "0320" -> batchReply(request)
                else -> error("Unexpected MTI")
            }
        }
        val coordinator = SettlementCoordinator(
            exchange,
            SettlementRequestFactory(profile, RotatingStan(), clock),
            BatchUploadRequestFactory(RotatingStan(100), clock),
            ledger,
            TransactionGate(EmptyJournal())
        )
        val result = coordinator.settle(totals)
        assertTrue(result is SettlementResult.Completed)
        assertEquals(1, (result as SettlementResult.Completed).uploadedCount)
        assertEquals(listOf("910000", "000000", "920000"), processingCodes)
        assertEquals(7, ledger.closedBatch)
    }

    private fun approvedTransaction(): ApprovedBatchTransaction {
        val request = IsoMessage("0200", mapOf(
            3 to "000000".toByteArray(), 4 to "000000001250".toByteArray(),
            11 to "000001".toByteArray(), 12 to "120000".toByteArray(), 13 to "0912".toByteArray(),
            22 to "0710".toByteArray(), 25 to "00".toByteArray(),
            41 to "TERM0001".toByteArray(), 42 to "MERCHANT0000001".toByteArray(),
            43 to profile.field43(), 49 to "0949".toByteArray(),
            63 to byteArrayOf(0x0C, 0, 18) + ByteArray(18)
        ))
        val response = IsoMessage("0210", mapOf(
            3 to request.fields.getValue(3), 11 to request.fields.getValue(11),
            12 to request.fields.getValue(12), 13 to request.fields.getValue(13),
            37 to "123456789012".toByteArray(), 38 to "ABC123".toByteArray(),
            39 to "00".toByteArray(), 41 to request.fields.getValue(41), 42 to request.fields.getValue(42)
        ))
        return ApprovedBatchTransaction("5400000000000001", request, response)
    }

    private fun settlementReply(request: IsoMessage, code: String): IsoMessage = IsoMessage("0510", mapOf(
        3 to request.fields.getValue(3), 11 to request.fields.getValue(11),
        12 to request.fields.getValue(12), 13 to request.fields.getValue(13),
        37 to "987654321098".toByteArray(), 39 to code.toByteArray()
    ))

    private fun batchReply(request: IsoMessage): IsoMessage = IsoMessage("0330", mapOf(
        3 to request.fields.getValue(3), 11 to request.fields.getValue(11),
        12 to request.fields.getValue(12), 13 to request.fields.getValue(13),
        37 to request.fields.getValue(37), 38 to request.fields.getValue(38),
        39 to "00".toByteArray(), 41 to request.fields.getValue(41), 42 to request.fields.getValue(42)
    ))

    private class MemoryLedger(private val records: List<ApprovedBatchTransaction>) : BatchLedger {
        var closedBatch: Int? = null
        override fun approvedTransactions(batchNumber: Int) = records
        override fun closeBatch(batchNumber: Int, hostReference: String) { closedBatch = batchNumber }
    }

    private class EmptyJournal : ReversalJournal {
        override fun pending(): PendingReversal? = null
        override fun save(entry: PendingReversal) = error("not used")
        override fun clear(expectedId: String) = error("not used")
    }
}
