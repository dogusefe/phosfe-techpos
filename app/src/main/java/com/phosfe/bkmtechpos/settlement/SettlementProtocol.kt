package com.phosfe.bkmtechpos.settlement

import com.phosfe.bkmtechpos.protocol.BkmTag
import com.phosfe.bkmtechpos.protocol.BkmTlv
import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.protocol.PackedDecimal
import com.phosfe.bkmtechpos.terminal.HostSignals
import com.phosfe.bkmtechpos.terminal.StanSource
import com.phosfe.bkmtechpos.terminal.TerminalClock
import com.phosfe.bkmtechpos.terminal.TerminalProfile
import java.io.ByteArrayOutputStream

data class AmountAggregate(val count: Int, val amountMinor: Long) {
    init {
        require(count in 0..65_535)
        require(amountMinor in 0..999_999_999_999)
    }
}

data class CurrencySettlement(
    val numericCode: String,
    val onlinePositive: AmountAggregate,
    val onlineNegative: AmountAggregate,
    val offlinePositive: AmountAggregate,
    val offlineNegative: AmountAggregate
) {
    init { require(numericCode.length == 3 && numericCode.all(Char::isDigit)) }
}

data class BatchSettlement(val batchNumber: Int, val currencies: List<CurrencySettlement>) {
    init {
        require(batchNumber in 1..999_999)
        require(currencies.size in 0..10)
        require(currencies.map { it.numericCode }.distinct().size == currencies.size)
    }
}

object SettlementTotalsCodec {
    fun encode(settlement: BatchSettlement): ByteArray = ByteArrayOutputStream().also { output ->
        output.write(PackedDecimal.encode(settlement.batchNumber.toString().padStart(6, '0')))
        val currencies = settlement.currencies.ifEmpty {
            listOf(CurrencySettlement("949", AmountAggregate(0, 0), AmountAggregate(0, 0), AmountAggregate(0, 0), AmountAggregate(0, 0)))
        }
        output.write(currencies.size)
        currencies.forEach { currency ->
            output.write(currency.numericCode.toByteArray(Charsets.US_ASCII))
            writeAggregate(output, currency.onlinePositive)
            writeAggregate(output, currency.onlineNegative)
            writeAggregate(output, currency.offlinePositive)
            writeAggregate(output, currency.offlineNegative)
        }
    }.toByteArray()

    private fun writeAggregate(output: ByteArrayOutputStream, aggregate: AmountAggregate) {
        output.write(aggregate.count ushr 8)
        output.write(aggregate.count and 0xFF)
        if (aggregate.count == 0) {
            require(aggregate.amountMinor == 0L) { "Empty settlement bucket cannot carry an amount" }
            output.write(0)
            return
        }
        val amount = PackedDecimal.encode(aggregate.amountMinor.toString())
        require(amount.size <= 255)
        output.write(amount.size)
        output.write(amount)
    }
}

enum class ReconciliationStep(val processingCode: String) {
    PRIMARY("910000"),
    AFTER_BATCH_UPLOAD("920000")
}

class SettlementRequestFactory(
    private val profile: TerminalProfile,
    private val stan: StanSource,
    private val clock: TerminalClock
) {
    fun create(
        step: ReconciliationStep,
        totals: BatchSettlement,
        terminalCapabilities: ByteArray? = null
    ): IsoMessage {
        require(terminalCapabilities == null || terminalCapabilities.size == 5)
        val moment = clock.now()
        val tags = mutableListOf(BkmTag(0x11, SettlementTotalsCodec.encode(totals)))
        terminalCapabilities?.let { tags += BkmTag(0x23, it.copyOf()) }
        return IsoMessage("0500", mapOf(
            3 to step.processingCode.ascii(),
            11 to stan.next().ascii(),
            12 to moment.time.ascii(),
            13 to moment.date.ascii(),
            43 to profile.field43(),
            63 to BkmTlv.encode(tags)
        ))
    }
}

sealed interface SettlementReply {
    val referenceNumber: String
    val signals: HostSignals
    data class Settled(override val referenceNumber: String, override val signals: HostSignals) : SettlementReply
    data class UploadRequired(override val referenceNumber: String, override val signals: HostSignals) : SettlementReply
    data class Rejected(val responseCode: String, override val referenceNumber: String, override val signals: HostSignals) : SettlementReply
}

object SettlementResponseParser {
    fun parse(request: IsoMessage, response: IsoMessage): SettlementReply {
        require(request.messageType == "0500" && response.messageType == "0510")
        require(response.text(3) == request.text(3)) { "Settlement processing code mismatch" }
        require(response.text(11) == request.text(11)) { "Settlement STAN mismatch" }
        val reference = response.text(37) ?: error("Settlement response has no F37")
        val code = response.text(39) ?: error("Settlement response has no F39")
        val signals = HostSignals.fromField48(response.fields[48])
        return when (code) {
            "00" -> SettlementReply.Settled(reference, signals)
            "95" -> SettlementReply.UploadRequired(reference, signals)
            else -> SettlementReply.Rejected(code, reference, signals)
        }
    }
}

data class ApprovedBatchTransaction(
    val pan: String,
    val authorizationRequest: IsoMessage,
    val authorizationResponse: IsoMessage,
    val recordId: String? = null
) {
    init {
        require(pan.length in 12..19 && pan.all(Char::isDigit))
        require(authorizationRequest.messageType == "0100" || authorizationRequest.messageType == "0200")
        require(authorizationResponse.messageType == if (authorizationRequest.messageType == "0100") "0110" else "0210")
        require(authorizationResponse.text(39) == "00")
    }
}

class BatchUploadRequestFactory {
    fun create(transaction: ApprovedBatchTransaction): IsoMessage {
        val original = transaction.authorizationRequest
        val reply = transaction.authorizationResponse
        val fields = linkedMapOf<Int, ByteArray>()
        fields[2] = transaction.pan.ascii()
        fields[3] = original.required(3)
        fields[4] = original.required(4)
        // BKM batch upload replays the original authorization trace/time/date; generating a
        // fresh STAN here breaks correlation with the host's original approval.
        fields[11] = original.required(11)
        fields[12] = original.required(12)
        fields[13] = original.required(13)
        fields[14] = original.required(14)
        listOf(22, 25, 41, 42, 43, 49).forEach { fields[it] = original.required(it) }
        fields[63] = batchUploadField63(original.required(63))
        fields[32] = original.required(32)
        fields[37] = reply.required(37)
        fields[38] = reply.required(38)
        fields[39] = reply.required(39)
        return IsoMessage("0320", fields)
    }
}

private fun batchUploadField63(original: ByteArray): ByteArray {
    val tags = BkmTlv.decode(original)
    val allowed = listOf(0x0A, 0x0C, 0x25)
    allowed.forEach { id -> require(tags.count { it.id == id } <= 1) { "Duplicate F63 tag $id" } }
    require(tags.singleOrNull { it.id == 0x0C }?.value?.size == 18) { "Batch upload requires 18-byte F63 tag 0C" }
    require(tags.firstOrNull { it.id == 0x0A }?.value?.size in listOf(null, 1)) { "Invalid F63 tag 0A" }
    require(tags.firstOrNull { it.id == 0x25 }?.value?.size in listOf(null, 4)) { "Invalid F63 tag 25" }
    return BkmTlv.encode(allowed.mapNotNull { id -> tags.firstOrNull { it.id == id } })
}

object BatchUploadResponseParser {
    fun requireApproved(request: IsoMessage, response: IsoMessage) {
        require(request.messageType == "0320" && response.messageType == "0330")
        require(response.text(3) == request.text(3)) { "Batch upload processing code mismatch" }
        require(response.text(11) == request.text(11)) { "Batch upload STAN mismatch" }
        require(response.text(12) == request.text(12)) { "Batch upload time mismatch" }
        require(response.text(13) == request.text(13)) { "Batch upload date mismatch" }
        require(response.text(39) in setOf("00", "08", "11")) { "Batch upload rejected with F39=${response.text(39)}" }
    }
}

private fun IsoMessage.required(number: Int): ByteArray =
    fields[number]?.copyOf() ?: error("Required F$number is missing")

private fun String.ascii(): ByteArray = toByteArray(Charsets.US_ASCII)
