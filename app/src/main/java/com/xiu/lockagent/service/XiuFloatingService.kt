package com.xiu.lockagent.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.example.MainActivity
import com.example.R
import com.xiu.lockagent.manager.ClipboardWatcher
import com.xiu.lockagent.manager.ExecutionLogManager
import com.xiu.lockagent.manager.SpeechRecognitionManager
import com.xiu.lockagent.model.AgentActionResponse
import com.xiu.lockagent.model.LogLevel
import com.xiu.lockagent.model.MascotPhase
import com.xiu.lockagent.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * XiuFloatingService: Window Overlay Manager untuk Maskot Mengambang 56x56 dp
 * dengan Lottie Animation Engine ringan (@raw/xiu_mascot_3d.json) untuk status
 * IDLE dan ACTIVE, lengkap dengan logika relokasi drag-and-drop & magnetic edge snapping.
 */
class XiuFloatingService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayRootView: View? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private lateinit var apiClient: ApiClient
    private lateinit var speechManager: SpeechRecognitionManager
    private lateinit var clipboardWatcher: ClipboardWatcher

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mascotContainer: FrameLayout? = null
    private var lottieMascotView: LottieAnimationView? = null
    private var speechBubbleView: LinearLayout? = null
    private var speechBubbleText: TextView? = null

    private var dismissRunnable: Runnable? = null
    private var snapAnimator: ValueAnimator? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        apiClient = ApiClient(this)
        speechManager = SpeechRecognitionManager(this)
        clipboardWatcher = ClipboardWatcher(this)

        startForegroundServiceWithNotification()
        initSpeechAndClipboard()
        initOverlayWindow()

        _isOverlayRunning.value = true
        ExecutionLogManager.log(
            tag = "FLOATING_SERVICE",
            message = "Floating Mascot Service (56x56 dp) aktif dengan Lottie 3D Engine",
            details = "Animasi Lottie (@raw/xiu_mascot_3d) siap melayani fase IDLE & ACTIVE. Konsumsi RAM < 10 MB.",
            level = LogLevel.SUCCESS
        )
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "xiu_lockagent_overlay_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Xiu LockAgent Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Layanan aktif maskot otonom AI"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_desc))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun initSpeechAndClipboard() {
        speechManager.initialize()

        // Respons akustik real-time modulasi Lottie speed
        serviceScope.launch {
            speechManager.audioRmsLevel.collect { rms ->
                if (_currentPhase.value == MascotPhase.MENDENGARKAN) {
                    mainHandler.post {
                        lottieMascotView?.speed = 1.0f + (rms * 1.5f)
                    }
                }
            }
        }

        speechManager.onSpeechResultListener = { recognizedText ->
            showSpeechBubble(recognizedText, MascotPhase.MENGEKSEKUSI)
            processUserInstruction(recognizedText)
        }
        speechManager.onSpeechErrorListener = { errorMsg ->
            showSpeechBubble(errorMsg, MascotPhase.IDLE)
            updateMascotVisualPhase(MascotPhase.IDLE)
        }

        clipboardWatcher.onClipDetected = { clipData ->
            showSpeechBubble(clipData.suggestionPrompt, MascotPhase.IDLE)
        }
        clipboardWatcher.startListening()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val mascotSizePx = (56 * density).toInt()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * density).toInt()
            y = (200 * density).toInt()
        }

        // Tata Letak Root Overlay
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }

        // 1. Pop-up Speech Bubble (@drawable/bg_popup_card, radius 14dp, border 1dp)
        speechBubbleView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = ContextCompat.getDrawable(this@XiuFloatingService, R.drawable.bg_popup_card)
            val padH = (14 * density).toInt()
            val padV = (10 * density).toInt()
            setPadding(padH, padV, padH, padV)
            visibility = View.GONE
            setOnClickListener {
                visibility = View.GONE
            }
        }

        speechBubbleText = TextView(this).apply {
            setTextColor(Color.parseColor("#FAFAFA"))
            textSize = 13f
            maxWidth = (240 * density).toInt()
            text = "Xiu LockAgent siap!"
        }
        speechBubbleView?.addView(speechBubbleText)

        // 2. Kontainer Maskot Tepat 56x56 dp
        mascotContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(mascotSizePx, mascotSizePx).apply {
                topMargin = (6 * density).toInt()
            }
            background = createMascotBackgroundDrawable(MascotPhase.IDLE)
            clipToOutline = true
        }

        // 3. Lottie Animation Engine Maskot 3D (@raw/xiu_mascot_3d.json)
        lottieMascotView = LottieAnimationView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
            setAnimation(R.raw.xiu_mascot_3d)
            repeatCount = LottieDrawable.INFINITE
            scaleType = ImageView.ScaleType.CENTER_CROP
            enableMergePathsForKitKatAndAbove(true)
        }
        mascotContainer?.addView(lottieMascotView)

        rootLayout.addView(speechBubbleView)
        rootLayout.addView(mascotContainer)
        overlayRootView = rootLayout

        // Setup Drag-and-Drop Relocation dengan Magnetic Snapping
        setupDragAndDropGesture(mascotContainer!!)

        // Mulai animasi IDLE
        updateMascotVisualPhase(MascotPhase.IDLE)

        try {
            windowManager?.addView(overlayRootView, windowLayoutParams)
            showSpeechBubble("Xiu LockAgent siap mendengarkan...", MascotPhase.IDLE)
        } catch (e: Exception) {
            ExecutionLogManager.log(
                tag = "FLOATING_SERVICE",
                message = "Gagal memasang overlay window: ${e.message}",
                level = LogLevel.ERROR
            )
        }
    }

    /**
     * Logika Gestur Drag-and-Drop untuk Relokasi Anchor (X, Y) dengan Boundary Check
     * dan Magnetic Edge Snapping.
     */
    private fun setupDragAndDropGesture(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        view.setOnTouchListener { _, event ->
            val params = windowLayoutParams ?: return@setOnTouchListener false
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val mascotWidth = view.width.coerceAtLeast((56 * metrics.density).toInt())

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        isDragging = true
                    }

                    if (isDragging) {
                        // Relokasi bebas (X, Y) dengan batas layar aman
                        val newX = (initialX + dx).coerceIn(0, screenWidth - mascotWidth)
                        val newY = (initialY + dy).coerceIn(40, screenHeight - mascotWidth - 60)
                        params.x = newX
                        params.y = newY
                        windowManager?.updateViewLayout(overlayRootView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Ketukan biasa: Toggle Speech Recognition
                        onMascotClicked()
                    } else {
                        // Relokasi selesai: Magnetic Edge Snapping ke sisi terdekat
                        val currentX = params.x
                        val marginHorizontal = (12 * metrics.density).toInt()
                        val targetX = if (currentX + mascotWidth / 2 < screenWidth / 2) {
                            marginHorizontal
                        } else {
                            screenWidth - mascotWidth - marginHorizontal
                        }
                        animateSnapToEdge(currentX, targetX, params)
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Animasi Magnetic Edge Snapping yang halus ke sisi layar terdekat
     */
    private fun animateSnapToEdge(fromX: Int, toX: Int, params: WindowManager.LayoutParams) {
        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofInt(fromX, toX).apply {
            duration = 240L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                params.x = animator.animatedValue as Int
                windowManager?.updateViewLayout(overlayRootView, params)
            }
            start()
        }
    }

    private fun onMascotClicked() {
        if (speechManager.isListening.value) {
            speechManager.stopListening()
            updateMascotVisualPhase(MascotPhase.IDLE)
            showSpeechBubble("Mendengarkan dibatalkan", MascotPhase.IDLE)
        } else {
            updateMascotVisualPhase(MascotPhase.MENDENGARKAN)
            showSpeechBubble("Mendengarkan suara...", MascotPhase.MENDENGARKAN)
            speechManager.startListening()
        }
    }

    /**
     * Memperbarui fase visual maskot dengan Lottie Animation Engine:
     * - IDLE: Hovering & breathing scale (frames 0 - 60)
     * - MENDENGARKAN: Active listening cyan ripples (frames 60 - 120)
     * - MENGEKSEKUSI: Spinning cyber runes (frames 120 - 180)
     * - SUKSES: Victory check flare
     */
    fun updateMascotVisualPhase(phase: MascotPhase) {
        _currentPhase.value = phase
        mainHandler.post {
            mascotContainer?.background = createMascotBackgroundDrawable(phase)
            lottieMascotView?.let { lottie ->
                when (phase) {
                    MascotPhase.IDLE -> {
                        lottie.setMinAndMaxFrame(0, 60)
                        lottie.speed = 0.9f
                        lottie.repeatCount = LottieDrawable.INFINITE
                        if (!lottie.isAnimating) lottie.playAnimation()
                    }
                    MascotPhase.MENDENGARKAN -> {
                        lottie.setMinAndMaxFrame(60, 120)
                        lottie.speed = 1.3f
                        lottie.repeatCount = LottieDrawable.INFINITE
                        lottie.playAnimation()
                    }
                    MascotPhase.MENGEKSEKUSI -> {
                        lottie.setMinAndMaxFrame(120, 180)
                        lottie.speed = 1.8f
                        lottie.repeatCount = LottieDrawable.INFINITE
                        lottie.playAnimation()
                    }
                    MascotPhase.SUKSES -> {
                        lottie.setMinAndMaxFrame(60, 180)
                        lottie.speed = 2.0f
                        lottie.repeatCount = 1
                        lottie.playAnimation()
                    }
                }
            }
        }
    }

    private fun createMascotBackgroundDrawable(phase: MascotPhase): GradientDrawable {
        val strokeColor = when (phase) {
            MascotPhase.IDLE -> Color.parseColor("#3F3F46")
            MascotPhase.MENDENGARKAN -> Color.parseColor("#06B6D4")
            MascotPhase.MENGEKSEKUSI -> Color.parseColor("#818CF8")
            MascotPhase.SUKSES -> Color.parseColor("#10B981")
        }
        val density = resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#18181B"))
            setStroke((2 * density).toInt(), strokeColor)
        }
    }

    /**
     * Menampilkan Pop-up AI Speech Bubble dengan auto-dismiss timer 3000 ms
     */
    fun showSpeechBubble(message: String, phase: MascotPhase = _currentPhase.value) {
        _lastBubbleMessage.value = message
        updateMascotVisualPhase(phase)

        mainHandler.post {
            speechBubbleText?.text = message
            speechBubbleView?.visibility = View.VISIBLE

            // Batalkan timer sebelumnya jika ada
            dismissRunnable?.let { mainHandler.removeCallbacks(it) }

            // Auto-dismiss timer 3000 ms sesuai PRD
            dismissRunnable = Runnable {
                speechBubbleView?.visibility = View.GONE
            }.also {
                mainHandler.postDelayed(it, 3000L)
            }
        }
    }

    /**
     * Memproses instruksi dan memanggil Accessibility Service untuk otomasi UI
     */
    fun processUserInstruction(instruction: String) {
        updateMascotVisualPhase(MascotPhase.MENGEKSEKUSI)
        serviceScope.launch {
            val result = apiClient.processInstruction(instruction)
            if (result.isSuccess) {
                val action = result.getOrThrow()
                showSpeechBubble(action.responseMessage, MascotPhase.MENGEKSEKUSI)

                // Eksekusi via Accessibility Service
                XiuAccessibilityService.execute(action) { success, msg ->
                    if (success) {
                        updateMascotVisualPhase(MascotPhase.SUKSES)
                        showSpeechBubble(msg, MascotPhase.SUKSES)
                        mainHandler.postDelayed({
                            updateMascotVisualPhase(MascotPhase.IDLE)
                        }, 2500L)
                    } else {
                        updateMascotVisualPhase(MascotPhase.IDLE)
                        showSpeechBubble("Gagal: $msg", MascotPhase.IDLE)
                    }
                }
            } else {
                updateMascotVisualPhase(MascotPhase.IDLE)
                showSpeechBubble("Galat AI: ${result.exceptionOrNull()?.message}", MascotPhase.IDLE)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isOverlayRunning.value = false
        speechManager.destroy()
        clipboardWatcher.stopListening()

        snapAnimator?.cancel()
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }

        lottieMascotView?.cancelAnimation()

        if (overlayRootView != null) {
            try {
                windowManager?.removeView(overlayRootView)
            } catch (_: Exception) {}
            overlayRootView = null
        }
        ExecutionLogManager.log(
            tag = "FLOATING_SERVICE",
            message = "Floating Mascot Service dihentikan",
            level = LogLevel.INFO
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 8801

        @Volatile
        private var instance: XiuFloatingService? = null

        private val _isOverlayRunning = MutableStateFlow(false)
        val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

        private val _currentPhase = MutableStateFlow(MascotPhase.IDLE)
        val currentPhase: StateFlow<MascotPhase> = _currentPhase.asStateFlow()

        private val _lastBubbleMessage = MutableStateFlow("")
        val lastBubbleMessage: StateFlow<String> = _lastBubbleMessage.asStateFlow()

        fun isRunning(): Boolean = instance != null

        fun triggerSpeechRecognition() {
            instance?.onMascotClicked()
        }

        fun sendInstruction(instruction: String) {
            instance?.processUserInstruction(instruction)
        }

        fun updatePhase(phase: MascotPhase) {
            instance?.updateMascotVisualPhase(phase)
        }

        fun displayBubble(message: String, phase: MascotPhase = MascotPhase.IDLE) {
            instance?.showSpeechBubble(message, phase)
        }
    }
}
