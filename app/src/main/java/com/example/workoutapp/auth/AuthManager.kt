package com.example.workoutapp.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }

        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser)

        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }.conflate()

    fun currentUserId(): String? = firebaseAuth.currentUser?.uid

    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            Unit
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
