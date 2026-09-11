package com.phosfe.bkmtechpos.parameters

import com.phosfe.bkmtechpos.transaction.JournalCipher
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

class FileParameterActivationStore(
    private val file: File,
    private val cipher: JournalCipher
) : ParameterActivationStore {
    private val nextFile get() = File(file.parentFile, "${file.name}.new")
    private val backupFile get() = File(file.parentFile, "${file.name}.bak")

    @Synchronized
    override fun activateAtomically(tables: List<ParameterTableBlock>) {
        require(tables.map { it.type }.distinct().size == tables.size) {
            "A parameter activation cannot contain duplicate table types"
        }
        val updated = currentTables().associateBy { it.type }.toMutableMap()
        tables.forEach { table ->
            if (table.deletesExistingTable) updated.remove(table.type)
            else updated[table.type] = table.copy(data = table.data.copyOf())
        }
        writeNext(updated.values.sortedBy { it.type })
        swapSnapshot()
    }

    @Synchronized
    fun currentTables(): List<ParameterTableBlock> {
        recoverInterruptedSwap()
        if (!file.exists()) return emptyList()
        val clear = cipher.open(file.readBytes())
        return decode(clear)
    }

    private fun writeNext(tables: List<ParameterTableBlock>) {
        file.parentFile?.mkdirs()
        val sealed = cipher.seal(encode(tables))
        FileOutputStream(nextFile).use { stream ->
            stream.write(sealed)
            stream.flush()
            stream.fd.sync()
        }
    }

    private fun swapSnapshot() {
        if (backupFile.exists()) check(backupFile.delete()) { "Could not clear stale parameter backup" }
        if (file.exists()) check(file.renameTo(backupFile)) { "Could not back up active parameter snapshot" }
        if (!nextFile.renameTo(file)) {
            if (!file.exists() && backupFile.exists()) backupFile.renameTo(file)
            error("Could not activate new parameter snapshot")
        }
        if (backupFile.exists()) check(backupFile.delete()) { "Could not remove parameter backup" }
    }

    private fun recoverInterruptedSwap() {
        if (!file.exists() && backupFile.exists()) {
            check(backupFile.renameTo(file)) { "Could not recover parameter snapshot backup" }
        }
        if (file.exists() && nextFile.exists()) check(nextFile.delete()) { "Could not discard incomplete parameter snapshot" }
    }

    private fun encode(tables: List<ParameterTableBlock>): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(FORMAT_VERSION)
            output.writeInt(tables.size)
            tables.forEach { table ->
                output.writeInt(table.type)
                output.writeInt(table.action)
                output.writeUTF(table.reference)
                output.writeLong(table.version)
                output.writeInt(table.data.size)
                output.write(table.data)
            }
        }
    }.toByteArray()

    private fun decode(clear: ByteArray): List<ParameterTableBlock> =
        DataInputStream(ByteArrayInputStream(clear)).use { input ->
            require(input.readInt() == FORMAT_VERSION) { "Unsupported parameter snapshot format" }
            val count = input.readInt().also { require(it in 0..65_535) }
            val tables = buildList(count) {
                repeat(count) {
                    val type = input.readInt().also { require(it in 0..0xFFFF) }
                    val action = input.readInt().also { require(it in 0..0xFF) }
                    val reference = input.readUTF()
                    val version = input.readLong().also { require(it in 0..0xFFFF_FFFFL) }
                    val length = input.readInt().also { require(it in 0..16_777_216) }
                    add(ParameterTableBlock(type, action, reference, version, ByteArray(length).also(input::readFully)))
                }
            }
            require(input.read() == -1) { "Trailing parameter snapshot data" }
            require(tables.map { it.type }.distinct().size == tables.size) { "Duplicate parameter table in snapshot" }
            tables
        }

    private companion object { const val FORMAT_VERSION = 1 }
}

