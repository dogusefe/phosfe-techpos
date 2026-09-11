package com.phosfe.bkmtechpos.storage

import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.data.local.TerminalDatabase
import com.phosfe.bkmtechpos.data.local.entity.DeliveryDebtEntity
import com.phosfe.bkmtechpos.data.local.entity.PaymentRecordEntity
import com.phosfe.bkmtechpos.settlement.ApprovedBatchTransaction
import com.phosfe.bkmtechpos.settlement.BatchLedger
import com.phosfe.bkmtechpos.transaction.JournalCipher
import java.time.Clock
import java.util.UUID

data class PaymentPersistenceInput(
    val id: String = UUID.randomUUID().toString(),
    val batchNumber: Int,
    val sequenceNumber: Int,
    val operation: String,
    val amountMinor: Long,
    val currencyNumeric: String,
    val pan: String?,
    val maskedPan: String?,
    val request: IsoMessage,
    val response: IsoMessage,
    val offlineAdvice: IsoMessage? = null,
    val tcAdvice: IsoMessage? = null
) {
    init {
        require(batchNumber in 1..999_999)
        require(sequenceNumber in 1..999_999)
        require(amountMinor >= 0)
        require(currencyNumeric.length == 3 && currencyNumeric.all(Char::isDigit))
        require(pan == null || pan.length in 12..19 && pan.all(Char::isDigit))
    }
}

/** Commits the payment and every host delivery obligation created by it as one SQLite transaction. */
class PaymentLedger(
    private val database: TerminalDatabase,
    private val cipher: JournalCipher,
    private val clock: Clock = Clock.systemUTC()
) {
    fun record(input: PaymentPersistenceInput) {
        val now = clock.millis()
        val responseCode = input.response.text(39)
        val approved = responseCode in setOf("00", "Y1", "Y3", "Z3")
        database.runInTransaction {
            database.batches().ensureOpen(input.batchNumber, now)
            database.payments().insert(
                PaymentRecordEntity(
                    id = input.id,
                    batchNumber = input.batchNumber,
                    sequenceNumber = input.sequenceNumber,
                    operation = input.operation,
                    amountMinor = input.amountMinor,
                    currencyNumeric = input.currencyNumeric,
                    stan = input.request.text(11).orEmpty(),
                    referenceNumber = input.response.text(37),
                    authorizationCode = input.response.text(38),
                    responseCode = responseCode,
                    state = if (approved) LedgerState.APPROVED else LedgerState.DECLINED,
                    maskedPan = input.maskedPan,
                    requestEnvelope = seal(input.request),
                    responseEnvelope = seal(input.response),
                    panEnvelope = input.pan?.toByteArray(Charsets.US_ASCII)?.let(cipher::seal),
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            input.offlineAdvice?.let { stageDebt(input.id, DeliveryKind.OFFLINE_ADVICE, 10, it, now) }
            input.tcAdvice?.let { stageDebt(input.id, DeliveryKind.TC_ADVICE, 20, it, now) }
        }
    }

    private fun stageDebt(paymentId: String, kind: String, priority: Int, request: IsoMessage, now: Long) {
        database.deliveryDebts().insert(
            DeliveryDebtEntity(
                id = UUID.randomUUID().toString(),
                paymentId = paymentId,
                kind = kind,
                priority = priority,
                requestEnvelope = seal(request),
                correlationStan = request.text(11),
                availableAtEpochMs = now,
                createdAtEpochMs = now
            )
        )
    }

    private fun seal(message: IsoMessage): ByteArray = cipher.seal(IsoPayloadCodec.encode(message))
}

class RoomBatchLedger(
    private val database: TerminalDatabase,
    private val cipher: JournalCipher,
    private val clock: Clock = Clock.systemUTC()
) : BatchLedger {
    override fun approvedTransactions(batchNumber: Int): List<ApprovedBatchTransaction> =
        database.payments().approvedForUpload(batchNumber).map { record ->
            val pan = record.panEnvelope?.let(cipher::open)?.toString(Charsets.US_ASCII)
                ?: error("Payment ${record.id} has no encrypted PAN for batch upload")
            val response = record.responseEnvelope?.let(cipher::open)?.let(IsoPayloadCodec::decode)
                ?: error("Payment ${record.id} has no authorization response")
            ApprovedBatchTransaction(
                pan,
                IsoPayloadCodec.decode(cipher.open(record.requestEnvelope)),
                response
            )
        }

    override fun closeBatch(batchNumber: Int, hostReference: String) {
        val now = clock.millis()
        database.runInTransaction {
            check(database.batches().close(batchNumber, hostReference, now) == 1) {
                "Batch $batchNumber is not open"
            }
            database.payments().markBatchSettled(batchNumber, now)
        }
    }
}
