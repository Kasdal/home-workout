package com.example.workoutapp.data.storage

import com.example.workoutapp.auth.AuthManager
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoUploader @Inject constructor(
    private val storage: FirebaseStorage,
    private val authManager: AuthManager
) {

    private fun photoRef(uid: String, exerciseId: Int) =
        storage.reference
            .child("users/$uid/exercises/$exerciseId/photo.jpg")

    suspend fun uploadExercisePhoto(exerciseId: Int, bytes: ByteArray): String {
        val uid = authManager.currentUserId()
            ?: throw IllegalStateException("User is not signed in")
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        val ref = photoRef(uid, exerciseId)
        ref.putBytes(bytes, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteExercisePhoto(exerciseId: Int) {
        val uid = authManager.currentUserId() ?: return
        val ref = photoRef(uid, exerciseId)
        try {
            ref.delete().await()
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete photo for exercise %d", exerciseId)
        }
    }
}
