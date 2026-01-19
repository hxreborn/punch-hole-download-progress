

## What's Changed in [v1.0.0-alpha6](https://github.com/hxreborn/punch-hole-download-progress/releases/tag/v1.0.0-alpha6) (2026-01-19)


### Bug Fixes

- [c33791a](https://github.com/hxreborn/punch-hole-download-progress/commit/c33791a220dfa95ee405cbab4a71fce9e5d6fa94) *(ci)* Use plain @mentions for GitHub avatar rendering by @hxreborn

  > Markdown link syntax `[@user](url)` disables GitHub's automatic avatar
  > display. Plain `@username` mentions let GitHub render with avatars.


### Refactor

- [273f8d6](https://github.com/hxreborn/punch-hole-download-progress/commit/273f8d68e91eacd1bfe0eac0a936325b646db6e5) *(util)* Improve root detection with lazy shell by @hxreborn

  > Use whoami check instead of Shell.isAppGrantedRoot(). Shell instance
  > reused and cleared on failure for retry. Check root before restart.



## What's Changed in [v1.0.0-alpha5](https://github.com/hxreborn/punch-hole-download-progress/releases/tag/v1.0.0-alpha5) (2026-01-19)


### Features

- [8a2611a](https://github.com/hxreborn/punch-hole-download-progress/commit/8a2611ab3b859991bd5ca3078d96a65d0b3fdbe2) *(ui)* Add splash screen with icon to avoid flashbang on cold start by @hxreborn


### Refactor

- [46219ce](https://github.com/hxreborn/punch-hole-download-progress/commit/46219cea8cd6ceb9df4edb9324e05ffe652a8cb0) *(ci)* Switch to ratatui-style changelog template by @hxreborn

- [8973945](https://github.com/hxreborn/punch-hole-download-progress/commit/8973945cd04fcd7d572a7c171490bdf2e65fbb0b) *(prefs)* Simplify state updates and reset defaults by @hxreborn

  > Collapse updatePrefsStateForKey to full read, add centralized DEFAULTS
  > map, remove unused serviceState LiveData.

- [58f98e1](https://github.com/hxreborn/punch-hole-download-progress/commit/58f98e1709f68db8bab3cc303a7b038fa1aaaf51) *(prefs)* Deduplicate preference mapping by @hxreborn

- [4baed56](https://github.com/hxreborn/punch-hole-download-progress/commit/4baed560a111689b068f3be4a028de488e3868e0) *(prefs)* Add shared preference reader extensions by @hxreborn

  > Centralize key, default, and coercion logic for reuse.

- [d4a6b68](https://github.com/hxreborn/punch-hole-download-progress/commit/d4a6b68227a8ce705df8816305c6b618ca33a74b) *(prefs)* Remove unused finishIntensity setting by @hxreborn

  > Always use high intensity (1.5f). Raise MIN_RING_GAP to 1.0f since
  > values below 1.0 make the ring invisible.

- [3cf544c](https://github.com/hxreborn/punch-hole-download-progress/commit/3cf544c895792d5e299d5aa932e78e03a8a2cd11) *(ui)* Standardize preference components by @hxreborn

  > Move preference components to dedicated package with unified styling
  > matching compose-preference library. Uses Tokens for consistent padding,
  > typography, and disabled state handling.

  > Components:TogglePreferenceWithIcon, SliderPreferenceWithReset,
SelectPreference, ColorPreference, ActionPreference.

- [0982fa7](https://github.com/hxreborn/punch-hole-download-progress/commit/0982fa7549cf907a78902d4fd4b01e2449cffb14) *(util)* Migrate root commands to libsu by @hxreborn

- [3e49b64](https://github.com/hxreborn/punch-hole-download-progress/commit/3e49b64f07607ffdbc7dac851c8e55aac75f733c) *(view)* Remove indistinct finish styles by @hxreborn

  > hold_fade, pulse, shine_sweep are visually indistinct from each other.
  > Keep only: snap, pop, segmented.


### Documentation

- [38620f9](https://github.com/hxreborn/punch-hole-download-progress/commit/38620f9d9263de8a3c32bb3a44ed080b29f93bce) *(changelog)* Update for v1.0.0-alpha5 by @hxreborn


### Miscellaneous

- [541d733](https://github.com/hxreborn/punch-hole-download-progress/commit/541d73365a402b128deb7e85a270a9117f95deb3) Ignore local asset directories by @hxreborn



## What's Changed in [v1.0.0-alpha4](https://github.com/hxreborn/punch-hole-download-progress/releases/tag/v1.0.0-alpha4) (2026-01-18)


### Features

- [bfa64bf](https://github.com/hxreborn/punch-hole-download-progress/commit/bfa64bff0c1b23a73c20875227887eefb4b58d78) *(ui)* Add version info section in settings by @hxreborn

  > Display version name and code in a new About section
  > on the System screen.


### Bug Fixes

- [59c32a6](https://github.com/hxreborn/punch-hole-download-progress/commit/59c32a6a7ab5a6ac66c85f4e0fe9002e823b51a0) *(ci)* Use dynamic contributor from GitHub API by @hxreborn

  > Show contributor only when API returns username, with proper
  > profile link. Works in Actions with GITHUB_TOKEN.


### Refactor

- [031df0f](https://github.com/hxreborn/punch-hole-download-progress/commit/031df0f86b879d9fbf5afa78230f9301047539f6) *(ci)* Simplify changelog contributor format by @hxreborn

  > - Remove "commit" prefix, show "(by @user)"
  > - Group commits by scope within each type
  > - Add editorconfig rules for toml/yaml/md

- [2314a4e](https://github.com/hxreborn/punch-hole-download-progress/commit/2314a4ea693b0a2cd249dca3b9d2e8ff04142966) *(ci)* Include version in changelog header by @hxreborn

- [b0f920b](https://github.com/hxreborn/punch-hole-download-progress/commit/b0f920bcf067a0283ef3e65af9e5db4155a0b040) *(ci)* Distinguish merged PRs from direct commits by @hxreborn

  > Show "(merged by @user in #PR)" for PRs, "(commit by @user)" for
  > direct commits.

- [e3e2cc3](https://github.com/hxreborn/punch-hole-download-progress/commit/e3e2cc349b08d6e81b883be09505f25a563a8d83) *(ci)* Show commit hash first in changelog entries by @hxreborn

  > Format:`hash` - message (commit by @user)


### Documentation

- [9538d08](https://github.com/hxreborn/punch-hole-download-progress/commit/9538d08d0c81331902bad0e881c6d7cb1205a660) Add LSPosed Next as alternative framework by @hxreborn



## What's Changed in [v1.0.0-alpha3](https://github.com/hxreborn/punch-hole-download-progress/releases/tag/v1.0.0-alpha3) (2026-01-18)


### Miscellaneous

- [92a997a](https://github.com/hxreborn/punch-hole-download-progress/commit/92a997ad199a113bf723587f753cf500376f2da0) *(ci)* Update git-cliff to github-keepachangelog template by @hxreborn in [#1](https://github.com/hxreborn/punch-hole-download-progress/pull/1)

  > - Use built-in github-keepachangelog template
  > - Add commit hash links and author attribution
  > - Group commits by conventional type (Added, Fixed, Changed, etc.)
  > - Remove header/footer, use "What's New" section title



## What's Changed in [v1.0.0-alpha2](https://github.com/hxreborn/punch-hole-download-progress/releases/tag/v1.0.0-alpha2) (2026-01-18)


### Features

- [b497438](https://github.com/hxreborn/punch-hole-download-progress/commit/b497438c6948c6eaec5ea29984f8fa652cd68e99) *(ci)* Add git-cliff changelog generation by @hxreborn

- [9080a95](https://github.com/hxreborn/punch-hole-download-progress/commit/9080a95b79cebdcdcf2b946f0602efdc09ca4767) Update app icon by @hxreborn


### Bug Fixes

- [581411a](https://github.com/hxreborn/punch-hole-download-progress/commit/581411ad31960b3e4e036344dd66d6789b3567fe) *(build)* Replace deprecated resourceConfigurations with localeFilters by @hxreborn

- [4acc60a](https://github.com/hxreborn/punch-hole-download-progress/commit/4acc60ae333956f2b662d8250d080b4364e6f33d) *(ci)* Use git-cliff GitHub template with API integration by @hxreborn

- [fa4286f](https://github.com/hxreborn/punch-hole-download-progress/commit/fa4286f16b17c9d316f20488220b42e0c12e2e46) *(ci)* Use git-cliff without GitHub API integration by @hxreborn

- [29dc91a](https://github.com/hxreborn/punch-hole-download-progress/commit/29dc91a3ac47b45b5436b727f85aae6f5ad7d4d2) *(ci)* Build both libxposed api and service in workflows by @hxreborn


### Refactor

- [4fc8759](https://github.com/hxreborn/punch-hole-download-progress/commit/4fc8759e5fc8a9441f1e3236a0caa55a27af304d) *(hook)* Import Method class directly by @hxreborn


### Documentation

- [11b867c](https://github.com/hxreborn/punch-hole-download-progress/commit/11b867c4dea42380d8f85ffb131cb3856ee688b2) Add GPLv3 license file by @hxreborn


### Miscellaneous

- [1562d0b](https://github.com/hxreborn/punch-hole-download-progress/commit/1562d0b6ef07761b3fb2868eee5399982b834a97) Update app name by @hxreborn



## What's Changed in [v1.0.0-alpha](https://github.com/hxreborn/punch-hole-download-progress/releases/tag/v1.0.0-alpha) (2026-01-18)


### Features

- [f2ecafd](https://github.com/hxreborn/punch-hole-download-progress/commit/f2ecafdf9a806a24e3ac123f4474abed058a8a06) *(ui)* Add option to show filename label by @hxreborn

- [6143ca9](https://github.com/hxreborn/punch-hole-download-progress/commit/6143ca9bf395a282e35003ccbbc7bd215a5ea4c1) Add semantic git versioning and CI workflows by @hxreborn

  > - versionCode = MAJOR × 10,000 + commit count
  > - versionName from git describe, fallback to 0.0.0-dev
  > - APK naming: phdp-v{version}-{buildType}.apk
  > - build.yml: release workflow on v* tags (pre-release)
  > - android-ci.yml: CI on push/PR to main


### Bug Fixes

- [54a7891](https://github.com/hxreborn/punch-hole-download-progress/commit/54a7891e673f262e4179b455f311d2b0f8731568) *(ci)* Add libxposed build step before app build by @hxreborn

- [8117852](https://github.com/hxreborn/punch-hole-download-progress/commit/8117852b5786931b341ce6d78d93ef55572e356e) *(ui)* Disable overscroll stretch on System screen by @hxreborn

- [8c07f2e](https://github.com/hxreborn/punch-hole-download-progress/commit/8c07f2ee0df5c0418c7f92826a519d8ba8bbc9fd) *(version)* Include commit hash when no semver tag by @hxreborn

- [5101591](https://github.com/hxreborn/punch-hole-download-progress/commit/5101591e33b32d576c2b1d90568afe6287e015b3) Avoid log() shadowing in XposedModule by @hxreborn


### Refactor

- [99fb1ad](https://github.com/hxreborn/punch-hole-download-progress/commit/99fb1adfcdfa3be27a7c3a5567e1531778022f0a) *(ui)* Use Material You icons for switch thumbs by @hxreborn

- [3d4a2e0](https://github.com/hxreborn/punch-hole-download-progress/commit/3d4a2e02b93a65888c527c94ca34defafcad28fe) *(ui)* Introduce RowPosition for shape math by @hxreborn

- [5583aec](https://github.com/hxreborn/punch-hole-download-progress/commit/5583aec4fa6b1bc35796dd6796866a9fc2498242) *(ui)* Use grouped-list layout for settings by @hxreborn

- [b0369ec](https://github.com/hxreborn/punch-hole-download-progress/commit/b0369ec7ae24ddf5b0eba800cbdd19c171c977b5) *(ui)* Match spacing to Android Settings density by @hxreborn

- [e4344eb](https://github.com/hxreborn/punch-hole-download-progress/commit/e4344ebbdfcc52041857c1d57ea1ed245178f851) *(ui)* Add Material 3 tokens and SettingsGroup by @hxreborn

- [f5c11d0](https://github.com/hxreborn/punch-hole-download-progress/commit/f5c11d0bd4a989f6690d92908b8d44a91cf54f29) *(version)* Use raw git describe output by @hxreborn

- [305a049](https://github.com/hxreborn/punch-hole-download-progress/commit/305a0498d8ce8bf0e941dfcdc64883fb42ddee9d) Rename PunchHoleDownloadProgress* to PunchHoleProgress* by @hxreborn

- [0ada881](https://github.com/hxreborn/punch-hole-download-progress/commit/0ada881964050b4cad15027741acda9528430e48) Rename PHPM to PHDP by @hxreborn

- [51a43cc](https://github.com/hxreborn/punch-hole-download-progress/commit/51a43cca16ed938d161514c40dae0374154c1cd9) Remove dead code and simplify haptic prefs by @hxreborn


### Documentation

- [a4b82aa](https://github.com/hxreborn/punch-hole-download-progress/commit/a4b82aa2d33a88b65fc5da6ac46c4cc50fc7fcc6) *(readme)* Refresh tagline and feature list by @hxreborn


### Performance Improvements

- [2f30f11](https://github.com/hxreborn/punch-hole-download-progress/commit/2f30f116c83684f45f771382382684537ef0029d) *(release)* Shrink APK 4.2MB to 1.8MB by @hxreborn

- [2c59d63](https://github.com/hxreborn/punch-hole-download-progress/commit/2c59d63fc9817bcbecede7aea846035f7827e34b) Cache reflection Method objects by @hxreborn


### Miscellaneous

- [d8463c2](https://github.com/hxreborn/punch-hole-download-progress/commit/d8463c286ba5c24ad15278bc6bcca993364fe654) *(proguard)* Keep LSPosed entries, strip debug logs by @hxreborn


### Init

- [4f849e0](https://github.com/hxreborn/punch-hole-download-progress/commit/4f849e0dc6e93698fc84adddf8708bdcc9c7e316) LSPosed module scaffold with Compose UI by @hxreborn


### New Contributors

* @hxreborn made their first contribution


