package com.massager.app.presentation.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object DeviceScan : Screen("device_scan")
    data object DeviceControl : Screen("device_control") {
        const val ARG_DEVICE_ID = "deviceId"
        const val ARG_DEVICE_NAME = "deviceName"
        const val ARG_DEVICE_MAC = "deviceMac"
        val routePattern: String =
            "device_control?$ARG_DEVICE_ID={$ARG_DEVICE_ID}&$ARG_DEVICE_NAME={$ARG_DEVICE_NAME}&$ARG_DEVICE_MAC={$ARG_DEVICE_MAC}"

        fun createRoute(deviceId: String, deviceName: String, deviceMac: String?): String {
            val encodedId = Uri.encode(deviceId)
            val encodedName = Uri.encode(deviceName)
            val encodedMac = Uri.encode(deviceMac ?: "")
            return "device_control?$ARG_DEVICE_ID=$encodedId&$ARG_DEVICE_NAME=$encodedName&$ARG_DEVICE_MAC=$encodedMac"
        }
    }
    data object AddDevice : Screen("add_device")
    data object ManualAddDevice : Screen("manual_add_device")
    data object Settings : Screen("settings")
    data object About : Screen("about")
    data object UserAgreement : Screen("user_agreement")
    data object PrivacyPolicy : Screen("privacy_policy")
    data object PersonalInfo : Screen("personal_info")
    data object Recovery : Screen("recovery")
    data object AccountSecurity : Screen("account_security")
    data object ChangePassword : Screen("change_password")
    data object DeleteAccount : Screen("delete_account")
    data object ForgetPassword : Screen("forget_password")
}
