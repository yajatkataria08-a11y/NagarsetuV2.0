# Contributing to NagarSetu

Thank you for taking the time to contribute! Please read this guide before opening an issue or pull request.

---

## Getting started

1. Fork the repo and clone locally.
2. Copy `local.properties.example` → `local.properties` and fill in your tokens.
3. Open the root folder in **Android Studio Hedgehog (2023.1.1) or newer** and let Gradle sync.
4. Run the app on an emulator or device (API 24+) via the `:frontend:app` configuration.

---

## Project layout

```
NagarSetu/
├── buildSrc/                   ← Convention plugins (shared Gradle config)
├── gradle/
│   └── libs.versions.toml      ← Version catalog (single source of truth for deps)
├── frontend/
│   ├── app/                    ← Shell activity + navigation
│   ├── common-ui/              ← Shared UI utilities
│   └── components/<feature>/   ← Compose screens + ViewModels per feature
└── backend/
    └── components/<feature>/   ← Repositories, API services, ML engines per feature
```

## Adding a new feature module

1. **Create the backend module** at `backend/components/<feature>/`.
   - Apply `id("nagarsetu.android.library")` in its `build.gradle.kts`.
   - Add `data/` (repository/data-source) and `domain/model/` packages.

2. **Create the frontend module** at `frontend/components/<feature>/`.
   - Apply `id("nagarsetu.android.library.compose")` in its `build.gradle.kts`.
   - Add `presentation/<feature>/` with a `Screen.kt` and `ViewModel.kt`.

3. **Register both modules** in `settings.gradle.kts`.

4. **Wire up navigation** in `frontend/app/src/main/java/com/nagarsetu/main/MainActivity.kt`.

## Dependency versions

All dependency versions live in **`gradle/libs.versions.toml`**. Do not hardcode version strings in individual `build.gradle.kts` files. To add a dependency:

```toml
# In [versions]
myLib = "1.2.3"

# In [libraries]
my-lib = { group = "com.example", name = "my-lib", version.ref = "myLib" }
```

Then reference it in a build file:
```kotlin
implementation(libs.my.lib)
```

## Code style

- Follow the **[Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)**.
- `kotlin.code.style=official` is enforced via `gradle.properties`.
- Run `./gradlew lintDebug` before opening a PR.

## Pull request checklist

- [ ] New feature has a matching backend + frontend module pair
- [ ] Version catalog updated for any new dependencies (no hardcoded version strings)
- [ ] `./gradlew testDebugUnitTest` passes locally
- [ ] `./gradlew lintDebug` produces no new errors
- [ ] README updated if project structure changed
- [ ] CHANGELOG updated under `[Unreleased]`

---

## Reporting bugs

Please include:
- Android version and device/emulator model
- Steps to reproduce
- Expected vs actual behaviour
- Logcat output (if applicable)
