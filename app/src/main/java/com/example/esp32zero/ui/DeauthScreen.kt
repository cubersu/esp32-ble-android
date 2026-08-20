package com.example.esp32zero.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.esp32zero.ui.components.LastResponseCard

@Composable
fun DeauthScreen(viewModel: BleViewModel, modifier: Modifier = Modifier) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val responseLog by viewModel.responseLog.collectAsStateWithLifecycle()

    var bssidInput by remember { mutableStateOf("") }
    var clientMacInput by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf(1) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "SADECE KENDİ AĞIN İÇİN",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
        )
        OutlinedTextField(
            value = bssidInput,
            onValueChange = { bssidInput = it },
            label = { Text("Hedef BSSID (kendi erişim noktan)") },
            placeholder = { Text("AA:BB:CC:DD:EE:FF") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = clientMacInput,
            onValueChange = { clientMacInput = it },
            label = { Text("İstemci MAC (boş = tüm istemciler)") },
            placeholder = { Text("AA:BB:CC:DD:EE:FF") },
            modifier = Modifier.fillMaxWidth(),
        )

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
            onClick = {
                viewModel.onSendDeauthClicked(bssid = bssidInput, clientMac = clientMacInput, channel = selectedChannel)
            },
            enabled = connectionState == BleConnectionState.CONNECTED && bssidInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Deauth Gönder")
        }

        errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        LastResponseCard(entry = responseLog.firstOrNull())
    }
}
