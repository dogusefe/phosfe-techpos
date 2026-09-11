package com.phosfe.bkmtechpos.terminal

import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class TerminalProfile(
    val serialNumber: String,
    val producerCode: String,
    val deviceType: String,
    val applicationVersion: String,
    val operatingSystemVersion: String,
    val defaultAcquirerId: Int
) {
    init {
        require(serialNumber.toByteArray(Charsets.US_ASCII).size in 10..12)
        require(producerCode.length == 3 && producerCode.isAscii())
        require(deviceType.length == 3 && deviceType.isAscii())
        require(applicationVersion.length == 4 && applicationVersion.isAscii())
        require(operatingSystemVersion.length == 4 && operatingSystemVersion.isAscii())
        require(defaultAcquirerId in 0..9999)
    }

    fun field43(): ByteArray = buildString(40) {
        append(serialNumber.padEnd(12, ' '))
        append("    ")
        append(producerCode)
        append(deviceType)
        append(applicationVersion)
        append(operatingSystemVersion)
        append(defaultAcquirerId.toString().padStart(4, '0'))
        append("      ")
    }.also { require(it.length == 40) }.toByteArray(Charsets.US_ASCII)
}

private fun String.isAscii(): Boolean = all { it.code in 0x20..0x7E }

interface StanSource { fun next(): String }

class RotatingStan(initialValue: Int = 0) : StanSource {
    private var current = initialValue.also { require(it in 0..999_999) }

    @Synchronized
    override fun next(): String {
        current = if (current >= 999_999) 1 else current + 1
        return current.toString().padStart(6, '0')
    }
}

data class MessageMoment(val time: String, val date: String)

class TerminalClock(private val clock: Clock = Clock.systemDefaultZone()) {
    fun now(): MessageMoment = LocalDateTime.now(clock).let {
        MessageMoment(it.format(TIME), it.format(DATE))
    }

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss")
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMdd")
    }
}

data class ParameterVersion(val tableType: Int, val version: Long) {
    init {
        require(tableType in 0..0xFFFF)
        require(version in 0..0xFFFF_FFFFL)
    }
}

object ParameterVersionCodec {
    fun encode(versions: List<ParameterVersion>): ByteArray {
        require(versions.size <= 255)
        val output = ByteArray(1 + versions.size * 6)
        output[0] = versions.size.toByte()
        versions.forEachIndexed { index, item ->
            val offset = 1 + index * 6
            output[offset] = (item.tableType ushr 8).toByte()
            output[offset + 1] = item.tableType.toByte()
            output[offset + 2] = (item.version ushr 24).toByte()
            output[offset + 3] = (item.version ushr 16).toByte()
            output[offset + 4] = (item.version ushr 8).toByte()
            output[offset + 5] = item.version.toByte()
        }
        return output
    }
}
