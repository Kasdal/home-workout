package com.example.workoutapp.ui.auth

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutapp.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

/** Credential type identifier for a Google ID token (see GoogleIdTokenCredential). */
private const val GOOGLE_ID_TOKEN_CREDENTIAL_TYPE =
    "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN"

@Composable
fun AuthGateScreen(
    startupFailed: Boolean = false,
    onRetryStartup: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val serverClientId = stringResource(R.string.default_web_client_id)

    fun launchGoogleSignIn() {
        scope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val response = credentialManager.getCredential(context, request)
                val credential = response.credential
                if (credential is CustomCredential &&
                    credential.type == GOOGLE_ID_TOKEN_CREDENTIAL_TYPE
                ) {                    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    viewModel.signInWithGoogleIdToken(idToken)
                } else {
                    Log.e("AuthGate", "Credential Manager returned unexpected credential type")
                    viewModel.onSignInError("Google sign-in returned an unexpected credential type.")
                }
            } catch (_: GetCredentialCancellationException) {
                // The user dismissed the one-tap dialog; treat as a no-op.
            } catch (e: GetCredentialException) {
                Log.e("AuthGate", "Credential Manager sign-in failed type=${e.type}", e)
                viewModel.onSignInError("Google sign-in failed. Please try again.")
            } catch (e: Exception) {
                Log.e("AuthGate", "Google sign-in failed", e)
                viewModel.onSignInError(e.message ?: "Google sign-in failed")
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Unable to read backup file")
            }.onSuccess {
                viewModel.importLegacyBackup(it)
            }.onFailure {
                viewModel.onSignInError(it.message ?: "Failed to read backup file")
            }
        }
    }

    val signInFailed = !state.isSignedIn && state.errorMessage != null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Cloud Sync",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Sign in with Google to sync your workout data across devices.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (state.isSignedIn) "Migrating local data to cloud..." else "Signing in...",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            } else if (signInFailed) {
                val errorMessage = state.errorMessage ?: "Google sign-in failed"
                Text(
                    text = "Google sign-in failed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        launchGoogleSignIn()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try Google Sign-in Again")
                }
            } else if (!state.isSignedIn) {
                Button(
                    onClick = {
                        launchGoogleSignIn()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google")
                }
            } else if (state.awaitingBackupImport) {
                Text(
                    text = state.infoMessage ?: "Import a backup file if you have one, or continue without importing.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.importConflict) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This account already has workout data in the cloud. The import was stopped before changing anything. Continue without importing to use the existing cloud data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { importBackupLauncher.launch(arrayOf("application/json", "text/plain")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Backup File")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.continueWithoutImport() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue Without Import")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign out")
                }
            } else if (startupFailed) {
                Text(
                    text = "Can't reach your cloud data right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Check your connection and try again. If you have used this app on this device before, trying again will also open your saved data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.retryMigration()
                        onRetryStartup()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try Again")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign out")
                }
            } else if (state.errorMessage != null) {
                val errorMessage = state.errorMessage ?: "Unknown error"
                Text(
                    text = "Migration failed. Your local data is still safe on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.retryMigration() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Retry Migration")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { importBackupLauncher.launch(arrayOf("application/json", "text/plain")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Backup File")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign out")
                }
            } else if (state.infoMessage != null) {
                Text(
                    text = state.infoMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
