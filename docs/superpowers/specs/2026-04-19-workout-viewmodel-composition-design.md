# WorkoutViewModel Composition Design

## Goal

Remove runtime seam construction from `WorkoutViewModel` so it consumes ready-made collaborators instead of acting as its own small composition root.

## Current State

- `WorkoutViewModel` has already shed most business workflow, timer logic, and sensor logic into focused collaborators.
- It still directly constructs several of those collaborators in its own body:
  - `WorkoutCountdownOrchestrator`
  - `WorkoutSessionClock`
  - `WorkoutSensorOrchestrator`
  - `WorkoutSessionCoordinator`
- That means the ViewModel still knows concrete constructor shapes and low-level dependency wiring details.

## Decision

Inject the workout runtime seams through DI-friendly abstractions or factories, rather than constructing them directly inside `WorkoutViewModel`.

Use factories where runtime values such as `viewModelScope` or callbacks are still required.

## Why This Approach

- It finishes a real decoupling layer after extracting the major logic seams.
- It removes constructor knowledge and low-level assembly from the ViewModel.
- It improves testability and substitution without changing behavior.

## Scope

### In Scope

- Inject `WorkoutSessionCoordinator` directly.
- Introduce factory-style collaborators for timer and sensor orchestrators if needed.
- Remove direct orchestrator/coordinator construction from `WorkoutViewModel`.
- Update tests as needed.

### Out Of Scope

- More behavior extraction.
- More repository redesign.
- UI changes.
- Sensor settings/state redesign.

## Target Architecture After This Slice

- `WorkoutViewModel` remains a UI-facing coordination boundary.
- It no longer knows concrete collaborator constructor details.
- DI provides either:
  - direct seam instances, or
  - focused factories for seams that need runtime callbacks/scope

## Likely Shape

### Direct injection

- `WorkoutSessionCoordinator`

### Factory injection

- countdown orchestrator factory
- session clock factory
- sensor orchestrator factory

Factories should stay narrow and only wrap creation details that still need runtime arguments.

## Files Likely To Change

- `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- `app/src/main/java/com/example/workoutapp/di/AppModule.kt` or related DI wiring
- one or more small factory files if needed
- `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`

## Verification

- Run focused `WorkoutViewModelTest`.
- Run `./gradlew :app:testDebugUnitTest`.
- Run `./gradlew :app:assembleDebug`.
