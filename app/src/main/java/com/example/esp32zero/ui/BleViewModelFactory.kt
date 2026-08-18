package com.example.esp32zero.ui

import android.content.Context
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.esp32zero.ble.AndroidBleManager

/** [BleViewModel]'i gerçek [AndroidBleManager] ile kuran fabrika. */
fun bleViewModelFactory(context: Context) = viewModelFactory {
    initializer {
        BleViewModel(AndroidBleManager(context.applicationContext))
    }
}
