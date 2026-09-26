package com.example.workoutapp.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.notifications.ReminderScheduler
import com.example.workoutapp.data.notifications.isReminderEnabled
import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.data.repository.SensorRepository
import com.example.workoutapp.data.repository.SessionHistoryRepository
import com.example.workoutapp.data.settings.LegacySettingsBootstrapper
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.SyncedWorkoutSettingsRepository
import com.example.workoutapp.data.sync.SyncStatus
import com.example.workoutapp.data.sync.SyncStatusMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val legacySettingsBootstrapper: LegacySettingsBootstrapper,
    private val exerciseRepository: ExerciseRepository,
    private val sessionHistoryRepository: SessionHistoryRepository,
    private val localAppPreferencesRepository: LocalAppPreferencesRepository,
    private val syncedWorkoutSettingsRepository: SyncedWorkoutSettingsRepository,
    private val soundManager: com.example.workoutapp.util.SoundManager,
    private val sensorRepository: SensorRepository,
    syncStatusMonitor: SyncStatusMonitor
) : ViewModel() {

    private val _settings = MutableStateFlow(SettingsScreenState())
    val settings: StateFlow<SettingsScreenState> = _settings.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(false)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _sensorConnectionState = MutableStateFlow<String?>(null)
    val sensorConnectionState: StateFlow<String?> = _sensorConnectionState.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = syncStatusMonitor.status

    init {
        loadSettings()
        _remindersEnabled.value = isReminderEnabled(appContext)
    }

    fun setRemindersEnabled(enabled: Boolean) {
        _remindersEnabled.value = enabled
        ReminderScheduler.setEnabled(appContext, enabled)
    }

    private fun loadSettings() {
        viewModelScope.launch {
            syncedWorkoutSettingsRepository.observeSessionSettings().collect { sessionSettings ->
                _settings.update {
                    it.copy(
                        restTimerDuration = sessionSettings.restTimerDuration,
                        exerciseSwitchDuration = sessionSettings.exerciseSwitchDuration,
                        undoLastSetEnabled = sessionSettings.undoLastSetEnabled,
                        calorieIntensity = sessionSettings.calorieIntensity
                    )
                }
            }
        }

        viewModelScope.launch {
            legacySettingsBootstrapper.seedFromLegacySettingsIfPresent()
        }

        viewModelScope.launch {
            localAppPreferencesRepository.settings.collect { localSettings ->
                _settings.update {
                    it.copy(
                        themeMode = localSettings.themeMode,
                        soundsEnabled = localSettings.soundsEnabled,
                        soundVolume = localSettings.soundVolume,
                        timerSoundType = localSettings.timerSoundType,
                        restCompleteSoundType = localSettings.restCompleteSoundType,
                        exerciseSwitchSoundType = localSettings.exerciseSwitchSoundType,
                        celebrationSoundType = localSettings.celebrationSoundType,
                        vibrationEnabled = localSettings.vibrationEnabled,
                        finalCountdownEnabled = localSettings.finalCountdownEnabled,
                        silentModeBehavior = localSettings.silentModeBehavior,
                        tutorialCompleted = localSettings.tutorialCompleted,
                        tutorialVersion = localSettings.tutorialVersion,
                        sensorEnabled = localSettings.sensorEnabled,
                        sensorIpAddress = localSettings.sensorIpAddress,
                        showExerciseCounter = localSettings.showExerciseCounter,
                        showInlineRpe = localSettings.showInlineRpe,
                        showExerciseNotes = localSettings.showExerciseNotes
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

    fun setRestCompleteSound(soundType: String) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSoundSettings(restCompleteSoundType = soundType)
        }
    }

    fun setExerciseSwitchSound(soundType: String) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSoundSettings(exerciseSwitchSoundType = soundType)
        }
    }

    fun setCelebrationSound(soundType: String) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSoundSettings(celebrationSoundType = soundType)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSoundSettings(vibrationEnabled = enabled)
        }
    }

    fun setFinalCountdownEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSoundSettings(finalCountdownEnabled = enabled)
        }
    }

    fun setSilentModeBehavior(behavior: String) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSoundSettings(silentModeBehavior = behavior)
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

    fun setCalorieIntensity(intensity: String) {
        viewModelScope.launch {
            syncedWorkoutSettingsRepository.setCalorieIntensity(intensity)
        }
    }
    
    fun previewTimerSound(soundType: String) {
        soundManager.playTimerSound(
            soundType = soundType,
            volume = _settings.value.soundVolume,
            enabled = _settings.value.soundsEnabled,
            vibrationEnabled = _settings.value.vibrationEnabled,
            silentModeBehavior = _settings.value.silentModeBehavior
        )
    }
    
    fun previewCelebrationSound(soundType: String) {
        soundManager.playCelebrationSound(
            soundType = soundType,
            volume = _settings.value.soundVolume,
            enabled = _settings.value.soundsEnabled,
            vibrationEnabled = _settings.value.vibrationEnabled,
            silentModeBehavior = _settings.value.silentModeBehavior
        )
    }

    fun previewVibration() {
        soundManager.vibrateCue(_settings.value.vibrationEnabled)
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
                appendLine("Date,Duration (min),Weight Lifted (kg),Calories,Calorie Formula Version,Calorie Estimate Mode,Calorie Intensity,Calorie User Weight (kg),Calorie MET Correction Factor,Calorie Active Seconds,Calorie Rest Seconds,Notes")
                sessions.forEach { session ->
                    appendLine("${session.date},${session.durationSeconds/60},${session.totalWeightLifted},${session.caloriesBurned},${session.calorieFormulaVersion},${session.calorieEstimateMode},${session.calorieIntensity},${session.calorieUserWeightKg},${session.calorieMetCorrectionFactor},${session.calorieActiveSeconds},${session.calorieRestSeconds},${session.notes ?: ""}")
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

    fun setShowExerciseCounter(enabled: Boolean) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSessionScreenSettings(showExerciseCounter = enabled)
        }
    }

    fun setShowInlineRpe(enabled: Boolean) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSessionScreenSettings(showInlineRpe = enabled)
        }
    }

    fun setShowExerciseNotes(enabled: Boolean) {
        viewModelScope.launch {
            localAppPreferencesRepository.updateSessionScreenSettings(showExerciseNotes = enabled)
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

    fun discoverSensor() {
        viewModelScope.launch {
            _sensorConnectionState.value = "Searching local network..."
            val discoveredIp = sensorRepository.discoverSensor()
            if (discoveredIp == null) {
                _sensorConnectionState.value = "No ESP sensor found"
            } else {
                localAppPreferencesRepository.updateSensorSettings(
                    enabled = true,
                    ipAddress = discoveredIp
                )
                _sensorConnectionState.value = "Found ESP at $discoveredIp"
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
