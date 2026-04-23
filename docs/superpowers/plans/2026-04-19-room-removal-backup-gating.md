# Room Removal Backup Gating Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent app entry from bypassing the backup-import decision after Room removal when a signed-in user has no remote data.

**Architecture:** Extend the startup/auth gating path so the app remains on `AuthGateScreen` while `AuthViewModel` reports that a backup-import decision is pending. Keep the existing import/continue UI, and make app-entry readiness depend on both migration readiness and backup-import resolution.

**Tech Stack:** Kotlin, Android ViewModel, Coroutines Flow, Compose, JUnit4, Coroutines Test

---

### Task 1: Add A Backup-Import-Pending Gate To App Entry

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/domain/startup/AppLaunchCoordinator.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/auth/AuthViewModel.kt`
- Possibly modify: `app/src/main/java/com/example/workoutapp/MainViewModel.kt`

- [ ] **Step 1: Introduce an app-entry signal for pending backup import**

Use the smallest extension to the current startup/auth state model so app entry can distinguish:
- auth required
- migration in progress
- awaiting backup import decision
- ready

- [ ] **Step 2: Drive that state from `AuthViewModel`’s pending-import flag**

The app-entry path should not consider the app ready while `awaitingBackupImport == true`.

- [ ] **Step 3: Keep the change narrow**

Do not redesign sign-in or migration behavior; only add the missing gate.

- [ ] **Step 4: Run focused startup/auth tests if touched**

Run the exact focused test commands for any changed startup/auth tests.
Expected: PASS

- [ ] **Step 5: Build to verify the new gate compiles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Keep AuthGateScreen Reachable Until Decision Resolution

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/MainActivity.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/auth/AuthGateScreen.kt` only if needed

- [ ] **Step 1: Make MainActivity treat backup-import-pending as auth-gate-required**

The app should remain on `AuthGateScreen` until the user either:
- imports a backup successfully, or
- explicitly chooses to continue without import.

- [ ] **Step 2: Preserve existing import/continue/sign-out UI behavior**

The current prompt flow should remain intact; only its app-entry reachability should be fixed.

- [ ] **Step 3: Build to verify the integrated startup path**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Add Or Update Focused Tests For The New Gate

**Files:**
- Modify or create focused tests around startup/auth gating:
  - likely `AuthViewModelTest.kt`
  - likely `AppLaunchCoordinatorTest.kt`
  - possibly `MainViewModelTest.kt`

- [ ] **Step 1: Add a focused test for the backup-import-pending branch**

Cover at least:
- signed in + migration result requires backup import -> app entry is not ready yet

- [ ] **Step 2: Add a focused resolution test**

Cover at least one of:
- continue without import resolves the gate
- successful backup import resolves the gate

- [ ] **Step 3: Run focused test commands**

Run the exact focused test commands for the updated test files.
Expected: PASS

### Task 4: Run Full Verification

**Files:**
- No additional file targets unless a scoped regression fix is required

- [ ] **Step 1: Run the full JVM unit test task**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: Run the debug build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Stop if failures are unrelated**

If unrelated regressions appear, report them instead of broadening scope silently.
