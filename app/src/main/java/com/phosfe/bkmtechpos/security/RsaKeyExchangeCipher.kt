package com.phosfe.bkmtechpos.security

import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher

/** RSA transport boundary: only wrapped session material leaves this class. */
class RsaKeyExchangeCipher {
    fun encryptSessionKey(sessionKey: ByteArray, modulusHex: String, exponentHex: String): ByteArray {
        require(sessionKey.isNotEmpty()) { "Session key is empty" }
        val key = publicKey(modulusHex, exponentHex)
        return Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
            init(Cipher.ENCRYPT_MODE, key)
            doFinal(sessionKey)
        }
    }

    private fun publicKey(modulusHex: String, exponentHex: String): PublicKey {
        require(modulusHex.matches(Regex("[0-9A-Fa-f]{256,}"))) { "RSA modulus must be hexadecimal" }
        require(exponentHex.matches(Regex("[0-9A-Fa-f]+"))) { "RSA exponent must be hexadecimal" }
        return KeyFactory.getInstance("RSA").generatePublic(
            RSAPublicKeySpec(BigInteger(modulusHex, 16), BigInteger(exponentHex, 16))
        )
    }
}
