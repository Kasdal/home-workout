# App Entry Coordinator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the split post-splash startup/auth routing logic with a single migration-aware app-entry state contract that simplifies `MainActivity` without changing auth or migration behavior.

**Architecture:** Add migration readiness observation in the Firestore layer, then have `AppLaunchCoordinator` derive app entry from auth state, migration metadata, and profile state together. `MainViewModel` exposes that app-entry state to `MainActivity`, which renders auth gate or app navigation from it while keeping splash timing local.

**Tech Stack:** Kotlin, Android, Compose, ViewModel, Coroutines Flow, Hilt, Firebase Auth, Firestore

---

### Task 1: Add Migration Readiness Observation

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/data/remote/FirestoreRepository.kt`
- Modify: `app/src/main/java/com/example/workoutapp/data/remote/model/CloudModels.kt`

- [ ] **Step 1: Add a migration-meta observation API**

Add a Firestore flow for the `users/{uid}/meta/migration` document, shaped like the existing observe-style repository methods:

```kotlin
fun observeMigrationMeta(uid: String): Flow<CloudMigrationMeta?> = callbackFlow {
    val listener = userRoot(uid).collection("meta").document("migration")
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject())
        }

    awaitClose { listener.remove() }
}.conflate()
```

- [ ] **Step 2: Preserve existing migration meta model behavior**

Do not change `CloudMigrationMeta` semantics. Keep `migrationComplete` as the readiness signal.

- [ ] **Step 3: Build to verify the observation seam**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Make AppLaunchCoordinator Migration-Aware

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/domain/startup/AppLaunchCoordinator.kt`

- [ ] **Step 1: Expand the app-entry state model**

Use a small sealed contract like:

```kotlin
sealed interface AppEntryState {
    data object AuthRequired : AppEntryState
    data object MigrationInProgress : AppEntryState
    data class Ready(val startDestination: String) : AppEntryState
}
```

- [ ] **Step 2: Derive app entry from auth plus migration readiness plus profile state**

The coordinator flow should behave like this:

```kotlin
fun appEntryState(): Flow<AppEntryState> {
    return authManager.currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(AppEntryState.AuthRequired)
        } else {
            firestoreRepository.observeMigrationMeta(user.uid).flatMapLatest { meta ->
                if (meta?.migrationComplete != true) {
                    flowOf(AppEntryState.MigrationInProgress)
                } else {
                    repository.getUserMetrics().map { metrics ->
                        AppEntryState.Ready(
                            startDestination = if (metrics != null) "workout" else "onboarding"
                        )
                    }
                }
            }
        }
    }.distinctUntilChanged()
}
```

Use the actual injected dependencies and existing patterns in the file.

- [ ] **Step 3: Keep a temporary compatibility bridge only if needed**

If compileability before Task 3 still needs it, keep `startDestination()` as an adapter inside `AppLaunchCoordinator.kt` only.

- [ ] **Step 4: Build to verify the coordinator slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Move MainViewModel And MainActivity To The New App Entry Flow

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/MainViewModel.kt`
- Modify: `app/src/main/java/com/example/workoutapp/MainActivity.kt`

- [ ] **Step 1: Expose app-entry state from MainViewModel**

Use the coordinator flow directly:

```kotlin
val appEntryState: StateFlow<AppEntryState?> = appLaunchCoordinator.appEntryState()
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)
```

Keep `themeMode` and legacy theme seeding unchanged.

- [ ] **Step 2: Remove the separate app unlock boolean from MainActivity**

Delete the local `appUnlocked` state and stop using `AuthGateScreen(onReady = ...)` as a separate unlock mechanism.

- [ ] **Step 3: Render from splash plus app-entry state**

After splash:

```kotlin
val appEntryState by mainViewModel.appEntryState.collectAsState()
```

Render:

```kotlin
when (val entryState = appEntryState) {
    null -> Surface(color = MaterialTheme.colorScheme.background) {}
    AppEntryState.AuthRequired,
    AppEntryState.MigrationInProgress -> AuthGateScreen(onReady = {})
    is AppEntryState.Ready -> {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = entryState.startDestination) {
            // existing destinations unchanged
        }
    }
}
```

Keep splash behavior unchanged.

- [ ] **Step 4: Build to verify the integrated routing slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 4: Refresh Startup/Auth Documentation

**Files:**
- Modify: `Architecture.md`
- Modify: `docs/decoupling-plan.md`

- [ ] **Step 1: Update architecture guidance**

Document these facts:

```text
Splash remains a local UI timer in MainActivity.
Post-splash app entry renders from a single app-entry state exposed by MainViewModel.
AppLaunchCoordinator owns auth-required vs migration-in-progress vs ready routing.
AuthViewModel still owns sign-in and migration behavior.
Migration readiness is sourced from Firestore migration metadata.
```

- [ ] **Step 2: Update decoupling-plan status**

Mark startup/auth coordination as complete for launch routing cleanup, while noting that sign-in and migration execution still remain in `AuthViewModel` as an intentional future seam.

- [ ] **Step 3: Build to verify the final slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`
