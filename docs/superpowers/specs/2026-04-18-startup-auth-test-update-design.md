# Startup/Auth Test Update Design

## Goal

Update stale JVM coverage so the migration-aware startup flow is tested against the current `AppEntryState` contract.

## Current State

- `AppLaunchCoordinatorTest` still targets the old `startDestination()` API.
- `MainViewModelTest` still mocks `AppLaunchCoordinator.startDestination()`.
- The implemented startup flow now uses:
  - `AppEntryState.AuthRequired`
  - `AppEntryState.MigrationInProgress`
  - `AppEntryState.Ready(startDestination)`
- The current migration-aware edge cases are only validated by code review and `assembleDebug`, not focused JVM tests.

## Decision

Update the coordinator and MainViewModel JVM tests to target the new `appEntryState()` contract.

Allow minimal production-code testability seams only if they are clearly needed to make the new coordinator behavior easy to test in JVM tests.

## Why This Approach

- It closes the most obvious testing gap introduced by the startup decoupling work.
- It keeps production changes tightly constrained to testability needs.
- It gives confidence around the highest-risk startup state transitions without broadening into UI tests.

## Scope

### In Scope

- Rewrite `AppLaunchCoordinatorTest` for migration-aware `AppEntryState` behavior.
- Update `MainViewModelTest` to mock and assert `appEntryState()`.
- Add only minimal production seams if the current coordinator dependencies are awkward for JVM testing.
- Run focused unit tests and the full unit-test task.
- Build with `:app:assembleDebug` after the test slice.

### Out Of Scope

- Compose/UI tests.
- Changing startup runtime behavior.
- Fixing unrelated test failures outside this slice unless they block the requested verification.

## Target Test Coverage

### AppLaunchCoordinator

- signed out -> `AuthRequired`
- signed in + migration incomplete/missing -> `MigrationInProgress`
- signed in + migration complete + metrics present -> `Ready("workout")`
- signed in + migration complete + no metrics -> `Ready("onboarding")`
- preserve any intentional transient-ready fallback behavior if that logic remains in the implementation

### MainViewModel

- exposes the coordinator `appEntryState`
- keeps theme observation intact
- keeps legacy theme seeding behavior intact

## Possible Minimal Production Seam

If mocking `FirestoreRepository` directly makes the coordinator tests brittle or overly concrete, add a narrow dependency seam for migration-meta observation rather than reshaping broader startup behavior.

If the tests stay straightforward with current dependencies, do not add the seam.

## Files Likely To Change

- `app/src/test/java/com/example/workoutapp/domain/startup/AppLaunchCoordinatorTest.kt`
- `app/src/test/java/com/example/workoutapp/MainViewModelTest.kt`
- possibly one small production file only if needed for testability

## Verification

- Run focused updated JVM tests first.
- Run `./gradlew :app:testDebugUnitTest`.
- Run `./gradlew :app:assembleDebug`.
