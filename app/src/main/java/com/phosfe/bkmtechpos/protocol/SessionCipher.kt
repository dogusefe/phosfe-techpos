package com.phosfe.bkmtechpos.protocol

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

interface PayloadCipher {
    fun encrypt(clear: ByteArray): ByteArray
    fun decrypt(encrypted: ByteArray): ByteArray
}

class TripleDesSessionCipher(key: ByteArray) : PayloadCipher {
    private val key24 = when (key.size) {
        16 -> key + key.copyOfRange(0, 8)
        24 -> key.copyOf()
        else -> throw IllegalArgumentException("3DES session key must contain 16 or 24 bytes")
    }

    override fun encrypt(clear: ByteArray): ByteArray = cipher(Cipher.ENCRYPT_MODE).doFinal(clear)
    override fun decrypt(encrypted: ByteArray): ByteArray = cipher(Cipher.DECRYPT_MODE).doFinal(encrypted)

    private fun cipher(mode: Int): Cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding").apply {
        init(mode, SecretKeySpec(key24, "DESede"), IvParameterSpec(ByteArray(8)))
    }
}

