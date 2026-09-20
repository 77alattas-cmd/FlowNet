package com.fn.has.code.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.fn.has.code.core.constants.AppConstants
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

class FlowNetVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN) {
            stopVpn()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return
        isRunning = true

        try {
            val builder = Builder()
                .setSession(AppConstants.APP_NAME)
                .addAddress("10.1.10.1", 24)
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()

            thread(start = true, name = "FlowNetVpnThread") {
                val input = FileInputStream(vpnInterface?.fileDescriptor)
                val output = FileOutputStream(vpnInterface?.fileDescriptor)
                val buffer = ByteArray(32767)

                while (isRunning) {
                    val length = input.read(buffer)
                    if (length > 0) {
                        // توجيه وإعادة كتابة الحزم هنا
                        output.write(buffer, 0, length)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun stopVpn() {
        isRunning = false
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP_VPN = "com.fn.has.code.STOP_VPN"
    }
}
