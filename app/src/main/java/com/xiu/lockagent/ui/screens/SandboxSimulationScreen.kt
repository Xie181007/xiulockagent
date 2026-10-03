package com.xiu.lockagent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import com.xiu.lockagent.manager.ExecutionLogManager
import com.xiu.lockagent.model.AgentActionResponse
import com.xiu.lockagent.model.LogLevel
import com.xiu.lockagent.model.MascotPhase
import com.xiu.lockagent.ui.XiuMascot56Dp
import com.xiu.lockagent.ui.XiuSpeechBubble
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SandboxSimulationScreen(
    onTriggerAiAction: (AgentActionResponse) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    var mascotPhase by remember { mutableStateOf(MascotPhase.IDLE) }
    var audioRms by remember { mutableFloatStateOf(0.4f) }
    var bubbleText by remember { mutableStateOf("Xiu LockAgent siap diuji!") }
    var isBubbleVisible by remember { mutableStateOf(true) }

    // Anchor Drag-and-drop Maskot 56x56 dp
    var mascotOffsetX by remember { mutableFloatStateOf(40f) }
    var mascotOffsetY by remember { mutableFloatStateOf(80f) }

    // Selected Dummy App Tab (0: Bank, 1: Toko, 2: Chat)
    var selectedAppTab by remember { mutableIntStateOf(0) }

    // State Dummy Bank App
    var bankAccountInput by remember { mutableStateOf("") }
    var nominalInput by remember { mutableStateOf("") }
    var transferStatus by remember { mutableStateOf<String?>(null) }
    var isTransferNodeTargeted by remember { mutableStateOf(false) }

    // State Dummy Toko App
    var isCheckoutClicked by remember { mutableStateOf(false) }
    var isPaymentClicked by remember { mutableStateOf(false) }

    // State Dummy Chat App
    var chatMessageInput by remember { mutableStateOf("") }
    var lastSentMessage by remember { mutableStateOf<String?>(null) }

    // Fallback Coordinate Gesture Animation
    var fallbackMarkerPos by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    val gesturePulse = remember { Animatable(0f) }

    fun triggerFallbackMarker(x: Float, y: Float) {
        fallbackMarkerPos = Pair(x, y)
        coroutineScope.launch {
            gesturePulse.snapTo(0f)
            gesturePulse.animateTo(1f, tween(100, easing = FastOutSlowInEasing))
            delay(1200L)
            fallbackMarkerPos = null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticBackground)
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SANDBOX SIMULASI MASKOT & UI",
                        color = MysticTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Drag 56x56 dp maskot • Tes 4 fase • Uji Node & Fallback",
                        color = MysticTextSecondary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "X: ${mascotOffsetX.toInt()} | Y: ${mascotOffsetY.toInt()}",
                    color = MysticCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MysticContainerElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Kontrol Fase Maskot & Audio RMS Slider
            Card(
                colors = CardDefaults.cardColors(containerColor = MysticContainer),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "GANTI STATUS VISUAL (4 FASE)",
                        color = MysticTextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MascotPhase.entries.forEach { phase ->
                            val isSelected = mascotPhase == phase
                            Button(
                                onClick = {
                                    mascotPhase = phase
                                    isBubbleVisible = true
                                    bubbleText = when (phase) {
                                        MascotPhase.IDLE -> "Fase IDLE aktif"
                                        MascotPhase.MENDENGARKAN -> "Mendengarkan instruksi..."
                                        MascotPhase.MENGEKSEKUSI -> "Mengeksekusi otomasi UI..."
                                        MascotPhase.SUKSES -> "Aksi berhasil dieksekusi!"
                                    }
                                    ExecutionLogManager.log("SIMULASI", "Fase diubah ke: ${phase.name}")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MysticPrimary else MysticContainerElevated,
                                    contentColor = if (isSelected) MysticTextPrimary else MysticTextSecondary
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("phase_${phase.name}")
                            ) {
                                Text(phase.name.take(5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (mascotPhase == MascotPhase.MENDENGARKAN) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Audio RMS:", color = MysticTextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = audioRms,
                                onValueChange = { audioRms = it },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MysticCyan,
                                    activeTrackColor = MysticCyan
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text("${(audioRms * 100).toInt()}%", color = MysticCyan, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Tabs Aplikasi Pihak Ketiga Dummy (Untuk Tes Navigasi & Node Traversal)
            TabRow(
                selectedTabIndex = selectedAppTab,
                containerColor = MysticContainer,
                contentColor = MysticPrimaryHighlight,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedAppTab]),
                        color = MysticPrimaryHighlight
                    )
                }
            ) {
                Tab(
                    selected = selectedAppTab == 0,
                    onClick = { selectedAppTab = 0 },
                    text = { Text("Bank Mobile", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedAppTab == 1,
                    onClick = { selectedAppTab = 1 },
                    text = { Text("Toko Online", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedAppTab == 2,
                    onClick = { selectedAppTab = 2 },
                    text = { Text("Chat Pesan", fontSize = 12.sp) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Konten Layar Aplikasi Pihak Ketiga Dummy
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131316)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedAppTab) {
                        // 1. BANK MOBILE DUMMY
                        0 -> {
                            Text(
                                text = "BCA Mobile • Menu Transfer Antar Rekening",
                                color = MysticTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            OutlinedTextField(
                                value = bankAccountInput,
                                onValueChange = { bankAccountInput = it },
                                label = { Text("Nomor Rekening Tujuan") },
                                placeholder = { Text("Contoh: 5420918231") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        if (isTransferNodeTargeted) 2.dp else 0.dp,
                                        if (isTransferNodeTargeted) MysticCyan else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .testTag("sandbox_bank_account_input")
                            )

                            OutlinedTextField(
                                value = nominalInput,
                                onValueChange = { nominalInput = it },
                                label = { Text("Nominal Transfer (Rp)") },
                                placeholder = { Text("100.000") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    transferStatus = "Transfer Rp $nominalInput ke $bankAccountInput Berhasil!"
                                    ExecutionLogManager.log("SANDBOX_APP", "Tombol 'Transfer Sekarang' ditekan")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MysticPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sandbox_btn_transfer_sekarang")
                            ) {
                                Text("Transfer Sekarang", fontWeight = FontWeight.Bold)
                            }

                            transferStatus?.let {
                                Text(it, color = MysticSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Tombol Automasi AI Sandbox
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            mascotPhase = MascotPhase.MENGEKSEKUSI
                                            bubbleText = "Mencari node 'Nomor Rekening'..."
                                            isBubbleVisible = true
                                            isTransferNodeTargeted = true
                                            delay(700L)
                                            bankAccountInput = "5420918231"
                                            nominalInput = "250.000"
                                            bubbleText = "Mengisi nomor rekening & nominal..."
                                            delay(800L)
                                            bubbleText = "Mengeklik 'Transfer Sekarang'..."
                                            transferStatus = "Transfer Rp 250.000 ke 5420918231 Berhasil!"
                                            mascotPhase = MascotPhase.SUKSES
                                            delay(500L)
                                            isTransferNodeTargeted = false
                                            ExecutionLogManager.log(
                                                tag = "AUTO_ENGINE",
                                                message = "Automasi transfer bank berhasil dieksekusi",
                                                level = LogLevel.SUCCESS,
                                                latencyMs = 210,
                                                tokensUsed = 52
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MysticContainerElevated),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simulasi Isi & Klik", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        // Uji Fallback Koordinat 100 ms
                                        triggerFallbackMarker(maxWidthPx * 0.5f, maxHeightPx * 0.65f)
                                        mascotPhase = MascotPhase.MENGEKSEKUSI
                                        bubbleText = "Fallback gestur dikirim ke (360, 720) 100ms"
                                        isBubbleVisible = true
                                        ExecutionLogManager.log(
                                            tag = "FALLBACK_TEST",
                                            message = "Fallback dispatchGesture(360, 720) 100ms dipicu",
                                            level = LogLevel.INFO
                                        )
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MysticWarning),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MysticWarning, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tes Fallback 100ms", fontSize = 11.sp, color = MysticWarning)
                                }
                            }
                        }

                        // 2. TOKO ONLINE DUMMY
                        1 -> {
                            Text(
                                text = "Marketplace • Keranjang Belanja",
                                color = MysticTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MysticContainerElevated)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Headset Wireless Hi-Fi 90Hz", color = MysticTextPrimary, fontSize = 13.sp)
                                    Text("Rp 349.000", color = MysticCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Qty: 1", color = MysticTextSecondary, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { isCheckoutClicked = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCheckoutClicked) MysticSuccess else MysticPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sandbox_btn_checkout")
                            ) {
                                Text(if (isCheckoutClicked) "Checkout Selesai" else "Checkout (1 Barang)")
                            }

                            if (isCheckoutClicked) {
                                Button(
                                    onClick = { isPaymentClicked = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MysticCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sandbox_btn_bayar_sekarang")
                                ) {
                                    Text(if (isPaymentClicked) "Pembayaran Dikonfirmasi" else "Bayar Sekarang")
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        mascotPhase = MascotPhase.MENGEKSEKUSI
                                        bubbleText = "Mengeklik 'Checkout'..."
                                        isBubbleVisible = true
                                        delay(600L)
                                        isCheckoutClicked = true
                                        delay(700L)
                                        bubbleText = "Mengeklik 'Bayar Sekarang'..."
                                        isPaymentClicked = true
                                        mascotPhase = MascotPhase.SUKSES
                                        bubbleText = "Transaksi berhasil diselesaikan!"
                                        ExecutionLogManager.log("AUTO_ENGINE", "Automasi belanja sukses", level = LogLevel.SUCCESS)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MysticPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Simulasi Otomasi Checkout", fontSize = 12.sp)
                            }
                        }

                        // 3. CHAT PESAN DUMMY
                        2 -> {
                            Text(
                                text = "WhatsApp / Pesan Cepat",
                                color = MysticTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            lastSentMessage?.let {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MysticPrimary)
                                        .padding(8.dp)
                                ) {
                                    Text(it, color = MysticTextPrimary, fontSize = 12.sp)
                                }
                            }

                            OutlinedTextField(
                                value = chatMessageInput,
                                onValueChange = { chatMessageInput = it },
                                label = { Text("Kolom Pesan") },
                                placeholder = { Text("Ketik pesan Anda...") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (chatMessageInput.isNotEmpty()) {
                                        lastSentMessage = chatMessageInput
                                        chatMessageInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MysticPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Kirim")
                            }

                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        mascotPhase = MascotPhase.MENGEKSEKUSI
                                        bubbleText = "Mengisi teks ke Kolom Pesan..."
                                        isBubbleVisible = true
                                        delay(500L)
                                        chatMessageInput = "Halo! Konfirmasi pembayaran sudah diterima. Terima kasih."
                                        delay(600L)
                                        bubbleText = "Mengeklik tombol 'Kirim'..."
                                        lastSentMessage = chatMessageInput
                                        chatMessageInput = ""
                                        mascotPhase = MascotPhase.SUKSES
                                        bubbleText = "Pesan berhasil dikirim!"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MysticContainerElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Simulasi Ketik & Kirim Otomatis", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- MASKOT 56x56 dp TERAPUNG DENGAN DRAG-AND-DROP DI ATAS LAYAR SANDBOX ---
        Box(
            modifier = Modifier
                .offset { IntOffset(mascotOffsetX.roundToInt(), mascotOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        mascotOffsetX = (mascotOffsetX + dragAmount.x).coerceIn(0f, maxWidthPx - 160f)
                        mascotOffsetY = (mascotOffsetY + dragAmount.y).coerceIn(0f, maxHeightPx - 160f)
                    }
                }
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                // Speech Bubble Pop-up 14dp radius, 1dp border, auto-dismiss 3000ms
                if (isBubbleVisible && bubbleText.isNotBlank()) {
                    XiuSpeechBubble(
                        message = bubbleText,
                        phase = mascotPhase,
                        onDismiss = { isBubbleVisible = false },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Maskot Tepat 56x56 dp
                XiuMascot56Dp(
                    phase = mascotPhase,
                    audioRms = audioRms,
                    onClick = {
                        isBubbleVisible = true
                        mascotPhase = when (mascotPhase) {
                            MascotPhase.IDLE -> MascotPhase.MENDENGARKAN
                            MascotPhase.MENDENGARKAN -> MascotPhase.MENGEKSEKUSI
                            MascotPhase.MENGEKSEKUSI -> MascotPhase.SUKSES
                            MascotPhase.SUKSES -> MascotPhase.IDLE
                        }
                    }
                )
            }
        }

        // --- Fallback Coordinate Dispatch Indicator (Target Marker 100ms) ---
        fallbackMarkerPos?.let { pos ->
            Box(
                modifier = Modifier
                    .offset { IntOffset((pos.first - 24).roundToInt(), (pos.second - 24).roundToInt()) }
                    .size(48.dp)
                    .scale(1f + gesturePulse.value * 0.8f)
                    .clip(CircleShape)
                    .border(2.dp, MysticWarning, CircleShape)
                    .background(MysticWarning.copy(alpha = 0.25f * (1f - gesturePulse.value))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Fallback Coordinate",
                    tint = MysticWarning,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
