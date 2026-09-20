package com.fn.has.code.core.utils

import android.content.Context
import android.content.SharedPreferences
import com.fn.has.code.core.constants.AppConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkSettingsState(
    val isNetworkActive: Boolean = false,
    val ssidSuffix: String = "FlowNet",
    val wifiPassword: String = "12345678",
    val isPasswordVisible: Boolean = false,
    val isQrCodeVisible: Boolean = false,
    val proxyPort: Int = AppConstants.PORT_PROXY,
    val fileServerPort: Int = AppConstants.PORT_FILE_SERVER,
    val dnsPort: Int = AppConstants.PORT_DNS
) {
    val fullSsid: String
        get() = "${AppConstants.HOTSPOT_PREFIX}$ssidSuffix"
}

class NetworkSettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("flownet_network_config", Context.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(loadSettings())
    val settingsState: StateFlow<NetworkSettingsState> = _settingsState.asStateFlow()

    private fun loadSettings(): NetworkSettingsState {
        return NetworkSettingsState(
            isNetworkActive = prefs.getBoolean("is_network_active", false),
            ssidSuffix = prefs.getString("ssid_suffix", "FlowNet") ?: "FlowNet",
            wifiPassword = prefs.getString("wifi_password", "12345678") ?: "12345678",
            isPasswordVisible = prefs.getBoolean("is_password_visible", false),
            isQrCodeVisible = prefs.getBoolean("is_qr_visible", false),
            proxyPort = prefs.getInt("proxy_port", AppConstants.PORT_PROXY),
            fileServerPort = prefs.getInt("file_server_port", AppConstants.PORT_FILE_SERVER),
            dnsPort = prefs.getInt("dns_port", AppConstants.PORT_DNS)
        )
    }

    fun toggleNetwork(active: Boolean) {
        prefs.edit().putBoolean("is_network_active", active).apply()
        _settingsState.value = _settingsState.value.copy(isNetworkActive = active)
    }

    fun updateSsidAndPassword(suffix: String, pass: String) {
        prefs.edit()
            .putString("ssid_suffix", suffix.trim())
            .putString("wifi_password", pass.trim())
            .apply()
        _settingsState.value = _settingsState.value.copy(
            ssidSuffix = suffix.trim(),
            wifiPassword = pass.trim()
        )
    }

    fun togglePasswordVisibility() {
        val nextState = !_settingsState.value.isPasswordVisible
        prefs.edit().putBoolean("is_password_visible", nextState).apply()
        _settingsState.value = _settingsState.value.copy(isPasswordVisible = nextState)
    }

    fun toggleQrVisibility() {
        val nextState = !_settingsState.value.isQrCodeVisible
        prefs.edit().putBoolean("is_qr_visible", nextState).apply()
        _settingsState.value = _settingsState.value.copy(isQrCodeVisible = nextState)
    }

    fun updatePorts(proxy: Int, fileServer: Int, dns: Int) {
        prefs.edit()
            .putInt("proxy_port", proxy)
            .putInt("file_server_port", fileServer)
            .putInt("dns_port", dns)
            .apply()
        _settingsState.value = _settingsState.value.copy(
            proxyPort = proxy,
            fileServerPort = fileServer,
            dnsPort = dns
        )
    }
}
