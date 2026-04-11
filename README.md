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
- **Cloud Backend**: [Firebase Authentication](https://firebase.google.com/docs/auth) + [Cloud Firestore](https://firebase.google.com/docs/firestore)
- **Concurrency**: Kotlin Coroutines & Flow

## ☁️ Cloud Sync Status

- Cloud migration foundation is now implemented.
- App startup requires Google sign-in before entering the main experience.
- Data sync is user-scoped in Firestore under `users/{uid}/...`.
- Local Room data is retained as migration safety fallback.
- Firebase Storage-based photo sync is currently out of scope on Spark plan.

See `CHANGELOG.md` for the latest migration milestones and details.

## 🏗️ Build Instructions

### Prerequisites
- JDK 17+
- Android SDK
- Gradle (Wrapper or Local Installation)
