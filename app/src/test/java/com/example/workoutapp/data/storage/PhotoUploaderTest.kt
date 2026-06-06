package com.example.workoutapp.data.storage

import android.net.Uri
import com.example.workoutapp.auth.AuthManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class PhotoUploaderTest {

    private val authManager: AuthManager = mockk()
    private val storage: FirebaseStorage = mockk()
    private val userRoot: StorageReference = mockk()
    private val photoRef: StorageReference = mockk(relaxed = true)
    private val pathSlot = slot<String>()
    private val uploader = PhotoUploader(storage, authManager)

    private fun wirePathRefs() {
        every { storage.reference } returns userRoot
        every { userRoot.child(capture(pathSlot)) } returns photoRef
    }

    @Test
    fun `uploadExercisePhoto writes bytes with image jpeg metadata at the expected path`() = runTest {
        wirePathRefs()
        every { authManager.currentUserId() } returns "user-123"
        val uploadTask: UploadTask = mockk()
        every { uploadTask.isComplete } returns true
        every { uploadTask.isSuccessful } returns true
        every { uploadTask.exception } returns null
        every { uploadTask.isCanceled } returns false
        every { uploadTask.result } returns mockk<UploadTask.TaskSnapshot>(relaxed = true)
        every { photoRef.putBytes(any(), any()) } returns uploadTask
        val downloadUri: Uri = mockk(relaxed = true)
        every { downloadUri.toString() } returns "https://example.com/p.jpg"
        every { photoRef.downloadUrl } returns Tasks.forResult(downloadUri)

        val url = uploader.uploadExercisePhoto(exerciseId = 42, bytes = byteArrayOf(1, 2, 3))

        assertEquals("https://example.com/p.jpg", url)
        assertEquals("users/user-123/exercises/42/photo.jpg", pathSlot.captured)
        val metadataSlot = slot<StorageMetadata>()
        verify { photoRef.putBytes(byteArrayOf(1, 2, 3), capture(metadataSlot)) }
        assertEquals("image/jpeg", metadataSlot.captured.contentType)
    }

    @Test
    fun `uploadExercisePhoto throws when no user is signed in`() = runTest {
        every { authManager.currentUserId() } returns null

        try {
            uploader.uploadExercisePhoto(exerciseId = 1, bytes = byteArrayOf())
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // expected
        }
    }

    @Test
    fun `uploadExercisePhoto rethrows storage network failure`() = runTest {
        wirePathRefs()
        every { authManager.currentUserId() } returns "user-123"
        val uploadTask: UploadTask = mockk()
        every { uploadTask.isComplete } returns true
        every { uploadTask.isSuccessful } returns false
        every { uploadTask.exception } returns IOException("offline")
        every { photoRef.putBytes(any(), any()) } returns uploadTask

        try {
            uploader.uploadExercisePhoto(exerciseId = 42, bytes = byteArrayOf(1))
            fail("Expected IOException")
        } catch (e: IOException) {
            // expected
        }
        assertEquals("users/user-123/exercises/42/photo.jpg", pathSlot.captured)
    }

    @Test
    fun `deleteExercisePhoto swallows storage failures and does not rethrow`() = runTest {
        wirePathRefs()
        every { authManager.currentUserId() } returns "user-123"
        every { photoRef.delete() } returns Tasks.forException(IOException("offline"))

        // Should not throw
        uploader.deleteExercisePhoto(exerciseId = 42)
        assertEquals("users/user-123/exercises/42/photo.jpg", pathSlot.captured)
    }

    @Test
    fun `deleteExercisePhoto is a no-op when no user is signed in`() = runTest {
        every { authManager.currentUserId() } returns null

        // Should not throw
        uploader.deleteExercisePhoto(exerciseId = 42)
    }
}
