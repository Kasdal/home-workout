# Personalized Calorie Estimation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build personalized, explainable MET-based calorie estimation with safe legacy compatibility.

**Architecture:** Keep calorie computation centralized in `CalorieCalculator`, but return a rich `CalorieEstimate` result. Session completion persists the resulting calories plus audit metadata; cloud mappings tolerate missing fields for old sessions. Exercise metadata gains optional calorie categories for better MET selection.

**Tech Stack:** Kotlin, Android, Coroutines, Hilt, Firestore cloud model mapping, JUnit JVM tests, Gradle wrapper on Windows.

---

## File Map

- `app/src/test/java/com/example/workoutapp/domain/session/WorkoutCountdownOrchestratorTest.kt`: baseline test compile fix for renamed timer callbacks.
- `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`: baseline test compile fix for renamed timer factory callbacks.
- `app/src/main/java/com/example/workoutapp/util/CalorieCalculator.kt`: calculator result model, standard and personalized MET logic, category MET selection.
- `app/src/test/java/com/example/workoutapp/util/CalorieCalculatorTest.kt`: deterministic calculator tests.
- `app/src/main/java/com/example/workoutapp/model/WorkoutSession.kt`: persisted calorie audit fields.
- `app/src/main/java/com/example/workoutapp/data/remote/model/CloudModels.kt`: cloud audit and exercise category mapping.
- `app/src/main/java/com/example/workoutapp/domain/session/SessionCompletionCalculator.kt`: maps `CalorieEstimate` to `WorkoutSession`.
- `app/src/test/java/com/example/workoutapp/domain/session/SessionCompletionCalculatorTest.kt`: session audit field tests.
- `app/src/main/java/com/example/workoutapp/model/Exercise.kt`: `CalorieCategory` enum and exercise field.
- `app/src/main/java/com/example/workoutapp/ui/settings/SettingsViewModel.kt`: CSV export fields.
- `app/src/test/java/com/example/workoutapp/ui/settings/SettingsViewModelTest.kt`: CSV export coverage if an existing settings test seam supports it; otherwise add focused pure helper tests only if extraction is minimal.

## Task 0: Restore Baseline Test Compilation

**Files:**
- Modify: `app/src/test/java/com/example/workoutapp/domain/session/WorkoutCountdownOrchestratorTest.kt`
- Modify: `app/src/test/java/com/example/workoutapp/ui/workout/WorkoutViewModelTest.kt`

- [ ] Update `WorkoutCountdownOrchestratorTest` constructor calls from `onTimerSound = ...` to both callbacks:

```kotlin
val beeps = mutableListOf<Unit>()
val completions = mutableListOf<Unit>()
val orchestrator = WorkoutCountdownOrchestrator(
    scope = backgroundScope,
    onCountdownWarning = { beeps += Unit },
    onTimerComplete = { completions += Unit }
)
```

- [ ] Update pause/stop tests to pass no-op callbacks:

```kotlin
val orchestrator = WorkoutCountdownOrchestrator(
    scope = backgroundScope,
    onCountdownWarning = {},
    onTimerComplete = {}
)
```

- [ ] Add an assertion in the countdown completion test:

```kotlin
assertEquals(1, completions.size)
```

- [ ] Update the `WorkoutViewModelTest` factory stub:

```kotlin
every { countdownOrchestratorFactory.create(any(), any(), any()) } answers {
    com.example.workoutapp.domain.session.WorkoutCountdownOrchestrator(
        scope = firstArg(),
        onCountdownWarning = secondArg(),
        onTimerComplete = thirdArg()
    )
}
```

- [ ] Update the runtime seam verification:

```kotlin
verify(exactly = 1) { countdownOrchestratorFactory.create(any(), any(), any()) }
```

- [ ] Run baseline verification:

```powershell
./gradlew.bat --no-daemon :app:testDebugUnitTest --tests "com.example.workoutapp.domain.session.WorkoutCountdownOrchestratorTest" --tests "com.example.workoutapp.ui.workout.WorkoutViewModelTest"
```

Expected: these tests compile and run. If behavior failures remain, fix only callback-related test expectations.

## Task 1: Add Deterministic Calculator Tests and Rich Estimate API

**Files:**
- Modify: `app/src/test/java/com/example/workoutapp/util/CalorieCalculatorTest.kt`
- Modify: `app/src/main/java/com/example/workoutapp/util/CalorieCalculator.kt`

- [ ] Add tests first for `calculateEstimate` covering standard formula, elapsed-time rest behavior, zero-elapsed programmed rest behavior, default profile fallback, unknown intensity fallback, and weighted MET cap.

- [ ] Expected API:

```kotlin
val estimate = CalorieCalculator.calculateEstimate(
    completedSets = mapOf(1 to 1),
    exercises = exercises,
    userMetrics = UserMetrics(weightKg = 80f),
    restSecondsBetweenSets = 30,
    restSecondsBetweenExercises = 60,
    elapsedSeconds = 120,
    intensity = "normal"
)
```

- [ ] Add result model in `CalorieCalculator.kt`:

```kotlin
data class CalorieEstimate(
    val calories: Float,
    val formulaVersion: Int,
    val mode: CalorieEstimateMode,
    val userWeightKg: Float,
    val metCorrectionFactor: Float,
    val activeSeconds: Float,
    val restSeconds: Int,
    val activeCalories: Float,
    val restCalories: Float,
    val intensity: String
)

enum class CalorieEstimateMode {
    STANDARD_MET,
    PERSONALIZED_MET
}
```

- [ ] Preserve existing public behavior:

```kotlin
fun calculateCalories(...): Float {
    return calculateEstimate(...).calories
}
```

- [ ] Run focused calculator tests:

```powershell
./gradlew.bat --no-daemon :app:testDebugUnitTest --tests "com.example.workoutapp.util.CalorieCalculatorTest"
```

Expected: calculator tests pass.

## Task 2: Persist Calorie Audit Fields Through Session and Cloud Models

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/model/WorkoutSession.kt`
- Modify: `app/src/main/java/com/example/workoutapp/data/remote/model/CloudModels.kt`
- Modify: `app/src/main/java/com/example/workoutapp/domain/session/SessionCompletionCalculator.kt`
- Modify: `app/src/test/java/com/example/workoutapp/domain/session/SessionCompletionCalculatorTest.kt`

- [ ] Add failing session completion test asserting audit fields are populated from a normal completed session.

- [ ] Add fields to `WorkoutSession` with legacy-safe defaults:

```kotlin
val calorieFormulaVersion: Int = 1,
val calorieEstimateMode: String = "STANDARD_MET",
val calorieIntensity: String = "normal",
val calorieUserWeightKg: Float = 70f,
val calorieMetCorrectionFactor: Float = 1f,
val calorieActiveSeconds: Float = 0f,
val calorieRestSeconds: Int = 0
```

- [ ] Add matching fields to `CloudWorkoutSession` with the same defaults.

- [ ] Map fields both ways in `WorkoutSession.toCloud()` and `CloudWorkoutSession.toLocal()`.

- [ ] In `SessionCompletionCalculator`, call `calculateEstimate`, set `caloriesBurned = estimate.calories`, and copy audit fields.

- [ ] Run:

```powershell
./gradlew.bat --no-daemon :app:testDebugUnitTest --tests "com.example.workoutapp.domain.session.SessionCompletionCalculatorTest"
```

Expected: session completion tests pass.

## Task 3: Add Personalized Corrected-MET Calculation

**Files:**
- Modify: `app/src/test/java/com/example/workoutapp/util/CalorieCalculatorTest.kt`
- Modify: `app/src/main/java/com/example/workoutapp/util/CalorieCalculator.kt`

- [ ] Add failing tests for complete profile using `PERSONALIZED_MET`, incomplete/invalid profile using `STANDARD_MET`, and correction factor clamping.

- [ ] Implement Harris-Benedict RMR helpers inside `CalorieCalculator`:

```text
Male RMR kcal/day = 66.4730 + 5.0033*heightCm + 13.7516*weightKg - 6.7550*age
Female RMR kcal/day = 655.0955 + 1.8496*heightCm + 9.5634*weightKg - 4.6756*age
```

- [ ] Convert RMR to `ml/kg/min`:

```text
kcal/day / 1440 = kcal/min
kcal/min / 5 = L/min
L/min / weightKg * 1000 = ml/kg/min
```

- [ ] Apply correction:

```kotlin
val correction = (3.5f / estimatedRmrMlKgMin).coerceIn(0.85f, 1.25f)
```

- [ ] Use corrected MET only for recognized `Male` or `Female`; use standard MET for `Other` or invalid values.

- [ ] Run calculator tests.

## Task 4: Add Exercise Calorie Categories

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/model/Exercise.kt`
- Modify: `app/src/main/java/com/example/workoutapp/data/remote/model/CloudModels.kt`
- Modify: `app/src/main/java/com/example/workoutapp/util/CalorieCalculator.kt`
- Modify: `app/src/test/java/com/example/workoutapp/util/CalorieCalculatorTest.kt`

- [ ] Add failing tests proving calorie categories influence MET selection.

- [ ] Add enum and field:

```kotlin
enum class CalorieCategory {
    LIGHT_RESISTANCE,
    MODERATE_RESISTANCE,
    HEAVY_COMPOUND,
    LIGHT_BODYWEIGHT,
    VIGOROUS_BODYWEIGHT,
    ISOMETRIC_HOLD
}
```

```kotlin
val calorieCategory: String? = null
```

- [ ] Add `calorieCategory` to `CloudExercise` and mappings.

- [ ] Category defaults when null:

```text
STANDARD -> MODERATE_RESISTANCE
BODYWEIGHT -> VIGOROUS_BODYWEIGHT
HOLD -> ISOMETRIC_HOLD
```

- [ ] Use category MET baselines while preserving weighted load adjustment for resistance categories.

- [ ] Run calculator tests and any cloud model tests.

## Task 5: Extend CSV Export With Calorie Audit Fields

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/settings/SettingsViewModel.kt`
- Modify or create focused tests if an existing test seam is practical.

- [ ] Update CSV session export header to include formula version, estimate mode, intensity, user weight used, MET correction factor, active seconds, and rest seconds.

- [ ] Append corresponding `WorkoutSession` fields to exported session rows.

- [ ] Run focused tests if available; otherwise run compile verification after implementation.

## Task 6: Final Verification

**Files:**
- No production edits unless verification exposes a defect.

- [ ] Run focused test suite:

```powershell
./gradlew.bat --no-daemon :app:testDebugUnitTest --tests "com.example.workoutapp.util.CalorieCalculatorTest" --tests "com.example.workoutapp.domain.session.SessionCompletionCalculatorTest" --tests "com.example.workoutapp.domain.session.WorkoutCountdownOrchestratorTest" --tests "com.example.workoutapp.ui.workout.WorkoutViewModelTest"
```

- [ ] Run full JVM tests:

```powershell
./gradlew.bat --no-daemon :app:testDebugUnitTest
```

- [ ] Run debug build:

```powershell
./gradlew.bat --no-daemon :app:assembleDebug
```

Expected: focused tests pass, full JVM tests pass, debug build succeeds. If unrelated failures remain, document exact failures with file paths and command output.
