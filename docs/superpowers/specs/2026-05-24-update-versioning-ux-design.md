# Design: Auto-Update, Versioning, and UX Improvements

**Date:** 2026-05-24
**Status:** Approved
**Approach:** B — Full-featured (GitHub Releases API + DataStore caching)

---

## 1. Auto-Update System

### Overview
Since the app is sideloaded via GitHub Releases, the update mechanism polls the GitHub Releases API and notifies users of new versions with a bottom sheet.

### Architecture

```
App Launch
  └─> UpdateChecker (fires after splash, before main nav)
       ├─ Check DataStore: has 24h passed since last check?
       │   ├─ No → skip
       │   └─ Yes → fetch GitHub Releases API
        ├─ GET https://api.github.com/repos/Kasdal/home-workout/releases/latest
       ├─ Parse tag_name (e.g. "v1.2.0"), compare semver to BuildConfig.VERSION_NAME
       └─ If newer AND version != skippedVersion → show UpdateAvailableBottomSheet
```

### New Files

| File | Purpose |
|------|---------|
| `data/remote/UpdateChecker.kt` | GitHub API call + version comparison logic |
| `data/settings/UpdateCheckPreferences.kt` | DataStore keys for `lastCheckTimestamp`, `skippedVersion`, `cachedChangelogMd`, `cachedChangelogVersion` |
| `ui/components/UpdateAvailableBottomSheet.kt` | Modal bottom sheet showing new version info |

### UpdateChecker Behavior
- Uses OkHttp (already in project) — no new dependencies
- GitHub Releases API is public, no auth needed for public repos
- Repo: `Kasdal/home-workout` — hardcoded as `BuildConfig` fields in `app/build.gradle.kts`:
  - `buildConfigField "String", "GITHUB_REPO_OWNER", "\"Kasdal\""`
  - `buildConfigField "String", "GITHUB_REPO_NAME", "\"home-workout\""`
- Check happens silently in background coroutine; only shows UI if update found
- 24h cooldown to avoid rate limiting (GitHub allows 60 req/hr unauthenticated)

### UpdateAvailableBottomSheet UI
- Title: "Update Available"
- Version: `v1.3.0` (current vs new)
- Release notes preview (first ~300 chars of release body)
- Three buttons:
  - **"Download Update"** — opens browser to release HTML URL
  - **"Remind Me Later"** — dismisses sheet (shows again next app launch after cooldown)
  - **"Skip This Version"** — stores skipped version in DataStore, never shows for this version again

### Integration Point
- `MainViewModel.kt` or `AppLaunchCoordinator.kt` — launch the check after `AppEntryState.Ready`
- Bottom sheet shown via a `MutableStateFlow<UpdateInfo?>` in MainViewModel

---

## 2. What's New / Changelog System

### Overview
Detect version changes on app start and show a "What's New" dialog exactly once per version.

### Trigger
On every app start, in `AppLaunchCoordinator`, compare:
- `BuildConfig.VERSION_NAME` vs `lastSeenVersion` (from DataStore)
- If different → store new version → emit event to show "What's New" dialog

### Content Sources (dual source, graceful fallback)

| Source | When used |
|--------|-----------|
| GitHub Releases API (cached) | Preferred — fetched during update check, cached in DataStore for 7 days |
| Embedded string resource (`res/values/changelog.xml`) | Fallback — bundled in APK, always available offline |

### DataStore Keys (in `UpdateCheckPreferences`)
- `lastSeenVersion: String` — e.g. `"1.2.0"`
- `cachedChangelogMd: String` — markdown from latest GitHub release
- `cachedChangelogVersion: String` — version the cache is for
- `lastCheckTimestamp: Long` — epoch millis of last update check
- `skippedVersion: String` — version user chose to skip

### "What's New" Dialog UI
- Title: "What's New in v{version}"
- Scrollable body rendering the changelog text
- Single "Got it" dismiss button
- Shown exactly once per version

### New Files

| File | Purpose |
|------|---------|
| `ui/components/WhatsNewDialog.kt` | Compose dialog for post-update changelog |
| `res/values/changelog.xml` | Embedded fallback changelog string (manually synced with `CHANGELOG.md` on each release) |

### Keeping Changelog in Sync
- The canonical source is `CHANGELOG.md` at the project root
- On each release, copy the relevant version section from `CHANGELOG.md` into `res/values/changelog.xml` as a string resource
- The CI workflow enhancement (Section 3) also extracts from `CHANGELOG.md` for the GitHub Release body
- Net result: one source of truth (`CHANGELOG.md`) feeds both the app and the GitHub Release

---

## 3. CI Release Workflow Enhancement

### Changes to `.github/workflows/release.yml`

The release job currently uses a generic body: "See the commit history for details". Improve it to:

1. Extract the relevant section from `CHANGELOG.md` for the tagged version
2. Use that as the GitHub Release body
3. This ensures the GitHub Releases API returns meaningful release notes that the app can display

### Implementation
- Add a step before `softprops/action-gh-release` that parses `CHANGELOG.md` and extracts the section matching the tag version
- Pass extracted text as the release `body`

---

## 4. About Screen Revamp

### Access
- Add a clickable "About" row at the bottom of the existing Settings screen (version already shown there)
- Tapping navigates to the full About screen
- No new bottom nav tab — avoids cluttering the 5 existing tabs

### About Screen Content

| Section | Content |
|---------|---------|
| App identity | App name, icon, `v1.2.0 (build 142)` |
| What's New | Button → opens changelog history dialog (cumulative list from cache or embedded) |
| Check for Updates | Button → forces immediate update check (bypasses 24h cooldown), shows result inline or bottom sheet |
| Licenses | Opens Android OSS licenses or a simple scrollable list |
| Feedback | Opens email intent with pre-filled subject including app version |
| Developer credit | "Developed by Milan Ples @2025" |

### Version Display Everywhere
- Format: `v{VERSION_NAME} (build {VERSION_CODE})`
- Both `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE` used
- Applied in: Settings screen, About screen, update bottom sheet, What's New dialog

### Files Changed

| File | Change |
|------|--------|
| `ui/settings/SettingsScreen.kt` | Update version display format, make About row clickable |
| `ui/about/AboutScreen.kt` | Full rewrite with new sections |

---

## 5. Onboarding Improvements

### Changes

**Auto-trigger tutorial for new users:**
- After `OnboardingScreen` saves metrics → navigate to `TutorialScreen` instead of `WorkoutScreen`
- Add `tutorialCompleted: Boolean` to DataStore (`LocalAppPreferencesRepository`)
- Tutorial only auto-shows once (when `tutorialCompleted == false` after metrics entry)

**Enhanced tutorial content:**
- Replace emoji illustrations with actual app screenshots (9 new WebP assets in `res/drawable/`)
- Each page shows a screenshot + descriptive caption

| Page | Screenshot | Caption |
|------|-----------|---------|
| 1 | Workout library | "Create exercises and manage your workout library" |
| 2 | Exercise wizard | "Add exercises with sets, reps, weights, hold timers, or sensor support" |
| 3 | Active session | "Start a session and work through your exercises one by one" |
| 4 | Hold-to-complete | "Hold the exercise card to mark a set as complete" |
| 5 | Rest timer | "Auto-countdown between sets with configurable rest periods" |
| 6 | Weight adjustment | "Adjust weight in 5kg increments during your session" |
| 7 | History dashboard | "Track your progress with volume charts, PRs, and streaks" |
| 8 | Rest day calendar | "Mark rest days and see your weekly/monthly consistency" |
| 9 | Settings | "Customize sounds, timers, theme, and ESP sensor support" |

**Skip/Revisit:**
- Tutorial has "Skip" and "Done" buttons (existing behavior preserved)
- Existing "How it works" button in Workouts screen still works for revisiting

### Files Changed

| File | Change |
|------|--------|
| `ui/onboarding/OnboardingScreen.kt` | Change post-metrics navigation destination |
| `ui/tutorial/TutorialScreen.kt` | Replace emoji with Image composables using drawable resources, update captions |
| `data/settings/LocalAppPreferencesRepository.kt` | Add `tutorialCompleted` preference |
| `data/settings/LocalAppSettings.kt` | Add `tutorialCompleted: Boolean` field |
| `res/drawable/` | Add 9 screenshot assets (`tutorial_page_1.webp` through `tutorial_page_9.webp`) |

---

## 6. Implementation Order

| Phase | Items | Dependencies |
|-------|-------|-------------|
| 1 | UpdateCheckPreferences + DataStore keys | None |
| 2 | UpdateChecker + GitHub API integration | Phase 1 |
| 3 | UpdateAvailableBottomSheet | Phase 2 |
| 4 | What's New dialog + version detection | Phase 1 |
| 5 | About screen revamp | Phase 1 |
| 6 | CI workflow enhancement | Phase 3 (verifies release notes) |
| 7 | Onboarding improvements (tutorial auto-trigger + screenshots) | Phase 1 |

Phases 1-2 are the foundation. Phases 3-6 can proceed in parallel after Phase 2. Phase 7 is independent.

---

## 7. Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| GitHub API rate limiting (60 req/hr unauthenticated) | 24h cooldown between checks, cached results |
| GitHub API unreachable / no internet | Graceful fallback — skip check silently, use cached changelog, embedded string as last resort |
| Screenshot assets increase APK size | Use WebP format, keep images small (360-480dp wide max) |
| Tutorial auto-trigger annoys returning users | Only triggers when `tutorialCompleted == false` AND metrics were just saved (existing user already has `tutorialCompleted` conceptually "true" via DataStore default) |
