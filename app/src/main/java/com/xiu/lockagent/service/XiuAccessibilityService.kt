package com.xiu.lockagent.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.xiu.lockagent.manager.ExecutionLogManager
import com.xiu.lockagent.model.AgentActionResponse
import com.xiu.lockagent.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * XiuAccessibilityService: Mesin Otomasi Navigasi Layar Android
 * Mendukung pencarian rekursif AccessibilityNodeInfo dan fallback dispatchGesture (100 ms).
 */
class XiuAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isAccessibilityActive.value = true
        ExecutionLogManager.log(
            tag = "ACCESSIBILITY",
            message = "Layanan Aksesibilitas Xiu LockAgent terhubung",
            details = "Siap melakukan traversal node hierarki dan emulasi gestur sentuh",
            level = LogLevel.SUCCESS
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Pemantauan event perubahan jendela atau interaksi layar
        if (event == null) return
        val packageName = event.packageName?.toString() ?: ""
        if (packageName.isNotEmpty() && !packageName.contains("xiu.lockagent")) {
            _currentForegroundPackage.value = packageName
        }
    }

    override fun onInterrupt() {
        ExecutionLogManager.log(
            tag = "ACCESSIBILITY",
            message = "Layanan Aksesibilitas diinterupsi oleh sistem",
            level = LogLevel.WARNING
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isAccessibilityActive.value = false
        }
        ExecutionLogManager.log(
            tag = "ACCESSIBILITY",
            message = "Layanan Aksesibilitas dinonaktifkan",
            level = LogLevel.INFO
        )
    }

    /**
     * Mengeksekusi aksi dari kontrak AI: CLICK_NODE, INPUT_TEXT, atau DISPATCH_COORDINATE
     */
    fun performAgentAction(action: AgentActionResponse, callback: ((Boolean, String) -> Unit)? = null) {
        val rootNode = rootInActiveWindow
        val target = action.targetIdentifier.trim()
        val actionType = action.actionType.uppercase()

        ExecutionLogManager.log(
            tag = "AUTO_ENGINE",
            message = "Mengeksekusi aksi: $actionType",
            details = "Target: '$target', Nilai: '${action.inputValue ?: "-"}', Fallback: (${action.fallbackX}, ${action.fallbackY})"
        )

        // Jika instruksi eksplisit adalah koordinat sentuh langsung
        if (actionType == "DISPATCH_COORDINATE") {
            dispatchFallbackGesture(action.fallbackX.toFloat(), action.fallbackY.toFloat()) { success ->
                val msg = if (success) "Sentuhan koordinat (${action.fallbackX}, ${action.fallbackY}) berhasil" else "Gagal menyentuh koordinat"
                callback?.invoke(success, msg)
            }
            return
        }

        // Pencarian rekursif node elemen UI pada layar aktif
        if (rootNode != null && target.isNotEmpty()) {
            val matchedNode = findNodeRecursively(rootNode, target)
            if (matchedNode != null) {
                val nodeBounds = Rect().apply { matchedNode.getBoundsInScreen(this) }
                ExecutionLogManager.log(
                    tag = "NODE_TRAVERSAL",
                    message = "Node cocok ditemukan: '${matchedNode.text ?: matchedNode.contentDescription}'",
                    details = "Bounds: [${nodeBounds.left}, ${nodeBounds.top}, ${nodeBounds.right}, ${nodeBounds.bottom}]",
                    level = LogLevel.SUCCESS
                )

                if (actionType == "INPUT_TEXT" && action.inputValue != null) {
                    val inputSuccess = executeSetText(matchedNode, action.inputValue)
                    if (inputSuccess) {
                        callback?.invoke(true, "Teks '${action.inputValue}' berhasil diisikan ke node")
                        return
                    }
                } else {
                    val clickSuccess = executeClick(matchedNode)
                    if (clickSuccess) {
                        callback?.invoke(true, "Klik pada elemen '$target' berhasil dieksekusi")
                        return
                    }
                }
            } else {
                ExecutionLogManager.log(
                    tag = "NODE_TRAVERSAL",
                    message = "Node '$target' tidak ditemukan di hierarki layar",
                    details = "Meluncurkan mekanisme fallback dispatchGesture...",
                    level = LogLevel.WARNING
                )
            }
        } else {
            ExecutionLogManager.log(
                tag = "NODE_TRAVERSAL",
                message = "Jendela aktif tidak dapat diakses atau node root null",
                details = "Meluncurkan mekanisme fallback dispatchGesture...",
                level = LogLevel.WARNING
            )
        }

        // --- Mekanisme Fallback Otomatis ---
        // Jika elemen node terproteksi atau tidak ditemukan, otomatis meluncurkan dispatchGesture()
        dispatchFallbackGesture(action.fallbackX.toFloat(), action.fallbackY.toFloat()) { success ->
            val msg = if (success) {
                "Fallback sentuhan ke (${action.fallbackX}, ${action.fallbackY}) dieksekusi (durasi 100ms)"
            } else {
                "Mekanisme fallback gagal mengeksekusi gestur"
            }
            callback?.invoke(success, msg)
        }
    }

    /**
     * Pencarian rekursif hierarki node UI (teks, content description, atau view ID)
     */
    private fun findNodeRecursively(node: AccessibilityNodeInfo, targetQuery: String): AccessibilityNodeInfo? {
        val queryLower = targetQuery.lowercase()

        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if (text.contains(queryLower) || contentDesc.contains(queryLower) || (viewId.isNotEmpty() && viewId.contains(queryLower))) {
            return node
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeRecursively(child, targetQuery)
            if (found != null) {
                return found
            }
        }
        return null
    }

    /**
     * Eksekusi klik otomatis pada node atau parent yang clickable
     */
    private fun executeClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                val clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    ExecutionLogManager.log(
                        tag = "ACTION_CLICK",
                        message = "ACTION_CLICK berhasil dilakukan pada node",
                        level = LogLevel.SUCCESS
                    )
                    return true
                }
            }
            current = current.parent
        }
        // Jika parent tidak clickable, coba klik node langsung
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * Eksekusi pengisian teks otomatis ke kolom input
     */
    private fun executeSetText(node: AccessibilityNodeInfo, text: String): Boolean {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        if (success) {
            ExecutionLogManager.log(
                tag = "ACTION_SET_TEXT",
                message = "ACTION_SET_TEXT berhasil mengisi: \"$text\"",
                level = LogLevel.SUCCESS
            )
        } else {
            ExecutionLogManager.log(
                tag = "ACTION_SET_TEXT",
                message = "Gagal melakukan ACTION_SET_TEXT, node mungkin tidak dapat diedit",
                level = LogLevel.WARNING
            )
        }
        return success
    }

    /**
     * Fallback Mechanism: dispatchGesture() ke koordinat (fallback_x, fallback_y) stroke duration 100 ms
     */
    fun dispatchFallbackGesture(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 100L)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                ExecutionLogManager.log(
                    tag = "GESTURE_FALLBACK",
                    message = "dispatchGesture sukses pada koordinat (${x.toInt()}, ${y.toInt()})",
                    details = "Durasi stroke: 100 ms",
                    level = LogLevel.SUCCESS
                )
                onComplete?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                ExecutionLogManager.log(
                    tag = "GESTURE_FALLBACK",
                    message = "dispatchGesture dibatalkan oleh sistem pada (${x.toInt()}, ${y.toInt()})",
                    level = LogLevel.WARNING
                )
                onComplete?.invoke(false)
            }
        }, null)

        if (!dispatched) {
            ExecutionLogManager.log(
                tag = "GESTURE_FALLBACK",
                message = "dispatchGesture ditolak oleh OS",
                level = LogLevel.ERROR
            )
            onComplete?.invoke(false)
        }
    }

    companion object {
        @Volatile
        private var instance: XiuAccessibilityService? = null

        private val _isAccessibilityActive = MutableStateFlow(false)
        val isAccessibilityActive: StateFlow<Boolean> = _isAccessibilityActive.asStateFlow()

        private val _currentForegroundPackage = MutableStateFlow("")
        val currentForegroundPackage: StateFlow<String> = _currentForegroundPackage.asStateFlow()

        fun getInstance(): XiuAccessibilityService? = instance

        fun isRunning(): Boolean = instance != null

        fun execute(action: AgentActionResponse, callback: ((Boolean, String) -> Unit)? = null) {
            instance?.performAgentAction(action, callback) ?: run {
                callback?.invoke(false, "Layanan Aksesibilitas belum diaktifkan di Pengaturan")
            }
        }
    }
}
