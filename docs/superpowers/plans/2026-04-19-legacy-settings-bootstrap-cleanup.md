# Legacy Settings Bootstrap Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove direct active runtime reads of the legacy shared `Settings` blob from startup and workout/session ViewModels while keeping legacy settings seeding behavior intact.

**Architecture:** Introduce a dedicated bootstrap/helper seam for reading legacy `Settings` and seeding DataStore-backed app preferences only when values are still unset. Update active runtime ViewModels to depend on that bootstrap seam rather than reading `SettingsRepository` directly.

**Tech Stack:** Kotlin, Android, Hilt, Coroutines Flow, DataStore, Firestore

---

### Task 1: Introduce A Legacy Settings Bootstrap Seam

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/data/settings/LegacySettingsBootstrapper.kt`
- Possibly modify: `app/src/main/java/com/example/workoutapp/di/AppModule.kt`

- [ ] **Step 1: Add a narrow helper that owns legacy settings seeding**

The helper should do one thing:

```kotlin
class LegacySettingsBootstrapper @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localAppPreferencesRepository: LocalAppPreferencesRepository
) {
    suspend fun seedLocalPreferencesFromLegacySettingsIfNeeded() {
        settingsRepository.getSettings().collect { settings ->
            settings?.let { localAppPreferencesRepository.seedFromLegacySettingsIfUnset(it) }
        }
    }
}
```

Keep the real implementation minimal and aligned with the current flow/lifecycle usage in the codebase.

- [ ] **Step 2: Keep the seam bootstrap-only**

Do not broaden it into a general-purpose settings service. It should exist only to hide legacy `Settings` reads from active runtime ViewModels.

- [ ] **Step 3: Build to verify the seam compiles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Remove Direct Legacy Settings Reads From MainViewModel And SettingsViewModel

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/MainViewModel.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/settings/SettingsViewModel.kt`

- [ ] **Step 1: Replace direct `SettingsRepository.getSettings()` usage with the bootstrap seam**

Remove direct logic shaped like:

```kotlin
settingsRepository.getSettings().collect { settings ->
    settings?.let { localAppPreferencesRepository.seedFromLegacySettingsIfUnset(it) }
}
```

and route this behavior through the new helper instead.

- [ ] **Step 2: Keep runtime behavior unchanged**

`MainViewModel` and `SettingsViewModel` should still seed unset DataStore values from legacy settings, but they should no longer know about the legacy `Settings` blob directly.

- [ ] **Step 3: Build to verify the startup/settings slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Remove Direct Legacy Settings Reads From WorkoutViewModel

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`

- [ ] **Step 1: Replace any direct legacy settings read used only for bootstrap/seeding**

If `WorkoutViewModel` still reads `SettingsRepository.getSettings()` only to seed unset DataStore-backed settings, route that through the bootstrap/helper seam instead.

- [ ] **Step 2: Avoid broader session-logic refactoring in this slice**

Do not change timer/session/sensor orchestration unless strictly required to preserve the existing seeding behavior.

- [ ] **Step 3: Build to verify the workout/bootstrap slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 4: Refresh Docs For The New Bootstrap Boundary

**Files:**
- Modify: `docs/decoupling-plan.md`
- Modify: `Architecture.md`

- [ ] **Step 1: Update architecture guidance**

Document these facts:

```text
Active runtime ViewModels no longer read the legacy Settings blob directly.
Legacy settings seeding now runs through a dedicated bootstrap/helper seam.
The legacy Settings model remains behind migration, backup, and bootstrap-only boundaries.
```

- [ ] **Step 2: Update decoupling-plan status**

Mark the direct runtime `Settings` readers cleanup as complete while noting that the legacy blob still exists for migration/backup/bootstrap boundaries.

- [ ] **Step 3: Build to verify the final slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`
