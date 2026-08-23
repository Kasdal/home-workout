package com.example.workoutapp.domain.startup

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.remote.FirestoreRepository
import com.example.workoutapp.data.repository.ProfileRepository
import com.example.workoutapp.data.settings.MigrationPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AppEntryState {
    data object AuthRequired : AppEntryState
    data object MigrationInProgress : AppEntryState
    data object StartupFailed : AppEntryState
    data class Ready(val startDestination: String) : AppEntryState
}

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class AppLaunchCoordinator @Inject constructor(
    private val repository: ProfileRepository,
    private val firestoreRepository: FirestoreRepository,
    private val authManager: AuthManager,
    private val migrationPreferences: MigrationPreferences
) {
    private val backupImportPending = MutableStateFlow(false)
    private val startupRetryToken = MutableStateFlow(0)

    fun setBackupImportPending(isPending: Boolean) {
        backupImportPending.value = isPending
    }

    fun retryStartup() {
        startupRetryToken.value += 1
    }

    fun appEntryState(): Flow<AppEntryState> {
        return authManager.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf<AppEntryState>(AppEntryState.AuthRequired)
            } else {
                startupRetryToken.flatMapLatest { startupFlow(user.uid) }
            }
        }.distinctUntilChanged()
    }

    private fun startupFlow(uid: String): Flow<AppEntryState> {
        var lastReadyState: AppEntryState.Ready? = null

        return combine(
            firestoreRepository.observeMigrationMeta(uid).onStart { emit(null) },
            backupImportPending,
            migrationPreferences.migrationComplete
        ) { migrationMeta, isBackupImportPending, locallyComplete ->
            Triple(migrationMeta, isBackupImportPending, locallyComplete)
        }.transformLatest { (migrationMeta, isBackupImportPending, locallyComplete) ->
            when {
                isBackupImportPending || migrationMeta?.backupImportPending == true -> emit(AppEntryState.MigrationInProgress)

                migrationMeta?.migrationComplete == true -> emitAll(
                    repository.getUserMetrics()
                        .map { metrics ->
                            AppEntryState.Ready(
                                startDestination = if (metrics != null) DESTINATION_WORKOUT else DESTINATION_ONBOARDING
                            )
                        }
                        .onEach { ready ->
                            lastReadyState = ready
                            migrationPreferences.markMigrationComplete(ready.startDestination)
                        }
                )

                migrationMeta == null && lastReadyState != null -> emit(lastReadyState!!)

                locallyComplete -> emit(
                    AppEntryState.Ready(
                        startDestination = migrationPreferences.lastStartDestination.first()
                            ?: lastReadyState?.startDestination
                            ?: DESTINATION_ONBOARDING
                    )
                )

                else -> {
                    emit(AppEntryState.MigrationInProgress)
                    delay(STARTUP_TIMEOUT_MS)
                    emit(AppEntryState.StartupFailed)
                }
            }
        }
    }

    companion object {
        const val STARTUP_TIMEOUT_MS = 8_000L
        const val DESTINATION_WORKOUT = "workout"
        const val DESTINATION_ONBOARDING = "onboarding"
    }
}
