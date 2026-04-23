package com.example.workoutapp.data.settings

import com.example.workoutapp.data.remote.LegacyMigrationDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacySettingsBootstrapper @Inject constructor(
    private val legacyMigrationDataSource: LegacyMigrationDataSource,
    private val localAppPreferencesRepository: LocalAppPreferencesRepository
) {
    suspend fun seedFromLegacySettingsIfPresent() {
        legacyMigrationDataSource.loadSettings()?.let { settings ->
            localAppPreferencesRepository.seedFromLegacySettingsIfUnset(settings)
        }
    }
}
