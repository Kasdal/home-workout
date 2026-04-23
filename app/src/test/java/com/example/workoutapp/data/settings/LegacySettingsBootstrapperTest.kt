package com.example.workoutapp.data.settings

import com.example.workoutapp.data.remote.LegacyMigrationDataSource
import com.example.workoutapp.model.Settings
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class LegacySettingsBootstrapperTest {

    private lateinit var legacyMigrationDataSource: LegacyMigrationDataSource
    private lateinit var localAppPreferencesRepository: LocalAppPreferencesRepository
    private lateinit var bootstrapper: LegacySettingsBootstrapper

    @Before
    fun setup() {
        legacyMigrationDataSource = mockk()
        localAppPreferencesRepository = mockk(relaxed = true)
        bootstrapper = LegacySettingsBootstrapper(legacyMigrationDataSource, localAppPreferencesRepository)
    }

    @Test
    fun `seedFromLegacySettingsIfPresent seeds local preferences when legacy settings exist`() = runTest {
        val legacySettings = Settings(themeMode = "light", soundsEnabled = false)
        coEvery { legacyMigrationDataSource.loadSettings() } returns legacySettings

        bootstrapper.seedFromLegacySettingsIfPresent()

        coVerify { localAppPreferencesRepository.seedFromLegacySettingsIfUnset(legacySettings) }
    }

    @Test
    fun `seedFromLegacySettingsIfPresent does nothing when legacy settings are missing`() = runTest {
        coEvery { legacyMigrationDataSource.loadSettings() } returns null

        bootstrapper.seedFromLegacySettingsIfPresent()

        coVerify(exactly = 0) { localAppPreferencesRepository.seedFromLegacySettingsIfUnset(any()) }
    }
}
