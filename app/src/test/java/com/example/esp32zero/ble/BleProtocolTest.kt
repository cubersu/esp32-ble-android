package com.example.esp32zero.ble

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BleProtocolTest {

    @Test
    fun `buildPingCommand gecerli json ve cmd alani icerir`() {
        val json = BleProtocol.buildPingCommand()

        val parsed = JSONObject(json)
        assertEquals("ping", parsed.getString("cmd"))
    }

    @Test
    fun `isPong gecerli pong yanitinda true doner`() {
        val response = JSONObject().apply { put("status", "ok"); put("data", "pong") }.toString()

        assertTrue(BleProtocol.isPong(response))
    }

    @Test
    fun `isPong pong olmayan yanitta false doner`() {
        val response = JSONObject().apply { put("status", "ok"); put("data", "something_else") }.toString()

        assertFalse(BleProtocol.isPong(response))
    }

    @Test
    fun `isPong error statuslu yanitta false doner`() {
        val response = JSONObject().apply { put("status", "error"); put("msg", "unknown command") }.toString()

        assertFalse(BleProtocol.isPong(response))
    }

    @Test
    fun `isPong bozuk json girdisinde exception firlatmadan false doner`() {
        assertFalse(BleProtocol.isPong("bu gecerli bir json degil"))
    }

    @Test
    fun `buildWifiScanCommand gecerli json ve cmd alani icerir`() {
        val json = BleProtocol.buildWifiScanCommand()

        assertEquals("wifi_scan", JSONObject(json).getString("cmd"))
    }

    @Test
    fun `buildBleScanCommand gecerli json ve cmd alani icerir`() {
        val json = BleProtocol.buildBleScanCommand()

        assertEquals("ble_scan", JSONObject(json).getString("cmd"))
    }

    @Test
    fun `parseWifiScanResponse gecerli yanitta ag listesini doner`() {
        val response = """
            {"status":"ok","data":{"type":"wifi_scan","networks":[
                {"ssid":"EvAgi","rssi":-45,"secure":true},
                {"ssid":"Misafir","rssi":-70,"secure":false}
            ]}}
        """.trimIndent()

        val networks = BleProtocol.parseWifiScanResponse(response)

        assertEquals(
            listOf(
                WifiNetwork("EvAgi", -45, true),
                WifiNetwork("Misafir", -70, false),
            ),
            networks,
        )
    }

    @Test
    fun `parseWifiScanResponse pong yanitinda null doner`() {
        val response = JSONObject().apply { put("status", "ok"); put("data", "pong") }.toString()

        assertNull(BleProtocol.parseWifiScanResponse(response))
    }

    @Test
    fun `parseWifiScanResponse bozuk json girdisinde null doner`() {
        assertNull(BleProtocol.parseWifiScanResponse("bu gecerli bir json degil"))
    }

    @Test
    fun `parseBleScanResponse gecerli yanitta cihaz listesini doner`() {
        val response = """
            {"status":"ok","data":{"type":"ble_scan","devices":[
                {"name":"ESP32-MultiTool","address":"AA:BB:CC:DD:EE:FF","rssi":-50}
            ]}}
        """.trimIndent()

        val devices = BleProtocol.parseBleScanResponse(response)

        assertEquals(
            listOf(BleDeviceInfo("ESP32-MultiTool", "AA:BB:CC:DD:EE:FF", -50)),
            devices,
        )
    }

    @Test
    fun `parseBleScanResponse wifi_scan yanitinda null doner`() {
        val response = """{"status":"ok","data":{"type":"wifi_scan","networks":[]}}"""

        assertNull(BleProtocol.parseBleScanResponse(response))
    }

    @Test
    fun `buildSubGhzCaptureCommand gecerli json cmd frekans ve timeout icerir`() {
        val json = BleProtocol.buildSubGhzCaptureCommand(frequencyHz = 868_000_000L, timeoutMs = 20_000L)

        val parsed = JSONObject(json)
        assertEquals("subghz_capture", parsed.getString("cmd"))
        assertEquals(868_000_000L, parsed.getLong("frequency_hz"))
        assertEquals(20_000L, parsed.getLong("timeout_ms"))
    }

    @Test
    fun `buildSubGhzCaptureCommand varsayilan frekans 433_92MHz'dir`() {
        val json = BleProtocol.buildSubGhzCaptureCommand()

        assertEquals(433_920_000L, JSONObject(json).getLong("frequency_hz"))
    }

    @Test
    fun `buildSubGhzReplayCommand sinyalin pulses_b64 ve frekansini tasir`() {
        val signal = SubGhzSignal(pulsesBase64 = "AQIDBA==", frequencyHz = 868_000_000L)

        val json = BleProtocol.buildSubGhzReplayCommand(signal)

        val parsed = JSONObject(json)
        assertEquals("subghz_replay", parsed.getString("cmd"))
        assertEquals("AQIDBA==", parsed.getString("pulses_b64"))
        assertEquals(868_000_000L, parsed.getLong("frequency_hz"))
    }

    @Test
    fun `parseSubGhzCaptureResponse gecerli yanitta sinyali doner`() {
        val response = """
            {"status":"ok","data":{"type":"subghz_capture","frequency_hz":433920000,"pulses_b64":"AQIDBA=="}}
        """.trimIndent()

        val signal = BleProtocol.parseSubGhzCaptureResponse(response)

        assertEquals("AQIDBA==", signal?.pulsesBase64)
        assertEquals(433920000L, signal?.frequencyHz)
    }

    @Test
    fun `parseSubGhzCaptureResponse bos pulses_b64 icin null doner`() {
        val response = """{"status":"ok","data":{"type":"subghz_capture","frequency_hz":433920000,"pulses_b64":""}}"""

        assertNull(BleProtocol.parseSubGhzCaptureResponse(response))
    }

    @Test
    fun `parseSubGhzCaptureResponse wifi_scan yanitinda null doner`() {
        val response = """{"status":"ok","data":{"type":"wifi_scan","networks":[]}}"""

        assertNull(BleProtocol.parseSubGhzCaptureResponse(response))
    }

    @Test
    fun `parseSubGhzCaptureResponse error statuslu yanitta null doner`() {
        val response = """{"status":"error","msg":"no signal captured"}"""

        assertNull(BleProtocol.parseSubGhzCaptureResponse(response))
    }

    @Test
    fun `buildOledTextCommand gecerli json cmd ve text alani icerir`() {
        val json = BleProtocol.buildOledTextCommand("Merhaba")

        val parsed = JSONObject(json)
        assertEquals("oled_text", parsed.getString("cmd"))
        assertEquals("Merhaba", parsed.getString("text"))
    }

    @Test
    fun `buildWifiCaptureCommand gecerli json cmd kanal ve timeout icerir`() {
        val json = BleProtocol.buildWifiCaptureCommand(channel = 6, timeoutMs = 10_000L)

        val parsed = JSONObject(json)
        assertEquals("wifi_capture", parsed.getString("cmd"))
        assertEquals(6, parsed.getInt("channel"))
        assertEquals(10_000L, parsed.getLong("timeout_ms"))
    }

    @Test
    fun `buildWifiCaptureCommand varsayilan timeout 15sn'dir`() {
        val json = BleProtocol.buildWifiCaptureCommand(channel = 1)

        assertEquals(15_000L, JSONObject(json).getLong("timeout_ms"))
    }

    @Test
    fun `parseWifiCaptureChunk gecerli yanitta parcayi doner`() {
        val response = """
            {"status":"ok","data":{"type":"wifi_capture_chunk","capture_id":12345,"seq":1,"total":3,
                "packet_count":4,"chunk_b64":"AQIDBA=="}}
        """.trimIndent()

        val chunk = BleProtocol.parseWifiCaptureChunk(response)

        assertEquals(12345L, chunk?.captureId)
        assertEquals(1, chunk?.seq)
        assertEquals(3, chunk?.total)
        assertEquals(4, chunk?.packetCount)
        assertEquals("AQIDBA==", chunk?.chunkBase64)
    }

    @Test
    fun `parseWifiCaptureChunk wifi_scan yanitinda null doner`() {
        val response = """{"status":"ok","data":{"type":"wifi_scan","networks":[]}}"""

        assertNull(BleProtocol.parseWifiCaptureChunk(response))
    }

    @Test
    fun `parseWifiCaptureChunk bozuk json girdisinde null doner`() {
        assertNull(BleProtocol.parseWifiCaptureChunk("bu gecerli bir json degil"))
    }
}
