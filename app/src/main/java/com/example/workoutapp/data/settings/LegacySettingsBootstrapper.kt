package com.example.workoutapp.data.settings

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacySettingsBootstrapper @Inject constructor() {
    suspend fun seedFromLegacySettingsIfPresent() = Unit
}
