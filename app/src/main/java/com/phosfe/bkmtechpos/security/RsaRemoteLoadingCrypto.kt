package com.phosfe.bkmtechpos.security

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/** Cryptographic primitives for the two-step RSA remote-loading exchange. */
class RsaRemoteLoadingCrypto(private val rsa: RsaKeyExchangeCipher = RsaKeyExchangeCipher()) {
    fun recoverMsk(immk: ByteArray, encryptedMsk: ByteArray): ByteArray {
        require(encryptedMsk.size == 16)
        return tripleDes(immk, encryptedMsk, Cipher.DECRYPT_MODE)
    }

    fun verifyTerminalChallenge(msk: ByteArray, encryptedChallenge: ByteArray, expected: ByteArray): Boolean {
        require(encryptedChallenge.size == 8 && expected.size == 8)
        return tripleDes(msk, encryptedChallenge, Cipher.DECRYPT_MODE).contentEquals(expected)
    }

    fun encryptHostChallenge(msk: ByteArray, challenge: ByteArray): ByteArray {
        require(challenge.size == 8)
        return tripleDes(msk, challenge, Cipher.ENCRYPT_MODE)
    }

    fun buildErsaBlock(itmk: ByteArray, msk: ByteArray, modulusHex: String, exponentHex: String): ByteArray {
        require(itmk.size == 16)
        val rsaBlock = rsa.encryptSessionKey(itmk, modulusHex, exponentHex)
        require(rsaBlock.size % 8 == 0)
        return tripleDes(msk, rsaBlock, Cipher.ENCRYPT_MODE)
    }

    private fun tripleDes(key: ByteArray, block: ByteArray, mode: Int): ByteArray {
        require(key.size == 16 || key.size == 24) { "3DES key must be 16 or 24 bytes" }
        require(block.isNotEmpty() && block.size % 8 == 0) { "3DES block must be aligned" }
        val expanded = if (key.size == 16) key + key.copyOfRange(0, 8) else key.copyOf()
        return Cipher.getInstance("DESede/ECB/NoPadding").run {
            init(mode, SecretKeySpec(expanded, "DESede"))
            doFinal(block)
        }
    }
}
