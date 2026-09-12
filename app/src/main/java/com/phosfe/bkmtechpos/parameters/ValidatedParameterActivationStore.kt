package com.phosfe.bkmtechpos.parameters

import com.phosfe.bkmtechpos.parameters.tables.ParameterTableDecoder

/** Parses known non-empty tables before delegating the atomic persistence operation. */
class ValidatedParameterActivationStore(
    private val delegate: ParameterActivationStore
) : ParameterActivationStore {
    override fun activateAtomically(tables: List<ParameterTableBlock>) {
        tables.filterNot(ParameterTableBlock::deletesExistingTable)
            .forEach(ParameterTableDecoder::decode)
        delegate.activateAtomically(tables)
    }
}
