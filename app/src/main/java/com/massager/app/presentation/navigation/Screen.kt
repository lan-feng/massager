package com.massager.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object DeviceScan : Screen("device_scan")
    data object DeviceControl : Screen("device_control")
    data object AddDevice : Screen("add_device")
    data object ManualAddDevice : Screen("manual_add_device")
    data object Settings : Screen("settings")
    data object PersonalInfo : Screen("personal_info")
    data object Recovery : Screen("recovery")
    data object AccountSecurity : Screen("account_security")
    data object ChangePassword : Screen("change_password")
    data object DeleteAccount : Screen("delete_account")
    data object ForgetPassword : Screen("forget_password")
}
