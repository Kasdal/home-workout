# WorkoutViewModel Composition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove runtime seam construction from `WorkoutViewModel` so it consumes injected collaborators and factories instead of acting as its own composition root.

**Architecture:** Inject `WorkoutSessionCoordinator` directly and introduce narrow factory-style seams for runtime collaborators that still need `viewModelScope` and callbacks. Keep `WorkoutViewModel` as the UI-facing coordination boundary, but remove knowledge of concrete constructor details.

**Tech Stack:** Kotlin, Android ViewModel, Hilt, Coroutines, JUnit4

---

### Task 1: Inject The Session Coordinator Directly

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- Possibly modify: `app/src/main/java/com/example/workoutapp/di/AppModule.kt`

- [ ] **Step 1: Add direct injection for `WorkoutSessionCoordinator`**

Move from local construction like:

```kotlin
private val sessionCoordinator = WorkoutSessionCoordinator(
    sessionReducer = sessionReducer,
    sessionCompletionCalculator = sessionCompletionCalculator,
    sessionHistoryRepository = sessionHistoryRepository
)
```

to constructor injection:

```kotlin
class WorkoutViewModel @Inject constructor(
    ...,
    private val sessionCoordinator: WorkoutSessionCoordinator,
    ...
)
```

- [ ] **Step 2: Remove now-redundant constructor knowledge from the ViewModel**

If `WorkoutSessionReducer`, `SessionCompletionCalculator`, or `SessionHistoryRepository` are only retained because the ViewModel used them to construct the coordinator, remove those direct dependencies from the ViewModel constructor when safe.

- [ ] **Step 3: Run focused WorkoutViewModel tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.workoutapp.ui.workout.WorkoutViewModelTest"`
Expected: PASS

- [ ] **Step 4: Build to verify the first composition slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Introduce Factories For Runtime Timer/Sensor Seams

**Files:**
- Create: one or more small factory files for:
  - `WorkoutCountdownOrchestrator`
  - `WorkoutSessionClock`
  - `WorkoutSensorOrchestrator`
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- Modify: DI wiring if needed

- [ ] **Step 1: Add narrow factory-style collaborators**

Factories should wrap only the creation details that still need runtime arguments, e.g.:

```kotlin
interface WorkoutCountdownOrchestratorFactory {
    fun create(scope: CoroutineScope, onTimerSound: () -> Unit): WorkoutCountdownOrchestrator
}
```

Apply the same idea only where needed; do not introduce unnecessary abstraction.

- [ ] **Step 2: Remove direct seam construction from `WorkoutViewModel`**

Replace local construction such as:

```kotlin
private val countdownOrchestrator = WorkoutCountdownOrchestrator(...)
private val sessionClock = WorkoutSessionClock(viewModelScope)
private val sensorOrchestrator = WorkoutSensorOrchestrator(...)
```

with factory calls.

- [ ] **Step 3: Keep runtime wiring behavior unchanged**

The ViewModel may still supply callbacks and `viewModelScope`; it just should not know concrete constructor details anymore.

- [ ] **Step 4: Run focused WorkoutViewModel tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.workoutapp.ui.workout.WorkoutViewModelTest"`
Expected: PASS

- [ ] **Step 5: Build to verify the second composition slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Align Tests With The New Composition Boundary

**Files:**
- Modify: `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`

- [ ] **Step 1: Update test setup to inject the new coordinator/factory seams explicitly**

Tests should no longer rely on the ViewModel constructing runtime collaborators internally.

- [ ] **Step 2: Keep tests focused on boundary behavior, not construction details**

The test setup should reflect the new DI/factory seams, but assertions should remain about public ViewModel state and behavior.

- [ ] **Step 3: Run focused WorkoutViewModel tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.workoutapp.ui.workout.WorkoutViewModelTest"`
Expected: PASS

### Task 4: Run Full Unit Tests And Build

**Files:**
- No additional file targets unless a scoped regression fix is required

- [ ] **Step 1: Run the full JVM unit test task**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: Fix only scoped regressions caused by the composition cleanup**

If unrelated tests fail, stop and report them rather than broadening scope silently.

- [ ] **Step 3: Run the debug build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`
