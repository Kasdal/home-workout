package com.example.workoutapp.data.storage

import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.model.Exercise
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LegacyPhotoMigratorTest {

    private val exerciseRepository: ExerciseRepository = mockk(relaxed = true)
    private val photoProcessor: PhotoProcessor = mockk()
    private val photoUploader: PhotoUploader = mockk()
    private val sourceOpener: SourceOpener = mockk()

    private fun migrator() = LegacyPhotoMigrator(
        exerciseRepository = exerciseRepository,
        photoProcessor = photoProcessor,
        photoUploader = photoUploader,
        sourceOpener = sourceOpener
    )

    @Test
    fun `observes content uri exercises and uploads them replacing the uri on the repo`() = runTest {
        val source = "content://media/external/images/42"
        val exercise = Exercise(id = 1, name = "Bench", weight = 100f, photoUri = source)
        every { exerciseRepository.getExercises() } returns MutableStateFlow(listOf(exercise))
        every { sourceOpener.canOpen(source) } returns true
        coEvery { photoProcessor.compressToJpeg(match { it.toString() == source }) } returns byteArrayOf(1, 2, 3)
        coEvery { photoUploader.uploadExercisePhoto(1, byteArrayOf(1, 2, 3)) } returns "https://example.com/p.jpg"

        val job = Job(coroutineContext[Job])
        val scope = CoroutineScope(coroutineContext + job)
        migrator().start(scope)
        // Drain the state flow
        advanceUntilIdle()
        job.cancel()

        coVerify {
            exerciseRepository.updateExercise(
                exercise.copy(photoUri = "https://example.com/p.jpg")
            )
        }
    }

    @Test
    fun `skips exercises whose photo uri is already an https url but still migrates legacy ones in the same list`() = runTest {
        val httpsSource = "https://example.com/p.jpg"
        val legacySource = "content://media/external/images/99"
        val httpsExercise = Exercise(id = 1, name = "Bench", weight = 100f, photoUri = httpsSource)
        val legacyExercise = Exercise(id = 2, name = "Squat", weight = 140f, photoUri = legacySource)
        every { exerciseRepository.getExercises() } returns MutableStateFlow(listOf(httpsExercise, legacyExercise))
        every { sourceOpener.canOpen(legacySource) } returns true
        coEvery { photoProcessor.compressToJpeg(match { it.toString() == legacySource }) } returns byteArrayOf(4, 5, 6)
        coEvery { photoUploader.uploadExercisePhoto(2, byteArrayOf(4, 5, 6)) } returns "https://example.com/squat.jpg"

        val job = Job(coroutineContext[Job])
        val scope = CoroutineScope(coroutineContext + job)
        migrator().start(scope)
        advanceUntilIdle()
        job.cancel()

        coVerify { exerciseRepository.updateExercise(legacyExercise.copy(photoUri = "https://example.com/squat.jpg")) }
        coVerify(exactly = 0) { exerciseRepository.updateExercise(httpsExercise) }
    }

    @Test
    fun `records failure for a content uri that cannot be opened and does not retry within process`() = runTest {
        val source = "content://media/external/images/42"
        val exercise = Exercise(id = 1, name = "Bench", weight = 100f, photoUri = source)
        val flow = MutableStateFlow(listOf(exercise))
        every { exerciseRepository.getExercises() } returns flow
        every { sourceOpener.canOpen(source) } returns false

        val job = Job(coroutineContext[Job])
        val scope = CoroutineScope(coroutineContext + job)
        migrator().start(scope)
        advanceUntilIdle()

        coVerify(exactly = 0) { photoProcessor.compressToJpeg(any()) }
        coVerify(exactly = 0) { exerciseRepository.updateExercise(any()) }

        // Subsequent observations do not re-attempt the same URI
        val newSource = "content://media/external/images/43"
        every { sourceOpener.canOpen(newSource) } returns false
        flow.value = listOf(exercise.copy(photoUri = newSource))   // passes distinctUntilChanged
        advanceUntilIdle()
        coVerify(exactly = 1) { sourceOpener.canOpen(source) }    // source not retried
        coVerify(exactly = 1) { sourceOpener.canOpen(newSource) } // new URI is attempted
        coVerify(exactly = 0) { photoProcessor.compressToJpeg(any()) }
        job.cancel()
    }
}
