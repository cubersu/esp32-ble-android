package com.example.esp32zero.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * BLE için gerekli runtime izinlerinin API seviyesine göre hesaplanması.
 * [sdkInt] parametresi, testlerde Build.VERSION.SDK_INT'e bağımlı olmadan
 * her iki dalı da (API 31 öncesi/sonrası) doğrulayabilmek için enjekte edilebilir.
 */
object PermissionUtils {

    fun requiredBlePermissions(sdkInt: Int = Build.VERSION.SDK_INT): Array<String> =
        if (sdkInt >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun hasAllBlePermissions(context: Context, sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        requiredBlePermissions(sdkInt).all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    fun hasScanPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }

    fun hasConnectPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            // API 31 öncesinde BLUETOOTH/BLUETOOTH_ADMIN "normal" korumalıdır, kurulumda otomatik verilir.
            true
        }
}
