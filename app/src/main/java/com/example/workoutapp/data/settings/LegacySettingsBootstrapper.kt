package com.example.workoutapp.data.settings

import com.example.workoutapp.data.local.dao.WorkoutDao
import com.example.workoutapp.data.local.room.entity.toDomain
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacySettingsBootstrapper @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val localAppPreferencesRepository: LocalAppPreferencesRepository
) {
    suspend fun seedFromLegacySettingsIfPresent() {
        workoutDao.getSettings().first()?.toDomain()?.let { settings ->
            localAppPreferencesRepository.seedFromLegacySettingsIfUnset(settings)
        }
    }
}
