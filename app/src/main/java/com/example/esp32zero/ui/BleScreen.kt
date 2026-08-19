package com.example.esp32zero.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esp32zero.ble.BleConnectionState
import com.example.esp32zero.ble.PermissionUtils
import com.example.esp32zero.ble.SubGhzSignal
import java.text.SimpleDateFormat
import java.util.Locale

private val subGhzTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

/** Yakalanan bir Sub-GHz sinyalini listede gösterilecek kısa etikete çevirir. */
private fun SubGhzSignal.toDisplayLabel(): String =
    "${subGhzTimeFormat.format(capturedAtMillis)} — ${frequencyHz / 1_000_000} MHz"

/** Bağlantı durumunu kullanıcıya Türkçe olarak gösteren etiket. */
private fun BleConnectionState.toDisplayLabel(): String = when (this) {
    BleConnectionState.DISCONNECTED -> "Bağlı değil"
    BleConnectionState.SCANNING -> "Taranıyor"
    BleConnectionState.CONNECTING -> "Bağlanıyor"
    BleConnectionState.CONNECTED -> "Bağlı"
}

@Composable
fun BleScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: BleViewModel = viewModel(factory = bleViewModelFactory(context))

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val responseLog by viewModel.responseLog.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val wifiNetworks by viewModel.wifiNetworks.collectAsStateWithLifecycle()
    val bleDevices by viewModel.bleDevices.collectAsStateWithLifecycle()
    val capturedSignals by viewModel.capturedSignals.collectAsStateWithLifecycle()

    var permissionsGranted by remember { mutableStateOf(PermissionUtils.hasAllBlePermissions(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
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

            Button(
                onClick = { viewModel.onScanWifiClicked() },
                enabled = connectionState == BleConnectionState.CONNECTED,
            ) {
                Text("Wi-Fi Tara")
            }

            Button(
                onClick = { viewModel.onScanBleDevicesClicked() },
                enabled = connectionState == BleConnectionState.CONNECTED,
            ) {
                Text("BLE Tara")
            }

            Button(
                onClick = { viewModel.onCaptureSubGhzClicked() },
                enabled = connectionState == BleConnectionState.CONNECTED,
            ) {
                Text("Sub-GHz Yakala")
            }

            errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            if (wifiNetworks.isNotEmpty()) {
                Text(text = "Wi-Fi Ağları", style = MaterialTheme.typography.titleSmall)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(wifiNetworks) { network ->
                        Card {
                            Text(
                                text = "${network.ssid}  (${network.rssi} dBm)" +
                                    if (network.secure) "  🔒" else "",
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            }

            if (bleDevices.isNotEmpty()) {
                Text(text = "BLE Cihazları", style = MaterialTheme.typography.titleSmall)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
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

            if (capturedSignals.isNotEmpty()) {
                Text(text = "Yakalanan Sinyaller", style = MaterialTheme.typography.titleSmall)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(capturedSignals) { signal ->
                        Card {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(text = signal.toDisplayLabel())
                                Button(
                                    onClick = { viewModel.onReplaySignalClicked(signal) },
                                    enabled = connectionState == BleConnectionState.CONNECTED,
                                ) {
                                    Text("Gönder")
                                }
                            }
                        }
                    }
                }
            }

            Text(text = "Yanıtlar", style = MaterialTheme.typography.titleSmall)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(responseLog) { entry ->
                    Card(
                        colors = if (entry.isPong) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            CardDefaults.cardColors()
                        },
                    ) {
                        Text(
                            text = entry.rawJson,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}
