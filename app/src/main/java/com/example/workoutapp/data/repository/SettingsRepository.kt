package com.example.workoutapp.data.repository

import com.example.workoutapp.data.local.entity.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<Settings?>
}
