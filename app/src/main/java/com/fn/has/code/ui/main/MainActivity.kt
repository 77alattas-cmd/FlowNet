package com.fn.has.code.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fn.has.code.core.constants.AppConstants
import com.fn.has.code.service.FlowNetCoreService
import com.fn.has.code.ui.theme.FlowNetTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlowNetTheme {
                MainDashboardScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object CaptiveLogs : Screen("captive_logs", "Captive Logs", Icons.Default.Security)
    object NetworkLogs : Screen("network_logs", "Network Logs", Icons.Default.List)
    object Tools : Screen("tools", "Tools", Icons.Default.Build)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowNetApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var isServiceRunning by remember { mutableStateOf(false) }

    val captiveLogs by viewModel.captiveLogs.collectAsState()
    val networkLogs by viewModel.networkLogs.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "FlowNet Network Toolkit", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val screens = listOf(Screen.Dashboard, Screen.CaptiveLogs, Screen.NetworkLogs, Screen.Tools)
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                is Screen.Dashboard -> MainDashboardScreen()
                is Screen.CaptiveLogs -> CaptiveLogsScreen(
                    logs = captiveLogs,
                    onClearLogs = { viewModel.clearCaptiveLogs() }
                )
                is Screen.NetworkLogs -> NetworkLogsScreen(
                    logs = networkLogs,
                    onClearLogs = { viewModel.clearNetworkLogs() },
                    onAddSampleLog = {
                        viewModel.addNetworkLog("Diagnostic Ping", "Gateway ping successful to 192.168.49.1", "INFO")
                    }
                )
                is Screen.Tools -> ToolsScreen()
            }
        }
    }
}

@Composable
fun DashboardScreen(
    isServiceRunning: Boolean,
    onToggleService: (Boolean) -> Unit,
    captiveCount: Int,
    networkLogCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Core Engine Status", style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isServiceRunning) "Status: Active & Routing" else "Status: Inactive",
                        color = if (isServiceRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = onToggleService
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Captive Intercepts",
                count = captiveCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Network Logs",
                count = networkLogCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Quick Network Info", style = MaterialTheme.typography.titleLarge)
                Text(text = "Gateway IP: ${AppConstants.DEFAULT_GATEWAY_IP}")
                Text(text = "Proxy Port: ${AppConstants.PORT_PROXY}")
                Text(text = "DNS Port: ${AppConstants.PORT_DNS}")
                Text(text = "Target TTL: ${AppConstants.TARGET_TTL_VALUE}")
            }
        }
    }
}

@Composable
fun StatCard(title: String, count: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = count, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun CaptiveLogsScreen(
    logs: List<com.fn.has.code.data.local.db.entity.CaptiveLogEntity>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Captive Portal Intercepts", style = MaterialTheme.typography.titleLarge)
            if (logs.isNotEmpty()) {
                Button(onClick = onClearLogs, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Clear")
                }
            }
        }

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No captive portal requests intercepted yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Host: ${log.targetHost}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "URL: ${log.redirectUrl}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Client IP: ${log.clientIp}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkLogsScreen(
    logs: List<com.fn.has.code.data.local.db.entity.NetworkLogEntity>,
    onClearLogs: () -> Unit,
    onAddSampleLog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Network Activity Logs", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddSampleLog) {
                    Text("Ping")
                }
                if (logs.isNotEmpty()) {
                    Button(onClick = onClearLogs, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Clear")
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No network logs recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = log.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(text = log.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            Text(text = log.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Advanced Tools", style = MaterialTheme.typography.titleLarge)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "TTL Normalization", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = "Ensures tethered traffic bypasses carrier hotspot inspection by fixing TTL to 64.")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Local File Share Server", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = "HTTP File Server active on port ${AppConstants.PORT_FILE_SERVER} for local device file sharing.")
            }
        }
    }
}
