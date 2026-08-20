package com.example.esp32zero.ble

/** ESP32'nin "wifi_scan" yanıtındaki tek bir Wi-Fi ağı. */
data class WifiNetwork(
    val ssid: String,
    val rssi: Int,
    val secure: Boolean,
)

/** ESP32'nin "ble_scan" yanıtındaki tek bir BLE cihazı. */
data class BleDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
)

/**
 * ESP32'nin "subghz_capture" yanıtındaki yakalanan ham Sub-GHz sinyali.
 * pulsesBase64, ham mikrosaniye darbe sürelerinin (uint16, little-endian)
 * base64 kodlanmış hâlidir; Android bu veriyi hiç çözmez (decode etmez),
 * yalnızca saklar ve "subghz_replay" ile aynen ESP32'ye geri gönderir.
 */
data class SubGhzSignal(
    val pulsesBase64: String,
    val frequencyHz: Long,
    val capturedAtMillis: Long = System.currentTimeMillis(),
)

/**
 * ESP32'nin "wifi_capture" yanıtından (parçalı bildirimler birleştirilerek)
 * elde edilen tam PCAP dosyası. SADECE KENDİ AĞINI TEST ETMEK İÇİN —
 * bkz. esp32-multitool deposundaki wifi_sniffer.h üstündeki yasal/etik not.
 */
data class WifiCapture(
    val pcapBytes: ByteArray,
    val packetCount: Int,
    val capturedAtMillis: Long = System.currentTimeMillis(),
) {
    // ByteArray, Kotlin'in ürettiği varsayılan equals/hashCode'da içerik
    // yerine referans karşılaştırır; data class'ın bunu elle geçersiz
    // kılması gerekiyor (lint/derleyici uyarısı bu yüzden bastırılmadı,
    // gerçekten kasıtlı).
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WifiCapture) return false
        return pcapBytes.contentEquals(other.pcapBytes) &&
            packetCount == other.packetCount &&
            capturedAtMillis == other.capturedAtMillis
    }

    override fun hashCode(): Int {
        var result = pcapBytes.contentHashCode()
        result = 31 * result + packetCount
        result = 31 * result + capturedAtMillis.hashCode()
        return result
    }
}

/** BleProtocol.parseWifiCaptureChunk()'ın döndürdüğü, ham bir "wifi_capture_chunk" parçası. */
data class WifiCaptureChunk(
    val captureId: Long,
    val seq: Int,
    val total: Int,
    val packetCount: Int,
    val chunkBase64: String,
)

/** ESP32'nin "rogue_ap_scan" yanıtındaki, belirli bir SSID'yi yayınlayan tek bir erişim noktası. */
data class RogueApEntry(
    val bssid: String,
    val rssi: Int,
    val secure: Boolean,
    val isKnown: Boolean,
)

/**
 * ESP32'nin "rogue_ap_scan" yanıtı: aranan SSID'yi yayınlayan tüm erişim
 * noktaları. suspicious, ya birden fazla BSSID bulunduğunda ya da
 * known_bssid verilip onunla eşleşmeyen bir BSSID bulunduğunda true olur.
 */
data class RogueApScanResult(
    val ssid: String,
    val accessPoints: List<RogueApEntry>,
    val suspicious: Boolean,
)

/** ESP32'nin "net_scan" yanıtındaki, açık portu bulunan tek bir canlı cihaz. */
data class NetScanHost(
    val ip: String,
    val openPorts: List<Int>,
)

/** ESP32'nin "net_scan" yanıtı. timedOut, tarama süresi dolduğunda (kısmi sonuç) true olur. */
data class NetScanResult(
    val localIp: String,
    val hosts: List<NetScanHost>,
    val timedOut: Boolean,
)

/** ESP32'nin "wps_check" yanıtı — belirli bir AP'nin beacon'ında WPS bilgi elemanının bulunup bulunmadığı. */
data class WpsCheckResult(
    val bssid: String,
    val wpsEnabled: Boolean,
)
