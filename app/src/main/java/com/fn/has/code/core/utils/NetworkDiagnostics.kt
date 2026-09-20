package com.fn.has.code.core.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket

object NetworkDiagnostics {

    suspend fun pingHost(host: String, timeoutMs: Int = 2000): PingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        return@withContext try {
            val address = InetAddress.getByName(host)
            val reachable = address.isReachable(timeoutMs)
            val timeTaken = System.currentTimeMillis() - startTime
            if (reachable) {
                PingResult.Success(host, address.hostAddress ?: "", timeTaken)
            } else {
                PingResult.Timeout(host)
            }
        } catch (e: Exception) {
            PingResult.Error(host, e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun checkPortOpen(host: String, port: Int, timeoutMs: Int = 1000): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}

sealed class PingResult {
    data class Success(val host: String, val ip: String, val timeMs: Long) : PingResult()
    data class Timeout(val host: String) : PingResult()
    data class Error(val host: String, val message: String) : PingResult()
}
