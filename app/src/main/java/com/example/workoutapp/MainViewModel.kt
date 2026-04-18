package com.example.workoutapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.repository.SettingsRepository
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.domain.startup.AppLaunchCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localAppPreferencesRepository: LocalAppPreferencesRepository,
    appLaunchCoordinator: AppLaunchCoordinator
) : ViewModel() {

    val startDestination: StateFlow<String?> = appLaunchCoordinator.startDestination()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode = localAppPreferencesRepository.settings

    init {
        migrateLegacyThemeIfNeeded()
    }

    private fun migrateLegacyThemeIfNeeded() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                settings?.let { localAppPreferencesRepository.seedFromLegacySettingsIfUnset(it) }
            }
        }
    }
}
