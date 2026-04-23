# Workout Session Policy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the remaining workout-session policy and completion persistence workflow from `WorkoutViewModel` while preserving current runtime behavior.

**Architecture:** Introduce a focused workout-session coordinator/use-case that owns progress policy application and session completion persistence flow. Keep `WorkoutViewModel` as the UI-facing state holder that delegates workflow policy and applies returned result/update payloads.

**Tech Stack:** Kotlin, Android ViewModel, Coroutines, JUnit4, Coroutines Test

---

### Task 1: Extract Set Progression And Undo Policy

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/domain/session/WorkoutSessionCoordinator.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- Possibly create: `app/src/test/java/com/example/workoutapp/domain/session/WorkoutSessionCoordinatorTest.kt`

- [ ] **Step 1: Add a focused session coordinator for progress policy**

The coordinator should own the imperative workflow around reducer output, for example:

```kotlin
data class WorkoutProgressResult(
    val completedSets: Map<Int, Int>,
    val activeExerciseSelection: ActiveExerciseSelection,
    val timerRequest: PostSetTimerRequest
)

class WorkoutSessionCoordinator(
    private val reducer: WorkoutSessionReducer,
    private val completionCalculator: SessionCompletionCalculator,
    private val sessionHistoryRepository: SessionHistoryRepository,
    private val profileRepository: ProfileRepository
) {
    fun completeNextSet(...): WorkoutProgressResult
    fun undoSet(...): WorkoutProgressResult
}
```

Adapt the exact shape to repo style, but keep it focused on workflow policy rather than UI state.

- [ ] **Step 2: Move `completeNextSetInternal` and `undoSet` policy flow out of `WorkoutViewModel`**

`WorkoutViewModel` should stop directly coordinating reducer calls plus result application logic. It should delegate to the coordinator and then apply the returned result.

- [ ] **Step 3: Add focused tests for progression and undo policy**

Cover at least:
- normal set completion
- completed-set cap behavior
- undo behavior
- post-set timer request selection preserved

- [ ] **Step 4: Run focused coordinator tests**

Run the exact focused test command for the new coordinator test file.
Expected: PASS

- [ ] **Step 5: Build to verify the first extraction slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Extract Session Completion Persistence Flow

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/domain/session/WorkoutSessionCoordinator.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- Possibly modify: `app/src/test/java/com/example/workoutapp/domain/session/WorkoutSessionCoordinatorTest.kt`

- [ ] **Step 1: Add a completion result type for persistence workflow**

The coordinator should own building and persisting the completed session flow, returning a result object such as:

```kotlin
data class WorkoutCompletionResult(
    val completedSession: WorkoutSession,
    val resetCompletedSets: Map<Int, Int>,
    val nextActiveExerciseSelection: ActiveExerciseSelection
)
```

Adapt the exact shape as needed, but keep the ViewModel-facing result explicit.

- [ ] **Step 2: Move most of `completeSession` workflow out of `WorkoutViewModel`**

The coordinator should own:
- reading current exercises/metrics inputs passed in from the ViewModel
- calling `SessionCompletionCalculator`
- saving the session
- saving session exercises
- returning a result payload

Keep the ViewModel responsible for UI-facing reset application and playing the celebration sound.

- [ ] **Step 3: Add focused tests for completion/persistence behavior**

Cover at least:
- successful completion persists session and exercise rows
- returned result preserves the calculated session data
- empty exercise rows do not trigger `saveSessionExercises`

- [ ] **Step 4: Run focused coordinator tests**

Run the exact focused test command for the coordinator test file.
Expected: PASS

- [ ] **Step 5: Build to verify the second extraction slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Update WorkoutViewModel Tests To The New Policy Seam

**Files:**
- Modify: `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`

- [ ] **Step 1: Replace direct policy-flow assumptions with seam-driven assertions**

Keep the ViewModel tests focused on observable boundary behavior rather than internal policy mechanics.

- [ ] **Step 2: Preserve coverage of user-visible outcomes**

The ViewModel tests should still verify:
- set completion updates visible state correctly
- undo updates visible state correctly
- completion resets public session state correctly
- timer requests still manifest through the public timer state

- [ ] **Step 3: Run focused WorkoutViewModel tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.workoutapp.ui.workout.WorkoutViewModelTest"`
Expected: PASS

### Task 4: Run Full Unit Tests And Build

**Files:**
- No additional file targets unless a scoped regression fix is required

- [ ] **Step 1: Run the full JVM unit test task**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: Fix only scoped regressions caused by the session-policy extraction**

If unrelated tests fail, stop and report them rather than broadening scope silently.

- [ ] **Step 3: Run the debug build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`
