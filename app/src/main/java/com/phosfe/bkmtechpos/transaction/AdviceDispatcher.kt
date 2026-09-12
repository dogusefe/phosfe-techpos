package com.phosfe.bkmtechpos.transaction

import com.phosfe.bkmtechpos.host.IsoExchange
import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.storage.PendingDelivery
import com.phosfe.bkmtechpos.storage.DurableDeliveryQueue

enum class AdviceDispatchResult { NOTHING_PENDING, COMPLETED, HOST_REJECTED, SKIPPED_NON_ADVICE }

/** Delivers one durable offline/TC advice and leaves transport failures retryable. */
class AdviceDispatcher(
    private val queue: AdviceDeliveryPort,
    private val exchange: IsoExchange
) {
    constructor(queue: DurableDeliveryQueue, exchange: IsoExchange) : this(DurableQueueAdvicePort(queue), exchange)
    fun dispatchOnce(): AdviceDispatchResult {
        val pending = queue.next() ?: return AdviceDispatchResult.NOTHING_PENDING
        if (pending.kind !in setOf("OFFLINE_ADVICE", "TC_ADVICE")) {
            queue.retry(pending.id, 0L, "SKIPPED")
            return AdviceDispatchResult.SKIPPED_NON_ADVICE
        }
        val response = exchange.exchange(pending.request)
        validate(pending.request, response)
        val code = response.text(39) ?: error("Advice response has no F39")
        return if (code == "00") {
            queue.acknowledge(pending.id, code, response.text(37))
            AdviceDispatchResult.COMPLETED
        } else {
            queue.retry(pending.id, ADVICE_RETRY_MS, code)
            AdviceDispatchResult.HOST_REJECTED
        }
    }

    private fun validate(request: IsoMessage, response: IsoMessage) {
        require(request.messageType == "0220") { "Advice request MTI mismatch" }
        require(response.messageType == "0230") { "Advice response MTI mismatch" }
        require(response.text(3) == request.text(3)) { "Advice processing code mismatch" }
        require(response.text(11) == request.text(11)) { "Advice STAN mismatch" }
    }

    private companion object { const val ADVICE_RETRY_MS = 60_000L }
}

class AdviceDrainCoordinator(private val dispatcher: AdviceDispatcher, private val maxItems: Int = 8) {
    init { require(maxItems > 0) }

    fun drain(): Int {
        var completed = 0
        repeat(maxItems) {
            when (dispatcher.dispatchOnce()) {
                AdviceDispatchResult.COMPLETED -> completed++
                AdviceDispatchResult.HOST_REJECTED,
                AdviceDispatchResult.NOTHING_PENDING -> return completed
                AdviceDispatchResult.SKIPPED_NON_ADVICE -> Unit
            }
        }
        return completed
    }
}

private class DurableQueueAdvicePort(private val queue: DurableDeliveryQueue) : AdviceDeliveryPort {
    override fun next() = queue.next()
    override fun acknowledge(id: String, responseCode: String, hostReference: String?) = queue.acknowledge(id, responseCode, hostReference)
    override fun retry(id: String, delayMillis: Long, responseCode: String?) = queue.retry(id, delayMillis, responseCode)
}

interface AdviceDeliveryPort {
    fun next(): PendingDelivery?
    fun acknowledge(id: String, responseCode: String, hostReference: String? = null)
    fun retry(id: String, delayMillis: Long, responseCode: String? = null)
}
