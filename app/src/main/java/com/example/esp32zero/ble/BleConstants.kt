package com.example.esp32zero.ble

import java.util.UUID

/**
 * ESP32 GATT sunucusuyla eşleşmesi gereken sabitler.
 * SERVICE_UUID / COMMAND / RESPONSE karakteristik UUID'leri esp32-multitool
 * deposundaki include/ble_uuids.h ile birebir aynı olmalıdır.
 */
object BleConstants {

    // esp32-multitool/include/ble_uuids.h -> BLE_SERVICE_UUID ile eşleşir.
    val SERVICE_UUID: UUID = UUID.fromString("1eac5c68-6cfd-46ca-b9dc-0d8dd2ade33f")

    // esp32-multitool/include/ble_uuids.h -> BLE_COMMAND_CHAR_UUID ile eşleşir.
    val COMMAND_CHARACTERISTIC_UUID: UUID = UUID.fromString("b10a8537-f7ed-46d1-801c-735e3c74ea5e")

    // esp32-multitool/include/ble_uuids.h -> BLE_RESPONSE_CHAR_UUID ile eşleşir.
    val RESPONSE_CHARACTERISTIC_UUID: UUID = UUID.fromString("f24e04f4-aec8-420c-9162-98efd1cd5fbf")

    // Standart Client Characteristic Configuration Descriptor (CCCD) UUID'si.
    // Bu, BLE spesifikasyonunda sabittir; placeholder değildir.
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Cihaz bulunamazsa taramayı sonlandırmak için zaman aşımı süresi.
    const val SCAN_TIMEOUT_MS: Long = 10_000L
}
