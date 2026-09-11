package com.phosfe.bkmtechpos.transaction

import com.phosfe.bkmtechpos.protocol.BkmTag
import com.phosfe.bkmtechpos.protocol.BkmTlv
import com.phosfe.bkmtechpos.protocol.IsoMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files
import javax.crypto.spec.SecretKeySpec

class ReversalJournalTest {
    @Test fun authorizationBecomesSpecCompliantReversal() {
        val authorization = authorizationRequest() + (55 to byteArrayOf(1, 2, 3))
        val reversal = ReversalRequestFactory.fromAuthorization(authorization, "5400000000000001")
        assertEquals("0400", reversal.messageType)
        assertEquals((authorization.fields.keys - 55) + 2, reversal.fields.keys)
        assertArrayEquals(authorization.fields.getValue(11), reversal.fields.getValue(11))
    }

    @Test fun journalSurvivesReopenAndBlocksTransactions() {
        val directory = Files.createTempDirectory("phosfe-reversal-").toFile()
        try {
            val file = directory.resolve("pending.bin")
            val entry = PendingReversal("test-id", ReversalRequestFactory.fromAuthorization(authorizationRequest(), "5400000000000001"))
            FileReversalJournal(file, journalCipher()).save(entry)

            val reopened = FileReversalJournal(file, journalCipher())
            assertEquals("test-id", reopened.pending()?.id)
            try {
                TransactionGate(reopened).requireReady()
                throw AssertionError("Gate should have blocked the transaction")
            } catch (_: IllegalStateException) {
                // Expected.
            }
            reopened.clear("test-id")
            assertNull(reopened.pending())
            TransactionGate(reopened).requireReady()
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun authorizationRequest(): IsoMessage = IsoMessage("0200", mapOf(
        3 to "000000".toByteArray(),
        4 to "000000001250".toByteArray(),
        11 to "123456".toByteArray(),
        12 to "142233".toByteArray(),
        13 to "0912".toByteArray(),
        22 to "0710".toByteArray(),
        25 to "00".toByteArray(),
        41 to "TERM0001".toByteArray(),
        42 to "MERCHANT0000001".toByteArray(),
        43 to "SERIAL000001    PHSPOS010035000012      ".take(40).padEnd(40).toByteArray(),
        49 to "0949".toByteArray(),
        63 to BkmTlv.encode(listOf(BkmTag(0x0C, ByteArray(18))))
    ))

    private operator fun IsoMessage.plus(field: Pair<Int, ByteArray>): IsoMessage =
        copy(fields = fields + field)

    private fun journalCipher(): JournalCipher = AesGcmJournalCipher(
        SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")
    )
}
