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
        val response = JSONObject().apply { put("cmd", "pong") }.toString()

        assertTrue(BleProtocol.isPong(response))
    }

    @Test
    fun `isPong pong olmayan yanitta false doner`() {
        val response = JSONObject().apply { put("cmd", "status") }.toString()

        assertFalse(BleProtocol.isPong(response))
    }

    @Test
    fun `isPong bozuk json girdisinde exception firlatmadan false doner`() {
        assertFalse(BleProtocol.isPong("bu gecerli bir json degil"))
    }
}
