package com.example.esp32zero.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [BleManager]'ın gerçek Android BLE API'lerini kullanan implementasyonu.
 * Bu dosya, projedeki android.bluetooth.* framework sınıflarına dokunan tek yerdir;
 * bu sınıflar JVM unit testlerde mock'lanamadığı için burası yalnızca derlenerek
 * doğrulanır, gerçek tarama/bağlanma davranışı fiziksel donanımla test edilir.
 */
class AndroidBleManager(private val context: Context) : BleManager {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _responses = MutableSharedFlow<String>(extraBufferCapacity = 16)

    private var bluetoothGatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var responseCharacteristic: BluetoothGattCharacteristic? = null

    // connect() tamamlanana (servis keşfi + notify açılana) kadar askıda tutulan devam.
    private var connectContinuation: CancellableContinuation<Unit>? = null

    // sendCommand() yazma onayını (onCharacteristicWrite) beklerken askıda tutulan devam.
    private var writeContinuation: CancellableContinuation<Unit>? = null

    @SuppressLint("MissingPermission")
    override suspend fun scanForDevice(serviceUuid: UUID): BluetoothDevice? {
        if (!PermissionUtils.hasScanPermission(context)) {
            throw SecurityException("BLE tarama izni verilmemiş")
        }
        val scanner = bluetoothManager?.adapter?.bluetoothLeScanner ?: return null

        _connectionState.value = BleConnectionState.SCANNING
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(serviceUuid)).build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val foundDevice = withTimeoutOrNull(BleConstants.SCAN_TIMEOUT_MS) {
            callbackFlow {
                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        trySend(result.device)
                    }

                    override fun onScanFailed(errorCode: Int) {
                        close(IllegalStateException("BLE tarama başarısız oldu: $errorCode"))
                    }
                }
                scanner.startScan(listOf(filter), settings, callback)
                awaitClose { scanner.stopScan(callback) }
            }.first()
        }

        if (foundDevice == null) {
            _connectionState.value = BleConnectionState.DISCONNECTED
        }
        return foundDevice
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(device: BluetoothDevice) {
        if (!PermissionUtils.hasConnectPermission(context)) {
            throw SecurityException("BLE bağlantı izni verilmemiş")
        }
        _connectionState.value = BleConnectionState.CONNECTING
        suspendCancellableCoroutine { cont ->
            connectContinuation = cont
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            cont.invokeOnCancellation {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun sendCommand(json: String) {
        if (!PermissionUtils.hasConnectPermission(context)) {
            throw SecurityException("BLE bağlantı izni verilmemiş")
        }
        val gatt = bluetoothGatt ?: throw IllegalStateException("GATT bağlantısı yok")
        val characteristic = commandCharacteristic
            ?: throw IllegalStateException("Komut karakteristiği henüz keşfedilmedi")

        suspendCancellableCoroutine { cont ->
            writeContinuation = cont
            val started = writeCharacteristicCompat(gatt, characteristic, json.toByteArray(Charsets.UTF_8))
            if (!started) {
                writeContinuation = null
                cont.resumeWithException(IllegalStateException("Komut yazımı başlatılamadı"))
            }
        }
    }

    override fun observeResponses(): Flow<String> = _responses.asSharedFlow()

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        commandCharacteristic = null
        responseCharacteristic = null
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                // Servis keşfinden önce daha büyük bir MTU talep ediyoruz;
                // asıl discoverServices() çağrısı onMtuChanged()'e taşındı.
                BluetoothProfile.STATE_CONNECTED -> gatt.requestMtu(BleConstants.REQUESTED_MTU)
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    connectContinuation?.let {
                        if (it.isActive) it.resumeWithException(IllegalStateException("Bağlantı koptu"))
                    }
                    connectContinuation = null
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            // MTU pazarlığı başarısız olsa (status != GATT_SUCCESS) bile
            // servis keşfine devam ediyoruz; GATT varsayılan (23 byte)
            // MTU ile de çalışır, sadece büyük yükler (örn. Sub-GHz
            // yakalama) tek bildirimde sığmayabilir.
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val cont = connectContinuation
            if (status != BluetoothGatt.GATT_SUCCESS) {
                cont?.let { if (it.isActive) it.resumeWithException(IllegalStateException("Servis keşfi başarısız: $status")) }
                connectContinuation = null
                return
            }

            val service = gatt.getService(BleConstants.SERVICE_UUID)
            val command = service?.getCharacteristic(BleConstants.COMMAND_CHARACTERISTIC_UUID)
            val response = service?.getCharacteristic(BleConstants.RESPONSE_CHARACTERISTIC_UUID)
            if (command == null || response == null) {
                cont?.let { if (it.isActive) it.resumeWithException(IllegalStateException("Beklenen BLE karakteristikleri bulunamadı")) }
                connectContinuation = null
                return
            }
            commandCharacteristic = command
            responseCharacteristic = response

            gatt.setCharacteristicNotification(response, true)
            val descriptor = response.getDescriptor(BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID)
            if (descriptor != null) {
                writeDescriptorCompat(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            }

            _connectionState.value = BleConnectionState.CONNECTED
            cont?.let { if (it.isActive) it.resume(Unit) }
            connectContinuation = null
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid != BleConstants.RESPONSE_CHARACTERISTIC_UUID) return
            val payload = characteristic.value?.toString(Charsets.UTF_8) ?: return
            _responses.tryEmit(payload)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val cont = writeContinuation ?: return
            writeContinuation = null
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (cont.isActive) cont.resume(Unit)
            } else {
                if (cont.isActive) cont.resumeWithException(IllegalStateException("Komut yazımı başarısız: $status"))
            }
        }
    }

    // API 33'te writeCharacteristic(characteristic) deprecated oldu; yeni imza karakteristik
    // içindeki mutable .value alanına bağımlı değil. Eski cihazlarda (minSdk 26) eski API kullanılır.
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun writeCharacteristicCompat(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                characteristic,
                value,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            ) == BluetoothGatt.GATT_SUCCESS
        } else {
            characteristic.value = value
            gatt.writeCharacteristic(characteristic)
        }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun writeDescriptorCompat(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
    ): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value) == BluetoothGatt.GATT_SUCCESS
        } else {
            descriptor.value = value
            gatt.writeDescriptor(descriptor)
        }
}
