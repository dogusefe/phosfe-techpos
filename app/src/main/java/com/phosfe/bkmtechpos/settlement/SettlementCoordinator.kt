package com.phosfe.bkmtechpos.settlement

import com.phosfe.bkmtechpos.host.IsoExchange
import com.phosfe.bkmtechpos.transaction.TransactionGate

interface BatchLedger {
    fun approvedTransactions(batchNumber: Int): List<ApprovedBatchTransaction>
    fun markUploaded(transaction: ApprovedBatchTransaction) {}
    fun closeBatch(batchNumber: Int, hostReference: String)
}

interface SettlementReceiptWriter {
    fun record(totals: BatchSettlement, hostReference: String, uploadedCount: Int)
}

sealed interface SettlementResult {
    data class Completed(val hostReference: String, val uploadedCount: Int) : SettlementResult
    data class Blocked(val responseCode: String) : SettlementResult
}

class SettlementCoordinator(
    private val exchange: IsoExchange,
    private val requests: SettlementRequestFactory,
    private val uploads: BatchUploadRequestFactory,
    private val ledger: BatchLedger,
    private val transactionGate: TransactionGate,
    private val receiptWriter: SettlementReceiptWriter? = null
) {
    fun settle(totals: BatchSettlement, terminalCapabilities: ByteArray? = null): SettlementResult {
        transactionGate.requireReady()
        val primaryRequest = requests.create(ReconciliationStep.PRIMARY, totals, terminalCapabilities)
        return when (val primary = SettlementResponseParser.parse(primaryRequest, exchange.exchange(primaryRequest))) {
            is SettlementReply.Settled -> complete(totals, primary.referenceNumber, 0)
            is SettlementReply.Rejected -> SettlementResult.Blocked(primary.responseCode)
            is SettlementReply.UploadRequired -> uploadAndReconcile(totals, terminalCapabilities)
        }
    }

    private fun uploadAndReconcile(totals: BatchSettlement, terminalCapabilities: ByteArray?): SettlementResult {
        val records = ledger.approvedTransactions(totals.batchNumber)
        records.forEach { record ->
            val request = uploads.create(record)
            BatchUploadResponseParser.requireApproved(request, exchange.exchange(request))
            ledger.markUploaded(record)
        }
        val secondaryRequest = requests.create(ReconciliationStep.AFTER_BATCH_UPLOAD, totals, terminalCapabilities)
        return when (val secondary = SettlementResponseParser.parse(secondaryRequest, exchange.exchange(secondaryRequest))) {
            is SettlementReply.Settled -> complete(totals, secondary.referenceNumber, records.size)
            is SettlementReply.Rejected -> SettlementResult.Blocked(secondary.responseCode)
            is SettlementReply.UploadRequired -> SettlementResult.Blocked("95")
        }
    }

    private fun complete(totals: BatchSettlement, reference: String, uploadedCount: Int): SettlementResult.Completed {
        ledger.closeBatch(totals.batchNumber, reference)
        receiptWriter?.record(totals, reference, uploadedCount)
        return SettlementResult.Completed(reference, uploadedCount)
    }
}
