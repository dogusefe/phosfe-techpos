package com.phosfe.bkmtechpos.host

import com.phosfe.bkmtechpos.protocol.BkmFrameCodec
import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.protocol.PayloadCipher
import com.phosfe.bkmtechpos.protocol.TerminalRoute

fun interface IsoExchange { fun exchange(request: IsoMessage): IsoMessage }

class BkmSession(
    private val route: TerminalRoute,
    private val transport: HostTransport,
    private val cipher: PayloadCipher,
    private val frameCodec: BkmFrameCodec = BkmFrameCodec()
) : IsoExchange {
    override fun exchange(request: IsoMessage): IsoMessage {
        val frame = frameCodec.encode(route, request, cipher)
        val reply = frameCodec.decode(transport.exchange(frame), cipher)
        require(reply.route.hostId == route.hostId) { "Reply host id mismatch" }
        require(reply.route.vendorId == route.vendorId) { "Reply vendor id mismatch" }
        require(reply.route.serialNumber == route.serialNumber) { "Reply serial number mismatch" }
        return reply.message
    }
}

