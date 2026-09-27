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

截至最新运行 `36283361482`，CI 已通过 Project Interface 资源校验、地图回放夹具校验、宿主构建和 APK 上传。产物约 53 MB。该结果验证的是宿主构建链和资源输入，不代表真机截图、输入或 OCR 已验收。

资源包校验由 `tools/validate-resources.sh` 执行；`pi-profile.yaml` 已接入 MaaFwApp，使用 `PI_PROFILE=... ./gradlew :app:syncPiAssets` 可生成包含当前资源的构建输入。带资源 APK 已由最新 CI 运行验证。
