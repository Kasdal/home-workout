# Native Android Workout Tracker

A modern, native Android application designed for tracking strength training workouts with a focus on simplicity and efficiency.

## 📱 Features

- **Workout Tracking**: Log sets, reps, and weights for various exercises.
- **Focus Mode**: Distraction-free view during active sessions.
- **Smart Timers**: Built-in rest timers and exercise switch timers with visual flash cues.
- **Analytics**: View volume trends and workout frequency charts.
- **Rest Days**: Calendar view to track and manage rest days with notes.
- **History**: Comprehensive view of past workout sessions.
- **Customization**: Configurable settings for themes (Light/Dark/Auto), sounds, and timers.
- **Tutorial**: Interactive guide to help new users get started.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetbrains/compose) (Material3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/)
- **Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Concurrency**: Kotlin Coroutines & Flow

## 🏗️ Build Instructions

### Prerequisites
- JDK 17+
- Android SDK
- Gradle (Wrapper or Local Installation)

### Building the APK
This project uses a local Gradle installation.

**Debug Build:**
```powershell
C:\Gradle\gradle-9.2.1\bin\gradle.bat assembleDebug
```

**Clean Build:**
```powershell
C:\Gradle\gradle-9.2.1\bin\gradle.bat clean
```

### Installing
To install the debug APK on a connected device:

```powershell
C:\platform-tools-latest-windows\platform-tools\adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
```

## 🚀 CI/CD

This project uses GitHub Actions for automated builds and releases.

### Automated Builds
- Every push to `master` triggers a build
- Both debug and release APKs are built and stored as artifacts

### Creating a Release
1. Update version in `version.properties`:
   ```properties
   VERSION_MAJOR=1
   VERSION_MINOR=0
   VERSION_PATCH=1
   VERSION_BUILD=2
   ```
2. Commit and push changes
3. Create and push a version tag:
   ```powershell
   git tag v1.0.1
   git push origin v1.0.1
   ```
4. GitHub Actions automatically creates a release with APK files

## 📂 Project Structure

- `ui/`: Composable screens and ViewModels
- `data/`: Database entities, DAOs, and Repositories
- `di/`: Dependency Injection modules
- `util/`: Utility classes and extensions

---
*Developed by Milan Ples @2025*
