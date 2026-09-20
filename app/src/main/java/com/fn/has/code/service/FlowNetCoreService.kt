package com.fn.has.code.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fn.has.code.core.constants.AppConstants
import com.fn.has.code.core.utils.PowerAndHotspotManager
import com.fn.has.code.core.utils.ServiceStateStore
import com.fn.has.code.data.local.db.AppDatabase
import com.fn.has.code.engine.dns.DnsEngine
import com.fn.has.code.engine.proxy.FlowNetProxyServer
import com.fn.has.code.server.HttpFileServer
import kotlinx.coroutines.*

class FlowNetCoreService : Service() {

    private var fileServer: HttpFileServer? = null
    private var proxyServer: FlowNetProxyServer? = null
    private var dnsEngine: DnsEngine? = null
    private var powerManager: PowerAndHotspotManager? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        powerManager = PowerAndHotspotManager(this).apply {
            acquireWakeLock()
        }

        val db = AppDatabase.getDatabase(this)

        // إطلاق خادم البروكسي
        proxyServer = FlowNetProxyServer(AppConstants.PORT_PROXY) { captiveLog ->
            serviceScope.launch {
                db.flowNetDao().insertCaptiveLog(captiveLog)
            }
        }.apply { start() }

        // إطلاق خادم الملفات المحلي
        fileServer = HttpFileServer(this, AppConstants.PORT_FILE_SERVER).apply { start() }

        // إطلاق محرك DNS المحلي
        dnsEngine = DnsEngine(this, AppConstants.PORT_DNS).apply { start() }

        // الاستماع للتغيرات اللحظية القادمة من الواجهة
        serviceScope.launch {
            ServiceStateStore.configState.collect { state ->
                fileServer?.apply {
                    sharedFolderUri = state.sharedFolderUri
                    isAuthEnabled = state.isAuthEnabled
                    username = state.username
                    password = state.password
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        fileServer?.stop()
        proxyServer?.stop()
        dnsEngine?.stop()
        powerManager?.releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FlowNet System Core Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(AppConstants.APP_NAME)
            .setContentText("خدمات الشبكة والرقابة والملفات تعمل في الخلفية بنجاح")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "flownet_core_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
