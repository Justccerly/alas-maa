# M0 基线记录

## 已固定版本

| 组件 | 版本 | 本地位置 |
|---|---|---|
| MaaFwApp | `v0.1.0` / `5f5871095fde7a0af5a0171145f3c84d3e2eac18` | `upstream/MaaFwApp` |
| MaaFramework | `v5.9.2` | arm64 native libraries已部署到 `upstream/MaaFwApp/app/src/main/jniLibs/arm64-v8a` |
| Android 最低版本 | API 28 | Android 9+ |
| Java | 17 | 当前环境已检测到 OpenJDK 17 |

## 基线结论

`MaaFwApp v0.1.0` 已具备本项目需要的宿主能力：

- Kotlin + Jetpack Compose Android GUI
- Project Interface V2 资源装载
- 前台/后台运行模式
- 虚拟显示和悬浮控制
- Shizuku/Root 特权进程
- MaaFramework runner、日志和任务配置
- CMake native bridge

因此第一阶段不重新实现 Android 宿主，而是在该基线上接入 Alas 资源和领域逻辑。

## 构建入口

在 `upstream/MaaFwApp` 中执行：

```bash
python3 scripts/setup_maa_framework.py --tag v5.9.2 --abi arm64-v8a
./gradlew :app:assembleDebug
```

完整 Android 构建还需要：

- Android SDK
- Android build tools
- MaaFramework Android release `.so`
- 能访问 GitHub Release 下载地址的网络

## 当前构建结果

- MaaFramework v5.9.2 arm64 release 已下载、ZIP 校验并部署成功。
- Gradle 9.4.1 已下载，Android SDK API 37、Build Tools 36 和 NDK 28.2 已安装。
- Gradle 依赖在线解析已通过。
- `assembleDebug` 在 `:app:compileDebugAidl` 阶段失败，原因是当前主机为 `aarch64`，SDK 的 `aidl` 是 `x86_64` ELF，无法启动。
- GitHub Actions 工作流已加入，使用 `ubuntu-24.04` x86_64 runner 绕过该主机限制。

## 当前阻塞

本地终端无法执行 x86_64 Android SDK 工具，导致 APK 构建无法在本机完成。使用 GitHub Actions 的 x86_64 runner 可以绕过该限制。

本地复现命令仍为：

```bash
cd upstream/MaaFwApp
python3 scripts/setup_maa_framework.py --tag v5.9.2 --abi arm64-v8a
./gradlew :app:assembleDebug
```

验证成功后，将 `baseline.json` 中的 `nativeArtifacts` 更新为 `ready`，并记录 APK 产物与构建信息。
