package com.example.workoutapp.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.auth.CredentialStateClearer
import com.example.workoutapp.data.remote.MigrationBootstrapResult
import com.example.workoutapp.data.remote.MigrationConflictException
import com.example.workoutapp.data.settings.MigrationPreferences
import com.example.workoutapp.domain.startup.AppLaunchCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val isMigrationComplete: Boolean = false,
    val awaitingBackupImport: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val importConflict: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val credentialStateClearer: CredentialStateClearer,
    private val authMigrationCoordinator: AuthMigrationCoordinator,
    private val appLaunchCoordinator: AppLaunchCoordinator,
    private val migrationPreferences: MigrationPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                if (user == null) {
                    updateState {
                        AuthUiState(
                            isLoading = false,
                            isSignedIn = false,
                            isMigrationComplete = false,
                            awaitingBackupImport = false,
                            infoMessage = null,
                            errorMessage = null,
                            importConflict = false
                        )
                    }
                    return@collect
                }

                updateState {
                    it.copy(
                        isSignedIn = true,
                        isLoading = false,
                        awaitingBackupImport = false,
                        infoMessage = null,
                        errorMessage = null,
                        importConflict = false
                    )
                }

                migrate(user.uid)
            }
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            val result = authManager.signInWithGoogleIdToken(idToken)
            result.exceptionOrNull()?.let { ex ->
                Log.e("AuthViewModel", "Firebase sign-in failed", ex)
            }
            updateState {
                it.copy(
                    isLoading = false,
                    infoMessage = null,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun onSignInError(message: String) {
        updateState {
            it.copy(
                isLoading = false,
                infoMessage = null,
                errorMessage = message,
                importConflict = false
            )
        }
    }

    fun importLegacyBackup(backupJson: String) {
        val uid = authManager.currentUserId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, infoMessage = null, errorMessage = null, importConflict = false) }
            val result = authMigrationCoordinator.importLegacyBackup(uid, backupJson)
            val failure = result.exceptionOrNull()
            updateState {
                it.copy(
                    isLoading = false,
                    isMigrationComplete = result.isSuccess,
                    awaitingBackupImport = !result.isSuccess,
                    infoMessage = null,
                    errorMessage = failure?.message,
                    importConflict = failure is MigrationConflictException
                )
            }
        }
    }

    fun continueWithoutImport() {
        val uid = authManager.currentUserId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null, importConflict = false) }
            val result = authMigrationCoordinator.continueWithoutBackupImport(uid)
            updateState {
                it.copy(
                    isLoading = false,
                    isMigrationComplete = result.isSuccess,
                    awaitingBackupImport = !result.isSuccess,
                    infoMessage = null,
                    errorMessage = result.exceptionOrNull()?.message,
                    importConflict = false
                )
            }
        }
    }

    fun retryMigration() {
        val uid = authManager.currentUserId() ?: return
        viewModelScope.launch {
            migrate(uid)
        }
    }

    fun signOut() {
        authManager.signOut()
        viewModelScope.launch {
            runCatching { credentialStateClearer.clear() }.onFailure { ex ->
                Log.w("AuthViewModel", "Failed to clear credential state", ex)
            }
            migrationPreferences.clear()
        }
    }

    private suspend fun migrate(uid: String) {
        updateState {
            it.copy(
                isLoading = true,
                errorMessage = null,
                isMigrationComplete = false,
                importConflict = false
            )
        }
        val migrationResult = authMigrationCoordinator.migrateIfNeeded(uid)
        updateState { currentState ->
            migrationResult.fold(
                onSuccess = { result ->
                    when (result) {
                        MigrationBootstrapResult.READY -> currentState.copy(
                            isLoading = false,
                            isMigrationComplete = true,
                            awaitingBackupImport = false,
                            infoMessage = null,
                            errorMessage = null,
                            importConflict = false
                        )

                        MigrationBootstrapResult.NEEDS_BACKUP_IMPORT -> currentState.copy(
                            isLoading = false,
                            isMigrationComplete = false,
                            awaitingBackupImport = true,
                            infoMessage = "Import a backup file if you have one, or continue without importing.",
                            errorMessage = null,
                            importConflict = false
                        )
                    }
                },
                onFailure = {
                    currentState.copy(
                        isLoading = false,
                        isMigrationComplete = false,
                        awaitingBackupImport = false,
                        infoMessage = null,
                        errorMessage = it.message,
                        importConflict = false
                    )
                }
            )
        }
    }

    private fun updateState(transform: (AuthUiState) -> AuthUiState) {
        _state.value = transform(_state.value)
        appLaunchCoordinator.setBackupImportPending(_state.value.awaitingBackupImport)
    }
}
