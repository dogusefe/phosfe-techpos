package com.phosfe.bkmtechpos.security

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPairGenerator

class RsaKeyExchangeCipherTest {
    @Test fun sessionKeyIsWrappedToRsaModulusLength() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val key = pair.public as java.security.interfaces.RSAPublicKey
        val modulus = key.modulus.toString(16).padStart(512, '0')
        val exponent = key.publicExponent.toString(16)
        val encrypted = RsaKeyExchangeCipher().encryptSessionKey(ByteArray(16) { it.toByte() }, modulus, exponent)
        assertEquals(256, encrypted.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun shortModulusIsRejected() {
        RsaKeyExchangeCipher().encryptSessionKey(ByteArray(16), "ABCD", "010001")
    }
}
