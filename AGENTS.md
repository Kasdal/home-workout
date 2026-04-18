# AGENTS.md

## Sources of truth

- Trust Gradle files and app code over `AGENT.MD` and `Architecture.md`; both local docs are gitignored and can lag the codebase. Example: Room is already on version `8`, not `7`.

## Structure

- This repo is a single Android module: `:app`.
- Main entrypoints are `app/src/main/java/com/example/workoutapp/WorkoutApp.kt` and `MainActivity.kt`.
- Runtime startup flow is `SplashScreen -> AuthGateScreen -> MainViewModel` before navigation picks `workout` or `onboarding`.
- `WorkoutRepository` is bound to `CloudWorkoutRepository` in `di/AppModule.kt`; Firestore is the live data source.
- Local Room still matters for migration and safety fallback: `MigrationOrchestrator` reads from `WorkoutDao` and uploads to Firestore on sign-in.

## High-risk changes

- Auth, startup, or profile-state changes affect the sign-in gate and one-time migration path. Read `ui/auth/AuthViewModel.kt`, `MainViewModel.kt`, and `data/remote/MigrationOrchestrator.kt` first.
- Room schema changes are risky because `AppModule.kt` still uses `fallbackToDestructiveMigration()`. Add explicit migrations when possible and review migration impact carefully.
- Current Room DB facts are in code: DB name `workout_db`, version `9`, migrations `3->4`, `4->5`, `6->7`, `7->8`, `8->9`.
- ESP sensor support depends on both the settings schema and cleartext network config. Keep `android:networkSecurityConfig="@xml/network_security_config"` in the manifest while the feature exists.

## Build and test

- Use the wrapper from repo root: `./gradlew`.
- Main verification commands:
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:connectedDebugAndroidTest`
  - `./gradlew signingReport`
- Focused unit test example: `./gradlew :app:testDebugUnitTest --tests "com.example.workoutapp.MainViewModelTest"`
- Instrumented tests need a device/emulator. Existing coverage is concentrated in Room DAO tests and Compose workout screen tests under `app/src/androidTest`.

## Firebase and local setup

- Do not commit `app/google-services.json`; it is gitignored.
- CI rebuilds that file from `GOOGLE_SERVICES_JSON_B64` in `.github/workflows/release.yml` before `assembleDebug` and `assembleRelease`.
- `local.properties` is also gitignored; local Android SDK resolution comes from that file.

## Release/versioning

- Release tags use the `v` prefix from Axion Release in the root `build.gradle.kts`.
- `versionName` comes from the SCM tag-derived project version; `versionCode` is the git commit count from `app/build.gradle.kts`. Avoid changing versioning logic casually.
