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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.esp32zero.ble.BleConnectionState

@Composable
fun NetScanScreen(viewModel: BleViewModel, modifier: Modifier = Modifier) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val result by viewModel.netScanResult.collectAsStateWithLifecycle()

    var ssidInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "ESP32 kendi ağına katılıp aynı alt ağı tarar — bu Wi-Fi'nin şifresini biliyor olman gerekir.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = ssidInput,
            onValueChange = { ssidInput = it },
            label = { Text("Ağ adı (SSID)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Şifre (açık ağ için boş bırak)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.onNetScanClicked(ssid = ssidInput, password = passwordInput) },
            enabled = connectionState == BleConnectionState.CONNECTED && ssidInput.isNotBlank(),
        ) {
            Text("Ağı Tara")
        }

        errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        result?.let { scanResult ->
            Text(text = "ESP32 IP: ${scanResult.localIp}", style = MaterialTheme.typography.bodySmall)
            if (scanResult.timedOut) {
                Text(
                    text = "Süre doldu — sonuçlar eksik olabilir (kısmi tarama)",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(text = "Bulunan Cihazlar", style = MaterialTheme.typography.titleSmall)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(scanResult.hosts) { host ->
                    Card {
                        Text(
                            text = "${host.ip} — portlar: ${host.openPorts.joinToString()}",
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}
