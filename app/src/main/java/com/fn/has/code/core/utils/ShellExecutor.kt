package com.fn.has.code.core.utils

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object ShellExecutor {

    enum class ExecutionMode {
        ROOT,
        SHIZUKU,
        NORMAL
    }

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()
            process.waitFor()
            output != null && output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val pingMethod = shizukuClass.getMethod("pingBinder")
            val checkPermMethod = shizukuClass.getMethod("checkSelfPermission")
            val isPinged = pingMethod.invoke(null) as? Boolean == true
            val perm = checkPermMethod.invoke(null) as? Int == PackageManager.PERMISSION_GRANTED
            isPinged && perm
        } catch (e: Exception) {
            false
        }
    }

    suspend fun executeCommand(command: String, mode: ExecutionMode): CommandResult = withContext(Dispatchers.IO) {
        return@withContext when (mode) {
            ExecutionMode.ROOT -> runAsRoot(command)
            ExecutionMode.SHIZUKU -> runAsShizuku(command)
            ExecutionMode.NORMAL -> runAsNormal(command)
        }
    }

    private fun runAsRoot(command: String): CommandResult {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\nexit\n")
            os.flush()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = reader.readText()
            val error = errorReader.readText()
            val exitCode = process.waitFor()

            CommandResult(exitCode == 0, output.ifBlank { error })
        } catch (e: Exception) {
            CommandResult(false, e.localizedMessage ?: "Root Execution Failed")
        }
    }

    private fun runAsShizuku(command: String): CommandResult {
        return try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcessMethod = shizukuClass.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = reader.readText()
            val error = errorReader.readText()
            val exitCode = process.waitFor()

            CommandResult(exitCode == 0, output.ifBlank { error })
        } catch (e: Exception) {
            CommandResult(false, e.localizedMessage ?: "Shizuku Execution Failed")
        }
    }

    private fun runAsNormal(command: String): CommandResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            val exitCode = process.waitFor()

            CommandResult(exitCode == 0, output)
        } catch (e: Exception) {
            CommandResult(false, e.localizedMessage ?: "Normal Shell Execution Failed")
        }
    }
}

data class CommandResult(
    val isSuccess: Boolean,
    val output: String
)
