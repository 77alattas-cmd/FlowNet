package com.fn.has.code.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fn.has.code.data.local.db.FlowNetDao
import com.fn.has.code.data.local.db.entity.CaptiveLogEntity
import com.fn.has.code.data.local.db.entity.NetworkLogEntity
import com.fn.has.code.server.LocalProxyServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isServiceRunning: Boolean = false,
    val isProxyRunning: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dao: FlowNetDao,
    private val proxyServer: LocalProxyServer
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val captiveLogs: StateFlow<List<CaptiveLogEntity>> = dao.observeCaptiveLogs().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val networkLogs: StateFlow<List<NetworkLogEntity>> = dao.getAllNetworkLogs().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    fun toggleService() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        try {
            if (_uiState.value.isServiceRunning) {
                // stop logic
                _uiState.update { it.copy(isServiceRunning = false, isLoading = false) }
            } else {
                // start logic
                _uiState.update { it.copy(isServiceRunning = true, isLoading = false) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
        }
    }

    fun toggleProxy(port: Int = 8080) = viewModelScope.launch {
        if (proxyServer.isRunning) {
            proxyServer.stop()
            _uiState.update { it.copy(isProxyRunning = false) }
        } else {
            proxyServer.start(port)
            _uiState.update { it.copy(isProxyRunning = true) }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun addNetworkLog(title: String, description: String, type: String = "INFO") {
        viewModelScope.launch {
            dao.insertNetworkLog(
                NetworkLogEntity(
                    title = title,
                    description = description,
                    type = type
                )
            )
        }
    }

    fun clearCaptiveLogs() {
        viewModelScope.launch {
            dao.clearCaptiveLogs()
        }
    }

    fun clearNetworkLogs() {
        viewModelScope.launch {
            dao.clearNetworkLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        proxyServer.stop()
    }
}
