package com.example.esp32zero.ble

/** ESP32'nin "wifi_scan" yanıtındaki tek bir Wi-Fi ağı. */
data class WifiNetwork(
    val ssid: String,
    val rssi: Int,
    val secure: Boolean,
)

/** ESP32'nin "ble_scan" yanıtındaki tek bir BLE cihazı. */
data class BleDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
)

/**
 * ESP32'nin "subghz_capture" yanıtındaki yakalanan ham Sub-GHz sinyali.
 * pulsesBase64, ham mikrosaniye darbe sürelerinin (uint16, little-endian)
 * base64 kodlanmış hâlidir; Android bu veriyi hiç çözmez (decode etmez),
 * yalnızca saklar ve "subghz_replay" ile aynen ESP32'ye geri gönderir.
 */
data class SubGhzSignal(
    val pulsesBase64: String,
    val frequencyHz: Long,
    val capturedAtMillis: Long = System.currentTimeMillis(),
)
