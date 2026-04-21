package com.example.workoutapp.ui.workout

import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.ExerciseSessionMode
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.local.entity.WorkoutSession
import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.data.repository.ProfileRepository
import com.example.workoutapp.data.repository.SessionHistoryRepository
import com.example.workoutapp.data.settings.LegacySettingsBootstrapper
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.LocalAppSettings
import com.example.workoutapp.data.settings.SyncedWorkoutSettingsRepository
import com.example.workoutapp.data.settings.WorkoutSessionSettings
import com.example.workoutapp.data.remote.EspSensorData
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
    private lateinit var sensorOrchestratorFactory: WorkoutSensorOrchestratorFactory
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
        sensorOrchestratorFactory = mockk()
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
        every { countdownOrchestratorFactory.create(any(), any()) } answers {
            com.example.workoutapp.domain.session.WorkoutCountdownOrchestrator(
                scope = firstArg(),
                onTimerSound = secondArg()
            )
        }
        every { sessionClockFactory.create(any()) } answers {
            com.example.workoutapp.domain.session.WorkoutSessionClock(firstArg())
        }
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
            profileRepository,
            legacySettingsBootstrapper,
            localAppPreferencesRepository,
            syncedWorkoutSettingsRepository,
            soundManager,
            sessionCoordinator,
            countdownOrchestratorFactory,
            sessionClockFactory,
            sensorOrchestratorFactory
        )
    }

    private fun clearViewModel() {
        val method = WorkoutViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)
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

        verify(exactly = 1) { countdownOrchestratorFactory.create(any(), any()) }
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

        viewModel.completeSession { session ->
            assertEquals(0L, session.durationSeconds)
            assertEquals(1000f, session.totalWeightLifted) // 1 set * 10 reps * 100 weight
        }
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

        viewModel.completeSession { }
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
}
