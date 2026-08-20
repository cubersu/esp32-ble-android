package com.example.esp32zero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.esp32zero.ble.BleConnectionState

@Composable
fun BleScanScreen(viewModel: BleViewModel, modifier: Modifier = Modifier) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val bleDevices by viewModel.bleDevices.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = { viewModel.onScanBleDevicesClicked() },
            enabled = connectionState == BleConnectionState.CONNECTED,
        ) {
            Text("BLE Tara")
        }

        errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(bleDevices) { device ->
                Card {
                    val label = device.name.ifBlank { "(isimsiz cihaz)" }
                    Text(
                        text = "$label — ${device.address}  (${device.rssi} dBm)",
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}
