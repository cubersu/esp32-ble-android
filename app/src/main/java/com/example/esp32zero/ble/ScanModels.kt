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
