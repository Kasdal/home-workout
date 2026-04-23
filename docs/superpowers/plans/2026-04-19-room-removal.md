# Room Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Room and the Room-backed legacy fallback now that runtime paths, shared models, and migration boundaries are decoupled.

**Architecture:** Delete the isolated Room adapter layer (`WorkoutDatabase`, `WorkoutDao`, Room entities, room mappers, and Room-backed legacy migration data source) and simplify the remaining migration path so backup/import/export are the only surviving legacy recovery mechanism.

**Tech Stack:** Kotlin, Android, Hilt, Gradle

---

### Task 1: Remove Room Adapter Code And DI Wiring

**Files:**
- Delete: `app/src/main/java/com/example/workoutapp/data/local/WorkoutDatabase.kt`
- Delete: `app/src/main/java/com/example/workoutapp/data/local/dao/WorkoutDao.kt`
- Delete: `app/src/main/java/com/example/workoutapp/data/local/room/entity/*`
- Delete: `app/src/main/java/com/example/workoutapp/data/remote/RoomLegacyMigrationDataSource.kt`
- Modify: `app/src/main/java/com/example/workoutapp/di/AppModule.kt`

- [ ] **Step 1: Remove Room-only source files**

Delete the Room database, DAO, Room entity classes, and Room-backed legacy migration data source.

- [ ] **Step 2: Remove Room providers and bindings from DI**

Delete Room-specific provider code and the Room-backed `LegacyMigrationDataSource` binding from `AppModule`.

- [ ] **Step 3: Build to reveal remaining compile references**

Run: `./gradlew :app:assembleDebug`
Expected: either `BUILD SUCCESSFUL` or compile errors that point to remaining Room references to clean up in later tasks.

### Task 2: Remove The LegacyMigrationDataSource Abstraction If No Longer Needed

**Files:**
- Delete or modify: `app/src/main/java/com/example/workoutapp/data/remote/LegacyMigrationDataSource.kt`
- Modify: `app/src/main/java/com/example/workoutapp/data/remote/MigrationOrchestrator.kt`
- Modify: any remaining callers/tests that still reference the abstraction

- [ ] **Step 1: Collapse the migration path onto the surviving backup/import/export mechanism**

If `LegacyMigrationDataSource` exists only to abstract the removed Room path, remove it and simplify `MigrationOrchestrator` accordingly.

- [ ] **Step 2: Keep backup/import/export behavior intact**

The surviving migration path should continue to support export/import of legacy backups.

- [ ] **Step 3: Build to verify migration path compiles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Update Imports, Tests, And Docs For A Room-Free Codebase

**Files:**
- Modify: tests still importing Room classes
- Modify: `docs/decoupling-plan.md`
- Modify: `Architecture.md`

- [ ] **Step 1: Remove or update tests that reference deleted Room classes**

Any JVM or androidTest imports/usages of deleted Room files must be removed or updated.

- [ ] **Step 2: Update docs to say Room is removed**

Document that:
- Room is no longer present
- backup/import/export is the surviving legacy recovery mechanism
- the old Room fallback path has been retired

- [ ] **Step 3: Run full JVM unit tests**

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
