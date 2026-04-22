# Settings Package Neutralization Design

## Goal

Move the legacy/shared `Settings` model out of `data.local.entity` into a storage-neutral package without changing its shape or behavior.

## Current State

- Most shared runtime models already live in `com.example.workoutapp.model`.
- `Settings` is the main remaining shared model still under `data.local.entity`.
- It is still used behind legacy-oriented boundaries:
  - Firestore settings mapping
  - migration payloads
  - backup import/export
  - bootstrap seeding into DataStore
- The current problem is primarily package semantics, not model shape.

## Decision

Move `Settings` into the storage-neutral shared model package and update imports only.

Do not redesign the `Settings` shape in this slice.

## Why This Approach

- It completes the package-neutralization effort with the smallest remaining change.
- It avoids mixing package cleanup with legacy settings redesign in one slice.
- It makes future Room-retirement work less confusing.

## Scope

### In Scope

- Move `Settings` to the shared neutral model package.
- Update imports across runtime code, mappers, migration/backup code, and tests.
- Preserve behavior and wire/storage formats.

### Out Of Scope

- Splitting or redesigning the `Settings` model.
- Changing migration payload format.
- Changing Firestore document shape.
- Room removal.

## Target Architecture After This Slice

- All shared models, including `Settings`, live in a storage-neutral package.
- Room-specific entity classes remain under Room-specific packages.
- Remaining legacy concerns are about behavior and persistence boundaries, not misleading package ownership.

## Files Likely To Change

- move `Settings.kt` into `app/src/main/java/com/example/workoutapp/model/`
- update imports in:
  - `data/repository/SettingsRepository.kt`
  - `data/repository/CloudWorkoutRepository.kt`
  - `data/remote/FirestoreRepository.kt`
  - `data/remote/model/CloudModels.kt`
  - `data/remote/LegacyMigrationDataSource.kt`
  - `data/remote/LegacyMigrationBackupCodec.kt`
  - `data/settings/LocalAppPreferencesRepository.kt`
  - `data/local/room/entity/RoomMappers.kt`
  - tests importing `Settings`

## Verification

- Run `./gradlew :app:testDebugUnitTest`.
- Run `./gradlew :app:assembleDebug`.
