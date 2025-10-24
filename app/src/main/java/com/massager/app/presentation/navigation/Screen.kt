package com.massager.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object DeviceScan : Screen("device_scan")
    data object Settings : Screen("settings")
    data object Recovery : Screen("recovery")
}
