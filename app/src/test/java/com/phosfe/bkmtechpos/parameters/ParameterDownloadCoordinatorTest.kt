package com.phosfe.bkmtechpos.parameters

import com.phosfe.bkmtechpos.host.IsoExchange
import com.phosfe.bkmtechpos.protocol.BkmTag
import com.phosfe.bkmtechpos.protocol.BkmTlv
import com.phosfe.bkmtechpos.protocol.IsoMessage
import com.phosfe.bkmtechpos.terminal.ParameterVersion
import com.phosfe.bkmtechpos.terminal.RotatingStan
import com.phosfe.bkmtechpos.terminal.TerminalClock
import com.phosfe.bkmtechpos.terminal.TerminalProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.CRC32

class ParameterDownloadCoordinatorTest {
    private val clock = TerminalClock(Clock.fixed(Instant.parse("2026-09-12T14:22:33Z"), ZoneOffset.UTC))
    private val terminal = TerminalProfile("SERIAL000001", "PHS", "POS", "0100", "3500", 12)

    @Test fun chunksAreValidatedThenActivatedOnceAndContinuationEchoesChunkStart() {
        val packet = ParameterPackageTest.table(1, "VTERM", 7, byteArrayOf(1, 2)) +
            ParameterPackageTest.table(5, "VEMVCON", 9, byteArrayOf(3, 4, 5))
        val crc = CRC32().apply { update(packet) }.value
        val split = packet.size / 2
        var call = 0
        val store = CapturingStore()
        val exchange = IsoExchange { request ->
            call++
            if (call == 2) {
                assertEquals("900001", request.text(3))
                assertEquals(0L, BkmTlv.find(request.fields[63], 0x09)!!.u32())
            }
            val offset = if (call == 1) 0 else split
            val data = if (call == 1) packet.copyOfRange(0, split) else packet.copyOfRange(split, packet.size)
            parameterReply(request, if (call == 1) "900001" else "900000", offset, packet.size, crc, data)
        }
        val coordinator = ParameterDownloadCoordinator(
            exchange,
            ParameterRequestFactory(terminal, RotatingStan(), clock),
            store
        )
        val report = coordinator.download(
            ParameterRequestProfile(
                ParameterLoadReason.PERIODIC,
                listOf(ParameterVersion(1, 6)),
                TerminalDeviceClass.POS
            )
        )
        assertEquals(2, report.chunkCount)
        assertEquals(1, store.activationCount)
        assertEquals(setOf(1, 5), report.tableTypes)
        assertTrue(report.requiresKernelReload)
    }

    @Test fun nonEmvTableDoesNotRequestKernelReload() {
        val packet = ParameterPackageTest.table(1, "VTERM", 1, byteArrayOf(7))
        val crc = CRC32().apply { update(packet) }.value
        val store = CapturingStore()
        val coordinator = ParameterDownloadCoordinator(
            IsoExchange { request -> parameterReply(request, "900000", 0, packet.size, crc, packet) },
            ParameterRequestFactory(terminal, RotatingStan(), clock),
            store
        )
        val report = coordinator.download(ParameterRequestProfile(ParameterLoadReason.CENTRAL, emptyList(), TerminalDeviceClass.POS))
        assertFalse(report.requiresKernelReload)
    }

    private fun parameterReply(
        request: IsoMessage,
        processingCode: String,
        offset: Int,
        fullSize: Int,
        crc: Long,
        data: ByteArray
    ): IsoMessage = IsoMessage("0810", mapOf(
        3 to processingCode.toByteArray(),
        11 to request.fields.getValue(11),
        12 to request.fields.getValue(12),
        13 to request.fields.getValue(13),
        37 to "123456789012".toByteArray(),
        39 to "00".toByteArray(),
        43 to terminal.field43(),
        62 to data,
        63 to BkmTlv.encode(listOf(BkmTag(0x08, PackageDescriptor(0, offset.toLong(), fullSize.toLong(), crc).encode())))
    ))

    private class CapturingStore : ParameterActivationStore {
        var activationCount = 0
        var tables: List<ParameterTableBlock> = emptyList()
        override fun activateAtomically(tables: List<ParameterTableBlock>) {
            activationCount++
            this.tables = tables
        }
    }

    private fun ByteArray.u32(): Long =
        ((this[0].toLong() and 0xFF) shl 24) or ((this[1].toLong() and 0xFF) shl 16) or
            ((this[2].toLong() and 0xFF) shl 8) or (this[3].toLong() and 0xFF)
}

