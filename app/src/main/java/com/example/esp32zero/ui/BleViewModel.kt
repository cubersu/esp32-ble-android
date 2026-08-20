package com.example.esp32zero.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esp32zero.ble.BleConnectionState
import com.example.esp32zero.ble.BleConstants
import com.example.esp32zero.ble.BleDeviceInfo
import com.example.esp32zero.ble.BleManager
import com.example.esp32zero.ble.BleProtocol
import com.example.esp32zero.ble.SubGhzSignal
import com.example.esp32zero.ble.WifiCapture
import com.example.esp32zero.ble.WifiCaptureChunk
import com.example.esp32zero.ble.WifiNetwork
import java.util.Base64
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

    // Yakalanan Wi-Fi paketleri (PCAP, en yeni başta). Şu an yalnızca
    // bellekte tutuluyor; kalıcı saklama Faz 7'nin kapsamında.
    private val _wifiCaptures = MutableStateFlow<List<WifiCapture>>(emptyList())
    val wifiCaptures: StateFlow<List<WifiCapture>> = _wifiCaptures.asStateFlow()

    // "wifi_capture_chunk" bildirimlerini capture_id'ye göre yeniden
    // birleştirmek için tutulan geçici durum. ESP32 tek seferde tek bir
    // yakalama gönderdiği için pratikte tek bir giriş oluyor; yine de
    // capture_id'ye göre anahtarlanarak olası bir çakışma (örn. eski bir
    // yakalamanın geç gelen son parçası) yanlış tamponla karışmaz.
    private val chunkBuffers = mutableMapOf<Long, MutableMap<Int, String>>()

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
                BleProtocol.parseWifiCaptureChunk(json)?.let { chunk ->
                    handleWifiCaptureChunk(chunk)
                }
            }
        }
    }

    private fun handleWifiCaptureChunk(chunk: WifiCaptureChunk) {
        val buffer = chunkBuffers.getOrPut(chunk.captureId) { mutableMapOf() }
        buffer[chunk.seq] = chunk.chunkBase64

        if (buffer.size < chunk.total) return

        // Tüm parçalar geldi: seq sırasına göre birleştirip base64'ü çöz.
        val fullBase64 = (0 until chunk.total).joinToString(separator = "") { seq -> buffer[seq].orEmpty() }
        chunkBuffers.remove(chunk.captureId)

        val pcapBytes = try {
            Base64.getDecoder().decode(fullBase64)
        } catch (e: IllegalArgumentException) {
            _errorMessage.value = "Yakalanan Wi-Fi verisi bozuk geldi"
            return
        }

        _wifiCaptures.update { current ->
            listOf(WifiCapture(pcapBytes = pcapBytes, packetCount = chunk.packetCount)) + current
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

    fun onCaptureWifiClicked(channel: Int) {
        if (connectionState.value != BleConnectionState.CONNECTED) return
        viewModelScope.launch {
            try {
                bleManager.sendCommand(BleProtocol.buildWifiCaptureCommand(channel))
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Wi-Fi paket yakalama başlatılamadı"
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
