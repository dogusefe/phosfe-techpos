package com.phosfe.bkmtechpos.host

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.net.ServerSocket
import kotlin.concurrent.thread

class TcpHostTransportTest {
    @Test fun readsFragmentedLengthPrefixedResponseCompletely() {
        ServerSocket(0).use { server ->
            val request = byteArrayOf(0, 2, 1, 2)
            val response = byteArrayOf(0, 5, 9, 8, 7, 6, 5)
            val worker = thread {
                server.accept().use { socket ->
                    assertArrayEquals(request, socket.getInputStream().readNBytes(request.size))
                    socket.getOutputStream().apply {
                        write(response, 0, 3)
                        flush()
                        write(response, 3, response.size - 3)
                        flush()
                    }
                }
            }
            val actual = TcpHostTransport("127.0.0.1", server.localPort, 2_000, 2_000)
                .exchange(request)
            worker.join(2_000)
            assertArrayEquals(response, actual)
        }
    }
}

