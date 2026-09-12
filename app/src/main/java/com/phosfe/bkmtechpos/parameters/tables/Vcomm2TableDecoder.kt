package com.phosfe.bkmtechpos.parameters.tables

import com.phosfe.bkmtechpos.protocol.BkmTlv

object Vcomm2TableDecoder {
    fun decode(data: ByteArray): Vcomm2Parameters {
        val tags = BkmTlv.decode(data).associateBy { it.id }
        val url = tags[0x01]?.value?.toString(Charsets.US_ASCII)?.also { require(it.isNotBlank()) }
            ?: error("VCOMM2 URL tag is missing")
        val port = tags[0x02]?.value?.toString(Charsets.US_ASCII)?.toIntOrNull()
            ?: error("VCOMM2 port tag is missing or invalid")
        require(port in 1..65_535)
        return Vcomm2Parameters(url, port)
    }
}
