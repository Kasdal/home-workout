# Workout Session Orchestration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove session and timer job orchestration from `WorkoutViewModel` while preserving current workout session behavior.

**Architecture:** Introduce a focused orchestration helper that owns the coroutine/job mechanics for the session elapsed timer and the countdown timer. Keep `WorkoutViewModel` as the UI-facing state holder that delegates timing mechanics while retaining repository integration, reducer usage, completion calculation, and sensor orchestration.

**Tech Stack:** Kotlin, Android ViewModel, Coroutines, StateFlow, JUnit4, Coroutines Test

---

### Task 1: Extract Countdown Timer Orchestration

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/domain/session/WorkoutCountdownOrchestrator.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- Possibly create: `app/src/test/java/com/example/workoutapp/domain/session/WorkoutCountdownOrchestratorTest.kt`

- [ ] **Step 1: Add a focused countdown orchestrator**

It should own the rest/exercise-switch timer mechanics only:

```kotlin
interface WorkoutCountdownOrchestrator {
    fun start(seconds: Int)
    fun pause()
    fun resume()
    fun stop()
    val secondsRemaining: StateFlow<Int>
    val isRunning: StateFlow<Boolean>
    val isPaused: StateFlow<Boolean>
}
```

Adapt the exact shape to repo style, but keep it narrow and timer-only.

- [ ] **Step 2: Move `timerJob` and `startTimerJob()` behavior out of `WorkoutViewModel`**

`WorkoutViewModel` should delegate `startTimer`, `pauseTimer`, `resumeTimer`, and `stopTimer` to the orchestrator instead of running the job loop itself.

- [ ] **Step 3: Add focused tests for countdown behavior**

Cover at least:
- starting a countdown
- pausing/resuming
- completion state transition

- [ ] **Step 4: Run focused countdown tests if added**

Run the exact focused test command for the new file if one is created.
Expected: PASS

- [ ] **Step 5: Build to verify the first extraction slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Extract Session Elapsed Timer Orchestration

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/domain/session/WorkoutSessionClock.kt`
- Modify: `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
- Possibly create: `app/src/test/java/com/example/workoutapp/domain/session/WorkoutSessionClockTest.kt`

- [ ] **Step 1: Add a focused session clock helper**

It should own only elapsed-time ticking for an active session, for example:

```kotlin
interface WorkoutSessionClock {
    fun start()
    fun pause()
    fun resume()
    fun stop()
    val elapsedSeconds: StateFlow<Int>
}
```

- [ ] **Step 2: Move `sessionTimerJob` mechanics out of `WorkoutViewModel`**

`startSession`, `pauseSession`, `resumeSession`, and `completeSession` should stop managing the while-loop job directly.

- [ ] **Step 3: Add focused tests for elapsed-time behavior**

Cover at least:
- start from zero
- tick while running
- stop advancing while paused
- reset on stop/new start if that is current behavior

- [ ] **Step 4: Run focused clock tests if added**

Run the exact focused test command for the new file if one is created.
Expected: PASS

- [ ] **Step 5: Build to verify the second extraction slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Update WorkoutViewModel Tests For The New Seams

**Files:**
- Modify: `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`

- [ ] **Step 1: Replace direct assumptions about internal job loops with seam-driven assertions**

Adjust the existing ViewModel tests so they verify behavior through the new orchestrator/clock abstractions rather than relying on the old internal coroutine-loop implementation details.

- [ ] **Step 2: Keep ViewModel tests focused on orchestration outcomes**

The ViewModel tests should still verify:
- session starts/stops update UI state correctly
- timer commands produce the expected observable state
- complete/undo set behavior still triggers timer start decisions correctly

- [ ] **Step 3: Run focused WorkoutViewModel tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.workoutapp.ui.workout.WorkoutViewModelTest"`
Expected: PASS

### Task 4: Run Full Unit Tests And Build

**Files:**
- No additional file targets unless a scoped regression fix is required

- [ ] **Step 1: Run the full JVM unit test task**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: Fix only scoped regressions caused by the orchestration extraction**

If unrelated tests fail, stop and report them rather than broadening scope silently.

- [ ] **Step 3: Run the debug build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`
