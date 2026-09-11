package com.phosfe.bkmtechpos.host

import java.io.EOFException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

fun interface HostTransport {
    fun exchange(request: ByteArray): ByteArray
}

class TcpHostTransport(
    private val host: String,
    private val port: Int,
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 45_000,
    private val maximumFrameSize: Int = 65_537
) : HostTransport {
    init {
        require(host.isNotBlank())
        require(port in 1..65_535)
        require(connectTimeoutMs > 0 && readTimeoutMs > 0)
        require(maximumFrameSize in 27..65_537)
    }

    override fun exchange(request: ByteArray): ByteArray {
        require(request.size in 4..maximumFrameSize) { "Invalid request frame size" }
        return Socket().use { socket ->
            socket.tcpNoDelay = true
            socket.soTimeout = readTimeoutMs
            socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
            socket.getOutputStream().apply {
                write(request)
                flush()
            }
            val lengthPrefix = socket.getInputStream().readExactly(2)
            val payloadSize = ((lengthPrefix[0].toInt() and 0xFF) shl 8) or (lengthPrefix[1].toInt() and 0xFF)
            require(payloadSize + 2 <= maximumFrameSize) { "Host frame exceeds configured limit" }
            lengthPrefix + socket.getInputStream().readExactly(payloadSize)
        }
    }
}

private fun InputStream.readExactly(length: Int): ByteArray {
    val output = ByteArray(length)
    var offset = 0
    while (offset < length) {
        val count = read(output, offset, length - offset)
        if (count < 0) throw EOFException("Host closed connection after $offset of $length bytes")
        offset += count
    }
    return output
}

