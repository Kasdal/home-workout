package com.example.workoutapp.data.repository

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.remote.FirestoreRepository
import com.example.workoutapp.data.remote.model.CloudWorkoutTemplate
import com.example.workoutapp.data.remote.model.toCloud
import com.example.workoutapp.data.remote.model.toLocal
import com.example.workoutapp.data.storage.PhotoUploader
import com.example.workoutapp.data.sync.SyncStatusMonitor
import com.example.workoutapp.model.WorkoutTemplate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudWorkoutRepositoryTemplateTest {

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
    fun `template cloud mapper round-trips exercise ids`() {
        val template = WorkoutTemplate(id = "t1", name = "Push", exerciseIds = listOf(3, 1, 2), sortOrder = 0)

        assertEquals(template, template.toCloud().toLocal())
    }

    @Test
    fun `observeTemplates maps cloud models and keeps order`() = runTest {
        val user = mockk<com.google.firebase.auth.FirebaseUser>()
        every { user.uid } returns "user-1"
        every { authManager.currentUser } returns flowOf(user)
        every { authManager.currentUserId() } returns "user-1"
        coEvery { firestoreRepository.observeTemplates("user-1") } returns flowOf(
            listOf(
                CloudWorkoutTemplate(id = "a", name = "Pull", exerciseIds = listOf(2L, 9L)),
                CloudWorkoutTemplate(id = "b", name = "Push", exerciseIds = listOf(5L))
            )
        )

        val templates = repository().observeTemplates().first()

        assertEquals(listOf("Pull", "Push"), templates.map { it.name })
        assertEquals(listOf(2, 9), templates[0].exerciseIds)
    }

    @Test
    fun `saveTemplate forwards to firestore with mapped ids`() = runTest {
        every { authManager.currentUserId() } returns "user-1"

        repository().saveTemplate(WorkoutTemplate(id = "", name = "Legs", exerciseIds = listOf(7, 8)))

        coVerify(exactly = 1) {
            firestoreRepository.saveTemplate("user-1", CloudWorkoutTemplate(id = "", name = "Legs", exerciseIds = listOf(7L, 8L)))
        }
    }

    @Test
    fun `deleteTemplate forwards to firestore`() = runTest {
        every { authManager.currentUserId() } returns "user-1"

        repository().deleteTemplate("tpl-9")

        coVerify(exactly = 1) { firestoreRepository.deleteTemplate("user-1", "tpl-9") }
    }

    @Test
    fun `observeTemplates is empty when signed out`() = runTest {
        every { authManager.currentUser } returns flowOf(null)

        val templates = repository().observeTemplates().first()

        assertEquals(emptyList<WorkoutTemplate>(), templates)
    }
}
