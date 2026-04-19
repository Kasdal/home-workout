# App Entry Coordinator Design

## Goal

Replace the current split startup/auth routing logic with a single app-entry state contract that simplifies `MainActivity` while preserving the existing auth and migration gate behavior.

## Current State

- `MainActivity` owns three different startup decisions:
  - splash visibility via `showSplash`
  - auth-gate unlock via `appUnlocked`
  - post-auth destination via `startDestination != null`
- `MainViewModel` exposes only a nullable `startDestination` plus theme state.
- `AppLaunchCoordinator` currently decides only `workout` vs `onboarding` after a signed-in user exists.
- `AuthGateScreen` advances the app by calling `onReady()` after auth and migration complete.
- `AuthViewModel` owns sign-in and migration state, including retry/import/export backup behavior.
- Migration readiness is not derived from auth alone. It currently becomes visible only inside `AuthViewModel` after `MigrationOrchestrator.migrateIfNeeded(uid)` succeeds.
- Firestore already persists migration completion under `users/{uid}/meta/migration`, but there is no runtime observation path for that state.

## Decision

Introduce a UI-facing app-entry state model and make `AppLaunchCoordinator` own app-entry routing after splash, including migration readiness.

The new contract should explicitly represent app-entry phases such as:

- `AuthRequired`
- `MigrationInProgress`
- `MigrationFailed` is not needed in the coordinator for this slice because migration error UI stays in `AuthGateScreen`
- `Ready(startDestination)`

Splash remains a UI concern in `MainActivity` for this slice to avoid broadening scope.

## Why This Approach

- It removes the current split ownership across `MainActivity`, `MainViewModel`, `AuthGateScreen`, and `AppLaunchCoordinator`.
- It simplifies startup rendering without regressing the existing migration gate.
- It preserves the existing high-risk sign-in and migration UI behavior inside `AuthViewModel` and `AuthGateScreen`.
- It uses a real persisted readiness source instead of guessing from auth state alone.
- It prepares a later step where migration/bootstrap responsibilities can be further isolated without forcing that change now.

## Scope

### In Scope

- Add a single app-entry state model for post-splash routing.
- Add a migration-readiness observation path from Firestore migration metadata.
- Expand `AppLaunchCoordinator` to emit that app-entry state.
- Update `MainViewModel` to expose app-entry state instead of only nullable start destination.
- Simplify `MainActivity` so it renders from the app-entry state instead of maintaining a separate app unlock boolean.
- Keep `AuthGateScreen` in place, but let app progression follow app-entry state instead of a separate local unlock flag.
- Update docs.

### Out Of Scope

- Moving migration logic out of `AuthViewModel`.
- Changing sign-in behavior.
- Changing migration retry/import/export behavior.
- Removing splash screen behavior.
- Replacing the migration UI with a coordinator-owned error state.
- Test fixes unless explicitly requested later.

## Target Architecture After This Slice

- `MainActivity` owns only splash timing and rendering.
- `MainViewModel` exposes a single post-splash app-entry state plus theme state.
- `AppLaunchCoordinator` maps signed-out vs signed-in-plus-migration-ready profile state into a UI-facing contract.
- `AuthGateScreen` remains responsible for sign-in and migration UI, and is shown whenever the app-entry state is not yet ready.

## Proposed State Shape

A small sealed UI model under `domain/startup/` or another startup-focused package:

- `AuthRequired`
- `MigrationInProgress`
- `Ready(startDestination: String)`

This slice does not need a splash value in the coordinator because splash is still locally timed in `MainActivity`.

## Data Flow

1. `MainActivity` shows splash while the splash timer runs.
2. After splash, it reads the app-entry state from `MainViewModel`.
3. If the state is `AuthRequired` or `MigrationInProgress`, it renders `AuthGateScreen`.
4. `AuthViewModel` still performs sign-in and runs migration.
5. Firestore migration metadata becomes the readiness source once migration succeeds.
6. `AppLaunchCoordinator` observes auth state plus migration metadata plus profile state.
7. Only after migration is complete does it emit `Ready(startDestination)` and allow the nav host to render.

## Behavioral Constraints

- Do not change when migration runs.
- Do not change the retry/import/export backup UI behavior.
- Do not change the onboarding-vs-workout routing logic.
- Do not add new navigation destinations.
- Do not allow auth success alone to bypass the migration gate.

## Files Expected To Change

- `app/src/main/java/com/example/workoutapp/data/remote/FirestoreRepository.kt`
- `app/src/main/java/com/example/workoutapp/domain/startup/AppLaunchCoordinator.kt`
- `app/src/main/java/com/example/workoutapp/MainViewModel.kt`
- `app/src/main/java/com/example/workoutapp/MainActivity.kt`
- `Architecture.md`
- `docs/decoupling-plan.md`

## Verification

- Run `./gradlew :app:assembleDebug` after the feature slice.
- Skip test work unless later asked to fix tests.
