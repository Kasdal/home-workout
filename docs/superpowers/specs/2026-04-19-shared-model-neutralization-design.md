# Shared Model Neutralization Design

## Goal

Move shared runtime models out of `data.local.entity` into a storage-neutral package, while leaving the legacy `Settings` blob in place for now.

## Current State

- Most active runtime/shared models still live under `com.example.workoutapp.data.local.entity`.
- These models are no longer true Room entities, but the package name still implies local-database ownership.
- Room-specific copies already exist under `data/local/room/entity/`.
- The legacy `Settings` blob is still special because it remains behind migration, backup, and bootstrap boundaries.

## Decision

Move the shared runtime models and enums out of `data.local.entity` into a storage-neutral package.

Leave `data.local.entity.Settings` where it is for now.

## Why This Approach

- It makes the package structure match reality.
- It reduces confusion before the eventual Room-retirement work.
- It avoids broadening into the still-legacy `Settings` blob in the same slice.

## Scope

### In Scope

- Move shared runtime models out of `data.local.entity`.
- Update imports across app, domain, repository, and remote mapping code.
- Keep Room-specific copies in `data/local/room/entity/` untouched.

### Out Of Scope

- Moving `Settings` in this slice.
- Changing storage formats, migration payload formats, or repository behavior.
- Room schema changes.

## Candidate Models To Move

- `Exercise`
- `ExerciseSessionMode`
- `ExerciseType` if still in the legacy package
- `RestDay`
- `SessionExercise`
- `UserMetrics`
- `WorkoutSession`
- `WorkoutStats`

## Target Package

Use a storage-neutral package such as:

- `com.example.workoutapp.model`

The exact package can be adjusted to match repo conventions, but it should not imply local or Room ownership.

## Target Architecture After This Slice

- Shared runtime models live in a storage-neutral package.
- Room-specific storage shapes remain under `data/local/room/entity/`.
- `Settings` remains in `data.local.entity` temporarily because it still belongs to legacy migration/backup/bootstrap boundaries.

## Files Likely To Change

- moved model files under `app/src/main/java/com/example/workoutapp/...`
- import updates across:
  - `ui/`
  - `domain/`
  - `data/repository/`
  - `data/remote/`
  - `data/local/room/entity/RoomMappers.kt`
  - tests that import those models

## Verification

- Run `./gradlew :app:testDebugUnitTest`.
- Run `./gradlew :app:assembleDebug`.
