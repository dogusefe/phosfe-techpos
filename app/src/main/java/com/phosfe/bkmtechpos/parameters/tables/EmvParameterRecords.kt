package com.phosfe.bkmtechpos.parameters.tables

data class EmvAidRecord(val tags: Map<Int, ByteArray>) {
    val aid: ByteArray get() = tags[0x9F06]?.copyOf() ?: error("EMV AID has no 9F06")
}

data class CaPublicKeyRecord(val tags: Map<Int, ByteArray>) {
    val rid: ByteArray get() = tags[0xDF8B01]?.copyOf() ?: error("CA key has no RID")
    val index: ByteArray get() = tags[0xDF8B02]?.copyOf() ?: error("CA key has no index")
    val modulus: ByteArray get() = tags[0xDF8B05]?.copyOf() ?: error("CA key has no modulus")
    val exponent: ByteArray get() = tags[0xDF8B06]?.copyOf() ?: error("CA key has no exponent")
}
