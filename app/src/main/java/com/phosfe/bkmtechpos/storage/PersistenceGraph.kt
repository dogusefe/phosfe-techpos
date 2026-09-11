package com.phosfe.bkmtechpos.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.phosfe.bkmtechpos.parameters.ParameterActivationStore
import com.phosfe.bkmtechpos.data.local.TerminalDatabase
import com.phosfe.bkmtechpos.transaction.AesGcmJournalCipher
import com.phosfe.bkmtechpos.transaction.JournalCipher
import com.phosfe.bkmtechpos.transaction.ReversalJournal
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class PersistenceGraph private constructor(context: Context) {
    val database: TerminalDatabase = TerminalDatabase.open(context)
    private val payloadCipher: JournalCipher = AesGcmJournalCipher(loadOrCreatePayloadKey())
    val reversals: ReversalJournal = RoomReversalJournal(database.deliveryDebts(), payloadCipher)
    val deliveries = DurableDeliveryQueue(database.deliveryDebts(), payloadCipher)
    val parameters: ParameterActivationStore = RoomParameterActivationStore(database.parameters())
    val payments = PaymentLedger(database, payloadCipher)
    val batches = RoomBatchLedger(database, payloadCipher)

    companion object {
        @Volatile private var instance: PersistenceGraph? = null

        fun create(context: Context): PersistenceGraph = instance ?: synchronized(this) {
            instance ?: PersistenceGraph(context.applicationContext).also { instance = it }
        }

        private fun loadOrCreatePayloadKey(): SecretKey {
            val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
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
        private const val KEY_ALIAS = "phosfe.techpos.persistence.payloads.v1"
    }
}
