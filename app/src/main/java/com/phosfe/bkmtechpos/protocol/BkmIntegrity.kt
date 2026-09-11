package com.phosfe.bkmtechpos.protocol

object BkmIntegrity {
    fun appendCrc32(input: ByteArray): ByteArray = input + crc32(input)

    fun verifyAndStrip(input: ByteArray): ByteArray {
        require(input.size >= 4) { "CRC protected data is too short" }
        val data = input.copyOfRange(0, input.size - 4)
        val received = input.copyOfRange(input.size - 4, input.size)
        require(received.contentEquals(crc32(data))) { "CRC mismatch" }
        return data
    }

    /** IEEE CRC-32, init/xor-out FFFFFFFF, reflected polynomial EDB88320. */
    fun crc32(input: ByteArray): ByteArray {
        var crc = 0xFFFF_FFFFL
        input.forEach { source ->
            repeat(8) { bit ->
                val carry = (crc and 1L).toInt()
                crc = crc ushr 1
                if (carry != ((source.toInt() shr bit) and 1)) crc = crc xor 0xEDB8_8320L
            }
        }
        crc = crc.inv() and 0xFFFF_FFFFL
        return byteArrayOf(
            (crc shr 24).toByte(),
            (crc shr 16).toByte(),
            (crc shr 8).toByte(),
            crc.toByte()
        )
    }
}

