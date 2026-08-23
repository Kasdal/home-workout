package com.example.workoutapp.ui.history

import com.example.workoutapp.data.repository.SessionHistoryRepository
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.WorkoutSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SessionHistoryRepository
    private lateinit var viewModel: HistoryViewModel

    private val session = WorkoutSession(
        id = 12,
        date = 1_755_000_000_000,
        durationSeconds = 3_600L,
        totalWeightLifted = 2_400f,
        caloriesBurned = 310f
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.getSessions() } returns flowOf(listOf(session))
        every { repository.getAllSessionExercises() } returns flowOf(emptyList())
        viewModel = HistoryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `openSessionDetail exposes the session and close clears it`() = runTest {
        viewModel.openSessionDetail(session)
        advanceUntilIdle()
        assertEquals(session, viewModel.selectedSession.value)

        viewModel.closeSessionDetail()
        advanceUntilIdle()
        assertNull(viewModel.selectedSession.value)
    }

    @Test
    fun `selectedSessionExercises streams exercises for the opened session`() = runTest {
        val entries = listOf(
            SessionExercise(
                id = 1,
                sessionId = 12,
                exerciseName = "Bench Press",
                weight = 60f,
                sets = 3,
                reps = 8,
                volume = 1_440f,
                sortOrder = 0
            ),
            SessionExercise(
                id = 2,
                sessionId = 12,
                exerciseName = "Row",
                weight = 50f,
                sets = 3,
                reps = 10,
                volume = 1_500f,
                sortOrder = 1
            )
        )
        every { repository.getSessionExercises(12) } returns flowOf(entries)

        viewModel.openSessionDetail(session)
        val emitted = mutableListOf<List<SessionExercise>>()
        val job = launch {
            viewModel.selectedSessionExercises.collect { emitted.add(it) }
        }
        advanceUntilIdle()

        assertEquals(1, emitted.size)
        assertEquals(listOf("Bench Press", "Row"), emitted.last().map { it.exerciseName })
        verify(exactly = 1) { repository.getSessionExercises(12) }
        job.cancel()
    }

    @Test
    fun `selectedSessionExercises stays empty while no session is open`() = runTest {
        var latest: List<SessionExercise>? = null
        val job = launch {
            viewModel.selectedSessionExercises.collect { latest = it }
        }
        advanceUntilIdle()

        assertEquals(emptyList<SessionExercise>(), latest)
        verify(exactly = 0) { repository.getSessionExercises(any()) }
        job.cancel()
    }
}
