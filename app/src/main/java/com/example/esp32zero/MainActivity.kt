package com.example.esp32zero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.esp32zero.ui.BleScreen
import com.example.esp32zero.ui.theme.Esp32ZeroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Esp32ZeroTheme {
                BleScreen()
            }
        }
    }
}
