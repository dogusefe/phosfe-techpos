package com.phosfe.bkmtechpos.transaction

import com.phosfe.bkmtechpos.host.IsoExchange
import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.storage.PendingDelivery
import org.junit.Assert.assertEquals
import org.junit.Test

class AdviceDrainCoordinatorTest {
    @Test fun drainStopsAtConfiguredLimit() {
        val port = FakePort(MutableList(5) { PendingDelivery(it.toString(), "OFFLINE_ADVICE", request(it), 1) })
        val dispatcher = AdviceDispatcher(port, IsoExchange { response(it) })
        assertEquals(3, AdviceDrainCoordinator(dispatcher, 3).drain())
        assertEquals(3, port.acknowledged)
    }

    private fun request(stan: Int) = IsoMessage("0220", mapOf(3 to "000000".toByteArray(), 11 to "%06d".format(stan).toByteArray()))
    private fun response(request: IsoMessage) = IsoMessage("0230", mapOf(3 to request.fields.getValue(3), 11 to request.fields.getValue(11), 39 to "00".toByteArray()))

    private class FakePort(private val pending: MutableList<PendingDelivery>) : AdviceDeliveryPort {
        var acknowledged = 0
        override fun next() = pending.removeFirstOrNull()
        override fun acknowledge(id: String, responseCode: String, hostReference: String?) { acknowledged++ }
        override fun retry(id: String, delayMillis: Long, responseCode: String?) = Unit
    }
}
