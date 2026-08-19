package com.example.esp32zero.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esp32zero.ble.BleConnectionState
import com.example.esp32zero.ble.BleConstants
import com.example.esp32zero.ble.BleDeviceInfo
import com.example.esp32zero.ble.BleManager
import com.example.esp32zero.ble.BleProtocol
import com.example.esp32zero.ble.SubGhzSignal
import com.example.esp32zero.ble.WifiNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** BLE yanıt log'undaki tek bir satır. */
data class ResponseLogEntry(
    val rawJson: String,
    val isPong: Boolean,
    val timestamp: Long,
)

/**
 * BLE katmanı (android.bluetooth.*) ile hiç doğrudan temas etmez, yalnızca
 * [BleManager] arayüzüne bağımlıdır. Bu sayede sahte bir BleManager ile
 * tamamen unit test edilebilir.
 */
class BleViewModel(private val bleManager: BleManager) : ViewModel() {

    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState

    private val _responseLog = MutableStateFlow<List<ResponseLogEntry>>(emptyList())
    val responseLog: StateFlow<List<ResponseLogEntry>> = _responseLog.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _wifiNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val wifiNetworks: StateFlow<List<WifiNetwork>> = _wifiNetworks.asStateFlow()

    private val _bleDevices = MutableStateFlow<List<BleDeviceInfo>>(emptyList())
    val bleDevices: StateFlow<List<BleDeviceInfo>> = _bleDevices.asStateFlow()

    // Yakalanan Sub-GHz sinyalleri (en yeni başta). Şu an yalnızca bellekte
    // tutuluyor; kalıcı saklama (Room database) Faz 7'nin kapsamında.
    private val _capturedSignals = MutableStateFlow<List<SubGhzSignal>>(emptyList())
    val capturedSignals: StateFlow<List<SubGhzSignal>> = _capturedSignals.asStateFlow()

    // Bir sonraki "Sub-GHz Yakala" komutunda kullanılacak frekans.
    private val _selectedFrequencyHz = MutableStateFlow(BleProtocol.DEFAULT_SUBGHZ_FREQUENCY_HZ)
    val selectedFrequencyHz: StateFlow<Long> = _selectedFrequencyHz.asStateFlow()

    init {
        viewModelScope.launch {
            bleManager.observeResponses().collect { json ->
                // Yeni yanıtlar log'un başına eklenir (en yeni üstte gösterilir).
                _responseLog.update { current ->
                    listOf(ResponseLogEntry(json, BleProtocol.isPong(json), System.currentTimeMillis())) + current
                }

                BleProtocol.parseWifiScanResponse(json)?.let { networks ->
                    _wifiNetworks.value = networks
                }
                BleProtocol.parseBleScanResponse(json)?.let { devices ->
                    _bleDevices.value = devices
                }
                BleProtocol.parseSubGhzCaptureResponse(json)?.let { signal ->
                    _capturedSignals.update { current -> listOf(signal) + current }
                }
            }
        }
    }

    fun onScanAndConnectClicked() {
        viewModelScope.launch {
            try {
                val device = bleManager.scanForDevice(BleConstants.SERVICE_UUID)
                if (device != null) {
                    bleManager.connect(device)
                } else {
                    _errorMessage.value = "Cihaz bulunamadı"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Bağlantı hatası"
            }
        }
    }

    fun onSendPingClicked() {
        if (connectionState.value != BleConnectionState.CONNECTED) return
        viewModelScope.launch {
            try {
                bleManager.sendCommand(BleProtocol.buildPingCommand())
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Komut gönderilemedi"
            }
        }
    }

    fun onScanWifiClicked() {
        if (connectionState.value != BleConnectionState.CONNECTED) return
        viewModelScope.launch {
            try {
                bleManager.sendCommand(BleProtocol.buildWifiScanCommand())
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Wi-Fi taraması başlatılamadı"
            }
        }
    }

    fun onScanBleDevicesClicked() {
        if (connectionState.value != BleConnectionState.CONNECTED) return
        viewModelScope.launch {
            try {
                bleManager.sendCommand(BleProtocol.buildBleScanCommand())
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "BLE taraması başlatılamadı"
            }
        }
    }

    fun onFrequencySelected(frequencyHz: Long) {
        _selectedFrequencyHz.value = frequencyHz
    }

    fun onCaptureSubGhzClicked() {
        if (connectionState.value != BleConnectionState.CONNECTED) return
        viewModelScope.launch {
            try {
                bleManager.sendCommand(
                    BleProtocol.buildSubGhzCaptureCommand(frequencyHz = _selectedFrequencyHz.value),
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Sub-GHz yakalama başlatılamadı"
            }
        }
    }

    fun onReplaySignalClicked(signal: SubGhzSignal) {
        if (connectionState.value != BleConnectionState.CONNECTED) return
        viewModelScope.launch {
            try {
                bleManager.sendCommand(BleProtocol.buildSubGhzReplayCommand(signal))
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Sinyal gönderilemedi"
            }
        }
    }

    fun onSendOledTextClicked(text: String) {
        if (connectionState.value != BleConnectionState.CONNECTED) return
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                bleManager.sendCommand(BleProtocol.buildOledTextCommand(text))
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Ekrana gönderilemedi"
            }
        }
    }

    override fun onCleared() {
        bleManager.disconnect()
    }
}
