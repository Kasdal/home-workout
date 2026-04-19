# Startup/Auth Test Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace stale startup/auth JVM tests with coverage for the migration-aware `AppEntryState` flow.

**Architecture:** Update the existing coordinator and MainViewModel unit tests to target `appEntryState()` instead of the old `startDestination()` seam. Keep production changes minimal and only add a narrow seam if current dependencies make the coordinator behavior unnecessarily awkward to test in JVM tests.

**Tech Stack:** Kotlin, JUnit4, MockK, Coroutines Test, Gradle

---

### Task 1: Update AppLaunchCoordinator Tests

**Files:**
- Modify: `app/src/test/java/com/example/workoutapp/domain/startup/AppLaunchCoordinatorTest.kt`
- Possibly modify: `app/src/main/java/com/example/workoutapp/domain/startup/AppLaunchCoordinator.kt`

- [ ] **Step 1: Replace the old `startDestination()` assertions with `appEntryState()` assertions**

Target scenarios:

```kotlin
assertEquals(AppEntryState.AuthRequired, result.first())
assertEquals(AppEntryState.MigrationInProgress, result.first())
assertEquals(AppEntryState.Ready("workout"), result.first())
assertEquals(AppEntryState.Ready("onboarding"), result.first())
```

- [ ] **Step 2: Cover migration-aware routing inputs**

Mock the dependency graph so the test can express:

```kotlin
every { authManager.currentUser } returns flowOf(user)
every { firestoreRepository.observeMigrationMeta(user.uid) } returns flowOf(CloudMigrationMeta(migrationComplete = true))
every { repository.getUserMetrics() } returns flowOf(UserMetrics(weightKg = 80f))
```

and the incomplete/missing migration case:

```kotlin
every { firestoreRepository.observeMigrationMeta(user.uid) } returns flowOf(null)
```

- [ ] **Step 3: Add only a minimal production seam if mocking the concrete dependency is too awkward**

If needed, introduce a narrow migration-meta observer abstraction rather than broad coordinator changes.

- [ ] **Step 4: Run focused coordinator tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.workoutapp.domain.startup.AppLaunchCoordinatorTest"`
Expected: PASS

### Task 2: Update MainViewModel Tests

**Files:**
- Modify: `app/src/test/java/com/example/workoutapp/MainViewModelTest.kt`

- [ ] **Step 1: Replace stale `startDestination()` mocks with `appEntryState()` mocks**

Use expectations shaped like:

```kotlin
every { appLaunchCoordinator.appEntryState() } returns flowOf(AppEntryState.Ready("workout"))
```

and:

```kotlin
every { appLaunchCoordinator.appEntryState() } returns flowOf(AppEntryState.AuthRequired)
```

- [ ] **Step 2: Keep existing MainViewModel assertions aligned with current behavior**

Test that the exposed state matches the coordinator flow and that theme/legacy seeding expectations still compile against the current ViewModel API.

- [ ] **Step 3: Run focused MainViewModel tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.workoutapp.MainViewModelTest"`
Expected: PASS

### Task 3: Run Full Unit Tests And Build

**Files:**
- No new files required unless a blocking unrelated test failure forces a targeted fix

- [ ] **Step 1: Run the full JVM unit test task**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: Fix only blocking test issues within the startup/auth test-update scope**

If failures are unrelated to the startup/auth test-update slice, stop and report them rather than broadening scope silently.

- [ ] **Step 3: Run assembleDebug after tests**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`
