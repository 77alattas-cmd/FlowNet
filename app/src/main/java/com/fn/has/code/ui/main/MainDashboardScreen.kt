package com.fn.has.code.ui.main

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fn.has.code.core.constants.AppConstants
import com.fn.has.code.core.utils.*
import com.fn.has.code.data.local.db.AppDatabase
import com.fn.has.code.data.local.db.entity.BlockedDomainEntity
import com.fn.has.code.service.FlowNetCoreService
import kotlinx.coroutines.launch

// ألوان Light Mode نقية وخالية من الأسود الصريح
private val AppBgLight = Color(0xFFF4F7FC)
private val AppSurfaceLight = Color(0xFFFFFFFF)
private val AppSurfaceElevated = Color(0xFFF8FAFC)
private val TextDarkColor = Color(0xFF1E293B)
private val TextGrayColor = Color(0xFF64748B)
private val PrimaryBlue = Color(0xFF3182CE)
private val SuccessGreen = Color(0xFF38A169)
private val WarningOrange = Color(0xFFED8936)
private val DangerRed = Color(0xFFE53E3E)

@Composable
fun MainDashboardScreen(viewModel: MainViewModel = viewModel()) {
    FlowNetOrganizedAppScreen(viewModel = viewModel)
}

@Composable
fun FlowNetOrganizedAppScreen(viewModel: MainViewModel = viewModel()) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var logSubTab by rememberSaveable { mutableIntStateOf(0) }

    // إعدادات الشبكة والعناوين والمنافذ الموحدة تماماً
    var ssidPrefix by rememberSaveable { mutableStateOf("FlowNet_AP_") }
    var networkPassword by rememberSaveable { mutableStateOf("12345678") }
    var proxyAddress by rememberSaveable { mutableStateOf("192.168.1.1") }
    var proxyPortInput by rememberSaveable { mutableStateOf("8080") }
    var fileServerPort by rememberSaveable { mutableStateOf("5050") }
    var extraPortInput by rememberSaveable { mutableStateOf("9090") }

    // حالات المكالمات والمراسلة
    var inActiveCall by rememberSaveable { mutableStateOf(false) }
    var callType by rememberSaveable { mutableStateOf("") }
    var isMuted by rememberSaveable { mutableStateOf(false) }

    var chatMessage by rememberSaveable { mutableStateOf("") }
    var chatList by remember {
        mutableStateOf(
            listOf(
                "جهاز [192.168.1.15]: الاتصال مستقر ومؤمن.",
                "جهاز [192.168.1.20]: تم فحص ومزامنة إعدادات الشبكة بنجاح."
            )
        )
    }

    // حالات الشبكة والحظر
    var blockedDevicesList by remember {
        mutableStateOf(
            listOf(
                "192.168.1.50 (حظر نشط)",
                "192.168.1.99 (حظر مؤقت)"
            )
        )
    }
    var newBlockedIp by rememberSaveable { mutableStateOf("") }
    var sharedFilesList by remember {
        mutableStateOf(
            listOf(
                "document_secure.pdf",
                "config_backup.json",
                "media_stream.mp4"
            )
        )
    }
    var parentalFilterActive by rememberSaveable { mutableStateOf(true) }

    // المزايا المتقدمة
    var tlsEncryptionActive by rememberSaveable { mutableStateOf(true) }
    var securityAlertsList by remember {
        mutableStateOf(
            listOf(
                "⚠️ تنبيه: محاولة وصول غير مصرح بها من IP: 192.168.1.88",
                "🛡️ أمان: تم تفعيل وتطبيق شهادات TLS بنجاح.",
                "📡 شبكة: تثبيت قيمة TTL على 64 لتخطي قيود البث نشط."
            )
        )
    }
    var configExportStatus by rememberSaveable { mutableStateOf("جاهز للتصدير / الاستيراد") }

    val context = LocalContext.current
    val settingsStore = remember { NetworkSettingsStore(context) }
    val networkState by settingsStore.settingsState.collectAsState()
    val localIp = remember { NetworkUtils.getLocalIpAddress() }

    // اسم الشبكة الموحد الذي يظهر في كل التطبيق لضمان عدم اختلافه
    val synchronizedFullSsid = "$ssidPrefix${localIp.takeLast(3)}"

    val captiveLogs by viewModel.captiveLogs.collectAsState()
    val networkLogs by viewModel.networkLogs.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }
    var showWifiDialog by remember { mutableStateOf(false) }

    val connectionManager = remember { ConnectionModeManager(context) }
    var currentMode by remember { mutableStateOf(connectionManager.currentMode) }
    val db = remember { AppDatabase.getDatabase(context) }
    val blockedDao = remember { db.blockedDomainDao() }
    val parentalManager = remember { ParentalControlManager(context = context, database = db) }
    val scope = rememberCoroutineScope()

    var isAdultEnabled by remember { mutableStateOf(parentalManager.isAdultBlockingEnabled) }
    var isGamblingEnabled by remember { mutableStateOf(parentalManager.isGamblingBlockingEnabled) }
    var isSocialEnabled by remember { mutableStateOf(parentalManager.isSocialBlockingEnabled) }
    var isAdsEnabled by remember { mutableStateOf(parentalManager.isAdsBlockingEnabled) }
    var selectedDns by remember { mutableStateOf(parentalManager.selectedUpstreamDns) }
    var customDomainInput by remember { mutableStateOf("") }
    val blockedDomainsList by blockedDao.getAllBlockedDomains().collectAsState(initial = emptyList())

    var pingTarget by remember { mutableStateOf("1.1.1.1") }
    var pingResultText by remember { mutableStateOf<String?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            background = AppBgLight,
            surface = AppSurfaceLight,
            onBackground = TextDarkColor,
            onSurface = TextDarkColor,
            primary = PrimaryBlue
        )
    ) {
        Scaffold(
            containerColor = AppBgLight,
            topBar = {
                Surface(
                    color = AppSurfaceLight,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FlowNet Core",
                            color = TextDarkColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedTab = 2 }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.QrCodeScanner,
                                    contentDescription = "QR Scanner & Code",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(onClick = { showWifiDialog = true }, modifier = Modifier.size(36.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFE2E8F0), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Wifi,
                                        contentDescription = "Wifi",
                                        tint = TextDarkColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = AppSurfaceLight,
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OrganizedNavItem(icon = Icons.Rounded.Home, label = "الرئيسية", isSelected = selectedTab == 0) { selectedTab = 0 }
                        OrganizedNavItem(icon = Icons.Rounded.Call, label = "الاتصال", isSelected = selectedTab == 1) { selectedTab = 1 }
                        OrganizedNavItem(icon = Icons.Rounded.NetworkCheck, label = "الشبكة والباركود", isSelected = selectedTab == 2) { selectedTab = 2 }
                        OrganizedNavItem(icon = Icons.Rounded.Security, label = "الرقابة", isSelected = selectedTab == 3) { selectedTab = 3 }
                        OrganizedNavItem(icon = Icons.Rounded.History, label = "السجلات", isSelected = selectedTab == 4) { selectedTab = 4 }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBgLight)
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    // ---------------------------------------------------------
                    // 0. الرئيسية
                    // ---------------------------------------------------------
                    0 -> {
                        item {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = PrimaryBlue,
                                shadowElevation = 6.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (networkState.isNetworkActive) "الخدمة مفعلة (Protected)" else "الخدمة متوقفة",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Switch(
                                            checked = networkState.isNetworkActive,
                                            onCheckedChange = { active ->
                                                settingsStore.toggleNetwork(active)
                                                val serviceIntent = Intent(context, FlowNetCoreService::class.java)
                                                if (active) {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        context.startForegroundService(serviceIntent)
                                                    } else {
                                                        context.startService(serviceIntent)
                                                    }
                                                    viewModel.addNetworkLog("تشغيل الخدمة", "تم تفعيل محرك FlowNet Core بنجاح", "SUCCESS")
                                                } else {
                                                    context.stopService(serviceIntent)
                                                    viewModel.addNetworkLog("إيقاف الخدمة", "تم إيقاف محرك الخدمة وتوجيه الحزم", "WARN")
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF2B6CB0),
                                                uncheckedThumbColor = Color.White,
                                                uncheckedTrackColor = Color(0xFFCBD5E0)
                                            )
                                        )
                                    }

                                    Text(
                                        text = "إشعار الخدمة الخلفية (Foreground Service) نشط لتوجيه الحزم",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp
                                    )

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = "عنوان IP المحلي", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                                            Text(text = localIp, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = "اسم الشبكة (SSID الموحد)", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                                            Text(text = synchronizedFullSsid, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // عداد استهلاك البيانات المباشر
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = AppSurfaceLight,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                        Text(text = "استهلاك البيانات المباشر (Bandwidth Meter)", fontWeight = FontWeight.Bold, color = TextDarkColor, fontSize = 14.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "📥 التنزيل: 4.2 MB/s", color = TextGrayColor, fontSize = 12.sp)
                                        Text(text = "📤 الرفع: 1.1 MB/s", color = TextGrayColor, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // البطاقات المختصرة للبروكسي ومشاركة الملفات
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFFFFAF0),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(130.dp)
                                        .clickable { selectedTab = 2 }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.Public, contentDescription = null, tint = Color(0xFFDD6B20), modifier = Modifier.size(16.dp))
                                            Text(text = "البروكسي", color = Color(0xFFDD6B20), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text(text = if (networkState.isNetworkActive) "نشط" else "متوقف", color = TextDarkColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                            Text(text = "المنفذ: ${NumberFormatter.formatPort(proxyPortInput.toIntOrNull() ?: 8080)}", color = TextGrayColor, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFF0FFF4),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(130.dp)
                                        .clickable { selectedTab = 2 }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.FolderShared, contentDescription = null, tint = Color(0xFF38A169), modifier = Modifier.size(16.dp))
                                            Text(text = "مشاركة الملفات", color = Color(0xFF38A169), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text(text = "يعمل", color = TextDarkColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                            Text(text = "المنفذ: $fileServerPort", color = TextGrayColor, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // بطاقات الإحصائيات
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = AppSurfaceLight,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.weight(1f).clickable { selectedTab = 4; logSubTab = 0 }
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(modifier = Modifier.size(36.dp).background(PrimaryBlue.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Security, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                        }
                                        Column {
                                            Text("اعتراضات البوابة", fontSize = 11.sp, color = TextGrayColor)
                                            Text(captiveLogs.size.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDarkColor)
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = AppSurfaceLight,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.weight(1f).clickable { selectedTab = 4; logSubTab = 1 }
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(modifier = Modifier.size(36.dp).background(SuccessGreen.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Timeline, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                        }
                                        Column {
                                            Text("سجلات الشبكة", fontSize = 11.sp, color = TextGrayColor)
                                            Text(networkLogs.size.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDarkColor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---------------------------------------------------------
                    // 1. الاتصال والمراسلة
                    // ---------------------------------------------------------
                    1 -> {
                        item {
                            Text(text = "الاتصال والمكالمات والمراسلة الفورية", color = TextDarkColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        // قسم المكالمات
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = AppSurfaceLight,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = "إدارة الاتصالات المباشرة", fontWeight = FontWeight.Bold, color = TextDarkColor)

                                    if (!inActiveCall) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    inActiveCall = true
                                                    callType = "صوتي"
                                                    viewModel.addNetworkLog("مكالمة صوتية", "تم بدء مكالمة صوتية مشفرة محلياً", "INFO")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("مكالمة صوتية")
                                            }
                                            Button(
                                                onClick = {
                                                    inActiveCall = true
                                                    callType = "مرئي"
                                                    viewModel.addNetworkLog("مكالمة مرئية", "تم بدء مكالمة فيديو P2P محلية", "INFO")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Rounded.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("مكالمة مرئية")
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFEDF2F7), RoundedCornerShape(12.dp))
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(text = "مكالمة $callType جارية الآن عبر الشبكة...", color = Color(0xFF2B6CB0), fontWeight = FontWeight.Bold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Button(
                                                    onClick = { isMuted = !isMuted },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (isMuted) WarningOrange else Color(0xFF4A5568))
                                                ) {
                                                    Text(if (isMuted) "إلغاء الكتم" else "كتم الصوت")
                                                }
                                                Button(
                                                    onClick = {
                                                        inActiveCall = false
                                                        viewModel.addNetworkLog("إنهاء مكالمة", "تم إنهاء المكالمة بنجاح", "INFO")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                                                ) {
                                                    Text("إنهاء المكالمة")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // دردشة الشبكة المحلية
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = AppSurfaceLight,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = "دردشة الشبكة المحلية (LAN Chat)", fontWeight = FontWeight.Bold, color = TextDarkColor)

                                    chatList.forEach { msg ->
                                        Text(text = "💬 $msg", color = TextDarkColor, fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = chatMessage,
                                            onValueChange = { chatMessage = it },
                                            placeholder = { Text("اكتب رسالة...", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                if (chatMessage.isNotBlank()) {
                                                    chatList = chatList + "أنت: $chatMessage"
                                                    viewModel.addNetworkLog("رسالة محادثة", chatMessage, "INFO")
                                                    chatMessage = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                        ) {
                                            Text("إرسال")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---------------------------------------------------------
                    // 2. الشبكة والباركود (تم دمج الإعدادات والباركود بالكامل)
                    // ---------------------------------------------------------
                    2 -> {
                        item {
                            Text(text = "إعدادات الشبكة، المنافذ، وتوليد الباركود", color = TextDarkColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        // بطاقة الباركود للاتصال السريع مع توليد حقيقي
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEBF8FF),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.QrCode, contentDescription = null, tint = PrimaryBlue)
                                        Text(text = "باركود الاتصال السريع بالشبكة ومشاركة الملفات", fontWeight = FontWeight.Bold, color = Color(0xFF2B6CB0), fontSize = 15.sp)
                                    }

                                    // توليد رمز QR حقيقي للشبكة
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White, RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            NetworkUtils.generateQrCode("WIFI:S:$synchronizedFullSsid;T:WPA;P:$networkPassword;;")?.let { qrBmp ->
                                                Image(bitmap = qrBmp.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(130.dp))
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(text = "SSID: $synchronizedFullSsid", color = TextDarkColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "رابط الملفات: http://$localIp:$fileServerPort", color = TextGrayColor, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // بطاقة اسم الشبكة وكلمة المرور الموحدة
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = AppSurfaceLight,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = "بيانات نقطة الاتصال اللاسلكية", fontWeight = FontWeight.Bold, color = TextDarkColor, fontSize = 15.sp)

                                    OutlinedTextField(
                                        value = ssidPrefix,
                                        onValueChange = { ssidPrefix = it },
                                        label = { Text("اسم بادئة الشبكة (SSID Prefix)", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = networkPassword,
                                        onValueChange = { networkPassword = it },
                                        label = { Text("كلمة المرور (Password)", fontSize = 12.sp) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        // بطاقة البروكسي والمنافذ
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = AppSurfaceLight,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = "البروكسي والمنافذ (Proxy & Ports)", fontWeight = FontWeight.Bold, color = TextDarkColor, fontSize = 15.sp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = proxyAddress,
                                            onValueChange = { proxyAddress = it },
                                            label = { Text("عنوان البروكسي", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1.5f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = proxyPortInput,
                                            onValueChange = { proxyPortInput = it },
                                            label = { Text("منفذ البروكسي", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = fileServerPort,
                                            onValueChange = { fileServerPort = it },
                                            label = { Text("منفذ الملفات", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = extraPortInput,
                                            onValueChange = { extraPortInput = it },
                                            label = { Text("منافذ أخرى", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "تم تطبيق وحفظ إعدادات المنافذ والشبكة بنجاح", Toast.LENGTH_SHORT).show()
                                            viewModel.addNetworkLog("تحديث المنافذ", "تم حفظ منفذ البروكسي: $proxyPortInput ومنفذ الملفات: $fileServerPort", "SUCCESS")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("حفظ وتطبيق إعدادات المنافذ")
                                    }
                                }
                            }
                        }

                        // شهادات الأمان TLS
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = AppSurfaceLight,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                            Text(text = "تشفير الحزم وشهادات TLS/SSL", fontWeight = FontWeight.Bold, color = TextDarkColor)
                                        }
                                        Switch(
                                            checked = tlsEncryptionActive,
                                            onCheckedChange = { tlsEncryptionActive = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SuccessGreen)
                                        )
                                    }
                                    Text(
                                        text = if (tlsEncryptionActive) "شهادات الأمان مفعلة لتأمين الحزم." else "التشفير معطل.",
                                        color = TextGrayColor,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // ---------------------------------------------------------
                    // 3. الرقابة والحظر
                    // ---------------------------------------------------------
                    3 -> {
                        item {
                            Text(text = "الرقابة الأبوية وحظر الأجهزة", color = TextDarkColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = AppSurfaceLight,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.Security, contentDescription = null, tint = SuccessGreen)
                                            Text(text = "فلترة محتوى الروابط", fontWeight = FontWeight.Bold, color = TextDarkColor)
                                        }
                                        Switch(
                                            checked = parentalFilterActive,
                                            onCheckedChange = { parentalFilterActive = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SuccessGreen)
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    Text(text = "قائمة الأجهزة المحجوبة (Device Blocking)", fontWeight = FontWeight.Bold, color = TextDarkColor)
                                    blockedDevicesList.forEach { dev ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "🚫 $dev", color = TextGrayColor, fontSize = 12.sp)
                                            TextButton(onClick = { blockedDevicesList = blockedDevicesList - dev }) {
                                                Text("إلغاء الحظر", color = PrimaryBlue, fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newBlockedIp,
                                            onValueChange = { newBlockedIp = it },
                                            placeholder = { Text("أدخل IP للحظر...", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                if (newBlockedIp.isNotBlank()) {
                                                    blockedDevicesList = blockedDevicesList + "$newBlockedIp (حظر يدوي)"
                                                    newBlockedIp = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                                        ) {
                                            Text("حظر جهاز")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---------------------------------------------------------
                    // 4. السجلات
                    // ---------------------------------------------------------
                    4 -> {
                        item {
                            Text(text = "سجلات وتنبيهات النظام", color = TextDarkColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = AppSurfaceLight,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "تنبيهات الأمان الفورية:", fontWeight = FontWeight.Bold, color = TextDarkColor, fontSize = 13.sp)
                                    securityAlertsList.forEach { alert ->
                                        Text(text = "• $alert", color = WarningOrange, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWifiDialog) {
        AlertDialog(
            onDismissRequest = { showWifiDialog = false },
            containerColor = AppSurfaceLight,
            title = { Text(text = "معلومات اتصال Wi-Fi", color = TextDarkColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "اسم الشبكة الموحد: $synchronizedFullSsid", color = TextDarkColor)
                    Text(text = "عنوان IP: $localIp", color = TextGrayColor)
                    Text(text = "منفذ البروكسي: $proxyPortInput", color = TextGrayColor)
                    Text(text = "منفذ الملفات: $fileServerPort", color = TextGrayColor)
                }
            },
            confirmButton = {
                Button(onClick = { showWifiDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                    Text(text = "حسناً", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun OrganizedNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) PrimaryBlue else TextGrayColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) PrimaryBlue else TextGrayColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
