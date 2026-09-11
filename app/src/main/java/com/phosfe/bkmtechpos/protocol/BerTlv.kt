package com.phosfe.bkmtechpos.protocol

import java.io.ByteArrayOutputStream

data class TlvEntry(val tag: Int, val value: ByteArray)

object BerTlv {
    fun encode(entries: List<TlvEntry>): ByteArray = ByteArrayOutputStream().also { output ->
        entries.forEach { entry ->
            require(entry.tag in 0..0xFFFF)
            if (entry.tag <= 0xFF) output.write(entry.tag) else {
                output.write(entry.tag ushr 8)
                output.write(entry.tag and 0xFF)
            }
            writeLength(output, entry.value.size)
            output.write(entry.value)
        }
    }.toByteArray()

    fun decode(data: ByteArray): List<TlvEntry> {
        val result = mutableListOf<TlvEntry>()
        var offset = 0
        while (offset < data.size) {
            var tag = data[offset++].toInt() and 0xFF
            if (tag and 0x1F == 0x1F) {
                require(offset < data.size)
                tag = (tag shl 8) or (data[offset++].toInt() and 0xFF)
            }
            require(offset < data.size)
            val firstLength = data[offset++].toInt() and 0xFF
            val length = if (firstLength and 0x80 == 0) firstLength else {
                val count = firstLength and 0x7F
                require(count in 1..2 && offset + count <= data.size)
                var value = 0
                repeat(count) { value = (value shl 8) or (data[offset++].toInt() and 0xFF) }
                value
            }
            require(offset + length <= data.size)
            result += TlvEntry(tag, data.copyOfRange(offset, offset + length))
            offset += length
        }
        return result
    }

    private fun writeLength(output: ByteArrayOutputStream, length: Int) {
        require(length <= 0xFFFF)
        when {
            length < 0x80 -> output.write(length)
            length <= 0xFF -> { output.write(0x81); output.write(length) }
            else -> { output.write(0x82); output.write(length ushr 8); output.write(length and 0xFF) }
        }
    }
}

