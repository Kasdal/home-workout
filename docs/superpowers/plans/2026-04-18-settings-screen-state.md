# Settings Screen State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the settings screen's dependency on the persisted `Settings` model with a storage-neutral `SettingsScreenState`.

**Architecture:** Add a UI-only `SettingsScreenState` under `ui/settings/` and make `SettingsViewModel` compose local app preferences and synced workout-session settings into that state. Keep the persisted `data.local.entity.Settings` model behind repository and migration boundaries, using it only for storage and legacy seeding.

**Tech Stack:** Kotlin, Android ViewModel, Coroutines Flow, Hilt, DataStore, Firestore

---

### Task 1: Add The Neutral Settings UI State

**Files:**
- Create: `app/src/main/java/com/example/workoutapp/ui/settings/SettingsScreenState.kt`

- [ ] **Step 1: Add the new UI state file**

```kotlin
package com.example.workoutapp.ui.settings

data class SettingsScreenState(
    val soundsEnabled: Boolean = true,
    val soundVolume: Float = 1.0f,
    val timerSoundType: String = "beep",
    val celebrationSoundType: String = "cheer",
    val themeMode: String = "dark",
    val tutorialCompleted: Boolean = false,
    val tutorialVersion: Int = 1,
    val restTimerDuration: Int = 30,
    val exerciseSwitchDuration: Int = 90,
    val undoLastSetEnabled: Boolean = true,
    val sensorEnabled: Boolean = false,
    val sensorIpAddress: String = "192.168.0.125"
)
```

- [ ] **Step 2: Build to verify the file is valid**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 2: Move SettingsViewModel Onto The UI State

**Files:**
- Modify: `app/src/main/java/com/example/workoutapp/ui/settings/SettingsViewModel.kt`

- [ ] **Step 1: Change the ViewModel state type**

Replace the current persisted model import and state declaration with:

```kotlin
import com.example.workoutapp.ui.settings.SettingsScreenState

private val _settings = MutableStateFlow(SettingsScreenState())
val settings: StateFlow<SettingsScreenState> = _settings.asStateFlow()
```

- [ ] **Step 2: Keep legacy seeding, but compose only UI state fields**

The loading logic should continue to seed DataStore from persisted settings:

```kotlin
viewModelScope.launch {
    settingsRepository.getSettings().collect { dbSettings ->
        dbSettings?.let { localAppPreferencesRepository.seedFromLegacySettingsIfUnset(it) }
    }
}
```

And the screen state updates should use only `SettingsScreenState.copy(...)`:

```kotlin
_settings.update {
    it.copy(
        themeMode = localSettings.themeMode,
        soundsEnabled = localSettings.soundsEnabled,
        soundVolume = localSettings.soundVolume,
        timerSoundType = localSettings.timerSoundType,
        celebrationSoundType = localSettings.celebrationSoundType,
        tutorialCompleted = localSettings.tutorialCompleted,
        tutorialVersion = localSettings.tutorialVersion,
        sensorEnabled = localSettings.sensorEnabled,
        sensorIpAddress = localSettings.sensorIpAddress
    )
}
```

- [ ] **Step 3: Keep transient command state separate**

Retain:

```kotlin
private val _sensorConnectionState = MutableStateFlow<String?>(null)
val sensorConnectionState: StateFlow<String?> = _sensorConnectionState.asStateFlow()
```

Do not fold it into `SettingsScreenState` in this slice.

- [ ] **Step 4: Build to verify the feature slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

### Task 3: Refresh Documentation For The New Boundary

**Files:**
- Modify: `Architecture.md`
- Modify: `docs/decoupling-plan.md`

- [ ] **Step 1: Update architecture guidance**

Document these facts in `Architecture.md`:

```text
SettingsScreen renders from SettingsScreenState.
SettingsViewModel composes local DataStore-backed settings and synced workout-session settings.
Persisted Settings remains behind repository and migration boundaries.
```

- [ ] **Step 2: Update decoupling-plan status**

Document that the settings split is now complete at the UI boundary, while persisted settings cleanup remains future work if still needed after migration fallback retirement.

- [ ] **Step 3: Build to verify the final slice**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`
