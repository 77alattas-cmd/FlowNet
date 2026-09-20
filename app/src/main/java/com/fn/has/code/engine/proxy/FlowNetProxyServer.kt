package com.fn.has.code.engine.proxy

import com.fn.has.code.core.constants.AppConstants
import com.fn.has.code.data.local.db.entity.CaptiveLogEntity
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class FlowNetProxyServer(
    private val port: Int = AppConstants.PORT_PROXY,
    private val onCaptivePortalIntercepted: (CaptiveLogEntity) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val proxyScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        if (isRunning) return
        isRunning = true

        proxyScope.launch {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning && serverSocket?.isClosed == false) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch { handleClient(clientSocket) }
                }
            } catch (e: Exception) {
                if (isRunning) e.printStackTrace()
            }
        }
    }

    private suspend fun handleClient(clientSocket: Socket) = withContext(Dispatchers.IO) {
        try {
            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()

            val requestLine = readLine(clientIn) ?: return@withContext
            if (requestLine.isBlank()) return@withContext

            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val targetHostPort = parts[1]

            if (method.equals("CONNECT", ignoreCase = true)) {
                // معالجة طلبات HTTPS المشفرة عبر نفق التوصيل المزدوج
                handleHttpsConnect(clientSocket, clientIn, clientOut, targetHostPort)
            } else if (isCaptivePortalRequest(requestLine)) {
                val clientIp = clientSocket.inetAddress?.hostAddress ?: "127.0.0.1"
                val log = CaptiveLogEntity(
                    targetHost = targetHostPort,
                    redirectUrl = targetHostPort,
                    clientIp = clientIp
                )
                onCaptivePortalIntercepted(log)
                sendHttp204Response(clientOut)
            } else {
                handleStandardHttpProxy(clientSocket, clientIn, clientOut, requestLine)
            }
        } catch (e: Exception) {
            // Connection error handling
        } finally {
            try { clientSocket.close() } catch (e: Exception) {}
        }
    }

    private fun handleHttpsConnect(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        targetHostPort: String
    ) {
        val hostParts = targetHostPort.split(":")
        val host = hostParts[0]
        val port = if (hostParts.size > 1) hostParts[1].toIntOrNull() ?: 443 else 443

        try {
            val remoteSocket = Socket(host, port)
            val remoteIn = remoteSocket.getInputStream()
            val remoteOut = remoteSocket.getOutputStream()

            // قراءة بقية الهيدرات والتخلص منها
            var line: String?
            while (true) {
                line = readLine(clientIn)
                if (line == null || line.isEmpty()) break
            }

            // إرسال تأكيد إنشاء النفق 200 Connection Established
            val response = "HTTP/1.1 200 Connection Established\r\nProxy-Agent: FlowNetProxy\r\n\r\n"
            clientOut.write(response.toByteArray(Charsets.UTF_8))
            clientOut.flush()

            // إطلاق مسارين لنقل البيانات الثنائية بالاتجاهين
            val t1 = thread(start = true, name = "ProxyTunnelIn") {
                pipeStream(clientIn, remoteOut)
            }
            val t2 = thread(start = true, name = "ProxyTunnelOut") {
                pipeStream(remoteIn, clientOut)
            }

            t1.join()
            t2.join()
            remoteSocket.close()
        } catch (e: Exception) {
            try {
                clientOut.write("HTTP/1.1 502 Bad Gateway\r\n\r\n".toByteArray())
                clientOut.flush()
            } catch (ex: Exception) {}
        }
    }

    private fun handleStandardHttpProxy(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        firstLine: String
    ) {
        try {
            val parts = firstLine.split(" ")
            val urlString = parts[1]
            val host = extractHostFromUrl(urlString)
            val remoteSocket = Socket(host, 80)
            val remoteOut = remoteSocket.getOutputStream()
            val remoteIn = remoteSocket.getInputStream()

            remoteOut.write("$firstLine\r\n".toByteArray())
            
            val t1 = thread { pipeStream(clientIn, remoteOut) }
            val t2 = thread { pipeStream(remoteIn, clientOut) }

            t1.join()
            t2.join()
            remoteSocket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pipeStream(input: InputStream, output: OutputStream) {
        try {
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                output.flush()
            }
        } catch (e: Exception) {
            // End of stream or connection reset
        }
    }

    private fun readLine(inputStream: InputStream): String? {
        val baos = java.io.ByteArrayOutputStream()
        var c: Int
        while (inputStream.read().also { c = it } != -1) {
            if (c == '\r'.code) {
                val next = inputStream.read()
                if (next == '\n'.code || next == -1) break
                baos.write(c)
                baos.write(next)
            } else if (c == '\n'.code) {
                break
            } else {
                baos.write(c)
            }
        }
        if (baos.size() == 0 && c == -1) return null
        return baos.toString("UTF-8")
    }

    private fun isCaptivePortalRequest(header: String): Boolean {
        return header.contains("connectivitycheck.gstatic.com") ||
               header.contains("generate_204") ||
               header.contains("msftncsi.com") ||
               header.contains("captive.apple.com")
    }

    private fun sendHttp204Response(output: OutputStream) {
        val response = "HTTP/1.1 204 No Content\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
        output.write(response.toByteArray())
        output.flush()
    }

    private fun extractHostFromUrl(url: String): String {
        return if (url.startsWith("http://")) {
            url.substring(7).substringBefore("/").substringBefore(":")
        } else {
            url.substringBefore("/").substringBefore(":")
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
        proxyScope.cancel()
    }
}
