package com.example.workoutapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.data.repository.SensorRepository
import com.example.workoutapp.data.repository.SessionHistoryRepository
import com.example.workoutapp.data.repository.SettingsRepository
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.SyncedWorkoutSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val exerciseRepository: ExerciseRepository,
    private val sessionHistoryRepository: SessionHistoryRepository,
    private val localAppPreferencesRepository: LocalAppPreferencesRepository,
    private val syncedWorkoutSettingsRepository: SyncedWorkoutSettingsRepository,
    private val soundManager: com.example.workoutapp.util.SoundManager,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(SettingsScreenState())
    val settings: StateFlow<SettingsScreenState> = _settings.asStateFlow()

    private val _sensorConnectionState = MutableStateFlow<String?>(null)
    val sensorConnectionState: StateFlow<String?> = _sensorConnectionState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            syncedWorkoutSettingsRepository.observeSessionSettings().collect { sessionSettings ->
                _settings.update {
                    it.copy(
                        restTimerDuration = sessionSettings.restTimerDuration,
                        exerciseSwitchDuration = sessionSettings.exerciseSwitchDuration,
                        undoLastSetEnabled = sessionSettings.undoLastSetEnabled
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.getSettings().collect { dbSettings ->
                dbSettings?.let { localAppPreferencesRepository.seedFromLegacySettingsIfUnset(it) }
            }
        }

        viewModelScope.launch {
            localAppPreferencesRepository.settings.collect { localSettings ->
                _settings.update {
                    it.copy(
                        themeMode = localSettings.themeMode,
                        soundsEnabled = localSettings.soundsEnabled,
                        soundVolume = localSettings.soundVolume,
                        timerSoundType = localSettings.timerSoundType,
                        celebrationSoundType = localSettings.celebrationSoundType,
                        tutorialCompleted = localSettings.tutorialCompleted,
                        tutorialVersion = localSettings.tutorialVersion,
                        sensorEnabled = localSettings.sensorEnabled,
                        sensorIpAddress = localSettings.sensorIpAddress
                    )
                }
            }
        }
    }

    fun toggleSounds(enabled: Boolean) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSoundSettings(enabled = enabled)
        }
    }

    fun setSoundVolume(volume: Float) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSoundSettings(volume = volume)
        }
    }

    fun setTimerSound(soundType: String) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSoundSettings(timerSoundType = soundType)
        }
    }

    fun setCelebrationSound(soundType: String) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSoundSettings(celebrationSoundType = soundType)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            localAppPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setRestTimerDuration(seconds: Int) {
        viewModelScope.launch {
            syncedWorkoutSettingsRepository.setRestTimerDuration(seconds)
        }
    }

    fun setExerciseSwitchDuration(seconds: Int) {
        viewModelScope.launch {
            syncedWorkoutSettingsRepository.setExerciseSwitchDuration(seconds)
        }
    }

    fun toggleUndoLastSet(enabled: Boolean) {
        viewModelScope.launch {
            syncedWorkoutSettingsRepository.setUndoLastSetEnabled(enabled)
        }
    }
    
    fun previewTimerSound(soundType: String) {
        soundManager.playTimerSound(
            soundType = soundType,
            volume = _settings.value.soundVolume,
            enabled = _settings.value.soundsEnabled
        )
    }
    
    fun previewCelebrationSound(soundType: String) {
        soundManager.playCelebrationSound(
            soundType = soundType,
            volume = _settings.value.soundVolume,
            enabled = _settings.value.soundsEnabled
        )
    }

    fun exportData(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            // Get all data
            val sessions = sessionHistoryRepository.getSessions().first()
            val exercises = exerciseRepository.getExercises().first()
            
            // Create CSV format
            val csv = buildString {
                appendLine("Workout Export")
                appendLine()
                appendLine("Sessions:")
                appendLine("Date,Duration (min),Weight Lifted (kg),Calories,Notes")
                sessions.forEach { session ->
                    appendLine("${session.date},${session.durationSeconds/60},${session.totalWeightLifted},${session.caloriesBurned},${session.notes ?: ""}")
                }
                appendLine()
                appendLine("Exercises:")
                appendLine("Name,Weight (kg),Reps,Sets")
                exercises.forEach { exercise ->
                    if (!exercise.isDeleted) {
                        appendLine("${exercise.name},${exercise.weight},${exercise.reps},${exercise.sets}")
                    }
                }
            }
            
            onComplete(csv)
        }
    }

    fun toggleSensor(enabled: Boolean) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSensorSettings(enabled = enabled)
        }
    }

    fun setSensorIpAddress(ipAddress: String) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSensorSettings(ipAddress = ipAddress)
        }
    }

    fun testSensorConnection() {
        viewModelScope.launch {
            _sensorConnectionState.value = "Testing..."
            val ipAddress = _settings.value.sensorIpAddress
            val isConnected = sensorRepository.testConnection(ipAddress)
            _sensorConnectionState.value = if (isConnected) "Connected ✓" else "Failed to connect"
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
