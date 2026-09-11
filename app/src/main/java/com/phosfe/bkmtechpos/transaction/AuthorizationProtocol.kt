package com.phosfe.bkmtechpos.transaction

import com.phosfe.bkmtechpos.host.IsoExchange
import com.phosfe.bkmtechpos.protocol.BkmTag
import com.phosfe.bkmtechpos.protocol.BkmTlv
import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.protocol.PackedDecimal
import com.phosfe.bkmtechpos.terminal.HostSignals
import com.phosfe.bkmtechpos.terminal.StanSource
import com.phosfe.bkmtechpos.terminal.TerminalClock
import com.phosfe.bkmtechpos.terminal.TerminalProfile

enum class AuthorizationKind(val messageType: String, val processingCode: String) {
    SALE("0200", "000000"),
    ONLINE_IMPRINTER("0200", "000001"),
    LOYALTY_INSTALLMENT("0200", "000002"),
    LOYALTY_POINTS("0200", "000003"),
    LOYALTY_QUERY("0800", "000004"),
    DEFERRED_INSTALLMENT("0200", "000005"),
    DEFERRED_INSTALLMENT_QUERY("0800", "000006"),
    NON_INSTALLMENT_SALE("0200", "000007"),
    DCC_SALE("0200", "000008"),
    FARMER_CARD_QUERY("0800", "000009"),
    FARMER_CARD_SALE("0200", "000010"),
    FARMER_CARD_CAMPAIGN_SALE("0200", "000011"),
    MATCHED_REFUND("0200", "200000"),
    UNMATCHED_REFUND("0200", "200001"),
    PRE_AUTHORIZATION("0100", "300000"),
    PRE_AUTHORIZATION_CLOSE("0200", "300001"),
    PRE_AUTHORIZATION_VOID_OUTSIDE_BATCH("0200", "300002"),
    QR_CREATE("0800", "400001"),
    QR_CARD_OR_FAST_DATA("0800", "400002")
}

data class BatchTrace(
    val batchNumber: Int,
    val transactionNumber: Int,
    val lastApprovedOnlineBatch: Int = 0,
    val lastApprovedOnlineTransaction: Int = 0,
    val lastApprovedOfflineBatch: Int = 0,
    val lastApprovedOfflineTransaction: Int = 0
) {
    init {
        require(batchNumber in 1..999_999)
        require(transactionNumber in 1..999_999)
        require(listOf(lastApprovedOnlineBatch, lastApprovedOnlineTransaction, lastApprovedOfflineBatch, lastApprovedOfflineTransaction).all { it in 0..999_999 })
    }

    fun encode(): ByteArray = listOf(
        batchNumber,
        transactionNumber,
        lastApprovedOnlineBatch,
        lastApprovedOnlineTransaction,
        lastApprovedOfflineBatch,
        lastApprovedOfflineTransaction
    ).fold(byteArrayOf()) { bytes, value -> bytes + PackedDecimal.encode(value.toString().padStart(6, '0')) }
}

data class AuthorizationInput(
    val kind: AuthorizationKind,
    val amountMinor: Long,
    val entryMode: String,
    val conditionCode: String,
    val terminalId: String,
    val merchantId: String,
    val currencyCode: String = "0949",
    val pan: String? = null,
    val track2: String? = null,
    val pinBlock: ByteArray? = null,
    val emvData: ByteArray? = null,
    val acquirerId: String? = null,
    val batchTrace: BatchTrace
) {
    init {
        require(amountMinor in 0..999_999_999_999)
        require(entryMode.length == 4 && entryMode.all(Char::isDigit))
        require(conditionCode.length == 2 && conditionCode.all(Char::isDigit))
        require(terminalId.length == 8)
        require(merchantId.length == 15)
        require(currencyCode.length == 4 && currencyCode.all(Char::isDigit))
        require(pan == null || (pan.length in 12..19 && pan.all(Char::isDigit)))
        require(pinBlock == null || pinBlock.size == 8)
        require(pan != null || track2 != null || kind.name.startsWith("QR_"))
    }
}

class AuthorizationRequestFactory(
    private val profile: TerminalProfile,
    private val stan: StanSource,
    private val clock: TerminalClock
) {
    fun create(input: AuthorizationInput): IsoMessage {
        val moment = clock.now()
        val fields = linkedMapOf<Int, ByteArray>()
        input.pan?.let { fields[2] = it.ascii() }
        fields[3] = input.kind.processingCode.ascii()
        fields[4] = input.amountMinor.toString().padStart(12, '0').ascii()
        fields[11] = stan.next().ascii()
        fields[12] = moment.time.ascii()
        fields[13] = moment.date.ascii()
        fields[22] = input.entryMode.ascii()
        fields[25] = input.conditionCode.ascii()
        input.acquirerId?.let { fields[32] = it.ascii() }
        input.track2?.let { fields[35] = it.ascii() }
        fields[41] = input.terminalId.ascii()
        fields[42] = input.merchantId.ascii()
        fields[43] = profile.field43()
        fields[49] = input.currencyCode.ascii()
        input.pinBlock?.let { fields[52] = it.copyOf() }
        input.emvData?.let { fields[55] = it.copyOf() }
        fields[63] = BkmTlv.encode(listOf(BkmTag(0x0C, input.batchTrace.encode())))
        return IsoMessage(input.kind.messageType, fields)
    }
}

data class AuthorizationReply(
    val approved: Boolean,
    val responseCode: String,
    val referenceNumber: String?,
    val authorizationCode: String?,
    val emvData: ByteArray?,
    val receiptData: ByteArray?,
    val signals: HostSignals
)

object AuthorizationResponseParser {
    fun parse(request: IsoMessage, response: IsoMessage): AuthorizationReply {
        val expectedType = when (request.messageType) {
            "0100" -> "0110"
            "0200" -> "0210"
            "0800" -> "0810"
            else -> error("Unsupported authorization request MTI ${request.messageType}")
        }
        require(response.messageType == expectedType) { "Authorization response MTI mismatch" }
        require(response.text(3) == request.text(3)) { "Authorization processing code mismatch" }
        require(response.text(11) == request.text(11)) { "Authorization STAN mismatch" }
        val code = response.text(39) ?: error("Authorization response has no F39")
        return AuthorizationReply(
            approved = code == "00",
            responseCode = code,
            referenceNumber = response.text(37),
            authorizationCode = response.text(38),
            emvData = response.fields[55]?.copyOf(),
            receiptData = response.fields[62]?.copyOf(),
            signals = HostSignals.fromField48(response.fields[48])
        )
    }
}

data class AuthorizationExchangeResult(
    val reply: AuthorizationReply,
    /** Non-null approval guard. Clear only after the card/EMV completion succeeds. */
    val completionToken: String?
)

class AuthorizationCoordinator(
    private val exchange: IsoExchange,
    private val reversalJournal: ReversalJournal
) {
    fun authorize(request: IsoMessage, reversalPan: String): AuthorizationExchangeResult {
        require(request.messageType == "0100" || request.messageType == "0200") {
            "Only financial authorizations use the reversal coordinator"
        }
        TransactionGate(reversalJournal).requireReady()
        val pending = PendingReversal(request = ReversalRequestFactory.fromAuthorization(request, reversalPan))
        reversalJournal.save(pending)
        val response = exchange.exchange(request)
        val reply = AuthorizationResponseParser.parse(request, response)
        if (!reply.approved) {
            reversalJournal.clear(pending.id)
            return AuthorizationExchangeResult(reply, null)
        }
        return AuthorizationExchangeResult(reply, pending.id)
    }

    fun confirmCardCompletion(token: String) = reversalJournal.clear(token)
}

private fun String.ascii(): ByteArray = toByteArray(Charsets.US_ASCII)
