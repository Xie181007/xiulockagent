package com.xiu.lockagent.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MysticBorder
import com.example.ui.theme.MysticContainer
import com.example.ui.theme.MysticContainerElevated
import com.example.ui.theme.MysticPrimary
import com.example.ui.theme.MysticPrimaryHighlight
import com.example.ui.theme.MysticTextPrimary
import com.example.ui.theme.MysticTextSecondary
import com.xiu.lockagent.manager.ClipboardWatcher
import com.xiu.lockagent.manager.DeviceHealthManager
import com.xiu.lockagent.service.XiuFloatingService
import com.xiu.lockagent.ui.screens.DashboardScreen
import com.xiu.lockagent.ui.screens.ExecutionLogScreen
import com.xiu.lockagent.ui.screens.SandboxSimulationScreen
import com.xiu.lockagent.ui.screens.SettingsProxyScreen

sealed class NavScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    object Dashboard : NavScreen("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard")
    object Simulation : NavScreen("Simulasi", Icons.Filled.Layers, Icons.Outlined.Layers, "nav_simulation")
    object Logs : NavScreen("Log", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "nav_logs")
    object Settings : NavScreen("Pengaturan", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

@Composable
fun MainAppNavigation(
    healthManager: DeviceHealthManager,
    clipboardWatcher: ClipboardWatcher
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val screens = listOf(
        NavScreen.Dashboard,
        NavScreen.Simulation,
        NavScreen.Logs,
        NavScreen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MysticContainer,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav")
            ) {
                screens.forEachIndexed { index, screen ->
                    val isSelected = selectedIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MysticTextPrimary,
                            selectedTextColor = MysticPrimaryHighlight,
                            unselectedIconColor = MysticTextSecondary,
                            unselectedTextColor = MysticTextSecondary,
                            indicatorColor = MysticPrimary
                        ),
                        modifier = Modifier.testTag(screen.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedIndex) {
                0 -> DashboardScreen(
                    healthManager = healthManager,
                    clipboardWatcher = clipboardWatcher,
                    onNavigateToSimulation = { selectedIndex = 1 },
                    onNavigateToLogs = { selectedIndex = 2 },
                    onTriggerInstruction = { instruction ->
                        XiuFloatingService.sendInstruction(instruction)
                    }
                )
                1 -> SandboxSimulationScreen()
                2 -> ExecutionLogScreen()
                3 -> SettingsProxyScreen()
            }
        }
    }
}
