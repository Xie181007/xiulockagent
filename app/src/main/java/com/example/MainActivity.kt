package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.MysticBackground
import com.xiu.lockagent.manager.ClipboardWatcher
import com.xiu.lockagent.manager.DeviceHealthManager
import com.xiu.lockagent.ui.MainAppNavigation

class MainActivity : ComponentActivity() {

    private lateinit var healthManager: DeviceHealthManager
    private lateinit var clipboardWatcher: ClipboardWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        healthManager = DeviceHealthManager(this)
        clipboardWatcher = ClipboardWatcher(this)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MysticBackground
                ) {
                    MainAppNavigation(
                        healthManager = healthManager,
                        clipboardWatcher = clipboardWatcher
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        healthManager.startMonitoring()
        clipboardWatcher.startListening()
    }

    override fun onStop() {
        super.onStop()
        healthManager.stopMonitoring()
        clipboardWatcher.stopListening()
    }
}
