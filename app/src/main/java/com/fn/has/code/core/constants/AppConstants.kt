package com.fn.has.code.core.constants

import androidx.compose.ui.graphics.Color

object AppConstants {
    const val PACKAGE_NAME = "com.fn.has.code"
    const val APP_NAME = "FlowNet"

    const val PORT_PROXY = 8080
    const val PORT_FILE_SERVER = 8888
    const val PORT_COMM = 9090
    const val PORT_DNS = 5353

    const val DEFAULT_GATEWAY_IP = "192.168.49.1"
    const val DEFAULT_SUBNET_MASK = "255.255.255.0"
    const val TARGET_TTL_VALUE = 64
    const val HOTSPOT_PREFIX = "Direct-FL-"

    const val NOTIFICATION_CHANNEL_ID = "flownet_core_channel"
    const val NOTIFICATION_CHANNEL_NAME = "FlowNet Foreground Service"
    const val NOTIFICATION_ID_CORE = 1001

    val COLOR_BACKGROUND = Color(0xFF0B0F17)
    val COLOR_SURFACE = Color(0xFF1E293B)
    val COLOR_ACCENT_CYAN = Color(0xFF00E5FF)
    val COLOR_SUCCESS_GREEN = Color(0xFF10B981)
    val COLOR_UP_BLUE = Color(0xFF3B82F6)
    val COLOR_DANGER_RED = Color(0xFFEF4444)
    val COLOR_TEXT_PRIMARY = Color(0xFFF8FAFC)
    val COLOR_TEXT_SECONDARY = Color(0xFF94A3B8)
}
