# Workout Session Orchestration Design

## Goal

Remove session and timer job orchestration from `WorkoutViewModel` while preserving current session behavior.

## Current State

- `WorkoutViewModel` still owns two separate coroutine-job systems:
  - session elapsed timer (`sessionTimerJob`)
  - rest/exercise-switch countdown timer (`timerJob`)
- The file already delegates some pure logic to extracted domain classes:
  - `WorkoutSessionReducer`
  - `SessionCompletionCalculator`
- The remaining complexity is mostly in lifecycle orchestration:
  - `startSession`
  - `pauseSession`
  - `resumeSession`
  - `startTimer` / `pauseTimer` / `resumeTimer` / `stopTimer`
  - `completeSession`

## Decision

Introduce a focused session/timer orchestration seam, such as `WorkoutSessionOrchestrator`, that owns the coroutine/job mechanics for session elapsed time and countdown timer behavior.

Keep `WorkoutViewModel` as the UI-facing state holder and repository coordinator.

## Why This Approach

- It attacks the highest remaining runtime complexity in `WorkoutViewModel`.
- It builds on the reducer/calculator seams already extracted.
- It avoids pulling sensor/device behavior into the same refactor.
- It should make timer/session tests less fragile by isolating long-lived coroutine behavior.

## Scope

### In Scope

- Extract session elapsed timer orchestration from `WorkoutViewModel`.
- Extract countdown timer orchestration from `WorkoutViewModel`.
- Keep session lifecycle behavior unchanged.
- Update tests as needed for the new seam.

### Out Of Scope

- Sensor polling and sensor-driven rep completion.
- Broader repository or settings changes.
- UI redesign.
- Navigation changes.

## Target Architecture After This Slice

- `WorkoutViewModel` still owns UI-facing `StateFlow`s and repository integration.
- A focused orchestration helper owns:
  - session timer job behavior
  - countdown timer job behavior
  - pause/resume/stop transitions for those timers
- `WorkoutViewModel` delegates orchestration and applies resulting state updates.

## Likely Helper Responsibilities

The helper should own only runtime timing mechanics, not domain workout rules.

Examples:

- start session elapsed timer
- pause/resume/stop session elapsed timer
- start countdown timer
- pause/resume/stop countdown timer
- emit elapsed-seconds / remaining-seconds / running-state updates

## Boundaries

- Keep `WorkoutSessionReducer` responsible for set progression and active exercise selection.
- Keep `SessionCompletionCalculator` responsible for final session summary construction.
- Keep sensor orchestration in `WorkoutViewModel` for now.

## Files Likely To Change

- `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- one new orchestration helper file under a session/workout package
- `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`
- possibly one new focused test file for the orchestration helper

## Verification

- Run focused unit tests for the extracted orchestration behavior.
- Run `./gradlew :app:testDebugUnitTest` if the scope naturally touches shared test setup.
- Run `./gradlew :app:assembleDebug`.
