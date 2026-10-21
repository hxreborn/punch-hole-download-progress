# Punch-hole Download Progress

[English](README.md) | **简体中文**

在摄像头镂空处以动态进度环显示下载进度的 Xposed 模块。

<div align="center">

![Android CI](https://github.com/hxreborn/punch-hole-download-progress/actions/workflows/android-ci.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhxreborn%2Fpunch-hole-download-progress%2Fmain%2Fgradle%2Flibs.versions.toml&query=%24.versions.kotlin&label=Kotlin&color=7F52FF&style=flat&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/API-28%2B-3DDC84?logo=android&logoColor=white)

</div>

<div align="center">
  <img src=".github/assets/demo-pop.gif" alt="下载进度动画" width="320" />
</div>

## 功能

- 摄像头镂空处进度环（基于原生 `DisplayCutout` API），按状态（进行中/完成/失败）独立配置颜色、弧宽、不透明度和方向
- 完成动效，支持可选触觉反馈
- 下载数角标
- [Duo Status Bar](https://github.com/kvmy666/duoStatusBar) 联动：在 Duo 圆环上绘制下载进度（系统 > 集成，需要 Duo [1.4.3-beta.1](https://github.com/kvmy666/duoStatusBar/releases/tag/24-1.4.3-beta.1) 或更新版本）
- 下载速度标签：读取承载下载的网络接口的接收计数（统计整机流量，而非单个下载），以比特或字节每秒显示
- 按旋转方向独立校准文字和角标位置
- 折叠屏支持，折叠/展开姿态分别校准进度环
- 热重载：在支持 libxposed API 102 的框架上无需重启系统界面即可应用模块更新
- 测试模式、省电渲染及药丸形镂空路径支持

## 要求

- Android 9+（API 28）
- 支持 libxposed API 101 的 Xposed 管理器，例如 Vector 2.1 或更新版本（Vector 2.2 起提供的 API 102 支持热重载）
- Root 可选（仅「重启系统界面」功能需要）

## 安装

1. 下载 APK：

   <a href="https://f-droid.org/packages/eu.hxreborn.phdp"><img src=".github/assets/badge_fdroid.png" height="60" alt="在 F-Droid 获取" /></a>
   <a href="https://apt.izzysoft.de/fdroid/index/apk/eu.hxreborn.phdp"><img src=".github/assets/badge_izzyondroid.png" height="60" alt="在 IzzyOnDroid 获取" /></a>
   <a href="../../releases"><img src=".github/assets/badge_github.png" height="60" alt="在 GitHub 获取" /></a>
   <a href="http://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22eu.hxreborn.phdp%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fhxreborn%2Fpunch-hole-download-progress%22%2C%22author%22%3A%22rafareborn%22%2C%22name%22%3A%22Punch-hole%20Download%20Progress%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%7D%22%7D"><img src=".github/assets/badge_obtainium.png" height="60" alt="在 Obtainium 获取" /></a>

2. 在你的 Xposed 管理器中安装并启用模块。
3. 作用域选择 `com.android.systemui`。
4. 重启 SystemUI 或重启设备。

应用溢出菜单内置「重启系统界面」选项。Magisk 会弹出权限请求；KernelSU/APatch 需手动添加应用授权。

## 构建

```bash
git clone https://github.com/hxreborn/punch-hole-download-progress.git
cd punch-hole-download-progress
./gradlew assembleRelease
```

需要 JDK 21 和 Android SDK。配置 `local.properties`：

```properties
sdk.dir=/path/to/android/sdk

# 可选签名配置
RELEASE_STORE_FILE=<path/to/keystore.jks>
RELEASE_STORE_PASSWORD=<store_password>
RELEASE_KEY_ALIAS=<key_alias>
RELEASE_KEY_PASSWORD=<key_password>
```

## 贡献

欢迎提交 Pull Request。如有 Bug 或功能建议，请[提交 Issue](https://github.com/hxreborn/punch-hole-download-progress/issues/new/choose)。

## 收录于

- [Lunaris AOSP](https://github.com/Lunaris-AOSP/frameworks_base/commit/c60d3e785a3261872caa99dc755e6f00a4714ce1)

## 许可证

<a href="LICENSE"><img src=".github/assets/gplv3.svg" height="90" alt="GPLv3"></a>

本项目基于 GNU 通用公共许可证 v3.0 发布，详见 [LICENSE](LICENSE)。
