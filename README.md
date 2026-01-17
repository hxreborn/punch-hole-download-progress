# Punch-Hole Progress Monitor

LSPosed module that displays download progress as an animated ring around the camera cutout. Hooks into SystemUI notifications to track downloads from browsers and the system download manager.

![Android CI](https://github.com/user/orbit/actions/workflows/android.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/API-31%2B-3DDC84?logo=android&logoColor=white)

<div align="center">
  <img src=".github/assets/orbit-preview.png" alt="Download progress ring around camera cutout" width="320" />
</div>

## Features

- Progress ring follows cutout shape (punch-hole, pill, or custom paths)
- Tracks downloads from Chrome, Firefox, Brave, Samsung Browser, and 30+ other browsers
- Configurable colors, stroke width, opacity, and animation styles
- Multiple completion effects: shine sweep, pop, pulse, segmented cascade
- Optional haptic feedback on download completion
- Power saver integration (dim or disable during battery saver)
- Percentage text display with configurable position

## Requirements

- Android 12 (API 31) or higher
- [LSPosed](https://github.com/JingMatrix/LSPosed) (JingMatrix fork recommended)
- Device with camera cutout (punch-hole or pill-shaped)

## Installation

1. Install and enable the module in LSPosed.
2. Configure the scope:
   - `com.android.systemui` – Shows the download progress ring overlay.
3. Reboot your device.

## Build

```bash
git clone --recurse-submodules https://github.com/user/orbit.git
cd orbit
./gradlew buildLibxposedApi
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

## License

<a href="LICENSE"><img src=".github/assets/gplv3.svg" height="90" alt="GPLv3"></a>

This project is licensed under the GNU General Public License v3.0 – see the [LICENSE](LICENSE) file for details.
