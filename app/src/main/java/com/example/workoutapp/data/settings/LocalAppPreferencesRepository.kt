package com.example.workoutapp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.workoutapp.model.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.localAppPreferencesDataStore by preferencesDataStore(name = "local_app_preferences")

@Singleton
class LocalAppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val soundsEnabledKey = booleanPreferencesKey("sounds_enabled")
    private val soundVolumeKey = floatPreferencesKey("sound_volume")
    private val timerSoundTypeKey = stringPreferencesKey("timer_sound_type")
    private val restCompleteSoundTypeKey = stringPreferencesKey("rest_complete_sound_type")
    private val exerciseSwitchSoundTypeKey = stringPreferencesKey("exercise_switch_sound_type")
    private val celebrationSoundTypeKey = stringPreferencesKey("celebration_sound_type")
    private val vibrationEnabledKey = booleanPreferencesKey("vibration_enabled")
    private val finalCountdownEnabledKey = booleanPreferencesKey("final_countdown_enabled")
    private val silentModeBehaviorKey = stringPreferencesKey("silent_mode_behavior")
    private val tutorialCompletedKey = booleanPreferencesKey("tutorial_completed")
    private val tutorialVersionKey = intPreferencesKey("tutorial_version")
    private val sensorEnabledKey = booleanPreferencesKey("sensor_enabled")
    private val sensorIpAddressKey = stringPreferencesKey("sensor_ip_address")

    val settings: Flow<LocalAppSettings> = context.localAppPreferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            LocalAppSettings(
                themeMode = preferences[themeModeKey] ?: "dark",
                soundsEnabled = preferences[soundsEnabledKey] ?: true,
                soundVolume = preferences[soundVolumeKey] ?: 1.0f,
                timerSoundType = preferences[timerSoundTypeKey] ?: "beep",
                restCompleteSoundType = preferences[restCompleteSoundTypeKey] ?: "chime",
                exerciseSwitchSoundType = preferences[exerciseSwitchSoundTypeKey] ?: "loud",
                celebrationSoundType = preferences[celebrationSoundTypeKey] ?: "cheer",
                vibrationEnabled = preferences[vibrationEnabledKey] ?: true,
                finalCountdownEnabled = preferences[finalCountdownEnabledKey] ?: true,
                silentModeBehavior = preferences[silentModeBehaviorKey] ?: "respect",
                tutorialCompleted = preferences[tutorialCompletedKey] ?: false,
                tutorialVersion = preferences[tutorialVersionKey] ?: 1,
                sensorEnabled = preferences[sensorEnabledKey] ?: false,
                sensorIpAddress = preferences[sensorIpAddressKey] ?: "192.168.0.125"
            )
        }

    suspend fun setThemeMode(mode: String) {
        context.localAppPreferencesDataStore.edit { preferences ->
            preferences[themeModeKey] = mode
        }
    }

    suspend fun updateSoundSettings(
        enabled: Boolean? = null,
        volume: Float? = null,
        timerSoundType: String? = null,
        restCompleteSoundType: String? = null,
        exerciseSwitchSoundType: String? = null,
        celebrationSoundType: String? = null,
        vibrationEnabled: Boolean? = null,
        finalCountdownEnabled: Boolean? = null,
        silentModeBehavior: String? = null
    ) {
        context.localAppPreferencesDataStore.edit { preferences ->
            enabled?.let { preferences[soundsEnabledKey] = it }
            volume?.let { preferences[soundVolumeKey] = it }
            timerSoundType?.let { preferences[timerSoundTypeKey] = it }
            restCompleteSoundType?.let { preferences[restCompleteSoundTypeKey] = it }
            exerciseSwitchSoundType?.let { preferences[exerciseSwitchSoundTypeKey] = it }
            celebrationSoundType?.let { preferences[celebrationSoundTypeKey] = it }
            vibrationEnabled?.let { preferences[vibrationEnabledKey] = it }
            finalCountdownEnabled?.let { preferences[finalCountdownEnabledKey] = it }
            silentModeBehavior?.let { preferences[silentModeBehaviorKey] = it }
        }
    }

    suspend fun updateTutorialSettings(completed: Boolean? = null, version: Int? = null) {
        context.localAppPreferencesDataStore.edit { preferences ->
            completed?.let { preferences[tutorialCompletedKey] = it }
            version?.let { preferences[tutorialVersionKey] = it }
        }
    }

    suspend fun updateSensorSettings(enabled: Boolean? = null, ipAddress: String? = null) {
        context.localAppPreferencesDataStore.edit { preferences ->
            enabled?.let { preferences[sensorEnabledKey] = it }
            ipAddress?.let { preferences[sensorIpAddressKey] = it }
        }
    }

    suspend fun seedFromLegacySettingsIfUnset(settings: Settings) {
        context.localAppPreferencesDataStore.edit { preferences ->
            if (preferences[themeModeKey] == null) {
                preferences[themeModeKey] = settings.themeMode
            }
            if (preferences[soundsEnabledKey] == null) {
                preferences[soundsEnabledKey] = settings.soundsEnabled
            }
            if (preferences[soundVolumeKey] == null) {
                preferences[soundVolumeKey] = settings.soundVolume
            }
            if (preferences[timerSoundTypeKey] == null) {
                preferences[timerSoundTypeKey] = settings.timerSoundType
            }
            if (preferences[restCompleteSoundTypeKey] == null) {
                preferences[restCompleteSoundTypeKey] = settings.restCompleteSoundType
            }
            if (preferences[exerciseSwitchSoundTypeKey] == null) {
                preferences[exerciseSwitchSoundTypeKey] = settings.exerciseSwitchSoundType
            }
            if (preferences[celebrationSoundTypeKey] == null) {
                preferences[celebrationSoundTypeKey] = settings.celebrationSoundType
            }
            if (preferences[vibrationEnabledKey] == null) {
                preferences[vibrationEnabledKey] = settings.vibrationEnabled
            }
            if (preferences[finalCountdownEnabledKey] == null) {
                preferences[finalCountdownEnabledKey] = settings.finalCountdownEnabled
            }
            if (preferences[silentModeBehaviorKey] == null) {
                preferences[silentModeBehaviorKey] = settings.silentModeBehavior
            }
            if (preferences[tutorialCompletedKey] == null) {
                preferences[tutorialCompletedKey] = settings.tutorialCompleted
            }
            if (preferences[tutorialVersionKey] == null) {
                preferences[tutorialVersionKey] = settings.tutorialVersion
            }
            if (preferences[sensorEnabledKey] == null) {
                preferences[sensorEnabledKey] = settings.sensorEnabled
            }
            if (preferences[sensorIpAddressKey] == null) {
                preferences[sensorIpAddressKey] = settings.sensorIpAddress
            }
        }
    }
}
