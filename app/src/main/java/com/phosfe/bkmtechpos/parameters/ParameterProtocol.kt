package com.phosfe.bkmtechpos.parameters

import com.phosfe.bkmtechpos.protocol.BkmTag
import com.phosfe.bkmtechpos.protocol.BkmTlv
import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.terminal.ParameterVersion
import com.phosfe.bkmtechpos.terminal.ParameterVersionCodec
import com.phosfe.bkmtechpos.terminal.StanSource
import com.phosfe.bkmtechpos.terminal.TerminalClock
import com.phosfe.bkmtechpos.terminal.TerminalProfile

enum class ParameterLoadReason(val wireValue: Int) { PERIODIC(1), CENTRAL(2), INITIAL_INSTALLATION(3) }
enum class TerminalDeviceClass(val wireValue: Int) { POS(0), FISCAL_DEVICE(1), SOFT_POS(2), GMU(3) }

data class FiscalIdentity(val nationalId: String?, val taxNumber: String?) {
    init {
        require(nationalId == null || (nationalId.length == 11 && nationalId.all(Char::isDigit)))
        require(taxNumber == null || (taxNumber.length == 10 && taxNumber.all(Char::isDigit)))
        require(nationalId != null || taxNumber != null) { "At least one fiscal identity is required" }
    }

    fun encode(): ByteArray = (nationalId ?: "".padEnd(11, ' ')).padEnd(11, ' ').ascii() +
        (taxNumber ?: "".padEnd(10, ' ')).padEnd(10, ' ').ascii()
}

data class ParameterRequestProfile(
    val reason: ParameterLoadReason,
    val versions: List<ParameterVersion>,
    val deviceClass: TerminalDeviceClass,
    val fiscalIdentity: FiscalIdentity? = null,
    val communicationTypes: ByteArray = byteArrayOf(1, 0, 0),
    val terminalCapabilities: ByteArray = ByteArray(5)
) {
    init {
        require(communicationTypes.size == 3)
        require(communicationTypes.all { (it.toInt() and 0xFF) in 0..4 })
        require(terminalCapabilities.size == 5)
        if (reason == ParameterLoadReason.INITIAL_INSTALLATION) {
            requireNotNull(fiscalIdentity) { "Initial installation requires fiscal identity" }
        }
    }
}

class ParameterRequestFactory(
    private val terminal: TerminalProfile,
    private val stan: StanSource,
    private val clock: TerminalClock
) {
    fun initial(profile: ParameterRequestProfile): IsoMessage {
        val tags = mutableListOf(
            BkmTag(0x07, ParameterVersionCodec.encode(profile.versions)),
            BkmTag(0x0F, profile.communicationTypes.copyOf()),
            BkmTag(0x23, profile.terminalCapabilities.copyOf()),
            BkmTag(0x30, byteArrayOf(profile.deviceClass.wireValue.toByte())),
            BkmTag(0x35, byteArrayOf(profile.reason.wireValue.toByte()))
        )
        profile.fiscalIdentity?.let { tags += BkmTag(0x0D, it.encode()) }
        return request("900000", null, tags)
    }

    fun continuation(referenceNumber: String, lastChunkStartOffset: Long, capabilities: ByteArray): IsoMessage {
        require(referenceNumber.length == 12)
        require(lastChunkStartOffset in 0..0xFFFF_FFFFL)
        require(capabilities.size == 5)
        return request(
            "900001",
            referenceNumber,
            listOf(
                BkmTag(0x09, lastChunkStartOffset.u32()),
                BkmTag(0x23, capabilities.copyOf())
            )
        )
    }

    private fun request(processingCode: String, rrn: String?, tags: List<BkmTag>): IsoMessage {
        val moment = clock.now()
        val fields = linkedMapOf(
            3 to processingCode.ascii(),
            11 to stan.next().ascii(),
            12 to moment.time.ascii(),
            13 to moment.date.ascii(),
            43 to terminal.field43(),
            63 to BkmTlv.encode(tags)
        )
        rrn?.let { fields[37] = it.ascii() }
        return IsoMessage("0800", fields)
    }
}

data class ParameterChunk(
    val descriptor: PackageDescriptor,
    val data: ByteArray,
    val referenceNumber: String,
    val finalChunk: Boolean
)

object ParameterResponseParser {
    fun parse(request: IsoMessage, response: IsoMessage): ParameterChunk {
        require(request.messageType == "0800" && response.messageType == "0810")
        require(response.text(11) == request.text(11)) { "Parameter response STAN mismatch" }
        val processingCode = response.text(3)
        require(processingCode == "900000" || processingCode == "900001") { "Unexpected parameter response processing code" }
        val responseCode = response.text(39) ?: error("Parameter response has no F39")
        require(responseCode == "00") { "Parameter download rejected with F39=$responseCode" }
        val descriptorValue = BkmTlv.find(response.fields[63], 0x08)
            ?: error("Parameter response has no package descriptor tag 0x08")
        return ParameterChunk(
            PackageDescriptor.decode(descriptorValue),
            response.fields[62]?.copyOf() ?: byteArrayOf(),
            response.text(37) ?: error("Parameter response has no F37"),
            processingCode == "900000"
        )
    }
}

private fun String.ascii(): ByteArray = toByteArray(Charsets.US_ASCII)
private fun Long.u32(): ByteArray = byteArrayOf(
    (this ushr 24).toByte(), (this ushr 16).toByte(), (this ushr 8).toByte(), toByte()
)
