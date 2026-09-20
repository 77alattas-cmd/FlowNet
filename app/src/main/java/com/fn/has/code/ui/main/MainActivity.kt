package com.fn.has.code.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fn.has.code.FlowNetApplication
import com.fn.has.code.core.security.SecureCredentialsStore
import com.fn.has.code.server.LocalProxyServer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as FlowNetApplication
                val credentials = SecureCredentialsStore(app)
                val proxyServer = LocalProxyServer(credentials)
                return MainViewModel(app.database.flowNetDao(), proxyServer) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainDashboardScreen(viewModel = viewModel)
        }
    }
}
