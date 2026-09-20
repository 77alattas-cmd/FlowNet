package com.fn.has.code.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fn.has.code.core.constants.AppConstants
import com.fn.has.code.core.utils.NetworkSettingsStore
import com.fn.has.code.core.utils.NetworkUtils
import com.fn.has.code.core.utils.NumberFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkConfigCard() {
    val context = LocalContext.current
    val settingsStore = remember { NetworkSettingsStore(context) }
    val networkState by settingsStore.settingsState.collectAsState()

    // حفظ النص المحلي للحقول لمنع مسح الكتابة أثناء الـ Recomposition
    var ssidSuffixInput by rememberSaveable(networkState.ssidSuffix) { mutableStateOf(networkState.ssidSuffix) }
    var passwordInput by rememberSaveable(networkState.wifiPassword) { mutableStateOf(networkState.wifiPassword) }

    var proxyPortInput by rememberSaveable(networkState.proxyPort) { mutableStateOf(networkState.proxyPort.toString()) }
    var filePortInput by rememberSaveable(networkState.fileServerPort) { mutableStateOf(networkState.fileServerPort.toString()) }
    var dnsPortInput by rememberSaveable(networkState.dnsPort) { mutableStateOf(networkState.dnsPort.toString()) }

    val qrContent = "WIFI:S:${networkState.fullSsid};T:WPA;P:${networkState.wifiPassword};;"
    val qrBitmap = remember(qrContent) { NetworkUtils.generateQrCode(qrContent) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // -------------------------------------------------------------
        // 1. قسم تغيير لاحقة اسم الشبكة وكلمة المرور وإظهارها/إخفائها
        // -------------------------------------------------------------
        GlassCard {
            Text(
                text = "إعدادات اسم الشبكة وكلمة المرور",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "الاسم الكامل المبتث: ${networkState.fullSsid}",
                color = AppConstants.COLOR_ACCENT_CYAN,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // حقل تغيير لاحقة الشبكة
            OutlinedTextField(
                value = ssidSuffixInput,
                onValueChange = { ssidSuffixInput = it },
                label = { Text("لاحقة اسم الشبكة (SSID Suffix)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppConstants.COLOR_ACCENT_CYAN,
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // حقل تغيير كلمة المرور مع زر الإظهار والإخفاء
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("كلمة مرور الشبكة") },
                singleLine = true,
                visualTransformation = if (networkState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { settingsStore.togglePasswordVisibility() }) {
                        Icon(
                            imageVector = if (networkState.isPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = "إظهار أو إخفاء كلمة المرور",
                            tint = AppConstants.COLOR_ACCENT_CYAN
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppConstants.COLOR_ACCENT_CYAN,
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    settingsStore.updateSsidAndPassword(ssidSuffixInput, passwordInput)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppConstants.COLOR_ACCENT_CYAN),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "حفظ اسم الشبكة وكلمة المرور", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // -------------------------------------------------------------
        // 2. قسم الباركود المخفي (QR Code)
        // -------------------------------------------------------------
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الباركود المخفي للاتصال السريع",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (networkState.isQrCodeVisible) "الرمز معروض الآن" else "الرمز مخفي حالياً",
                        color = AppConstants.COLOR_TEXT_SECONDARY,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { settingsStore.toggleQrVisibility() }) {
                    Icon(
                        imageVector = if (networkState.isQrCodeVisible) Icons.Rounded.QrCode2 else Icons.Rounded.QrCodeScanner,
                        contentDescription = "تبديل حالة الباركود",
                        tint = AppConstants.COLOR_ACCENT_CYAN
                    )
                }
            }

            if (networkState.isQrCodeVisible) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    qrBitmap?.let { bmp ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Hidden WiFi Barcode",
                                modifier = Modifier
                                    .size(160.dp)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. قسم تغيير المنافذ (Proxy / HTTP File / DNS)
        // -------------------------------------------------------------
        GlassCard {
            Text(
                text = "تغيير منافذ الخدمات (Ports)",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = proxyPortInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) proxyPortInput = it },
                    label = { Text("البروكسي") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppConstants.COLOR_ACCENT_CYAN),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = filePortInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) filePortInput = it },
                    label = { Text("HTTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppConstants.COLOR_SUCCESS_GREEN),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = dnsPortInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) dnsPortInput = it },
                    label = { Text("DNS") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppConstants.COLOR_UP_BLUE),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val pProxy = proxyPortInput.toIntOrNull() ?: networkState.proxyPort
                    val pFile = filePortInput.toIntOrNull() ?: networkState.fileServerPort
                    val pDns = dnsPortInput.toIntOrNull() ?: networkState.dnsPort
                    settingsStore.updatePorts(pProxy, pFile, pDns)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppConstants.COLOR_ACCENT_CYAN),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "تحديث المنافذ النشطة", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "البروكسي: ${NumberFormatter.formatPort(networkState.proxyPort)}",
                    color = AppConstants.COLOR_TEXT_SECONDARY,
                    fontSize = 11.sp
                )
                Text(
                    text = "الملفات: ${NumberFormatter.formatPort(networkState.fileServerPort)}",
                    color = AppConstants.COLOR_TEXT_SECONDARY,
                    fontSize = 11.sp
                )
                Text(
                    text = "DNS: ${NumberFormatter.formatPort(networkState.dnsPort)}",
                    color = AppConstants.COLOR_TEXT_SECONDARY,
                    fontSize = 11.sp
                )
            }
        }
    }
}
