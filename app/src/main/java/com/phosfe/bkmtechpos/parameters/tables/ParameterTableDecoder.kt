package com.phosfe.bkmtechpos.parameters.tables

import com.phosfe.bkmtechpos.parameters.ParameterTableBlock

sealed interface DecodedParameterTable {
    val source: ParameterTableBlock
    data class Vterm(override val source: ParameterTableBlock, val value: VtermParameters) : DecodedParameterTable
    data class Vbin(override val source: ParameterTableBlock, val value: VbinParameters) : DecodedParameterTable
    data class Veod(override val source: ParameterTableBlock, val value: VeodParameters) : DecodedParameterTable
    data class Vcomm2(override val source: ParameterTableBlock, val value: Vcomm2Parameters) : DecodedParameterTable
    data class EmvAids(override val source: ParameterTableBlock, val value: List<EmvAidRecord>) : DecodedParameterTable
    data class CaKeys(override val source: ParameterTableBlock, val value: List<CaPublicKeyRecord>) : DecodedParameterTable
    data class Raw(override val source: ParameterTableBlock) : DecodedParameterTable
}

object ParameterTableDecoder {
    fun decode(block: ParameterTableBlock): DecodedParameterTable {
        if (block.deletesExistingTable) return DecodedParameterTable.Raw(block)
        return when (block.type) {
            ParameterTableType.VTERM -> DecodedParameterTable.Vterm(block, VtermTableDecoder.decode(block.data))
            ParameterTableType.VBIN -> DecodedParameterTable.Vbin(block, VbinTableDecoder.decode(block.data))
            ParameterTableType.VEOD -> DecodedParameterTable.Veod(block, VeodTableDecoder.decode(block.data))
            ParameterTableType.VCOMM2 -> DecodedParameterTable.Vcomm2(block, Vcomm2TableDecoder.decode(block.data))
            ParameterTableType.VEMV_CONTACT,
            ParameterTableType.VEMV_CL_PP3,
            ParameterTableType.VEMV_CL_PP2,
            ParameterTableType.VEMV_CL_PW2 -> DecodedParameterTable.EmvAids(block, EmvTableDecoder.aidRecords(block.data))
            ParameterTableType.VEMV_CAK -> DecodedParameterTable.CaKeys(block, EmvTableDecoder.caPublicKeys(block.data))
            else -> DecodedParameterTable.Raw(block)
        }
    }
}
