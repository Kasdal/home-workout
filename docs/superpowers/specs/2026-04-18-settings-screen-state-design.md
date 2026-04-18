# Settings Screen State Design

## Goal

Remove the settings screen's dependency on the persisted `data.local.entity.Settings` model by introducing a storage-neutral UI state model.

## Current State

- `SettingsViewModel` exposes `StateFlow<Settings>`.
- That `Settings` type is a persisted model still shared with Firestore mappings, legacy Room mapping, migration backup codec, and migration fallback.
- Settings ownership is already split in practice:
  - local app preferences live in `LocalAppPreferencesRepository`
  - synced workout-session settings live in `SyncedWorkoutSettingsRepository`
- The remaining coupling is primarily in the UI layer.

## Decision

Introduce `SettingsScreenState` under `ui/settings/` and make `SettingsViewModel` expose only that type to the screen.

Keep the persisted `Settings` model unchanged for now because it still participates in:

- Firestore settings persistence
- legacy Room mapping
- migration backup/import-export
- legacy seeding into DataStore

## Why This Approach

- It completes the UI-facing part of the settings split with low risk.
- It avoids broad migration and persistence churn in the same slice.
- It creates a cleaner seam for a future persisted-model split if needed.

## Scope

### In Scope

- Add a neutral screen-state type for settings UI.
- Update `SettingsViewModel` to build and expose that screen state.
- Keep screen behavior unchanged.
- Update docs to reflect the new boundary.

### Out Of Scope

- Renaming or splitting the persisted `Settings` model.
- Changing Firestore settings storage.
- Changing legacy Room or migration fallback behavior.
- Test fixes unless explicitly requested later.

## Target Architecture After This Slice

- `SettingsScreen` reads from `SettingsScreenState`.
- `SettingsViewModel` composes local and synced settings into that UI state.
- `data.local.entity.Settings` stays behind repository, mapping, and migration boundaries.

## Proposed State Shape

`SettingsScreenState` should contain only the fields the settings UI actually needs:

- `soundsEnabled`
- `soundVolume`
- `timerSoundType`
- `celebrationSoundType`
- `themeMode`
- `tutorialCompleted`
- `tutorialVersion`
- `restTimerDuration`
- `exerciseSwitchDuration`
- `undoLastSetEnabled`
- `sensorEnabled`
- `sensorIpAddress`

Keep `sensorConnectionState` as a separate `StateFlow<String?>` in `SettingsViewModel` for this slice to avoid mixing transient command state into the main screen-state model.

## Data Flow

1. `SettingsViewModel` starts with a default `SettingsScreenState`.
2. `SyncedWorkoutSettingsRepository.observeSessionSettings()` updates synced fields.
3. `LocalAppPreferencesRepository.settings` updates local-only fields.
4. `SettingsRepository.getSettings()` remains only for legacy seeding into DataStore.
5. UI actions continue calling the existing ViewModel intent methods.

## Files Expected To Change

- `app/src/main/java/com/example/workoutapp/ui/settings/SettingsScreenState.kt`
- `app/src/main/java/com/example/workoutapp/ui/settings/SettingsViewModel.kt`
- `Architecture.md`
- `docs/decoupling-plan.md`

## Verification

- Run `./gradlew :app:assembleDebug` after the feature slice.
- Skip test work unless later asked to fix tests.
