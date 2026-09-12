package com.phosfe.bkmtechpos.security

/** Vendor-neutral contract for RSA transport of a TR-31 key block. Plain keys never cross this API. */
data class RsaTr31KeyBlock(val header: String, val encryptedPayload: ByteArray, val mac: ByteArray) {
    init {
        require(header.length == 16) { "TR-31 header must be 16 characters" }
        require(header.all { it.code in 0x20..0x7E }) { "TR-31 header must be printable ASCII" }
        require(encryptedPayload.isNotEmpty()) { "TR-31 encrypted payload is empty" }
        require(mac.size in 8..32) { "TR-31 MAC length is invalid" }
    }
}

interface RsaTr31KeyBlockTransport {
    fun importKeyBlock(block: RsaTr31KeyBlock): Boolean
}
