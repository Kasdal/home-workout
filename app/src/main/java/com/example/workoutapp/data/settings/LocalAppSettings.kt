package com.example.workoutapp.data.settings

data class LocalAppSettings(
    val themeMode: String = "dark",
    val soundsEnabled: Boolean = true,
    val soundVolume: Float = 1.0f,
    val timerSoundType: String = "beep",
    val restCompleteSoundType: String = "chime",
    val exerciseSwitchSoundType: String = "loud",
    val celebrationSoundType: String = "cheer",
    val vibrationEnabled: Boolean = true,
    val finalCountdownEnabled: Boolean = true,
    val silentModeBehavior: String = "respect",
    val tutorialCompleted: Boolean = false,
    val tutorialVersion: Int = 1,
    val sensorEnabled: Boolean = false,
    val sensorIpAddress: String = "192.168.0.125"
)
