package com.xiu.lockagent.manager

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import com.xiu.lockagent.model.LogLevel
import com.xiu.lockagent.utils.SecurePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern

/**
 * Pemantau Papan Klip (Clipboard Listener) Otomatis
 * Mendeteksi nomor rekening, alamat, dan teks transaksi secara real-time.
 */
class ClipboardWatcher(private val context: Context) {

    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val prefs = SecurePreferences.getInstance(context)

    private val _lastDetectedClip = MutableStateFlow<DetectedClipData?>(null)
    val lastDetectedClip: StateFlow<DetectedClipData?> = _lastDetectedClip.asStateFlow()

    private var clipListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    var onClipDetected: ((DetectedClipData) -> Unit)? = null

    data class DetectedClipData(
        val rawText: String,
        val type: ClipType,
        val suggestionPrompt: String,
        val targetIdentifier: String,
        val detectedAt: Long = System.currentTimeMillis()
    )

    enum class ClipType {
        BANK_ACCOUNT,
        ADDRESS,
        TRANSACTION_KEYWORD,
        GENERAL_TEXT
    }

    fun startListening() {
        if (!prefs.isClipboardMonitoringEnabled) return
        if (clipListener != null) return

        clipListener = ClipboardManager.OnPrimaryClipChangedListener {
            checkClipboard()
        }
        clipboardManager.addPrimaryClipChangedListener(clipListener)
        ExecutionLogManager.log(
            tag = "CLIPBOARD",
            message = "Pemantau papan klip aktif",
            level = LogLevel.INFO
        )
    }

    fun checkClipboard() {
        if (!clipboardManager.hasPrimaryClip()) return
        val clipData = clipboardManager.primaryClip ?: return
        if (clipData.itemCount <= 0) return

        val text = clipData.getItemAt(0).text?.toString()?.trim() ?: return
        if (text.isEmpty() || text == _lastDetectedClip.value?.rawText) return

        analyzeClipContent(text)
    }

    private fun analyzeClipContent(text: String) {
        val rekeningPattern = Pattern.compile("\\b(\\d{8,16})\\b")
        val rekeningMatcher = rekeningPattern.matcher(text)

        val detected = when {
            rekeningMatcher.find() -> {
                val accountNumber = rekeningMatcher.group(1) ?: text
                DetectedClipData(
                    rawText = text,
                    type = ClipType.BANK_ACCOUNT,
                    suggestionPrompt = "Deteksi rekening $accountNumber. Kirim uang sekarang?",
                    targetIdentifier = "Nomor Rekening"
                )
            }
            text.contains(Regex("(?i)\\b(jl\\.|jalan|kelurahan|kecamatan|rt\\s*\\d|rw\\s*\\d|kode\\s*pos)\\b")) -> {
                DetectedClipData(
                    rawText = text,
                    type = ClipType.ADDRESS,
                    suggestionPrompt = "Deteksi format alamat. Isi ke formulir pengiriman?",
                    targetIdentifier = "Alamat Pengiriman"
                )
            }
            text.contains(Regex("(?i)\\b(bayar|transfer|checkout|invoice|tagihan|pesanan)\\b")) -> {
                DetectedClipData(
                    rawText = text,
                    type = ClipType.TRANSACTION_KEYWORD,
                    suggestionPrompt = "Deteksi kata transaksi. Buka otomatisasi pembayaran?",
                    targetIdentifier = "Tombol Pembayaran"
                )
            }
            else -> {
                DetectedClipData(
                    rawText = text,
                    type = ClipType.GENERAL_TEXT,
                    suggestionPrompt = "Teks tersalin: \"${text.take(30)}...\". Isi otomatis?",
                    targetIdentifier = "Kolom Teks"
                )
            }
        }

        _lastDetectedClip.value = detected
        ExecutionLogManager.log(
            tag = "CLIPBOARD",
            message = "Teks disalin terdeteksi: [${detected.type}]",
            details = "\"${text.take(50)}\"",
            level = LogLevel.SUCCESS
        )
        onClipDetected?.invoke(detected)
    }

    fun clearLastClip() {
        _lastDetectedClip.value = null
    }

    fun stopListening() {
        clipListener?.let {
            clipboardManager.removePrimaryClipChangedListener(it)
            clipListener = null
        }
    }
}
