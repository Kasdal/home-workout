# Shared Model Neutralization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move shared runtime models out of `data.local.entity` into a storage-neutral package while leaving the legacy `Settings` blob in place for now.

**Architecture:** Relocate shared runtime models and enums to a storage-neutral package and update imports across the app, domain, repository, remote mapping, and tests. Keep Room-specific storage shapes under `data/local/room/entity/`, and keep `Settings` in the legacy package until a later slice.

**Tech Stack:** Kotlin, Android, Hilt, Gradle

---

### Task 1: Move Core Shared Models To A Storage-Neutral Package

**Files:**
- Move: `app/src/main/java/com/example/workoutapp/data/local/entity/Exercise.kt`
- Move: `app/src/main/java/com/example/workoutapp/data/local/entity/ExerciseSessionMode.kt`
- Move: `app/src/main/java/com/example/workoutapp/data/local/entity/RestDay.kt`
- Move: `app/src/main/java/com/example/workoutapp/data/local/entity/SessionExercise.kt`
- Move: `app/src/main/java/com/example/workoutapp/data/local/entity/UserMetrics.kt`
- Move: `app/src/main/java/com/example/workoutapp/data/local/entity/WorkoutSession.kt`
- Move: `app/src/main/java/com/example/workoutapp/data/local/entity/WorkoutStats.kt`
- Move: `app/src/main/java/com/example/workoutapp/data/local/entity/ExerciseType.kt` if present in that package

- [ ] **Step 1: Create the storage-neutral destination package**

Move the shared runtime models into a package such as:

```text
app/src/main/java/com/example/workoutapp/model/
```

Use the same type names; only the package should change.

- [ ] **Step 2: Keep `Settings` in the legacy package**

Do not move:

```text
app/src/main/java/com/example/workoutapp/data/local/entity/Settings.kt
```

in this slice.

- [ ] **Step 3: Build to verify the moved model files compile**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Update Runtime Imports Across App, Domain, And Repositories

**Files:**
- Modify imports across app code that still reference `com.example.workoutapp.data.local.entity.*`

- [ ] **Step 1: Update active runtime imports to the new model package**

This includes likely files under:
- `ui/`
- `domain/`
- `data/repository/`
- `data/remote/`
- `util/`

Examples:

```kotlin
import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.model.UserMetrics
```

- [ ] **Step 2: Keep legacy `Settings` imports unchanged**

Any import of `data.local.entity.Settings` that still belongs to migration, backup, bootstrap, or legacy repository paths should remain as-is.

- [ ] **Step 3: Build to verify the runtime import migration**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Update Room Mappers, Remote Models, And Tests

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/data/local/room/entity/RoomMappers.kt`
- Modify: `app/src/main/java/com/example/workoutapp/data/remote/model/CloudModels.kt`
- Modify tests importing moved models

- [ ] **Step 1: Point Room mappers at the new shared model package**

Room-specific entity classes should continue mapping to the shared runtime models, but now from the storage-neutral package.

- [ ] **Step 2: Point remote/cloud mapping code at the new shared model package**

Remote model mappers should import the moved shared models from the new package.

- [ ] **Step 3: Update test imports**

Any test file that imports moved shared runtime models should use the new package.

- [ ] **Step 4: Run full JVM unit tests**

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
