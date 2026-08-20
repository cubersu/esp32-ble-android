package com.example.esp32zero.ui.navigation

/** Uygulamadaki her sayfanın (NavHost rotası) tanımı. */
enum class AppDestination(val route: String, val title: String) {
    HOME("home", "ESP32-MultiTool"),
    WIFI_SCAN("wifi_scan", "Wi-Fi Tarama"),
    BLE_SCAN("ble_scan", "BLE Tarama"),
    SUB_GHZ("sub_ghz", "Sub-GHz"),
    WIFI_CAPTURE("wifi_capture", "Wi-Fi Paket Yakalama"),
    DEAUTH("deauth", "Deauth"),
    OLED_TEXT("oled_text", "OLED Ekrana Yaz"),
    RESPONSE_LOG("response_log", "Yanıtlar (Debug)"),
}

/** Ana sayfadaki özellik menüsünde gösterilen, HOME dışındaki tüm sayfalar. */
val featureDestinations: List<AppDestination> = listOf(
    AppDestination.WIFI_SCAN,
    AppDestination.BLE_SCAN,
    AppDestination.SUB_GHZ,
    AppDestination.WIFI_CAPTURE,
    AppDestination.DEAUTH,
    AppDestination.OLED_TEXT,
    AppDestination.RESPONSE_LOG,
)
