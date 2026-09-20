package com.fn.has.code.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi

class PowerAndHotspotManager(private val context: Context) {

    private var wakeLock: PowerManager.WakeLock? = null
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null

    @SuppressLint("WakelockTimeout")
    fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FlowNet::BackgroundServiceWakeLock"
            )
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
        }
    }

    fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startLocalOnlyHotspot(
        onSuccess: (String, String) -> Unit,
        onFailed: (String) -> Unit
    ) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        try {
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    super.onStarted(reservation)
                    hotspotReservation = reservation
                    val ssid = reservation.wifiConfiguration?.SSID ?: "FlowNetHotspot"
                    val password = reservation.wifiConfiguration?.preSharedKey ?: ""
                    onSuccess(ssid, password)
                }

                override fun onStopped() {
                    super.onStopped()
                    onFailed("تم إيقاف نقطة الاتصال المحلية.")
                }

                override fun onFailed(reason: Int) {
                    super.onFailed(reason)
                    onFailed("فشل تشغيل نقطة الاتصال المحلية (الرمز: $reason)")
                }
            }, null)
        } catch (e: Exception) {
            onFailed(e.localizedMessage ?: "حدث خطأ أثناء تشغيل الهوتسبوت")
        }
    }

    fun stopLocalOnlyHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hotspotReservation?.close()
            hotspotReservation = null
        }
    }
}
