# Punch-hole Download Progress

**[English](README.md) | [简体中文](README.zh-CN.md)**

Xposed module that renders download progress as an animated ring around the camera cutout.

<div align="center">

![Android CI](https://github.com/hxreborn/punch-hole-download-progress/actions/workflows/android-ci.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/API-28%2B-3DDC84?logo=android&logoColor=white)

</div>

<div align="center">
  <img src=".github/assets/demo-pop.gif" alt="Download progress animation" width="320" />
</div>

## Features

- Progress ring around the camera cutout (via native `DisplayCutout` API) with per-state color, thickness, opacity, and direction (active/completed/failed)
- Completion animations with optional haptic feedback
- Download counter badge
- Per-rotation calibration for text and badge offsets
- Material 3 Expressive settings UI
- Test mode, battery saver rendering, and pill-cutout path support

## Requirements

- Android 9+ (API 28)
- Xposed Manager with API 101 support (official LSPosed recommended)
- Root optional (only needed for `Restart SystemUI`)

## Installation

1. Download the APK:

   <a href="https://f-droid.org/packages/eu.hxreborn.phdp"><img src=".github/assets/badge_fdroid.png" height="60" alt="Get it on F-Droid" /></a>
   <a href="https://apt.izzysoft.de/fdroid/index/apk/eu.hxreborn.phdp"><img src=".github/assets/badge_izzyondroid.png" height="60" alt="Get it on IzzyOnDroid" /></a>
   <a href="../../releases"><img src=".github/assets/badge_github.png" height="60" alt="Get it on GitHub" /></a>
   <a href="http://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22eu.hxreborn.phdp%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fhxreborn%2Fpunch-hole-download-progress%22%2C%22author%22%3A%22rafareborn%22%2C%22name%22%3A%22Punch-hole%20Download%20Progress%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%7D%22%7D"><img src=".github/assets/badge_obtainium.png" height="60" alt="Get it on Obtainium" /></a>

2. Install and enable the module in LSPosed.
3. Scope to `com.android.systemui`
4. Restart SystemUI or reboot the device

The app includes a built-in `Restart SystemUI` option in the overflow menu. Magisk will prompt for permission; KernelSU/APatch require adding the app manually.

## Build

```bash
git clone https://github.com/hxreborn/punch-hole-download-progress.git
cd punch-hole-download-progress
./gradlew assembleRelease
```

Requires JDK 21 and Android SDK. Configure `local.properties`:

```properties
sdk.dir=/path/to/android/sdk

# Optional signing
RELEASE_STORE_FILE=<path/to/keystore.jks>
RELEASE_STORE_PASSWORD=<store_password>
RELEASE_KEY_ALIAS=<key_alias>
RELEASE_KEY_PASSWORD=<key_password>
```

## Contributing

Pull requests welcome. [Open an issue](https://github.com/hxreborn/punch-hole-download-progress/issues/new/choose) for bugs or feature requests.

## Also found in

- [Lunaris AOSP](https://github.com/Lunaris-AOSP/frameworks_base/commit/c60d3e785a3261872caa99dc755e6f00a4714ce1)

## License

<a href="LICENSE"><img src=".github/assets/gplv3.svg" height="90" alt="GPLv3"></a>

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.
