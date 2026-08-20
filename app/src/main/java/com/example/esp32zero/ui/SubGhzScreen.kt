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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.esp32zero.ble.BleConnectionState
import com.example.esp32zero.ble.BleProtocol
import com.example.esp32zero.ble.SubGhzSignal
import java.text.SimpleDateFormat
import java.util.Locale

private val subGhzTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

/** Hz cinsinden bir frekansı "433.92 MHz" gibi okunabilir bir etikete çevirir. */
fun Long.toMhzLabel(): String {
    val mhz = this / 1_000_000.0
    return if (mhz == mhz.toLong().toDouble()) "${mhz.toLong()} MHz" else "$mhz MHz"
}

/** Yakalanan bir Sub-GHz sinyalini listede gösterilecek kısa etikete çevirir. */
private fun SubGhzSignal.toDisplayLabel(): String =
    "${subGhzTimeFormat.format(capturedAtMillis)} — ${frequencyHz.toMhzLabel()}"

@Composable
fun SubGhzScreen(viewModel: BleViewModel, modifier: Modifier = Modifier) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val capturedSignals by viewModel.capturedSignals.collectAsStateWithLifecycle()
    val selectedFrequencyHz by viewModel.selectedFrequencyHz.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var customFrequencyMhzText by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Frekans: ${selectedFrequencyHz.toMhzLabel()}",
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BleProtocol.COMMON_SUBGHZ_FREQUENCIES_HZ.forEach { frequencyHz ->
                Button(
                    onClick = { viewModel.onFrequencySelected(frequencyHz) },
                    colors = if (frequencyHz == selectedFrequencyHz) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                ) {
                    Text(frequencyHz.toMhzLabel())
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = customFrequencyMhzText,
                onValueChange = { customFrequencyMhzText = it },
                label = { Text("Özel MHz") },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    customFrequencyMhzText.toDoubleOrNull()?.let { mhz ->
                        viewModel.onFrequencySelected((mhz * 1_000_000).toLong())
                    }
                },
            ) {
                Text("Uygula")
            }
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

        Text(text = "Yakalanan Sinyaller", style = MaterialTheme.typography.titleSmall)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
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
}
