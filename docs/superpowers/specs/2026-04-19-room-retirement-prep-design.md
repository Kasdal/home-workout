# Room Retirement Preparation Design

## Goal

Prepare the codebase for safe Room retirement without removing the Room-backed legacy fallback in this release line.

## Current State

- Room is no longer part of normal runtime repository reads or writes.
- The remaining Room-backed path is concentrated in legacy migration/import fallback:
  - `WorkoutDatabase`
  - `WorkoutDao`
  - `RoomLegacyMigrationDataSource`
  - `LegacyMigrationDataSource`
  - `MigrationOrchestrator`
  - Room providers in `AppModule`
- This is already much smaller than earlier in the project, but the final deletion plan is still implicit.

## Decision

Do a Room-retirement preparation slice rather than removing Room now.

The slice should make the remaining Room-backed fallback path more explicitly isolated and document the exact deletion set and preconditions for final removal.

## Why This Approach

- It preserves the current fallback safety for one more release line.
- It reduces uncertainty around the final Room-removal step.
- It turns the remaining Room usage into a clearly bounded legacy adapter concern.

## Scope

### In Scope

- Tighten the remaining Room-backed fallback boundary if any Room concepts still leak beyond it.
- Make the final Room-removal deletion set explicit.
- Update docs/checklists to reflect the real end-state conditions.

### Out Of Scope

- Actually deleting Room in this slice.
- Changing migration/import/export behavior.
- Replacing the fallback mechanism.

## Target Architecture After This Slice

- Room is visibly a temporary legacy adapter only.
- The only remaining Room-backed path is the migration/import fallback boundary.
- The final Room-removal work becomes a smaller, mechanical slice.

## Likely Focus Areas

- verify no accidental Room dependency spread exists outside:
  - `MigrationOrchestrator`
  - `RoomLegacyMigrationDataSource`
  - `AppModule` providers
- update `docs/decoupling-plan.md` with a concrete Room-removal checklist
- update local `Architecture.md` accordingly

## Room Removal Readiness Checklist

The final Room removal should happen only when all of these are true:

- manual backup/import path is considered sufficient
- local Room recovery is no longer needed for the release line
- no runtime feature depends on `WorkoutDatabase` / `WorkoutDao`
- the deletion set is understood and verified

## Files Likely To Change

- `docs/decoupling-plan.md`
- `Architecture.md`
- possibly a small number of boundary files if any stray Room references remain

## Verification

- Run `./gradlew :app:testDebugUnitTest`.
- Run `./gradlew :app:assembleDebug`.
