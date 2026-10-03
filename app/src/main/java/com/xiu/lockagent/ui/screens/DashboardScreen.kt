package com.xiu.lockagent.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.xiu.lockagent.manager.ClipboardWatcher
import com.xiu.lockagent.manager.DeviceHealthManager
import com.xiu.lockagent.model.DeviceHealthState
import com.xiu.lockagent.model.MascotPhase
import com.xiu.lockagent.service.XiuAccessibilityService
import com.xiu.lockagent.service.XiuFloatingService
import com.xiu.lockagent.ui.XiuMascot56Dp

@Composable
fun DashboardScreen(
    healthManager: DeviceHealthManager,
    clipboardWatcher: ClipboardWatcher,
    onNavigateToSimulation: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onTriggerInstruction: (String) -> Unit
) {
    val context = LocalContext.current
    val healthState by healthManager.healthState.collectAsState()
    val isOverlayActive by XiuFloatingService.isOverlayRunning.collectAsState()
    val isAccessibilityActive by XiuAccessibilityService.isAccessibilityActive.collectAsState()
    val currentPhase by XiuFloatingService.currentPhase.collectAsState()
    val lastBubbleMessage by XiuFloatingService.lastBubbleMessage.collectAsState()
    val lastDetectedClip by clipboardWatcher.lastDetectedClip.collectAsState()

    var customPromptInput by remember { mutableStateOf("") }

    val presetAutomations = listOf(
        "Transfer uang ke rekening BCA 5420918231",
        "Buka keranjang belanja dan checkout",
        "Bayar transaksi sekarang",
        "Ketik konfirmasi: Pesanan sudah diterima"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Header & Identitas Perangkat
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "XIU LOCKAGENT",
                        color = MysticTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Autonomous AI Agent • vivo Y18 (Helio G85)",
                        color = MysticTextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Quick Launch Floating Button
                Button(
                    onClick = {
                        toggleFloatingService(context, isOverlayActive)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverlayActive) MysticContainerElevated else MysticPrimary,
                        contentColor = if (isOverlayActive) MysticWarning else MysticTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("toggle_floating_service_btn")
                ) {
                    Icon(
                        imageVector = if (isOverlayActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Service",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverlayActive) "Stop Maskot" else "Mulai Maskot",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2. Status Layanan Sistem (Overlay & Aksesibilitas)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCard(
                    title = "Overlay Maskot",
                    status = if (isOverlayActive) "Aktif (56x56 dp)" else "Nonaktif",
                    isActive = isOverlayActive,
                    icon = Icons.Default.Layers,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            toggleFloatingService(context, isOverlayActive)
                        }
                    }
                )

                StatusCard(
                    title = "Aksesibilitas",
                    status = if (isAccessibilityActive) "Aktif (Traversal)" else "Perlu Izin",
                    isActive = isAccessibilityActive,
                    icon = Icons.Default.AccessibilityNew,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }
        }

        // 3. Maskot Live Preview & Quick Mic Control Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MysticContainer),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XiuMascot56Dp(
                        phase = currentPhase,
                        onClick = {
                            if (isOverlayActive) {
                                XiuFloatingService.triggerSpeechRecognition()
                            } else {
                                onNavigateToSimulation()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Status Maskot: ",
                                color = MysticTextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = currentPhase.name,
                                color = when (currentPhase) {
                                    MascotPhase.IDLE -> MysticPrimaryHighlight
                                    MascotPhase.MENDENGARKAN -> MysticCyan
                                    MascotPhase.MENGEKSEKUSI -> MysticWarning
                                    MascotPhase.SUKSES -> MysticSuccess
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (lastBubbleMessage.isNotEmpty()) "\"$lastBubbleMessage\"" else "Sentuh maskot untuk perintah suara atau tes simulasi.",
                            color = MysticTextPrimary,
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateToSimulation,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MysticPrimaryHighlight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("sandbox_preview_btn")
                    ) {
                        Text("Simulasi", fontSize = 12.sp)
                    }
                }
            }
        }

        // 4. Deteksi Clipboard Real-Time Card
        item {
            AnimatedVisibility(visible = lastDetectedClip != null) {
                lastDetectedClip?.let { clip ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MysticContainerElevated),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MysticCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Clipboard",
                                    tint = MysticCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Papan Klip Terdeteksi [${clip.type}]",
                                    color = MysticCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = clip.suggestionPrompt,
                                color = MysticTextPrimary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onTriggerInstruction("Isi ${clip.rawText} ke ${clip.targetIdentifier}")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MysticPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("execute_clipboard_action_btn")
                                ) {
                                    Text("Jalankan Otomasi", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { clipboardWatcher.clearLastClip() },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Tutup", fontSize = 11.sp, color = MysticTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Metrik Sensor Perangkat vivo Y18 (Helio G85 & Token Meter)
        item {
            Text(
                text = "KESEHATAN PERANGKAT & TOKEN METER",
                color = MysticTextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Suhu Baterai",
                    value = "${String.format(java.util.Locale.US, "%.1f", healthState.batteryTempCelsius)}°C",
                    subtext = "Optimal < 38°C",
                    icon = Icons.Default.Thermostat,
                    accentColor = if (healthState.batteryTempCelsius > 38f) MysticWarning else MysticSuccess,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "RAM Overlay",
                    value = "${healthState.overlayRamMb} MB",
                    subtext = "Batas < 10 MB (Aman)",
                    icon = Icons.Default.Memory,
                    accentColor = MysticPrimaryHighlight,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Frame Rate",
                    value = "${healthState.currentFps} FPS",
                    subtext = "Display 90 Hz",
                    icon = Icons.Default.Speed,
                    accentColor = MysticCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Akumulasi Token",
                    value = "${healthState.totalTokensAccumulated}",
                    subtext = "Sesi aktif saat ini",
                    icon = Icons.Default.Bolt,
                    accentColor = MysticPrimary,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Throughput Streaming",
                    value = "${healthState.streamingTokensPerSec} Tok/s",
                    subtext = healthState.cpuThermalStatus,
                    icon = Icons.Default.Speed,
                    accentColor = MysticPrimaryHighlight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 6. Preset Automasi Bahasa Indonesia
        item {
            Text(
                text = "PRESET INSTRUKSI CEPAT (id-ID)",
                color = MysticTextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                presetAutomations.forEach { preset ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MysticContainer),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTriggerInstruction(preset)
                            }
                            .testTag("preset_${preset.take(10)}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MysticPrimaryHighlight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = preset,
                                color = MysticTextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Kirim",
                                color = MysticPrimaryHighlight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    status: String,
    isActive: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MysticContainer),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) MysticSuccess.copy(alpha = 0.5f) else MysticBorder
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isActive) MysticSuccess else MysticTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = MysticTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = status,
                color = if (isActive) MysticSuccess else MysticTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MysticContainer),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    color = MysticTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = MysticTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtext,
                color = MysticTextTertiary,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

private fun toggleFloatingService(context: Context, isRunning: Boolean) {
    val intent = Intent(context, XiuFloatingService::class.java)
    if (isRunning) {
        context.stopService(intent)
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val permIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(permIntent)
        } else {
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
