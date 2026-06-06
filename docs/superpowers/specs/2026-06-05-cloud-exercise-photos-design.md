# Cloud-Backed Exercise Photos Design

Date: 2026-06-05

## Goal

Exercise photos persist across reinstalls and show up on every device signed in to the same account. Photos live in Firebase Cloud Storage, indexed by URL in the existing `Exercise.photoUri` Firestore field. Coil renders the URL exactly as it does today.

## Current State

- `model/Exercise.kt:28` has `val photoUri: String?`. `data/remote/model/CloudModels.kt:30` mirrors it to `CloudExercise`. It is already synced to Firestore via `FirestoreRepository.upsertExercise` (`FirestoreRepository.kt:166`) under `users/{uid}/exercises/{id}`.
- `ui/workouts/WorkoutsScreen.kt:126` uses the Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) and stores `uri.toString()` verbatim, including the `content://...` scheme.
- `ui/workout/WorkoutViewModel.kt:356` writes that string back through `ExerciseRepository.updateExercise`.
- `ui/workout/ExerciseCard.kt:406` and `WorkoutsScreen.kt:448` render the string through `coil.compose.AsyncImage`.
- `firebase-storage` is on the classpath (`app/build.gradle.kts:72`) but unused. There is no `FirebaseStorage` provider, no `StorageReference` consumer, and no Storage security rules file in the repo.
- `takePersistableUriPermission` is called at `WorkoutsScreen.kt:132`, but persistable permission only survives on the device that picked the URI. On a second install or a different device, the same `content://` URI is unreachable and Coil silently fails to load.

Net effect today: photos work on the device that picked them, are broken on every other device, and are lost on app data clear.

## Product Decision

Photos are uploaded to Firebase Cloud Storage at pick time, immediately compressed and resized to a 1080 px JPEG client-side. The resulting `https://` download URL is written to `Exercise.photoUri` and observed through the existing Firestore flow. Storage security rules cap each object at 2 MB and require an image MIME type, enforcing the client-side compression contract on the server.

Existing `content://` URIs in Firestore are migrated lazily. The first time an exercise with a `content://` photo is observed on a device that can still read the local file, the photo is re-uploaded to Storage and the Firestore record is patched with the new `https://` URL. URIs that can no longer be resolved locally are left as-is; those exercises show the placeholder until the user re-picks.

## Scope

In scope:

- A new `data/storage/` package with `PhotoProcessor` (image-side) and `PhotoUploader` (Storage-side).
- A Hilt provider for `FirebaseStorage`.
- Rewriting `WorkoutViewModel.updateExercisePhoto` to drive the new pipeline and emit a `PhotoUploadResult` to the UI.
- Wiring `WorkoutsScreen` and `WorkoutScreen` to surface upload failures as snackbars.
- Cascade-deleting the Storage object when an exercise is deleted and when the user removes a photo.
- Storage security rules and a deployment note.
- JVM unit tests for the new pipeline.
- Lazy migration of legacy `content://` URIs.

Out of scope:

- Heart-rate, video, or multi-photo support.
- Storage quota warnings to the user.
- Image transformations beyond resize and JPEG re-encode.
- Background offline upload queue. Uploads are explicit and require connectivity.
- Account-deletion or GDPR wipe flows.

## Architecture

```
WorkoutsScreen / WorkoutScreen  ──pick──►  PhotoPicker contract
        │                                     │
        │ source Uri + exerciseId             │ (Uri, no bytes yet)
        ▼                                     ▼
WorkoutViewModel.updateExercisePhoto  ──►  PhotoProcessor.compressToJpeg
        │                                     │
        │                                     │ ByteArray (~150-300 KB)
        │                                     ▼
        │                              PhotoUploader.uploadExercisePhoto
        │                                     │
        │                                     │ ref = users/{uid}/exercises/{id}/photo.jpg
        │                                     │ putBytes(bytes, image/jpeg)
        │                                     │
        │                              downloadUrl.toString()
        │                                     │
        │  PhotoUploadResult.Success(url)     │
        ▼                                     │
ExerciseRepository.updateExercise(photoUri = url)
        │
        ▼
Firestore users/{uid}/exercises/{id}  ◄── observed by existing Flow
        │
        ▼
ExerciseCard / WorkoutsScreen  ──coil──►  https URL renders
```

## Data Flow

### New types

```kotlin
// data/storage/PhotoProcessor.kt
class PhotoProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun compressToJpeg(
        source: Uri,
        maxEdgePx: Int = 1080,
        quality: Int = 85
    ): ByteArray
    // Uses BitmapFactory.decodeStream with inSampleSize, decodes once at target size,
    // bitmap.compress(JPEG, quality, baos), returns bytes. Runs on Dispatchers.IO.
    // Throws SourceUnreadableException when ContentResolver.openInputStream returns null
    // or throws FileNotFoundException/SecurityException. Any other IOException is wrapped
    // and rethrown so the caller can treat it as an UploadFailed.
}

class SourceUnreadableException : IOException("Photo source could not be opened.")

// data/storage/PhotoUploader.kt
class PhotoUploader @Inject constructor(
    private val storage: FirebaseStorage,
    private val authManager: AuthManager
) {
    suspend fun uploadExercisePhoto(exerciseId: Int, bytes: ByteArray): String
    // 1. uid = authManager.currentUserId() ?: throw IllegalStateException("User is not signed in")
    // 2. ref = storage.reference.child("users/$uid/exercises/$exerciseId/photo.jpg")
    // 3. metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
    // 4. ref.putBytes(bytes, metadata).await()
    // 5. return ref.downloadUrl.await().toString()
    // Throws on auth-missing, network failure, or task failure; caller maps to UI state.

    suspend fun deleteExercisePhoto(exerciseId: Int)
    // Best-effort: uid -> ref -> ref.delete().await(). Timber.w on failure, do not rethrow.
}

// data/storage/PhotoUploadResult.kt
sealed interface PhotoUploadResult {
    data class Success(val photoUri: String) : PhotoUploadResult
    data object SourceUnreadable : PhotoUploadResult
    data class UploadFailed(val cause: Throwable) : PhotoUploadResult
}
```

### ViewModel

`WorkoutViewModel.updateExercisePhoto(exerciseId, sourceUri)` (replaces lines 356-364) becomes:

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
            val current = exercises.value.firstOrNull { it.id == exerciseId } ?: return@launch
            exerciseRepository.updateExercise(current.copy(photoUri = outcome.photoUri))
        }

        _photoUploadEvents.emit(outcome)
    }
}
```

`WorkoutViewModel` exposes a new `val photoUploadEvents: SharedFlow<PhotoUploadResult>` for the UI to collect.

### Photo picker wiring

`WorkoutsScreen.kt:126-141` changes from `(Int, String) -> Unit` to `(Int, Uri) -> Unit`. The Photo Picker callback stops converting the URI to a string at the edge — strings only enter the data model after a successful upload.

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

`takePersistableUriPermission` is kept so the lazy migrator can re-resolve the URI later on the same device.

### Lazy migration

`LegacyPhotoMigrator` is a Hilt-injected helper that observes the exercise list and re-uploads any `content://` URIs it can still open locally. The migrator maintains an in-memory `Set<String>` of URIs it has already attempted. When a new URI is observed that is not in the set, the migrator runs `ContentResolver.openInputStream(uri)`. On success the bytes are uploaded via `PhotoUploader.uploadExercisePhoto` and the Exercise doc is patched via `ExerciseRepository.updateExercise`. On any failure (URI expired, provider revoked, file deleted, upload failed) the URI is added to the set anyway so the migrator does not retry forever within the same app process. The set is process-lifetime only — a future app launch can retry once.

The UI does not see the migration: it just observes a stable `https://` URL arrive a moment after the exercise card first appears.

The migrator is owned by `MainViewModel` (or a dedicated startup coordinator) so it survives navigation but does not need to live in `WorkoutViewModel`, which is scoped to a session.

### Data model

No changes to `Exercise` or `CloudExercise`. The `photoUri: String?` field keeps its meaning; only the shape of the string it holds changes from `content://…` to `https://…`. This keeps `AGENTS.md` guidance intact (no assumptions reintroduced onto shared runtime models).

## Storage Security Rules

New file `storage.rules` at repo root, deployed via Firebase CLI:

```
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

The 2 MB ceiling enforces the client-side compression contract: a 1080 px JPEG at 85% quality is always well under 1 MB, so the rule rejects accidental raw-camera uploads. The `image/*` MIME guard prevents replacing the object with a script. The catch-all deny makes any future Storage path opt-in.

The release workflow in `.github/workflows/release.yml` gains a `firebase deploy --only storage` step that runs against the same Firebase project already used by the existing `google-services.json` rebuild. The deploy should be conditional on the same `secrets.FIREBASE_TOKEN` (or equivalent) that already gates other Firebase actions, and the existing `GOOGLE_SERVICES_JSON_B64` rebuild step remains unchanged. The exact placement and gating live in the implementation plan, not the spec.

## Error Handling

| Outcome | UI behavior |
|---|---|
| `Success(url)` | Card updates immediately via the existing Flow. No toast (Coil swaps the image). |
| `SourceUnreadable` | Snackbar: "Couldn't read that photo. Try picking it again." (strings.xml.) |
| `UploadFailed(cause)` | Snackbar: "Photo upload failed. Check your connection and retry." `Exercise.photoUri` is left unchanged. |
| Network drop mid-upload | `putBytes(...).await()` throws, mapped to `UploadFailed`, snackbar. Old photo, or none, is preserved. |

A Material 3 `SnackbarHostState` is added to both `WorkoutsScreen` and `WorkoutScreen` and collects `photoUploadEvents` from their respective `WorkoutViewModel` instances. Snackbar text is routed through `strings.xml` so translations are preserved.

## Deletion Semantics

- **Exercise deleted** (`CloudWorkoutRepository.deleteExercise` → `FirestoreRepository.markExerciseDeleted`): in the same coroutine, `PhotoUploader.deleteExercisePhoto(id)` removes the Storage object. A delete failure is logged via Timber and does not roll back the Firestore tombstone — the user's intent wins, the orphan is bounded by the per-user path and is reclaimable with a periodic GC job (YAGNI for v1).
- **Account / sign-out**: photos are kept. Firebase Auth sign-out does not touch Storage. Re-signing in on the same account restores photos. This matches user expectations of "my data is in my account."
- **Remove Photo** (existing dropdown at `ExerciseCard.kt:443-451`): the on-tap action now flows through a new `WorkoutViewModel.removeExercisePhoto(exerciseId)` coroutine that calls `PhotoUploader.deleteExercisePhoto(id)` and then `ExerciseRepository.updateExercise(... copy(photoUri = null))`. Delete failure is logged via Timber; the Firestore clear always proceeds, so the user's intent ("remove this photo") wins.

## Testing Strategy

Three seams, all unit-testable on the JVM. No instrumented test changes are required because `ExerciseCard`, `WorkoutScreen`, and `WorkoutsScreen` keep their public behavior — they read `photoUri` and Coil renders whatever string is there.

1. `PhotoProcessorTest` — given a fake `InputStream`-backed `Uri` mock, asserts output bytes are valid JPEG, total bytes are bounded for a typical 12 MP input (e.g. ≤ 500 KB), and the longest decoded edge is ≤ 1080 px. Uses `BitmapFactory`-compatible test fixtures.
2. `PhotoUploaderTest` — uses a `FakeStorage` Hilt module to stub `StorageReference.putBytes` and `StorageReference.downloadUrl`. Asserts: ref path is `users/{uid}/exercises/{id}/photo.jpg`, content-type metadata is `image/jpeg`, returned URL is the mock download URL. Asserts: throws when no signed-in user. Asserts: rethrows network failures unchanged. Asserts: `deleteExercisePhoto` swallows exceptions and does not throw.
3. `WorkoutViewModelTest` (extends the existing file at `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`):
   - `updateExercisePhoto` emits `Success` and calls `repository.updateExercise` with the new `https://` URI.
   - Emits `UploadFailed` and does **not** mutate the repo when the uploader throws a generic `IOException`.
   - Emits `SourceUnreadable` and does **not** mutate the repo when the processor throws `SourceUnreadableException`.
   - `removeExercisePhoto` calls `PhotoUploader.deleteExercisePhoto` and then clears `photoUri` on the repo, even when the Storage delete throws.

## Migration and Compatibility

- Existing cloud documents with `photoUri = "content://..."` are tolerated unchanged. No schema version bump on `CloudExercise` is required.
- The lazy migrator is best-effort. When an exercise is observed with a `content://` URI, it attempts `ContentResolver.openInputStream`. On success, the bytes are uploaded to Storage and the Firestore record is patched with the new `https://` URL. On failure (URI expired, provider revoked, file deleted), the value is left as-is and the UI shows the placeholder. The exercise itself is never lost or blocked.
- A `photoMigrated: Boolean` field is intentionally not added in v1. Per `AGENTS.md` guidance about not reintroducing assumptions on shared runtime models, this field can be introduced later only if a real bug surfaces that requires distinguishing "migrated" from "failed."

## Implementation Phases

1. Add `FirebaseStorage` to `di/AppModule.kt`; add `PhotoProcessor` and `PhotoUploader` with Hilt injection.
2. Add `PhotoUploadResult` sealed type and rewrite `WorkoutViewModel.updateExercisePhoto` to use the new pipeline. Expose `photoUploadEvents: SharedFlow<PhotoUploadResult>`.
3. Update `WorkoutsScreen` and `WorkoutScreen` photo picker callbacks to pass `Uri` instead of `String`. Add `SnackbarHostState` to both and collect the new event flow.
4. Add `LegacyPhotoMigrator`, wire it into the startup scope.
5. Cascade-delete Storage objects from `CloudWorkoutRepository.deleteExercise` and from the existing "Remove Photo" path in `ExerciseCard`.
6. Write `storage.rules`, add a `firebase deploy --only storage` step to the release workflow.
7. Add `PhotoProcessorTest`, `PhotoUploaderTest`, and the new `WorkoutViewModelTest` cases.
8. Run `./gradlew.bat :app:testDebugUnitTest` and `./gradlew.bat :app:assembleDebug`.

## Acceptance Criteria

- Picking a photo on a fresh install with a signed-in account produces a `https://` URL on the Exercise doc within ~2 s on Wi-Fi, and Coil renders the new URL.
- Signing in on a second device shows the same photo on the same exercise without any user action.
- Reinstalling the app on the same device, signing in with the same account, shows existing photos without a re-pick.
- Pre-existing exercises with `content://` URIs are transparently migrated the first time they're observed on a device that can still read the local file. On a fresh install on a second device, they show the placeholder until the user re-picks. No exercise data is lost.
- Storage rules reject any write to a path other than `users/{uid}/exercises/{id}/photo.jpg`, any payload > 2 MB, and any non-image content type. Verified manually with `firebase emulators:start` against a test project.
- All new code is covered by JVM unit tests; the three new test files are green; `./gradlew.bat :app:testDebugUnitTest` passes; `./gradlew.bat :app:assembleDebug` passes; existing `WorkoutScreenTest` and `WorkoutsScreenTest` continue to pass.
