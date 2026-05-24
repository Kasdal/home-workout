# Changelog

All notable changes to this project are documented in this file.

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