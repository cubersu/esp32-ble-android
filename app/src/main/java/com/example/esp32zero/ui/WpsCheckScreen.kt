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

@Composable
fun WpsCheckScreen(viewModel: BleViewModel, modifier: Modifier = Modifier) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val result by viewModel.wpsCheckResult.collectAsStateWithLifecycle()

    var bssidInput by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf(1) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Yalnızca \"WPS açık mı\" pasif keşfi — PIN kaba kuvvet aracı değil.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = bssidInput,
            onValueChange = { bssidInput = it },
            label = { Text("Hedef BSSID (kendi erişim noktan)") },
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
            onClick = { viewModel.onCheckWpsClicked(bssid = bssidInput, channel = selectedChannel) },
            enabled = connectionState == BleConnectionState.CONNECTED && bssidInput.isNotBlank(),
        ) {
            Text("WPS Kontrol Et")
        }

        errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        result?.let { wpsResult ->
            Text(
                text = if (wpsResult.wpsEnabled) {
                    "⚠️ ${wpsResult.bssid}: WPS AÇIK"
                } else {
                    "${wpsResult.bssid}: WPS kapalı/yayınlanmıyor"
                },
                color = if (wpsResult.wpsEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
