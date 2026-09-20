package com.fn.has.code.ui.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.fn.has.code.core.constants.AppConstants
import com.fn.has.code.core.utils.*
import com.fn.has.code.data.local.db.AppDatabase
import com.fn.has.code.data.local.db.entity.BlockedDomainEntity
import com.fn.has.code.service.FlowNetCoreService
import com.fn.has.code.service.FlowNetVpnService
import com.fn.has.code.ui.components.CameraQrScannerView
import com.fn.has.code.ui.components.GlassCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = AppConstants.COLOR_ACCENT_CYAN
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "الرئيسية والمسح") },
                    label = { Text("الرئيسية والمسح", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.WifiTethering, contentDescription = "الشبكة والمشاركة") },
                    label = { Text("الشبكة والمشاركة", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Security, contentDescription = "الرقابة و DNS") },
                    label = { Text("الرقابة & DNS", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        },
        containerColor = AppConstants.COLOR_BACKGROUND
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> HomeAndScannerTab()
                1 -> NetworkAndSharingTab()
                2 -> ParentalDnsTab()
            }
        }
    }
}

// -------------------------------------------------------------
// التبويب الأول الموحد: الشاشة الرئيسية، الـ IP، الباركود، والماسح الحي
// -------------------------------------------------------------
@Composable
fun HomeAndScannerTab() {
    val context = LocalContext.current
    val settingsStore = remember { NetworkSettingsStore(context) }
    val networkState by settingsStore.settingsState.collectAsState()
    val localIp = remember { NetworkUtils.getLocalIpAddress() }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var scannedResultText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val portProxyFormatted = remember(networkState.proxyPort) { NumberFormatter.formatPort(networkState.proxyPort) }
    val portHttpFormatted = remember(networkState.fileServerPort) { NumberFormatter.formatPort(networkState.fileServerPort) }
    val portDnsFormatted = remember(networkState.dnsPort) { NumberFormatter.formatPort(networkState.dnsPort) }

    val qrContent = "WIFI:S:${networkState.fullSsid};T:WPA;P:${networkState.wifiPassword};;"
    val qrBitmap = remember(qrContent) { NetworkUtils.generateQrCode(qrContent) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // الهيدر الرئيسي وزر التشغيل
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppConstants.APP_NAME,
                        color = AppConstants.COLOR_ACCENT_CYAN,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "مركز الاتصال السريع ومسح الشبكات",
                        color = AppConstants.COLOR_TEXT_SECONDARY,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (networkState.isNetworkActive) AppConstants.COLOR_SUCCESS_GREEN.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (networkState.isNetworkActive) AppConstants.COLOR_SUCCESS_GREEN else Color.Gray)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (networkState.isNetworkActive) AppConstants.COLOR_SUCCESS_GREEN else Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (networkState.isNetworkActive) "نشط" else "متوقف",
                            color = if (networkState.isNetworkActive) AppConstants.COLOR_SUCCESS_GREEN else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // زر تشغيل الخدمة المباشر
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "تشغيل بث الشبكة والخدمات", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (networkState.isNetworkActive) "الخدمات تعمل بنجاح في الخلفية" else "انقر لتشغيل الخدمات الشبكية",
                            color = AppConstants.COLOR_TEXT_SECONDARY,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = networkState.isNetworkActive,
                        onCheckedChange = { active ->
                            settingsStore.toggleNetwork(active)
                            val serviceIntent = Intent(context, FlowNetCoreService::class.java)
                            if (active) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                            } else {
                                context.stopService(serviceIntent)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AppConstants.COLOR_ACCENT_CYAN
                        )
                    )
                }
            }
        }

        // تفاصيل الـ IP ومعلومات الشبكة
        item {
            GlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "عنوان IP المحلي (IPv4)", color = AppConstants.COLOR_TEXT_SECONDARY, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = localIp, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اسم الشبكة المكتشف: ${networkState.fullSsid}",
                        color = AppConstants.COLOR_ACCENT_CYAN,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // الباركود المباشر للاتصال والماسح الضوئي بالكاميرا جنبًا إلى جنب
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. باركود الاتصال الخاص بك
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "باركود الاتصال بالشبكة (QR)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "امسحه من هاتف آخر للاتصال المباشر", color = AppConstants.COLOR_TEXT_SECONDARY, fontSize = 11.sp)
                        }
                        IconButton(onClick = { settingsStore.toggleQrVisibility() }) {
                            Icon(
                                imageVector = if (networkState.isQrCodeVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.QrCode2,
                                contentDescription = "إظهار/إخفاء الباركود",
                                tint = AppConstants.COLOR_ACCENT_CYAN
                            )
                        }
                    }

                    if (networkState.isQrCodeVisible) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            qrBitmap?.let { bmp ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "WiFi QR",
                                        modifier = Modifier.size(150.dp).padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. الماسح الحي بالكاميرا (شغال دائماً في الرئيسية عند منح الإذن)
                GlassCard {
                    Text(text = "الماسح الحي بالكاميرا (Always-On Scanner)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (hasCameraPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        ) {
                            CameraQrScannerView { scannedUrl ->
                                scannedResultText = scannedUrl
                            }
                        }
                    } else {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppConstants.COLOR_ACCENT_CYAN),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "منح إذن الكاميرا للتشغيل الحي", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (scannedResultText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "الرابط المكتشف: $scannedResultText",
                                color = AppConstants.COLOR_ACCENT_CYAN,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // عرض المنافذ
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusMetricCard(
                    title = "البروكسي",
                    value = portProxyFormatted,
                    icon = Icons.Rounded.Router,
                    accentColor = AppConstants.COLOR_ACCENT_CYAN,
                    modifier = Modifier.weight(1f)
                )
                StatusMetricCard(
                    title = "HTTP",
                    value = portHttpFormatted,
                    icon = Icons.Rounded.Http,
                    accentColor = AppConstants.COLOR_SUCCESS_GREEN,
                    modifier = Modifier.weight(1f)
                )
                StatusMetricCard(
                    title = "DNS",
                    value = portDnsFormatted,
                    icon = Icons.Rounded.Dns,
                    accentColor = AppConstants.COLOR_UP_BLUE,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// التبويب الثاني: إعدادات الشبكة، كلمة المرور المخفية، المنافذ ومشاركة SAF
// -------------------------------------------------------------
@Composable
fun NetworkAndSharingTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { NetworkSettingsStore(context) }
    val networkState by settingsStore.settingsState.collectAsState()

    val modeManager = remember { ConnectionModeManager(context) }
    var selectedMode by remember { mutableStateOf(modeManager.currentMode) }
    var isVpnActive by remember { mutableStateOf(false) }

    var editableSsidSuffix by rememberSaveable(networkState.ssidSuffix) { mutableStateOf(networkState.ssidSuffix) }
    var editablePassword by rememberSaveable(networkState.wifiPassword) { mutableStateOf(networkState.wifiPassword) }

    var editableProxyPort by rememberSaveable(networkState.proxyPort) { mutableStateOf(networkState.proxyPort.toString()) }
    var editableFilePort by rememberSaveable(networkState.fileServerPort) { mutableStateOf(networkState.fileServerPort.toString()) }
    var editableDnsPort by rememberSaveable(networkState.dnsPort) { mutableStateOf(networkState.dnsPort.toString()) }

    var folderName by remember { mutableStateOf("لم يتم تحديد مجلد بعد") }
    var isAuthEnabled by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("1234") }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val docFile = DocumentFile.fromTreeUri(context, it)
            folderName = docFile?.name ?: "مجلد مجهول"
            ServiceStateStore.updateFolderUri(it)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "إعدادات الشبكة والمشاركة", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // 1. تغيير لاحقة الشبكة وكلمة المرور المخفية
        item {
            GlassCard {
                Text(text = "تعديل اسم الشبكة وكلمة المرور", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editableSsidSuffix,
                    onValueChange = { editableSsidSuffix = it },
                    label = { Text("لاحقة اسم الشبكة (SSID Suffix)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppConstants.COLOR_ACCENT_CYAN),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editablePassword,
                    onValueChange = { editablePassword = it },
                    label = { Text("كلمة مرور الشبكة") },
                    singleLine = true,
                    visualTransformation = if (networkState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { settingsStore.togglePasswordVisibility() }) {
                            Icon(
                                imageVector = if (networkState.isPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = "إظهار/إخفاء كلمة المرور",
                                tint = AppConstants.COLOR_ACCENT_CYAN
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppConstants.COLOR_ACCENT_CYAN),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { 
                        settingsStore.updateSsidAndPassword(editableSsidSuffix, editablePassword)
                        // إعادة تطبيق الإعدادات على الخدمة المباشرة
                        restartServiceIfActive(context, networkState.isNetworkActive)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppConstants.COLOR_ACCENT_CYAN),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "حفظ بيانات الشبكة وتطبيقها", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. تعديل المنافذ
        item {
            GlassCard {
                Text(text = "تغيير منافذ الخدمات (Ports)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editableProxyPort,
                        onValueChange = { if (it.all { c -> c.isDigit() }) editableProxyPort = it },
                        label = { Text("البروكسي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editableFilePort,
                        onValueChange = { if (it.all { c -> c.isDigit() }) editableFilePort = it },
                        label = { Text("HTTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editableDnsPort,
                        onValueChange = { if (it.all { c -> c.isDigit() }) editableDnsPort = it },
                        label = { Text("DNS") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val pProxy = editableProxyPort.toIntOrNull() ?: networkState.proxyPort
                        val pFile = editableFilePort.toIntOrNull() ?: networkState.fileServerPort
                        val pDns = editableDnsPort.toIntOrNull() ?: networkState.dnsPort
                        settingsStore.updatePorts(pProxy, pFile, pDns)
                        restartServiceIfActive(context, networkState.isNetworkActive)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppConstants.COLOR_ACCENT_CYAN),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "تحديث المنافذ النشطة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. مشاركة الذاكرة المحلية (SAF)
        item {
            GlassCard {
                Text(text = "مشاركة الملفات المحلية (HTTP SAF)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "المجلد المشارك:", color = AppConstants.COLOR_TEXT_SECONDARY, fontSize = 12.sp)
                        Text(text = folderName, color = AppConstants.COLOR_ACCENT_CYAN, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppConstants.COLOR_ACCENT_CYAN)
                    ) {
                        Text(text = "اختر مجلد", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "تفعيل كلمة مرور للملفات", color = Color.White)
                    Switch(
                        checked = isAuthEnabled,
                        onCheckedChange = {
                            isAuthEnabled = it
                            ServiceStateStore.updateAuthConfig(it, username, password)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = AppConstants.COLOR_ACCENT_CYAN)
                    )
                }
            }
        }

        // 4. تحديد نمط التوجيه
        item {
            GlassCard {
                Text(text = "نمط التوجيه والصلاحيات", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                ConnectionType.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == mode,
                            onClick = {
                                selectedMode = mode
                                modeManager.currentMode = mode
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = AppConstants.COLOR_ACCENT_CYAN)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = mode.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        scope.launch { modeManager.applyConnectionSettings() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppConstants.COLOR_ACCENT_CYAN),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "تطبيق نمط التوجيه", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// التبويب الثالث: الرقابة الأبوية وخيارات DNS المكتملة (مع خيار بدون)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalDnsTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parentalManager = remember { ParentalControlManager(context) }
    val db = remember { AppDatabase.getDatabase(context) }

    var isUnlocked by remember { mutableStateOf(!parentalManager.isPinSet()) }
    var inputPin by remember { mutableStateOf("") }
    var pinErrorText by remember { mutableStateOf("") }

    var isAdultBlocked by remember { mutableStateOf(parentalManager.isAdultBlockingEnabled) }
    var isGamblingBlocked by remember { mutableStateOf(parentalManager.isGamblingBlockingEnabled) }
    var isSocialBlocked by remember { mutableStateOf(parentalManager.isSocialBlockingEnabled) }
    var isAdsBlocked by remember { mutableStateOf(parentalManager.isAdsBlockingEnabled) }
    var upstreamDns by remember { mutableStateOf(parentalManager.selectedUpstreamDns) }

    var newCustomDomain by remember { mutableStateOf("") }
    val customDomainsList by db.blockedDomainDao().getAllBlockedDomains().collectAsState(initial = emptyList())

    if (!isUnlocked) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = AppConstants.COLOR_ACCENT_CYAN, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "الرقابة الأبوية محمية برمز PIN", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "أدخل الرمز المكون من 4 أرقام للمتابعة", color = AppConstants.COLOR_TEXT_SECONDARY, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) inputPin = it },
                        label = { Text("رمز PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppConstants.COLOR_ACCENT_CYAN)
                    )

                    if (pinErrorText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = pinErrorText, color = AppConstants.COLOR_DANGER_RED, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (parentalManager.verifyPin(inputPin)) {
                                isUnlocked = true
                                pinErrorText = ""
                            } else {
                                pinErrorText = "رمز PIN غير صحيح!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppConstants.COLOR_ACCENT_CYAN),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "تأكيد الدخول", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "إعدادات الرقابة الأبوية وترشيح DNS", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // اختيار خادم DNS العلوي مع خيار "بدون"
        item {
            GlassCard {
                Text(text = "خادم DNS العلوي (Upstream)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = upstreamDns == "none",
                        onClick = {
                            upstreamDns = "none"
                            parentalManager.selectedUpstreamDns = "none"
                        },
                        label = { Text("بدون (مباشر)") }
                    )
                    FilterChip(
                        selected = upstreamDns == "1.1.1.3",
                        onClick = {
                            upstreamDns = "1.1.1.3"
                            parentalManager.selectedUpstreamDns = "1.1.1.3"
                        },
                        label = { Text("Cloudflare") }
                    )
                    FilterChip(
                        selected = upstreamDns == "9.9.9.9",
                        onClick = {
                            upstreamDns = "9.9.9.9"
                            parentalManager.selectedUpstreamDns = "9.9.9.9"
                        },
                        label = { Text("Quad9") }
                    )
                    FilterChip(
                        selected = upstreamDns == "8.8.8.8",
                        onClick = {
                            upstreamDns = "8.8.8.8"
                            parentalManager.selectedUpstreamDns = "8.8.8.8"
                        },
                        label = { Text("Google") }
                    )
                }
            }
        }

        item {
            GlassCard {
                Text(text = "تصنيفات الحجب التلقائية", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                CategoryToggleRow("حجب المواقع الإباحية والكبار", isAdultBlocked) {
                    isAdultBlocked = it
                    parentalManager.isAdultBlockingEnabled = it
                }
                CategoryToggleRow("حجب القمار والمراهنات", isGamblingBlocked) {
                    isGamblingBlocked = it
                    parentalManager.isGamblingBlockingEnabled = it
                }
                CategoryToggleRow("حجب منصات التواصل الاجتماعي", isSocialBlocked) {
                    isSocialBlocked = it
                    parentalManager.isSocialBlockingEnabled = it
                }
                CategoryToggleRow("حجب الإعلانات والبرمجيات الخبيثة", isAdsBlocked) {
                    isAdsBlocked = it
                    parentalManager.isAdsBlockingEnabled = it
                }
            }
        }

        item {
            GlassCard {
                Text(text = "إضافة نطاق مخصص للحجب", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCustomDomain,
                        onValueChange = { newCustomDomain = it },
                        label = { Text("اسم النطاق (مثال: example.com)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppConstants.COLOR_ACCENT_CYAN)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCustomDomain.isNotBlank()) {
                                scope.launch {
                                    db.blockedDomainDao().insertDomain(
                                        BlockedDomainEntity(domain = newCustomDomain.trim())
                                    )
                                    newCustomDomain = ""
                                }
                            }
                        },
                        modifier = Modifier.background(AppConstants.COLOR_ACCENT_CYAN, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "إضافة", tint = Color.Black)
                    }
                }
            }
        }

        item {
            Text(
                text = "النطاقات المحجوبة المخصصة (${NumberFormatter.formatCount(customDomainsList.size.toLong())})",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(customDomainsList) { domainEntity ->
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = domainEntity.domain, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = domainEntity.category, color = AppConstants.COLOR_TEXT_SECONDARY, fontSize = 11.sp)
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                db.blockedDomainDao().deleteDomain(domainEntity)
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = AppConstants.COLOR_DANGER_RED)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// المكونات المساعدة للبطاقات والتنسيق
// -------------------------------------------------------------
@Composable
fun StatusMetricCard(title: String, value: String, icon: ImageVector, accentColor: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, color = AppConstants.COLOR_TEXT_SECONDARY, fontSize = 11.sp)
                Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CategoryToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = AppConstants.COLOR_TEXT_PRIMARY, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AppConstants.COLOR_ACCENT_CYAN)
        )
    }
}
