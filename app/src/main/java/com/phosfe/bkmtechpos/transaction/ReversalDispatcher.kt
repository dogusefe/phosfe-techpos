package com.phosfe.bkmtechpos.transaction

import com.phosfe.bkmtechpos.host.IsoExchange
import com.phosfe.bkmtechpos.protocol.IsoMessage

enum class ReversalDispatchResult { NOTHING_PENDING, COMPLETED, HOST_REJECTED }

class ReversalDispatcher(
    private val journal: ReversalJournal,
    private val exchange: IsoExchange
) {
    /**
     * Sends the exact durable 0400 snapshot. Transport/protocol failures deliberately leave it
     * pending, so the caller can retry with backoff before allowing another transaction.
     */
    fun dispatchOnce(): ReversalDispatchResult {
        val pending = journal.pending() ?: return ReversalDispatchResult.NOTHING_PENDING
        val response = exchange.exchange(pending.request)
        validateResponse(pending.request, response)
        return if (response.text(39) == "00") {
            journal.clear(pending.id)
            ReversalDispatchResult.COMPLETED
        } else {
            ReversalDispatchResult.HOST_REJECTED
        }
    }

    private fun validateResponse(request: IsoMessage, response: IsoMessage) {
        require(response.messageType == "0410") { "Reversal response MTI mismatch" }
        require(response.text(3) == request.text(3)) { "Reversal processing code mismatch" }
        require(response.text(11) == request.text(11)) { "Reversal STAN mismatch" }
        require(response.text(12) == request.text(12)) { "Reversal time mismatch" }
        require(response.text(13) == request.text(13)) { "Reversal date mismatch" }
        requireNotNull(response.text(39)) { "Reversal response has no F39" }
    }
}

