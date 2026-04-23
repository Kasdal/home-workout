# Auth Migration Orchestration Design

## Goal

Remove migration execution and backup workflow orchestration from `AuthViewModel` while preserving current auth and migration behavior.

## Current State

- `AuthViewModel` currently owns both UI-state handling and migration workflow execution.
- It directly coordinates:
  - `migrateIfNeeded(uid)`
  - retry migration
  - export legacy backup
  - import legacy backup
- It also owns UI-facing auth state:
  - sign-in progress
  - migration progress/results
  - error/info messages

## Decision

Introduce a focused auth-migration orchestration seam, such as `AuthMigrationCoordinator`, that owns migration execution and backup workflows.

Keep `AuthViewModel` as the UI-facing auth state holder that maps those workflow results into `AuthUiState`.

## Why This Approach

- It targets one of the remaining high-risk mixed-responsibility files.
- It separates UI-state management from migration/business workflow.
- It complements the already cleaner launch/app-entry routing.

## Scope

### In Scope

- Extract migration execution out of `AuthViewModel`.
- Extract retry/import/export backup workflow orchestration out of `AuthViewModel`.
- Keep sign-in intent and sign-in/out handling in `AuthViewModel`.
- Update tests.

### Out Of Scope

- Redesigning Google sign-in handling.
- Changing migration behavior.
- Changing backup payload formats.
- More startup/app-entry refactoring in this slice.

## Target Architecture After This Slice

- `AuthViewModel` remains the UI-facing auth ViewModel.
- A focused auth-migration seam owns:
  - migrate-if-needed
  - retry migration
  - export legacy backup
  - import legacy backup
- `AuthViewModel` maps seam results into `AuthUiState`.

## Likely Helper Responsibilities

Examples:

- serialize migration execution for a given user
- wrap `MigrationOrchestrator` calls into a smaller UI-agnostic API
- expose `Result`-style outcomes for retry/import/export actions

## Boundaries

- Keep `AuthManager` usage in `AuthViewModel` for sign-in/sign-out and auth observation.
- Keep `MigrationOrchestrator` itself unchanged unless a minimal change is needed for testability.
- Keep launch-routing state outside this seam.

## Files Likely To Change

- `app/src/main/java/com/example/workoutapp/ui/auth/AuthViewModel.kt`
- one new auth-migration helper/coordinator file
- `app/src/test/java/com/example/workoutapp/ui/auth/AuthViewModelTest.kt` if present or newly added
- one new focused test file for the extracted seam

## Verification

- Run focused unit tests for the extracted auth-migration seam.
- Run `./gradlew :app:testDebugUnitTest`.
- Run `./gradlew :app:assembleDebug`.
