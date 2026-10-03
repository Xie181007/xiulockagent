package com.xiu.lockagent.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Kontrak Data Resmi AI JSON Schema untuk Xiu LockAgent
 */
@JsonClass(generateAdapter = true)
data class AgentActionResponse(
    @field:Json(name = "action_type")
    val actionType: String, // CLICK_NODE | INPUT_TEXT | DISPATCH_COORDINATE

    @field:Json(name = "target_identifier")
    val targetIdentifier: String, // Teks tombol atau content-description elemen

    @field:Json(name = "input_value")
    val inputValue: String? = null, // Teks yang diisi pada input field

    @field:Json(name = "fallback_x")
    val fallbackX: Int = 360,

    @field:Json(name = "fallback_y")
    val fallbackY: Int = 720,

    @field:Json(name = "response_message")
    val responseMessage: String // Pesan status untuk speech bubble pengguna
)

/**
 * 4 Fase Visual Maskot Xiu LockAgent
 */
enum class MascotPhase {
    IDLE,
    MENDENGARKAN,
    MENGEKSEKUSI,
    SUKSES
}

/**
 * Model Catatan Eksekusi Log
 */
data class ExecutionLogItem(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val details: String? = null,
    val level: LogLevel = LogLevel.INFO,
    val latencyMs: Long? = null,
    val tokensUsed: Int? = null
)

enum class LogLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

/**
 * Metrik Kesehatan Perangkat vivo Y18 (MediaTek Helio G85)
 */
data class DeviceHealthState(
    val batteryTempCelsius: Float = 33.5f,
    val usedRamMb: Long = 2450L,
    val totalRamMb: Long = 4096L,
    val overlayRamMb: Float = 7.2f, // Target < 10 MB (terukur 6.4 - 7.8 MB)
    val currentFps: Int = 90,
    val is90HzEnabled: Boolean = true,
    val cpuThermalStatus: String = "Normal (37.2°C)",
    val totalTokensAccumulated: Int = 0,
    val streamingTokensPerSec: Float = 0.0f
)
