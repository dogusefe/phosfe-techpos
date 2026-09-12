package com.phosfe.bkmtechpos.parameters.tables

import com.phosfe.bkmtechpos.parameters.ParameterTableBlock
import com.phosfe.bkmtechpos.protocol.BerTlv
import com.phosfe.bkmtechpos.protocol.BkmTag
import com.phosfe.bkmtechpos.protocol.BkmTlv
import com.phosfe.bkmtechpos.protocol.TlvEntry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ParameterTableDecoderTest {
    @Test fun vtermReadsBatchStanAndEmptyAcquirerSet() {
        val data = ByteArray(8) + byteArrayOf(0, 0, 7, 0, 0, 0x42, 0, 0, 0)
        val decoded = ParameterTableDecoder.decode(block(ParameterTableType.VTERM, data)) as DecodedParameterTable.Vterm
        assertEquals("000007", decoded.value.startBatchNumber)
        assertEquals("000042", decoded.value.startStan)
        assertEquals(0, decoded.value.acquirers.size)
    }

    @Test fun vbinDeltaReadsAdditionAndRemoval() {
        val bin = byteArrayOf(0x12, 0x34, 0x56, 0xFF.toByte())
        val data = byteArrayOf(1, 0, 1) + bin + byteArrayOf('M'.code.toByte(), 0, 42, 0, 7, 1, 'C'.code.toByte(), 1, 0, 0, 1) + bin
        val decoded = ParameterTableDecoder.decode(block(ParameterTableType.VBIN, data)) as DecodedParameterTable.Vbin
        assertEquals("123456", decoded.value.additions.single().bin)
        assertEquals("123456", decoded.value.removals.single())
        assertEquals('M', decoded.value.additions.single().cardBrand)
    }

    @Test fun veodReadsSchedulerFields() {
        val data = byteArrayOf(0, 10, 1, 0x12, 0x30, 3, 5, 2)
        val decoded = ParameterTableDecoder.decode(block(ParameterTableType.VEOD, data)) as DecodedParameterTable.Veod
        assertEquals(10, decoded.value.maximumTransactions)
        assertEquals(true, decoded.value.automaticEodEnabled)
        assertEquals("1230", decoded.value.automaticEodTime)
        assertEquals(3, decoded.value.retryLimit)
    }

    @Test fun vcomm2ReadsBkmUrlAndPortTags() {
        val data = BkmTlv.encode(listOf(BkmTag(1, "techpos.example".toByteArray()), BkmTag(2, "12500".toByteArray())))
        val decoded = ParameterTableDecoder.decode(block(ParameterTableType.VCOMM2, data)) as DecodedParameterTable.Vcomm2
        assertEquals("techpos.example", decoded.value.url)
        assertEquals(12500, decoded.value.port)
    }

    @Test fun emvAidAndCaKeyRecordsExposeRequiredTags() {
        val aid = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x03)
        val aidData = BerTlv.encode(listOf(TlvEntry(0xBF8B02, BerTlv.encode(listOf(TlvEntry(0x9F06, aid), TlvEntry(0x9F09, byteArrayOf(0x00, 0x01)))))))
        val aidDecoded = ParameterTableDecoder.decode(block(ParameterTableType.VEMV_CONTACT, aidData)) as DecodedParameterTable.EmvAids
        assertArrayEquals(aid, aidDecoded.value.single().aid)

        val caData = BerTlv.encode(listOf(TlvEntry(0xBF8B01, BerTlv.encode(listOf(
            TlvEntry(0xDF8B01, byteArrayOf(1, 2, 3, 4, 5)), TlvEntry(0xDF8B02, byteArrayOf(0x01)),
            TlvEntry(0xDF8B05, byteArrayOf(0x11)), TlvEntry(0xDF8B06, byteArrayOf(0x03))
        )))))
        val caDecoded = ParameterTableDecoder.decode(block(ParameterTableType.VEMV_CAK, caData)) as DecodedParameterTable.CaKeys
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), caDecoded.value.single().rid)
        assertArrayEquals(byteArrayOf(0x03), caDecoded.value.single().exponent)
    }

    private fun block(type: Int, data: ByteArray) = ParameterTableBlock(type, 0, "test", 1, data)
}
