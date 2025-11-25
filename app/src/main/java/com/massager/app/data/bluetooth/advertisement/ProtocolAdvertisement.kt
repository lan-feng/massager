package com.massager.app.data.bluetooth.advertisement

// 文件说明：描述蓝牙广播的协议包装与解析辅助类。
data class ProtocolAdvertisement(
    val productId: Int?,
    val firmwareVersion: String?,
    val uniqueId: String?
)
