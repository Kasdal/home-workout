package com.example.workoutapp.ui.settings

data class SettingsScreenState(
    val soundsEnabled: Boolean = true,
    val soundVolume: Float = 1.0f,
    val timerSoundType: String = "beep",
    val celebrationSoundType: String = "cheer",
    val themeMode: String = "dark",
    val tutorialCompleted: Boolean = false,
    val tutorialVersion: Int = 1,
    val restTimerDuration: Int = 30,
    val exerciseSwitchDuration: Int = 90,
    val undoLastSetEnabled: Boolean = true,
    val sensorEnabled: Boolean = false,
    val sensorIpAddress: String = "192.168.0.125"
)
