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
    fun `buildSubGhzCaptureCommand gecerli json cmd ve timeout icerir`() {
        val json = BleProtocol.buildSubGhzCaptureCommand(timeoutMs = 20_000L)

        val parsed = JSONObject(json)
        assertEquals("subghz_capture", parsed.getString("cmd"))
        assertEquals(20_000L, parsed.getLong("timeout_ms"))
    }

    @Test
    fun `buildSubGhzReplayCommand sinyalin pulses_b64 alanini tasir`() {
        val signal = SubGhzSignal(pulsesBase64 = "AQIDBA==", frequencyHz = 433920000L)

        val json = BleProtocol.buildSubGhzReplayCommand(signal)

        val parsed = JSONObject(json)
        assertEquals("subghz_replay", parsed.getString("cmd"))
        assertEquals("AQIDBA==", parsed.getString("pulses_b64"))
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
}
