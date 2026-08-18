package com.example.esp32zero.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esp32zero.ble.BleConnectionState
import com.example.esp32zero.ble.BleConstants
import com.example.esp32zero.ble.BleManager
import com.example.esp32zero.ble.BleProtocol
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

    init {
        viewModelScope.launch {
            bleManager.observeResponses().collect { json ->
                // Yeni yanıtlar log'un başına eklenir (en yeni üstte gösterilir).
                _responseLog.update { current ->
                    listOf(ResponseLogEntry(json, BleProtocol.isPong(json), System.currentTimeMillis())) + current
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

    override fun onCleared() {
        bleManager.disconnect()
    }
}
