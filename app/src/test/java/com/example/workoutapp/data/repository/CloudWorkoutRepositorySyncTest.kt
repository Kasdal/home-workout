package com.example.workoutapp.data.repository

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.remote.FirestoreRepository
import com.example.workoutapp.data.storage.PhotoUploader
import com.example.workoutapp.data.sync.SyncState
import com.example.workoutapp.data.sync.SyncStatusMonitor
import com.example.workoutapp.model.WorkoutSession
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CloudWorkoutRepositorySyncTest {

    private val authManager = mockk<AuthManager>(relaxed = true)
    private val firestoreRepository = mockk<FirestoreRepository>(relaxed = true)
    private val photoUploader = mockk<PhotoUploader>(relaxed = true)
    private val syncStatusMonitor = SyncStatusMonitor()

    private fun repository() = CloudWorkoutRepository(
        authManager = authManager,
        firestoreRepository = firestoreRepository,
        photoUploader = photoUploader,
        syncStatusMonitor = syncStatusMonitor
    )

    @Test
    fun `successful session save updates last synced time`() = runTest {
        every { authManager.currentUserId() } returns "user-1"
        coEvery { firestoreRepository.saveSession("user-1", any()) } returns 7L

        val id = repository().saveSession(emptySession())

        assertEquals(7L, id)
        assertEquals(SyncState.IDLE, syncStatusMonitor.status.value.state)
        assertNotNull(syncStatusMonitor.status.value.lastSyncedAtMillis)
    }

    @Test
    fun `failed session save rethrows and marks error state`() = runTest {
        every { authManager.currentUserId() } returns "user-1"
        coEvery { firestoreRepository.saveSession("user-1", any()) } throws IOException("quota exceeded")

        var thrown: Throwable? = null
        try {
            repository().saveSession(emptySession())
        } catch (t: Throwable) {
            thrown = t
        }

        assertTrue(thrown is IOException)
        assertEquals(SyncState.ERROR, syncStatusMonitor.status.value.state)
        assertEquals("quota exceeded", syncStatusMonitor.status.value.lastErrorMessage)
    }

    private fun emptySession() = WorkoutSession(
        date = 0L,
        durationSeconds = 0L,
        totalWeightLifted = 0f,
        caloriesBurned = 0f
    )
}
