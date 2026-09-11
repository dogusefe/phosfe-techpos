package com.phosfe.bkmtechpos.protocol

data class IsoMessage(
    val messageType: String,
    val fields: Map<Int, ByteArray>
) {
    init {
        require(messageType.length == 4 && messageType.all(Char::isDigit))
        require(fields.keys.all { it in 2..64 }) { "Only primary bitmap fields are supported" }
    }

    fun text(field: Int): String? = fields[field]?.toString(Charsets.US_ASCII)
}

enum class WireFormat { BCD_NUMERIC, BCD_NUMERIC_RIGHT_PAD, ASCII, BINARY, TRACK }

data class FieldRule(
    val number: Int,
    val maxLength: Int,
    val format: WireFormat,
    val variableLengthDigits: Int = 0
)

object BkmFieldRules {
    val rules: Map<Int, FieldRule> = listOf(
        FieldRule(2, 19, WireFormat.BCD_NUMERIC_RIGHT_PAD, 2),
        FieldRule(3, 6, WireFormat.BCD_NUMERIC),
        FieldRule(4, 12, WireFormat.BCD_NUMERIC),
        FieldRule(11, 6, WireFormat.BCD_NUMERIC),
        FieldRule(12, 6, WireFormat.BCD_NUMERIC),
        FieldRule(13, 4, WireFormat.BCD_NUMERIC),
        FieldRule(14, 4, WireFormat.BCD_NUMERIC),
        FieldRule(22, 4, WireFormat.BCD_NUMERIC),
        FieldRule(23, 3, WireFormat.BCD_NUMERIC),
        FieldRule(24, 4, WireFormat.BCD_NUMERIC),
        FieldRule(25, 2, WireFormat.BCD_NUMERIC),
        FieldRule(32, 11, WireFormat.BCD_NUMERIC, 2),
        FieldRule(35, 37, WireFormat.TRACK, 2),
        FieldRule(37, 12, WireFormat.ASCII),
        FieldRule(38, 6, WireFormat.ASCII),
        FieldRule(39, 2, WireFormat.ASCII),
        FieldRule(41, 8, WireFormat.ASCII),
        FieldRule(42, 15, WireFormat.ASCII),
        FieldRule(43, 40, WireFormat.ASCII),
        FieldRule(48, 999, WireFormat.BINARY, 4),
        FieldRule(49, 4, WireFormat.BCD_NUMERIC),
        FieldRule(52, 8, WireFormat.BINARY),
        FieldRule(55, 999, WireFormat.BINARY, 4),
        FieldRule(57, 16, WireFormat.ASCII),
        FieldRule(62, 999, WireFormat.BINARY, 4),
        FieldRule(63, 999, WireFormat.BINARY, 4)
    ).associateBy(FieldRule::number)
}
