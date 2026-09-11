package com.phosfe.bkmtechpos.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

@JvmInline
value class MinorAmount(val value: Long) {
    init { require(value >= 0) { "Amount cannot be negative" } }

    fun display(currencyCode: String = "TRY"): String {
        val currency = Currency.getInstance(currencyCode)
        val major = BigDecimal(value).movePointLeft(currency.defaultFractionDigits)
            .setScale(currency.defaultFractionDigits, RoundingMode.UNNECESSARY)
        return "%s %s".format(currency.symbol, major.toPlainString())
    }
}

enum class PaymentOperation(val title: String) {
    SALE("Satış"),
    REFUND("İade"),
    VOID("İptal"),
    PRE_AUTH("Ön provizyon"),
    CLOSE_PRE_AUTH("Provizyon kapama"),
    POINTS("Puan işlemi"),
    QR("Karekod")
}

enum class CardChannel { CONTACT, CONTACTLESS, MAGNETIC, MANUAL, QR }

data class PaymentOrder(
    val operation: PaymentOperation,
    val amount: MinorAmount,
    val currencyCode: String = "TRY",
    val installmentCount: Int = 1,
    val externalReference: String? = null
)

sealed interface PaymentOutcome {
    data class Approved(val authorizationCode: String, val referenceNumber: String) : PaymentOutcome
    data class Declined(val responseCode: String, val message: String) : PaymentOutcome
    data class Failed(val reason: String, val retryable: Boolean) : PaymentOutcome
    data object Cancelled : PaymentOutcome
}

