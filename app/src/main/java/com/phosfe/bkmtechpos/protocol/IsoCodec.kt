package com.phosfe.bkmtechpos.protocol

import java.io.ByteArrayOutputStream

class IsoCodec(private val rules: Map<Int, FieldRule> = BkmFieldRules.rules) {
    fun encode(message: IsoMessage): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(PackedDecimal.encode(message.messageType))
        output.write(bitmap(message.fields.keys))
        message.fields.toSortedMap().forEach { (number, value) ->
            val rule = rules[number] ?: error("No field rule for F$number")
            output.write(encodeField(rule, value))
        }
        return output.toByteArray()
    }

    fun decode(payload: ByteArray): IsoMessage {
        require(payload.size >= 10) { "ISO message is shorter than MTI and bitmap" }
        var offset = 0
        val messageType = PackedDecimal.decode(payload.copyOfRange(offset, offset + 2), 4)
        offset += 2
        val bitmap = payload.copyOfRange(offset, offset + 8)
        offset += 8
        val decoded = linkedMapOf<Int, ByteArray>()
        for (number in 2..64) {
            if (!isSet(bitmap, number)) continue
            val rule = rules[number] ?: error("No field rule for F$number")
            val length = if (rule.variableLengthDigits > 0) {
                val byteCount = (rule.variableLengthDigits + 1) / 2
                require(offset + byteCount <= payload.size) { "Missing F$number length" }
                val digits = PackedDecimal.decode(payload.copyOfRange(offset, offset + byteCount), rule.variableLengthDigits)
                offset += byteCount
                digits.toInt()
            } else rule.maxLength
            require(length <= rule.maxLength) { "F$number exceeds ${rule.maxLength}" }
            val byteCount = byteLength(rule, length)
            require(offset + byteCount <= payload.size) { "Truncated F$number" }
            val wire = payload.copyOfRange(offset, offset + byteCount)
            offset += byteCount
            decoded[number] = when (rule.format) {
                WireFormat.BCD_NUMERIC -> PackedDecimal.decode(wire, length).toByteArray(Charsets.US_ASCII)
                WireFormat.BCD_NUMERIC_RIGHT_PAD -> PackedDecimal.decode(wire, length, padRight = true).toByteArray(Charsets.US_ASCII)
                WireFormat.TRACK -> PackedDecimal.decodeTrack(wire, length).toByteArray(Charsets.US_ASCII)
                else -> wire
            }
        }
        require(offset == payload.size) { "Unexpected ${payload.size - offset} trailing bytes" }
        return IsoMessage(messageType, decoded)
    }

    private fun encodeField(rule: FieldRule, value: ByteArray): ByteArray {
        val logicalLength = when (rule.format) {
            WireFormat.BCD_NUMERIC, WireFormat.BCD_NUMERIC_RIGHT_PAD, WireFormat.TRACK -> value.toString(Charsets.US_ASCII).length
            else -> value.size
        }
        require(logicalLength <= rule.maxLength) { "F${rule.number} exceeds ${rule.maxLength}" }
        if (rule.variableLengthDigits == 0) require(logicalLength == rule.maxLength) {
            "F${rule.number} must have length ${rule.maxLength}, was $logicalLength"
        }
        val output = ByteArrayOutputStream()
        if (rule.variableLengthDigits > 0) {
            output.write(PackedDecimal.encode(logicalLength.toString().padStart(rule.variableLengthDigits, '0')))
        }
        val body = when (rule.format) {
            WireFormat.BCD_NUMERIC -> PackedDecimal.encode(value.toString(Charsets.US_ASCII))
            WireFormat.BCD_NUMERIC_RIGHT_PAD -> PackedDecimal.encode(value.toString(Charsets.US_ASCII), padRight = true)
            WireFormat.TRACK -> PackedDecimal.encodeTrack(value.toString(Charsets.US_ASCII))
            else -> value
        }
        output.write(body)
        return output.toByteArray()
    }

    private fun byteLength(rule: FieldRule, logicalLength: Int): Int = when (rule.format) {
        WireFormat.BCD_NUMERIC, WireFormat.BCD_NUMERIC_RIGHT_PAD, WireFormat.TRACK -> (logicalLength + 1) / 2
        else -> logicalLength
    }

    private fun bitmap(fields: Set<Int>): ByteArray = ByteArray(8).also { bytes ->
        fields.forEach { number ->
            val zeroBased = number - 1
            bytes[zeroBased / 8] = (bytes[zeroBased / 8].toInt() or (1 shl (7 - zeroBased % 8))).toByte()
        }
    }

    private fun isSet(bitmap: ByteArray, number: Int): Boolean {
        val zeroBased = number - 1
        return bitmap[zeroBased / 8].toInt() and (1 shl (7 - zeroBased % 8)) != 0
    }
}
