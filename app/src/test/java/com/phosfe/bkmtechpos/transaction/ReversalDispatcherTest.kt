package com.phosfe.bkmtechpos.transaction

import com.phosfe.bkmtechpos.host.IsoExchange
import com.phosfe.bkmtechpos.protocol.BkmTag
import com.phosfe.bkmtechpos.protocol.BkmTlv
import com.phosfe.bkmtechpos.protocol.IsoMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReversalDispatcherTest {
    @Test fun successfulMatchingReplyClearsJournal() {
        val journal = MemoryJournal(reversal())
        val result = ReversalDispatcher(journal, IsoExchange { reply(it, "00") }).dispatchOnce()
        assertEquals(ReversalDispatchResult.COMPLETED, result)
        assertNull(journal.pending())
    }

    @Test fun rejectedReplyLeavesExactReversalPending() {
        val request = reversal()
        val journal = MemoryJournal(request)
        val result = ReversalDispatcher(journal, IsoExchange { reply(it, "05") }).dispatchOnce()
        assertEquals(ReversalDispatchResult.HOST_REJECTED, result)
        assertNotNull(journal.pending())
        assertEquals(request.text(11), journal.pending()?.request?.text(11))
    }

    private fun reversal(): IsoMessage = IsoMessage("0400", mapOf(
        2 to "5400000000000001".toByteArray(),
        3 to "000000".toByteArray(),
        4 to "000000001250".toByteArray(),
        11 to "000321".toByteArray(),
        12 to "142233".toByteArray(),
        13 to "0912".toByteArray(),
        22 to "0710".toByteArray(),
        25 to "00".toByteArray(),
        41 to "TERM0001".toByteArray(),
        42 to "MERCHANT0000001".toByteArray(),
        43 to ByteArray(40) { 0x20 },
        49 to "0949".toByteArray(),
        63 to BkmTlv.encode(listOf(BkmTag(0x0C, ByteArray(18))))
    ))

    private fun reply(request: IsoMessage, code: String): IsoMessage = IsoMessage("0410", mapOf(
        3 to request.fields.getValue(3),
        11 to request.fields.getValue(11),
        12 to request.fields.getValue(12),
        13 to request.fields.getValue(13),
        37 to "123456789012".toByteArray(),
        39 to code.toByteArray(),
        41 to request.fields.getValue(41),
        42 to request.fields.getValue(42)
    ))

    private class MemoryJournal(request: IsoMessage?) : ReversalJournal {
        private var value = request?.let { PendingReversal("pending", it) }
        override fun pending(): PendingReversal? = value
        override fun save(entry: PendingReversal) { check(value == null); value = entry }
        override fun clear(expectedId: String) { require(value?.id == expectedId); value = null }
    }
}

