package com.fn.has.code.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fn.has.code.FlowNetApplication
import com.fn.has.code.data.local.db.entity.CaptiveLogEntity
import com.fn.has.code.data.local.db.entity.NetworkLogEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as FlowNetApplication).database.flowNetDao()

    val captiveLogs: StateFlow<List<CaptiveLogEntity>> = dao.getAllCaptiveLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val networkLogs: StateFlow<List<NetworkLogEntity>> = dao.getAllNetworkLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
}
