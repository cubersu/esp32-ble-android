package com.example.esp32zero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.esp32zero.ble.BleConnectionState

@Composable
fun RogueApScanScreen(viewModel: BleViewModel, modifier: Modifier = Modifier) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val result by viewModel.rogueApResult.collectAsStateWithLifecycle()

    var ssidInput by remember { mutableStateOf("") }
    var knownBssidInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Tamamen pasif tarama — hiçbir şey yayınlamaz.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = ssidInput,
            onValueChange = { ssidInput = it },
            label = { Text("İzlenecek SSID (kendi ağının adı)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = knownBssidInput,
            onValueChange = { knownBssidInput = it },
            label = { Text("Bilinen BSSID (isteğe bağlı)") },
            placeholder = { Text("AA:BB:CC:DD:EE:FF") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.onScanRogueApClicked(ssid = ssidInput, knownBssid = knownBssidInput) },
            enabled = connectionState == BleConnectionState.CONNECTED && ssidInput.isNotBlank(),
        ) {
            Text("Tara")
        }

        errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        result?.let { scanResult ->
            if (scanResult.suspicious) {
                Text(
                    text = "⚠️ Şüpheli: \"${scanResult.ssid}\" birden fazla veya beklenmeyen BSSID'den yayınlanıyor",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleSmall,
                )
            } else {
                Text(
                    text = "\"${scanResult.ssid}\" için şüpheli bir şey bulunamadı",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            scanResult.accessPoints.forEach { ap ->
                Card {
                    Text(
                        text = "${ap.bssid}  (${ap.rssi} dBm)" +
                            (if (ap.secure) "  🔒" else "") +
                            (if (ap.isKnown) "  ✓ bilinen" else ""),
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}
