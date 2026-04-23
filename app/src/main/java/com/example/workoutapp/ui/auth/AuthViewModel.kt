package com.example.workoutapp.ui.auth

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.auth.GoogleSignInClientFactory
import com.example.workoutapp.data.remote.MigrationBootstrapResult
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
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val googleSignInClientFactory: GoogleSignInClientFactory,
    private val authMigrationCoordinator: AuthMigrationCoordinator
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
                _state.value = AuthUiState(
                    isLoading = false,
                    isSignedIn = false,
                    isMigrationComplete = false,
                    awaitingBackupImport = false,
                    infoMessage = null,
                    errorMessage = null
                )
                    return@collect
                }

                _state.value = _state.value.copy(
                    isSignedIn = true,
                    isLoading = false,
                    awaitingBackupImport = false,
                    infoMessage = null,
                    errorMessage = null
                )

                migrate(user.uid)
            }
        }
    }

    fun buildGoogleSignInIntent(): Intent {
        return googleSignInClientFactory.create().signInIntent
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val result = authManager.signInWithGoogleIdToken(idToken)
            result.exceptionOrNull()?.let { ex ->
                Log.e("AuthViewModel", "Firebase sign-in failed", ex)
            }
            _state.value = _state.value.copy(
                isLoading = false,
                infoMessage = null,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun onSignInError(message: String) {
        _state.value = _state.value.copy(
            isLoading = false,
            infoMessage = null,
            errorMessage = message
        )
    }

    fun importLegacyBackup(backupJson: String) {
        val uid = authManager.currentUserId() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, infoMessage = null, errorMessage = null)
            val result = authMigrationCoordinator.importLegacyBackup(uid, backupJson)
            _state.value = _state.value.copy(
                isLoading = false,
                isMigrationComplete = result.isSuccess,
                awaitingBackupImport = !result.isSuccess,
                infoMessage = null,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun continueWithoutImport() {
        _state.value = _state.value.copy(
            isMigrationComplete = true,
            awaitingBackupImport = false,
            infoMessage = null,
            errorMessage = null
        )
    }

    fun retryMigration() {
        val uid = authManager.currentUserId() ?: return
        viewModelScope.launch {
            migrate(uid)
        }
    }

    fun signOut() {
        googleSignInClientFactory.create().signOut()
        authManager.signOut()
    }

    private suspend fun migrate(uid: String) {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null, isMigrationComplete = false)
        val migrationResult = authMigrationCoordinator.migrateIfNeeded(uid)
        _state.value = migrationResult.fold(
            onSuccess = { result ->
                when (result) {
                    MigrationBootstrapResult.READY -> _state.value.copy(
                        isLoading = false,
                        isMigrationComplete = true,
                        awaitingBackupImport = false,
                        infoMessage = null,
                        errorMessage = null
                    )

                    MigrationBootstrapResult.NEEDS_BACKUP_IMPORT -> _state.value.copy(
                        isLoading = false,
                        isMigrationComplete = false,
                        awaitingBackupImport = true,
                        infoMessage = "Import a backup file if you have one, or continue without importing.",
                        errorMessage = null
                    )
                }
            },
            onFailure = {
                _state.value.copy(
                    isLoading = false,
                    isMigrationComplete = false,
                    awaitingBackupImport = false,
                    infoMessage = null,
                    errorMessage = it.message
                )
            }
        )
    }
}
