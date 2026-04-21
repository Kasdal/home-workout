# Auth Migration Orchestration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove migration execution and backup workflow orchestration from `AuthViewModel` while preserving current auth and migration behavior.

**Architecture:** Introduce a focused auth-migration coordinator that owns migrate-if-needed, retry, export backup, and import backup workflows. Keep `AuthViewModel` as the UI-facing auth state holder that observes auth and maps workflow results into `AuthUiState`.

**Tech Stack:** Kotlin, Android ViewModel, Coroutines, Mutex, JUnit4, Coroutines Test

---

### Task 1: Extract Migration Execution Into An Auth-Migration Coordinator

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/ui/auth/AuthMigrationCoordinator.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/auth/AuthViewModel.kt`
- Possibly create: `app/src/test/java/com/example/workoutapp/ui/auth/AuthMigrationCoordinatorTest.kt`

- [ ] **Step 1: Add a focused auth-migration coordinator**

It should own migration execution and serialization, for example:

```kotlin
class AuthMigrationCoordinator @Inject constructor(
    private val migrationOrchestrator: MigrationOrchestrator
) {
    private val migrationMutex = Mutex()

    suspend fun migrateIfNeeded(uid: String): Result<Unit> {
        return migrationMutex.withLock {
            migrationOrchestrator.migrateIfNeeded(uid)
        }
    }
}
```

Adapt the exact shape to repo style, but keep it focused on migration workflow, not UI state.

- [ ] **Step 2: Remove direct migration execution logic from `AuthViewModel`**

Move the internal `migrate(uid)` execution/serialization out of `AuthViewModel` and make the ViewModel map the coordinator result into `AuthUiState`.

- [ ] **Step 3: Add focused tests for migration execution behavior**

Cover at least:
- successful migrate-if-needed
- failed migrate-if-needed
- serialized execution behavior if practical in the current test setup

- [ ] **Step 4: Run focused auth-migration coordinator tests**

Run the exact focused test command for the new coordinator test file.
Expected: PASS

- [ ] **Step 5: Build to verify the first extraction slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Extract Backup Import/Export Workflow Orchestration

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/auth/AuthMigrationCoordinator.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/auth/AuthViewModel.kt`
- Possibly modify: `app/src/test/java/com/example/workoutapp/ui/auth/AuthMigrationCoordinatorTest.kt`

- [ ] **Step 1: Move backup export/import workflow calls behind the coordinator**

The coordinator should own:
- `exportLegacyBackup()`
- `importLegacyBackup(uid, backupJson)`

and return plain workflow results for the ViewModel to map to UI state.

- [ ] **Step 2: Keep `AuthViewModel` focused on UI-state mapping**

`AuthViewModel` should still decide:
- loading flags
- success/error/info messages
- whether to set `isMigrationComplete`

but it should no longer call `MigrationOrchestrator` directly for import/export.

- [ ] **Step 3: Add focused tests for import/export workflow behavior**

Cover at least:
- successful export
- failed export
- successful import
- failed import

- [ ] **Step 4: Run focused auth-migration coordinator tests**

Run the exact focused test command for the coordinator test file.
Expected: PASS

- [ ] **Step 5: Build to verify the second extraction slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Align AuthViewModel Tests With The New Seam

**Files:**
- Modify: `app/src/test/java/com/example/workoutapp/ui/auth/AuthViewModelTest.kt` if present
- Or create it if the repo currently lacks focused AuthViewModel unit coverage

- [ ] **Step 1: Update or add focused AuthViewModel tests**

The ViewModel tests should focus on UI-state mapping behavior, not on direct migration execution details.

- [ ] **Step 2: Preserve boundary-level auth behavior coverage**

Cover at least:
- auth observation drives signed-in/signed-out state
- migration coordinator success/failure maps to `AuthUiState`
- import/export success/failure maps to the right loading/info/error fields

- [ ] **Step 3: Run focused AuthViewModel tests**

Run the exact focused test command for the AuthViewModel test file.
Expected: PASS

### Task 4: Run Full Unit Tests And Build

**Files:**
- No additional file targets unless a scoped regression fix is required

- [ ] **Step 1: Run the full JVM unit test task**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: Fix only scoped regressions caused by the auth-migration extraction**

If unrelated tests fail, stop and report them rather than broadening scope silently.

- [ ] **Step 3: Run the debug build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`
