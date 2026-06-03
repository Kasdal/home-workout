# Changelog

All notable changes to this project are documented in this file.

## [1.1.5] - 2026-06-03

### Changed
- **In-session workout flow:** The "Complete Session" button is now hidden while exercises remain in the queue; it only appears once all exercises are done. Mid-session exit is no longer offered, encouraging users to finish the planned workout.
- **Sensor card redesigned:** Reduced the in-session sensor card footprint by ~20% (smaller rep number, tighter padding/spacing, removed redundant helper text) so the active exercise card stays the visual focus.
- **Sensor mode indicator:** The "Sensor Tracked" / "Hold Timer" / "Manual Reps" chip has been replaced with a small circular mode icon pinned to the top-right of the active exercise card, freeing up vertical real estate.
- **In-session weight edits:** The kg `+`/`-` controls on the active exercise card now work mid-session, wiring through to `viewModel.updateExercise` so you can correct the working weight without leaving the workout.

### Fixed
- `WorkoutScreenTest` no longer asserts the in-session "Complete Session" button is displayed and now passes the new `onUpdateExercise` callback; `WorkoutsScreenTest` provides the newer `getExerciseHistory` and `onReorderExercises` parameters.
- Release workflow `awk` script: kept master's safer `index($0, ver)` form over the regex-based `$0 ~ ver` to avoid bracket-metacharacter bugs during changelog extraction.

## [1.1.4] - 2026-05-24

### Added
- **Auto-update system:** App polls GitHub Releases API on startup (24h cooldown) and shows an update bottom sheet when a newer version is available.
- **What's New dialog:** Shown once per version after update, displaying release notes from GitHub (cached) with offline fallback.
- **Revamped About screen:** Clickable from Settings, shows full version string (`vX.Y.Z (build N)`), send feedback via email, view licenses.

### Changed
- **Onboarding flow:** New users are automatically taken through the tutorial after metrics entry instead of landing on an empty workout screen.
- **Tutorial:** Replaced emoji illustrations with screenshot images for each page.
- **Version display:** Full format `v{VERSION_NAME} (build {VERSION_CODE})` shown in Settings and About.

### CI
- Release workflow extracts relevant section from `CHANGELOG.md` for GitHub Release body instead of generic text.

## [Unreleased]