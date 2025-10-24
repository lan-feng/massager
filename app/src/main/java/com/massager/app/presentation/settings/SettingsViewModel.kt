package com.massager.app.presentation.settings

import androidx.lifecycle.ViewModel
import com.massager.app.domain.usecase.settings.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    fun logout() {
        logoutUseCase()
    }
}
