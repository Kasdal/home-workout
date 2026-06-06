package com.example.workoutapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.remote.UpdateChecker
import com.example.workoutapp.data.remote.UpdateInfo
import com.example.workoutapp.data.settings.LegacySettingsBootstrapper
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.UpdateCheckPreferences
import com.example.workoutapp.data.storage.LegacyPhotoMigrator
import com.example.workoutapp.domain.startup.AppEntryState
import com.example.workoutapp.domain.startup.AppLaunchCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val legacySettingsBootstrapper: LegacySettingsBootstrapper,
    private val localAppPreferencesRepository: LocalAppPreferencesRepository,
    private val updateCheckPreferences: UpdateCheckPreferences,
    private val updateChecker: UpdateChecker,
    @ApplicationContext private val appContext: Context,
    private val legacyPhotoMigrator: LegacyPhotoMigrator,
    appLaunchCoordinator: AppLaunchCoordinator
) : ViewModel() {

    val appEntryState: StateFlow<AppEntryState?> = appLaunchCoordinator.appEntryState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode = localAppPreferencesRepository.settings

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    private val _showWhatsNew = MutableStateFlow(false)
    val showWhatsNew: StateFlow<Boolean> = _showWhatsNew

    private val _whatsNewChangelog = MutableStateFlow("")
    val whatsNewChangelog: StateFlow<String> = _whatsNewChangelog

    init {
        migrateLegacyThemeIfNeeded()
        legacyPhotoMigrator.start(viewModelScope)
    }

    private fun migrateLegacyThemeIfNeeded() {
        viewModelScope.launch {
            legacySettingsBootstrapper.seedFromLegacySettingsIfPresent()
        }
    }

    fun onAppReady() {
        viewModelScope.launch {
            checkForUpdate()
            checkWhatNew()
        }
    }

    private suspend fun checkForUpdate() {
        val result = updateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
        if (result != null) {
            updateCheckPreferences.setCachedChangelog(result.version, result.changelog)
            _updateInfo.value = result
        }
    }

    private suspend fun checkWhatNew() {
        val currentVersion = BuildConfig.VERSION_NAME
        val lastSeen = updateCheckPreferences.lastSeenVersion.first()
        if (lastSeen != currentVersion) {
            updateCheckPreferences.setLastSeenVersion(currentVersion)
            val cachedMd = updateCheckPreferences.cachedChangelogMd.first()
            val fallback = appContext.getString(R.string.changelog_fallback)
            _whatsNewChangelog.value = cachedMd ?: fallback
            _showWhatsNew.value = true
        }
    }

    fun dismissUpdateSheet() {
        _updateInfo.value = null
    }

    fun skipVersion(version: String) {
        viewModelScope.launch {
            updateCheckPreferences.setSkippedVersion(version)
            _updateInfo.value = null
        }
    }

    fun dismissWhatsNew() {
        _showWhatsNew.value = false
    }
}
