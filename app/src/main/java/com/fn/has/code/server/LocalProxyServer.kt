package com.fn.has.code.server

import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class LocalProxyServer(private val port: Int) {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true

        thread(start = true, name = "LocalProxyThread") {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleProxyConnection(clientSocket)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleProxyConnection(clientSocket: Socket) {
        thread {
            try {
                val clientIn = clientSocket.getInputStream()
                val clientOut = clientSocket.getOutputStream()

                // Tunneling basics (Pass-through traffic logic)
                val buffer = ByteArray(8192)
                var bytesRead = clientIn.read(buffer)

                while (isRunning && bytesRead != -1) {
                    clientOut.write(buffer, 0, bytesRead)
                    clientOut.flush()
                    bytesRead = clientIn.read(buffer)
                }
            } catch (e: Exception) {
                // Connection closed or interrupted
            } finally {
                clientSocket.close()
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
