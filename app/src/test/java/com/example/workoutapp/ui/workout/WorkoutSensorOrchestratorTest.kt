package com.example.workoutapp.ui.workout

import com.example.workoutapp.data.remote.EspSensorData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSensorOrchestratorTest {

    @Test
    fun `start updates snapshot from polled sensor status`() = runTest {
        val orchestrator = WorkoutSensorOrchestrator(
            scope = this,
            pollSensorStatus = { _, _ ->
                flowOf(EspSensorData(reps = 3, state = "LIFTING", dist = 42))
            }
        )

        orchestrator.start(ipAddress = "192.168.0.10")
        advanceUntilIdle()

        val snapshot = orchestrator.sensorSnapshot.value
        assertTrue(snapshot.connected)
        assertEquals(3, snapshot.reps)
        assertEquals("LIFTING", snapshot.state)
        assertEquals(42, snapshot.distance)
    }

    @Test
    fun `polling failures and stop reset snapshot to disconnected rest state`() = runTest {
        val orchestrator = WorkoutSensorOrchestrator(
            scope = this,
            pollSensorStatus = { _, _ ->
                flow {
                    emit(EspSensorData(reps = 2, state = "BOTTOM", dist = 18))
                    throw IllegalStateException("boom")
                }
            }
        )

        orchestrator.start(ipAddress = "192.168.0.10")
        advanceUntilIdle()

        val disconnectedSnapshot = orchestrator.sensorSnapshot.value
        assertFalse(disconnectedSnapshot.connected)
        assertEquals(2, disconnectedSnapshot.reps)
        assertEquals("BOTTOM", disconnectedSnapshot.state)
        assertEquals(18, disconnectedSnapshot.distance)

        orchestrator.stop()

        val stoppedSnapshot = orchestrator.sensorSnapshot.value
        assertFalse(stoppedSnapshot.connected)
        assertEquals(0, stoppedSnapshot.reps)
        assertEquals("REST", stoppedSnapshot.state)
        assertEquals(0, stoppedSnapshot.distance)
    }

    @Test
    fun `isPolling becomes false when polling coroutine completes`() = runTest {
        val orchestrator = WorkoutSensorOrchestrator(
            scope = this,
            pollSensorStatus = { _, _ ->
                flowOf(EspSensorData(reps = 1, state = "TOP", dist = 7))
            }
        )

        orchestrator.start(ipAddress = "192.168.0.10")

        assertTrue(orchestrator.isPolling)

        advanceUntilIdle()

        assertFalse(orchestrator.isPolling)
    }

    @Test
    fun `crossing target threshold triggers set completion once`() = runTest {
        val completedExerciseIds = mutableListOf<Int>()
        val orchestrator = WorkoutSensorOrchestrator(
            scope = this,
            pollSensorStatus = { _, _ ->
                flow {
                    emit(EspSensorData(reps = 1, state = "BOTTOM", dist = 10))
                    emit(EspSensorData(reps = 3, state = "LIFTING", dist = 11))
                    emit(EspSensorData(reps = 4, state = "TOP", dist = 12))
                    emit(EspSensorData(reps = 6, state = "TOP", dist = 13))
                }
            },
            currentSetCompletionTarget = {
                SensorSetCompletionTarget(exerciseId = 99, targetReps = 4)
            },
            onSetCompletionTriggered = completedExerciseIds::add
        )

        orchestrator.start(ipAddress = "192.168.0.10")
        advanceUntilIdle()

        assertEquals(listOf(99), completedExerciseIds)
    }

    @Test
    fun `resetRepTracking allows a new threshold crossing`() = runTest {
        val completedExerciseIds = mutableListOf<Int>()
        var pollCount = 0
        val orchestrator = WorkoutSensorOrchestrator(
            scope = this,
            pollSensorStatus = { _, _ ->
                pollCount += 1
                flowOf(EspSensorData(reps = 4, state = "TOP", dist = pollCount))
            },
            currentSetCompletionTarget = {
                SensorSetCompletionTarget(exerciseId = 12, targetReps = 4)
            },
            onSetCompletionTriggered = completedExerciseIds::add
        )

        orchestrator.start(ipAddress = "192.168.0.10")
        advanceUntilIdle()
        orchestrator.resetRepTracking()
        orchestrator.start(ipAddress = "192.168.0.10")
        advanceUntilIdle()

        assertEquals(listOf(12, 12), completedExerciseIds)
    }

    @Test
    fun `start resets rep tracking before a restarted polling session`() = runTest {
        val completedExerciseIds = mutableListOf<Int>()
        var pollCount = 0
        val orchestrator = WorkoutSensorOrchestrator(
            scope = this,
            pollSensorStatus = { _, _ ->
                pollCount += 1
                when (pollCount) {
                    1 -> flowOf(EspSensorData(reps = 8, state = "TOP", dist = 20))
                    else -> flowOf(EspSensorData(reps = 4, state = "TOP", dist = 21))
                }
            },
            currentSetCompletionTarget = {
                SensorSetCompletionTarget(exerciseId = 7, targetReps = 4)
            },
            onSetCompletionTriggered = completedExerciseIds::add
        )

        orchestrator.start(ipAddress = "192.168.0.10")
        advanceUntilIdle()
        orchestrator.start(ipAddress = "192.168.0.10")
        advanceUntilIdle()

        assertEquals(listOf(7, 7), completedExerciseIds)
    }

    @Test
    fun `successful sensor completion schedules counter reset and clears local reps`() = runTest {
        val resetRequests = mutableListOf<String>()
        val orchestrator = WorkoutSensorOrchestrator(
            scope = this,
            pollSensorStatus = { _, _ ->
                flowOf(EspSensorData(reps = 4, state = "TOP", dist = 15))
            },
            currentSetCompletionTarget = {
                SensorSetCompletionTarget(exerciseId = 21, targetReps = 4)
            },
            onSetCompletionTriggered = { true },
            resetCounter = {
                resetRequests += it
                true
            },
            completionResetDelayMs = 1000
        )

        orchestrator.start(ipAddress = "192.168.0.10")
        runCurrent()

        assertEquals(4, orchestrator.sensorSnapshot.value.reps)
        assertTrue(resetRequests.isEmpty())

        advanceTimeBy(1000)
        advanceUntilIdle()

        assertEquals(listOf("192.168.0.10"), resetRequests)
        assertEquals(0, orchestrator.sensorSnapshot.value.reps)
    }

    @Test
    fun `stop cancels pending delayed counter reset`() = runTest {
        val resetRequests = mutableListOf<String>()
        val orchestrator = WorkoutSensorOrchestrator(
            scope = this,
            pollSensorStatus = { _, _ ->
                flowOf(EspSensorData(reps = 4, state = "TOP", dist = 15))
            },
            currentSetCompletionTarget = {
                SensorSetCompletionTarget(exerciseId = 21, targetReps = 4)
            },
            onSetCompletionTriggered = { true },
            resetCounter = {
                resetRequests += it
                true
            },
            completionResetDelayMs = 1000
        )

        orchestrator.start(ipAddress = "192.168.0.10")
        runCurrent()
        orchestrator.stop()

        advanceTimeBy(1000)
        advanceUntilIdle()

        assertTrue(resetRequests.isEmpty())
        assertEquals(0, orchestrator.sensorSnapshot.value.reps)
    }

    @Test
    fun `restart cancels prior delayed reset so old session cannot clear new snapshot`() = runTest {
        val resetRequests = mutableListOf<String>()
        var pollCount = 0
        val orchestrator = WorkoutSensorOrchestrator(
            scope = this,
            pollSensorStatus = { _, _ ->
                pollCount += 1
                when (pollCount) {
                    1 -> flowOf(EspSensorData(reps = 4, state = "TOP", dist = 15))
                    else -> flowOf(EspSensorData(reps = 9, state = "TOP", dist = 30))
                }
            },
            currentSetCompletionTarget = {
                SensorSetCompletionTarget(exerciseId = 21, targetReps = 4)
            },
            onSetCompletionTriggered = { true },
            resetCounter = {
                resetRequests += it
                true
            },
            completionResetDelayMs = 1000
        )

        orchestrator.start(ipAddress = "192.168.0.10")
        runCurrent()
        orchestrator.start(ipAddress = "192.168.0.11")
        runCurrent()

        advanceTimeBy(1000)
        advanceUntilIdle()

        assertEquals(listOf("192.168.0.11"), resetRequests)
        assertEquals(0, orchestrator.sensorSnapshot.value.reps)
    }

    @Test
    fun `failed counter reset keeps local state and avoids duplicate completion on unchanged reading`() = runTest {
        val completedExerciseIds = mutableListOf<Int>()
        val orchestrator = WorkoutSensorOrchestrator(
            scope = this,
            pollSensorStatus = { _, _ ->
                flow {
                    emit(EspSensorData(reps = 4, state = "TOP", dist = 15))
                    delay(1500)
                    emit(EspSensorData(reps = 4, state = "TOP", dist = 16))
                }
            },
            currentSetCompletionTarget = {
                SensorSetCompletionTarget(exerciseId = 21, targetReps = 4)
            },
            onSetCompletionTriggered = {
                completedExerciseIds += it
                true
            },
            resetCounter = { false },
            completionResetDelayMs = 1000
        )

        orchestrator.start(ipAddress = "192.168.0.10")
        runCurrent()
        advanceTimeBy(1500)
        advanceUntilIdle()

        assertEquals(listOf(21), completedExerciseIds)
        assertEquals(4, orchestrator.sensorSnapshot.value.reps)
    }
}
