package com.massager.app.data.bluetooth

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MassagerBluetoothService @Inject constructor(
    private val bluetoothManager: BluetoothManager
) {

    fun scanNearbyDevices(): Flow<List<BluetoothDevice>> = flow {
        // Placeholder scan emitting paired devices after a short delay.
        delay(500)
        emit(bluetoothManager.pairedDevices())
    }
}
