package com.phosfe.bkmtechpos.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class RsaRemoteLoadingCryptoTest {
    @Test fun hostCanUnwrapItmkAfterMskAndRsaLayers() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(1024) }.generateKeyPair()
        val publicKey = pair.public as RSAPublicKey
        val immk = ByteArray(16) { (it + 1).toByte() }
        val msk = ByteArray(16) { (it + 17).toByte() }
        val challenge = ByteArray(8) { (it + 33).toByte() }
        val itmk = ByteArray(16) { (it + 49).toByte() }
        val crypto = RsaRemoteLoadingCrypto()
        val encryptedMsk = des(msk, immk, Cipher.ENCRYPT_MODE)
        val recoveredMsk = crypto.recoverMsk(immk, encryptedMsk)
        assertArrayEquals(msk, recoveredMsk)
        assertTrue(crypto.verifyTerminalChallenge(msk, des(challenge, msk, Cipher.ENCRYPT_MODE), challenge))
        assertArrayEquals(challenge, des(crypto.encryptHostChallenge(msk, challenge), msk, Cipher.DECRYPT_MODE))

        val ersa = crypto.buildErsaBlock(itmk, msk, publicKey.modulus.toString(16).padStart(256, '0'), publicKey.publicExponent.toString(16))
        val rsaBlock = des(ersa, msk, Cipher.DECRYPT_MODE)
        val unwrapped = Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
            init(Cipher.DECRYPT_MODE, pair.private)
            doFinal(rsaBlock)
        }
        assertArrayEquals(itmk, unwrapped)
    }

    private fun des(block: ByteArray, key: ByteArray, mode: Int): ByteArray =
        Cipher.getInstance("DESede/ECB/NoPadding").run {
            init(mode, SecretKeySpec(key + key.copyOfRange(0, 8), "DESede"))
            doFinal(block)
        }
}
