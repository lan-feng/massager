package com.massager.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.massager.app.presentation.auth.AuthViewModel
import com.massager.app.presentation.auth.LoginScreen
import com.massager.app.presentation.auth.ForgetPasswordScreen
import com.massager.app.presentation.auth.ForgetPasswordViewModel
import com.massager.app.presentation.auth.RegisterScreen
import com.massager.app.presentation.device.AddDeviceScreen
import com.massager.app.presentation.device.AddDeviceViewModel
import com.massager.app.presentation.device.DeviceControlScreen
import com.massager.app.presentation.device.DeviceControlViewModel
import com.massager.app.presentation.device.DeviceScanScreen
import com.massager.app.presentation.device.DeviceViewModel
import com.massager.app.presentation.device.ManualAddDeviceScreen
import com.massager.app.presentation.device.ManualAddDeviceViewModel
import com.massager.app.presentation.home.AppBottomTab
import com.massager.app.presentation.home.HomeDashboardScreen
import com.massager.app.presentation.home.HomeViewModel
import com.massager.app.presentation.recovery.RecoverySelectionScreen
import com.massager.app.presentation.settings.AccountSecurityScreen
import com.massager.app.presentation.settings.AccountSecurityViewModel
import com.massager.app.presentation.settings.ChangePasswordScreen
import com.massager.app.presentation.settings.DeleteAccountConfirmScreen
import com.massager.app.presentation.settings.PersonalInformationScreen
import com.massager.app.presentation.settings.PersonalInformationViewModel
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
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onForgotPassword = { navController.navigate(Screen.ForgetPassword.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                state = authState.value,
                onRegister = { name, email, password, code ->
                    authViewModel.register(name, email, password, code)
                },
                onSendVerificationCode = { email ->
                    authViewModel.sendRegisterVerificationCode(email)
                },
                onNavigateToLogin = { navController.popBackStack() },
                onRegistrationHandled = {
                    authViewModel.clearRegistrationFlag()
                    authViewModel.clearError()
                }
            )
        }
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            val homeState = viewModel.uiState.collectAsStateWithLifecycle()
            HomeDashboardScreen(
                state = homeState.value,
                effects = viewModel.effects,
                currentTab = AppBottomTab.Home,
                onAddDevice = { navController.navigate(Screen.AddDevice.route) },
                onDeviceToggle = { device -> viewModel.toggleDeviceSelection(device.id) },
                onDeviceOpen = { device ->
                    navController.navigate(
                        Screen.DeviceControl.createRoute(
                            device.id,
                            device.name,
                            device.macAddress
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
                onRenameClick = viewModel::showRenameDialog,
                onRemoveClick = viewModel::showRemoveDialog,
                onCancelManagement = viewModel::cancelManagement,
                onRenameInputChanged = viewModel::onRenameInputChanged,
                onRenameConfirm = viewModel::confirmRename,
                onRenameDismiss = viewModel::dismissRenameDialog,
                onRemoveConfirm = viewModel::confirmRemove,
                onRemoveDismiss = viewModel::dismissRemoveDialog,
                onDismissError = viewModel::clearError,
                onTabSelected = { tab ->
                    when (tab) {
                        AppBottomTab.Home -> Unit
                        AppBottomTab.Manual -> {
                            navController.navigate(Screen.Recovery.route) {
                                launchSingleTop = true
                            }
                        }
                        AppBottomTab.Devices -> {
                            navController.navigate(Screen.DeviceScan.route) {
                                launchSingleTop = true
                            }
                        }
                        AppBottomTab.Profile -> {
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
            )
        }
        composable(Screen.DeviceScan.route) {
            val viewModel: DeviceViewModel = hiltViewModel()
            DeviceScanScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.DeviceControl.routePattern,
            arguments = listOf(
                navArgument(Screen.DeviceControl.ARG_DEVICE_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Screen.DeviceControl.ARG_DEVICE_NAME) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Screen.DeviceControl.ARG_DEVICE_MAC) {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) {
            val viewModel: DeviceControlViewModel = hiltViewModel()
            DeviceControlScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddDevice.route) {
            val viewModel: AddDeviceViewModel = hiltViewModel()
            AddDeviceScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateManual = { navController.navigate(Screen.ManualAddDevice.route) },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ManualAddDevice.route) {
            val viewModel: ManualAddDeviceViewModel = hiltViewModel()
            ManualAddDeviceScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val settingsState = viewModel.uiState.collectAsStateWithLifecycle()
            SettingsScreen(
                state = settingsState.value,
                currentTab = AppBottomTab.Profile,
                onTabSelected = { tab ->
                    when (tab) {
                        AppBottomTab.Home -> navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Settings.route) { inclusive = true }
                        }
                        AppBottomTab.Profile -> Unit
                        else -> Unit
                    }
                },
                onToggleTemperature = viewModel::toggleTemperatureUnit,
                onClearCache = viewModel::clearCache,
                onUpdateName = viewModel::updateUserName,
                onUpdateAvatar = viewModel::updateAvatar,
                onNavigatePersonalInfo = { navController.navigate(Screen.PersonalInfo.route) },
                onNavigateAccountSecurity = { navController.navigate(Screen.AccountSecurity.route) },
                onNavigateHistory = { /* TODO */ },
                onNavigateFavorites = { /* TODO */ },
                onNavigateAbout = { /* TODO */ },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                    authViewModel.clearAuthenticationFlag()
                },
                onConsumeToast = viewModel::consumeToast
            )
        }
        composable(Screen.Recovery.route) {
            RecoverySelectionScreen(options = emptyList())
        }
        composable(Screen.ForgetPassword.route) {
            val viewModel: ForgetPasswordViewModel = hiltViewModel()
            val state = viewModel.uiState.collectAsStateWithLifecycle()
            ForgetPasswordScreen(
                state = state.value,
                onBack = { navController.popBackStack() },
                onSendCode = viewModel::sendCode,
                onSubmit = viewModel::resetPassword,
                onConsumeToast = viewModel::consumeToast,
                onConsumeError = viewModel::consumeError,
                onConsumeSnackbar = viewModel::consumeSnackbar,
                onPasswordResetSuccess = {
                    authViewModel.clearAuthenticationFlag()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.PersonalInfo.route) {
            val viewModel: PersonalInformationViewModel = hiltViewModel()
            PersonalInformationScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AccountSecurity.route) {
            val viewModel: AccountSecurityViewModel = hiltViewModel()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(uiState.value.logoutCompleted) {
                if (uiState.value.logoutCompleted) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                    viewModel.consumeLogoutNavigation()
                }
            }
            AccountSecurityScreen(
                state = uiState.value,
                onBack = { navController.popBackStack() },
                onSetPassword = { navController.navigate(Screen.ChangePassword.route) },
                onDeleteAccount = { navController.navigate(Screen.DeleteAccount.route) },
                onRequestLogout = { viewModel.toggleLogoutDialog(true) },
                onConfirmLogout = { viewModel.logout() },
                onDismissLogoutDialog = { viewModel.toggleLogoutDialog(false) }
            )
        }
        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onBack = { navController.popBackStack() },
                onPasswordChanged = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                    authViewModel.clearAuthenticationFlag()
                },
                onForgotPassword = { navController.navigate(Screen.ForgetPassword.route) }
            )
        }
        composable(Screen.DeleteAccount.route) {
            DeleteAccountConfirmScreen(
                onBack = { navController.popBackStack() },
                onConfirm = { navController.popBackStack() }
            )
        }
    }
}
