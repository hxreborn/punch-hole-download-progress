# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OrbitX (package: `eu.hxreborn.phdp`) is an Android Xposed module that displays download progress as an animated ring around the camera punch-hole cutout. It has two runtime contexts: a settings app (Compose UI) and SystemUI hooks (Xposed).

## Build Commands

```bash
# Build debug APK (includes ktlint check)
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Run ktlint check only
./gradlew ktlintCheck

# Auto-format with ktlint
./gradlew ktlintFormat

# Full clean build
./gradlew clean assembleDebug
```

APK output: `app/build/outputs/apk/{debug,release}/phdp-v{version}-{buildType}.apk`

## Architecture

**Two-process design:**
- **Settings app** (`ui/`): Jetpack Compose UI for configuring the module. MVVM with `SettingsViewModel` + reactive `StateFlow<SettingsUiState>`.
- **Xposed hooks** (`xposed/`): Injected into SystemUI via libxposed. `PHDPModule` is the entry point; `SystemUIHooker` and `DownloadProgressHooker` do the actual hooking. `IndicatorView` renders the progress ring.

**Data flow:** Preferences are stored via `PrefsRepository` (SharedPreferences-backed) and bridged across processes using libxposed's remote preferences (`PrefsManager`). All pref keys are defined in `prefs/Prefs.kt` as typed `PrefSpec<T>` objects.

**Navigation:** Uses Navigation3 (`navigation3-runtime`/`navigation3-ui`) with `@Serializable` route objects. Bottom nav has 4 tabs: Design, Motion, Packages, System.

**No DI framework** -- uses manual factory pattern (`SettingsViewModelFactory`).

## Code Style

- **ktlint** runs as a `preBuild` dependency (build fails on violations)
- Code style: `ktlint_official`
- Max line length: 100 (general), 140 (`ui/` package), 120 (`.kts` files)
- Trailing commas: enabled
- `@Composable` functions are exempt from function naming rules

## Key Conventions

- Version is managed in `gradle.properties` (`version.code`, `version.name`), updated by `release.sh` and parsed by F-Droid
- JDK 21 required (both compile and toolchain)
- Compile/target SDK 36, min SDK 28
- ProGuard rules in `app/proguard-rules.pro` keep Xposed entry points, hook classes, and the remote prefs manager
- English-only locale filter
- No unit/instrumentation tests in this project
