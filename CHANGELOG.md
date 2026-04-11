# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

### Added
- Firebase integration foundation for cloud sync (`Auth`, `Firestore`, `Google Services` plugin and dependencies).
- Google sign-in gate before app usage to ensure user-scoped cloud data.
- Firestore-backed repository implementation (`CloudWorkoutRepository`) with user path isolation under `users/{uid}/...`.
- One-time migration flow from local Room DB to Firestore with migration metadata tracking (`users/{uid}/meta/migration`).
- Migration verification step that checks uploaded document counts before marking migration complete.
- New auth UI layer for sign-in and migration state handling.
- `WorkoutStats` model and repository/DAO method alignment for cloud and local parity.

### Changed
- Startup flow now runs as: splash -> auth gate -> app navigation.
- Start destination logic now reacts to live profile state and no longer relies on one-time snapshot checks.
- Onboarding now redirects to profiles when existing profiles are already present.

### Fixed
- Resolved repeated onboarding/profile-creation loop when profile data existed but active profile state was not correctly resolved.
- Added fallback profile resolution when no profile is marked `isActive`.
- Ensured initial migration assigns an active profile if missing.
- Corrected cloud mapping for deleted exercise flag to match local model behavior.

### Notes
- Local Room data remains preserved as safety fallback after migration.
- Firebase Storage upload/sync