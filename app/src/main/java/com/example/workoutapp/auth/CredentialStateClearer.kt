package com.example.workoutapp.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Clears the Credential Manager state after sign-out. */
interface CredentialStateClearer {
    suspend fun clear()
}

class DefaultCredentialStateClearer @Inject constructor(
    @ApplicationContext private val context: Context
) : CredentialStateClearer {
    override suspend fun clear() {
        CredentialManager.create(context)
            .clearCredentialState(ClearCredentialStateRequest())
    }
}
