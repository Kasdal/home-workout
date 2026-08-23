package com.example.workoutapp.ui.workout

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.model.Category
import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseSessionMode
import com.example.workoutapp.model.ExerciseType
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.model.WorkoutTemplate
import com.example.workoutapp.data.repository.CategoryRepository
import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.data.repository.ProfileRepository
import com.example.workoutapp.data.repository.SessionHistoryRepository
import com.example.workoutapp.data.settings.LegacySettingsBootstrapper
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.SyncedWorkoutSettingsRepository
import com.example.workoutapp.data.storage.PhotoProcessor
import com.example.workoutapp.data.storage.PhotoUploadResult
import com.example.workoutapp.data.storage.PhotoUploader
import com.example.workoutapp.data.storage.SourceUnreadableException
import com.example.workoutapp.data.sync.SyncStatus
import com.example.workoutapp.data.sync.SyncStatusMonitor
import com.example.workoutapp.domain.session.CountdownType
import com.example.workoutapp.domain.session.PostSetTimerRequest
import com.example.workoutapp.domain.session.WorkoutCountdownOrchestrator
import com.example.workoutapp.domain.session.WorkoutCountdownOrchestratorFactory
import com.example.workoutapp.domain.session.WorkoutSessionClock
import com.example.workoutapp.domain.session.WorkoutSessionClockFactory
import com.example.workoutapp.domain.session.WorkoutSessionCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class SessionStartError {
    data object NoActiveExercises : SessionStartError()
}

data class SyncErrorEvent(
    val label: String,
    val canRetry: Boolean
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val sessionHistoryRepository: SessionHistoryRepository,
    private val profileRepository: ProfileRepository,
    private val legacySettingsBootstrapper: LegacySettingsBootstrapper,
    private val localAppPreferencesRepository: LocalAppPreferencesRepository,
    private val syncedWorkoutSettingsRepository: SyncedWorkoutSettingsRepository,
    private val syncStatusMonitor: SyncStatusMonitor,
    private val soundManager: com.example.workoutapp.util.SoundManager,
    private val sessionCoordinator: WorkoutSessionCoordinator,
    private val countdownOrchestratorFactory: WorkoutCountdownOrchestratorFactory,
    private val sessionClockFactory: WorkoutSessionClockFactory,
    private val sensorOrchestratorFactory: WorkoutSensorOrchestratorFactory,
    private val photoProcessor: PhotoProcessor,
    private val photoUploader: PhotoUploader,
    private val categoryRepository: CategoryRepository,
    private val templateRepository: com.example.workoutapp.data.repository.TemplateRepository
) : ViewModel() {

    // Exercises from DB
    val exercises = exerciseRepository.getExercises()

    val categories: Flow<List<Category>> = categoryRepository.observeActiveCategories()

    val templates: Flow<List<WorkoutTemplate>> = templateRepository.observeTemplates()

    val sessions: Flow<List<WorkoutSession>> = sessionHistoryRepository.getSessions()

    fun getExerciseHistory(exerciseName: String): kotlinx.coroutines.flow.Flow<List<SessionExercise>> {
        return sessionHistoryRepository.getExerciseHistory(exerciseName)
    }

    private val countdownOrchestrator: WorkoutCountdownOrchestrator = countdownOrchestratorFactory.create(
        scope = viewModelScope,
        onCountdownWarning = {
            if (finalCountdownEnabled) {
                playTimerCue(timerSoundType)
            }
        },
        onTimerComplete = {
            playTimerCue(activeTimerCompleteSoundType)
        }
    )

    private val sessionClock: WorkoutSessionClock = sessionClockFactory.create(viewModelScope)
    private val sensorOrchestrator: WorkoutSensorOrchestrator = sensorOrchestratorFactory.create(
        scope = viewModelScope,
        currentSetCompletionTarget = ::getSensorSetCompletionTarget,
        onSetCompletionTriggered = ::onSensorSetCompletionTriggered
    )
    // Timer State (rest timer between sets/exercises)
    val timerSeconds: StateFlow<Int> = countdownOrchestrator.timerSeconds
    val isTimerRunning: StateFlow<Boolean> = countdownOrchestrator.isTimerRunning
    val isTimerPaused: StateFlow<Boolean> = countdownOrchestrator.isTimerPaused
    val timerType: StateFlow<CountdownType> = countdownOrchestrator.timerType

    // Custom timer durations
    private val _restTimerDuration = MutableStateFlow(30)
    val restTimerDuration: StateFlow<Int> = _restTimerDuration.asStateFlow()

    private val _exerciseSwitchDuration = MutableStateFlow(90)
    val exerciseSwitchDuration: StateFlow<Int> = _exerciseSwitchDuration.asStateFlow()

    private val _undoLastSetEnabled = MutableStateFlow(true)
    val undoLastSetEnabled: StateFlow<Boolean> = _undoLastSetEnabled.asStateFlow()

    private val _calorieIntensity = MutableStateFlow("normal")

    // Session State
    private val _sessionStarted = MutableStateFlow(false)
    val sessionStarted: StateFlow<Boolean> = _sessionStarted.asStateFlow()

    private val _isSessionPaused = MutableStateFlow(false)
    val isSessionPaused: StateFlow<Boolean> = _isSessionPaused.asStateFlow()

    private val _isCompletingSession = MutableStateFlow(false)

    // Session elapsed time (total time since session started)
    val sessionElapsedSeconds: StateFlow<Int> = sessionClock.elapsedSeconds

    // Map of ExerciseId -> Number of Completed Sets (0-4)
    private val _completedSets = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val completedSets: StateFlow<Map<Int, Int>> = _completedSets.asStateFlow()

    // Snapshot of the exercises that were active when the current session was started.
    // Cleared when the session ends (see completeSession).
    private val _sessionExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val sessionExercises: StateFlow<List<Exercise>> = _sessionExercises.asStateFlow()

    // Exercises bypassed with "Skip exercise" during the running session.
    private val _skippedExerciseIds = MutableStateFlow<Set<Int>>(emptySet())
    val skippedExerciseIds: StateFlow<Set<Int>> = _skippedExerciseIds.asStateFlow()

    // Exercises whose completed sets count as warm-up only (excluded from working volume).
    private val _warmUpOnlyExerciseIds = MutableStateFlow<Set<Int>>(emptySet())
    val warmUpOnlyExerciseIds: StateFlow<Set<Int>> = _warmUpOnlyExerciseIds.asStateFlow()

    // Optional per-exercise effort (RPE 1-10) and notes captured during the session.
    private val _exerciseRpe = MutableStateFlow<Map<Int, Int?>>(emptyMap())
    val exerciseRpe: StateFlow<Map<Int, Int?>> = _exerciseRpe.asStateFlow()

    private val _exerciseNotes = MutableStateFlow<Map<Int, String>>(emptyMap())
    val exerciseNotes: StateFlow<Map<Int, String>> = _exerciseNotes.asStateFlow()

    private val _sessionStartErrors = Channel<SessionStartError>(Channel.BUFFERED)
    val sessionStartErrors: Flow<SessionStartError> = _sessionStartErrors.receiveAsFlow()

    private val _photoUploadEvents = MutableSharedFlow<PhotoUploadResult>(extraBufferCapacity = 4)
    val photoUploadEvents: SharedFlow<PhotoUploadResult> = _photoUploadEvents.asSharedFlow()

    private val _syncErrorEvents = MutableSharedFlow<SyncErrorEvent>(extraBufferCapacity = 4)
    val syncErrorEvents: SharedFlow<SyncErrorEvent> = _syncErrorEvents.asSharedFlow()

    val syncStatus: StateFlow<SyncStatus> = syncStatusMonitor.status

    private var sessionStartTime = 0L
    
    // Sound settings cache
    private var soundsEnabled = true
    private var soundVolume = 1.0f
    private var timerSoundType = "beep"
    private var restCompleteSoundType = "chime"
    private var exerciseSwitchSoundType = "loud"
    private var celebrationSoundType = "cheer"
    private var vibrationEnabled = true
    private var finalCountdownEnabled = true
    private var silentModeBehavior = "respect"
    private var activeTimerCompleteSoundType = "chime"

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
                _calorieIntensity.value = settings.calorieIntensity
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
                restCompleteSoundType = settings.restCompleteSoundType
                exerciseSwitchSoundType = settings.exerciseSwitchSoundType
                celebrationSoundType = settings.celebrationSoundType
                vibrationEnabled = settings.vibrationEnabled
                finalCountdownEnabled = settings.finalCountdownEnabled
                silentModeBehavior = settings.silentModeBehavior
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
                launchSyncedWrite("Seed default exercises") {
                    defaults.forEachIndexed { index, name ->
                        exerciseRepository.addExercise(
                            Exercise(
                                name = name,
                                weight = 20f,
                                exerciseType = com.example.workoutapp.model.ExerciseType.STANDARD.name,
                                usesSensor = true,
                                holdDurationSeconds = 30,
                                sortOrder = index
                            )
                        )
                    }
                }
            }
        }
    }

    // --- Session Management ---
    fun startSession(template: WorkoutTemplate? = null) {
        _isCompletingSession.value = false
        viewModelScope.launch {
            val allExercises = exercises.first()
            val activeExercises = if (template == null) {
                allExercises.filter { !it.isDeleted && it.activeInSession }
            } else {
                val byId = allExercises.associateBy { it.id }
                template.exerciseIds.mapNotNull { byId[it] }
                    .filter { !it.isDeleted && it.activeInSession }
            }
            if (activeExercises.isEmpty()) {
                _sessionExercises.value = emptyList()
                _sessionStartErrors.send(SessionStartError.NoActiveExercises)
                return@launch
            }
            _sessionExercises.value = activeExercises
            _skippedExerciseIds.value = emptySet()
            _completedSets.value = emptyMap()
            _warmUpOnlyExerciseIds.value = emptySet()
            _exerciseRpe.value = emptyMap()
            _exerciseNotes.value = emptyMap()
            _sessionStarted.value = true
            _isSessionPaused.value = false
            sessionStartTime = System.currentTimeMillis()
            sessionClock.start()
            applySessionStateUpdate(
                sessionCoordinator.startSession(
                    exercises = activeExercises,
                    completedSets = emptyMap()
                )
            )
            if (sensorEnabled) {
                startSensorPolling()
            }
        }
    }

    fun skipCurrentExercise() {
        val currentId = _activeExerciseId.value ?: return
        viewModelScope.launch {
            val result = sessionCoordinator.skipExercise(
                exercises = exercises.first(),
                completedSets = _completedSets.value,
                exerciseId = currentId,
                exerciseSwitchDuration = _exerciseSwitchDuration.value,
                skippedExerciseIds = _skippedExerciseIds.value
            )
            if (result.didUpdate) {
                _skippedExerciseIds.value = _skippedExerciseIds.value + currentId
                applySessionStateUpdate(result.stateUpdate)
                handlePostSetTimerRequest(result.timerRequest)
            }
        }
    }

    fun finishCurrentExercise() {
        val currentId = _activeExerciseId.value ?: return
        viewModelScope.launch {
            val result = sessionCoordinator.finishExercise(
                exercises = exercises.first(),
                completedSets = _completedSets.value,
                exerciseId = currentId,
                exerciseSwitchDuration = _exerciseSwitchDuration.value,
                skippedExerciseIds = _skippedExerciseIds.value
            )
            if (result.didUpdate) {
                applySessionStateUpdate(result.stateUpdate)
                handlePostSetTimerRequest(result.timerRequest)
            }
        }
    }

    fun toggleActiveInSession(exercise: Exercise) {
        launchSyncedWrite("Update exercise") {
            exerciseRepository.updateExercise(
                exercise.copy(activeInSession = !exercise.activeInSession)
            )
        }
    }

    fun setExerciseCategory(exercise: Exercise, categoryId: String?) {
        launchSyncedWrite("Update exercise category") {
            exerciseRepository.updateExercise(exercise.copy(categoryId = categoryId))
        }
    }

    fun saveTemplate(template: WorkoutTemplate) {
        launchSyncedWrite("Save template") {
            templateRepository.saveTemplate(template)
        }
    }

    fun fallbackToManualReps() {
        _activeExerciseMode.value = ExerciseSessionMode.MANUAL_REPS
    }

    fun setExerciseWarmUpOnly(exerciseId: Int, isWarmUpOnly: Boolean) {
        _warmUpOnlyExerciseIds.value = if (isWarmUpOnly) {
            _warmUpOnlyExerciseIds.value + exerciseId
        } else {
            _warmUpOnlyExerciseIds.value - exerciseId
        }
    }

    fun setExerciseRpe(exerciseId: Int, rpe: Int?) {
        _exerciseRpe.value = _exerciseRpe.value + (exerciseId to rpe)
    }

    fun setExerciseNote(exerciseId: Int, note: String) {
        _exerciseNotes.value = _exerciseNotes.value + (exerciseId to note)
    }

    fun deleteTemplate(templateId: String) {
        launchSyncedWrite("Delete template") {
            templateRepository.deleteTemplate(templateId)
        }
    }

    fun completeSession(
        onComplete: (WorkoutSession) -> Unit,
        sessionRpe: Int? = null,
        sessionNotes: String? = null
    ) {
        viewModelScope.launch {
            _isCompletingSession.value = true
            sessionClock.pause()

            try {
                val result = sessionCoordinator.completeSession(
                    exercises = exercises.first(),
                    completedSets = _completedSets.value,
                    elapsedSeconds = sessionElapsedSeconds.value.toLong(),
                    endTime = System.currentTimeMillis(),
                    userMetrics = profileRepository.getUserMetrics().first(),
                    restTimerDuration = _restTimerDuration.value,
                    exerciseSwitchDuration = _exerciseSwitchDuration.value,
                    calorieIntensity = _calorieIntensity.value,
                    skippedExerciseIds = _skippedExerciseIds.value,
                    warmUpOnlyExerciseIds = _warmUpOnlyExerciseIds.value,
                    exerciseRpe = _exerciseRpe.value.filterValues { it != null }.mapValues { it.value!! },
                    exerciseNotes = _exerciseNotes.value,
                    sessionRpe = sessionRpe,
                    sessionNotes = sessionNotes
                )

                applySessionStateUpdate(result.stateUpdate)
                _sessionStarted.value = false
                _isSessionPaused.value = false
                _sessionExercises.value = emptyList()
                _skippedExerciseIds.value = emptySet()
                _warmUpOnlyExerciseIds.value = emptySet()
                _exerciseRpe.value = emptyMap()
                _exerciseNotes.value = emptyMap()

                stopSensorPolling()

                soundManager.playCelebrationSound(
                    celebrationSoundType,
                    soundVolume,
                    soundsEnabled,
                    vibrationEnabled,
                    silentModeBehavior
                )

                onComplete(result.completedSession)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Timber.e(t, "Failed to persist completed session")
                syncStatusMonitor.reportFailure(t)
                _syncErrorEvents.emit(SyncErrorEvent(label = "Save workout", canRetry = false))
            } finally {
                _isCompletingSession.value = false
                sessionClock.stop()
            }
        }
    }
    
    // Pause session timer
    fun pauseSession() {
        if (!_sessionStarted.value || _isCompletingSession.value) return
        sessionClock.pause()
        countdownOrchestrator.pauseTimer()
        _isSessionPaused.value = true
    }

    // Resume session timer
    fun resumeSession() {
        if (!_sessionStarted.value || _isCompletingSession.value || !_isSessionPaused.value) return
        sessionClock.resume()
        countdownOrchestrator.resumeTimer()
        _isSessionPaused.value = false
    }

    fun retryPendingWrites() {
        viewModelScope.launch {
            syncStatusMonitor.retryPending()
        }
    }

    private fun launchSyncedWrite(label: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            val saved = syncStatusMonitor.runTrackedWithRetry(label, block)
            if (!saved) {
                Timber.w("Firestore write failed: %s", label)
                _syncErrorEvents.emit(SyncErrorEvent(label = label, canRetry = true))
            }
        }
    }

    // --- Timer Logic ---
    fun setRestTimerDuration(seconds: Int) {
        _restTimerDuration.value = seconds
        launchSyncedWrite("Save rest timer setting") {
            syncedWorkoutSettingsRepository.setRestTimerDuration(seconds)
        }
    }

    fun setExerciseSwitchDuration(seconds: Int) {
        _exerciseSwitchDuration.value = seconds
        launchSyncedWrite("Save exercise switch setting") {
            syncedWorkoutSettingsRepository.setExerciseSwitchDuration(seconds)
        }
    }

    fun startTimer(seconds: Int) {
        activeTimerCompleteSoundType = restCompleteSoundType
        countdownOrchestrator.startTimer(seconds, CountdownType.REST)
    }

    fun startRestTimer() {
        activeTimerCompleteSoundType = restCompleteSoundType
        countdownOrchestrator.startTimer(_restTimerDuration.value, CountdownType.REST)
    }

    fun startExerciseSwitchTimer() {
        activeTimerCompleteSoundType = exerciseSwitchSoundType
        countdownOrchestrator.startTimer(_exerciseSwitchDuration.value, CountdownType.SWITCH)
    }

    fun skipActiveTimer() {
        countdownOrchestrator.skipTimer()
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

    fun resetSensorCounter() {
        sensorOrchestrator.resetCounterNow()
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
                undoEnabled = _undoLastSetEnabled.value,
                skippedExerciseIds = _skippedExerciseIds.value
            )
            applySessionStateUpdate(result.stateUpdate)
        }
    }
    
    fun updateExercise(exercise: Exercise) {
        launchSyncedWrite("Update exercise") {
            // The running session renders from a snapshot captured at startSession().
            // Persist the change and also refresh that live snapshot so in-session
            // weight adjustments (the +/- controls) are reflected immediately.
            exerciseRepository.updateExercise(exercise)
            if (_sessionStarted.value && _sessionExercises.value.isNotEmpty()) {
                _sessionExercises.value = _sessionExercises.value.map { existing ->
                    if (existing.id == exercise.id) exercise else existing
                }
            }
        }
    }
    
    fun updateExercisePhoto(exerciseId: Int, source: Uri) {
        viewModelScope.launch {
            val photoUri: String = try {
                val bytes = photoProcessor.compressToJpeg(source)
                photoUploader.uploadExercisePhoto(exerciseId, bytes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SourceUnreadableException) {
                Timber.w(e, "Photo upload failed for exercise %d", exerciseId)
                _photoUploadEvents.emit(PhotoUploadResult.SourceUnreadable)
                return@launch
            } catch (e: Throwable) {
                Timber.w(e, "Photo upload failed for exercise %d", exerciseId)
                _photoUploadEvents.emit(PhotoUploadResult.UploadFailed(e))
                return@launch
            }

            val existing = exercises.first().firstOrNull { it.id == exerciseId }
            if (existing != null) {
                val saved = syncStatusMonitor.runTrackedWithRetry("Save exercise photo") {
                    exerciseRepository.updateExercise(existing.copy(photoUri = photoUri))
                }
                if (!saved) {
                    _syncErrorEvents.emit(SyncErrorEvent(label = "Save exercise photo", canRetry = true))
                }
            }
            _photoUploadEvents.emit(PhotoUploadResult.Success(photoUri))
        }
    }

    fun removeExercisePhoto(exerciseId: Int) {
        viewModelScope.launch {
            val existing = exercises.first().firstOrNull { it.id == exerciseId }
                ?: return@launch
            runCatching { photoUploader.deleteExercisePhoto(exerciseId) }
                .onFailure { Timber.w(it, "Failed to delete remote photo for exercise %d", exerciseId) }
            val saved = syncStatusMonitor.runTrackedWithRetry("Remove exercise photo") {
                exerciseRepository.updateExercise(existing.copy(photoUri = null))
            }
            if (!saved) {
                _syncErrorEvents.emit(SyncErrorEvent(label = "Remove exercise photo", canRetry = true))
            }
        }
    }

    fun addExercise() {
        launchSyncedWrite("Add exercise") {
            val currentExercises = exercises.first()
            val nextSortOrder = nextSortOrder(currentExercises)
            exerciseRepository.addExercise(Exercise(name = "New Exercise", weight = 0f, sortOrder = nextSortOrder))
        }
    }

    fun addExercise(exercise: Exercise) {
        launchSyncedWrite("Add exercise") {
            val currentExercises = exercises.first()
            val nextSortOrder = nextSortOrder(currentExercises)
            exerciseRepository.addExercise(exercise.copy(sortOrder = nextSortOrder))
        }
    }

    fun moveExercise(exerciseId: Int, direction: Int) {
        launchSyncedWrite("Reorder exercises") {
            val orderedExercises = exercises.first()
            val fromIndex = orderedExercises.indexOfFirst { it.id == exerciseId }
            val toIndex = fromIndex + direction

            if (fromIndex !in orderedExercises.indices || toIndex !in orderedExercises.indices) return@launchSyncedWrite

            val reordered = orderedExercises.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }

            reordered.forEachIndexed { index, exercise ->
                exerciseRepository.updateExercise(exercise.copy(sortOrder = index))
            }
        }
    }

    fun updateExerciseOrder(orderedExercises: List<Exercise>) {
        launchSyncedWrite("Reorder exercises") {
            orderedExercises.forEachIndexed { index, exercise ->
                exerciseRepository.updateExercise(exercise.copy(sortOrder = index))
            }
        }
    }

    private fun nextSortOrder(currentExercises: List<Exercise>): Int {
        val finiteSortOrders = currentExercises.mapNotNull { it.sortOrder.takeIf { order -> order != Int.MAX_VALUE } }
        return if (finiteSortOrders.isEmpty()) Int.MAX_VALUE else (finiteSortOrders.maxOrNull() ?: -1) + 1
    }

    fun deleteExercise(exerciseId: Int) {
        launchSyncedWrite("Delete exercise") {
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
        val skipped = _skippedExerciseIds.value
        val incompleteExercise = exerciseList.firstOrNull { exercise ->
            val completedSets = _completedSets.value[exercise.id] ?: 0
            exercise.id !in skipped &&
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

    private fun handlePostSetTimerRequest(request: PostSetTimerRequest) {
        when (request) {
            is PostSetTimerRequest.Start -> {
                activeTimerCompleteSoundType = if (request.timerType == CountdownType.SWITCH) {
                    exerciseSwitchSoundType
                } else {
                    restCompleteSoundType
                }
                countdownOrchestrator.startTimer(request.seconds, request.timerType)
            }
            PostSetTimerRequest.None -> Unit
        }
    }

    private suspend fun completeNextSetInternal(exerciseId: Int): Boolean {
        val result = sessionCoordinator.completeNextSet(
            exercises = exercises.first(),
            completedSets = _completedSets.value,
            exerciseId = exerciseId,
            restTimerDuration = _restTimerDuration.value,
            exerciseSwitchDuration = _exerciseSwitchDuration.value,
            skippedExerciseIds = _skippedExerciseIds.value
        )

        if (!result.didUpdate) {
            return false
        }

        applySessionStateUpdate(result.stateUpdate)
        handlePostSetTimerRequest(result.timerRequest)
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

    private fun playTimerCue(soundType: String) {
        soundManager.playTimerSound(
            soundType,
            soundVolume,
            soundsEnabled,
            vibrationEnabled,
            silentModeBehavior
        )
    }
}
