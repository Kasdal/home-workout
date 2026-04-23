# Settings Package Neutralization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move `Settings` into the storage-neutral shared model package without changing its shape or behavior.

**Architecture:** Relocate `Settings` from `data.local.entity` to the shared neutral model package and update imports across repositories, remote mapping, migration/backup/bootstrap code, room mappers, and tests. Preserve the model’s fields and all existing storage/wire behavior.

**Tech Stack:** Kotlin, Android, Hilt, Gradle

---

### Task 1: Move `Settings` To The Neutral Model Package

**Files:**
- Move: `app/src/main/java/com/example/workoutapp/data/local/entity/Settings.kt`

- [ ] **Step 1: Relocate the file into the neutral model package**

Move:

```text
app/src/main/java/com/example/workoutapp/data/local/entity/Settings.kt
```

to:

```text
app/src/main/java/com/example/workoutapp/model/Settings.kt
```

Keep the type name and fields unchanged.

- [ ] **Step 2: Do not redesign the model in this slice**

No field changes, no renames, no behavior changes.

- [ ] **Step 3: Build to verify the moved file compiles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Update Runtime And Legacy-Boundary Imports

**Files:**
- Modify import sites that still reference `com.example.workoutapp.data.local.entity.Settings`

- [ ] **Step 1: Update runtime and legacy-boundary imports to the new package**

Likely files include:
- `data/repository/SettingsRepository.kt`
- `data/repository/CloudWorkoutRepository.kt`
- `data/remote/FirestoreRepository.kt`
- `data/remote/model/CloudModels.kt`
- `data/remote/LegacyMigrationDataSource.kt`
- `data/remote/LegacyMigrationBackupCodec.kt`
- `data/settings/LocalAppPreferencesRepository.kt`
- `data/local/room/entity/RoomMappers.kt`

- [ ] **Step 2: Keep behavior unchanged**

Only package/import updates should happen. Firestore mapping, backup payload format, bootstrap behavior, and room mapping behavior should stay the same.

- [ ] **Step 3: Build to verify the import migration**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Update Tests Importing `Settings`

**Files:**
- Modify tests that still import `Settings` from the old package

- [ ] **Step 1: Update test imports to `com.example.workoutapp.model.Settings`**

Change only the moved `Settings` package references in tests.

- [ ] **Step 2: Run full JVM unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

### Task 4: Run Final Build Verification

**Files:**
- No additional file targets unless a scoped regression fix is required

- [ ] **Step 1: Run the debug build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Stop if failures are unrelated**

If unrelated regressions appear, report them instead of broadening the scope silently.
