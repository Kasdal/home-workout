package com.example.workoutapp.data.settings

import kotlinx.coroutines.test.runTest
import org.junit.Test

class LegacySettingsBootstrapperTest {

    @Test
    fun `seedFromLegacySettingsIfPresent is a no-op without legacy room data`() = runTest {
        val bootstrapper = LegacySettingsBootstrapper()

        bootstrapper.seedFromLegacySettingsIfPresent()
    }
}
