package com.example.workoutapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1, // Singleton
    val soundsEnabled: Boolean = true,
    val soundVolume: Float = 1.0f,
    val timerSoundType: String = "beep",
    val celebrationSoundType: String = "cheer",
    val themeMode: String = "dark", // light/dark/auto
    val tutorialCompleted: Boolean = false,
    val tutorialVersion: Int = 1,
    val restTimerDuration: Int = 30,
    val exerciseSwitchDuration: Int = 90
)
