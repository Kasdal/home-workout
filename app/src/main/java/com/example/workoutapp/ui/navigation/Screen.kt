package com.example.workoutapp.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Workout : Screen("workout")
    object History : Screen("history")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Tutorial : Screen("tutorial")
    object RestDays : Screen("rest_days")
}

