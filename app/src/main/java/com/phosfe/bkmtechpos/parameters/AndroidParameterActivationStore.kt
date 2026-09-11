package com.phosfe.bkmtechpos.parameters

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.phosfe.bkmtechpos.transaction.AesGcmJournalCipher
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object AndroidParameterActivationStore {
    fun create(context: Context): FileParameterActivationStore = FileParameterActivationStore(
        File(context.noBackupFilesDir, "payment-state/parameters.bin"),
        AesGcmJournalCipher(loadOrCreateKey())
    )

    private fun loadOrCreateKey(): SecretKey {
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
    private const val KEY_ALIAS = "phosfe.techpos.parameters.aes.v1"
}

