package com.phosfe.bkmtechpos.protocol

import java.io.ByteArrayOutputStream

data class TerminalRoute(
    val hostId: Int = 1,
    val vendorId: Int,
    val serialNumber: String
) {
    init {
        require(hostId in 0..255)
        require(vendorId in 0..255)
        require(serialNumber.toByteArray(Charsets.US_ASCII).size <= 12)
    }
}

data class DecodedBkmFrame(val route: TerminalRoute, val encrypted: Boolean, val message: IsoMessage)

class BkmFrameCodec(private val isoCodec: IsoCodec = IsoCodec()) {
    fun encode(
        route: TerminalRoute,
        message: IsoMessage,
        cipher: PayloadCipher? = null
    ): ByteArray {
        val processingCode = message.fields[3]?.toString(Charsets.US_ASCII)
            ?: error("F3 processing code is mandatory in BKM routing header")
        require(processingCode.length == 6 && processingCode.all(Char::isDigit))

        val iso = isoCodec.encode(message)
        val protectedBody = BkmIntegrity.appendCrc32(iso)
        val transmittedBody = cipher?.encrypt(protectedBody) ?: protectedBody
        val output = ByteArrayOutputStream()
        output.write(route.hostId)
        output.write(SUB_HEADER_LENGTH)
        output.write(if (cipher == null) 0 else 1)
        output.write(route.vendorId)
        output.write(route.serialNumber.padEnd(12, ' ').toByteArray(Charsets.US_ASCII))
        output.write("    ".toByteArray(Charsets.US_ASCII))
        output.write(PackedDecimal.encode(message.messageType))
        output.write(PackedDecimal.encode(processingCode))
        output.write(transmittedBody)
        val payload = output.toByteArray()
        require(payload.size <= 0xFFFF)
        return byteArrayOf((payload.size ushr 8).toByte(), payload.size.toByte()) + payload
    }

    fun decode(frame: ByteArray, cipher: PayloadCipher? = null): DecodedBkmFrame {
        require(frame.size >= 2 + 2 + SUB_HEADER_LENGTH) { "BKM frame is too short" }
        val declaredLength = ((frame[0].toInt() and 0xFF) shl 8) or (frame[1].toInt() and 0xFF)
        require(declaredLength == frame.size - 2) { "BKM frame length mismatch" }
        var offset = 2
        val hostId = frame[offset++].toInt() and 0xFF
        require((frame[offset++].toInt() and 0xFF) == SUB_HEADER_LENGTH) { "Unsupported sub-header" }
        val encrypted = when (frame[offset++].toInt() and 0xFF) {
            0 -> false
            1 -> true
            else -> error("Invalid encryption indicator")
        }
        val vendorId = frame[offset++].toInt() and 0xFF
        val serial = frame.copyOfRange(offset, offset + 12).toString(Charsets.US_ASCII).trimEnd()
        offset += 12
        require(frame.copyOfRange(offset, offset + 4).all { it == 0x20.toByte() }) { "RFU must contain spaces" }
        offset += 4
        val routingMti = PackedDecimal.decode(frame.copyOfRange(offset, offset + 2), 4)
        offset += 2
        val routingProcessingCode = PackedDecimal.decode(frame.copyOfRange(offset, offset + 3), 6)
        offset += 3
        val bodyOnWire = frame.copyOfRange(offset, frame.size)
        val clearBody = if (encrypted) {
            requireNotNull(cipher) { "Encrypted frame requires a session cipher" }.decrypt(bodyOnWire)
        } else bodyOnWire
        val message = isoCodec.decode(BkmIntegrity.verifyAndStrip(clearBody))
        require(message.messageType == routingMti) { "Routing MTI does not match body" }
        require(message.text(3) == routingProcessingCode) { "Routing processing code does not match F3" }
        return DecodedBkmFrame(TerminalRoute(hostId, vendorId, serial), encrypted, message)
    }

    private companion object { const val SUB_HEADER_LENGTH = 23 }
}

