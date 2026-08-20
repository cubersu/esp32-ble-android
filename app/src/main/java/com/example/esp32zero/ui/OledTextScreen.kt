package com.example.esp32zero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.example.esp32zero.ui.components.LastResponseCard

@Composable
fun OledTextScreen(viewModel: BleViewModel, modifier: Modifier = Modifier) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val responseLog by viewModel.responseLog.collectAsStateWithLifecycle()

    var oledTextInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = oledTextInput,
                onValueChange = { oledTextInput = it },
                label = { Text("Mesaj") },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    viewModel.onSendOledTextClicked(oledTextInput)
                    oledTextInput = ""
                },
                enabled = connectionState == BleConnectionState.CONNECTED && oledTextInput.isNotBlank(),
            ) {
                Text("Gönder")
            }
        }

        errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        LastResponseCard(entry = responseLog.firstOrNull())
    }
}
