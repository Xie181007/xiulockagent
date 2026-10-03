package com.xiu.lockagent.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.xiu.lockagent.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manajer Pengenalan Suara Native Android (Bahasa Indonesia id-ID)
 */
class SpeechRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    var onSpeechResultListener: ((String) -> Unit)? = null
    var onSpeechErrorListener: ((String) -> Unit)? = null

    fun initialize() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
            ExecutionLogManager.log(
                tag = "SPEECH_ENGINE",
                message = "Native SpeechRecognizer (id-ID) siap digunakan",
                level = LogLevel.INFO
            )
        } else {
            ExecutionLogManager.log(
                tag = "SPEECH_ENGINE",
                message = "Pengenalan suara sistem tidak didukung langsung di perangkat ini",
                level = LogLevel.WARNING
            )
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                ExecutionLogManager.log("SPEECH", "Siap mendengarkan suara Bahasa Indonesia...")
            }

            override fun onBeginningOfSpeech() {
                ExecutionLogManager.log("SPEECH", "Suara terdeteksi, mulai merekam...")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Normalisasi nilai dB (biasanya -2 hingga 10) ke rentang 0f - 1f
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _audioRmsLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
                _audioRmsLevel.value = 0f
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _audioRmsLevel.value = 0f
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Galat perekaman audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Galat klien speech"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon belum diberikan"
                    SpeechRecognizer.ERROR_NETWORK -> "Galat jaringan pengenalan suara"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Batas waktu jaringan habis"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada suara yang cocok atau tidak terdengar jelas"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Layanan pengenalan suara sedang sibuk"
                    SpeechRecognizer.ERROR_SERVER -> "Galat server speech"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tidak ada masukan suara terdeteksi"
                    else -> "Galat suara kode: $error"
                }
                ExecutionLogManager.log("SPEECH", errorMsg, level = LogLevel.WARNING)
                onSpeechErrorListener?.invoke(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _audioRmsLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim() ?: ""
                if (recognizedText.isNotEmpty()) {
                    _partialText.value = recognizedText
                    ExecutionLogManager.log(
                        tag = "SPEECH",
                        message = "Suara berhasil dikenali: \"$recognizedText\"",
                        level = LogLevel.SUCCESS
                    )
                    onSpeechResultListener?.invoke(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    _partialText.value = text
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            initialize()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
            val err = "Gagal memulai mic: ${e.message}"
            ExecutionLogManager.log("SPEECH", err, level = LogLevel.ERROR)
            onSpeechErrorListener?.invoke(err)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
        _audioRmsLevel.value = 0f
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
    }
}
