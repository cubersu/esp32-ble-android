package com.example.esp32zero.ui

import com.example.esp32zero.ble.BleConnectionState
import com.example.esp32zero.ble.BleProtocol
import com.example.esp32zero.ble.FakeBleManager
import com.example.esp32zero.ble.SubGhzSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BleViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeBleManager: FakeBleManager
    private lateinit var viewModel: BleViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakeBleManager = FakeBleManager()
        viewModel = BleViewModel(fakeBleManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `baslangic durumu bagli degil ve log bos`() {
        assertEquals(BleConnectionState.DISCONNECTED, viewModel.connectionState.value)
        assertTrue(viewModel.responseLog.value.isEmpty())
    }

    @Test
    fun `tara ve baglan cihaz bulununca baglaniyor`() = runTest {
        fakeBleManager.deviceFound = true

        viewModel.onScanAndConnectClicked()

        assertEquals(BleConnectionState.CONNECTED, viewModel.connectionState.value)
        assertEquals(1, fakeBleManager.connectCallCount)
    }

    @Test
    fun `baglantidayken ping gonderilince ping komutu yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onSendPingClicked()

        assertEquals(BleProtocol.buildPingCommand(), fakeBleManager.lastSentCommand)
    }

    @Test
    fun `bagli degilken ping gonderilmez`() = runTest {
        viewModel.onSendPingClicked()

        assertNull(fakeBleManager.lastSentCommand)
    }

    @Test
    fun `gelen pong yaniti log a isaretli sekilde eklenir`() = runTest {
        fakeBleManager.emitResponse("""{"status":"ok","data":"pong"}""")

        val entry = viewModel.responseLog.value.single()
        assertTrue(entry.isPong)
    }

    @Test
    fun `pong olmayan yanit log a isaretsiz eklenir`() = runTest {
        fakeBleManager.emitResponse("""{"status":"error","msg":"unknown command"}""")

        val entry = viewModel.responseLog.value.single()
        assertTrue(!entry.isPong)
    }

    @Test
    fun `baglantidayken wifi tara tiklaninca wifi_scan komutu yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onScanWifiClicked()

        assertEquals(BleProtocol.buildWifiScanCommand(), fakeBleManager.lastSentCommand)
    }

    @Test
    fun `bagli degilken wifi tara komutu gonderilmez`() = runTest {
        viewModel.onScanWifiClicked()

        assertNull(fakeBleManager.lastSentCommand)
    }

    @Test
    fun `gelen wifi_scan yaniti ag listesini gunceller`() = runTest {
        fakeBleManager.emitResponse(
            """{"status":"ok","data":{"type":"wifi_scan","networks":[{"ssid":"EvAgi","rssi":-45,"secure":true}]}}""",
        )

        val networks = viewModel.wifiNetworks.value
        assertEquals(1, networks.size)
        assertEquals("EvAgi", networks.single().ssid)
    }

    @Test
    fun `baglantidayken ble tara tiklaninca ble_scan komutu yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onScanBleDevicesClicked()

        assertEquals(BleProtocol.buildBleScanCommand(), fakeBleManager.lastSentCommand)
    }

    @Test
    fun `gelen ble_scan yaniti cihaz listesini gunceller`() = runTest {
        fakeBleManager.emitResponse(
            """{"status":"ok","data":{"type":"ble_scan","devices":[{"name":"Kulaklik","address":"AA:BB:CC:DD:EE:FF","rssi":-60}]}}""",
        )

        val devices = viewModel.bleDevices.value
        assertEquals(1, devices.size)
        assertEquals("Kulaklik", devices.single().name)
    }

    @Test
    fun `baglantidayken sub-ghz yakala tiklaninca subghz_capture komutu yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onCaptureSubGhzClicked()

        assertEquals(BleProtocol.buildSubGhzCaptureCommand(), fakeBleManager.lastSentCommand)
    }

    @Test
    fun `frekans secilince sub-ghz yakala secilen frekansi kullanir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onFrequencySelected(868_000_000L)
        viewModel.onCaptureSubGhzClicked()

        assertEquals(
            BleProtocol.buildSubGhzCaptureCommand(frequencyHz = 868_000_000L),
            fakeBleManager.lastSentCommand,
        )
    }

    @Test
    fun `gelen subghz_capture yaniti sinyal listesinin basina eklenir`() = runTest {
        fakeBleManager.emitResponse(
            """{"status":"ok","data":{"type":"subghz_capture","frequency_hz":433920000,"pulses_b64":"AQIDBA=="}}""",
        )

        val signals = viewModel.capturedSignals.value
        assertEquals(1, signals.size)
        assertEquals("AQIDBA==", signals.single().pulsesBase64)
    }

    @Test
    fun `sinyal gonder tiklaninca subghz_replay komutu ilgili sinyalle yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()
        val signal = SubGhzSignal(pulsesBase64 = "AQIDBA==", frequencyHz = 433920000L)

        viewModel.onReplaySignalClicked(signal)

        assertEquals(BleProtocol.buildSubGhzReplayCommand(signal), fakeBleManager.lastSentCommand)
    }

    @Test
    fun `bagli degilken sinyal gonderilmez`() = runTest {
        val signal = SubGhzSignal(pulsesBase64 = "AQIDBA==", frequencyHz = 433920000L)

        viewModel.onReplaySignalClicked(signal)

        assertNull(fakeBleManager.lastSentCommand)
    }

    @Test
    fun `baglantidayken oled metni gonderilince oled_text komutu yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onSendOledTextClicked("Merhaba")

        assertEquals(BleProtocol.buildOledTextCommand("Merhaba"), fakeBleManager.lastSentCommand)
    }

    @Test
    fun `bagli degilken oled metni gonderilmez`() = runTest {
        viewModel.onSendOledTextClicked("Merhaba")

        assertNull(fakeBleManager.lastSentCommand)
    }

    @Test
    fun `bos oled metni gonderilmez`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onSendOledTextClicked("   ")

        assertNull(fakeBleManager.lastSentCommand)
    }
}
