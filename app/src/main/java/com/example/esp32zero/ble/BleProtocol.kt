package com.example.esp32zero.ble

import org.json.JSONException
import org.json.JSONObject

/**
 * ESP32 ile konuşulan JSON komut/yanıt protokolünün saf Kotlin kısmı.
 * Android BLE framework'üne bağımlı değildir, bu yüzden gerçek org.json ile
 * doğrudan unit test edilebilir. Alan adları placeholder'dır; ESP32 firmware'i
 * yazıldığında protokole göre güncellenmelidir.
 */
object BleProtocol {

    /** Basit bir "ping" komutu JSON string olarak oluşturur. */
    fun buildPingCommand(): String =
        JSONObject().apply {
            put("cmd", "ping")
        }.toString()

    /**
     * Gelen yanıtın "pong" olup olmadığını kontrol eder.
     * Bozuk/JSON olmayan girdilerde exception fırlatmak yerine false döner,
     * böylece hatalı BLE verisi uygulamayı çökertmez.
     */
    fun isPong(responseJson: String): Boolean =
        try {
            JSONObject(responseJson).optString("cmd") == "pong"
        } catch (e: JSONException) {
            false
        }
}
