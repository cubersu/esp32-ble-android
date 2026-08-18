package com.example.esp32zero.ble

import android.Manifest
import android.os.Build
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class PermissionUtilsTest {

    @Test
    fun `API 31 oncesinde sadece fine location istenir`() {
        val permissions = PermissionUtils.requiredBlePermissions(sdkInt = Build.VERSION_CODES.R)

        assertArrayEquals(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), permissions)
    }

    @Test
    fun `API 31 ve sonrasinda scan ve connect istenir`() {
        val permissions = PermissionUtils.requiredBlePermissions(sdkInt = Build.VERSION_CODES.S)

        assertArrayEquals(
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
            permissions,
        )
    }
}
