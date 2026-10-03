package com.xiu.lockagent.manager

import com.xiu.lockagent.model.ExecutionLogItem
import com.xiu.lockagent.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Pengelola Log Eksekusi Real-time untuk Xiu LockAgent
 */
object ExecutionLogManager {

    private val _logs = MutableStateFlow<List<ExecutionLogItem>>(
        listOf(
            ExecutionLogItem(
                id = UUID.randomUUID().toString(),
                tag = "SISTEM",
                message = "Xiu LockAgent runtime diinisialisasi untuk vivo Y18 (Helio G85)",
                details = "Display 90 Hz, 720x1612 px. Target memori overlay < 10 MB.",
                level = LogLevel.INFO
            )
        )
    )
    val logs: StateFlow<List<ExecutionLogItem>> = _logs.asStateFlow()

    fun log(
        tag: String,
        message: String,
        details: String? = null,
        level: LogLevel = LogLevel.INFO,
        latencyMs: Long? = null,
        tokensUsed: Int? = null
    ) {
        val newItem = ExecutionLogItem(
            id = UUID.randomUUID().toString(),
            tag = tag,
            message = message,
            details = details,
            level = level,
            latencyMs = latencyMs,
            tokensUsed = tokensUsed
        )
        // Keep last 150 items to conserve memory
        _logs.value = listOf(newItem) + _logs.value.take(149)
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
