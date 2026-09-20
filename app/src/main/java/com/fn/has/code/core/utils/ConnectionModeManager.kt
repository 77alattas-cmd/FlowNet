package com.fn.has.code.core.utils

import android.content.Context
import com.fn.has.code.core.constants.AppConstants

enum class ConnectionType(val title: String, val description: String) {
    PROXY("البروكسي التقليدي", "إعداد يدوي لمنفذ 8080 في الأجهزة المتصلة بدون صلاحيات خاصة."),
    DIRECT_VPN("VPN المباشر (توجيه آلي)", "توجيه البيانات عبر واجهة TUN بدون تعديل البروكسي يدوياً."),
    SHIZUKU("نمط ADB / Shizuku", "تطبيق البروكسي وتجاوز قيود TTL أوتوماتيكياً عبر صلاحيات ADB بدون روت."),
    ROOT("نمط الروت الكامل (Root SU)", "توجيه شفاف كلياً وتجاوز قيود شبكات التغطية عبر iptables بحرية مطلقة.")
}

class ConnectionModeManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("flownet_connection_prefs", Context.MODE_PRIVATE)

    var currentMode: ConnectionType
        get() {
            val raw = prefs.getString("connection_type", ConnectionType.PROXY.name)
            return try {
                ConnectionType.valueOf(raw ?: ConnectionType.PROXY.name)
            } catch (e: Exception) {
                ConnectionType.PROXY
            }
        }
        set(value) {
            prefs.edit().putString("connection_type", value.name).apply()
        }

    suspend fun applyConnectionSettings(): CommandResult {
        val proxyPort = NumberFormatter.formatPort(AppConstants.PORT_PROXY)
        return when (currentMode) {
            ConnectionType.PROXY -> {
                CommandResult(true, "تم اختيار نمط البروكسي التقليدي. يرجى ضبط المنفذ $proxyPort في الأجهزة المجاورة.")
            }
            ConnectionType.DIRECT_VPN -> {
                CommandResult(true, "تم تفعيل محرك VPN المحلي. يتم توجيه الحزم آلياً.")
            }
            ConnectionType.SHIZUKU -> {
                if (!ShellExecutor.isShizukuAvailable()) {
                    return CommandResult(false, "خدمة Shizuku غير مجهزة أو لم يتم منح الإذن.")
                }
                // ضبط بروكسي النظام عبر ADB ورفع قيمة TTL إلى 64
                val cmd1 = "settings put global http_proxy 127.0.0.1:$proxyPort"
                val cmd2 = "iptables -t mangle -A POSTROUTING -j TTL --ttl-set ${AppConstants.TARGET_TTL_VALUE}"
                ShellExecutor.executeCommand("$cmd1 && $cmd2", ShellExecutor.ExecutionMode.SHIZUKU)
            }
            ConnectionType.ROOT -> {
                if (!ShellExecutor.isRootAvailable()) {
                    return CommandResult(false, "صلاحية الروت غير متوفرة على هذا الجهاز.")
                }
                // تطبيق التوجيه الشفاف وتثبيت TTL في جداول iptables
                val cmdTtl = "iptables -t mangle -A POSTROUTING -j TTL --ttl-set ${AppConstants.TARGET_TTL_VALUE}"
                val cmdRedirect = "iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports $proxyPort"
                ShellExecutor.executeCommand("$cmdTtl && $cmdRedirect", ShellExecutor.ExecutionMode.ROOT)
            }
        }
    }

    suspend fun clearConnectionSettings(): CommandResult {
        return when (currentMode) {
            ConnectionType.SHIZUKU -> {
                val cmd = "settings delete global http_proxy && settings delete global global_http_proxy_host"
                ShellExecutor.executeCommand(cmd, ShellExecutor.ExecutionMode.SHIZUKU)
            }
            ConnectionType.ROOT -> {
                val cmd = "iptables -t nat -F && iptables -t mangle -F"
                ShellExecutor.executeCommand(cmd, ShellExecutor.ExecutionMode.ROOT)
            }
            else -> CommandResult(true, "تم الإلغاء.")
        }
    }
}
