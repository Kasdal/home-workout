package com.example.workoutapp.data.settings

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SyncedWorkoutSettingsRepositoryTest {

    private lateinit var syncedWorkoutSettingsStore: SyncedWorkoutSettingsStore
    private lateinit var syncedWorkoutSettingsRepository: SyncedWorkoutSettingsRepository

    @Before
    fun setup() {
        syncedWorkoutSettingsStore = mockk(relaxed = true)
        syncedWorkoutSettingsRepository = SyncedWorkoutSettingsRepository(syncedWorkoutSettingsStore)
    }

    @Test
    fun `observeSessionSettings returns synced workout settings from repository`() = runTest {
        val syncedSettings = WorkoutSessionSettings(
            restTimerDuration = 45,
            exerciseSwitchDuration = 120,
            undoLastSetEnabled = false
        )
        every { syncedWorkoutSettingsStore.observeSyncedWorkoutSettings() } returns flowOf(syncedSettings)

        assertEquals(syncedSettings, syncedWorkoutSettingsRepository.observeSessionSettings().first())
    }

    @Test
    fun `setRestTimerDuration saves updated synced workout settings`() = runTest {
        val currentSettings = WorkoutSessionSettings()
        every { syncedWorkoutSettingsStore.observeSyncedWorkoutSettings() } returns flowOf(currentSettings)
        coEvery { syncedWorkoutSettingsStore.saveSyncedWorkoutSettings(any()) } returns Unit

        syncedWorkoutSettingsRepository.setRestTimerDuration(45)

        coVerify {
            syncedWorkoutSettingsStore.saveSyncedWorkoutSettings(currentSettings.copy(restTimerDuration = 45))
        }
    }

    @Test
    fun `setExerciseSwitchDuration saves updated synced workout settings`() = runTest {
        val currentSettings = WorkoutSessionSettings()
        every { syncedWorkoutSettingsStore.observeSyncedWorkoutSettings() } returns flowOf(currentSettings)
        coEvery { syncedWorkoutSettingsStore.saveSyncedWorkoutSettings(any()) } returns Unit

        syncedWorkoutSettingsRepository.setExerciseSwitchDuration(120)

        coVerify {
            syncedWorkoutSettingsStore.saveSyncedWorkoutSettings(currentSettings.copy(exerciseSwitchDuration = 120))
        }
    }

    @Test
    fun `setUndoLastSetEnabled saves updated synced workout settings`() = runTest {
        val currentSettings = WorkoutSessionSettings()
        every { syncedWorkoutSettingsStore.observeSyncedWorkoutSettings() } returns flowOf(currentSettings)
        coEvery { syncedWorkoutSettingsStore.saveSyncedWorkoutSettings(any()) } returns Unit

        syncedWorkoutSettingsRepository.setUndoLastSetEnabled(false)

        coVerify {
            syncedWorkoutSettingsStore.saveSyncedWorkoutSettings(currentSettings.copy(undoLastSetEnabled = false))
        }
    }

    @Test
    fun `setCalorieIntensity saves updated synced workout settings`() = runTest {
        val currentSettings = WorkoutSessionSettings()
        every { syncedWorkoutSettingsStore.observeSyncedWorkoutSettings() } returns flowOf(currentSettings)
        coEvery { syncedWorkoutSettingsStore.saveSyncedWorkoutSettings(any()) } returns Unit

        syncedWorkoutSettingsRepository.setCalorieIntensity("hard")

        coVerify {
            syncedWorkoutSettingsStore.saveSyncedWorkoutSettings(currentSettings.copy(calorieIntensity = "hard"))
        }
    }
}
