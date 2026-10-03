package com.xiu.lockagent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
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
import com.xiu.lockagent.manager.ExecutionLogManager
import com.xiu.lockagent.model.ExecutionLogItem
import com.xiu.lockagent.model.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExecutionLogScreen() {
    val logs by ExecutionLogManager.logs.collectAsState()
    var selectedFilter by remember { mutableStateOf("SEMUA") }

    val filterOptions = listOf("SEMUA", "AUTO_ENGINE", "NODE_TRAVERSAL", "GESTURE_FALLBACK", "SPEECH", "CLIPBOARD", "PROXY_GO")

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == "SEMUA") logs
        else logs.filter { it.tag.contains(selectedFilter, ignoreCase = true) }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LOG EKSEKUSI AGEN",
                    color = MysticTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredLogs.size} peristiwa tercatat • Latensi & Token",
                    color = MysticTextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { ExecutionLogManager.clear() },
                modifier = Modifier.testTag("clear_logs_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Hapus Log",
                    tint = MysticTextSecondary
                )
            }
        }

        // Filter Chips Bar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterOptions) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MysticPrimary,
                        selectedLabelColor = MysticTextPrimary,
                        containerColor = MysticContainer,
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

        // List Log Entries
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada peristiwa log untuk filter ini.",
                    color = MysticTextTertiary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredLogs, key = { it.id }) { item ->
                    LogCard(item, timeFormat)
                }
            }
        }
    }
}

@Composable
private fun LogCard(item: ExecutionLogItem, timeFormat: SimpleDateFormat) {
    val levelColor = when (item.level) {
        LogLevel.INFO -> MysticCyan
        LogLevel.SUCCESS -> MysticSuccess
        LogLevel.WARNING -> MysticWarning
        LogLevel.ERROR -> MysticError
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MysticContainer),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MysticBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(levelColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.tag,
                        color = levelColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = timeFormat.format(Date(item.timestamp)),
                    color = MysticTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.message,
                color = MysticTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            item.details?.let { details ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = details,
                    color = MysticTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (item.latencyMs != null || item.tokensUsed != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item.latencyMs?.let { latency ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MysticTextTertiary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${latency} ms",
                                color = MysticTextTertiary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    item.tokensUsed?.let { tokens ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MysticPrimaryHighlight,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$tokens Tok",
                                color = MysticPrimaryHighlight,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
