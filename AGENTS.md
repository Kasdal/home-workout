# AGENTS.md

## Sources of truth

- Trust Gradle files and app code over `AGENT.MD` and `Architecture.md`; both local docs can lag the codebase.

## Structure

- This repo is a single Android module: `:app`.
- Main entrypoints are `app/src/main/java/com/example/workoutapp/WorkoutApp.kt` and `MainActivity.kt`.
- Runtime startup flow is `SplashScreen -> AuthGateScreen -> MainViewModel`, and `AppLaunchCoordinator` decides whether the app can enter navigation.
- `CloudWorkoutRepository` is the main Firestore-backed runtime implementation and is exposed through focused interfaces in `di/AppModule.kt`.
- Shared runtime models live under `com.example.workoutapp.model`.
- Room has been removed. Legacy recovery now relies on backup import/export paths rather than a local Room fallback.

## High-risk changes

- Auth, startup, or profile-state changes affect the sign-in gate and one-time migration path. Read `ui/auth/AuthViewModel.kt`, `MainViewModel.kt`, and `data/remote/MigrationOrchestrator.kt` first.
- Backup import / startup gating changes are high-risk now that Room is gone. Read `ui/auth/AuthViewModel.kt`, `ui/auth/AuthGateScreen.kt`, `domain/startup/AppLaunchCoordinator.kt`, and `data/remote/MigrationOrchestrator.kt` first.
- Do not reintroduce Room annotations or local database assumptions onto shared runtime models in `com.example.workoutapp.model`.
- ESP sensor support depends on both the settings schema and cleartext network config. Keep `android:networkSecurityConfig="@xml/network_security_config"` in the manifest while the feature exists.

## Build and test

- Use the wrapper from repo root: `./gradlew`.
- Main verification commands:
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:connectedDebugAndroidTest`
  - `./gradlew signingReport`
- Focused unit test example: `./gradlew :app:testDebugUnitTest --tests "com.example.workoutapp.MainViewModelTest"`
- Instrumented tests need a device/emulator. Current coverage is concentrated in Compose workout screen tests and a growing set of focused JVM tests around startup/auth, session/timer, sensor, and migration seams.

## Firebase and local setup

- Do not commit `app/google-services.json`; it is gitignored.
- CI rebuilds that file from `GOOGLE_SERVICES_JSON_B64` in `.github/workflows/release.yml` before `assembleDebug` and `assembleRelease`.
- `local.properties` is also gitignored; local Android SDK resolution comes from that file.
- If Gradle/KSP/Hilt starts failing with missing generated files, duplicate `*_Factory` classes, or unreadable generated outputs after large refactors, clear stale outputs before retrying: remove `app/build/generated/ksp`, `app/build/intermediates`, and `app/build/tmp`, then rerun `./gradlew --no-daemon`.

## Release/versioning

- Release tags use the `v` prefix from Axion Release in the root `build.gradle.kts`.
- `versionName` comes from the SCM tag-derived project version; `versionCode` is the git commit count from `app/build.gradle.kts`. Avoid changing versioning logic casually.
