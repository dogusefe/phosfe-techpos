package com.phosfe.bkmtechpos.parameters

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

data class PackageDescriptor(
    val compression: Int,
    val offset: Long,
    val fullSize: Long,
    val fullCrc32: Long
) {
    init {
        require(compression in 0..255)
        require(offset in 0..0xFFFF_FFFFL)
        require(fullSize in 0..0xFFFF_FFFFL)
        require(fullCrc32 in 0..0xFFFF_FFFFL)
    }

    fun encode(): ByteArray = byteArrayOf(compression.toByte()) +
        offset.u32() + fullSize.u32() + fullCrc32.u32()

    companion object {
        fun decode(value: ByteArray): PackageDescriptor {
            require(value.size == 13) { "Parameter package descriptor must contain 13 bytes" }
            return PackageDescriptor(
                value[0].toInt() and 0xFF,
                value.u32(1),
                value.u32(5),
                value.u32(9)
            )
        }
    }
}

class PackageCollector {
    private val bytes = ByteArrayOutputStream()
    private var expected: PackageDescriptor? = null
    private var completed = false
    var lastChunkOffset: Long? = null
        private set

    val receivedSize: Int get() = bytes.size()

    fun append(descriptor: PackageDescriptor, chunk: ByteArray) {
        check(!completed) { "Parameter package collection is already complete" }
        require(descriptor.compression == 0) { "Unsupported parameter compression ${descriptor.compression}" }
        val first = expected
        if (first == null) {
            require(descriptor.offset == 0L) { "First parameter chunk offset must be zero" }
            expected = descriptor
        } else {
            require(descriptor.fullSize == first.fullSize) { "Parameter package size changed between chunks" }
            require(descriptor.fullCrc32 == first.fullCrc32) { "Parameter package CRC changed between chunks" }
            require(descriptor.compression == first.compression) { "Parameter package compression changed" }
        }
        require(descriptor.offset == bytes.size().toLong()) {
            "Expected chunk offset ${bytes.size()}, received ${descriptor.offset}"
        }
        require(bytes.size().toLong() + chunk.size <= descriptor.fullSize) { "Parameter chunk exceeds full package size" }
        require(chunk.isNotEmpty() || descriptor.fullSize == 0L) { "Empty chunk cannot advance an incomplete package" }
        lastChunkOffset = descriptor.offset
        bytes.write(chunk)
        completed = bytes.size().toLong() == descriptor.fullSize
    }

    fun finish(): ByteArray {
        val descriptor = expected ?: error("No parameter chunk was received")
        require(completed) { "Parameter package is incomplete: ${bytes.size()}/${descriptor.fullSize}" }
        val result = bytes.toByteArray()
        val actual = CRC32().apply { update(result) }.value
        require(actual == descriptor.fullCrc32) {
            "Parameter package CRC mismatch: expected ${descriptor.fullCrc32}, actual $actual"
        }
        return result
    }
}

data class ParameterTableBlock(
    val type: Int,
    val action: Int,
    val reference: String,
    val version: Long,
    val data: ByteArray
) {
    val deletesExistingTable: Boolean get() = data.isEmpty()
    val requiresKernelReload: Boolean get() = type in 5..9
}

object ParameterTablePacket {
    fun decode(packet: ByteArray): List<ParameterTableBlock> {
        val reader = BinaryCursor(packet)
        val tables = mutableListOf<ParameterTableBlock>()
        while (reader.remaining > 0) {
            val type = reader.u16("table type")
            val action = reader.u8("action")
            val referenceLength = reader.u16("reference length")
            require(referenceLength <= 1024) { "Parameter table reference exceeds 1024 bytes" }
            val reference = reader.bytes(referenceLength, "reference").toString(Charsets.US_ASCII)
            require(reference.all { it.code in 0x20..0x7E }) { "Parameter table reference is not ASCII" }
            val version = reader.u32("version")
            val dataLength = reader.u32("data length")
            require(dataLength <= Int.MAX_VALUE.toLong()) { "Parameter table is too large" }
            val data = reader.bytes(dataLength.toInt(), "table data")
            tables += ParameterTableBlock(type, action, reference, version, data)
        }
        return tables
    }
}

private class BinaryCursor(private val data: ByteArray) {
    private var offset = 0
    val remaining: Int get() = data.size - offset

    fun u8(label: String): Int = bytes(1, label)[0].toInt() and 0xFF
    fun u16(label: String): Int = bytes(2, label).let { ((it[0].toInt() and 0xFF) shl 8) or (it[1].toInt() and 0xFF) }
    fun u32(label: String): Long = bytes(4, label).u32(0)
    fun bytes(length: Int, label: String): ByteArray {
        require(length >= 0 && offset + length <= data.size) { "Truncated parameter $label" }
        return data.copyOfRange(offset, offset + length).also { offset += length }
    }
}

private fun Long.u32(): ByteArray = byteArrayOf(
    (this ushr 24).toByte(), (this ushr 16).toByte(), (this ushr 8).toByte(), toByte()
)

private fun ByteArray.u32(offset: Int): Long =
    ((this[offset].toLong() and 0xFF) shl 24) or
        ((this[offset + 1].toLong() and 0xFF) shl 16) or
        ((this[offset + 2].toLong() and 0xFF) shl 8) or
        (this[offset + 3].toLong() and 0xFF)

