# Architecture and Development Guide

## 1) Project Identity

- App: WorkoutApp
- Package: `com.example.workoutapp`
- Platform: Native Android (Kotlin + Jetpack Compose)
- Repo root: `C:\Users\plesm\Desktop\home-workout`

## 2) Local Development Environment

### 2.1 Environment summary

- OS: Windows
- Java: JDK 17
- Gradle: Wrapper (`./gradlew.bat` from PowerShell)
- Android SDK path: `C:\Users\plesm\AppData\Local\Android\Sdk`
- `local.properties` should point to `C:\\Users\\plesm\\AppData\\Local\\Android\\Sdk`

### 2.2 Tool locations

- JDK: local Windows JDK 17 installation, commonly managed by Android Studio or `JAVA_HOME`
- adb: `C:\Users\plesm\AppData\Local\Android\Sdk\platform-tools\adb.exe`
- To run adb without the full path, add `C:\Users\plesm\AppData\Local\Android\Sdk\platform-tools` to `PATH`
- sdkmanager: `C:\Users\plesm\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat`
- Debug keystore:
  - `C:\Users\plesm\.android\debug.keystore`

### 2.3 Recommended shell env vars

```powershell
$env:JAVA_HOME = "<path-to-jdk-17>"
$env:ANDROID_SDK_ROOT = "C:\Users\plesm\AppData\Local\Android\Sdk"
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
$env:PATH += ";$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:ANDROID_SDK_ROOT\emulator"
```

## 3) Build and Dependency Setup

### 3.1 Core build files

- `build.gradle.kts`
- `app/build.gradle.kts`
- `settings.gradle.kts`

### 3.2 Firebase and cloud deps

- Google Services plugin configured at root and app module
- Firebase deps:
  - `firebase-auth`
  - `firebase-firestore`
  - `firebase-storage` (compiled, currently not used at runtime)
  - `play-services-auth`
  - `kotlinx-coroutines-play-services`

### 3.3 ESP counter/network deps

- Retrofit + Gson converter + OkHttp + logging interceptor
- Cleartext local-network support enabled via `network_security_config.xml`

## 4) Runtime Architecture Overview

High-level architecture: MVVM + Hilt + Firestore-backed runtime repositories.

- UI: `app/src/main/java/com/example/workoutapp/ui`
- ViewModels: per-screen orchestration
- Runtime repository interfaces:
  - `ProfileRepository`
  - `ExerciseRepository`
  - `SessionHistoryRepository`
  - `SettingsRepository`
  - `RestDayRepository`
- Runtime data source: Firestore (`CloudWorkoutRepository`), which implements the focused interfaces
- Shared runtime models live under `com.example.workoutapp.model`
- Local Room database has been removed

Settings boundaries:

- Active synced workout settings now use a focused runtime seam via `SyncedWorkoutSettingsRepository` and `SyncedWorkoutSettingsStore`
- The active synced-settings path no longer depends on the shared persisted `Settings` blob for runtime reads or writes
- `SettingsViewModel` builds `SettingsScreenState` from two active sources:
  - `LocalAppPreferencesRepository` for DataStore-backed local app settings
  - `SyncedWorkoutSettingsRepository` for Firestore-backed workout-session settings
- Active runtime ViewModels no longer read the legacy persisted `Settings` blob directly
- Legacy settings seeding now runs through the dedicated `LegacySettingsBootstrapper` helper seam
- The shared `Settings` model is now package-neutral too and remains only behind migration, backup, and bootstrap-only boundaries

Startup flow:

- `SplashScreen` -> `AuthGateScreen` (Google sign-in) -> app navigation
- `AuthViewModel` owns auth UI state and maps `AuthMigrationCoordinator` results into `AuthUiState`
- `AuthMigrationCoordinator` owns migrate-if-needed and backup import workflows
- `AppLaunchCoordinator` gates app entry on auth state, migration metadata, and backup-import-pending state
- `MainViewModel` exposes the coordinator result and migrates legacy theme state into DataStore if needed

## 5) Cloud and Migration

### 5.1 Firebase status

Configured:

- Firebase project + Android app registration
- Auth (Google provider)
- Firestore + production rules (`users/{uid}/...` scoped)
- CI reconstructs `app/google-services.json` from secret

CI secret:

- `GOOGLE_SERVICES_JSON_B64`
- Workflow step writes decoded file to `app/google-services.json` before build

### 5.2 Migration behavior

- One-time migration/bootstrap is coordinated via `MigrationOrchestrator`
- Migration metadata doc:
  - `users/{uid}/meta/migration`
- Backup import remains the surviving legacy recovery path after Room removal
- Empty-remote sign-in now prompts the user to import a backup or continue without importing before app entry becomes ready
- Profile fallback logic ensures an active/usable profile is selected

## 6) Database

### 6.1 Persistence

- Firestore is the only remaining persisted runtime store.
- DataStore holds local app preferences.
- Room has been removed from the codebase.

## 7) ESP Counter Feature

### 7.1 Data + settings

- Settings fields:
  - `sensorEnabled`
  - `sensorIpAddress`
- Local-only settings now live in DataStore-backed app preferences
- Synced workout-session settings are handled separately from local app preferences

### 7.2 Components

- API models/service:
  - `app/src/main/java/com/example/workoutapp/data/remote/EspApiService.kt`
  - `app/src/main/java/com/example/workoutapp/data/remote/EspSensorData.kt`
- Polling repository:
  - `app/src/main/java/com/example/workoutapp/data/repository/SensorRepository.kt`
- Workout integration:
  - `app/src/main/java/com/example/workoutapp/ui/workout/WorkoutViewModel.kt`
  - sensor polling starts/stops with session
  - sensor reps can auto-complete sets
- UI surfaces:
  - Settings sensor card (`SettingsScreen`)
  - Live sensor status in session card (`ExerciseCard`)

## 8) Build, Run, Test Commands

From repo root:

```powershell
./gradlew.bat :app:assembleDebug
./gradlew.bat :app:assembleRelease
./gradlew.bat signingReport
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :app:connectedDebugAndroidTest
```

Install on device:

```powershell
C:\Users\plesm\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

Logcat:

```powershell
C:\Users\plesm\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat
```

## 9) Backups and Safety

- Primary backup location: `C:\Users\plesm\workout-backup`
- Legacy recovery now depends on exported backup files rather than a local Room fallback
- Do not remove or weaken the backup import path casually

## 10) Firebase Fingerprints (current debug keystore)

- SHA1: `08:DD:75:A7:63:32:EC:F8:63:01:9E:4E:55:18:6A:AA:FE:2D:D7:B7`
- SHA-256: `97:89:0E:95:90:DC:D9:D4:16:9A:2F:AC:86:5D:49:43:E6:17:7E:F8:1E:53:7D:4D:88:A1:FD:53:5D:A7:98:5D`

## 11) Known Constraints and Risks

- Firebase Storage requires Blaze billing for production use; photo sync is currently out of scope.
- Google Sign-In API currently used is deprecated; migrate to Credential Manager later.
- Backup-import gating is now part of startup readiness; auth/startup changes should be checked carefully against `AuthGateScreen`, `AuthViewModel`, and `AppLaunchCoordinator`.
