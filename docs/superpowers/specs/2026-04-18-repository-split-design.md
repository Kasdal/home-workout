# Repository Split Design

## Goal

Finish removing `WorkoutRepository` from active app code while keeping Room only for legacy migration and backup import/export fallback.

## Current State

- Firestore is the runtime source of truth.
- `CloudWorkoutRepository` is the single runtime implementation.
- Most active consumers already depend on narrow interfaces: `ProfileRepository`, `ExerciseRepository`, `SessionHistoryRepository`, `RestDayRepository`, and `SettingsRepository`.
- `WorkoutRepository` still remains as a broad compatibility seam.
- `SyncedWorkoutSettingsRepository` still depends on `WorkoutRepository`.
- Room remains in use only through `LegacyMigrationDataSource` during auth-time migration and manual legacy backup handling.

## Decision

Use the smallest completion step:

- Keep one concrete runtime implementation class: `CloudWorkoutRepository`.
- Remove `WorkoutRepository` from active app wiring.
- Make helper repositories depend only on the narrow interfaces they actually need.
- Leave Room, `WorkoutDatabase`, `WorkoutDao`, `MigrationOrchestrator`, and `LegacyMigrationDataSource` unchanged in this slice.

## Why This Approach

- It completes the active decoupling work already underway in the dirty worktree.
- It minimizes risk in auth, startup, and migration flows.
- It keeps behavior stable while tightening boundaries.
- It preserves a later option to split `CloudWorkoutRepository` into multiple concrete classes only if that becomes necessary.

## Scope

### In Scope

- Remove direct active-app dependence on `WorkoutRepository`.
- Update DI to provide only the narrow repository interfaces for runtime consumers.
- Update `SyncedWorkoutSettingsRepository` to depend on `SettingsRepository`.
- Update documentation to reflect the actual architecture.

### Out of Scope

- Removing Room in this release line.
- Changing migration fallback behavior.
- Splitting `CloudWorkoutRepository` into multiple classes.
- Fixing or expanding tests unless they block the build or are part of an explicit test-fix task.

## Target Architecture After This Slice

- Firestore-backed runtime services are consumed through focused repository interfaces.
- `CloudWorkoutRepository` implements the focused interfaces.
- Shared app logic no longer depends on the broad `WorkoutRepository` contract.
- Room remains isolated to the legacy migration path.

## Files Expected To Change

- `app/src/main/java/com/example/workoutapp/data/settings/SyncedWorkoutSettingsRepository.kt`
- `app/src/main/java/com/example/workoutapp/di/AppModule.kt`
- `app/src/main/java/com/example/workoutapp/data/repository/CloudWorkoutRepository.kt`
- `app/src/main/java/com/example/workoutapp/data/repository/WorkoutRepository.kt`
- `Architecture.md`
- `docs/decoupling-plan.md`

## Verification

- Build after each finished feature slice with `./gradlew :app:assembleDebug`.
- Skip test work until explicitly fixing tests.
