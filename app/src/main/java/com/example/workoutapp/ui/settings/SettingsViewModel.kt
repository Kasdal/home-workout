package com.example.workoutapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.local.entity.Settings
import com.example.workoutapp.data.repository.SensorRepository
import com.example.workoutapp.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val soundManager: com.example.workoutapp.util.SoundManager,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _sensorConnectionState = MutableStateFlow<String?>(null)
    val sensorConnectionState: StateFlow<String?> = _sensorConnectionState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getSettings().collect { dbSettings ->
                _settings.value = dbSettings ?: Settings()
            }
        }
    }

    fun toggleSounds(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(soundsEnabled = enabled)
            repository.saveSettings(updated)
        }
    }

    fun setSoundVolume(volume: Float) {
        viewModelScope.launch {
            val updated = _settings.value.copy(soundVolume = volume)
            repository.saveSettings(updated)
        }
    }

    fun setTimerSound(soundType: String) {
        viewModelScope.launch {
            val updated = _settings.value.copy(timerSoundType = soundType)
            repository.saveSettings(updated)
        }
    }

    fun setCelebrationSound(soundType: String) {
        viewModelScope.launch {
            val updated = _settings.value.copy(celebrationSoundType = soundType)
            repository.saveSettings(updated)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            val updated = _settings.value.copy(themeMode = mode)
            repository.saveSettings(updated)
        }
    }

    fun setRestTimerDuration(seconds: Int) {
        viewModelScope.launch {
            val updated = _settings.value.copy(restTimerDuration = seconds)
            repository.saveSettings(updated)
        }
    }

    fun setExerciseSwitchDuration(seconds: Int) {
        viewModelScope.launch {
            val updated = _settings.value.copy(exerciseSwitchDuration = seconds)
            repository.saveSettings(updated)
        }
    }

    fun toggleUndoLastSet(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(undoLastSetEnabled = enabled)
            repository.saveSettings(updated)
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
            val sessions = repository.getSessions().first()
            val exercises = repository.getExercises().first()
            
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
            val updated = _settings.value.copy(sensorEnabled = enabled)
            repository.saveSettings(updated)
        }
    }

    fun setSensorIpAddress(ipAddress: String) {
        viewModelScope.launch {
            val updated = _settings.value.copy(sensorIpAddress = ipAddress)
            repository.saveSettings(updated)
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
