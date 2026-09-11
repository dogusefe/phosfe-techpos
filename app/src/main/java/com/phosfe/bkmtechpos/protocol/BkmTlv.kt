package com.phosfe.bkmtechpos.protocol

import java.io.ByteArrayOutputStream

data class BkmTag(val id: Int, val value: ByteArray) {
    init {
        require(id in 0..0xFF) { "BKM tag must fit in one byte" }
        require(value.size <= 0xFFFF) { "BKM tag value is too long" }
    }
}

/** BKM F48/F63 TLV: one-byte tag, two-byte unsigned binary length, raw value. */
object BkmTlv {
    fun encode(tags: List<BkmTag>): ByteArray = ByteArrayOutputStream().also { output ->
        tags.forEach { tag ->
            output.write(tag.id)
            output.write(tag.value.size ushr 8)
            output.write(tag.value.size and 0xFF)
            output.write(tag.value)
        }
    }.toByteArray()

    fun decode(data: ByteArray): List<BkmTag> {
        val tags = mutableListOf<BkmTag>()
        var offset = 0
        while (offset < data.size) {
            require(offset + 3 <= data.size) { "Truncated BKM TLV header" }
            val id = data[offset++].toInt() and 0xFF
            val length = ((data[offset++].toInt() and 0xFF) shl 8) or (data[offset++].toInt() and 0xFF)
            require(offset + length <= data.size) { "Truncated BKM TLV value for tag 0x${id.toString(16)}" }
            tags += BkmTag(id, data.copyOfRange(offset, offset + length))
            offset += length
        }
        return tags
    }

    fun find(data: ByteArray?, id: Int): ByteArray? = data?.let(::decode)?.firstOrNull { it.id == id }?.value
}

