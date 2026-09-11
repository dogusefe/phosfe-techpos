package com.phosfe.bkmtechpos.parameters

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

class ParameterPackageTest {
    @Test fun orderedChunksAreJoinedOnlyAfterSizeAndCrcValidation() {
        val full = byteArrayOf(1, 2, 3, 4, 5)
        val crc = CRC32().apply { update(full) }.value
        val collector = PackageCollector()
        collector.append(PackageDescriptor(0, 0, full.size.toLong(), crc), full.copyOfRange(0, 2))
        collector.append(PackageDescriptor(0, 2, full.size.toLong(), crc), full.copyOfRange(2, 5))
        assertArrayEquals(full, collector.finish())
    }

    @Test(expected = IllegalArgumentException::class)
    fun offsetGapIsRejected() {
        val collector = PackageCollector()
        collector.append(PackageDescriptor(0, 0, 4, 0), byteArrayOf(1, 2))
        collector.append(PackageDescriptor(0, 3, 4, 0), byteArrayOf(4))
    }

    @Test fun multipleTablesAndDeleteMarkerDecodeWithoutPartialApply() {
        val packet = table(9, "VEMVCAK", 1, byteArrayOf(1, 2, 3)) +
            table(4, "VCOMM", 2, byteArrayOf())
        val decoded = ParameterTablePacket.decode(packet)
        assertEquals(listOf(9, 4), decoded.map { it.type })
        assertTrue(decoded[0].requiresKernelReload)
        assertTrue(decoded[1].deletesExistingTable)
    }

    @Test(expected = IllegalArgumentException::class)
    fun trailingPartialHeaderIsRejected() {
        ParameterTablePacket.decode(table(1, "VTERM", 1, byteArrayOf(1)) + byteArrayOf(0))
    }

    companion object {
        fun table(type: Int, name: String, version: Long, data: ByteArray): ByteArray =
            ByteArrayOutputStream().also { output ->
                output.write(type ushr 8); output.write(type)
                output.write(0)
                val nameBytes = name.toByteArray(Charsets.US_ASCII)
                output.write(nameBytes.size ushr 8); output.write(nameBytes.size)
                output.write(nameBytes)
                output.write((version ushr 24).toInt()); output.write((version ushr 16).toInt())
                output.write((version ushr 8).toInt()); output.write(version.toInt())
                output.write(data.size ushr 24); output.write(data.size ushr 16)
                output.write(data.size ushr 8); output.write(data.size)
                output.write(data)
            }.toByteArray()
    }
}

