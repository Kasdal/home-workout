package com.example.workoutapp.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.WorkoutSession
import com.example.workoutapp.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val soundManager: com.example.workoutapp.util.SoundManager
) : ViewModel() {

    // Exercises from DB
    val exercises = repository.getExercises()

    // Timer State (rest timer between sets/exercises)
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _isTimerPaused = MutableStateFlow(false)
    val isTimerPaused: StateFlow<Boolean> = _isTimerPaused.asStateFlow()

    private var timerJob: Job? = null

    // Custom timer durations
    private val _restTimerDuration = MutableStateFlow(30)
    val restTimerDuration: StateFlow<Int> = _restTimerDuration.asStateFlow()

    private val _exerciseSwitchDuration = MutableStateFlow(90)
    val exerciseSwitchDuration: StateFlow<Int> = _exerciseSwitchDuration.asStateFlow()

    // Session State
    private val _sessionStarted = MutableStateFlow(false)
    val sessionStarted: StateFlow<Boolean> = _sessionStarted.asStateFlow()

    // Session elapsed time (total time since session started)
    private val _sessionElapsedSeconds = MutableStateFlow(0)
    val sessionElapsedSeconds: StateFlow<Int> = _sessionElapsedSeconds.asStateFlow()

    private var sessionTimerJob: Job? = null

    // Map of ExerciseId -> Number of Completed Sets (0-4)
    private val _completedSets = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val completedSets: StateFlow<Map<Int, Int>> = _completedSets.asStateFlow()

    private var sessionStartTime = 0L
    
    // Sound settings cache
    private var soundsEnabled = true
    private var soundVolume = 1.0f
    private var timerSoundType = "beep"
    private var celebrationSoundType = "cheer"

    init {
        initializeDefaultExercises()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            repository.getSettings().collect { settings ->
                if (settings != null) {
                    _restTimerDuration.value = settings.restTimerDuration
                    _exerciseSwitchDuration.value = settings.exerciseSwitchDuration
                    soundsEnabled = settings.soundsEnabled
                    soundVolume = settings.soundVolume
                    timerSoundType = settings.timerSoundType
                    celebrationSoundType = settings.celebrationSoundType
                }
            }
        }
    }

    private fun initializeDefaultExercises() {
        viewModelScope.launch {
            if (repository.getExercises().first().isEmpty()) {
                val defaults = listOf(
                    "Bench Press", "Squat", "Deadlift", "Overhead Press",
                    "Barbell Row", "Pull Up", "Dips", "Bicep Curl",
                    "Tricep Extension", "Lateral Raise", "Calf Raise"
                )
                defaults.forEach { name ->
                    repository.addExercise(Exercise(name = name, weight = 20f))
                }
            }
        }
    }

    // --- Session Management ---
    fun startSession() {
        _sessionStarted.value = true
        sessionStartTime = System.currentTimeMillis()
        _sessionElapsedSeconds.value = 0
        
        // Start session timer
        sessionTimerJob = viewModelScope.launch {
            while (_sessionStarted.value) {
                delay(1000L)
                _sessionElapsedSeconds.value++
            }
        }
    }

    fun completeSession(onComplete: (WorkoutSession) -> Unit) {
        viewModelScope.launch {
            sessionTimerJob?.cancel()
            
            val endTime = System.currentTimeMillis()
            val duration = _sessionElapsedSeconds.value.toLong()
            
            // Calculate Stats
            val exerciseList = exercises.first()
            var totalWeight = 0f
            var totalVolume = 0f
            
            _completedSets.value.forEach { (exId, setCount) ->
                val exercise = exerciseList.find { it.id == exId }
                if (exercise != null) {
                    val weight = setCount * exercise.reps * exercise.weight
                    totalWeight += weight
                    totalVolume += weight
                }
            }

            // Calorie Calc
            val userMetrics = repository.getUserMetrics().first()
            val weightKg = userMetrics?.weightKg ?: 70f
            val hours = duration / 3600f
            val calories = 5.0f * weightKg * hours

            val session = WorkoutSession(
                date = endTime,
                durationSeconds = duration,
                totalWeightLifted = totalWeight,
                caloriesBurned = calories,
                totalVolume = totalVolume
            )
            
            val sessionId = repository.saveSession(session)
            
            // NEW: Save individual exercise details
            val sessionExercises = exerciseList.mapIndexed { index, exercise ->
                val completedSets = _completedSets.value[exercise.id] ?: 0
                if (completedSets > 0) {
                    com.example.workoutapp.data.local.entity.SessionExercise(
                        sessionId = sessionId.toInt(),
                        exerciseName = exercise.name,
                        weight = exercise.weight,
                        sets = completedSets,
                        reps = exercise.reps,
                        volume = completedSets * exercise.reps * exercise.weight,
                        sortOrder = index
                    )
                } else null
            }.filterNotNull()
            
            if (sessionExercises.isNotEmpty()) {
                repository.saveSessionExercises(sessionExercises)
            }
            
            // Reset state
            _completedSets.value = emptyMap()
            _sessionStarted.value = false
            _sessionElapsedSeconds.value = 0
            
            // Play celebration sound for completing the workout!
            soundManager.playCelebrationSound(celebrationSoundType, soundVolume, soundsEnabled)
            
            onComplete(session.copy(id = sessionId.toInt()))
        }
    }
    
    // Pause session timer
    fun pauseSession() {
        sessionTimerJob?.cancel()
    }
    
    // Resume session timer
    fun resumeSession() {
        if (_sessionStarted.value) {
            sessionTimerJob = viewModelScope.launch {
                while (_sessionStarted.value) {
                    delay(1000L)
                    _sessionElapsedSeconds.value++
                }
            }
        }
    }

    // --- Timer Logic ---
    fun setRestTimerDuration(seconds: Int) {
        _restTimerDuration.value = seconds
        viewModelScope.launch {
            val currentSettings = repository.getSettings().first() ?: com.example.workoutapp.data.local.entity.Settings()
            repository.saveSettings(currentSettings.copy(restTimerDuration = seconds))
        }
    }

    fun setExerciseSwitchDuration(seconds: Int) {
        _exerciseSwitchDuration.value = seconds
        viewModelScope.launch {
            val currentSettings = repository.getSettings().first() ?: com.example.workoutapp.data.local.entity.Settings()
            repository.saveSettings(currentSettings.copy(exerciseSwitchDuration = seconds))
        }
    }

    fun startTimer(seconds: Int) {
        timerJob?.cancel()
        _timerSeconds.value = seconds
        _isTimerRunning.value = true
        _isTimerPaused.value = false
        startTimerJob()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _isTimerPaused.value = true
        _isTimerRunning.value = false
    }

    fun resumeTimer() {
        _isTimerPaused.value = false
        _isTimerRunning.value = true
        startTimerJob()
    }

    private fun startTimerJob() {
        timerJob = viewModelScope.launch {
            while (_timerSeconds.value > 0) {
                val remaining = _timerSeconds.value
                if (remaining <= 3) {
                    soundManager.playTimerSound(timerSoundType, soundVolume, soundsEnabled)
                }
                delay(1000L)
                _timerSeconds.value--
            }
            // Timer finished - just beep, no celebration
            soundManager.playTimerSound(timerSoundType, soundVolume, soundsEnabled)
            _isTimerRunning.value = false
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _isTimerPaused.value = false
    }

    // --- Set Completion Logic ---
    fun completeNextSet(exerciseId: Int) {
        viewModelScope.launch {
            val current = _completedSets.value.toMutableMap()
            val currentCount = current[exerciseId] ?: 0
            
            // Get the actual exercise to check its set count
            val exercise = exercises.first().find { it.id == exerciseId }
            val maxSets = exercise?.sets ?: 4
            
            if (currentCount < maxSets) {
                val newCount = currentCount + 1
                current[exerciseId] = newCount
                _completedSets.value = current
                
                // Auto-start timer after set
                if (newCount >= maxSets) {
                    // Last set - start 90s timer for exercise switch
                    startTimer(_exerciseSwitchDuration.value)
                } else {
                    // Regular set - start 30s rest timer
                    startTimer(_restTimerDuration.value)
                }
            }
        }
    }
    
    // Undo last set
    fun undoSet(exerciseId: Int) {
        val current = _completedSets.value.toMutableMap()
        val currentCount = current[exerciseId] ?: 0
        if (currentCount > 0) {
            current[exerciseId] = currentCount - 1
            _completedSets.value = current
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.updateExercise(exercise)
        }
    }

    fun addExercise() {
        viewModelScope.launch {
            repository.addExercise(Exercise(name = "New Exercise", weight = 0f))
        }
    }

    fun deleteExercise(exerciseId: Int) {
        viewModelScope.launch {
            repository.deleteExercise(exerciseId)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
