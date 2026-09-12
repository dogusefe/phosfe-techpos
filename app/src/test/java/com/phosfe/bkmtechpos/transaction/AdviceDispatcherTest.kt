package com.phosfe.bkmtechpos.transaction

import com.phosfe.bkmtechpos.host.IsoExchange
import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.storage.PendingDelivery
import org.junit.Assert.assertEquals
import org.junit.Test

class AdviceDispatcherTest {
    @Test fun approvedAdviceIsAcknowledged() {
        val port = FakePort(PendingDelivery("id", "OFFLINE_ADVICE", request(), 1))
        val result = AdviceDispatcher(port, IsoExchange { reply() }).dispatchOnce()
        assertEquals(AdviceDispatchResult.COMPLETED, result)
        assertEquals("00", port.ackCode)
    }

    @Test fun rejectedAdviceIsRescheduled() {
        val port = FakePort(PendingDelivery("id", "TC_ADVICE", request(), 1))
        val result = AdviceDispatcher(port, IsoExchange { reply("05") }).dispatchOnce()
        assertEquals(AdviceDispatchResult.HOST_REJECTED, result)
        assertEquals("05", port.retryCode)
    }

    private fun request() = IsoMessage("0220", mapOf(3 to "000000".toByteArray(), 11 to "123456".toByteArray()))
    private fun reply(code: String = "00") = IsoMessage("0230", mapOf(3 to "000000".toByteArray(), 11 to "123456".toByteArray(), 39 to code.toByteArray()))

    private class FakePort(private val pending: PendingDelivery?) : AdviceDeliveryPort {
        var ackCode: String? = null
        var retryCode: String? = null
        override fun next() = pending
        override fun acknowledge(id: String, responseCode: String, hostReference: String?) { ackCode = responseCode }
        override fun retry(id: String, delayMillis: Long, responseCode: String?) { retryCode = responseCode }
    }
}
