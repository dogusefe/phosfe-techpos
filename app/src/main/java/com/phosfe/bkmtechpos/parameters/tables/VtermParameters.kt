package com.phosfe.bkmtechpos.parameters.tables

data class VtermParameters(
    val startBatchNumber: String,
    val startStan: String,
    val generalExtendedData: ByteArray,
    val acquirers: List<AcquirerParameters>
)

data class AcquirerParameters(
    val acquirerId: String,
    val acquirerName: String,
    val terminalId: String,
    val merchantNumber: String,
    val brandSharing: Int,
    val selectionText: String,
    val receiptCode: String,
    val merchantName: String,
    val merchantAddress: String,
    val merchantCity: String,
    val merchantPhone: String,
    val merchantCategoryCode: String,
    val transactionCategoryCode: String,
    val supportPhone: String,
    val authorizationPhone: String,
    val nationalId: String,
    val taxNumber: String,
    val taxOffice: String,
    val doctorPos: Int,
    val doctorVatRate: ByteArray,
    val doctorWithholdingRate: ByteArray,
    val offlineSurchargeRate: ByteArray,
    val defaultCurrency: String,
    val permissions: ByteArray,
    val extendedData: ByteArray
)
