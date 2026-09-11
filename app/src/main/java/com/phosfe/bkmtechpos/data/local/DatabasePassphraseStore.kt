package com.phosfe.bkmtechpos.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps the random SQLCipher passphrase wrapped by a non-exportable Android Keystore key. */
class DatabasePassphraseStore(private val context: Context) {
    fun getOrCreate(): ByteArray {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        preferences.getString(SEALED_PASSPHRASE, null)?.let { return open(Base64.decode(it, Base64.NO_WRAP)) }
        val passphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        val sealed = seal(passphrase)
        check(preferences.edit().putString(SEALED_PASSPHRASE, Base64.encodeToString(sealed, Base64.NO_WRAP)).commit()) {
            "Could not persist the database passphrase"
        }
        return passphrase
    }

    private fun seal(clear: ByteArray): ByteArray {
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val encrypted = cipher(Cipher.ENCRYPT_MODE, iv).doFinal(clear)
        return byteArrayOf(FORMAT_VERSION) + iv + encrypted
    }

    private fun open(sealed: ByteArray): ByteArray {
        require(sealed.size >= 29 && sealed[0] == FORMAT_VERSION)
        val iv = sealed.copyOfRange(1, 13)
        return cipher(Cipher.DECRYPT_MODE, iv).doFinal(sealed.copyOfRange(13, sealed.size))
    }

    private fun cipher(mode: Int, iv: ByteArray): Cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(mode, key(), GCMParameterSpec(128, iv))
        updateAAD(AAD)
    }

    private fun key(): SecretKey {
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

    private companion object {
        const val PREFERENCES = "phosfe_database_key_v1"
        const val SEALED_PASSPHRASE = "sealed_passphrase"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "phosfe.techpos.database.wrapper.v1"
        const val FORMAT_VERSION: Byte = 1
        val AAD = "com.phosfe.bkmtechpos.database-key.v1".toByteArray(Charsets.US_ASCII)
    }
}
