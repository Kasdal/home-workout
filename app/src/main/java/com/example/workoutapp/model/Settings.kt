package com.example.workoutapp.model

data class Settings(
    val id: Int = 1, // Singleton
    val soundsEnabled: Boolean = true,
    val soundVolume: Float = 1.0f,
    val timerSoundType: String = "beep",
    val restCompleteSoundType: String = "chime",
    val exerciseSwitchSoundType: String = "loud",
    val celebrationSoundType: String = "cheer",
    val vibrationEnabled: Boolean = true,
    val finalCountdownEnabled: Boolean = true,
    val silentModeBehavior: String = "respect",
    val themeMode: String = "dark", // light/dark/auto
    val tutorialCompleted: Boolean = false,
    val tutorialVersion: Int = 1,
    val restTimerDuration: Int = 30,
    val exerciseSwitchDuration: Int = 90,
    val undoLastSetEnabled: Boolean = true,
    val sensorEnabled: Boolean = false,
    val sensorIpAddress: String = "192.168.0.125",
    val calorieIntensity: String = "normal"
)
