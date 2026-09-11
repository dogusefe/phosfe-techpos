package com.phosfe.bkmtechpos.parameters

import com.phosfe.bkmtechpos.host.IsoExchange

interface ParameterActivationStore {
    /** Must commit every table change together, or leave the previous set untouched. */
    fun activateAtomically(tables: List<ParameterTableBlock>)
}

data class ParameterActivationReport(
    val chunkCount: Int,
    val totalBytes: Int,
    val tableTypes: Set<Int>
) {
    val requiresKernelReload: Boolean get() = tableTypes.any { it in 5..9 }
}

class ParameterDownloadCoordinator(
    private val exchange: IsoExchange,
    private val requests: ParameterRequestFactory,
    private val store: ParameterActivationStore,
    private val maximumChunks: Int = 4_096
) {
    fun download(profile: ParameterRequestProfile): ParameterActivationReport {
        var request = requests.initial(profile)
        val collector = PackageCollector()
        var chunks = 0
        while (true) {
            require(++chunks <= maximumChunks) { "Parameter download exceeded $maximumChunks chunks" }
            val chunk = ParameterResponseParser.parse(request, exchange.exchange(request))
            collector.append(chunk.descriptor, chunk.data)
            if (chunk.finalChunk) {
                val packet = collector.finish()
                val tables = ParameterTablePacket.decode(packet)
                store.activateAtomically(tables)
                return ParameterActivationReport(chunks, packet.size, tables.map { it.type }.toSet())
            }
            request = requests.continuation(
                chunk.referenceNumber,
                collector.lastChunkOffset ?: error("No chunk offset"),
                profile.terminalCapabilities
            )
        }
    }
}

