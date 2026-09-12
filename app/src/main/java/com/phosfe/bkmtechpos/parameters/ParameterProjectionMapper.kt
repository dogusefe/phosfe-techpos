package com.phosfe.bkmtechpos.parameters

import com.phosfe.bkmtechpos.data.local.entity.ParameterProjectionEntity
import com.phosfe.bkmtechpos.parameters.tables.*

object ParameterProjectionMapper {
    fun map(block: ParameterTableBlock, decoded: DecodedParameterTable, now: Long): ParameterProjectionEntity =
        ParameterProjectionEntity(block.type, block.reference, block.version, decoded::class.simpleName ?: "Raw", summary(decoded), now)

    private fun summary(decoded: DecodedParameterTable): String = when (decoded) {
        is DecodedParameterTable.Vterm -> "batch=${decoded.value.startBatchNumber};stan=${decoded.value.startStan};acquirers=${decoded.value.acquirers.size}"
        is DecodedParameterTable.Vbin -> "format=${decoded.value.format};additions=${decoded.value.additions.size};removals=${decoded.value.removals.size}"
        is DecodedParameterTable.Veod -> "max=${decoded.value.maximumTransactions};auto=${decoded.value.automaticEodEnabled};time=${decoded.value.automaticEodTime}"
        is DecodedParameterTable.Vcomm2 -> "url=${decoded.value.url};port=${decoded.value.port}"
        is DecodedParameterTable.EmvAids -> "records=${decoded.value.size};aids=${decoded.value.joinToString(",") { it.aid.toHex() }}"
        is DecodedParameterTable.CaKeys -> "records=${decoded.value.size};rids=${decoded.value.joinToString(",") { it.rid.toHex() }}"
        is DecodedParameterTable.Raw -> "raw"
    }

    private fun ByteArray.toHex() = joinToString("") { "%02X".format(it.toInt() and 0xFF) }
}
