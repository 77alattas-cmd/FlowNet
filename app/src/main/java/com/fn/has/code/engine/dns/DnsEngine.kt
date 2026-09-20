package com.fn.has.code.engine.dns

import android.content.Context
import com.fn.has.code.core.constants.AppConstants
import com.fn.has.code.core.utils.ParentalControlManager
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DnsEngine(
    private val context: Context,
    private val port: Int = AppConstants.PORT_DNS
) {
    private var socket: DatagramSocket? = null
    private var isRunning = false
    private val dnsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val parentalManager = ParentalControlManager(context)

    fun start() {
        if (isRunning) return
        isRunning = true

        dnsScope.launch {
            try {
                socket = DatagramSocket(port)
                val buffer = ByteArray(512)

                while (isRunning && socket?.isClosed == false) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    launch {
                        processDnsPacket(packet)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) e.printStackTrace()
            }
        }
    }

    private suspend fun processDnsPacket(requestPacket: DatagramPacket) = withContext(Dispatchers.IO) {
        try {
            val data = requestPacket.data
            val length = requestPacket.length

            if (length < 12) return@withContext

            val queriedDomain = parseDomainFromDnsPacket(data, length)

            if (queriedDomain.isNotBlank() && parentalManager.isDomainBlocked(queriedDomain)) {
                // إرسال استجابة NXDOMAIN (حجب النطاق)
                val nxDomainResponse = createNxDomainResponse(data, length)
                val responsePacket = DatagramPacket(
                    nxDomainResponse,
                    nxDomainResponse.size,
                    requestPacket.address,
                    requestPacket.port
                )
                socket?.send(responsePacket)
            } else {
                // إعادة توجيه الطلب إلى خادم DNS الخارجي (Upstream DNS)
                forwardToUpstream(data, length, requestPacket.address, requestPacket.port)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun forwardToUpstream(
        requestData: ByteArray,
        requestLength: Int,
        clientAddress: InetAddress,
        clientPort: Int
    ) {
        try {
            val rawUpstream = parentalManager.selectedUpstreamDns
            val upstreamIp = if (rawUpstream.equals("none", ignoreCase = true) || rawUpstream.isBlank()) {
                "8.8.8.8"
            } else {
                rawUpstream
            }
            val upstreamAddress = InetAddress.getByName(upstreamIp)
            val upstreamSocket = DatagramSocket()
            upstreamSocket.soTimeout = 2000

            val sendPacket = DatagramPacket(requestData, requestLength, upstreamAddress, 53)
            upstreamSocket.send(sendPacket)

            val receiveBuffer = ByteArray(512)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
            upstreamSocket.receive(receivePacket)

            val clientResponsePacket = DatagramPacket(
                receivePacket.data,
                receivePacket.length,
                clientAddress,
                clientPort
            )
            socket?.send(clientResponsePacket)
            upstreamSocket.close()
        } catch (e: Exception) {
            // انقضاء المهلة أو تعذر الوصول للخادم العلوي
        }
    }

    private fun parseDomainFromDnsPacket(data: ByteArray, length: Int): String {
        return try {
            var pos = 12
            val domainBuilder = StringBuilder()

            while (pos < length) {
                val labelLen = data[pos].toInt() and 0xFF
                if (labelLen == 0) break
                if (domainBuilder.isNotEmpty()) domainBuilder.append(".")
                pos++
                if (pos + labelLen > length) break
                domainBuilder.append(String(data, pos, labelLen, Charsets.US_ASCII))
                pos += labelLen
            }
            domainBuilder.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun createNxDomainResponse(requestData: ByteArray, length: Int): ByteArray {
        val response = requestData.copyOf(length)
        // ضبط Flags لتشغيل علامة Response و NXDOMAIN (RCODE = 3)
        response[2] = (0x81).toByte()
        response[3] = (0x83).toByte()
        return response
    }

    fun stop() {
        isRunning = false
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dnsScope.cancel()
    }
}
