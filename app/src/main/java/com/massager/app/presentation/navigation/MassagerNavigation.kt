package com.massager.app.presentation.navigation

// 文件说明：构建应用导航图，连接各功能模块的路由。
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.massager.app.R
import com.massager.app.core.preferences.LanguageManager
import com.massager.app.presentation.auth.AuthViewModel
import com.massager.app.presentation.auth.LoginScreen
import com.massager.app.presentation.auth.ForgetPasswordScreen
import com.massager.app.presentation.auth.ForgetPasswordViewModel
import com.massager.app.presentation.auth.RegisterScreen
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
import com.massager.app.presentation.settings.AboutScreen
import com.massager.app.presentation.settings.AccountSecurityScreen
import com.massager.app.presentation.settings.AccountSecurityViewModel
import com.massager.app.presentation.settings.ChangePasswordScreen
import com.massager.app.presentation.settings.DeleteAccountConfirmScreen
import com.massager.app.presentation.settings.DeleteAccountViewModel
import com.massager.app.presentation.settings.PersonalInformationScreen
import com.massager.app.presentation.settings.PersonalInformationViewModel
import com.massager.app.presentation.settings.SettingsScreen
import com.massager.app.presentation.settings.SettingsViewModel
import com.massager.app.presentation.settings.WebDocumentScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import java.util.Locale
import android.os.LocaleList
import android.content.res.Configuration
import kotlinx.coroutines.flow.collectLatest

private enum class GoogleLoginSource { AuthFlow, AccountSecurity }

private const val DEVICE_SCAN_RESULT_KEY = "device_scan_result_serial"

@Composable
fun MassagerNavHost(
    navController: NavHostController = rememberNavController(),
    pendingRoute: String? = null
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState = authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var preservedLocales by remember { mutableStateOf(configuration.locales) }

    val googleSignInClient = remember {
        val webClientId = context.getString(R.string.default_web_client_id)
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
        )
    }

    var googleLoginSource by remember { mutableStateOf(GoogleLoginSource.AuthFlow) }
    var onExternalGoogleBindSuccess by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onExternalGoogleBindFailed by remember { mutableStateOf<((String?) -> Unit)?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Google 登录返回后强制恢复持久化语言，避免外部 Activity 覆盖为英文
        val localesToRestore = LanguageManager.getPersistedLocales(context.applicationContext)
        if (localesToRestore.size() > 0) {
            LocaleList.setDefault(localesToRestore)
            Locale.setDefault(localesToRestore[0])
            val resources = context.applicationContext.resources
            val config = Configuration(resources.configuration).apply { setLocales(localesToRestore) }
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
        preservedLocales = localesToRestore
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                authViewModel.onExternalAuthFailed(
                    "Google sign-in failed: missing token",
                    resetAuth = googleLoginSource == GoogleLoginSource.AuthFlow
                )
            } else {
                authViewModel.loginWithGoogle(
                    idToken,
                    onSuccess = onExternalGoogleBindSuccess,
                    onFailure = onExternalGoogleBindFailed,
                    resetAuth = googleLoginSource == GoogleLoginSource.AuthFlow
                )
            }
        } catch (exception: ApiException) {
            authViewModel.onExternalAuthFailed(
                "Google sign-in was cancelled or failed (${exception.statusCode})",
                resetAuth = googleLoginSource == GoogleLoginSource.AuthFlow
            )
        } catch (throwable: Throwable) {
            authViewModel.onExternalAuthFailed(
                throwable.message ?: "Google sign-in failed",
                resetAuth = googleLoginSource == GoogleLoginSource.AuthFlow
            )
        }
        googleLoginSource = GoogleLoginSource.AuthFlow
        onExternalGoogleBindSuccess = null
        onExternalGoogleBindFailed = null
    }

    val authStartRoutes = remember {
        setOf(Screen.Login.route, Screen.Register.route, Screen.ForgetPassword.route)
    }
    var lastAuthState by remember { mutableStateOf(authState.value.isAuthenticated) }
    LaunchedEffect(authState.value.isAuthenticated) {
        val current = authState.value.isAuthenticated
        val previous = lastAuthState
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (!previous && current && currentRoute in authStartRoutes && googleLoginSource == GoogleLoginSource.AuthFlow) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
        lastAuthState = current
    }

    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { target ->
            // Defer navigation until the graph is ready
            navController.navigate(target) {
                popUpTo(target) { inclusive = false }
                launchSingleTop = true
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
                onForgotPassword = { navController.navigate(Screen.ForgetPassword.route) },
                onGuestLogin = { authViewModel.enterGuestMode() },
                onGoogleLogin = {
                    googleLoginSource = GoogleLoginSource.AuthFlow
                    preservedLocales = configuration.locales
                    authViewModel.beginGoogleLogin()
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                onOpenUserAgreement = { navController.navigate(Screen.UserAgreement.route) },
                onOpenPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onConsumeError = authViewModel::clearError
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
                },
                onOpenUserAgreement = { navController.navigate(Screen.UserAgreement.route) },
                onOpenPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
            )
        }
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            val homeState = viewModel.uiState.collectAsStateWithLifecycle()
            HomeDashboardScreen(
                state = homeState.value,
                effects = viewModel.effects,
                currentTab = AppBottomTab.Home,
                onAddDevice = {
                    val excludedSerials = homeState.value.devices.mapNotNull { it.macAddress }
                    navController.navigate(
                        Screen.DeviceScan.createRoute(
                            source = DeviceScanSource.HOME,
                            excludedSerials = excludedSerials
                        )
                    )
                },
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
                        AppBottomTab.Profile -> {
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        }
                        else -> Unit
                    }
                }
            )
        }
        composable(
            Screen.DeviceScan.routePattern,
            arguments = listOf(
                navArgument(Screen.DeviceScan.ARG_SOURCE) {
                    type = NavType.StringType
                    defaultValue = DeviceScanSource.HOME.name
                },
                navArgument(Screen.DeviceScan.ARG_EXCLUDED) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Screen.DeviceScan.ARG_OWNER_DEVICE_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            val viewModel: DeviceViewModel = hiltViewModel()
            DeviceScanScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateControl = { serial ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        DEVICE_SCAN_RESULT_KEY,
                        serial.orEmpty()
                    )
                    navController.popBackStack()
                }
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
        ) { backStackEntry ->
            val viewModel: DeviceControlViewModel = hiltViewModel()
            val comboResultFlow = backStackEntry.savedStateHandle.getStateFlow(
                DEVICE_SCAN_RESULT_KEY,
                ""
            )
            LaunchedEffect(comboResultFlow) {
                comboResultFlow.collectLatest { serial ->
                    if (!serial.isNullOrBlank()) {
                        viewModel.handleComboResult(serial)
                        backStackEntry.savedStateHandle[DEVICE_SCAN_RESULT_KEY] = ""
                    }
                }
            }
            DeviceControlScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAddDevice = { excludedSerials ->
                    val ownerId = backStackEntry.arguments
                        ?.getString(Screen.DeviceControl.ARG_DEVICE_ID)
                        .orEmpty()
                    navController.navigate(
                        Screen.DeviceScan.createRoute(
                            source = DeviceScanSource.CONTROL,
                            ownerDeviceId = ownerId,
                            excludedSerials = excludedSerials
                        )
                    )
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
                onSelectTheme = viewModel::setTheme,
                onSelectLanguage = viewModel::setLanguage,
                onClearCache = viewModel::clearCache,
                onUpdateName = viewModel::updateUserName,
                onUpdateAvatar = viewModel::updateAvatar,
                onNavigatePersonalInfo = { navController.navigate(Screen.PersonalInfo.route) },
                onNavigateAccountSecurity = { navController.navigate(Screen.AccountSecurity.route) },
                onNavigateHistory = { /* TODO */ },
                onNavigateFavorites = { /* TODO */ },
                onNavigateAbout = { navController.navigate(Screen.About.route) },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                    authViewModel.clearAuthenticationFlag()
                },
                onGuestRestricted = viewModel::showGuestRestriction,
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
                },
                onOpenUserAgreement = { navController.navigate(Screen.UserAgreement.route) },
                onOpenPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
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
                    authViewModel.clearAuthenticationFlag()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                    viewModel.consumeLogoutNavigation()
                }
            }
            AccountSecurityScreen(
                state = uiState.value,
                onBack = { navController.popBackStack() },
                onSetPassword = {
                    navController.navigate(
                        Screen.ChangePassword.createRoute(requireOldPassword = uiState.value.hasPassword)
                    )
                },
                onDeleteAccount = { navController.navigate(Screen.DeleteAccount.route) },
                onRequestLogout = { viewModel.toggleLogoutDialog(true) },
                onConfirmLogout = { viewModel.logout() },
                onDismissLogoutDialog = { viewModel.toggleLogoutDialog(false) },
                onBindGoogle = {
                    googleLoginSource = GoogleLoginSource.AccountSecurity
                    preservedLocales = configuration.locales
                    onExternalGoogleBindSuccess = viewModel::onExternalBindSuccess
                    onExternalGoogleBindFailed = viewModel::onExternalBindFailed
                    viewModel.onExternalBindStart()
                    authViewModel.beginGoogleLogin()
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                onUnbind = viewModel::unbind,
                onConsumeUnbindResult = viewModel::consumeUnbindResult,
                onConsumeBindResult = viewModel::consumeBindResult
            )
        }
        composable(
            Screen.ChangePassword.routePattern,
            arguments = listOf(
                navArgument(Screen.ChangePassword.ARG_REQUIRE_OLD) {
                    type = NavType.BoolType
                    defaultValue = true
                }
            )
        ) { backStackEntry ->
            val requireOldPassword =
                backStackEntry.arguments?.getBoolean(Screen.ChangePassword.ARG_REQUIRE_OLD) ?: true
            ChangePasswordScreen(
                requireOldPassword = requireOldPassword,
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
        composable(Screen.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onOpenUserAgreement = { navController.navigate(Screen.UserAgreement.route) },
                onOpenPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
            )
        }
        composable(Screen.UserAgreement.route) {
            WebDocumentScreen(
                title = stringResource(R.string.user_agreement),
                url = stringResource(R.string.user_agreement_url),
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PrivacyPolicy.route) {
            WebDocumentScreen(
                title = stringResource(R.string.privacy_policy),
                url = stringResource(R.string.privacy_policy_url),
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.DeleteAccount.route) {
            val viewModel: DeleteAccountViewModel = hiltViewModel()
            DeleteAccountConfirmScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    authViewModel.clearAuthenticationFlag()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
