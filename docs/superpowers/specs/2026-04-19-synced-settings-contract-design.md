# Synced Settings Contract Design

## Goal

Remove the active synced workout-settings path from the shared persisted `Settings` blob while leaving legacy migration and backup paths unchanged.

## Current State

- The settings UI now renders from `SettingsScreenState`.
- Local app preferences already live in `LocalAppPreferencesRepository` backed by DataStore.
- The remaining active coupling is in the synced workout-settings path:
  - `SettingsRepository` still exposes `Flow<Settings?>` and `saveSettings(Settings)`
  - `SyncedWorkoutSettingsRepository` still reads and writes the shared persisted `Settings` model
- The shared `Settings` blob is still legitimately used in legacy-only paths:
  - migration payloads
  - backup import/export codec
  - legacy seeding into DataStore

## Decision

Introduce a storage-neutral synced-settings contract for the active Firestore-backed workout-session settings only.

Keep the legacy `Settings` blob unchanged in migration, backup, and seeding paths for now.

## Why This Approach

- It finishes the active runtime settings seam without forcing risky changes into backup or migration logic.
- It shrinks the repository boundary to the fields the active synced path actually owns.
- It keeps the next steps incremental and reversible.

## Scope

### In Scope

- Add a dedicated synced workout-settings model for active runtime use.
- Narrow the active synced-settings repository contract to that model.
- Update the active Firestore-backed implementation to serve that contract.
- Update `SyncedWorkoutSettingsRepository` to depend on the narrowed contract.
- Update docs.

### Out Of Scope

- Renaming or deleting the legacy `Settings` model.
- Changing migration payloads or backup codec shapes.
- Changing `LocalAppPreferencesRepository.seedFromLegacySettingsIfUnset(settings: Settings)`.
- Room retirement work.

## Target Architecture After This Slice

- Active runtime synced settings use a focused model such as `SyncedWorkoutSettings`.
- The synced-settings path no longer depends on `data.local.entity.Settings`.
- The legacy `Settings` blob remains behind migration, backup, and bootstrap boundaries only.

## Proposed Active Synced Model

A small storage-neutral model containing only the Firestore-backed workout-session fields:

- `restTimerDuration`
- `exerciseSwitchDuration`
- `undoLastSetEnabled`

This can either replace the existing `WorkoutSessionSettings` type or sit just below it, depending on what keeps the code smaller.

## Repository Direction

The active synced-settings seam should expose dedicated operations, for example:

- observe synced workout settings
- save synced workout settings

The exact naming should follow the existing repository style and stay minimal.

## Storage Behavior

- Firestore may continue using the same underlying settings document if that is the smallest change.
- The important change is the app-side contract and dependency boundary, not the remote document shape.

## Files Likely To Change

- `app/src/main/java/com/example/workoutapp/data/repository/SettingsRepository.kt`
- `app/src/main/java/com/example/workoutapp/data/repository/CloudWorkoutRepository.kt`
- `app/src/main/java/com/example/workoutapp/data/settings/SyncedWorkoutSettingsRepository.kt`
- possibly `app/src/main/java/com/example/workoutapp/data/remote/FirestoreRepository.kt`
- `docs/decoupling-plan.md`
- `Architecture.md`

## Verification

- Run `./gradlew :app:assembleDebug` after the slice.
- Only expand test work if the chosen seam naturally requires test updates.
