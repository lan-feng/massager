package com.massager.app.presentation.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.massager.app.data.repository.MassagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DeviceViewModel @Inject constructor(
    repository: MassagerRepository
) : ViewModel() {
    val pairedDevices: StateFlow<List<String>> = repository.pairedDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
