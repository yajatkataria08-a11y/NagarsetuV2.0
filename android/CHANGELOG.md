# Changelog

All notable changes to NagarSetu are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased]

### Added
- `gradle/libs.versions.toml` — version catalog as single source of truth for all dependency versions
- `buildSrc/` — convention plugins (`nagarsetu.android.library`, `nagarsetu.android.library.compose`) eliminating ~30 lines of boilerplate per module
- `.github/workflows/ci.yml` — GitHub Actions CI (build, unit tests, lint)
- `CONTRIBUTING.md` — contributor guide including "how to add a feature module"
- `CHANGELOG.md` — this file
- `proguard-rules.pro` + `consumer-rules.pro` stubs in every module (were referenced but missing)
- Gradle performance flags: `parallel=true`, `caching=true`, `configuration-cache=true`

### Changed
- All `build.gradle.kts` files now use version catalog aliases (`libs.*`) instead of hardcoded version strings
- `build.gradle.kts` root plugin block updated to use `alias(libs.plugins.*)`
- `gradle.properties` CRLF → LF

### Fixed
- Inconsistent Compose UI version strings across frontend modules (some used BOM, some hardcoded `1.6.1`) — all now use the BOM

---

## [2.0.0] — 2026-05-20

### Added
- Initial multi-module structure with `frontend/` and `backend/` layers
- Features: ChargeUp, Dashboard, DriveLegal, EmergencyAI, GreenRoute, HealthWatch, ParkEase, Predictive, Raksha, RoadWatch
- Jetpack Compose UI with Material 3
- Mapbox Maps SDK integration
- TensorFlow Lite pothole detection (RoadWatch)
- ML Kit OCR for number plate scanning (DriveLegal)
- Socket.io real-time alert feed (Dashboard)
- Shake-detect SOS trigger (EmergencyAI)
- TF-IDF chatbot for traffic law queries (DriveLegal)
