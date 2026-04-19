package com.example.workoutapp.data.settings

import com.example.workoutapp.data.local.dao.WorkoutDao
import com.example.workoutapp.data.local.room.entity.RoomSettings
import com.example.workoutapp.data.local.room.entity.toDomain
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class LegacySettingsBootstrapperTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var localAppPreferencesRepository: LocalAppPreferencesRepository
    private lateinit var bootstrapper: LegacySettingsBootstrapper

    @Before
    fun setup() {
        workoutDao = mockk()
        localAppPreferencesRepository = mockk(relaxed = true)
        bootstrapper = LegacySettingsBootstrapper(workoutDao, localAppPreferencesRepository)
    }

    @Test
    fun `seedFromLegacySettingsIfPresent seeds local preferences when legacy settings exist`() = runTest {
        val legacySettings = RoomSettings(themeMode = "light", soundsEnabled = false)
        every { workoutDao.getSettings() } returns flowOf(legacySettings)

        bootstrapper.seedFromLegacySettingsIfPresent()

        coVerify { localAppPreferencesRepository.seedFromLegacySettingsIfUnset(legacySettings.toDomain()) }
    }

    @Test
    fun `seedFromLegacySettingsIfPresent does nothing when legacy settings are missing`() = runTest {
        every { workoutDao.getSettings() } returns flowOf(null)

        bootstrapper.seedFromLegacySettingsIfPresent()

        coVerify(exactly = 0) { localAppPreferencesRepository.seedFromLegacySettingsIfUnset(any()) }
    }
}
