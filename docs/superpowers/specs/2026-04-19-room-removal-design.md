# Room Removal Design

## Goal

Remove Room and the Room-backed legacy fallback from the app now that runtime behavior, shared models, and migration boundaries have been decoupled and the remaining Room usage is explicitly isolated.

## Current State

- Normal runtime app behavior no longer depends on Room.
- Remaining Room usage is isolated to the legacy fallback boundary:
  - `WorkoutDatabase`
  - `WorkoutDao`
  - `data/local/room/entity/*`
  - `RoomMappers`
  - `RoomLegacyMigrationDataSource`
  - `LegacyMigrationDataSource`
  - Room providers in `AppModule`
- Manual backup/import flow is now the intended legacy recovery mechanism.

## Decision

Remove the Room-backed fallback path entirely in one final legacy-retirement slice.

Keep the cloud backup/import/export path and Firestore runtime path intact.

## Why This Approach

- The codebase is already prepared for it.
- Keeping a dead-end Room fallback longer adds maintenance cost and conceptual drag.
- The remaining Room footprint is now isolated enough that removal should be mostly mechanical.

## Scope

### In Scope

- Delete Room database, DAO, Room entities, Room mappers, and Room legacy migration data source.
- Remove Room providers and bindings from DI.
- Remove `LegacyMigrationDataSource` abstraction if it is no longer needed.
- Update `MigrationOrchestrator` and any callers to rely on the surviving backup/import path only.
- Update docs to reflect that Room is gone.

### Out Of Scope

- Redesigning backup/import/export formats.
- Changing Firestore runtime behavior.
- Broader auth UI redesign.

## Deletion Set

Expected removal targets:

- `app/src/main/java/com/example/workoutapp/data/local/WorkoutDatabase.kt`
- `app/src/main/java/com/example/workoutapp/data/local/dao/WorkoutDao.kt`
- `app/src/main/java/com/example/workoutapp/data/local/room/entity/*`
- `app/src/main/java/com/example/workoutapp/data/local/room/entity/RoomMappers.kt`
- `app/src/main/java/com/example/workoutapp/data/remote/RoomLegacyMigrationDataSource.kt`
- `app/src/main/java/com/example/workoutapp/data/remote/LegacyMigrationDataSource.kt` if no longer needed
- Room-related provider code in `app/src/main/java/com/example/workoutapp/di/AppModule.kt`

## Target Architecture After This Slice

- Firestore and backup/import paths are the only remaining persistence mechanisms.
- No Room or local database fallback remains.
- Legacy recovery relies on exported backup payloads rather than a retained local Room store.

## Risks To Watch

- startup migration path behavior when no backup exists
- manual backup import/export still working after Room deletion
- any hidden tests or helper code still importing Room-specific classes

## Verification

- Run `./gradlew :app:testDebugUnitTest`.
- Run `./gradlew :app:assembleDebug`.
- If there are any targeted migration/import tests available, run them too.
