# Workout Sensor Orchestration Design

## Goal

Remove sensor polling and sensor-driven set-completion orchestration from `WorkoutViewModel` while preserving the current sensor UI state and behavior.

## Current State

- `WorkoutViewModel` still owns sensor runtime orchestration:
  - polling lifecycle job
  - raw sensor state updates
  - rep-edge detection via `lastSensorReps`
  - auto-complete trigger logic
  - delayed counter reset flow
- Sensor settings observation already happens separately through local preferences.
- UI-facing sensor state flows already exist in the ViewModel:
  - `sensorReps`
  - `sensorState`
  - `sensorDistance`
  - `sensorConnected`

## Decision

Introduce a focused sensor orchestration seam, such as `WorkoutSensorOrchestrator`, that owns runtime sensor polling and sensor-driven completion behavior.

Keep sensor settings observation and UI-facing sensor state exposure in `WorkoutViewModel` for now.

## Why This Approach

- It targets the largest remaining non-trivial behavior cluster in `WorkoutViewModel`.
- It isolates external device timing/polling concerns from the ViewModel.
- It avoids broadening into sensor settings/state ownership changes in the same slice.

## Scope

### In Scope

- Extract sensor polling lifecycle mechanics from `WorkoutViewModel`.
- Extract rep-edge detection and auto-complete trigger logic.
- Extract delayed sensor reset-counter behavior.
- Update tests for the new sensor orchestration seam.

### Out Of Scope

- Changing sensor settings storage or observation.
- Changing UI state shape for sensor data.
- Broader workout/session/timer refactoring.
- Network/repository redesign beyond what the orchestration seam needs.

## Target Architecture After This Slice

- `WorkoutViewModel` still owns sensor settings values and UI-facing sensor state flows.
- A focused sensor orchestration helper owns:
  - polling `SensorRepository`
  - translating raw samples into sensor status updates
  - rep-edge detection
  - auto-complete events
  - reset-counter follow-up behavior
- `WorkoutViewModel` delegates sensor runtime behavior to that helper.

## Likely Helper Responsibilities

The helper should be runtime-only and sensor-specific.

Examples:

- start/stop polling
- emit sensor status snapshots
- detect rep count increases
- emit an event when a set should be auto-completed
- perform delayed sensor reset after a successful auto-complete event

## Boundaries

- Keep `SensorRepository` as the low-level polling/reset dependency.
- Keep `WorkoutSessionReducer` and `SessionCompletionCalculator` untouched.
- Keep sensor settings values in `WorkoutViewModel` for now.

## Files Likely To Change

- `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- one new sensor orchestration helper file
- `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`
- possibly one new focused test file for the sensor orchestrator

## Verification

- Run focused unit tests for the extracted sensor orchestration behavior.
- Run `./gradlew :app:testDebugUnitTest`.
- Run `./gradlew :app:assembleDebug`.
