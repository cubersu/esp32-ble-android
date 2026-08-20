package com.example.esp32zero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Ham BLE yanıt akışı — komuta özgü bir sonuç listesi olmayan durumları (deauth, oled_text) hata ayıklamak için. */
@Composable
fun ResponseLogScreen(viewModel: BleViewModel, modifier: Modifier = Modifier) {
    val responseLog by viewModel.responseLog.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
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
