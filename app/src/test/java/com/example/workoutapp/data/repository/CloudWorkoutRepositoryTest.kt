package com.example.workoutapp.data.repository

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.remote.FirestoreRepository
import com.example.workoutapp.data.remote.model.CloudCategory
import com.example.workoutapp.data.remote.model.toCloud
import com.example.workoutapp.data.settings.WorkoutSessionSettings
import com.example.workoutapp.data.storage.PhotoUploader
import com.example.workoutapp.model.Category
import com.example.workoutapp.model.Exercise
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CloudWorkoutRepositoryTest {

    private lateinit var authManager: AuthManager
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var photoUploader: PhotoUploader
    private lateinit var repository: CloudWorkoutRepository

    @Before
    fun setup() {
        authManager = mockk(relaxed = true)
        firestoreRepository = mockk(relaxed = true)
        photoUploader = mockk(relaxed = true)
        repository = CloudWorkoutRepository(authManager, firestoreRepository, photoUploader)
    }

    @Test
    fun `saveSyncedWorkoutSettings writes partial synced settings directly to firestore`() = runTest {
        val settings = WorkoutSessionSettings(
            restTimerDuration = 45,
            exerciseSwitchDuration = 120,
            undoLastSetEnabled = false
        )
        every { authManager.currentUserId() } returns "user-123"
        coEvery { firestoreRepository.saveSyncedWorkoutSettings("user-123", settings) } returns Unit

        repository.saveSyncedWorkoutSettings(settings)

        coVerify { firestoreRepository.saveSyncedWorkoutSettings("user-123", settings) }
    }

    @Test
    fun `observeSyncedWorkoutSettings delegates to focused firestore settings stream`() = runTest {
        val settings = WorkoutSessionSettings(
            restTimerDuration = 45,
            exerciseSwitchDuration = 120,
            undoLastSetEnabled = false
        )
        every { firestoreRepository.observeSyncedWorkoutSettings("user-123") } returns flowOf(settings)
        every { authManager.currentUser } returns flowOf(mockk {
            every { uid } returns "user-123"
        })

        assertEquals(settings, repository.observeSyncedWorkoutSettings().first())
    }

    @Test
    fun `deleteExercise cascades remote photo delete when exercise has https photoUri`() = runTest {
        val exercise = Exercise(
            id = 42,
            name = "Bench Press",
            weight = 50f,
            photoUri = "https://firebasestorage.googleapis.com/path/photo.jpg"
        )
        every { authManager.currentUserId() } returns "user-123"
        every { authManager.currentUser } returns flowOf(mockk {
            every { uid } returns "user-123"
        })
        every { firestoreRepository.observeExercises("user-123") } returns flowOf(listOf(exercise))
        coEvery { photoUploader.deleteExercisePhoto(42) } returns Unit
        coEvery { firestoreRepository.markExerciseDeleted("user-123", 42) } returns Unit

        repository.deleteExercise(42)

        coVerify { photoUploader.deleteExercisePhoto(42) }
        coVerify { firestoreRepository.markExerciseDeleted("user-123", 42) }
    }

    @Test
    fun `deleteExercise attempts photo delete for any exercise`() = runTest {
        val exercise = Exercise(id = 42, name = "Bench Press", weight = 50f, photoUri = null)
        every { authManager.currentUserId() } returns "user-123"
        every { authManager.currentUser } returns flowOf(mockk {
            every { uid } returns "user-123"
        })
        every { firestoreRepository.observeExercises("user-123") } returns flowOf(listOf(exercise))
        coEvery { photoUploader.deleteExercisePhoto(42) } returns Unit
        coEvery { firestoreRepository.markExerciseDeleted("user-123", 42) } returns Unit

        repository.deleteExercise(42)

        coVerify { photoUploader.deleteExercisePhoto(42) }
        coVerify { firestoreRepository.markExerciseDeleted("user-123", 42) }
    }

    @Test
    fun `deleteExercise still deletes firestore doc when photo delete fails`() = runTest {
        val exercise = Exercise(
            id = 42,
            name = "Bench Press",
            weight = 50f,
            photoUri = "https://firebasestorage.googleapis.com/path/photo.jpg"
        )
        every { authManager.currentUserId() } returns "user-123"
        every { authManager.currentUser } returns flowOf(mockk {
            every { uid } returns "user-123"
        })
        every { firestoreRepository.observeExercises("user-123") } returns flowOf(listOf(exercise))
        coEvery { photoUploader.deleteExercisePhoto(42) } throws RuntimeException("storage offline")
        coEvery { firestoreRepository.markExerciseDeleted("user-123", 42) } returns Unit

        repository.deleteExercise(42)

        coVerify { firestoreRepository.markExerciseDeleted("user-123", 42) }
    }

    @Test
    fun `observeActiveCategories returns mapped domain categories`() = runTest {
        val clouds = listOf(
            CloudCategory(id = "push", name = "Push", iconName = "FitnessCenter", sortOrder = 0),
            CloudCategory(id = "legacy", name = "Legacy", iconName = "History", sortOrder = 999, isLegacy = true)
        )
        every { authManager.currentUser } returns flowOf(mockk {
            every { uid } returns "user-123"
        })
        every { firestoreRepository.observeActiveCategories("user-123") } returns flowOf(clouds)

        val result = repository.observeActiveCategories().first()

        assertEquals(
            listOf(
                Category("push", "Push", "FitnessCenter", sortOrder = 0, isLegacy = false),
                Category("legacy", "Legacy", "History", sortOrder = 999, isLegacy = true)
            ),
            result
        )
    }

    @Test
    fun `upsertCategory translates domain to cloud and calls Firestore`() = runTest {
        val domain = Category("core", "Core", "SelfImprovement", sortOrder = 3)
        every { authManager.currentUserId() } returns "user-123"
        coEvery { firestoreRepository.upsertCategory("user-123", domain.toCloud()) } returns Unit

        repository.upsertCategory(domain)

        coVerify { firestoreRepository.upsertCategory("user-123", domain.toCloud()) }
    }

    @Test
    fun `deleteAndReassign forbids the legacy category`() = runTest {
        val error = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.deleteAndReassign(categoryId = Category.LEGACY_ID, reassignToCategoryId = "push")
            }
        }
        assertEquals("Cannot delete the protected legacy category", error.message)
    }

    @Test
    fun `deleteAndReassign marks category deleted and reassigns exercises`() = runTest {
        every { authManager.currentUserId() } returns "user-123"
        coEvery { firestoreRepository.markExercisesWithCategory("user-123", "arms", "push") } returns Unit
        coEvery { firestoreRepository.markCategoryDeleted("user-123", "arms") } returns Unit

        repository.deleteAndReassign("arms", "push")

        coVerifyOrder {
            firestoreRepository.markExercisesWithCategory("user-123", "arms", "push")
            firestoreRepository.markCategoryDeleted("user-123", "arms")
        }
    }
}
