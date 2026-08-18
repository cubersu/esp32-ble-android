package com.example.esp32zero.ble

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * ESP32 GATT sunucusuyla haberleşmeyi soyutlayan arayüz.
 * Gerçek Android BLE çağrılarını [AndroidBleManager] yapar; bu arayüz sayesinde
 * ViewModel katmanı gerçek Bluetooth framework sınıflarına bağımlı olmadan,
 * sahte (fake) bir implementasyonla unit test edilebilir.
 */
interface BleManager {

    /** Güncel bağlantı durumunu yayınlayan akış. */
    val connectionState: StateFlow<BleConnectionState>

    /**
     * Verilen servis UUID'sini reklam eden bir cihaz bulunana ya da zaman aşımına
     * uğrayana kadar tarama yapar. Bulunursa cihazı, bulunamazsa null döner.
     */
    suspend fun scanForDevice(serviceUuid: UUID): BluetoothDevice?

    /** Verilen cihaza GATT bağlantısı kurar, servisleri keşfeder ve notify'ı açar. */
    suspend fun connect(device: BluetoothDevice)

    /** Komut karakteristiğine JSON string'i UTF-8 olarak yazar. */
    suspend fun sendCommand(json: String)

    /** Yanıt karakteristiğinden gelen notify verilerini JSON string olarak yayınlar. */
    fun observeResponses(): Flow<String>

    /** GATT bağlantısını kapatır ve kaynakları serbest bırakır. */
    fun disconnect()
}
