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

    @Test
    fun `baglantidayken wifi paket yakala tiklaninca wifi_capture komutu yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onCaptureWifiClicked(6)

        assertEquals(BleProtocol.buildWifiCaptureCommand(6), fakeBleManager.lastSentCommand)
    }

    @Test
    fun `bagli degilken wifi paket yakala komutu gonderilmez`() = runTest {
        viewModel.onCaptureWifiClicked(6)

        assertNull(fakeBleManager.lastSentCommand)
    }

    @Test
    fun `tek parcali wifi_capture_chunk yaniti dogrudan yakalama listesine eklenir`() = runTest {
        // "AQI=" base64 -> [0x01, 0x02]
        fakeBleManager.emitResponse(
            """{"status":"ok","data":{"type":"wifi_capture_chunk","capture_id":1,"seq":0,"total":1,
                "packet_count":2,"chunk_b64":"AQI="}}""",
        )

        val captures = viewModel.wifiCaptures.value
        assertEquals(1, captures.size)
        assertEquals(2, captures.single().packetCount)
        assertTrue(captures.single().pcapBytes.contentEquals(byteArrayOf(0x01, 0x02)))
    }

    @Test
    fun `coklu parcali wifi_capture_chunk yanitlari sira ile birlestirilir`() = runTest {
        // Tam base64 "AQIDBA==" ([0x01,0x02,0x03,0x04]) iki parcaya bolunuyor: "AQI" + "DBA=="
        fakeBleManager.emitResponse(
            """{"status":"ok","data":{"type":"wifi_capture_chunk","capture_id":42,"seq":0,"total":2,
                "packet_count":4,"chunk_b64":"AQI"}}""",
        )
        assertTrue(viewModel.wifiCaptures.value.isEmpty())

        fakeBleManager.emitResponse(
            """{"status":"ok","data":{"type":"wifi_capture_chunk","capture_id":42,"seq":1,"total":2,
                "packet_count":4,"chunk_b64":"DBA=="}}""",
        )

        val captures = viewModel.wifiCaptures.value
        assertEquals(1, captures.size)
        assertEquals(4, captures.single().packetCount)
        assertTrue(captures.single().pcapBytes.contentEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04)))
    }

    @Test
    fun `bozuk base64 iceren wifi_capture_chunk hata mesaji yazar ve listeye eklenmez`() = runTest {
        fakeBleManager.emitResponse(
            """{"status":"ok","data":{"type":"wifi_capture_chunk","capture_id":7,"seq":0,"total":1,
                "packet_count":1,"chunk_b64":"bu gecerli bir base64 degil ??"}}""",
        )

        assertTrue(viewModel.wifiCaptures.value.isEmpty())
        assertEquals("Yakalanan Wi-Fi verisi bozuk geldi", viewModel.errorMessage.value)
    }

    @Test
    fun `baglantidayken gecerli bssid ile deauth gonderilince wifi_deauth komutu yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onSendDeauthClicked(bssid = "AA:BB:CC:DD:EE:FF", clientMac = "", channel = 6)

        assertEquals(
            BleProtocol.buildWifiDeauthCommand(bssid = "AA:BB:CC:DD:EE:FF", channel = 6),
            fakeBleManager.lastSentCommand,
        )
    }

    @Test
    fun `deauth istemci mac verilince onu kullanir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onSendDeauthClicked(bssid = "AA:BB:CC:DD:EE:FF", clientMac = "11:22:33:44:55:66", channel = 1)

        assertEquals(
            BleProtocol.buildWifiDeauthCommand(
                bssid = "AA:BB:CC:DD:EE:FF",
                channel = 1,
                clientMac = "11:22:33:44:55:66",
            ),
            fakeBleManager.lastSentCommand,
        )
    }

    @Test
    fun `gecersiz bssid ile deauth komutu gonderilmez ve hata yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onSendDeauthClicked(bssid = "gecersiz", clientMac = "", channel = 1)

        assertNull(fakeBleManager.lastSentCommand)
        assertEquals("Geçersiz MAC adresi (AA:BB:CC:DD:EE:FF biçiminde olmalı)", viewModel.errorMessage.value)
    }

    @Test
    fun `bagli degilken deauth komutu gonderilmez`() = runTest {
        viewModel.onSendDeauthClicked(bssid = "AA:BB:CC:DD:EE:FF", clientMac = "", channel = 1)

        assertNull(fakeBleManager.lastSentCommand)
    }

    @Test
    fun `baglantidayken sahte AP tara tiklaninca rogue_ap_scan komutu yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onScanRogueApClicked(ssid = "EvAgi", knownBssid = "AA:BB:CC:DD:EE:FF")

        assertEquals(
            BleProtocol.buildRogueApScanCommand(ssid = "EvAgi", knownBssid = "AA:BB:CC:DD:EE:FF"),
            fakeBleManager.lastSentCommand,
        )
    }

    @Test
    fun `bos ssid ile sahte AP taramasi gonderilmez`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onScanRogueApClicked(ssid = "  ", knownBssid = "")

        assertNull(fakeBleManager.lastSentCommand)
    }

    @Test
    fun `gelen rogue_ap_scan yaniti sonucu gunceller`() = runTest {
        fakeBleManager.emitResponse(
            """{"status":"ok","data":{"type":"rogue_ap_scan","ssid":"EvAgi","suspicious":false,"access_points":[]}}""",
        )

        assertEquals("EvAgi", viewModel.rogueApResult.value?.ssid)
    }

    @Test
    fun `baglantidayken gecerli bssid ile wps kontrolu tiklaninca wps_check komutu yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onCheckWpsClicked(bssid = "AA:BB:CC:DD:EE:FF", channel = 6)

        assertEquals(
            BleProtocol.buildWpsCheckCommand(bssid = "AA:BB:CC:DD:EE:FF", channel = 6),
            fakeBleManager.lastSentCommand,
        )
    }

    @Test
    fun `gecersiz bssid ile wps kontrolu komutu gonderilmez ve hata yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onCheckWpsClicked(bssid = "gecersiz", channel = 1)

        assertNull(fakeBleManager.lastSentCommand)
        assertEquals("Geçersiz MAC adresi (AA:BB:CC:DD:EE:FF biçiminde olmalı)", viewModel.errorMessage.value)
    }

    @Test
    fun `gelen wps_check yaniti sonucu gunceller`() = runTest {
        fakeBleManager.emitResponse(
            """{"status":"ok","data":{"type":"wps_check","bssid":"AA:BB:CC:DD:EE:FF","wps_enabled":true}}""",
        )

        assertEquals(true, viewModel.wpsCheckResult.value?.wpsEnabled)
    }

    @Test
    fun `baglantidayken ag tara tiklaninca net_scan komutu yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onNetScanClicked(ssid = "EvAgi", password = "gizli123")

        assertEquals(
            BleProtocol.buildNetScanCommand(ssid = "EvAgi", password = "gizli123"),
            fakeBleManager.lastSentCommand,
        )
    }

    @Test
    fun `bos ssid ile ag taramasi gonderilmez`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onNetScanClicked(ssid = "  ", password = "")

        assertNull(fakeBleManager.lastSentCommand)
    }

    @Test
    fun `gelen net_scan yaniti sonucu gunceller`() = runTest {
        fakeBleManager.emitResponse(
            """{"status":"ok","data":{"type":"net_scan","local_ip":"192.168.1.50","timed_out":false,"hosts":[]}}""",
        )

        assertEquals("192.168.1.50", viewModel.netScanResult.value?.localIp)
    }
}
