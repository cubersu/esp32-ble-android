package com.example.esp32zero.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.esp32zero.ble.BleConnectionState
import com.example.esp32zero.ble.WifiCapture
import com.example.esp32zero.ble.WifiCaptureExporter
import java.text.SimpleDateFormat
import java.util.Locale

private val wifiCaptureTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

/** Yakalanan bir Wi-Fi paket setini listede gösterilecek kısa etikete çevirir. */
private fun WifiCapture.toDisplayLabel(): String =
    "${wifiCaptureTimeFormat.format(capturedAtMillis)} — $packetCount paket"

@Composable
fun WifiCaptureScreen(viewModel: BleViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val wifiCaptures by viewModel.wifiCaptures.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var selectedChannel by remember { mutableStateOf(1) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Wi-Fi Kanalı: $selectedChannel", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..13).forEach { channel ->
                Button(
                    onClick = { selectedChannel = channel },
                    colors = if (channel == selectedChannel) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                ) {
                    Text("$channel")
                }
            }
        }
        Button(
            onClick = { viewModel.onCaptureWifiClicked(selectedChannel) },
            enabled = connectionState == BleConnectionState.CONNECTED,
        ) {
            Text("Wi-Fi Paket Yakala")
        }

        errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        Text(text = "Yakalanan Wi-Fi Paketleri", style = MaterialTheme.typography.titleSmall)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(wifiCaptures) { capture ->
                Card {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = capture.toDisplayLabel())
                        Button(
                            onClick = {
                                context.startActivity(WifiCaptureExporter.buildShareIntent(context, capture))
                            },
                        ) {
                            Text("PCAP Paylaş")
                        }
                    }
                }
            }
        }
    }
}
