# M0 基线记录

## 已固定版本

| 组件 | 版本 | 本地位置 |
|---|---|---|
| MaaFwApp | `v0.1.0` / `5f5871095fde7a0af5a0171145f3c84d3e2eac18` | `upstream/MaaFwApp` |
| MaaFramework | `v5.10.5` | arm64 native libraries 已部署到 `upstream/MaaFwApp/app/src/main/jniLibs/arm64-v8a` |
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
python3 scripts/setup_maa_framework.py --tag v5.10.5 --abi arm64-v8a
./gradlew :app:assembleDebug
```

完整 Android 构建还需要 Android SDK、Android build tools、MaaFramework Android release `.so` 和可访问 GitHub Release 的网络。

## 构建结果

- MaaFramework v5.10.5 arm64 release 已下载、ZIP 校验并部署成功。
- GitHub Actions 最新运行 `36283361482` 在 `ubuntu-24.04` x86_64 runner 上完成资源校验、native 部署、`assembleDebug` 和 APK 上传。
- Debug APK 产物 `alas-maa-host-baseline-debug` 已生成，Artifact ID 为 `10919283791`。
- 本地 Windows x86_64 主机已完成 `:app:assembleDebug`（Java 17 + Android SDK 36 + NDK 28），产物约 103 MB。此前记录的「本地不能构建」针对的是 `aarch64` 且缺少 Java 的 Termux 终端，不再是当前约束。
- Project Interface V2 资源骨架和地图回放夹具已通过 CI 校验。
- `pi-profile.yaml` 已接入宿主构建，`PI_PROFILE=... ./gradlew :app:syncPiAssets` 在本地成功完成。
- `alas-domain` 和 `alas-maafw` 已通过 `tools/integrate-domain-module.sh` 接入宿主构建；CI 每次 push 运行 `:app:compileDebugKotlin` 与两个模块的单元测试。

## 真机验收

在 MuMu Player 15.0（Android 15 / API 35，`arm64-v8a` 经 houdini 转译）上完成首轮验收。

已确认：

- 安装与启动：进程存活，`MainActivity` 正常渲染，无崩溃。
- MaaFramework native 加载：`[BOOT] MAA_LOAD_OK v5.10.5`，`MaaFramework loaded: v5.10.5`。
- PI 资源：`assets/pi.zip` 打包进 APK，解包到应用外部 `files/pi`，`interface.json` 与各资源目录齐全。
- PI 解析：宿主任务目录正确渲染出 `tasks/startup.json` 的 4 个任务（启动碧蓝航线 / 资源加载检查 / 地图动作计划入口 / 地图识别回放）。
- 任务执行：`资源加载检查` 完整跑到 `MaaEventCallback on Tasker.Task.Succeeded`；控制器创建成功（`Controller.Action.Succeeded`），虚拟显示创建成功（`VD created: 1280x720`）。
- 启动碧蓝航线：可正常打开游戏。
- 后台模式与全屏点击：正常。

本轮同时暴露并修复了基线自身的版本矛盾：MaaFwApp `v0.1.0` 调用 `MaaAndroidNativeControllerCreate`，而该符号在原先锁定的 MaaFramework `v5.9.2` 中不存在，导致真机运行任务时 `UnsatisfiedLinkError`。核对与修复过程见 [适配层核对记录](maafw-adapter.md)。

## 尚未验收

以下项目仍需真实设备或后续设备测试：

- Shizuku/Root 状态检测（当前测试环境未安装 Shizuku，也无 `su`）
- MaaFramework 模板识别和 OCR（真实截图上的算法）
- 任务取消
- 碧蓝航线实际画面上的地图识别、寻路与后续 Pipeline
- 真机地图流程与动作几何参数标定
