package com.phosfe.bkmtechpos.parameters.tables

object VeodTableDecoder {
    fun decode(data: ByteArray): VeodParameters {
        val reader = TableBinaryReader(data)
        val result = VeodParameters(
            maximumTransactions = reader.u16("VEOD maximum transactions"),
            automaticEodEnabled = reader.u8("VEOD automatic status") == 1,
            automaticEodTime = reader.bcd(4, "VEOD automatic time"),
            retryLimit = reader.u8("VEOD retry limit"),
            retryPeriodMinutes = reader.u8("VEOD retry period"),
            minimumWaitAfterManualMinutes = reader.u8("VEOD manual wait")
        )
        reader.requireEnd("VEOD")
        return result
    }
}
