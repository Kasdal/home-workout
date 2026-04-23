# Room Retirement Preparation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prepare the codebase for safe Room retirement without removing the Room-backed legacy fallback in this release line.

**Architecture:** Tighten the remaining Room-backed fallback boundary so it is visibly isolated to the migration/import path, and document the exact deletion set and preconditions for the eventual Room-removal slice. Preserve current fallback behavior.

**Tech Stack:** Kotlin, Android, Room, Hilt, Gradle

---

### Task 1: Verify And Tighten The Remaining Room Boundary

**Files:**
- Likely modify a small subset of:
  - `app/src/main/java/com/example/workoutapp/data/remote/LegacyMigrationDataSource.kt`
  - `app/src/main/java/com/example/workoutapp/data/remote/RoomLegacyMigrationDataSource.kt`
  - `app/src/main/java/com/example/workoutapp/data/remote/MigrationOrchestrator.kt`
  - `app/src/main/java/com/example/workoutapp/di/AppModule.kt`

- [ ] **Step 1: Check for Room leakage beyond the intended boundary**

The intended surviving Room path should remain concentrated around:
- `WorkoutDatabase`
- `WorkoutDao`
- `RoomLegacyMigrationDataSource`
- `LegacyMigrationDataSource`
- `MigrationOrchestrator`
- Room providers in `AppModule`

- [ ] **Step 2: Make only minimal boundary-tightening changes if needed**

If any Room concepts still leak beyond that path, tighten them with the smallest structural change. Do not remove Room in this task.

- [ ] **Step 3: Build to verify the boundary still compiles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Document The Final Room-Removal Deletion Set

**Files:**
- Modify: `docs/decoupling-plan.md`
- Modify: `Architecture.md`

- [ ] **Step 1: Add an explicit Room-removal checklist**

Document the concrete deletion set and preconditions, including likely files/components such as:

```text
WorkoutDatabase
WorkoutDao
data/local/room/entity/*
RoomMappers
RoomLegacyMigrationDataSource
LegacyMigrationDataSource + bindings if no longer needed
Room providers in AppModule
```

Make it explicit that actual deletion is deferred until fallback removal is approved.

- [ ] **Step 2: Update architecture notes to describe Room as a temporary legacy adapter only**

Clarify that normal runtime paths no longer depend on Room, and only the legacy migration/import fallback path remains.

- [ ] **Step 3: Build to verify docs-only slice didn’t disturb the codebase**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Run Final Verification

**Files:**
- No additional file targets unless a scoped regression fix is required

- [ ] **Step 1: Run full JVM unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: Run final debug build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Stop if failures are unrelated**

If unrelated regressions appear, report them rather than broadening the scope silently.
