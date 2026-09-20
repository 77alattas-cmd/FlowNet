package com.fn.has.code.core.utils

import android.net.Uri
import com.fn.has.code.core.constants.AppConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ServerConfigState(
    val sharedFolderUri: Uri? = null,
    val isAuthEnabled: Boolean = false,
    val username: String = "admin",
    val password: String = "1234",
    val isProxyRunning: Boolean = true,
    val isFileServerRunning: Boolean = true,
    val isDnsEngineRunning: Boolean = true,
    val proxyPort: Int = AppConstants.PORT_PROXY,
    val fileServerPort: Int = AppConstants.PORT_FILE_SERVER,
    val dnsPort: Int = AppConstants.PORT_DNS,
    val hotspotSsidSuffix: String = "FlowNet",
    val hotspotPassword: String = "12345678"
)

object ServiceStateStore {

    private val _configState = MutableStateFlow(ServerConfigState())
    val configState: StateFlow<ServerConfigState> = _configState.asStateFlow()

    fun updateFolderUri(uri: Uri?) {
        _configState.value = _configState.value.copy(sharedFolderUri = uri)
    }

    fun updateAuthConfig(isAuth: Boolean, user: String, pass: String) {
        _configState.value = _configState.value.copy(
            isAuthEnabled = isAuth,
            username = user,
            password = pass
        )
    }

    fun updateServersRunning(proxy: Boolean, fileServer: Boolean, dns: Boolean) {
        _configState.value = _configState.value.copy(
            isProxyRunning = proxy,
            isFileServerRunning = fileServer,
            isDnsEngineRunning = dns
        )
    }

    fun updatePorts(proxy: Int, fileServer: Int, dns: Int) {
        _configState.value = _configState.value.copy(
            proxyPort = proxy,
            fileServerPort = fileServer,
            dnsPort = dns
        )
    }

    fun updateHotspotConfig(suffix: String, pass: String) {
        _configState.value = _configState.value.copy(
            hotspotSsidSuffix = suffix,
            hotspotPassword = pass
        )
    }
}
