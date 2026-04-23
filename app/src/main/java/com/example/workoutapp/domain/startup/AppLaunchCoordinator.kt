package com.example.workoutapp.domain.startup

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.remote.FirestoreRepository
import com.example.workoutapp.data.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AppEntryState {
    data object AuthRequired : AppEntryState
    data object MigrationInProgress : AppEntryState
    data class Ready(val startDestination: String) : AppEntryState
}

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class AppLaunchCoordinator @Inject constructor(
    private val repository: ProfileRepository,
    private val firestoreRepository: FirestoreRepository,
    private val authManager: AuthManager
) {
    private val backupImportPending = MutableStateFlow(false)

    fun setBackupImportPending(isPending: Boolean) {
        backupImportPending.value = isPending
    }

    fun appEntryState(): Flow<AppEntryState> {
        return authManager.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf<AppEntryState>(AppEntryState.AuthRequired)
            } else {
                var lastReadyState: AppEntryState.Ready? = null

                combine(
                    firestoreRepository.observeMigrationMeta(user.uid),
                    backupImportPending
                ) { migrationMeta, isBackupImportPending -> migrationMeta to isBackupImportPending }
                    .transformLatest { (migrationMeta, isBackupImportPending) ->
                        when {
                            isBackupImportPending || migrationMeta?.backupImportPending == true -> emit(AppEntryState.MigrationInProgress)
                            migrationMeta?.migrationComplete == true -> {
                                emitAll(
                                    repository.getUserMetrics()
                                        .map { metrics ->
                                            AppEntryState.Ready(
                                                startDestination = if (metrics != null) "workout" else "onboarding"
                                            )
                                        }
                                        .onEach { lastReadyState = it }
                                )
                            }
                            migrationMeta == null && lastReadyState != null -> emit(lastReadyState!!)
                            else -> emit(AppEntryState.MigrationInProgress)
                        }
                    }
            }
        }.distinctUntilChanged()
    }
}
