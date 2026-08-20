package com.example.esp32zero.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.esp32zero.ui.ResponseLogEntry

/**
 * En son gelen BLE yanıtını ham JSON olarak gösteren küçük bir kart.
 * Kendi state/liste'si olmayan komutların (örn. deauth, oled_text) sonucunu
 * kullanıcıya o an bulunduğu sayfada göstermek için kullanılır; tüm geçmiş
 * için ayrı bir "Yanıtlar" (debug log) sayfası var.
 */
@Composable
fun LastResponseCard(entry: ResponseLogEntry?, modifier: Modifier = Modifier) {
    if (entry == null) return

    Card(modifier = modifier) {
        Text(
            text = "Son yanıt: ${entry.rawJson}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp),
        )
    }
}
