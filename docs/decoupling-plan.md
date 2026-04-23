# Decoupling Plan

## Current assessment

- Firestore is the persisted runtime data store.
- Room has been removed from the codebase.
- Shared app models are plain Kotlin data classes, not Room entities.
- Legacy recovery is now limited to manual backup import into Firestore.
- The old Room-backed migration fallback path is retired.
- The biggest remaining coupling hotspots are the legacy `Settings` blob boundaries that still exist for backup import and compatibility-only flows, plus remaining `WorkoutViewModel` cleanup.

## Verified Room status

- Active app code no longer depends on `WorkoutRepository`.
- `CloudWorkoutRepository` remains the single Firestore-backed runtime implementation, exposed through focused repository interfaces.
- Room adapters, Room entities, DAO/database wiring, and legacy Room migration seams have been removed.
- `MigrationOrchestrator` now completes empty-payload migration metadata for first-run sign-in and imports legacy JSON backups into Firestore.
- `LegacySettingsBootstrapper` is intentionally a no-op.
- `AuthGateScreen` no longer offers local backup export.
- No active runtime code or tests depend on `LegacyMigrationDataSource`, `RoomLegacyMigrationDataSource`, `WorkoutDao`, or `WorkoutDatabase`.

## Room removal decision

- Room removal is complete.
- Manual backup import is the surviving legacy recovery path.

Why this was safe:

- A non-Room recovery path exists through legacy backup import.
- Sign-in no longer retries by reading local Room payloads.
- The legacy fallback promise is now satisfied by importing a backup file rather than reading on-device Room state.

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

- Done.
- Local-only settings now live in `LocalAppPreferencesRepository` backed by DataStore.
- Active synced workout-session settings now flow through the focused `SyncedWorkoutSettingsRepository` contract.
- `SettingsScreen` now renders from `SettingsScreenState`.
- `SettingsViewModel` now composes DataStore-backed local settings and synced workout-session settings into that UI state.
- Active runtime ViewModels no longer read the legacy persisted `Settings` blob directly.
- Legacy settings seeding now runs through the dedicated `LegacySettingsBootstrapper` seam instead of direct blob reads in runtime state assembly.
- Persisted `Settings` remains only behind migration, backup, and bootstrap or seeding boundaries.
- The direct runtime `Settings` readers cleanup is complete.
- The active synced-settings seam is complete.
- Further cleanup of the legacy persisted `Settings` blob is future work behind those migration, backup, and bootstrap boundaries.

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
  - remove the remaining Room-backed migration/import fallback boundary
  - delete `MigrationOrchestrator` Room-loading behavior
  - delete `WorkoutDatabase`
  - delete `WorkoutDao`
  - delete `data/local/room/entity/*`
  - delete `RoomMappers`
  - delete `RoomLegacyMigrationDataSource`
  - delete `LegacyMigrationDataSource` plus bindings if no storage-neutral fallback seam is still needed
  - remove Room providers from `AppModule`
  - delete remaining Room tests
  - remove Room dependencies from Gradle

Status:

- Complete.
- Room-specific runtime code, tests, and Gradle dependencies have been removed.
- `MigrationOrchestrator` now supports only empty migration completion plus legacy backup import.
- Backup import is the only surviving legacy recovery path.
- The old Room-backed fallback path is retired.

Final removal preconditions:

- Satisfied in this branch.
- A non-Room import path exists for users who still have legacy local-only data.
- Sign-in retry and manual backup import no longer depend on `MigrationOrchestrator` reading Room-backed payloads.
- No active runtime code depends on `LegacyMigrationDataSource`, `RoomLegacyMigrationDataSource`, `WorkoutDao`, or `WorkoutDatabase`.
- Existing legacy recovery is narrowed to importing a backup file.

Final Room-removal checklist:

- Done in this branch:
- The migration/import path no longer reads from Room in app code or tests.
- `WorkoutDatabase`, `WorkoutDao`, `data/local/room/entity/*`, `RoomMappers`, `RoomLegacyMigrationDataSource`, and `LegacyMigrationDataSource` are removed.
- Room-backed DI wiring is removed from `AppModule`.
- Remaining Room-specific tests and fixtures are removed.
- Room dependencies are removed from Gradle.
- `./gradlew :app:assembleDebug` remains the verification that the app still builds without Room.

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
