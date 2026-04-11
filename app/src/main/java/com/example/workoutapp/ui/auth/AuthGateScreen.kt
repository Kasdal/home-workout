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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthGateScreen(
    onReady: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken != null) {
                viewModel.signInWithGoogleIdToken(idToken)
            } else {
                Log.e("AuthGate", "Google sign-in returned null ID token")
                viewModel.onSignInError("Google sign-in did not return an ID token.")
            }
        } catch (e: Exception) {
            if (e is ApiException) {
                Log.e("AuthGate", "Google sign-in ApiException code=${e.statusCode}", e)
            } else {
                Log.e("AuthGate", "Google sign-in failed", e)
            }
            viewModel.onSignInError(e.message ?: "Google sign-in failed")
        }
    }

    LaunchedEffect(state.isSignedIn, state.isMigrationComplete) {
        if (state.isSignedIn && state.isMigrationComplete) {
            onReady()
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
                    color = Color.Red,
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
                        signInLauncher.launch(viewModel.buildGoogleSignInIntent())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try Google Sign-in Again")
                }
            } else if (!state.isSignedIn) {
                Button(
                    onClick = {
                        signInLauncher.launch(viewModel.buildGoogleSignInIntent())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google")
                }
            } else if (state.errorMessage != null) {
                val errorMessage = state.errorMessage ?: "Unknown error"
                Text(
                    text = "Migration failed. Your local data is still safe on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red,
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
                Button(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign out")
                }
            }
        }
    }
}
