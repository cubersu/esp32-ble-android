package com.example.esp32zero.ble

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Yakalanan bir [WifiCapture]'ı context.cacheDir/pcap altına .pcap dosyası
 * olarak yazıp, FileProvider content:// URI'si ile bir paylaşım (share sheet)
 * Intent'i oluşturur. Böylece yakalama Wireshark/aircrack-ng gibi harici
 * araçlara aktarılabilir. SADECE KENDİ AĞINI TEST ETMEK İÇİN — bkz.
 * esp32-multitool deposundaki wifi_sniffer.h üstündeki yasal/etik not.
 */
object WifiCaptureExporter {

    private val fileTimeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun buildShareIntent(context: Context, capture: WifiCapture): Intent {
        val pcapDir = File(context.cacheDir, "pcap").apply { mkdirs() }
        val file = File(pcapDir, "esp32_capture_${fileTimeFormat.format(capture.capturedAtMillis)}.pcap")
        file.writeBytes(capture.pcapBytes)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.tcpdump.pcap"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "PCAP dosyasını paylaş")
    }
}
