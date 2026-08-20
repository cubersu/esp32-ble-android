package com.example.esp32zero.ble

import org.json.JSONException
import org.json.JSONObject

/**
 * ESP32 ile konuşulan JSON komut/yanıt protokolünün saf Kotlin kısmı.
 * Android BLE framework'üne bağımlı değildir, bu yüzden gerçek org.json ile
 * doğrudan unit test edilebilir. Alan adları esp32-multitool deposundaki
 * command_protocol.h ile birebir eşleşir: yanıtlar {"status":..,"data"/"msg":..}
 * biçiminde gelir, komut adı yanıtta tekrarlanmaz. "data" alanı ping için
 * düz string ("pong"), tarama komutları için "type" alanlı bir JSON nesnesi
 * ({"type":"wifi_scan","networks":[...]} gibi) olabilir.
 */
object BleProtocol {

    /** Basit bir "ping" komutu JSON string olarak oluşturur. */
    fun buildPingCommand(): String =
        JSONObject().apply {
            put("cmd", "ping")
        }.toString()

    /** Yakındaki Wi-Fi ağlarını taramak için "wifi_scan" komutu oluşturur. */
    fun buildWifiScanCommand(): String =
        JSONObject().apply {
            put("cmd", "wifi_scan")
        }.toString()

    /** Yakındaki BLE cihazlarını taramak için "ble_scan" komutu oluşturur. */
    fun buildBleScanCommand(): String =
        JSONObject().apply {
            put("cmd", "ble_scan")
        }.toString()

    // 868/915MHz kasıtlı olarak listede değil: kullanıcının fiziksel CC1101
    // modülü "433MHz" için satılan, tek banda göre anten eşleştirilmiş bir
    // modül — çip firmware ile o frekanslara "kilitlenebilir" ama modülün
    // eşleştirme devresi/SAW filtresi onlara göre değil, pratikte menzil/
    // hassasiyet neredeyse sıfıra iner. Farklı (çok bantlı ya da 868/915'e
    // özel) bir modül alınırsa buraya geri eklenebilir.
    /** ESP32'nin desteklediği en yaygın Sub-GHz ISM/SRD frekansları (Hz). */
    val COMMON_SUBGHZ_FREQUENCIES_HZ: List<Long> = listOf(315_000_000L, 433_920_000L)

    const val DEFAULT_SUBGHZ_FREQUENCY_HZ: Long = 433_920_000L

    /**
     * Belirtilen frekansta (Hz) bir Sub-GHz sinyali yakalamak için
     * "subghz_capture" komutu oluşturur.
     */
    fun buildSubGhzCaptureCommand(
        frequencyHz: Long = DEFAULT_SUBGHZ_FREQUENCY_HZ,
        timeoutMs: Long = 15_000L,
    ): String =
        JSONObject().apply {
            put("cmd", "subghz_capture")
            put("frequency_hz", frequencyHz)
            put("timeout_ms", timeoutMs)
        }.toString()

    /**
     * Daha önce yakalanmış (veya kullanıcı tarafından saklanmış) bir
     * sinyali ESP32 üzerinden tekrar göndermek (replay) için "subghz_replay"
     * komutu oluşturur. Sinyalin yakalandığı frekansı (signal.frequencyHz)
     * kullanır, böylece replay her zaman doğru frekansta yapılır.
     */
    fun buildSubGhzReplayCommand(signal: SubGhzSignal): String =
        JSONObject().apply {
            put("cmd", "subghz_replay")
            put("pulses_b64", signal.pulsesBase64)
            put("frequency_hz", signal.frequencyHz)
        }.toString()

    /**
     * Belirtilen Wi-Fi kanalında (1-13) WPA/WPA2 el sıkışma (EAPOL)
     * paketlerini yakalamak için "wifi_capture" komutu oluşturur. SADECE
     * KENDİ AĞINI TEST ETMEK İÇİN — bkz. esp32-multitool deposundaki
     * wifi_sniffer.h üstündeki yasal/etik not.
     */
    fun buildWifiCaptureCommand(channel: Int, timeoutMs: Long = 15_000L): String =
        JSONObject().apply {
            put("cmd", "wifi_capture")
            put("channel", channel)
            put("timeout_ms", timeoutMs)
        }.toString()

    /**
     * ESP32'ye bağlı OLED ekranda (varsa) serbest bir metin göstermek için
     * "oled_text" komutu oluşturur. ESP32 tarafında OLED şu an yalnızca
     * durum ekranı modundaysa (ENABLE_OLED_STATUS) gösterilir; tam menü
     * modunda (ENABLE_LOCAL_CONTROLS) ya da OLED hiç aktif değilken komut
     * sessizce yok sayılır.
     */
    fun buildOledTextCommand(text: String): String =
        JSONObject().apply {
            put("cmd", "oled_text")
            put("text", text)
        }.toString()

    /**
     * Gelen yanıtın "pong" olup olmadığını kontrol eder.
     * ESP32 ping yanıtına {"status":"ok","data":"pong"} döner; "cmd" alanı
     * yanıtta bulunmaz. Bozuk/JSON olmayan girdilerde exception fırlatmak
     * yerine false döner, böylece hatalı BLE verisi uygulamayı çökertmez.
     */
    fun isPong(responseJson: String): Boolean =
        try {
            val json = JSONObject(responseJson)
            json.optString("status") == "ok" && json.optString("data") == "pong"
        } catch (e: JSONException) {
            false
        }

    /**
     * Yanıt bir "wifi_scan" sonucuysa ağ listesini döner, değilse (farklı bir
     * yanıt türü ya da bozuk JSON) null döner.
     */
    fun parseWifiScanResponse(responseJson: String): List<WifiNetwork>? =
        try {
            val data = responseJson.dataObjectOfType("wifi_scan")
            val networks = data?.optJSONArray("networks")
            networks?.let { array ->
                (0 until array.length()).map { index ->
                    val network = array.getJSONObject(index)
                    WifiNetwork(
                        ssid = network.optString("ssid"),
                        rssi = network.optInt("rssi"),
                        secure = network.optBoolean("secure"),
                    )
                }
            }
        } catch (e: JSONException) {
            null
        }

    /**
     * Yanıt bir "ble_scan" sonucuysa cihaz listesini döner, değilse (farklı
     * bir yanıt türü ya da bozuk JSON) null döner.
     */
    fun parseBleScanResponse(responseJson: String): List<BleDeviceInfo>? =
        try {
            val data = responseJson.dataObjectOfType("ble_scan")
            val devices = data?.optJSONArray("devices")
            devices?.let { array ->
                (0 until array.length()).map { index ->
                    val device = array.getJSONObject(index)
                    BleDeviceInfo(
                        name = device.optString("name"),
                        address = device.optString("address"),
                        rssi = device.optInt("rssi"),
                    )
                }
            }
        } catch (e: JSONException) {
            null
        }

    /**
     * Yanıt bir "subghz_capture" sonucuysa yakalanan sinyali döner, değilse
     * (farklı bir yanıt türü, bozuk JSON ya da boş "pulses_b64") null döner.
     */
    fun parseSubGhzCaptureResponse(responseJson: String): SubGhzSignal? =
        try {
            val data = responseJson.dataObjectOfType("subghz_capture")
            val pulsesBase64 = data?.optString("pulses_b64")
            if (pulsesBase64.isNullOrEmpty()) {
                null
            } else {
                SubGhzSignal(
                    pulsesBase64 = pulsesBase64,
                    frequencyHz = data.optLong("frequency_hz"),
                )
            }
        } catch (e: JSONException) {
            null
        }

    /**
     * Yanıt bir "wifi_capture_chunk" parçasıysa ham parçayı döner (henüz
     * birleştirilmemiş), değilse null döner. Birleştirme/yeniden derleme
     * mantığı burada değil, BleViewModel'de — bu fonksiyon durumsuz (stateless)
     * kalmalı, aksi halde gerçek org.json ile doğrudan test edilemez.
     */
    fun parseWifiCaptureChunk(responseJson: String): WifiCaptureChunk? =
        try {
            val data = responseJson.dataObjectOfType("wifi_capture_chunk")
            data?.let {
                WifiCaptureChunk(
                    captureId = it.optLong("capture_id"),
                    seq = it.optInt("seq"),
                    total = it.optInt("total"),
                    packetCount = it.optInt("packet_count"),
                    chunkBase64 = it.optString("chunk_b64"),
                )
            }
        } catch (e: JSONException) {
            null
        }

    /**
     * "status":"ok" olan ve "data" alanı verilen "type" değerine sahip bir
     * JSON nesnesi olan yanıtlarda o nesneyi döner; aksi halde null döner.
     */
    private fun String.dataObjectOfType(type: String): JSONObject? {
        val json = JSONObject(this)
        if (json.optString("status") != "ok") return null
        val data = json.optJSONObject("data") ?: return null
        if (data.optString("type") != type) return null
        return data
    }
}
