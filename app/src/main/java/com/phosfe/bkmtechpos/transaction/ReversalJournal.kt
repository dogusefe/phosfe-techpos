package com.phosfe.bkmtechpos.transaction

import com.phosfe.bkmtechpos.protocol.IsoMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class PendingReversal(val id: String = UUID.randomUUID().toString(), val request: IsoMessage)

interface ReversalJournal {
    fun pending(): PendingReversal?
    fun save(entry: PendingReversal)
    fun clear(expectedId: String)
}

interface JournalCipher {
    fun seal(clear: ByteArray): ByteArray
    fun open(sealed: ByteArray): ByteArray
}

class AesGcmJournalCipher(
    private val key: SecretKey,
    private val random: SecureRandom = SecureRandom(),
    associatedData: ByteArray = DEFAULT_AAD
) : JournalCipher {
    private val associatedData = associatedData.copyOf()
    override fun seal(clear: ByteArray): ByteArray {
        val iv = ByteArray(12).also(random::nextBytes)
        val encrypted = cipher(Cipher.ENCRYPT_MODE, iv).doFinal(clear)
        return byteArrayOf(ENVELOPE_VERSION.toByte()) + iv + encrypted
    }

    override fun open(sealed: ByteArray): ByteArray {
        require(sealed.size >= 1 + 12 + 16) { "Encrypted reversal journal is too short" }
        require(sealed[0].toInt() and 0xFF == ENVELOPE_VERSION) { "Unsupported encrypted journal envelope" }
        val iv = sealed.copyOfRange(1, 13)
        return cipher(Cipher.DECRYPT_MODE, iv).doFinal(sealed.copyOfRange(13, sealed.size))
    }

    private fun cipher(mode: Int, iv: ByteArray): Cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(mode, key, GCMParameterSpec(128, iv))
        updateAAD(associatedData)
    }

    private companion object {
        const val ENVELOPE_VERSION = 1
        val DEFAULT_AAD = "techpos.reversal.v1".toByteArray(Charsets.US_ASCII)
    }
}

class FileReversalJournal(private val file: File, private val cipher: JournalCipher) : ReversalJournal {
    @Synchronized
    override fun pending(): PendingReversal? {
        if (!file.exists()) return null
        val clear = cipher.open(FileInputStream(file).use { it.readBytes() })
        return DataInputStream(ByteArrayInputStream(clear)).use { input ->
            require(input.readInt() == FORMAT_VERSION) { "Unsupported reversal journal format" }
            val id = input.readUTF()
            val messageType = input.readUTF()
            val fieldCount = input.readInt().also { require(it in 0..63) }
            val fields = linkedMapOf<Int, ByteArray>()
            repeat(fieldCount) {
                val number = input.readInt().also { require(it in 2..64) }
                val length = input.readInt().also { require(it in 0..65_535) }
                fields[number] = ByteArray(length).also(input::readFully)
            }
            require(input.read() == -1) { "Trailing reversal journal data" }
            PendingReversal(id, IsoMessage(messageType, fields))
        }
    }

    @Synchronized
    override fun save(entry: PendingReversal) {
        check(!file.exists()) { "A reversal is already pending" }
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.new")
        val clear = ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(FORMAT_VERSION)
                output.writeUTF(entry.id)
                output.writeUTF(entry.request.messageType)
                output.writeInt(entry.request.fields.size)
                entry.request.fields.toSortedMap().forEach { (number, value) ->
                    output.writeInt(number)
                    output.writeInt(value.size)
                    output.write(value)
                }
            }
        }.toByteArray()
        val sealed = cipher.seal(clear)
        FileOutputStream(temporary).use { stream ->
            stream.write(sealed)
            stream.flush()
            stream.fd.sync()
        }
        check(temporary.renameTo(file)) { "Could not atomically replace reversal journal" }
    }

    @Synchronized
    override fun clear(expectedId: String) {
        val current = pending() ?: return
        require(current.id == expectedId) { "Refusing to clear a different reversal" }
        check(file.delete() || !file.exists()) { "Could not delete completed reversal" }
    }

    private companion object { const val FORMAT_VERSION = 1 }
}

object ReversalRequestFactory {
    private val allowedFields = setOf(2, 3, 4, 11, 12, 13, 22, 25, 32, 37, 38, 39, 41, 42, 43, 49, 63)

    fun fromAuthorization(original: IsoMessage, pan: String): IsoMessage {
        require(original.messageType == "0100" || original.messageType == "0200")
        require(pan.length in 12..19 && pan.all(Char::isDigit))
        val fields = (original.fields + (2 to pan.toByteArray(Charsets.US_ASCII))).filterKeys(allowedFields::contains)
            .mapValues { (_, value) -> value.copyOf() }
        require(fields.keys.containsAll(listOf(2, 3, 4, 11, 12, 13, 22, 25, 41, 42, 43, 49, 63))) {
            "Authorization lacks mandatory reversal fields"
        }
        return IsoMessage("0400", fields)
    }
}

class TransactionGate(private val journal: ReversalJournal) {
    fun requireReady() {
        check(journal.pending() == null) { "A pending reversal must be completed before a new transaction" }
    }
}
