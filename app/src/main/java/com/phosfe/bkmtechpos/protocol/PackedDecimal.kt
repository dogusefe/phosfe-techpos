package com.phosfe.bkmtechpos.protocol

object PackedDecimal {
    fun encode(digits: String, padRight: Boolean = false): ByteArray {
        require(digits.all(Char::isDigit)) { "Packed decimal accepts digits only" }
        val padded = when {
            digits.length % 2 == 0 -> digits
            padRight -> digits + "F"
            else -> "0$digits"
        }
        return ByteArray(padded.length / 2) { index ->
            val high = padded[index * 2].digitToInt(16)
            val low = padded[index * 2 + 1].digitToInt(16)
            ((high shl 4) or low).toByte()
        }
    }

    fun decode(bytes: ByteArray, digitCount: Int, padRight: Boolean = false): String {
        require(digitCount >= 0)
        val raw = buildString(bytes.size * 2) {
            bytes.forEach { byte ->
                append(((byte.toInt() ushr 4) and 0xF).toString(16))
                append((byte.toInt() and 0xF).toString(16))
            }
        }.uppercase()
        val value = if (digitCount % 2 == 0) raw else if (padRight) raw.dropLast(1) else raw.drop(1)
        require(value.length == digitCount && value.all(Char::isDigit)) { "Invalid packed decimal" }
        return value
    }

    fun encodeTrack(value: String): ByteArray {
        val normalized = value.uppercase().replace('=', 'D')
        require(normalized.all { it.isDigit() || it == 'D' }) { "Invalid track-2 character" }
        val padded = if (normalized.length % 2 == 0) normalized else normalized + "F"
        return ByteArray(padded.length / 2) { index ->
            ((padded[index * 2].digitToInt(16) shl 4) or padded[index * 2 + 1].digitToInt(16)).toByte()
        }
    }

    fun decodeTrack(bytes: ByteArray, characterCount: Int): String {
        val raw = bytes.joinToString("") { byte -> "%02X".format(byte.toInt() and 0xFF) }
            .take(characterCount)
        require(raw.all { it.isDigit() || it == 'D' }) { "Invalid packed track-2 data" }
        return raw.replace('D', '=')
    }
}
