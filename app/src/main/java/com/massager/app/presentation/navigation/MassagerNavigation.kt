package com.massager.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.massager.app.presentation.auth.AuthViewModel
import com.massager.app.presentation.auth.LoginScreen
import com.massager.app.presentation.auth.RegisterScreen
import com.massager.app.presentation.device.DeviceScanScreen
import com.massager.app.presentation.device.DeviceViewModel
import com.massager.app.presentation.home.HomeDashboardScreen
import com.massager.app.presentation.home.HomeViewModel
import com.massager.app.presentation.recovery.RecoverySelectionScreen
import com.massager.app.presentation.settings.SettingsScreen
import com.massager.app.presentation.settings.SettingsViewModel

@Composable
fun MassagerNavHost(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState = authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(authState.value.isAuthenticated) {
        if (authState.value.isAuthenticated) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (authState.value.isAuthenticated) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                state = authState.value,
                onLogin = { email, password -> authViewModel.login(email, password) },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                state = authState.value,
                onRegister = { name, email, password ->
                    authViewModel.register(name, email, password)
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            val homeState = viewModel.uiState.collectAsStateWithLifecycle()
            HomeDashboardScreen(
                state = homeState.value,
                onRefresh = viewModel::refresh,
                onDismissError = viewModel::clearError
            )
        }
        composable(Screen.DeviceScan.route) {
            val viewModel: DeviceViewModel = hiltViewModel()
            DeviceScanScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Recovery.route) {
            RecoverySelectionScreen(options = emptyList())
        }
    }
}
