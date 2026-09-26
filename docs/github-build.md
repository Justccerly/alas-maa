# GitHub 构建说明

本地 Android 构建在当前开发终端无法完成：终端架构为 `aarch64`，Android SDK 的 `aidl` 工具为 `x86_64`，没有可用的 x86 模拟器。MaaFramework native 库和 Gradle 依赖已经可以准备，但 AIDL 工具无法启动。

仓库提供了 GitHub Actions 工作流：

- 文件：`.github/workflows/android-baseline.yml`
- runner：`ubuntu-24.04`，x86_64
- Java：17
- Android：API 37、Build Tools 36、NDK 28.2.13676358
- MaaFwApp：`v0.1.0`
- MaaFramework：`v5.9.2`
- 架构：`arm64-v8a`
- 产物：`alas-maa-host-baseline-debug`

绑定 GitHub 远程仓库后，推送到 `main` 或创建 Pull Request 即会触发构建。当前本地仓库没有 remote，也没有发现可用于推送的 SSH key，因此这里只能先提交工作流，不能代替用户完成远程绑定和推送。
