package com.phosfe.bkmtechpos.parameters.tables

object VbinTableDecoder {
    fun decode(data: ByteArray): VbinParameters {
        val reader = TableBinaryReader(data)
        val format = reader.u8("VBIN format").also { require(it == 0 || it == 1) }
        val additions = (0 until reader.u16("VBIN addition count")).map {
            BinParameters(
                bin = reader.bcdWithFiller("VBIN bin"),
                cardBrand = reader.ascii(1, "VBIN card brand").single().also { require(it in "ACDJMTV-") },
                issuerId = reader.u16("VBIN issuer id"),
                acquirerId = reader.u16("VBIN acquirer id"),
                brandSharing = reader.u8("VBIN brand sharing"),
                cardType = reader.ascii(1, "VBIN card type").single().also { require(it in "DCP") },
                permissions = reader.bytes(2, "VBIN permissions")
            )
        }
        val removals = if (format == 1) (0 until reader.u16("VBIN removal count")).map {
            reader.bcdWithFiller("VBIN removed bin")
        } else emptyList()
        reader.requireEnd("VBIN")
        return VbinParameters(format, additions, removals)
    }
}
