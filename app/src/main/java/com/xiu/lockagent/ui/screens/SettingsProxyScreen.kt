package com.xiu.lockagent.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MysticBackground
import com.example.ui.theme.MysticBorder
import com.example.ui.theme.MysticContainer
import com.example.ui.theme.MysticContainerElevated
import com.example.ui.theme.MysticCyan
import com.example.ui.theme.MysticError
import com.example.ui.theme.MysticPrimary
import com.example.ui.theme.MysticPrimaryHighlight
import com.example.ui.theme.MysticSuccess
import com.example.ui.theme.MysticTextPrimary
import com.example.ui.theme.MysticTextSecondary
import com.example.ui.theme.MysticTextTertiary
import com.example.ui.theme.MysticWarning
import com.xiu.lockagent.manager.ExecutionLogManager
import com.xiu.lockagent.service.XiuAccessibilityService
import com.xiu.lockagent.service.XiuFloatingService
import com.xiu.lockagent.utils.SecurePreferences

@Composable
fun SettingsProxyScreen() {
    val context = LocalContext.current
    val prefs = remember { SecurePreferences.getInstance(context) }

    var proxyUrlInput by remember { mutableStateOf(prefs.proxyUrl) }
    var apiKeyInput by remember { mutableStateOf(prefs.customApiKey) }
    var selectedModel by remember { mutableStateOf(prefs.selectedModel) }
    var fallbackXInput by remember { mutableStateOf(prefs.defaultFallbackX.toString()) }
    var fallbackYInput by remember { mutableStateOf(prefs.defaultFallbackY.toString()) }
    var isClipboardEnabled by remember { mutableStateOf(prefs.isClipboardMonitoringEnabled) }
    var is90HzEnabled by remember { mutableStateOf(prefs.is90HzMode) }
    var saveStatusMsg by remember { mutableStateOf<String?>(null) }

    val isOverlayGranted = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
    }
    val isAccessibilityActive by XiuAccessibilityService.isAccessibilityActive.collectAsState()

    var isMicGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isMicGranted = granted
    }

    val availableModels = listOf("gemini-3.5-flash", "groq/llama-3.3-70b", "openrouter/auto")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Column {
            Text(
                text = "PENGATURAN & PROXY MULTI-MODEL",
                color = MysticTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Konfigurasi Go Backend Proxy, Fallback Koordinat & Izin Sistem",
                color = MysticTextSecondary,
                fontSize = 11.sp
            )
        }

        // 1. Konfigurasi Go Backend Proxy & Model
        Card(
            colors = CardDefaults.cardColors(containerColor = MysticContainer),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = MysticPrimaryHighlight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Go Backend Proxy Endpoint", color = MysticTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = proxyUrlInput,
                    onValueChange = { proxyUrlInput = it },
                    label = { Text("Proxy URL") },
                    placeholder = { Text("http://10.0.2.2:8080") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_proxy_url_input")
                )

                Text("Pilihan Model AI:", color = MysticTextSecondary, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableModels.forEach { model ->
                        val isSelected = selectedModel == model
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedModel = model },
                            label = { Text(model.substringAfterLast('/'), fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MysticPrimary,
                                selectedLabelColor = MysticTextPrimary,
                                containerColor = MysticContainerElevated,
                                labelColor = MysticTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) MysticPrimaryHighlight else MysticBorder
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Gemini API Key (Opsional)") },
                    placeholder = { Text("Otomatis dari Secrets / Proxy jika kosong") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Fallback Coordinate Parameters (vivo Y18: 720x1612 px)
        Card(
            colors = CardDefaults.cardColors(containerColor = MysticContainer),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "KOORDINAT CADANGAN (FALLBACK GESTURE)",
                    color = MysticTextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Jika target node UI tidak ditemukan, sentuhan otomatis dikirim ke koordinat ini dengan stroke 100 ms.",
                    color = MysticTextSecondary,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fallbackXInput,
                        onValueChange = { fallbackXInput = it },
                        label = { Text("Fallback X (px)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fallback_x_input")
                    )
                    OutlinedTextField(
                        value = fallbackYInput,
                        onValueChange = { fallbackYInput = it },
                        label = { Text("Fallback Y (px)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fallback_y_input")
                    )
                }
            }
        }

        // 3. Toggles Kinerja & Papan Klip
        Card(
            colors = CardDefaults.cardColors(containerColor = MysticContainer),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pemantau Papan Klip (Clipboard)", color = MysticTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Deteksi nomor rekening, alamat, dan kata transaksi otomatis", color = MysticTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isClipboardEnabled,
                        onCheckedChange = { isClipboardEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MysticPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Optimasi 90 Hz vivo Helio G85", color = MysticTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Pertahankan render maskot pada frame rate tinggi", color = MysticTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = is90HzEnabled,
                        onCheckedChange = { is90HzEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MysticCyan)
                    )
                }
            }
        }

        // 4. Pusat Izin Sistem Android
        Card(
            colors = CardDefaults.cardColors(containerColor = MysticContainer),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PUSAT IZIN SISTEM ANDROID",
                    color = MysticTextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                PermissionRow(
                    title = "Izin Window Overlay (SYSTEM_ALERT_WINDOW)",
                    isGranted = isOverlayGranted,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                )

                PermissionRow(
                    title = "Layanan Aksesibilitas (Node Traversal & Gesture)",
                    isGranted = isAccessibilityActive,
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )

                PermissionRow(
                    title = "Perekaman Mikrofon (Speech Recognition id-ID)",
                    isGranted = isMicGranted,
                    onClick = {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )

                Text(
                    text = "Catatan Funtouch OS / OriginOS 6: Matikan penghemat baterai agresif untuk Xiu LockAgent di Pengaturan Baterai vivo.",
                    color = MysticTextTertiary,
                    fontSize = 10.sp
                )
            }
        }

        // Tombol Simpan
        Button(
            onClick = {
                prefs.proxyUrl = proxyUrlInput
                prefs.customApiKey = apiKeyInput
                prefs.selectedModel = selectedModel
                prefs.defaultFallbackX = fallbackXInput.toIntOrNull() ?: 360
                prefs.defaultFallbackY = fallbackYInput.toIntOrNull() ?: 720
                prefs.isClipboardMonitoringEnabled = isClipboardEnabled
                prefs.is90HzMode = is90HzEnabled

                saveStatusMsg = "Konfigurasi berhasil disimpan!"
                ExecutionLogManager.log("SETTINGS", "Pengaturan disimpan: Proxy=${proxyUrlInput}, Model=${selectedModel}")
            },
            colors = ButtonDefaults.buttonColors(containerColor = MysticPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_settings_btn")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Simpan Konfigurasi", fontWeight = FontWeight.Bold)
        }

        saveStatusMsg?.let {
            Text(
                text = it,
                color = MysticSuccess,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionRow(
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MysticContainerElevated)
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MysticTextPrimary, fontSize = 12.sp)
            Text(
                if (isGranted) "Sudah Diizinkan" else "Belum Diberikan (Ketuk untuk Buka)",
                color = if (isGranted) MysticSuccess else MysticWarning,
                fontSize = 10.sp
            )
        }
        Icon(
            imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Security,
            contentDescription = null,
            tint = if (isGranted) MysticSuccess else MysticWarning,
            modifier = Modifier.size(16.dp)
        )
    }
}
