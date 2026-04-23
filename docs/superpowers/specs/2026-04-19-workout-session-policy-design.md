# Workout Session Policy Design

## Goal

Remove the remaining workout-session policy and completion persistence flow from `WorkoutViewModel` while preserving current runtime behavior.

## Current State

- `WorkoutViewModel` no longer owns timer or sensor coroutine mechanics.
- It still owns most remaining workout-session workflow policy:
  - `completeNextSet` / `completeNextSetInternal`
  - `undoSet`
  - active exercise selection application
  - post-set timer decisions application
  - `completeSession` persistence flow and state reset sequencing
- Pure calculation and progression logic is already partially extracted:
  - `WorkoutSessionReducer`
  - `SessionCompletionCalculator`

## Decision

Introduce a focused workout-session coordinator/use-case seam that owns progress policy application and session completion persistence workflow.

Keep `WorkoutViewModel` as the UI-facing state holder and seam-composer.

## Why This Approach

- It targets the biggest remaining “do everything” hotspot in `WorkoutViewModel`.
- It builds naturally on the already-extracted reducer, completion calculator, timer seams, and sensor seam.
- It should make completion failure-path behavior easier to test directly.

## Scope

### In Scope

- Extract progress policy flow for:
  - `completeNextSet`
  - `undoSet`
  - timer requests
  - active exercise selection updates
- Extract completion persistence flow for:
  - session completion calculation
  - saving session + exercises
  - returning reset/update payloads
- Update tests for the new seam.

### Out Of Scope

- More sensor refactoring.
- More timer refactoring.
- Repository contract redesign beyond what the extracted coordinator needs.
- Navigation/UI changes.

## Target Architecture After This Slice

- `WorkoutViewModel` keeps UI-facing `StateFlow`s and delegates workflow policy.
- A focused session coordinator/use-case owns:
  - applying reducer output to workflow decisions
  - orchestrating completion calculation + persistence
  - returning explicit update/result payloads back to the ViewModel
- `WorkoutSessionReducer` and `SessionCompletionCalculator` remain focused collaborators under that coordinator.

## Likely Helper Responsibilities

Examples:

- compute next progress update for a completed set
- compute undo update
- translate reducer timer requests into ViewModel-facing instructions
- build session completion payloads
- persist session history rows
- return a result object that tells the ViewModel what state to reset and what completed session to surface

## Boundaries

- Keep `WorkoutViewModel` responsible for:
  - exposing observable UI state
  - applying update/result payloads
  - playing celebration sound
  - coordinating with extracted timer/sensor seams
- Keep `WorkoutSessionReducer` focused on progression rules.
- Keep `SessionCompletionCalculator` focused on completion data assembly.

## Files Likely To Change

- `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- one new coordinator/use-case file under `domain/session/` or `ui/workout/`
- `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`
- one new focused test file for the extracted coordinator/use-case

## Verification

- Run focused unit tests for the extracted coordinator/use-case.
- Run `./gradlew :app:testDebugUnitTest`.
- Run `./gradlew :app:assembleDebug`.
