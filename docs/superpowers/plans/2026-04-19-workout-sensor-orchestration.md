# Workout Sensor Orchestration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove sensor polling and sensor-driven set-completion orchestration from `WorkoutViewModel` while preserving the current sensor UI state and behavior.

**Architecture:** Introduce a focused sensor orchestration helper that owns polling `SensorRepository`, sensor status translation, rep-edge detection, auto-complete events, and delayed counter reset behavior. Keep `WorkoutViewModel` as the UI-facing state holder that still owns sensor settings values and sensor UI state exposure.

**Tech Stack:** Kotlin, Android ViewModel, Coroutines, StateFlow, JUnit4, Coroutines Test

---

### Task 1: Extract Sensor Polling And Status Translation

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutSensorOrchestrator.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- Possibly create: `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutSensorOrchestratorTest.kt`

- [ ] **Step 1: Add a focused sensor orchestration helper**

It should own runtime sensor polling mechanics only, for example:

```kotlin
data class WorkoutSensorSnapshot(
    val connected: Boolean = false,
    val reps: Int = 0,
    val state: String = "REST",
    val distance: Int = 0
)

interface WorkoutSensorOrchestrator {
    val sensorSnapshot: StateFlow<WorkoutSensorSnapshot>
    fun start(ipAddress: String)
    fun stop()
}
```

Adapt the shape to repo style, but keep it narrow and runtime-only.

- [ ] **Step 2: Move polling lifecycle and raw state updates out of `WorkoutViewModel`**

`startSensorPolling()` / `stopSensorPolling()` and direct `_sensorConnected`, `_sensorReps`, `_sensorState`, `_sensorDistance` mutation from repository polling should move behind the helper.

- [ ] **Step 3: Add focused tests for sensor polling/status translation**

Cover at least:
- start emits sensor snapshots from repository data
- stop resets sensor snapshot state
- null/error samples map to disconnected state

- [ ] **Step 4: Run focused sensor orchestrator tests if added**

Run the exact focused test command for the new file if one is created.
Expected: PASS

- [ ] **Step 5: Build to verify the first extraction slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Extract Rep-Edge Detection And Auto-Complete Events

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutSensorOrchestrator.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- Possibly modify: `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutSensorOrchestratorTest.kt`

- [ ] **Step 1: Move rep-edge detection out of `WorkoutViewModel`**

The helper should own `lastSensorReps`-style tracking and emit an event/callback when a sensor-driven set completion should occur.

- [ ] **Step 2: Keep set-completion rules unchanged**

Preserve current behavior:
- only sensor-enabled exercises
- not hold exercises
- only when rep count crosses the target threshold

`WorkoutViewModel` may still decide how to apply the event to `completeNextSet(exerciseId)` if that keeps the boundary smaller.

- [ ] **Step 3: Add focused tests for rep-edge and auto-complete triggering**

Cover at least:
- rep increases trigger an event once
- repeated identical rep counts do not retrigger
- disconnected/null samples do not trigger completion

- [ ] **Step 4: Run focused sensor orchestrator tests**

Run the exact focused test command for the sensor orchestrator test file.
Expected: PASS

### Task 3: Extract Delayed Counter Reset Follow-Up

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutSensorOrchestrator.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- Possibly modify: `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutSensorOrchestratorTest.kt`

- [ ] **Step 1: Move delayed reset-counter behavior behind the sensor helper**

The helper should own the delayed `sensorRepository.resetCounter(sensorIpAddress)` follow-up and local rep reset behavior after a successful sensor-driven completion event.

- [ ] **Step 2: Keep sensor UI state exposure unchanged**

`WorkoutViewModel` should still expose:
- `sensorReps`
- `sensorState`
- `sensorDistance`
- `sensorConnected`

but these values should now come from the helper output rather than direct polling logic.

- [ ] **Step 3: Add focused reset-path tests**

Cover at least:
- successful auto-complete schedules reset
- reset clears local rep display state
- reset is not triggered when no completion event happened

- [ ] **Step 4: Run focused sensor orchestrator tests**

Run the exact focused test command for the sensor orchestrator test file.
Expected: PASS

- [ ] **Step 5: Build to verify the complete extraction slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 4: Update WorkoutViewModel Tests And Run Full Verification

**Files:**
- Modify: `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`

- [ ] **Step 1: Align ViewModel tests with the new sensor seam**

Update `WorkoutViewModelTest` so it verifies public sensor behavior through observable state and completion outcomes rather than direct assumptions about polling internals.

- [ ] **Step 2: Keep ViewModel tests focused on boundary behavior**

The ViewModel tests should still verify:
- sensor state is surfaced correctly
- session start/stop gates sensor orchestration correctly
- sensor-driven completion still affects workout progress as expected

- [ ] **Step 3: Run focused WorkoutViewModel tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.workoutapp.ui.workout.WorkoutViewModelTest"`
Expected: PASS

- [ ] **Step 4: Run full JVM unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Run the debug build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`
