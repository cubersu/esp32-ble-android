package com.example.esp32zero.ble

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
