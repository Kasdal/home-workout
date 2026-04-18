package com.example.workoutapp.domain.startup

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.repository.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class AppLaunchCoordinator @Inject constructor(
    private val repository: WorkoutRepository,
    private val authManager: AuthManager
) {
    fun startDestination(): Flow<String?> {
        return authManager.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(null)
            } else {
                repository.getUserMetrics().flatMapLatest { metrics ->
                    flowOf(if (metrics != null) "workout" else "onboarding")
                }
            }
        }.distinctUntilChanged()
    }
}
