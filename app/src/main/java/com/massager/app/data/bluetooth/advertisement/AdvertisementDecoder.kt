package com.massager.app.data.bluetooth.advertisement

import android.bluetooth.le.ScanResult

interface AdvertisementDecoder {
    fun decode(result: ScanResult): ProtocolAdvertisement?
}
