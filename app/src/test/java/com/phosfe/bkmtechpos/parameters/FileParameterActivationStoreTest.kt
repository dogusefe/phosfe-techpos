package com.phosfe.bkmtechpos.parameters

import com.phosfe.bkmtechpos.transaction.AesGcmJournalCipher
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import javax.crypto.spec.SecretKeySpec

class FileParameterActivationStoreTest {
    @Test fun missingTablesRemainAndZeroLengthBlockDeletesOnlyItsTable() {
        val directory = Files.createTempDirectory("phosfe-params-").toFile()
        try {
            val file = directory.resolve("parameters.bin")
            val store = FileParameterActivationStore(file, cipher())
            store.activateAtomically(listOf(
                ParameterTableBlock(1, 0, "VTERM", 1, byteArrayOf(1)),
                ParameterTableBlock(4, 0, "VCOMM", 1, byteArrayOf(4))
            ))
            FileParameterActivationStore(file, cipher()).activateAtomically(listOf(
                ParameterTableBlock(1, 0, "VTERM", 2, byteArrayOf(2)),
                ParameterTableBlock(4, 0, "VCOMM", 2, byteArrayOf())
            ))
            val reopened = FileParameterActivationStore(file, cipher()).currentTables()
            assertEquals(listOf(1), reopened.map { it.type })
            assertEquals(2L, reopened.single().version)
            assertArrayEquals(byteArrayOf(2), reopened.single().data)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun interruptedSwapRecoversLastCompleteSnapshot() {
        val directory = Files.createTempDirectory("phosfe-params-recovery-").toFile()
        try {
            val file = directory.resolve("parameters.bin")
            val store = FileParameterActivationStore(file, cipher())
            store.activateAtomically(listOf(ParameterTableBlock(1, 0, "VTERM", 1, byteArrayOf(9))))
            check(file.renameTo(directory.resolve("parameters.bin.bak")))
            val recovered = FileParameterActivationStore(file, cipher()).currentTables()
            assertEquals(1L, recovered.single().version)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun cipher() = AesGcmJournalCipher(
        SecretKeySpec(ByteArray(32) { (it + 11).toByte() }, "AES")
    )
}

