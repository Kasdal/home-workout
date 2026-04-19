# Legacy Settings Bootstrap Cleanup Design

## Goal

Remove the remaining active runtime reads of the legacy shared `Settings` blob from app startup and workout/session code, leaving that blob behind migration and backup boundaries only.

## Current State

- The settings UI is already decoupled from the persisted `Settings` blob.
- Active synced workout settings now use a focused runtime contract.
- The legacy `Settings` blob is still read in a few active runtime places only to seed unset DataStore values:
  - `MainViewModel`
  - `SettingsViewModel`
  - `WorkoutViewModel`
- Those readers do not need the full shared model for normal runtime behavior; they only preserve old user preferences during bootstrap.

## Decision

Move legacy settings seeding behind a dedicated bootstrap seam so active runtime ViewModels no longer read `SettingsRepository.getSettings()` directly.

Keep the legacy `Settings` model itself unchanged for now, because it still belongs to migration and backup boundaries.

## Why This Approach

- It is the lowest-risk next decoupling step.
- It removes a remaining active runtime dependency on the legacy blob without changing user-facing behavior.
- It keeps the migration and backup code stable.
- It prepares a later slice where the legacy `Settings` model can become migration-only.

## Scope

### In Scope

- Introduce a dedicated bootstrap/seeding seam for legacy settings reads.
- Remove direct `SettingsRepository.getSettings()` usage from `MainViewModel`, `SettingsViewModel`, and `WorkoutViewModel`.
- Keep legacy settings seeding behavior intact.
- Update docs.

### Out Of Scope

- Renaming or deleting the legacy `Settings` model.
- Changing backup payloads or migration data sources.
- Refactoring broader `WorkoutViewModel` session logic beyond this bootstrap concern.
- Room removal.

## Target Architecture After This Slice

- Active runtime ViewModels no longer read the shared legacy `Settings` blob directly.
- Legacy settings seeding is handled through a dedicated bootstrap component or narrowly scoped helper.
- The legacy `Settings` model remains behind migration, backup, and bootstrap-only boundaries.

## Likely Shape

A small helper or service that does one thing:

- read legacy `Settings` if present
- seed `LocalAppPreferencesRepository` only where values are still unset

This helper should be called from the narrowest appropriate startup/bootstrap points rather than from multiple runtime ViewModels.

## Files Likely To Change

- `app/src/main/java/com/example/workoutapp/MainViewModel.kt`
- `app/src/main/java/com/example/workoutapp/ui/settings/SettingsViewModel.kt`
- `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- one new bootstrap/helper file if needed
- `docs/decoupling-plan.md`
- `Architecture.md`

## Verification

- Run `./gradlew :app:assembleDebug` after the slice.
- Add or update focused tests only if the chosen bootstrap seam naturally needs them.
