package com.phosfe.bkmtechpos.parameters

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidatedParameterActivationStoreTest {
    @Test(expected = IllegalArgumentException::class)
    fun malformedKnownTableIsRejectedBeforeDelegation() {
        var calls = 0
        val store = ValidatedParameterActivationStore(object : ParameterActivationStore {
            override fun activateAtomically(tables: List<ParameterTableBlock>) { calls++ }
        })
        try {
            store.activateAtomically(listOf(ParameterTableBlock(1, 0, "VTERM", 1, byteArrayOf(1))))
        } finally {
            assertEquals(0, calls)
        }
    }

    @Test fun unknownTableRemainsForwardCompatible() {
        var calls = 0
        val store = ValidatedParameterActivationStore(object : ParameterActivationStore {
            override fun activateAtomically(tables: List<ParameterTableBlock>) { calls++ }
        })
        store.activateAtomically(listOf(ParameterTableBlock(99, 0, "FUTURE", 1, byteArrayOf(1, 2))))
        assertEquals(1, calls)
    }
}
