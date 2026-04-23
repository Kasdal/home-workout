# Room Removal Backup Gating Design

## Goal

Ensure the Room-free startup flow cannot bypass the backup-import decision when a signed-in user has no remote data.

## Current State

- `MigrationOrchestrator.migrateIfNeeded(uid)` can now return `MigrationBootstrapResult.NEEDS_BACKUP_IMPORT` when there is no remote data.
- `AuthViewModel` maps that into `AuthUiState.awaitingBackupImport = true`.
- `AuthGateScreen` renders import/continue actions for that state.
- But `AppLaunchCoordinator` still routes only on auth + migration metadata, so app entry can become `Ready(...)` before the user handles the backup-import prompt.

## Decision

Introduce an explicit app-entry gate for the “awaiting backup import” state.

The app should remain on the auth gate until the user either:

- imports a backup successfully, or
- explicitly chooses to continue without importing.

## Why This Approach

- It matches the intended user flow after Room removal.
- It prevents the app from navigating into onboarding/workout before the user resolves the backup-import decision.
- It keeps the fix focused on startup/auth gating rather than redesigning backup behavior.

## Scope

### In Scope

- Add an app-entry gating signal for “awaiting backup import”.
- Keep `AuthGateScreen` visible until the user resolves that state.
- Preserve the new import/continue UI behavior.
- Update tests around the startup/auth path as needed.

### Out Of Scope

- Redesigning backup format or migration behavior.
- Broader auth UI changes.
- More startup/app-entry refactoring beyond the new gate.

## Target Architecture After This Slice

- `AuthViewModel` remains the source of the backup-import prompt state.
- `AppLaunchCoordinator` (or another app-entry seam) incorporates that state into readiness decisions.
- `MainActivity` only navigates into the app when both conditions are true:
  - migration/bootstrap is ready
  - no backup-import decision is pending

## Likely Shape

The smallest likely fix is one of:

- extend the app-entry state model to include an auth-gate-required variant that covers backup-import pending, or
- combine the `AuthViewModel` pending-import signal with the current app-entry routing before `MainActivity` decides to render the nav host

The implementation should prefer the smaller change that keeps responsibilities understandable.

## Verification

- Run focused tests for the updated startup/auth path.
- Run `./gradlew :app:testDebugUnitTest`.
- Run `./gradlew :app:assembleDebug`.
