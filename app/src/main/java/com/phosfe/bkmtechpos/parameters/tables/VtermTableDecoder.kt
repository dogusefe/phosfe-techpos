package com.phosfe.bkmtechpos.parameters.tables

object VtermTableDecoder {
    fun decode(data: ByteArray): VtermParameters {
        val reader = TableBinaryReader(data)
        reader.bytes(8, "VTERM RFU")
        val batch = reader.bcd(6, "VTERM start batch")
        val stan = reader.bcd(6, "VTERM start STAN")
        val generalData = reader.sizedBinary("VTERM general extended data", 65_535)
        val count = reader.u8("VTERM acquirer count")
        require(count <= 32) { "VTERM acquirer count exceeds 32" }
        val acquirers = (0 until count).map {
            AcquirerParameters(
                acquirerId = reader.bcd(4, "acquirer id"),
                acquirerName = reader.sizedAscii("acquirer name", 40),
                terminalId = reader.ascii(8, "acquirer terminal id"),
                merchantNumber = reader.ascii(15, "acquirer merchant number"),
                brandSharing = reader.u8("brand sharing"),
                selectionText = reader.sizedAscii("selection text", 16),
                receiptCode = reader.ascii(2, "BKM receipt code"),
                merchantName = reader.sizedAscii("merchant slip name", 40),
                merchantAddress = reader.sizedAscii("merchant slip address", 164),
                merchantCity = reader.sizedAscii("merchant city", 20),
                merchantPhone = reader.ascii(10, "merchant phone"),
                merchantCategoryCode = reader.ascii(4, "MCC"),
                transactionCategoryCode = reader.ascii(1, "TCC"),
                supportPhone = reader.ascii(10, "support phone"),
                authorizationPhone = reader.ascii(10, "authorization phone"),
                nationalId = reader.ascii(11, "national id"),
                taxNumber = reader.ascii(10, "tax number"),
                taxOffice = reader.sizedAscii("tax office", 20),
                doctorPos = reader.u8("doctor POS"),
                doctorVatRate = reader.bytes(4, "doctor VAT rate"),
                doctorWithholdingRate = reader.bytes(4, "doctor withholding rate"),
                offlineSurchargeRate = reader.bytes(4, "offline surcharge rate"),
                defaultCurrency = reader.ascii(3, "default currency"),
                permissions = reader.bytes(10, "permissions"),
                extendedData = reader.sizedBinary("acquirer extended data", 65_535)
            )
        }
        reader.requireEnd("VTERM")
        return VtermParameters(batch, stan, generalData, acquirers)
    }
}
