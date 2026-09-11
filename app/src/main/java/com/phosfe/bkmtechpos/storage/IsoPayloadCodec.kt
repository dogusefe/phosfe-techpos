package com.phosfe.bkmtechpos.storage

import com.phosfe.bkmtechpos.protocol.IsoMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object IsoPayloadCodec {
    fun encode(message: IsoMessage): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(FORMAT_VERSION)
            output.writeUTF(message.messageType)
            output.writeInt(message.fields.size)
            message.fields.toSortedMap().forEach { (number, value) ->
                output.writeInt(number)
                output.writeInt(value.size)
                output.write(value)
            }
        }
    }.toByteArray()

    fun decode(encoded: ByteArray): IsoMessage = DataInputStream(ByteArrayInputStream(encoded)).use { input ->
        require(input.readInt() == FORMAT_VERSION) { "Unsupported ISO payload format" }
        val messageType = input.readUTF()
        val count = input.readInt().also { require(it in 0..63) }
        val fields = linkedMapOf<Int, ByteArray>()
        repeat(count) {
            val number = input.readInt().also { require(it in 2..64) }
            val length = input.readInt().also { require(it in 0..65_535) }
            require(number !in fields) { "Duplicate F$number in ISO payload" }
            fields[number] = ByteArray(length).also(input::readFully)
        }
        require(input.read() == -1) { "Trailing ISO payload data" }
        IsoMessage(messageType, fields)
    }

    private const val FORMAT_VERSION = 1
}
