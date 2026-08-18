package com.example.esp32zero.ble

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mockito.Mockito
import java.util.UUID

/**
 * [BleManager]'ın bellek-içi test double'ı. Gerçek Android BLE framework'üne
 * hiç dokunmaz, bu yüzden [BleViewModel][com.example.esp32zero.ui.BleViewModel]
 * gibi tüketicileri gerçek donanım/Robolectric olmadan unit test etmeyi sağlar.
 */
class FakeBleManager : BleManager {

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _responses = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Testin scanForDevice çağrısında bir cihaz "bulunup bulunmayacağını" kontrol eder. */
    var deviceFound: Boolean = true

    /** connect() çağrılıp çağrılmadığını doğrulamak için. */
    var connectCallCount: Int = 0
        private set

    /** sendCommand() ile en son gönderilen JSON, assert'ler için. */
    var lastSentCommand: String? = null
        private set

    /** disconnect() kaç kez çağrıldı. */
    var disconnectCallCount: Int = 0
        private set

    override suspend fun scanForDevice(serviceUuid: UUID): BluetoothDevice? {
        _connectionState.value = BleConnectionState.SCANNING
        return if (deviceFound) {
            // BluetoothDevice'ın public bir constructor'ı yok; Mockito ile sahte bir örnek
            // üretiyoruz, connect() bu örneğin içeriğini hiç kullanmıyor.
            Mockito.mock(BluetoothDevice::class.java)
        } else {
            _connectionState.value = BleConnectionState.DISCONNECTED
            null
        }
    }

    override suspend fun connect(device: BluetoothDevice) {
        connectCallCount++
        _connectionState.value = BleConnectionState.CONNECTED
    }

    override suspend fun sendCommand(json: String) {
        lastSentCommand = json
    }

    override fun observeResponses(): Flow<String> = _responses.asSharedFlow()

    override fun disconnect() {
        disconnectCallCount++
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    /** Testten ESP32'den bir notify geldiğini simüle eder. */
    suspend fun emitResponse(json: String) {
        _responses.emit(json)
    }
}
