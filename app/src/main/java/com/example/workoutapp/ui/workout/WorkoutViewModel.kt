package com.example.workoutapp.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.ExerciseSessionMode
import com.example.workoutapp.data.local.entity.ExerciseType
import com.example.workoutapp.data.local.entity.WorkoutSession
import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.data.repository.ProfileRepository
import com.example.workoutapp.data.repository.SensorRepository
import com.example.workoutapp.data.repository.SessionHistoryRepository
import com.example.workoutapp.data.settings.LegacySettingsBootstrapper
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.SyncedWorkoutSettingsRepository
import com.example.workoutapp.domain.session.PostSetTimerRequest
import com.example.workoutapp.domain.session.SessionCompletionCalculator
import com.example.workoutapp.domain.session.WorkoutCountdownOrchestrator
import com.example.workoutapp.domain.session.WorkoutSessionClock
import com.example.workoutapp.domain.session.WorkoutSessionCoordinator
import com.example.workoutapp.domain.session.WorkoutSessionReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository,
    private val sessionHistoryRepository: SessionHistoryRepository,
    private val legacySettingsBootstrapper: LegacySettingsBootstrapper,
    private val localAppPreferencesRepository: LocalAppPreferencesRepository,
    private val syncedWorkoutSettingsRepository: SyncedWorkoutSettingsRepository,
    private val soundManager: com.example.workoutapp.util.SoundManager,
    private val sensorRepository: SensorRepository,
    private val sessionCompletionCalculator: SessionCompletionCalculator,
    private val sessionReducer: WorkoutSessionReducer = WorkoutSessionReducer()
) : ViewModel() {

    // Exercises from DB
    val exercises = exerciseRepository.getExercises()

    private val countdownOrchestrator = WorkoutCountdownOrchestrator(
        scope = viewModelScope,
        onTimerSound = {
            soundManager.playTimerSound(timerSoundType, soundVolume, soundsEnabled)
        }
    )

    private val sessionClock = WorkoutSessionClock(viewModelScope)
    private val sensorOrchestrator = WorkoutSensorOrchestrator(
        scope = viewModelScope,
        pollSensorStatus = sensorRepository::pollSensorStatus,
        currentSetCompletionTarget = ::getSensorSetCompletionTarget,
        onSetCompletionTriggered = ::onSensorSetCompletionTriggered,
        resetCounter = sensorRepository::resetCounter
    )
    private val sessionCoordinator = WorkoutSessionCoordinator(
        sessionReducer = sessionReducer,
        sessionCompletionCalculator = sessionCompletionCalculator,
        sessionHistoryRepository = sessionHistoryRepository
    )

    // Timer State (rest timer between sets/exercises)
    val timerSeconds: StateFlow<Int> = countdownOrchestrator.timerSeconds
    val isTimerRunning: StateFlow<Boolean> = countdownOrchestrator.isTimerRunning
    val isTimerPaused: StateFlow<Boolean> = countdownOrchestrator.isTimerPaused

    // Custom timer durations
    private val _restTimerDuration = MutableStateFlow(30)
    val restTimerDuration: StateFlow<Int> = _restTimerDuration.asStateFlow()

    private val _exerciseSwitchDuration = MutableStateFlow(90)
    val exerciseSwitchDuration: StateFlow<Int> = _exerciseSwitchDuration.asStateFlow()

    private val _undoLastSetEnabled = MutableStateFlow(true)
    val undoLastSetEnabled: StateFlow<Boolean> = _undoLastSetEnabled.asStateFlow()

    // Session State
    private val _sessionStarted = MutableStateFlow(false)
    val sessionStarted: StateFlow<Boolean> = _sessionStarted.asStateFlow()

    private val _isCompletingSession = MutableStateFlow(false)

    // Session elapsed time (total time since session started)
    val sessionElapsedSeconds: StateFlow<Int> = sessionClock.elapsedSeconds

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

    private val _activeExerciseId = MutableStateFlow<Int?>(null)
    val activeExerciseId: StateFlow<Int?> = _activeExerciseId.asStateFlow()

    private val _activeExerciseMode = MutableStateFlow(ExerciseSessionMode.MANUAL_REPS)
    val activeExerciseMode: StateFlow<ExerciseSessionMode> = _activeExerciseMode.asStateFlow()

    init {
        initializeDefaultExercises()
        observeSyncedSettings()
        observeLocalSettings()
        observeSensorSnapshot()
    }

    private fun observeSensorSnapshot() {
        viewModelScope.launch {
            sensorOrchestrator.sensorSnapshot.collect { snapshot ->
                _sensorConnected.value = snapshot.connected
                _sensorReps.value = snapshot.reps
                _sensorState.value = snapshot.state
                _sensorDistance.value = snapshot.distance
            }
        }
    }

    private fun observeSyncedSettings() {
        viewModelScope.launch {
            syncedWorkoutSettingsRepository.observeSessionSettings().collect { settings ->
                _restTimerDuration.value = settings.restTimerDuration
                _exerciseSwitchDuration.value = settings.exerciseSwitchDuration
                _undoLastSetEnabled.value = settings.undoLastSetEnabled
            }
        }

        viewModelScope.launch {
            legacySettingsBootstrapper.seedFromLegacySettingsIfPresent()
        }
    }

    private fun observeLocalSettings() {
        viewModelScope.launch {
            localAppPreferencesRepository.settings.collect { settings ->
                soundsEnabled = settings.soundsEnabled
                soundVolume = settings.soundVolume
                timerSoundType = settings.timerSoundType
                celebrationSoundType = settings.celebrationSoundType
                sensorEnabled = settings.sensorEnabled
                sensorIpAddress = settings.sensorIpAddress
                if (_sessionStarted.value && sensorEnabled && !sensorOrchestrator.isPolling) {
                    startSensorPolling()
                } else if (!sensorEnabled && sensorOrchestrator.isPolling) {
                    stopSensorPolling()
                }
            }
        }
    }

    private fun initializeDefaultExercises() {
        viewModelScope.launch {
            if (exerciseRepository.getExercises().first().isEmpty()) {
                val defaults = listOf(
                    "Bench Press", "Squat", "Deadlift", "Overhead Press",
                    "Barbell Row", "Pull Up", "Dips", "Bicep Curl",
                    "Tricep Extension", "Lateral Raise", "Calf Raise"
                )
                defaults.forEach { name ->
                    exerciseRepository.addExercise(
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
        _isCompletingSession.value = false
        _sessionStarted.value = true
        sessionStartTime = System.currentTimeMillis()
        sessionClock.start()
        viewModelScope.launch {
            applySessionStateUpdate(
                sessionCoordinator.startSession(
                    exercises = exercises.first(),
                    completedSets = _completedSets.value
                )
            )
        }

        if (sensorEnabled) {
            startSensorPolling()
        }
    }

    fun completeSession(onComplete: (WorkoutSession) -> Unit) {
        viewModelScope.launch {
            _isCompletingSession.value = true
            sessionClock.pause()

            val result = sessionCoordinator.completeSession(
                exercises = exercises.first(),
                completedSets = _completedSets.value,
                elapsedSeconds = sessionElapsedSeconds.value.toLong(),
                endTime = System.currentTimeMillis(),
                userMetrics = profileRepository.getUserMetrics().first(),
                restTimerDuration = _restTimerDuration.value,
                exerciseSwitchDuration = _exerciseSwitchDuration.value
            )

            applySessionStateUpdate(result.stateUpdate)
            _sessionStarted.value = false
            sessionClock.stop()
            _isCompletingSession.value = false

            stopSensorPolling()

            soundManager.playCelebrationSound(celebrationSoundType, soundVolume, soundsEnabled)

            onComplete(result.completedSession)
        }
    }
    
    // Pause session timer
    fun pauseSession() {
        sessionClock.pause()
    }
    
    // Resume session timer
    fun resumeSession() {
        if (_sessionStarted.value && !_isCompletingSession.value) {
            sessionClock.resume()
        }
    }

    // --- Timer Logic ---
    fun setRestTimerDuration(seconds: Int) {
        _restTimerDuration.value = seconds
        viewModelScope.launch {
            syncedWorkoutSettingsRepository.setRestTimerDuration(seconds)
        }
    }

    fun setExerciseSwitchDuration(seconds: Int) {
        _exerciseSwitchDuration.value = seconds
        viewModelScope.launch {
            syncedWorkoutSettingsRepository.setExerciseSwitchDuration(seconds)
        }
    }

    fun startTimer(seconds: Int) {
        countdownOrchestrator.startTimer(seconds)
    }

    fun pauseTimer() {
        countdownOrchestrator.pauseTimer()
    }

    fun resumeTimer() {
        countdownOrchestrator.resumeTimer()
    }

    fun stopTimer() {
        countdownOrchestrator.stopTimer()
    }

    // --- Set Completion Logic ---
    fun completeNextSet(exerciseId: Int) {
        viewModelScope.launch {
            completeNextSetInternal(exerciseId)
        }
    }

    fun undoSet(exerciseId: Int) {
        viewModelScope.launch {
            val result = sessionCoordinator.undoSet(
                exercises = exercises.first(),
                completedSets = _completedSets.value,
                exerciseId = exerciseId,
                undoEnabled = _undoLastSetEnabled.value
            )
            applySessionStateUpdate(result.stateUpdate)
        }
    }
    
    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            exerciseRepository.updateExercise(exercise)
        }
    }
    
    fun updateExercisePhoto(exerciseId: Int, photoUri: String) {
        viewModelScope.launch {
            val exerciseList = exercises.first()
            val exercise = exerciseList.find { it.id == exerciseId }
            exercise?.let {
                exerciseRepository.updateExercise(it.copy(photoUri = photoUri))
            }
        }
    }

    fun addExercise() {
        viewModelScope.launch {
            exerciseRepository.addExercise(Exercise(name = "New Exercise", weight = 0f))
        }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            exerciseRepository.addExercise(exercise)
        }
    }

    fun deleteExercise(exerciseId: Int) {
        viewModelScope.launch {
            exerciseRepository.deleteExercise(exerciseId)
        }
    }

    private fun startSensorPolling() {
        sensorOrchestrator.start(sensorIpAddress)
    }

    private fun stopSensorPolling() {
        sensorOrchestrator.stop()
    }

    private suspend fun getSensorSetCompletionTarget(): SensorSetCompletionTarget? {
        val exerciseList = exercises.first()
        val incompleteExercise = exerciseList.firstOrNull { exercise ->
            val completedSets = _completedSets.value[exercise.id] ?: 0
            completedSets < exercise.sets &&
                exercise.usesSensor &&
                exercise.exerciseType != ExerciseType.HOLD.name
        }

        return incompleteExercise?.let { exercise ->
            SensorSetCompletionTarget(
                exerciseId = exercise.id,
                targetReps = exercise.reps
            )
        }
    }

    private suspend fun onSensorSetCompletionTriggered(exerciseId: Int): Boolean {
        return completeNextSetInternal(exerciseId)
    }

    private suspend fun completeNextSetInternal(exerciseId: Int): Boolean {
        val result = sessionCoordinator.completeNextSet(
            exercises = exercises.first(),
            completedSets = _completedSets.value,
            exerciseId = exerciseId,
            restTimerDuration = _restTimerDuration.value,
            exerciseSwitchDuration = _exerciseSwitchDuration.value
        )

        if (!result.didUpdate) {
            return false
        }

        applySessionStateUpdate(result.stateUpdate)
        when (val timerRequest = result.timerRequest) {
            is PostSetTimerRequest.Start -> startTimer(timerRequest.seconds)
            PostSetTimerRequest.None -> Unit
        }
        return true
    }
    
    override fun onCleared() {
        countdownOrchestrator.stopTimer()
        sessionClock.stop()
        super.onCleared()
        stopSensorPolling()
        soundManager.release()
    }

    private fun applyActiveExerciseSelection(selection: com.example.workoutapp.domain.session.ActiveExerciseSelection) {
        _activeExerciseId.value = selection.activeExerciseId
        _activeExerciseMode.value = selection.activeExerciseMode
    }

    private fun applySessionStateUpdate(update: com.example.workoutapp.domain.session.WorkoutSessionStateUpdate?) {
        if (update == null) return
        _completedSets.value = update.completedSets
        applyActiveExerciseSelection(update.activeExerciseSelection)
    }
}
