package com.example.workoutapp.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.ExerciseSessionMode
import com.example.workoutapp.data.local.entity.ExerciseType
import com.example.workoutapp.data.local.entity.WorkoutSession
import com.example.workoutapp.data.repository.SensorRepository
import com.example.workoutapp.data.repository.WorkoutRepository
import com.example.workoutapp.util.CalorieCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val soundManager: com.example.workoutapp.util.SoundManager,
    private val sensorRepository: SensorRepository
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

    // Sensor settings cache
    private var sensorEnabled = false
    private var sensorIpAddress = "192.168.0.125"

    // Sensor state
    private val _sensorReps = MutableStateFlow(0)
    val sensorReps: StateFlow<Int> = _sensorReps.asStateFlow()

    private val _sensorState = MutableStateFlow("REST")
    val sensorState: StateFlow<String> = _sensorState.asStateFlow()

    private val _sensorDistance = MutableStateFlow(0)
    val sensorDistance: StateFlow<Int> = _sensorDistance.asStateFlow()

    private val _sensorConnected = MutableStateFlow(false)
    val sensorConnected: StateFlow<Boolean> = _sensorConnected.asStateFlow()

    private var sensorPollingJob: Job? = null
    private var lastSensorReps = 0

    private val _activeExerciseId = MutableStateFlow<Int?>(null)
    val activeExerciseId: StateFlow<Int?> = _activeExerciseId.asStateFlow()

    private val _activeExerciseMode = MutableStateFlow(ExerciseSessionMode.MANUAL_REPS)
    val activeExerciseMode: StateFlow<ExerciseSessionMode> = _activeExerciseMode.asStateFlow()

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
                    sensorEnabled = settings.sensorEnabled
                    sensorIpAddress = settings.sensorIpAddress
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
                    repository.addExercise(
                        Exercise(
                            name = name,
                            weight = 20f,
                            exerciseType = com.example.workoutapp.data.local.entity.ExerciseType.STANDARD.name,
                            usesSensor = true,
                            holdDurationSeconds = 30
                        )
                    )
                }
            }
        }
    }

    // --- Session Management ---
    fun startSession() {
        _sessionStarted.value = true
        sessionStartTime = System.currentTimeMillis()
        _sessionElapsedSeconds.value = 0
        refreshActiveExerciseState()
        
        // Start session timer
        sessionTimerJob = viewModelScope.launch {
            while (_sessionStarted.value) {
                delay(1000L)
                _sessionElapsedSeconds.value++
            }
        }

        if (sensorEnabled) {
            startSensorPolling()
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
                    val weight = when (exercise.exerciseType) {
                        com.example.workoutapp.data.local.entity.ExerciseType.HOLD.name -> {
                            val holdRepsEquivalent = (exercise.holdDurationSeconds / 5f).roundToInt().coerceAtLeast(1)
                            setCount * holdRepsEquivalent * 1f
                        }

                        com.example.workoutapp.data.local.entity.ExerciseType.BODYWEIGHT.name -> {
                            val userWeight = repository.getUserMetrics().first()?.weightKg ?: 70f
                            setCount * exercise.reps * userWeight
                        }

                        else -> setCount * exercise.reps * exercise.weight
                    }
                    totalWeight += weight
                    totalVolume += weight
                }
            }

            val userMetrics = repository.getUserMetrics().first()
            val calories = CalorieCalculator.calculateCalories(
                durationSeconds = duration.toInt(),
                completedSets = _completedSets.value,
                exercises = exerciseList,
                userMetrics = userMetrics
            )

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
                    val displayReps = if (exercise.exerciseType == com.example.workoutapp.data.local.entity.ExerciseType.HOLD.name) {
                        exercise.holdDurationSeconds
                    } else {
                        exercise.reps
                    }

                    com.example.workoutapp.data.local.entity.SessionExercise(
                        sessionId = sessionId.toInt(),
                        exerciseName = exercise.name,
                        weight = exercise.weight,
                        sets = completedSets,
                        reps = displayReps,
                        volume = when (exercise.exerciseType) {
                            com.example.workoutapp.data.local.entity.ExerciseType.HOLD.name -> {
                                val holdRepsEquivalent = (exercise.holdDurationSeconds / 5f).roundToInt().coerceAtLeast(1)
                                completedSets * holdRepsEquivalent * 1f
                            }

                            com.example.workoutapp.data.local.entity.ExerciseType.BODYWEIGHT.name -> {
                                val userWeight = repository.getUserMetrics().first()?.weightKg ?: 70f
                                completedSets * exercise.reps * userWeight
                            }

                            else -> completedSets * exercise.reps * exercise.weight
                        },
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
            _activeExerciseId.value = null
            _activeExerciseMode.value = ExerciseSessionMode.MANUAL_REPS

            stopSensorPolling()
            
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
                refreshActiveExerciseState()
                
                // Auto-start timer after set
                if (exercise?.exerciseType == com.example.workoutapp.data.local.entity.ExerciseType.HOLD.name) {
                    startTimer(exercise.holdDurationSeconds)
                } else if (newCount >= maxSets) {
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
            refreshActiveExerciseState()
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.updateExercise(exercise)
        }
    }
    
    fun updateExercisePhoto(exerciseId: Int, photoUri: String) {
        viewModelScope.launch {
            val exerciseList = exercises.first()
            val exercise = exerciseList.find { it.id == exerciseId }
            exercise?.let {
                repository.updateExercise(it.copy(photoUri = photoUri))
            }
        }
    }

    fun addExercise() {
        viewModelScope.launch {
            repository.addExercise(Exercise(name = "New Exercise", weight = 0f))
        }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.addExercise(exercise)
        }
    }

    fun deleteExercise(exerciseId: Int) {
        viewModelScope.launch {
            repository.deleteExercise(exerciseId)
        }
    }

    private fun startSensorPolling() {
        sensorPollingJob?.cancel()
        sensorPollingJob = viewModelScope.launch {
            sensorRepository.pollSensorStatus(sensorIpAddress, 200)
                .catch { emitAll(flowOf(null)) }
                .collect { sensorData ->
                if (sensorData != null) {
                    _sensorConnected.value = true
                    _sensorReps.value = sensorData.reps
                    _sensorState.value = sensorData.state
                    _sensorDistance.value = sensorData.dist

                    if (sensorData.reps > lastSensorReps && sensorData.reps > 0) {
                        checkAndCompleteSet(sensorData.reps)
                    }
                    lastSensorReps = sensorData.reps
                } else {
                    _sensorConnected.value = false
                }
            }
        }
    }

    private fun stopSensorPolling() {
        sensorPollingJob?.cancel()
        sensorPollingJob = null
        _sensorConnected.value = false
        _sensorReps.value = 0
        _sensorState.value = "REST"
        _sensorDistance.value = 0
        lastSensorReps = 0
    }

    private suspend fun checkAndCompleteSet(currentReps: Int) {
        val exerciseList = exercises.first()
        val incompleteExercise = exerciseList.firstOrNull { exercise ->
            val completedSets = _completedSets.value[exercise.id] ?: 0
            completedSets < exercise.sets &&
                exercise.usesSensor &&
                exercise.exerciseType != ExerciseType.HOLD.name
        }

        incompleteExercise?.let { exercise ->
            if (currentReps >= exercise.reps) {
                completeNextSet(exercise.id)

                viewModelScope.launch {
                    delay(1000)
                    sensorRepository.resetCounter(sensorIpAddress)
                    lastSensorReps = 0
                    _sensorReps.value = 0
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopSensorPolling()
        soundManager.release()
    }

    private fun refreshActiveExerciseState() {
        viewModelScope.launch {
            val exerciseList = exercises.first()
            val activeExercise = exerciseList.firstOrNull { exercise ->
                val completed = _completedSets.value[exercise.id] ?: 0
                completed < exercise.sets
            }

            _activeExerciseId.value = activeExercise?.id
            _activeExerciseMode.value = when {
                activeExercise == null -> ExerciseSessionMode.MANUAL_REPS
                activeExercise.exerciseType == ExerciseType.HOLD.name -> ExerciseSessionMode.HOLD_TIMER
                activeExercise.usesSensor -> ExerciseSessionMode.SENSOR_REPS
                else -> ExerciseSessionMode.MANUAL_REPS
            }
        }
    }
}
