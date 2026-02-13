# Punch-hole Download Progress

Xposed module that displays download progress as an animated ring around the camera cutout.

![Android CI](https://github.com/hxreborn/punch-hole-download-progress/actions/workflows/android-ci.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/API-28%2B-3DDC84?logo=android&logoColor=white)

<div align="center">
  <img src=".github/assets/demo-pop.gif" alt="Download progress animation" width="320" />
</div>

## Features

- Progress ring rendered around the camera cutout using the native `DisplayCutout` API
- Path renderer for pill-shaped cutouts (opt-in)
- Customizable appearance: colors per state (active/completed/failed), arc thickness, opacity, and direction
- Completion animations and optional haptic feedback
- Active download counter badge
- Battery saver-aware rendering
- Built-in test mode for simulating states
- Theme preference: light, dark, or follow system with Material You dynamic colors
- Material 3 Expressive settings UI with Jetpack Compose

## Requirements

- Android 9 (API 28) or higher (experimental on Android 9-11)
- [LSPosed](https://github.com/JingMatrix/LSPosed) (JingMatrix fork recommended)
- Pixel/AOSP-based ROM (other ROMs may not work as expected)
- Root access (optional, for `Restart SystemUI` in settings)
- A phone with a punch-hole display

## Installation

1. Download the APK:

   <a href="https://apt.izzysoft.de/fdroid/index/apk/eu.hxreborn.phdp"><img src=".github/assets/badge_izzyondroid.png" height="60" alt="Get it on IzzyOnDroid" /></a>
   <a href="../../releases"><img src=".github/assets/badge_github.png" height="60" alt="Get it on GitHub" /></a>
   <a href="http://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22eu.hxreborn.phdp%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fhxreborn%2Fpunch-hole-download-progress%22%2C%22author%22%3A%22rafareborn%22%2C%22name%22%3A%22Punch-hole%20Download%20Progress%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%7D%22%7D"><img src=".github/assets/badge_obtainium.png" height="60" alt="Get it on Obtainium" /></a>

2. Install and enable the module in LSPosed.
3. Scope to `com.android.systemui`
4. Restart SystemUI or reboot the device

The app includes a built-in `Restart SystemUI` option in the overflow menu. Magisk will prompt for permission; KernelSU/APatch require adding the app manually.

## Build

```bash
git clone --recurse-submodules https://github.com/hxreborn/punch-hole-download-progress.git
cd punch-hole-download-progress
./gradlew buildLibxposed
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

## License

<a href="LICENSE"><img src=".github/assets/gplv3.svg" height="90" alt="GPLv3"></a>

This project is licensed under the GNU General Public License v3.0 – see the [LICENSE](LICENSE) file for details.
