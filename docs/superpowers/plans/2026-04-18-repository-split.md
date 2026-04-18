# Repository Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove `WorkoutRepository` from active app code while keeping Room isolated to legacy migration and backup fallback.

**Architecture:** Keep `CloudWorkoutRepository` as the single Firestore-backed runtime implementation, but expose it only through focused repository interfaces. Update helper repositories and DI wiring so active app code depends only on the narrow interfaces it actually uses.

**Tech Stack:** Kotlin, Android, Hilt, Coroutines Flow, Firestore, Room (legacy-only)

---

### Task 1: Finish Runtime Settings Decoupling

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/data/settings/SyncedWorkoutSettingsRepository.kt`
- Modify: `app/src/main/java/com/example/workoutapp/data/repository/SettingsRepository.kt`

- [ ] **Step 1: Replace broad repository dependency**

```kotlin
class SyncedWorkoutSettingsRepository @Inject constructor(
    private val settingsRepository: SettingsRepository
)
```

- [ ] **Step 2: Switch all method calls to `settingsRepository`**

```kotlin
return settingsRepository.getSettings().map { settings ->
    WorkoutSessionSettings(
        restTimerDuration = settings?.restTimerDuration ?: 30,
        exerciseSwitchDuration = settings?.exerciseSwitchDuration ?: 90,
        undoLastSetEnabled = settings?.undoLastSetEnabled ?: true
    )
}
```

```kotlin
val current = settingsRepository.getSettings().first() ?: Settings()
settingsRepository.saveSettings(current.copy(restTimerDuration = seconds))
```

- [ ] **Step 3: Build to verify the feature slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Remove Active DI Use Of `WorkoutRepository`

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/di/AppModule.kt`
- Modify: `app/src/main/java/com/example/workoutapp/data/repository/CloudWorkoutRepository.kt`

- [ ] **Step 1: Remove the `WorkoutRepository` binding from Hilt**

```kotlin
@Provides
@Singleton
fun provideProfileRepository(
    cloudWorkoutRepository: CloudWorkoutRepository
): ProfileRepository = cloudWorkoutRepository
```

Keep only focused interface providers.

- [ ] **Step 2: Remove `WorkoutRepository` from the implementation contract list**

```kotlin
class CloudWorkoutRepository @Inject constructor(
    private val authManager: AuthManager,
    private val firestoreRepository: FirestoreRepository
 ) : ProfileRepository,
    SessionHistoryRepository,
    RestDayRepository,
    ExerciseRepository,
    SettingsRepository {
```

- [ ] **Step 3: Build to verify the feature slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Remove The Broad Repository Contract

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/data/repository/WorkoutRepository.kt`
- Search: `app/src/main/java/**/*.kt`

- [ ] **Step 1: Verify there are no remaining active references**

Run: `rg "WorkoutRepository" app/src/main/java`
Expected: no references outside legacy docs or the soon-to-be-deleted interface file

- [ ] **Step 2: Delete the unused contract if no code references remain**

Delete:

```text
app/src/main/java/com/example/workoutapp/data/repository/WorkoutRepository.kt
```

- [ ] **Step 3: Build to verify the feature slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 4: Refresh Architecture Docs

**Files:**
- Modify: `Architecture.md`
- Modify: `docs/decoupling-plan.md`

- [ ] **Step 1: Update architecture text to match runtime reality**

Document these facts:

```text
Firestore is the runtime source of truth.
Room is legacy-only for migration/export fallback.
Active app code depends on focused repository interfaces.
CloudWorkoutRepository remains the single runtime implementation.
```

- [ ] **Step 2: Update decoupling plan status**

Document that repository split is complete for active app code, while concrete implementation splitting and Room retirement remain future work.

- [ ] **Step 3: Build to verify the final slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`
