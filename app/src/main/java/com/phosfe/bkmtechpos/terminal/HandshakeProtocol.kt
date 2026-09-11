package com.phosfe.bkmtechpos.terminal

import com.phosfe.bkmtechpos.protocol.BkmTag
import com.phosfe.bkmtechpos.protocol.BkmTlv
import com.phosfe.bkmtechpos.protocol.IsoMessage

enum class HandshakeReason(val processingCode: String) {
    PARAMETERS_ACTIVATED("710000"),
    PERIODIC("710001")
}

class HandshakeRequestFactory(
    private val profile: TerminalProfile,
    private val stan: StanSource,
    private val clock: TerminalClock
) {
    fun create(reason: HandshakeReason, versions: List<ParameterVersion>): IsoMessage {
        val moment = clock.now()
        return IsoMessage("0800", linkedMapOf(
            3 to reason.processingCode.ascii(),
            11 to stan.next().ascii(),
            12 to moment.time.ascii(),
            13 to moment.date.ascii(),
            43 to profile.field43(),
            63 to BkmTlv.encode(listOf(BkmTag(0x07, ParameterVersionCodec.encode(versions))))
        ))
    }
}

enum class DeferredAction(val wireValue: Int) {
    AFTER_SUCCESSFUL_END_OF_DAY(0),
    AFTER_NEXT_TRANSACTION(1),
    BEFORE_NEXT_TRANSACTION(2),
    IGNORE(3);

    companion object {
        fun fromWire(value: Int): DeferredAction = entries.firstOrNull { it.wireValue == value }
            ?: error("Unsupported host signal value $value")
    }
}

data class HostSignals(
    val parameterDownload: DeferredAction,
    val keyExchange: DeferredAction
) {
    companion object {
        val NONE = HostSignals(DeferredAction.IGNORE, DeferredAction.IGNORE)

        fun fromField48(field48: ByteArray?): HostSignals {
            val value = BkmTlv.find(field48, 0x0D) ?: return NONE
            require(value.size == 2) { "Host signal tag 0x0D must contain two bytes" }
            return HostSignals(
                parameterDownload = DeferredAction.fromWire(value[0].toInt() and 0xFF),
                keyExchange = DeferredAction.fromWire(value[1].toInt() and 0xFF)
            )
        }
    }
}

data class HandshakeReply(val referenceNumber: String, val signals: HostSignals)

object HandshakeResponseParser {
    fun parse(request: IsoMessage, response: IsoMessage): HandshakeReply {
        require(response.messageType == "0810")
        require(response.text(3) == request.text(3)) { "Handshake processing code mismatch" }
        require(response.text(11) == request.text(11)) { "Handshake STAN mismatch" }
        val responseCode = response.text(39) ?: error("Handshake response has no F39")
        require(responseCode == "00") { "Handshake declined with response code $responseCode" }
        return HandshakeReply(
            referenceNumber = response.text(37) ?: error("Handshake response has no F37"),
            signals = HostSignals.fromField48(response.fields[48])
        )
    }
}

private fun String.ascii(): ByteArray = toByteArray(Charsets.US_ASCII)

