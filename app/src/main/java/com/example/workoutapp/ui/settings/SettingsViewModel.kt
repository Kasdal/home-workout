package com.example.workoutapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.local.entity.Settings
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
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

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
}
