package com.phosfe.bkmtechpos.parameters.tables

data class VbinParameters(val format: Int, val additions: List<BinParameters>, val removals: List<String>)

data class BinParameters(
    val bin: String,
    val cardBrand: Char,
    val issuerId: Int,
    val acquirerId: Int,
    val brandSharing: Int,
    val cardType: Char,
    val permissions: ByteArray
)
