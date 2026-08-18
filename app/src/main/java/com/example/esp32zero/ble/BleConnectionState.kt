package com.example.esp32zero.ble

/**
 * BLE bağlantısının olası durumları. UI katmanında Türkçe etiketlere eşlenir:
 * DISCONNECTED -> "Bağlı değil", SCANNING -> "Taranıyor",
 * CONNECTING -> "Bağlanıyor", CONNECTED -> "Bağlı".
 */
enum class BleConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
}
