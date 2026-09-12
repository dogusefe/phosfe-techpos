package com.phosfe.bkmtechpos.parameters.tables

import com.phosfe.bkmtechpos.protocol.BerTlv

object EmvTableDecoder {
    fun aidRecords(data: ByteArray): List<EmvAidRecord> = BerTlv.decode(data)
        .filter { it.tag == 0xBF8B02 }
        .map { EmvAidRecord(BerTlv.decode(it.value).associate { entry -> entry.tag to entry.value }) }
        .also { require(it.isNotEmpty()) { "EMV table contains no BF8B02 AID records" } }

    fun caPublicKeys(data: ByteArray): List<CaPublicKeyRecord> = BerTlv.decode(data)
        .filter { it.tag == 0xBF8B01 }
        .map { CaPublicKeyRecord(BerTlv.decode(it.value).associate { entry -> entry.tag to entry.value }) }
        .also { require(it.isNotEmpty()) { "CA table contains no BF8B01 key records" } }
}
