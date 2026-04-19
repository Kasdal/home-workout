# Decoupling Plan

## Current assessment

- Firestore is the real runtime data store.
- Room is no longer used for normal repository reads or writes.
- Shared app models are no longer Room entities.
- Room still exists only as a legacy read adapter for migration/export fallback.
- The biggest coupling hotspots are the shared `Settings` blob, `WorkoutViewModel`, and the broad repository surface.

## Verified Room status

- Active app code no longer depends on `WorkoutRepository`.
- `CloudWorkoutRepository` remains the single Firestore-backed runtime implementation, exposed through focused repository interfaces.
- Room is still built and injected in `app/src/main/java/com/example/workoutapp/di/AppModule.kt`.
- The only active runtime Room dependency is `MigrationOrchestrator`, now reading through `LegacyMigrationDataSource`, used from `AuthViewModel` during sign-in or manual legacy backup export.
- `WorkoutRepositoryImpl` has been removed.
- `WorkoutDaoTest` has been removed.
- Room-specific entities now live under `app/src/main/java/com/example/workoutapp/data/local/room/entity/`.

## Room removal decision

- Do not remove Room immediately.
- Remove Room only after a manual import fallback exists.

Why:

- Legacy users may still have local-only data in Room.
- Failed migration users still rely on the retry path that reads Room.
- The auth UI explicitly promises local data remains safe on device after migration failure.

## Target architecture

- Firestore is the only persisted workout/profile/session source of truth.
- Firestore local persistence handles cloud cache/offline behavior.
- DataStore handles device-local app preferences.
- Domain/session logic moves into plain Kotlin classes.
- ViewModels orchestrate; Compose screens render.
- Most tests become plain JVM tests.

## Phased plan

### Phase 0: Migration safety

- Audit whether unmigrated Room users still need support.
- Add a manual import fallback before removing Room.

### Phase 1: Settings split

- Replace the current shared `Settings` blob with:
  - `LocalAppSettings` in DataStore
  - `SyncedWorkoutSettings` in Firestore
  - UI-only `SettingsScreenState`

Recommended ownership:

- DataStore:
  - `themeMode`
  - `soundsEnabled`
  - `soundVolume`
  - `timerSoundType`
  - `celebrationSoundType`
  - `tutorialCompleted`
  - `tutorialVersion`
  - `sensorEnabled`
  - `sensorIpAddress`
- Firestore:
  - `restTimerDuration`
  - `exerciseSwitchDuration`
  - `undoLastSetEnabled`

Smallest first slice:

- Move theme only to DataStore.

Status:

- Done, and expanded.
- Local-only settings now live in `LocalAppPreferencesRepository` backed by DataStore.
- Synced workout-session settings are isolated in `SyncedWorkoutSettingsRepository`.

### Phase 2: Workout session engine

- Extract pure Kotlin session logic from `WorkoutViewModel`.

First target classes:

- `WorkoutSessionState`
- `WorkoutSessionReducer`
- `PostSetTimerRequest`

First moved logic:

- complete next set
- undo set
- active exercise selection
- post-set timer decision

Status:

- Done for the first reducer/calculator seams.
- `WorkoutSessionReducer` and `SessionCompletionCalculator` are live and covered by focused unit tests.

### Phase 3: Startup/auth coordinator

- Consolidate auth, migration, onboarding, and routing into a single launch coordinator.

Status:

- Partially done.
- Launch routing cleanup is complete.
- Splash remains a local UI timer in `MainActivity`.
- Post-splash app entry now renders from a single `appEntryState` exposed by `MainViewModel`.
- `AppLaunchCoordinator` now owns auth-required vs migration-in-progress vs ready routing, and derives readiness from Firestore migration metadata.
- `AuthViewModel` still owns sign-in and migration execution, so startup/auth behavior is not fully consolidated into a single coordinator yet.

### Phase 4: Retire Room runtime

- After manual import fallback is available:
  - delete `MigrationOrchestrator`
  - delete `WorkoutDatabase`
  - delete `WorkoutDao`
  - remove Room providers from `AppModule`
  - delete `WorkoutRepositoryImpl`
  - delete Room DAO tests
  - remove Room dependencies from Gradle

Status:

- Not complete yet.
- Manual JSON backup export/import fallback exists.
- Remaining blocker: the fallback still reads from the legacy Room adapter in the same release line, so full Room removal should happen only after that fallback is no longer needed.

### Phase 5: Neutralize shared models

- Move shared data models out of `data.local.entity` into a storage-neutral package.
- Remove Room annotations from those models.

Status:

- Partially done.
- Room annotations have been removed from shared models.
- Room-specific copies now exist under `data/local/room/entity/`.

### Phase 6: Split repository surface

- Break `WorkoutRepository` and `FirestoreRepository` into smaller interfaces:
  - profile
  - exercise
  - session
  - settings
  - rest day
  - auth/session

Status:

- Complete for active app code.
- Runtime consumers now depend on focused repository interfaces instead of `WorkoutRepository`.
- `CloudWorkoutRepository` still implements multiple focused interfaces, which is acceptable for now.
- Future cleanup, if needed, would split the concrete Firestore implementation itself rather than reintroduce a broad contract.

### Phase 7: Reshape tests

- Move extracted logic into plain JVM tests.
- Keep only a few Android/Compose integration tests.

Status:

- Started.
- Focused JVM tests now exist for:
  - `WorkoutSessionReducer`
  - `SessionCompletionCalculator`
- Earlier focused tests existed for the pre-migration `AppLaunchCoordinator` seam.
- The current migration-aware `appEntryState` flow is not yet covered by updated coordinator tests.

## Multi-agent execution plan

### Agent 1: Migration fallback

- Design and validate the manual import fallback required to remove Room safely.

### Agent 2: Settings split

- Move theme first, then split local-vs-synced settings ownership.

### Agent 3: Session engine

- Extract session rules from `WorkoutViewModel` into pure Kotlin logic.

### Agent 4: Startup/auth coordination

- Simplify launch routing after migration behavior is isolated.

### Agent 5: Repository/test-speed cleanup

- Split repository interfaces and move more logic into JVM-testable code.

## Recommended execution order

1. migration safety and manual import fallback design
2. theme to DataStore
3. session reducer extraction
4. startup/auth coordinator
5. Room removal
6. model neutralization
7. test-speed improvements
