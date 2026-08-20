package com.example.esp32zero.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.esp32zero.ble.BleConnectionState
import com.example.esp32zero.ble.PermissionUtils
import com.example.esp32zero.ui.navigation.AppDestination
import com.example.esp32zero.ui.navigation.featureDestinations

/** Bağlantı durumunu kullanıcıya Türkçe olarak gösteren etiket. */
fun BleConnectionState.toDisplayLabel(): String = when (this) {
    BleConnectionState.DISCONNECTED -> "Bağlı değil"
    BleConnectionState.SCANNING -> "Taranıyor"
    BleConnectionState.CONNECTING -> "Bağlanıyor"
    BleConnectionState.CONNECTED -> "Bağlı"
}

/**
 * Ana sayfa: bağlantı kurma/ping ve her özelliğin kendi sayfasına giden bir
 * menü. Sonuç listeleri (Wi-Fi/BLE tarama, Sub-GHz, vb.) artık burada değil,
 * her özelliğin kendi sayfasında — tek bir uzun, taşan sayfa yerine.
 */
@Composable
fun HomeScreen(viewModel: BleViewModel, onNavigate: (AppDestination) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    var permissionsGranted by remember { mutableStateOf(PermissionUtils.hasAllBlePermissions(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Durum: ${connectionState.toDisplayLabel()}",
            style = MaterialTheme.typography.titleMedium,
        )

        if (!permissionsGranted) {
            Text("BLE tarama/bağlantı için izin gerekiyor.")
            Button(onClick = { permissionLauncher.launch(PermissionUtils.requiredBlePermissions()) }) {
                Text("İzin Ver")
            }
        }

        Button(
            onClick = { viewModel.onScanAndConnectClicked() },
            enabled = permissionsGranted && connectionState == BleConnectionState.DISCONNECTED,
        ) {
            Text("Tara ve Bağlan")
        }

        Button(
            onClick = { viewModel.onSendPingClicked() },
            enabled = connectionState == BleConnectionState.CONNECTED,
        ) {
            Text("Ping Gönder")
        }

        Text(text = "Özellikler", style = MaterialTheme.typography.titleSmall)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(featureDestinations) { destination ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onNavigate(destination) },
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                    ) {
                        Text(destination.title)
                    }
                }
            }
        }
    }
}
