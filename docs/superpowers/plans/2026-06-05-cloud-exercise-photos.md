# Cloud-Backed Exercise Photos Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move exercise photos from device-local `content://` URIs to Firebase Cloud Storage download URLs, so photos survive reinstalls and show on every device signed into the same account.

**Architecture:** A new `data/storage/` package with `PhotoProcessor` (Bitmap decode → 1080px JPEG re-encode) and `PhotoUploader` (Firebase Storage `putBytes` + `downloadUrl`). The existing `Exercise.photoUri: String?` field stays untouched; only the *shape* of the string it holds changes from `content://…` to `https://…`. `WorkoutViewModel.updateExercisePhoto` is rewritten to drive the pipeline and emit a `SharedFlow<PhotoUploadResult>` that the UI surfaces as snackbars. A `LegacyPhotoMigrator` re-uploads existing `content://` URIs lazily on first observation.

**Tech Stack:** Kotlin, Hilt, Coroutines, Coil (no change), Firebase Cloud Storage (`firebase-storage` already on classpath), kotlinx-coroutines-play-services (for `Task<T>.await()`), JUnit4 + MockK + Turbine for tests.

---

## File Structure

| File | Responsibility |
|---|---|
| `app/src/main/java/com/example/workoutapp/data/storage/SourceUnreadableException.kt` | New. Custom `IOException` thrown when the source URI cannot be opened. |
| `app/src/main/java/com/example/workoutapp/data/storage/PhotoProcessor.kt` | New. Decodes a `Uri` to a 1080px JPEG `ByteArray` on `Dispatchers.IO`. |
| `app/src/main/java/com/example/workoutapp/data/storage/PhotoUploader.kt` | New. Uploads bytes to `users/{uid}/exercises/{id}/photo.jpg`, returns download URL. Provides best-effort `deleteExercisePhoto`. |
| `app/src/main/java/com/example/workoutapp/data/storage/PhotoUploadResult.kt` | New. Sealed result type emitted from the ViewModel. |
| `app/src/main/java/com/example/workoutapp/data/storage/LegacyPhotoMigrator.kt` | New. Observes exercise list, re-uploads `content://` URIs that resolve locally. |
| `app/src/main/java/com/example/workoutapp/di/AppModule.kt` | Modify. Add `FirebaseStorage` provider. |
| `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt` | Modify. Inject processor + uploader + migrator. Rewrite `updateExercisePhoto(exerciseId, sourceUri)`. Add `removeExercisePhoto(exerciseId)`. Expose `photoUploadEvents: SharedFlow<PhotoUploadResult>`. |
| `app/src/main/java/com/example/workoutapp/ui/workouts/WorkoutsScreen.kt` | Modify. Change picker callback signature to `(Int, Uri) -> Unit`. Add `SnackbarHostState`, collect `photoUploadEvents`. |
| `app/src/main/java/com/example/workoutapp/ui/workout/ExerciseCard.kt` | Modify. Change "Remove Photo" path to use the new `removeExercisePhoto` callback (passed from the screen). |
| `app/src/main/java/com/example/workoutapp/data/repository/CloudWorkoutRepository.kt` | Modify. `deleteExercise` now also calls `photoUploader.deleteExercisePhoto(id)`. Inject `PhotoUploader`. |
| `app/src/main/java/com/example/workoutapp/MainViewModel.kt` | Modify. Own the `LegacyPhotoMigrator` and start its observation. |
| `app/src/main/res/values/strings.xml` | Modify. Add snackbar strings. |
| `storage.rules` | New at repo root. Security rules. |
| `app/src/test/java/com/example/workoutapp/data/storage/PhotoProcessorTest.kt` | New. |
| `app/src/test/java/com/example/workoutapp/data/storage/PhotoUploaderTest.kt` | New. |
| `app/src/test/java/com/example/workoutapp/data/storage/LegacyPhotoMigratorTest.kt` | New. |
| `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt` | Modify. Add photo upload and remove cases. |

---

## Task 1: Add `FirebaseStorage` Hilt provider

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/di/AppModule.kt:39`

- [ ] **Step 1: Add the import and provider**

Open `app/src/main/java/com/example/workoutapp/di/AppModule.kt` and add the `FirebaseStorage` import next to the existing `FirebaseFirestore` import (around line 5). Then add a new provider method after `provideFirebaseFirestore()` (after line 41):

```kotlin
import com.google.firebase.storage.FirebaseStorage

@Provides
@Singleton
fun provideFirebaseStorage(): FirebaseStorage {
    return FirebaseStorage.getInstance()
}
```

- [ ] **Step 2: Verify the file compiles**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/di/AppModule.kt
git commit -m "feat(storage): add FirebaseStorage Hilt provider"
```

---

## Task 2: Create `SourceUnreadableException`

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/data/storage/SourceUnreadableException.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.example.workoutapp.data.storage

import java.io.IOException

class SourceUnreadableException(
    message: String = "Photo source could not be opened."
) : IOException(message)
```

- [ ] **Step 2: Verify the file compiles**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/data/storage/SourceUnreadableException.kt
git commit -m "feat(storage): add SourceUnreadableException"
```

---

## Task 3: Create `PhotoProcessor` (with TDD)

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/data/storage/PhotoProcessor.kt`
- Create: `app/src/test/java/com/example/workoutapp/data/storage/PhotoProcessorTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/workoutapp/data/storage/PhotoProcessorTest.kt`:

```kotlin
package com.example.workoutapp.data.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream

class PhotoProcessorTest {

    private val context: Context = mockk(relaxed = true)
    private val resolver: ContentResolver = mockk(relaxed = true)
    private val processor = PhotoProcessor(context)

    @org.junit.Before
    fun setup() {
        every { context.contentResolver } returns resolver
    }

    @Test
    fun `compressToJpeg returns bytes that decode back as a JPEG image`() = runTest {
        val source = Uri.parse("content://media/external/images/42")
        val original = makeTestJpeg(width = 4000, height = 3000)
        every { resolver.openInputStream(source) } returns ByteArrayInputStream(original)

        val bytes = processor.compressToJpeg(source, maxEdgePx = 1080, quality = 85)

        assertNotNull(bytes)
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull("Result should be a valid JPEG", decoded)
        assertTrue(
            "Longest edge must be <= maxEdgePx but was ${maxOf(decoded!!.width, decoded.height)}",
            maxOf(decoded.width, decoded.height) <= 1080
        )
    }

    @Test
    fun `compressToJpeg shrinks a 12 megapixel input to a small payload`() = runTest {
        val source = Uri.parse("content://media/external/images/42")
        val original = makeTestJpeg(width = 4000, height = 3000)
        every { resolver.openInputStream(source) } returns ByteArrayInputStream(original)

        val bytes = processor.compressToJpeg(source, maxEdgePx = 1080, quality = 85)

        assertTrue(
            "Expected 1080p JPEG re-encode to be well under 1 MB, was ${bytes.size} bytes",
            bytes.size < 1_000_000
        )
    }

    @Test
    fun `compressToJpeg throws SourceUnreadableException when ContentResolver returns null stream`() = runTest {
        val source = Uri.parse("content://media/external/images/missing")
        every { resolver.openInputStream(source) } returns null

        try {
            processor.compressToJpeg(source)
            fail("Expected SourceUnreadableException")
        } catch (e: SourceUnreadableException) {
            // expected
        }
    }

    @Test
    fun `compressToJpeg throws SourceUnreadableException when ContentResolver throws FileNotFoundException`() = runTest {
        val source = Uri.parse("content://media/external/images/missing")
        every { resolver.openInputStream(source) } throws FileNotFoundException("gone")

        try {
            processor.compressToJpeg(source)
            fail("Expected SourceUnreadableException")
        } catch (e: SourceUnreadableException) {
            // expected
        }
    }

    private fun makeTestJpeg(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        bitmap.recycle()
        return out.toByteArray()
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.example.workoutapp.data.storage.PhotoProcessorTest"`
Expected: compilation failure or test failure — `PhotoProcessor` does not exist yet.

- [ ] **Step 3: Implement `PhotoProcessor`**

Create `app/src/main/java/com/example/workoutapp/data/storage/PhotoProcessor.kt`:

```kotlin
package com.example.workoutapp.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class PhotoProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun compressToJpeg(
        source: Uri,
        maxEdgePx: Int = 1080,
        quality: Int = 85
    ): ByteArray = withContext(Dispatchers.IO) {
        val stream = try {
            context.contentResolver.openInputStream(source)
                ?: throw SourceUnreadableException("ContentResolver returned null for $source")
        } catch (e: SourceUnreadableException) {
            throw e
        } catch (e: IOException) {
            throw SourceUnreadableException("Could not open $source: ${e.message}").initCauseCompat(e)
        }

        stream.use { input ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, bounds)

            val (rawW, rawH) = bounds.outWidth to bounds.outHeight
            require(rawW > 0 && rawH > 0) { "Image bounds invalid: ${rawW}x$rawH" }

            val inSampleSize = computeInSampleSize(rawW, rawH, maxEdgePx)
            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(source)
                    ?: throw SourceUnreadableException("ContentResolver returned null on re-open of $source"),
                null,
                decodeOpts
            ) ?: throw SourceUnreadableException("BitmapFactory returned null for $source")

            val scaled = scaleDownIfNeeded(decoded, maxEdgePx)
            val out = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            scaled.recycle()
            if (scaled !== decoded) decoded.recycle()
            out.toByteArray()
        }
    }

    private fun computeInSampleSize(width: Int, height: Int, maxEdgePx: Int): Int {
        var sample = 1
        val longest = max(width, height)
        while (longest / (sample * 2) >= maxEdgePx) {
            sample *= 2
        }
        return sample
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxEdgePx: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdgePx) return bitmap
        val ratio = maxEdgePx.toFloat() / longest.toFloat()
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun <E : Throwable> E.initCauseCompat(cause: Throwable): E {
        return try {
            initCause(cause); this
        } catch (_: IllegalStateException) {
            this
        } catch (_: IllegalArgumentException) {
            this
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.example.workoutapp.data.storage.PhotoProcessorTest"`
Expected: all 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/data/storage/PhotoProcessor.kt \
        app/src/test/java/com/example/workoutapp/data/storage/PhotoProcessorTest.kt
git commit -m "feat(storage): add PhotoProcessor with JPEG resize pipeline"
```

---

## Task 4: Create `PhotoUploadResult` sealed type

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/data/storage/PhotoUploadResult.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.example.workoutapp.data.storage

sealed interface PhotoUploadResult {
    data class Success(val photoUri: String) : PhotoUploadResult
    data object SourceUnreadable : PhotoUploadResult
    data class UploadFailed(val cause: Throwable) : PhotoUploadResult
}
```

- [ ] **Step 2: Verify the file compiles**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/data/storage/PhotoUploadResult.kt
git commit -m "feat(storage): add PhotoUploadResult sealed type"
```

---

## Task 5: Create `PhotoUploader` (with TDD)

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/data/storage/PhotoUploader.kt`
- Create: `app/src/test/java/com/example/workoutapp/data/storage/PhotoUploaderTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/workoutapp/data/storage/PhotoUploaderTest.kt`:

```kotlin
package com.example.workoutapp.data.storage

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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.URL

class PhotoUploaderTest {

    private val authManager: AuthManager = mockk()
    private val storage: FirebaseStorage = mockk()
    private val exercisesRef: StorageReference = mockk(relaxed = true)
    private val photoRef: StorageReference = mockk(relaxed = true)
    private val uploadTask: UploadTask = mockk()
    private val uploader = PhotoUploader(storage, authManager)

    private fun wirePathRefs() {
        val userRoot = mockk<StorageReference>(relaxed = true)
        val userFolder = mockk<StorageReference>(relaxed = true)
        val exerciseFolder = mockk<StorageReference>(relaxed = true)
        every { storage.reference } returns userRoot
        every { userRoot.child("users/user-123") } returns userFolder
        every { userFolder.child("exercises/42") } returns exerciseFolder
        every { exerciseFolder.child("photo.jpg") } returns photoRef
    }

    @Test
    fun `uploadExercisePhoto writes bytes with image jpeg metadata at the expected path`() = runTest {
        wirePathRefs()
        every { authManager.currentUserId() } returns "user-123"
        every { photoRef.putBytes(any(), any()) } returns uploadTask
        every { uploadTask.isSuccessful } returns true
        every { uploadTask.result } returns mockk()
        every { photoRef.downloadUrl } returns Tasks.forResult(URL("https://example.com/p.jpg"))

        val url = uploader.uploadExercisePhoto(exerciseId = 42, bytes = byteArrayOf(1, 2, 3))

        assertEquals("https://example.com/p.jpg", url)
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
        every { photoRef.putBytes(any(), any()) } returns uploadTask
        every { uploadTask.isSuccessful } returns false
        every { uploadTask.exception } returns IOException("offline")

        try {
            uploader.uploadExercisePhoto(exerciseId = 42, bytes = byteArrayOf(1))
            fail("Expected IOException")
        } catch (e: IOException) {
            // expected
        }
    }

    @Test
    fun `deleteExercisePhoto swallows storage failures and does not rethrow`() = runTest {
        wirePathRefs()
        every { authManager.currentUserId() } returns "user-123"
        every { photoRef.delete() } returns Tasks.forException(IOException("offline"))

        // Should not throw
        uploader.deleteExercisePhoto(exerciseId = 42)
    }

    @Test
    fun `deleteExercisePhoto is a no-op when no user is signed in`() = runTest {
        every { authManager.currentUserId() } returns null

        // Should not throw
        uploader.deleteExercisePhoto(exerciseId = 42)
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.example.workoutapp.data.storage.PhotoUploaderTest"`
Expected: compilation failure — `PhotoUploader` does not exist yet.

- [ ] **Step 3: Implement `PhotoUploader`**

Create `app/src/main/java/com/example/workoutapp/data/storage/PhotoUploader.kt`:

```kotlin
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
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.example.workoutapp.data.storage.PhotoUploaderTest"`
Expected: all 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/data/storage/PhotoUploader.kt \
        app/src/test/java/com/example/workoutapp/data/storage/PhotoUploaderTest.kt
git commit -m "feat(storage): add PhotoUploader for Firebase Storage"
```

---

## Task 6: Create `LegacyPhotoMigrator` (with TDD)

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/data/storage/LegacyPhotoMigrator.kt`
- Create: `app/src/test/java/com/example/workoutapp/data/storage/LegacyPhotoMigratorTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/workoutapp/data/storage/LegacyPhotoMigratorTest.kt`:

```kotlin
package com.example.workoutapp.data.storage

import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.model.Exercise
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

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

        migrator().start(this)
        // Drain the state flow
        kotlinx.coroutines.test.advanceUntilIdle()

        coVerify {
            exerciseRepository.updateExercise(
                exercise.copy(photoUri = "https://example.com/p.jpg")
            )
        }
    }

    @Test
    fun `skips exercises whose photo uri is already an https url`() = runTest {
        val exercise = Exercise(id = 1, name = "Bench", weight = 100f, photoUri = "https://example.com/p.jpg")
        every { exerciseRepository.getExercises() } returns MutableStateFlow(listOf(exercise))

        migrator().start(this)
        kotlinx.coroutines.test.advanceUntilIdle()

        coVerify(exactly = 0) { photoProcessor.compressToJpeg(any()) }
    }

    @Test
    fun `records failure for a content uri that cannot be opened and does not retry within process`() = runTest {
        val source = "content://media/external/images/42"
        val exercise = Exercise(id = 1, name = "Bench", weight = 100f, photoUri = source)
        val flow = MutableStateFlow(listOf(exercise))
        every { exerciseRepository.getExercises() } returns flow
        every { sourceOpener.canOpen(source) } returns false

        migrator().start(this)
        kotlinx.coroutines.test.advanceUntilIdle()

        coVerify(exactly = 0) { photoProcessor.compressToJpeg(any()) }
        coVerify(exactly = 0) { exerciseRepository.updateExercise(any()) }

        // Subsequent observations do not re-attempt
        flow.value = listOf(exercise.copy(photoUri = source))
        kotlinx.coroutines.test.advanceUntilIdle()
        coVerify(exactly = 0) { photoProcessor.compressToJpeg(any()) }
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.example.workoutapp.data.storage.LegacyPhotoMigratorTest"`
Expected: compilation failure — `LegacyPhotoMigrator` and `SourceOpener` do not exist yet.

- [ ] **Step 3: Implement `LegacyPhotoMigrator` and `SourceOpener`**

Create `app/src/main/java/com/example/workoutapp/data/storage/SourceOpener.kt`:

```kotlin
package com.example.workoutapp.data.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

fun interface SourceOpener {
    fun canOpen(uriString: String): Boolean
}

@Singleton
class AndroidSourceOpener @Inject constructor(
    @ApplicationContext private val context: Context
) : SourceOpener {
    override fun canOpen(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }
}
```

Create `app/src/main/java/com/example/workoutapp/data/storage/LegacyPhotoMigrator.kt`:

```kotlin
package com.example.workoutapp.data.storage

import android.net.Uri
import com.example.workoutapp.data.repository.ExerciseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyPhotoMigrator @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val photoProcessor: PhotoProcessor,
    private val photoUploader: PhotoUploader,
    private val sourceOpener: SourceOpener
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            exerciseRepository.getExercises()
                .map { exercises -> exercises.mapNotNull { it.photoUri?.takeIf(::isLegacy) } }
                .distinctUntilChanged()
                .collect { legacyUris -> migrateAll(legacyUris) }
        }
    }

    private fun isLegacy(uri: String): Boolean =
        uri.startsWith("content://") || uri.startsWith("file://")

    private suspend fun migrateAll(uris: List<String>) {
        uris.forEach { uriString ->
            if (attempted.add(uriString)) {
                migrateOne(uriString)
            }
        }
    }

    private suspend fun migrateOne(uriString: String) {
        if (!sourceOpener.canOpen(uriString)) {
            Timber.w("Legacy photo uri unresolvable, leaving as-is: %s", uriString)
            return
        }
        val current = exerciseRepository.getExercises().first()
            .firstOrNull { it.photoUri == uriString } ?: return
        try {
            val bytes = photoProcessor.compressToJpeg(Uri.parse(uriString))
            val url = photoUploader.uploadExercisePhoto(current.id, bytes)
            exerciseRepository.updateExercise(current.copy(photoUri = url))
        } catch (e: Exception) {
            Timber.w(e, "Failed to migrate legacy photo for exercise %d", current.id)
        }
    }

    private val attempted = mutableSetOf<String>()
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.example.workoutapp.data.storage.LegacyPhotoMigratorTest"`
Expected: all 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/data/storage/LegacyPhotoMigrator.kt \
        app/src/main/java/com/example/workoutapp/data/storage/SourceOpener.kt \
        app/src/test/java/com/example/workoutapp/data/storage/LegacyPhotoMigratorTest.kt
git commit -m "feat(storage): add lazy LegacyPhotoMigrator"
```

---

## Task 7: Rewrite `WorkoutViewModel.updateExercisePhoto` and add `removeExercisePhoto`

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`

- [ ] **Step 1: Add new imports and fields**

At the top of `WorkoutViewModel.kt`, add these imports alongside the existing `kotlinx.coroutines.*` imports (around line 23):

```kotlin
import android.net.Uri
import com.example.workoutapp.data.storage.PhotoProcessor
import com.example.workoutapp.data.storage.PhotoUploadResult
import com.example.workoutapp.data.storage.PhotoUploader
import com.example.workoutapp.data.storage.SourceUnreadableException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
```

- [ ] **Step 2: Add the new constructor parameters**

In the `@HiltViewModel class WorkoutViewModel @Inject constructor(...)` block (lines 32-44), add these parameters after `sensorOrchestratorFactory`:

```kotlin
    private val photoProcessor: PhotoProcessor,
    private val photoUploader: PhotoUploader
```

- [ ] **Step 3: Add the `photoUploadEvents` field**

Just below the `val exercises` declaration at line 47, add:

```kotlin
    private val _photoUploadEvents = MutableSharedFlow<PhotoUploadResult>(extraBufferCapacity = 4)
    val photoUploadEvents: SharedFlow<PhotoUploadResult> = _photoUploadEvents.asSharedFlow()
```

- [ ] **Step 4: Replace `updateExercisePhoto` and add `removeExercisePhoto`**

Replace the existing `updateExercisePhoto` at lines 356-364 with:

```kotlin
    fun updateExercisePhoto(exerciseId: Int, sourceUri: Uri) {
        viewModelScope.launch {
            val outcome: PhotoUploadResult = try {
                val bytes = photoProcessor.compressToJpeg(sourceUri)
                val url = photoUploader.uploadExercisePhoto(exerciseId, bytes)
                PhotoUploadResult.Success(url)
            } catch (e: SourceUnreadableException) {
                PhotoUploadResult.SourceUnreadable
            } catch (e: Throwable) {
                PhotoUploadResult.UploadFailed(e)
            }

            if (outcome is PhotoUploadResult.Success) {
                val current = exercises.first().firstOrNull { it.id == exerciseId } ?: return@launch
                exerciseRepository.updateExercise(current.copy(photoUri = outcome.photoUri))
            }

            _photoUploadEvents.emit(outcome)
        }
    }

    fun removeExercisePhoto(exerciseId: Int) {
        viewModelScope.launch {
            photoUploader.deleteExercisePhoto(exerciseId)
            val current = exercises.first().firstOrNull { it.id == exerciseId } ?: return@launch
            try {
                exerciseRepository.updateExercise(current.copy(photoUri = null))
            } catch (e: Throwable) {
                Timber.w(e, "Failed to clear photoUri on exercise %d", exerciseId)
            }
        }
    }
```

- [ ] **Step 5: Verify the file compiles**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If `WorkoutViewModelTest` fails to construct the VM, fix the test in the next task before proceeding.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt
git commit -m "feat(workout): rewrite updateExercisePhoto and add removeExercisePhoto"
```

---

## Task 8: Update `WorkoutViewModelTest` for the new VM signature

**Files:**
- Modify: `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`

- [ ] **Step 1: Add the new constructor arguments and the mocks**

In `setup()` around line 67, add two more `mockk(relaxed = true)` declarations alongside the existing ones (lines 70-85):

```kotlin
    private lateinit var photoProcessor: PhotoProcessor
    private lateinit var photoUploader: PhotoUploader
```

Add inside `setup()` after `sensorOrchestratorFactory = mockk()` (line 85):

```kotlin
        photoProcessor = mockk()
        photoUploader = mockk()
        coEvery { photoProcessor.compressToJpeg(any()) } returns byteArrayOf(1, 2, 3)
        coEvery { photoUploader.uploadExercisePhoto(any(), any()) } returns "https://example.com/p.jpg"
        coEvery { photoUploader.deleteExercisePhoto(any()) } returns Unit
```

Also adjust the `WorkoutViewModel(...)` constructor call in `createViewModel()` (lines 132-144) to pass the new dependencies:

```kotlin
    private fun createViewModel(): WorkoutViewModel {
        return WorkoutViewModel(
            exerciseRepository,
            sessionHistoryRepository,
            profileRepository,
            legacySettingsBootstrapper,
            localAppPreferencesRepository,
            syncedWorkoutSettingsRepository,
            soundManager,
            sessionCoordinator,
            countdownOrchestratorFactory,
            sessionClockFactory,
            sensorOrchestratorFactory,
            photoProcessor,
            photoUploader
        )
    }
```

- [ ] **Step 2: Add imports for the new types**

At the top of the test file, add alongside the existing imports:

```kotlin
import com.example.workoutapp.data.storage.PhotoProcessor
import com.example.workoutapp.data.storage.PhotoUploadResult
import com.example.workoutapp.data.storage.PhotoUploader
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.test
import app.cash.turbine.test
```

- [ ] **Step 3: Add the new test cases**

Append these tests inside `class WorkoutViewModelTest { ... }` (before the final `}` of the class):

```kotlin
    @Test
    fun `updateExercisePhoto emits Success and updates the repo with the new https uri`() = runTest {
        viewModel.updateExercisePhoto(1, android.net.Uri.parse("content://media/p/1"))
        advanceUntilIdle()

        coVerify { exerciseRepository.updateExercise(match { it.id == 1 && it.photoUri == "https://example.com/p.jpg" }) }
    }

    @Test
    fun `updateExercisePhoto emits UploadFailed and does not mutate the repo on storage error`() = runTest {
        coEvery { photoUploader.uploadExercisePhoto(any(), any()) } throws java.io.IOException("offline")

        viewModel.updateExercisePhoto(1, android.net.Uri.parse("content://media/p/1"))
        advanceUntilIdle()

        coVerify(exactly = 0) { exerciseRepository.updateExercise(any()) }
    }

    @Test
    fun `updateExercisePhoto emits SourceUnreadable and does not mutate the repo on read failure`() = runTest {
        coEvery { photoProcessor.compressToJpeg(any()) } throws com.example.workoutapp.data.storage.SourceUnreadableException()

        viewModel.updateExercisePhoto(1, android.net.Uri.parse("content://media/p/missing"))
        advanceUntilIdle()

        coVerify(exactly = 0) { exerciseRepository.updateExercise(any()) }
    }

    @Test
    fun `updateExercisePhoto emits the result on photoUploadEvents`() = runTest {
        viewModel.photoUploadEvents.test {
            viewModel.updateExercisePhoto(1, android.net.Uri.parse("content://media/p/1"))
            advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is PhotoUploadResult.Success)
            assertEquals("https://example.com/p.jpg", (event as PhotoUploadResult.Success).photoUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removeExercisePhoto calls delete and clears the photoUri on the repo`() = runTest {
        viewModel.removeExercisePhoto(1)
        advanceUntilIdle()

        coVerify { photoUploader.deleteExercisePhoto(1) }
        coVerify { exerciseRepository.updateExercise(match { it.id == 1 && it.photoUri == null }) }
    }

    @Test
    fun `removeExercisePhoto still clears the repo field when storage delete fails`() = runTest {
        coEvery { photoUploader.deleteExercisePhoto(any()) } throws java.io.IOException("offline")

        viewModel.removeExercisePhoto(1)
        advanceUntilIdle()

        coVerify { exerciseRepository.updateExercise(match { it.id == 1 && it.photoUri == null }) }
    }
```

- [ ] **Step 4: Run the test suite to verify everything passes**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.example.workoutapp.ui.workout.WorkoutViewModelTest"`
Expected: all tests (existing + new 6) PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt
git commit -m "test(workout): cover photo upload and remove paths in WorkoutViewModel"
```

---

## Task 9: Update `WorkoutsScreen` photo picker wiring and add snackbar

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/workouts/WorkoutsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add snackbar strings**

Replace `app/src/main/res/values/strings.xml` with:

```xml
<resources>
    <string name="app_name">WorkoutApp</string>
    <string name="photo_upload_source_unreadable">Couldn\'t read that photo. Try picking it again.</string>
    <string name="photo_upload_failed">Photo upload failed. Check your connection and retry.</string>
</resources>
```

- [ ] **Step 2: Update the `WorkoutsScreenContent` callback signature**

In `app/src/main/java/com/example/workoutapp/ui/workouts/WorkoutsScreen.kt`, change the `WorkoutsScreenContent` signature at lines 104-113. Replace:

```kotlin
fun WorkoutsScreenContent(
    exercises: List<Exercise>,
    onNavigateToRoute: (String) -> Unit,
    onAddExercise: (Exercise) -> Unit,
    onUpdateExercise: (Exercise) -> Unit,
    onDeleteExercise: (Int) -> Unit,
    onUpdateExercisePhoto: (Int, String) -> Unit,
    getExerciseHistory: (String) -> Flow<List<SessionExercise>>,
    onReorderExercises: (List<Exercise>) -> Unit
) {
```

with:

```kotlin
fun WorkoutsScreenContent(
    exercises: List<Exercise>,
    onNavigateToRoute: (String) -> Unit,
    onAddExercise: (Exercise) -> Unit,
    onUpdateExercise: (Exercise) -> Unit,
    onDeleteExercise: (Int) -> Unit,
    onUpdateExercisePhoto: (Int, Uri) -> Unit,
    getExerciseHistory: (String) -> Flow<List<SessionExercise>>,
    onReorderExercises: (List<Exercise>) -> Unit
) {
```

- [ ] **Step 3: Update the picker callback to pass `Uri` instead of `String`**

In the same file, replace the `photoPickerLauncher` block at lines 126-141 with:

```kotlin
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            selectedExerciseId?.let { id -> onUpdateExercisePhoto(id, it) }
        }
    }
```

- [ ] **Step 4: Add `Uri` import**

Add alongside the existing imports in the same file (near the top):

```kotlin
import android.net.Uri
```

- [ ] **Step 5: Wire the snackbar host into the Scaffold**

Pass `photoUploadEvents` through `WorkoutsScreenContent`. First, change the wrapper at the top of the file (`WorkoutsScreen`, lines 84-100) to forward the new flow:

```kotlin
fun WorkoutsScreen(
    navController: NavController,
    viewModel: com.example.workoutapp.ui.workout.WorkoutViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState(initial = emptyList())

    WorkoutsScreenContent(
        exercises = exercises,
        onNavigateToRoute = navController::navigate,
        onAddExercise = viewModel::addExercise,
        onUpdateExercise = viewModel::updateExercise,
        onDeleteExercise = viewModel::deleteExercise,
        onUpdateExercisePhoto = viewModel::updateExercisePhoto,
        getExerciseHistory = viewModel::getExerciseHistory,
        onReorderExercises = viewModel::updateExerciseOrder,
        photoUploadEvents = viewModel.photoUploadEvents
    )
}
```

Update `WorkoutsScreenContent`'s parameter list (currently lines 104-113) to accept the new flow and add the snackbar plumbing. Add a new parameter after `onReorderExercises`:

```kotlin
    photoUploadEvents: kotlinx.coroutines.flow.SharedFlow<com.example.workoutapp.data.storage.PhotoUploadResult>
```

Inside `WorkoutsScreenContent`, immediately after the existing `val context = LocalContext.current` line, add:

```kotlin
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val photoUploadFailure = stringResource(R.string.photo_upload_failed)
    val photoSourceUnreadable = stringResource(R.string.photo_upload_source_unreadable)
    val snackbarScope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(photoUploadEvents) {
        photoUploadEvents.collect { event ->
            val message = when (event) {
                com.example.workoutapp.data.storage.PhotoUploadResult.SourceUnreadable -> photoSourceUnreadable
                is com.example.workoutapp.data.storage.PhotoUploadResult.UploadFailed -> photoUploadFailure
                is com.example.workoutapp.data.storage.PhotoUploadResult.Success -> null
            }
            if (message != null) {
                snackbarScope.launch { snackbarHostState.showSnackbar(message) }
            }
        }
    }
```

Add the new imports at the top of the file alongside the existing ones:

```kotlin
import androidx.compose.ui.res.stringResource
import com.example.workoutapp.R
import kotlinx.coroutines.launch
```

Wire `snackbarHost` into the `Scaffold(...)` invocation:

```kotlin
    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = { ... },
        ...
    )
```

- [ ] **Step 6: Verify the file compiles**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/ui/workouts/WorkoutsScreen.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(ui): wire photo upload events to snackbar and pass Uri to VM"
```

---

## Task 10: Update the "Remove Photo" path in `ExerciseCard`

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/ExerciseCard.kt`

- [ ] **Step 1: Add a new `onRemovePhoto` parameter**

In `ExerciseCard.kt`, find the `ExerciseCard` composable parameter list (lines 45-63). Add a new parameter alongside `onPhotoUpload`:

```kotlin
    onRemovePhoto: (() -> Unit)? = null,
```

- [ ] **Step 2: Wire the dropdown "Remove Photo" entry to the new callback**

In the same file, find the "Remove Photo" `DropdownMenuItem` (line 443-451). Replace:

```kotlin
                                DropdownMenuItem(
                                    text = { Text("Remove Photo") },
                                    onClick = {
                                        showPhotoMenu = false
                                        onUpdate(exercise.copy(photoUri = null))
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                )
```

with:

```kotlin
                                DropdownMenuItem(
                                    text = { Text("Remove Photo") },
                                    onClick = {
                                        showPhotoMenu = false
                                        onRemovePhoto?.invoke() ?: onUpdate(exercise.copy(photoUri = null))
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                )
```

The `onUpdate` fallback preserves the existing behavior for any caller that hasn't wired the new callback.

- [ ] **Step 3: Verify the file compiles**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/ui/workout/ExerciseCard.kt
git commit -m "feat(card): route Remove Photo through optional onRemovePhoto callback"
```

---

## Task 11: Cascade-delete the Storage object from `deleteExercise`

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/data/repository/CloudWorkoutRepository.kt`

- [ ] **Step 1: Inject `PhotoUploader` into `CloudWorkoutRepository`**

Replace the `CloudWorkoutRepository` constructor at lines 22-25 with:

```kotlin
class CloudWorkoutRepository @Inject constructor(
    private val authManager: AuthManager,
    private val firestoreRepository: FirestoreRepository,
    private val photoUploader: com.example.workoutapp.data.storage.PhotoUploader
) : ProfileRepository, SessionHistoryRepository, RestDayRepository, ExerciseRepository, SettingsRepository, SyncedWorkoutSettingsStore {
```

- [ ] **Step 2: Update `deleteExercise` to remove the Storage object first**

Replace the `deleteExercise` body at lines 67-69 with:

```kotlin
    override suspend fun deleteExercise(exerciseId: Int) {
        photoUploader.deleteExercisePhoto(exerciseId)
        firestoreRepository.markExerciseDeleted(requireUid(), exerciseId)
    }
```

- [ ] **Step 3: Update `AppModule` to pass the new dependency**

In `app/src/main/java/com/example/workoutapp/di/AppModule.kt`, find `provideCloudWorkoutRepository` (lines 60-68). Replace:

```kotlin
    @Provides
    @Singleton
    fun provideCloudWorkoutRepository(
        authManager: AuthManager,
        firestoreRepository: FirestoreRepository
    ): CloudWorkoutRepository {
        return CloudWorkoutRepository(
            authManager = authManager,
            firestoreRepository = firestoreRepository
        )
    }
```

with:

```kotlin
    @Provides
    @Singleton
    fun provideCloudWorkoutRepository(
        authManager: AuthManager,
        firestoreRepository: FirestoreRepository,
        photoUploader: com.example.workoutapp.data.storage.PhotoUploader
    ): CloudWorkoutRepository {
        return CloudWorkoutRepository(
            authManager = authManager,
            firestoreRepository = firestoreRepository,
            photoUploader = photoUploader
        )
    }
```

- [ ] **Step 4: Verify the build passes**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run all unit tests**

Run: `./gradlew.bat :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass. If `CloudWorkoutRepositoryTest` fails to construct the repo, update its `setup()` to pass the new dependency (a `mockk<PhotoUploader>(relaxed = true)` is enough).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/data/repository/CloudWorkoutRepository.kt \
        app/src/main/java/com/example/workoutapp/di/AppModule.kt \
        app/src/test/java/com/example/workoutapp/data/repository/CloudWorkoutRepositoryTest.kt
git commit -m "feat(repo): cascade-delete Storage object on exercise delete"
```

---

## Task 12: Start the `LegacyPhotoMigrator` from `MainViewModel`

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/MainViewModel.kt`

- [ ] **Step 1: Read the existing `MainViewModel` to find the right place to start the migrator**

Open `app/src/main/java/com/example/workoutapp/MainViewModel.kt` and identify where its Hilt dependencies are declared and where the `init` block lives.

- [ ] **Step 2: Inject `LegacyPhotoMigrator` and call `start`**

Add the import at the top of the file:

```kotlin
import com.example.workoutapp.data.storage.LegacyPhotoMigrator
```

Add the new constructor parameter to the `@HiltViewModel` constructor block:

```kotlin
    private val legacyPhotoMigrator: LegacyPhotoMigrator
```

In the `init { ... }` block of `MainViewModel`, add the migrator start:

```kotlin
    init {
        legacyPhotoMigrator.start(viewModelScope)
    }
```

If the existing `init` block contains other logic, place the migrator start at the end of that block so it doesn't gate any critical startup work.

- [ ] **Step 3: Verify the build passes**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run all unit tests**

Run: `./gradlew.bat :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass. If `MainViewModelTest` fails to construct the VM, update its `setup()` to pass a `mockk<LegacyPhotoMigrator>(relaxed = true)` and call `every { legacyPhotoMigrator.start(any()) } returns Unit`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutapp/MainViewModel.kt \
        app/src/test/java/com/example/workoutapp/MainViewModelTest.kt
git commit -m "feat(startup): start LegacyPhotoMigrator from MainViewModel"
```

---

## Task 13: Add `storage.rules` at repo root

**Files:**
- Create: `storage.rules` (repo root)

- [ ] **Step 1: Create the rules file**

```text
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{uid}/exercises/{exerciseId}/photo.jpg {
      allow read:   if request.auth != null && request.auth.uid == uid;
      allow write:  if request.auth != null
                    && request.auth.uid == uid
                    && request.resource.size < 2 * 1024 * 1024
                    && request.resource.contentType.matches('image/.*');
    }
    match /users/{uid}/{path=**} {
      allow read, write: if false;
    }
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add storage.rules
git commit -m "feat(storage): add Firebase Storage security rules"
```

---

## Task 14: Add CI step to deploy Storage rules

**Files:**
- Modify: `.github/workflows/release.yml`

- [ ] **Step 1: Read the existing release workflow**

Open `.github/workflows/release.yml` and locate the `assembleRelease` step. Note the existing `FIREBASE_TOKEN` / `FIREBASE_PROJECT_ID` secrets (or equivalent) already in use.

- [ ] **Step 2: Add a `firebase deploy --only storage` step after `assembleRelease`**

Insert a new step that runs only on the same conditions that already gate the existing Firebase deploys in the file. Match the surrounding step style (Node version, working directory) exactly. If the existing workflow already uses a service-account key in a `FIREBASE_SERVICE_ACCOUNT` secret, prefer that and drop the `--token` flag:

```yaml
      - name: Deploy Storage rules
        if: ${{ env.DEPLOY_FIREBASE == 'true' }}
        run: npx -y firebase-tools@latest deploy --only storage --token "${{ secrets.FIREBASE_TOKEN }}"
```

The `DEPLOY_FIREBASE` env var is set by the existing Firebase deploy step in the same job; the `if` condition should mirror whatever condition that step already uses, not invent a new one. If the existing step does not gate on an env var, copy its `if:` expression verbatim.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: deploy Firebase Storage rules in release workflow"
```

---

## Task 15: Final verification

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew.bat :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: Run the debug build**

Run: `./gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: (Optional) Run instrumented tests on a device**

Run: `./gradlew.bat :app:connectedDebugAndroidTest`
Expected: BUILD SUCCESSFUL. The existing `WorkoutsScreenTest` and `WorkoutScreenTest` continue to pass.

- [ ] **Step 4: Final commit if any cleanup was needed**

```bash
git status
# If anything is unstaged:
git add -A
git commit -m "chore: cleanup after cloud photos implementation"
```
