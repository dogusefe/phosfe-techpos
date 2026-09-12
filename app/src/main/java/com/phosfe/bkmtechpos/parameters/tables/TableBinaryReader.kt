package com.phosfe.bkmtechpos.parameters.tables

internal class TableBinaryReader(private val source: ByteArray) {
    private var cursor = 0
    val remaining: Int get() = source.size - cursor

    fun u8(label: String): Int = bytes(1, label)[0].toInt() and 0xFF

    fun u16(label: String): Int = bytes(2, label).let {
        ((it[0].toInt() and 0xFF) shl 8) or (it[1].toInt() and 0xFF)
    }

    fun bytes(length: Int, label: String): ByteArray {
        require(length >= 0 && cursor + length <= source.size) { "Truncated $label" }
        return source.copyOfRange(cursor, cursor + length).also { cursor += length }
    }

    fun ascii(length: Int, label: String): String = bytes(length, label).toString(Charsets.US_ASCII)

    fun sizedAscii(label: String, maxLength: Int): String {
        val length = u8("$label length")
        require(length <= maxLength) { "$label exceeds $maxLength bytes" }
        return ascii(length, label)
    }

    fun sizedBinary(label: String, maxLength: Int): ByteArray {
        val length = u16("$label length")
        require(length <= maxLength) { "$label exceeds $maxLength bytes" }
        return bytes(length, label)
    }

    fun bcd(digitCount: Int, label: String): String {
        require(digitCount >= 0)
        val raw = bytes((digitCount + 1) / 2, label).joinToString("") { "%02X".format(it.toInt() and 0xFF) }
        val value = if (digitCount % 2 == 0) raw else raw.drop(1)
        require(value.length == digitCount && value.all(Char::isDigit)) { "Invalid packed decimal $label" }
        return value
    }

    fun bcdWithFiller(label: String): String = buildString {
        bytes(4, label).forEach { byte ->
            val high = (byte.toInt() ushr 4) and 0xF
            val low = byte.toInt() and 0xF
            if (high != 0xF) append(high.toString(16))
            if (low != 0xF) append(low.toString(16))
        }
    }.uppercase().also { require(it.isNotEmpty() && it.all(Char::isDigit)) { "Invalid $label" } }

    fun requireEnd(label: String) = require(remaining == 0) { "Trailing bytes in $label: $remaining" }
}
