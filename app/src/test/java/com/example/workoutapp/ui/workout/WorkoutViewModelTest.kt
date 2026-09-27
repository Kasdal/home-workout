package com.example.workoutapp.ui.workout

import android.net.Uri
import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseSessionMode
import com.example.workoutapp.model.ExerciseType
import com.example.workoutapp.model.UserMetrics
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.data.repository.ProfileRepository
import com.example.workoutapp.data.repository.SessionHistoryRepository
import com.example.workoutapp.data.settings.LegacySettingsBootstrapper
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.LocalAppSettings
import com.example.workoutapp.data.settings.SyncedWorkoutSettingsRepository
import com.example.workoutapp.data.settings.WorkoutSessionSettings
import com.example.workoutapp.data.remote.EspSensorData
import com.example.workoutapp.data.storage.PhotoProcessor
import com.example.workoutapp.data.storage.PhotoUploadResult
import com.example.workoutapp.data.storage.PhotoUploader
import com.example.workoutapp.data.storage.SourceUnreadableException
import com.example.workoutapp.data.sync.SyncState
import com.example.workoutapp.data.sync.SyncStatusMonitor
import com.example.workoutapp.domain.session.SessionCompletionCalculator
import com.example.workoutapp.domain.session.WorkoutCountdownOrchestratorFactory
import com.example.workoutapp.domain.session.WorkoutSessionClockFactory
import com.example.workoutapp.domain.session.WorkoutSessionCoordinator
import com.example.workoutapp.domain.session.WorkoutSessionReducer
import kotlinx.coroutines.flow.Flow
import com.example.workoutapp.util.SoundManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WorkoutViewModelTest {

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var sessionHistoryRepository: SessionHistoryRepository
    private lateinit var legacySettingsBootstrapper: LegacySettingsBootstrapper
    private lateinit var localAppPreferencesRepository: LocalAppPreferencesRepository
    private lateinit var syncedWorkoutSettingsRepository: SyncedWorkoutSettingsRepository
    private lateinit var soundManager: SoundManager
    private lateinit var sessionCompletionCalculator: SessionCompletionCalculator
    private lateinit var sessionCoordinator: WorkoutSessionCoordinator
    private lateinit var countdownOrchestratorFactory: WorkoutCountdownOrchestratorFactory
    private lateinit var sessionClockFactory: WorkoutSessionClockFactory
    private lateinit var sessionClock: ManualSessionClock
    private lateinit var sensorOrchestratorFactory: WorkoutSensorOrchestratorFactory
    private lateinit var photoProcessor: PhotoProcessor
    private lateinit var photoUploader: PhotoUploader
    private lateinit var categoryRepository: com.example.workoutapp.data.repository.CategoryRepository
    private lateinit var templateRepository: com.example.workoutapp.data.repository.TemplateRepository
    private lateinit var syncStatusMonitor: SyncStatusMonitor
    private lateinit var exercisesFlow: MutableStateFlow<List<Exercise>>
    private lateinit var localSettingsFlow: MutableStateFlow<LocalAppSettings>
    private lateinit var sessionSettingsFlow: MutableStateFlow<WorkoutSessionSettings>
    private var sensorStatusFlow: Flow<EspSensorData?> = emptyFlow()
    private var sensorResetResult = false
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        exerciseRepository = mockk(relaxed = true)
        profileRepository = mockk(relaxed = true)
        sessionHistoryRepository = mockk(relaxed = true)
        legacySettingsBootstrapper = mockk(relaxed = true)
        localAppPreferencesRepository = mockk(relaxed = true)
        syncedWorkoutSettingsRepository = mockk(relaxed = true)
        soundManager = mockk(relaxed = true)
        sessionCompletionCalculator = SessionCompletionCalculator()
        sessionCoordinator = WorkoutSessionCoordinator(
            sessionReducer = WorkoutSessionReducer(),
            sessionCompletionCalculator = sessionCompletionCalculator,
            sessionHistoryRepository = sessionHistoryRepository
        )
        countdownOrchestratorFactory = mockk()
        sessionClockFactory = mockk()
        sessionClock = ManualSessionClock()
        sensorOrchestratorFactory = mockk()
        photoProcessor = mockk(relaxed = true)
        photoUploader = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        every { categoryRepository.observeActiveCategories() } returns flowOf(emptyList())
        templateRepository = mockk(relaxed = true)
        every { templateRepository.observeTemplates() } returns flowOf(emptyList())
        syncStatusMonitor = SyncStatusMonitor()
        exercisesFlow = MutableStateFlow(
            listOf(
                Exercise(id = 1, name = "Bench Press", weight = 100f, reps = 10, sets = 4),
                Exercise(id = 2, name = "Squat", weight = 150f, reps = 5, sets = 5)
            )
        )
        localSettingsFlow = MutableStateFlow(LocalAppSettings())
        sessionSettingsFlow = MutableStateFlow(WorkoutSessionSettings())
        sensorStatusFlow = emptyFlow()
        sensorResetResult = false

        // Default mocks
        every { exerciseRepository.getExercises() } returns exercisesFlow
        every { profileRepository.getUserMetrics() } returns flowOf(UserMetrics(weightKg = 80f))
        every { localAppPreferencesRepository.settings } returns localSettingsFlow
        every { syncedWorkoutSettingsRepository.observeSessionSettings() } returns sessionSettingsFlow
        every { countdownOrchestratorFactory.create(any(), any(), any()) } answers {
            com.example.workoutapp.domain.session.WorkoutCountdownOrchestrator(
                scope = firstArg(),
                onCountdownWarning = secondArg(),
                onTimerComplete = thirdArg()
            )
        }
        every { sessionClockFactory.create(any()) } returns sessionClock
        every {
            sensorOrchestratorFactory.create(
                any(),
                any(),
                any()
            )
        } answers {
            WorkoutSensorOrchestrator(
                scope = firstArg(),
                pollSensorStatus = { _, _ -> sensorStatusFlow },
                currentSetCompletionTarget = secondArg(),
                onSetCompletionTriggered = thirdArg(),
                resetCounter = { sensorResetResult }
            )
        }

        viewModel = createViewModel()
    }

    private fun createViewModel(): WorkoutViewModel {
        return WorkoutViewModel(
            exerciseRepository,
            sessionHistoryRepository,
            profileRepository,
            legacySettingsBootstrapper,
            localAppPreferencesRepository,
            syncedWorkoutSettingsRepository,
            syncStatusMonitor,
            soundManager,
            sessionCoordinator,
            countdownOrchestratorFactory,
            sessionClockFactory,
            sensorOrchestratorFactory,
            photoProcessor,
            photoUploader,
            categoryRepository,
            templateRepository
        )
    }

    private fun clearViewModel() {
        val method = WorkoutViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)
    }

    /** Never ticks on its own, so `advanceUntilIdle` cannot loop forever on virtual time. */
    private class ManualSessionClock : com.example.workoutapp.domain.session.WorkoutSessionClock(
        kotlinx.coroutines.MainScope(),
        { 0L }
    ) {
        override fun start() { }
        override fun pause() { }
        override fun resume() { }
        override fun stop() {
            _elapsedSeconds.value = 0
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init seeds local settings through bootstrapper`() = runTest {
        advanceUntilIdle()

        coVerify { legacySettingsBootstrapper.seedFromLegacySettingsIfPresent() }
    }

    @Test
    fun `init creates runtime seams through factories`() = runTest {
        advanceUntilIdle()

        verify(exactly = 1) { countdownOrchestratorFactory.create(any(), any(), any()) }
        verify(exactly = 1) { sessionClockFactory.create(any()) }
        verify(exactly = 1) { sensorOrchestratorFactory.create(any(), any(), any()) }
    }

    @Test
    fun `completeSession saves session and resets state`() = runTest {
        // Simulate completing sets
        viewModel.completeNextSet(1) // Bench Press set 1
        runCurrent()

        coEvery { sessionHistoryRepository.saveSession(any()) } returns 1L
        coEvery { sessionHistoryRepository.saveSessionExercises(any()) } just Runs

        viewModel.completeSession(onComplete = { session ->
            assertEquals(0L, session.durationSeconds)
            assertEquals(1000f, session.totalWeightLifted) // 1 set * 10 reps * 100 weight
        })
        runCurrent()

        coVerify { sessionHistoryRepository.saveSession(any()) }
        coVerify { sessionHistoryRepository.saveSessionExercises(any()) }

        assertFalse(viewModel.sessionStarted.value)
        assertEquals(0, viewModel.sessionElapsedSeconds.value)
        assertTrue(viewModel.completedSets.value.isEmpty())
        assertEquals(null, viewModel.activeExerciseId.value)
        assertEquals(ExerciseSessionMode.MANUAL_REPS, viewModel.activeExerciseMode.value)

        clearViewModel()
        runCurrent()
    }

    @Test
    fun `completeNextSet updates visible workout progress state`() = runTest {
        viewModel.completeNextSet(1)
        runCurrent()

        val sets = viewModel.completedSets.value
        assertEquals(1, sets[1])
        assertEquals(1, viewModel.activeExerciseId.value)
        assertEquals(ExerciseSessionMode.SENSOR_REPS, viewModel.activeExerciseMode.value)
    }

    @Test
    fun `completeNextSet surfaces timer requests through public timer state`() = runTest {
        viewModel.setRestTimerDuration(17)
        runCurrent()

        viewModel.completeNextSet(1)
        runCurrent()

        assertTrue(viewModel.isTimerRunning.value)
        assertFalse(viewModel.isTimerPaused.value)
        assertEquals(17, viewModel.timerSeconds.value)
    }

    @Test
    fun `undoSet updates visible workout progress state without disturbing the active timer`() = runTest {
        viewModel.completeNextSet(1)
        runCurrent()

        assertTrue(viewModel.isTimerRunning.value)
        assertEquals(30, viewModel.timerSeconds.value)

        advanceTimeBy(5000)
        runCurrent()
        val remainingBeforeUndo = viewModel.timerSeconds.value

        viewModel.undoSet(1)
        runCurrent()

        val sets = viewModel.completedSets.value
        assertEquals(0, sets[1])
        assertEquals(1, viewModel.activeExerciseId.value)
        assertEquals(ExerciseSessionMode.SENSOR_REPS, viewModel.activeExerciseMode.value)
        assertTrue(viewModel.isTimerRunning.value)
        assertEquals(remainingBeforeUndo, viewModel.timerSeconds.value)

        advanceTimeBy(1000)
        runCurrent()
        assertEquals(remainingBeforeUndo - 1, viewModel.timerSeconds.value)
    }
    
    @Test
    fun `completeNextSet uses public timer state for final-set timer requests`() = runTest {
        viewModel.setExerciseSwitchDuration(61)
        runCurrent()

        viewModel.completeNextSet(1)
        runCurrent()
        viewModel.completeNextSet(1)
        runCurrent()
        viewModel.completeNextSet(1)
        runCurrent()
        viewModel.completeNextSet(1)
        runCurrent()
        
        val sets = viewModel.completedSets.value
        assertEquals(4, sets[1])

        assertTrue(viewModel.isTimerRunning.value)
        assertFalse(viewModel.isTimerPaused.value)
        assertEquals(61, viewModel.timerSeconds.value)
    }

    @Test
    fun `timer commands update observable timer state`() = runTest {
        viewModel.startTimer(5)

        assertTrue(viewModel.isTimerRunning.value)
        assertFalse(viewModel.isTimerPaused.value)
        assertEquals(5, viewModel.timerSeconds.value)

        viewModel.pauseTimer()

        assertFalse(viewModel.isTimerRunning.value)
        assertTrue(viewModel.isTimerPaused.value)

        viewModel.resumeTimer()

        assertTrue(viewModel.isTimerRunning.value)
        assertFalse(viewModel.isTimerPaused.value)

        viewModel.stopTimer()

        assertFalse(viewModel.isTimerRunning.value)
        assertFalse(viewModel.isTimerPaused.value)
    }

    @Test
    fun `pauseSession pauses the session clock and the exercise timer together`() = runTest {
        viewModel.startSession()
        runCurrent()
        viewModel.completeNextSet(1)
        runCurrent()

        assertTrue(viewModel.isTimerRunning.value)
        assertFalse(viewModel.isSessionPaused.value)

        viewModel.pauseSession()
        runCurrent()

        assertTrue(viewModel.isSessionPaused.value)
        assertTrue(viewModel.isTimerPaused.value)

        viewModel.resumeSession()
        runCurrent()
        assertFalse(viewModel.isSessionPaused.value)
        assertTrue(viewModel.isTimerRunning.value)
    }

    @Test
    fun `session screen display options surface through view model state`() = runTest {
        advanceUntilIdle()

        assertFalse(viewModel.showExerciseCounter.value)
        assertFalse(viewModel.showInlineRpe.value)
        assertFalse(viewModel.showExerciseNotes.value)

        localSettingsFlow.value = LocalAppSettings(
            showExerciseCounter = true,
            showInlineRpe = true,
            showExerciseNotes = true
        )
        runCurrent()

        assertTrue(viewModel.showExerciseCounter.value)
        assertTrue(viewModel.showInlineRpe.value)
        assertTrue(viewModel.showExerciseNotes.value)
    }

    @Test
    fun `finishCurrentExercise stores rpe and note before advancing`() = runTest {
        viewModel.startSession()
        runCurrent()

        viewModel.finishCurrentExercise(rpe = 7, note = "felt strong")
        advanceUntilIdle()

        assertEquals(7, viewModel.exerciseRpe.value[1])
        assertEquals("felt strong", viewModel.exerciseNotes.value[1])
        assertEquals(2, viewModel.activeExerciseId.value)
    }

    @Test
    fun `finishCurrentExercise without feedback skips storing and still advances`() = runTest {
        viewModel.startSession()
        runCurrent()

        viewModel.finishCurrentExercise(rpe = null, note = "   ")
        advanceUntilIdle()

        assertFalse(viewModel.exerciseRpe.value.containsKey(1))
        assertFalse(viewModel.exerciseNotes.value.containsKey(1))
        assertEquals(2, viewModel.activeExerciseId.value)
    }

    @Test
    fun `fallbackToManualReps suppresses the sensor banner until process restart`() = runTest {
        localSettingsFlow.value = LocalAppSettings(sensorEnabled = true, sensorIpAddress = "10.0.0.5")

        viewModel.startSession()
        runCurrent()
        viewModel.completeNextSet(1)
        runCurrent()

        assertEquals(ExerciseSessionMode.SENSOR_REPS, viewModel.activeExerciseMode.value)
        assertFalse(viewModel.sensorFallbackDismissed.value)

        viewModel.fallbackToManualReps()
        assertEquals(ExerciseSessionMode.MANUAL_REPS, viewModel.activeExerciseMode.value)
        assertTrue(viewModel.sensorFallbackDismissed.value)

        // Session state updates re-select the exercise mode, but the dismissal sticks.
        viewModel.completeNextSet(1)
        runCurrent()

        assertEquals(ExerciseSessionMode.SENSOR_REPS, viewModel.activeExerciseMode.value)
        assertTrue(viewModel.sensorFallbackDismissed.value)
    }

    @Test
    fun `sensor state is surfaced through observable view model state`() = runTest {
        localSettingsFlow.value = LocalAppSettings(sensorEnabled = true, sensorIpAddress = "10.0.0.5")
        val sensorEvents = MutableSharedFlow<EspSensorData?>()
        sensorStatusFlow = sensorEvents

        viewModel.startSession()
        runCurrent()

        sensorEvents.emit(EspSensorData(reps = 7, state = "LIFTING", dist = 33))
        runCurrent()

        assertTrue(viewModel.sensorConnected.value)
        assertEquals(7, viewModel.sensorReps.value)
        assertEquals("LIFTING", viewModel.sensorState.value)
        assertEquals(33, viewModel.sensorDistance.value)

        localSettingsFlow.value = localSettingsFlow.value.copy(sensorEnabled = false)
        runCurrent()
        viewModel.pauseSession()
    }

    @Test
    fun `session lifecycle gates sensor updates and resets sensor state on completion`() = runTest {
        localSettingsFlow.value = LocalAppSettings(sensorEnabled = true, sensorIpAddress = "10.0.0.5")
        val sensorEvents = MutableSharedFlow<EspSensorData?>()
        sensorStatusFlow = sensorEvents
        io.mockk.coEvery { sessionHistoryRepository.saveSession(any()) } returns 1L

        sensorEvents.emit(EspSensorData(reps = 4, state = "TOP", dist = 12))
        runCurrent()

        assertFalse(viewModel.sensorConnected.value)
        assertEquals(0, viewModel.sensorReps.value)

        viewModel.startSession()
        runCurrent()

        sensorEvents.emit(EspSensorData(reps = 4, state = "TOP", dist = 12))
        runCurrent()

        assertTrue(viewModel.sensorConnected.value)
        assertEquals(4, viewModel.sensorReps.value)

        viewModel.completeSession(onComplete = { })
        runCurrent()

        assertFalse(viewModel.sensorConnected.value)
        assertEquals(0, viewModel.sensorReps.value)
        assertEquals("REST", viewModel.sensorState.value)
        assertEquals(0, viewModel.sensorDistance.value)
    }

    @Test
    fun `sensor driven set completion updates workout progress`() = runTest {
        localSettingsFlow.value = LocalAppSettings(sensorEnabled = true, sensorIpAddress = "10.0.0.5")
        val sensorEvents = MutableSharedFlow<EspSensorData?>()
        sensorStatusFlow = sensorEvents
        sensorResetResult = true

        viewModel.startSession()
        runCurrent()

        sensorEvents.emit(EspSensorData(reps = 10, state = "TOP", dist = 20))
        runCurrent()

        assertEquals(1, viewModel.completedSets.value[1])
        assertTrue(viewModel.isTimerRunning.value)
        assertEquals(30, viewModel.timerSeconds.value)

        advanceTimeBy(1000)
        runCurrent()

        assertEquals(0, viewModel.sensorReps.value)

        localSettingsFlow.value = localSettingsFlow.value.copy(sensorEnabled = false)
        runCurrent()
        viewModel.pauseSession()
    }

    /**
     * Regression: the sensor used to re-scan the exercise list for the first
     * incomplete exercise that also used the sensor and was not a hold. That is a
     * different predicate from the one the reducer uses to choose the exercise
     * the user is looking at, so with a hold first and a sensor exercise second
     * the sensor completed sets on an exercise that was not on screen, corrupting
     * volume, set counts and personal records.
     */
    @Test
    fun `sensor does not complete a set for an exercise that is not the active one`() = runTest {
        localSettingsFlow.value = LocalAppSettings(sensorEnabled = true, sensorIpAddress = "10.0.0.5")
        val sensorEvents = MutableSharedFlow<EspSensorData?>()
        sensorStatusFlow = sensorEvents
        sensorResetResult = true

        // A hold exercise first, so the reducer selects it as the active one,
        // and a sensor-tracked exercise second.
        exercisesFlow.value = listOf(
            Exercise(
                id = 1,
                name = "Plank",
                weight = 0f,
                reps = 10,
                sets = 3,
                exerciseType = ExerciseType.HOLD.name,
                holdDurationSeconds = 30,
                usesSensor = false,
                activeInSession = true
            ),
            Exercise(
                id = 2,
                name = "Bench Press",
                weight = 100f,
                reps = 10,
                sets = 4,
                usesSensor = true,
                activeInSession = true
            )
        )

        viewModel.startSession()
        runCurrent()

        // The hold is what the user is being shown.
        assertEquals(1, viewModel.activeExerciseId.value)

        sensorEvents.emit(EspSensorData(reps = 10, state = "TOP", dist = 20))
        runCurrent()

        assertNull(
            "Sensor completed a set on an exercise that is not the active one",
            viewModel.completedSets.value[2]
        )
        assertEquals(1, viewModel.activeExerciseId.value)

        localSettingsFlow.value = localSettingsFlow.value.copy(sensorEnabled = false)
        runCurrent()
        viewModel.pauseSession()
    }

    @Test
    fun `sensor completes a set once the sensor exercise becomes the active one`() = runTest {
        localSettingsFlow.value = LocalAppSettings(sensorEnabled = true, sensorIpAddress = "10.0.0.5")
        val sensorEvents = MutableSharedFlow<EspSensorData?>()
        sensorStatusFlow = sensorEvents
        sensorResetResult = true

        exercisesFlow.value = listOf(
            Exercise(
                id = 1,
                name = "Bench Press",
                weight = 100f,
                reps = 10,
                sets = 4,
                usesSensor = true,
                activeInSession = true
            )
        )

        viewModel.startSession()
        runCurrent()

        sensorEvents.emit(EspSensorData(reps = 10, state = "TOP", dist = 20))
        runCurrent()

        assertEquals(
            "Sensor must still complete a set for the active sensor exercise",
            1,
            viewModel.completedSets.value[1]
        )

        localSettingsFlow.value = localSettingsFlow.value.copy(sensorEnabled = false)
        runCurrent()
        viewModel.pauseSession()
    }

    @Test
    fun `updateExercisePhoto compresses and uploads and updates repo and emits success`() = runTest {
        val source = Uri.parse("content://media/external/images/42")
        val bytes = byteArrayOf(1, 2, 3)
        val uploadedUrl = "https://example.com/photo.jpg"
        val existing = Exercise(id = 1, name = "Bench Press", weight = 100f, reps = 10, sets = 4)
        coEvery { photoProcessor.compressToJpeg(source) } returns bytes
        coEvery { photoUploader.uploadExercisePhoto(1, bytes) } returns uploadedUrl

        val resultDeferred = async { viewModel.photoUploadEvents.first() }
        viewModel.updateExercisePhoto(1, source)
        advanceUntilIdle()
        val result = resultDeferred.await()

        assertEquals(PhotoUploadResult.Success(uploadedUrl), result)
        coVerify { photoProcessor.compressToJpeg(source) }
        coVerify { photoUploader.uploadExercisePhoto(1, bytes) }
        coVerify { exerciseRepository.updateExercise(existing.copy(photoUri = uploadedUrl)) }
    }

    @Test
    fun `updateExercisePhoto emits SourceUnreadable when photoProcessor throws SourceUnreadableException`() = runTest {
        val source = Uri.parse("content://media/external/images/missing")
        coEvery { photoProcessor.compressToJpeg(source) } throws SourceUnreadableException("nope")

        val resultDeferred = async { viewModel.photoUploadEvents.first() }
        viewModel.updateExercisePhoto(1, source)
        advanceUntilIdle()
        val result = resultDeferred.await()

        assertEquals(PhotoUploadResult.SourceUnreadable, result)
        coVerify(exactly = 0) { photoUploader.uploadExercisePhoto(any(), any()) }
        coVerify(exactly = 0) { exerciseRepository.updateExercise(any()) }
    }

    @Test
    fun `updateExercisePhoto emits UploadFailed when photoUploader throws`() = runTest {
        val source = Uri.parse("content://media/external/images/42")
        val bytes = byteArrayOf(4, 5, 6)
        val failure = RuntimeException("network down")
        coEvery { photoProcessor.compressToJpeg(source) } returns bytes
        coEvery { photoUploader.uploadExercisePhoto(1, bytes) } throws failure

        val resultDeferred = async { viewModel.photoUploadEvents.first() }
        viewModel.updateExercisePhoto(1, source)
        advanceUntilIdle()
        val result = resultDeferred.await()

        assertTrue(result is PhotoUploadResult.UploadFailed)
        assertEquals(failure, (result as PhotoUploadResult.UploadFailed).cause)
        coVerify(exactly = 0) { exerciseRepository.updateExercise(any()) }
    }

    @Test
    fun `removeExercisePhoto deletes from storage and clears photoUri`() = runTest {
        val existing = Exercise(id = 1, name = "Bench Press", weight = 100f, reps = 10, sets = 4, photoUri = "https://old.example.com/p.jpg")
        exercisesFlow.value = listOf(existing)
        coEvery { photoUploader.deleteExercisePhoto(1) } returns Unit

        viewModel.removeExercisePhoto(1)
        advanceUntilIdle()

        coVerify { photoUploader.deleteExercisePhoto(1) }
        coVerify { exerciseRepository.updateExercise(existing.copy(photoUri = null)) }
    }

    @Test
    fun `removeExercisePhoto persists null even if storage delete throws`() = runTest {
        val existing = Exercise(id = 1, name = "Bench Press", weight = 100f, reps = 10, sets = 4, photoUri = "https://old.example.com/p.jpg")
        exercisesFlow.value = listOf(existing)
        coEvery { photoUploader.deleteExercisePhoto(1) } throws RuntimeException("storage unavailable")

        viewModel.removeExercisePhoto(1)
        advanceUntilIdle()

        coVerify { photoUploader.deleteExercisePhoto(1) }
        coVerify { exerciseRepository.updateExercise(existing.copy(photoUri = null)) }
    }

    @Test
    fun `updateExercisePhoto does not update repo when exerciseId is unknown but still emits success`() = runTest {
        val source = Uri.parse("content://media/external/images/42")
        val bytes = byteArrayOf(7, 8, 9)
        val uploadedUrl = "https://example.com/orphan.jpg"
        coEvery { photoProcessor.compressToJpeg(source) } returns bytes
        coEvery { photoUploader.uploadExercisePhoto(99, bytes) } returns uploadedUrl

        val resultDeferred = async { viewModel.photoUploadEvents.first() }
        viewModel.updateExercisePhoto(99, source)
        advanceUntilIdle()
        val result = resultDeferred.await()

        assertEquals(PhotoUploadResult.Success(uploadedUrl), result)
        coVerify { photoProcessor.compressToJpeg(source) }
        coVerify { photoUploader.uploadExercisePhoto(99, bytes) }
        coVerify(exactly = 0) { exerciseRepository.updateExercise(any()) }
    }

    @Test
    fun `startSession populates sessionExercises only with active exercises`() = runTest {
        val active = Exercise(id = 1, name = "Squat", weight = 80f, activeInSession = true)
        val hidden = Exercise(id = 2, name = "Lunges", weight = 20f, activeInSession = false)
        exercisesFlow.value = listOf(active, hidden)

        viewModel.startSession()
        advanceUntilIdle()

        assertEquals(listOf(active), viewModel.sessionExercises.value)
    }

    @Test
    fun `updateExercise during an active session refreshes the live sessionExercises snapshot`() = runTest {
        val exercise = Exercise(id = 1, name = "Bench Press", weight = 100f, reps = 10, sets = 4, activeInSession = true)
        exercisesFlow.value = listOf(exercise)
        coEvery { exerciseRepository.updateExercise(any()) } returns Unit

        viewModel.startSession()
        advanceUntilIdle()

        assertEquals(100f, viewModel.sessionExercises.value.single().weight)

        val updated = exercise.copy(weight = 105f)
        viewModel.updateExercise(updated)
        advanceUntilIdle()

        assertEquals(105f, viewModel.sessionExercises.value.single().weight)
        coVerify { exerciseRepository.updateExercise(updated) }
    }

    @Test
    fun `updateExercise without an active session leaves sessionExercises untouched`() = runTest {
        val exercise = Exercise(id = 1, name = "Bench Press", weight = 100f, reps = 10, sets = 4, activeInSession = true)
        exercisesFlow.value = listOf(exercise)
        coEvery { exerciseRepository.updateExercise(any()) } returns Unit

        val updated = exercise.copy(weight = 105f)
        viewModel.updateExercise(updated)
        advanceUntilIdle()

        assertTrue(viewModel.sessionExercises.value.isEmpty())
        coVerify { exerciseRepository.updateExercise(updated) }
    }


    @Test
    fun `startSession emits NoActiveExercises when all exercises are inactive`() = runTest {
        exercisesFlow.value = listOf(
            Exercise(id = 1, name = "Plank", weight = 0f, activeInSession = false),
            Exercise(id = 2, name = "Lunge", weight = 0f, activeInSession = false)
        )

        viewModel.startSession()
        advanceUntilIdle()

        assertTrue(viewModel.sessionExercises.value.isEmpty())
        val event = withTimeoutOrNull(1000) { viewModel.sessionStartErrors.firstOrNull() }
        assertEquals(SessionStartError.NoActiveExercises, event)
    }

    @Test
    fun `toggleActiveInSession flips the exercise's active flag and persists`() = runTest {
        val initial = Exercise(id = 1, name = "Squat", weight = 80f, activeInSession = true)
        exercisesFlow.value = listOf(initial)
        coEvery { exerciseRepository.updateExercise(any()) } returns Unit

        viewModel.toggleActiveInSession(initial)
        advanceUntilIdle()

        coVerify { exerciseRepository.updateExercise(initial.copy(activeInSession = false)) }
    }

    @Test
    fun `setExerciseCategory persists the new category id`() = runTest {
        val initial = Exercise(id = 1, name = "Plank", weight = 0f, categoryId = null)
        exercisesFlow.value = listOf(initial)
        coEvery { exerciseRepository.updateExercise(any()) } returns Unit

        viewModel.setExerciseCategory(exercise = initial, categoryId = "core")
        advanceUntilIdle()

        coVerify { exerciseRepository.updateExercise(initial.copy(categoryId = "core")) }
    }

    @Test
    fun `completeSession failure resets completing flag and emits non-retryable sync error`() = runTest {
        val events = mutableListOf<SyncErrorEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testDispatcher.scheduler)) {
            viewModel.syncErrorEvents.collect { events += it }
        }
        coEvery { sessionHistoryRepository.saveSession(any()) } throws IOException("rules denied")

        viewModel.completeSession(onComplete = { })
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertFalse(events[0].canRetry)
        assertEquals(SyncState.ERROR, syncStatusMonitor.status.value.state)

        coEvery { sessionHistoryRepository.saveSession(any()) } returns 42L
        var saved: WorkoutSession? = null
        viewModel.completeSession(onComplete = { saved = it })
        advanceUntilIdle()

        assertNotNull(saved)

        // In production the repository's tracked write flips the monitor back to IDLE.
        // The mocked history repo bypasses that layer, so run one successful synced
        // write to mirror the recovery path.
        coEvery { exerciseRepository.updateExercise(any()) } returns Unit
        exercisesFlow.value = listOf(Exercise(id = 1, name = "Bench", weight = 100f))
        viewModel.setExerciseCategory(exercise = Exercise(id = 1, name = "Bench", weight = 100f), categoryId = "core")
        advanceUntilIdle()

        assertEquals(SyncState.IDLE, syncStatusMonitor.status.value.state)
    }

    @Test
    fun `failed exercise write surfaces retryable error and manual retry succeeds`() = runTest {
        val events = mutableListOf<SyncErrorEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testDispatcher.scheduler)) {
            viewModel.syncErrorEvents.collect { events += it }
        }
        var failWrites = true
        coEvery { exerciseRepository.updateExercise(any()) } answers {
            if (failWrites) throw IOException("offline") else Unit
        }

        viewModel.updateExercise(Exercise(id = 1, name = "Bench Press", weight = 100f))
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0].canRetry)

        failWrites = false
        viewModel.retryPendingWrites()
        advanceUntilIdle()

        coVerify(exactly = 2) { exerciseRepository.updateExercise(any()) }
        assertNotNull(syncStatusMonitor.status.value.lastSyncedAtMillis)
    }
}
