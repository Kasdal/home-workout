# Synced Settings Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the active synced workout-settings path from the shared persisted `Settings` blob while leaving legacy migration and backup paths unchanged.

**Architecture:** Introduce a focused synced-settings model and repository contract for the active Firestore-backed workout-session fields. Keep the legacy `Settings` model only in migration, backup, and bootstrap paths, and update runtime consumers to depend on the narrower contract.

**Tech Stack:** Kotlin, Android, Hilt, Coroutines Flow, Firestore

---

### Task 1: Add A Focused Synced Settings Model And Contract

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/data/repository/SettingsRepository.kt`
- Possibly create: `app/src/main/java/com/example/workoutapp/data/settings/SyncedWorkoutSettings.kt`

- [ ] **Step 1: Introduce a focused synced-settings model**

Use a storage-neutral model containing only:

```kotlin
data class SyncedWorkoutSettings(
    val restTimerDuration: Int = 30,
    val exerciseSwitchDuration: Int = 90,
    val undoLastSetEnabled: Boolean = true
)
```

If the existing `WorkoutSessionSettings` can serve this role cleanly with no duplication, reuse it instead of creating another type.

- [ ] **Step 2: Narrow the repository contract for active synced settings**

Change the active runtime seam from full-blob methods like:

```kotlin
fun getSettings(): Flow<Settings?>
suspend fun saveSettings(settings: Settings)
```

to a focused synced-settings contract, for example:

```kotlin
fun observeSyncedWorkoutSettings(): Flow<SyncedWorkoutSettings>
suspend fun saveSyncedWorkoutSettings(settings: SyncedWorkoutSettings)
```

Keep naming aligned with repo patterns and stay minimal.

- [ ] **Step 3: Build to verify the new contract compiles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Move The Firestore-Backed Implementation To The Focused Contract

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/data/repository/CloudWorkoutRepository.kt`
- Possibly modify: `app/src/main/java/com/example/workoutapp/data/remote/FirestoreRepository.kt`

- [ ] **Step 1: Implement the new synced-settings repository methods**

Map the existing remote settings document to the focused synced-settings model. The underlying Firestore document may remain unchanged if that is the smallest implementation.

- [ ] **Step 2: Keep legacy full `Settings` behavior out of the active contract**

Do not break legacy migration/backup/bootstrap paths. If those still need full `Settings`, keep them behind their existing non-active boundaries.

- [ ] **Step 3: Build to verify the runtime implementation slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Move SyncedWorkoutSettingsRepository Off The Legacy Blob

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/data/settings/SyncedWorkoutSettingsRepository.kt`

- [ ] **Step 1: Remove direct dependence on `data.local.entity.Settings`**

Replace code shaped like:

```kotlin
val current = settingsRepository.getSettings().first() ?: Settings()
settingsRepository.saveSettings(current.copy(restTimerDuration = seconds))
```

with focused synced-settings reads and writes against the narrowed contract.

- [ ] **Step 2: Keep the public runtime behavior unchanged**

`observeSessionSettings()`, `setRestTimerDuration()`, `setExerciseSwitchDuration()`, and `setUndoLastSetEnabled()` should still behave the same from the rest of the app’s perspective.

- [ ] **Step 3: Build to verify the active synced-settings seam**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 4: Refresh Documentation For The New Boundary

**Files:**
- Modify: `docs/decoupling-plan.md`
- Modify: `Architecture.md`

- [ ] **Step 1: Update architecture guidance**

Document these facts:

```text
Active synced workout settings now use a focused synced-settings contract.
The active synced-settings path no longer depends on the shared persisted Settings blob.
The legacy Settings model remains only in migration, backup, and bootstrap/seeding paths.
```

- [ ] **Step 2: Update decoupling-plan status**

Mark the active synced-settings seam as complete while noting that the legacy `Settings` blob still exists behind migration/backup boundaries for now.

- [ ] **Step 3: Build to verify the final slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`
