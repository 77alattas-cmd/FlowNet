package com.fn.has.code.server

import com.fn.has.code.core.security.SecureCredentialsStore
import kotlinx.coroutines.*
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalProxyServer @Inject constructor(
    private val credentials: SecureCredentialsStore
) {
    private var serverJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var isRunning: Boolean = false
        private set

    fun start(port: Int = 8080) {
        if (isRunning) return
        isRunning = true

        serverJob = scope.launch {
            try {
                ServerSocket(port).use { server ->
                    while (isActive) {
                        val client = server.accept()
                        launch { handleClient(client) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        serverJob?.cancel()
        serverJob = null
    }

    private suspend fun handleClient(client: Socket) = withContext(Dispatchers.IO) {
        client.use { sock ->
            val input = sock.getInputStream().bufferedReader()
            val requestLine = input.readLine() ?: return@withContext
            // … منطق البروكسي (Basic Auth + تمرير الطلب)
        }
    }
}
