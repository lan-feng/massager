package com.massager.app.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothManager @Inject constructor(
    private val adapter: BluetoothAdapter?
) {

    private val _isEnabled = MutableStateFlow(adapter?.isEnabled == true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    fun refreshState() {
        _isEnabled.value = adapter?.isEnabled == true
    }

    fun pairedDevices(): List<BluetoothDevice> {
        return adapter?.bondedDevices?.toList().orEmpty()
    }
}
