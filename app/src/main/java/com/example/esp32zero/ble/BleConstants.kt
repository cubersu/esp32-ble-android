package com.example.esp32zero.ble

import java.util.UUID

/**
 * ESP32 GATT sunucusuyla eşleşmesi gereken sabitler.
 * SERVICE_UUID / COMMAND / RESPONSE karakteristik UUID'leri şu an placeholder'dır,
 * ESP32 firmware'i yazıldığında bu değerler elle güncellenmelidir.
 */
object BleConstants {

    // Placeholder: ESP32 tarafındaki gerçek servis UUID'si ile değiştirilecek.
    val SERVICE_UUID: UUID = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb")

    // Placeholder: telefon -> ESP32 komut yazımı için kullanılan karakteristik.
    val COMMAND_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")

    // Placeholder: ESP32 -> telefon yanıt/notify karakteristiği.
    val RESPONSE_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")

    // Standart Client Characteristic Configuration Descriptor (CCCD) UUID'si.
    // Bu, BLE spesifikasyonunda sabittir; placeholder değildir.
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Cihaz bulunamazsa taramayı sonlandırmak için zaman aşımı süresi.
    const val SCAN_TIMEOUT_MS: Long = 10_000L
}
