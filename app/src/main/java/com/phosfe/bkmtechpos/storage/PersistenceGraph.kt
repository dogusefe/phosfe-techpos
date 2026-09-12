package com.phosfe.bkmtechpos.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.phosfe.bkmtechpos.parameters.ParameterActivationStore
import com.phosfe.bkmtechpos.parameters.ValidatedParameterActivationStore
import com.phosfe.bkmtechpos.data.local.TerminalDatabase
import com.phosfe.bkmtechpos.transaction.AesGcmJournalCipher
import com.phosfe.bkmtechpos.transaction.JournalCipher
import com.phosfe.bkmtechpos.transaction.ReversalJournal
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class PersistenceGraph private constructor(context: Context) {
    val database: TerminalDatabase = TerminalDatabase.open(context)
    private val payloadCipher: JournalCipher = AesGcmJournalCipher(
        key = loadOrCreatePayloadKey(context.packageName),
        associatedData = "${context.packageName}.persistence.payloads.v1".toByteArray(Charsets.US_ASCII)
    )
    val reversals: ReversalJournal = RoomReversalJournal(database.deliveryDebts(), payloadCipher)
    val deliveries = DurableDeliveryQueue(database.deliveryDebts(), payloadCipher)
    val parameters: ParameterActivationStore = ValidatedParameterActivationStore(
        RoomParameterActivationStore(database)
    )
    val payments = PaymentLedger(database, payloadCipher)
    val batches = RoomBatchLedger(database, payloadCipher)
    val settlementReceipts = RoomSettlementReceiptWriter(database.settlements())

    companion object {
        @Volatile private var instance: PersistenceGraph? = null

        fun create(context: Context): PersistenceGraph = instance ?: synchronized(this) {
            instance ?: PersistenceGraph(context.applicationContext).also { instance = it }
        }

        private fun loadOrCreatePayloadKey(packageName: String): SecretKey {
            val alias = "$packageName.persistence.payloads.v1"
            val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            (store.getKey(alias, null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
                init(
                    KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                generateKey()
            }
        }

        private const val KEYSTORE = "AndroidKeyStore"
    }
}
