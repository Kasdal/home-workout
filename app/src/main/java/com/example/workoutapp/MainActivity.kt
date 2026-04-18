package com.example.workoutapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.workoutapp.ui.history.HistoryScreen
import com.example.workoutapp.ui.navigation.Screen
import com.example.workoutapp.ui.onboarding.OnboardingScreen
import com.example.workoutapp.ui.theme.WorkoutAppTheme
import com.example.workoutapp.ui.workout.WorkoutScreen
import com.example.workoutapp.ui.auth.AuthGateScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            var appUnlocked by remember { mutableStateOf(false) }

            if (showSplash) {
                WorkoutAppTheme(darkTheme = true) {
                    com.example.workoutapp.ui.splash.SplashScreen(
                        onTimeout = { showSplash = false }
                    )
                }
            } else if (!appUnlocked) {
                WorkoutAppTheme(darkTheme = true) {
                    AuthGateScreen(
                        onReady = { appUnlocked = true }
                    )
                }
            } else {
                val mainViewModel: MainViewModel = hiltViewModel()
                val startDestination by mainViewModel.startDestination.collectAsState()
                val settings by mainViewModel.settings.collectAsState(initial = null)

                WorkoutAppTheme(
                    darkTheme = when (settings?.themeMode) {
                        "light" -> false
                        "dark" -> true
                        else -> isSystemInDarkTheme()
                    }
                ) {
                    if (startDestination != null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = startDestination!!) {
                            composable(Screen.Onboarding.route) {
                                OnboardingScreen(navController = navController)
                            }
                            composable(Screen.Workout.route) {
                                WorkoutScreen(navController = navController)
                            }
                            composable(Screen.History.route) {
                                HistoryScreen(navController = navController)
                            }
                            composable(Screen.Profile.route) {
                                com.example.workoutapp.ui.profile.ProfileScreen(navController = navController)
                            }
                            composable(Screen.Settings.route) {
                                com.example.workoutapp.ui.settings.SettingsScreen(navController = navController)
                            }
                            composable(Screen.Tutorial.route) {
                                com.example.workoutapp.ui.tutorial.TutorialScreen(navController = navController)
                            }
                            composable(Screen.RestDays.route) {
                                com.example.workoutapp.ui.restdays.RestDaysScreen(navController = navController)
                            }
                            composable(Screen.About.route) {
                                com.example.workoutapp.ui.about.AboutScreen(navController = navController)
                            }
                            composable(Screen.Workouts.route) {
                                com.example.workoutapp.ui.workouts.WorkoutsScreen(navController = navController)
                            }
                            composable(Screen.Insights.route) {
                                com.example.workoutapp.ui.insights.InsightsScreen(navController = navController)
                            }
                        }

                    }
                    } else {
                        Surface(color = MaterialTheme.colorScheme.background) {}
                    }
                }
            }
        }
    }
}
