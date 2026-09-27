# GitHub 构建说明
本地 Android 构建在当前开发终端无法完成：终端架构为 `aarch64`，Android SDK 的 `aidl` 工具为 `x86_64`，没有可用的 x86 模拟器。MaaFramework native 库和 Gradle 依赖已经可以准备，但 AIDL 工具无法启动。
仓库提供了 GitHub Actions 工作流：
- 文件：`.github/workflows/android-baseline.yml`
- runner：`ubuntu-24.04`，x86_64
- Java：17
- Android：API 36、Build Tools 36、NDK 28.2.13676358
- MaaFwApp：`v0.1.0`，CI 临时将其 `compileSdk` 从 37 调整为 36，并跳过当前 runner 无法满足的 AAR metadata 检查；API 37 可用后应恢复严格构建
- MaaFramework：`v5.9.2`
- 架构：`arm64-v8a`
- 产物：`alas-maa-host-baseline-debug`

CI 分为两层：

- 每次 push / Pull Request：只运行资源 JSON、Project Interface 和地图回放夹具校验，通常约几分钟内完成。
- APK 构建：仅在 GitHub Actions 页面手动执行 `workflow_dispatch`，或提交信息包含 `[build-apk]` 时执行。只有需要安装测试、宿主改动或里程碑验收时才触发。

手动构建完成后，产物名为 `alas-maa-host-baseline-debug`。
