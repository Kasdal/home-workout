package com.example.workoutapp.data.repository

import com.example.workoutapp.model.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<Settings?>
}
