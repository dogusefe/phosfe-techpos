package com.phosfe.bkmtechpos.security

import com.phosfe.bkmtechpos.protocol.BkmTag
import com.phosfe.bkmtechpos.protocol.BkmTlv

data class RsaStepOneReply(
    val hostChallenge: ByteArray,
    val encryptedMsk: ByteArray,
    val encryptedTerminalChallenge: ByteArray
)

data class RsaStepTwoReply(
    val offlineCardKey: ByteArray,
    val terminalMasterKeyKcv: ByteArray,
    val terminalMasterKeyBlock: ByteArray,
    val pinKeyIndicator: Int,
    val pinKeyKcv: ByteArray,
    val pinKeyBlock: ByteArray
)

/** BKM F63 contract for RSA remote loading with TR-31 host-supplied key blocks. */
object RsaRemoteLoadingFields {
    fun firstRequest(terminalChallenge: ByteArray): ByteArray {
        require(terminalChallenge.size == 8)
        return BkmTlv.encode(listOf(
            BkmTag(0x01, terminalChallenge.copyOf()),
            BkmTag(0x20, byteArrayOf(0, 0)) // RSA method; no RKL-loaded iTMK.
        ))
    }

    fun firstReply(field63: ByteArray): RsaStepOneReply {
        val tags = uniqueTags(field63)
        val challenge = required(tags, 0x02, 8)
        val wrapped = required(tags, 0x03, 24)
        return RsaStepOneReply(challenge, wrapped.copyOfRange(0, 16), wrapped.copyOfRange(16, 24))
    }

    fun secondRequest(encryptedHostChallenge: ByteArray, ersaBlock: ByteArray): ByteArray {
        require(encryptedHostChallenge.size == 8)
        require(ersaBlock.size >= 128 && ersaBlock.size % 8 == 0)
        return BkmTlv.encode(listOf(
            BkmTag(0x04, encryptedHostChallenge.copyOf()),
            BkmTag(0x1A, ersaBlock.copyOf()),
            BkmTag(0x33, byteArrayOf(0x01, 0x42)) // TR-31 block version B.
        ))
    }

    fun secondReply(field63: ByteArray): RsaStepTwoReply {
        val tags = uniqueTags(field63)
        val offlineKey = tags[0x17]?.value?.copyOf() ?: error("Missing F63 tag 17")
        require(offlineKey.isNotEmpty())
        val master = tags[0x37]?.value ?: error("Missing F63 tag 37")
        val pin = tags[0x38]?.value ?: error("Missing F63 tag 38")
        require(master.size > 3 && pin.size > 4) { "Truncated TR-31 key block response" }
        return RsaStepTwoReply(
            offlineKey,
            master.copyOfRange(0, 3), master.copyOfRange(3, master.size),
            pin[0].toInt() and 0xFF,
            pin.copyOfRange(1, 4), pin.copyOfRange(4, pin.size)
        )
    }

    private fun uniqueTags(data: ByteArray): Map<Int, BkmTag> = BkmTlv.decode(data).also { tags ->
        require(tags.map(BkmTag::id).distinct().size == tags.size) { "Duplicate F63 key exchange tag" }
    }.associateBy(BkmTag::id)

    private fun required(tags: Map<Int, BkmTag>, id: Int, length: Int): ByteArray =
        (tags[id]?.value ?: error("Missing F63 tag $id")).also {
            require(it.size == length) { "Invalid F63 tag $id length" }
        }.copyOf()
}
