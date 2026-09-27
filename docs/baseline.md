# M0 基线记录

## 已固定版本

| 组件 | 版本 | 本地位置 |
|---|---|---|
| MaaFwApp | `v0.1.0` / `5f5871095fde7a0af5a0171145f3c84d3e2eac18` | `upstream/MaaFwApp` |
| MaaFramework | `v5.9.2` | arm64 native libraries 已部署到 `upstream/MaaFwApp/app/src/main/jniLibs/arm64-v8a` |
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

完整 Android 构建还需要 Android SDK、Android build tools、MaaFramework Android release `.so` 和可访问 GitHub Release 的网络。

## 构建结果

- MaaFramework v5.9.2 arm64 release 已下载、ZIP 校验并部署成功。
- GitHub Actions 最新运行 `36283361482` 在 `ubuntu-24.04` x86_64 runner 上完成资源校验、native 部署、`assembleDebug` 和 APK 上传。
- Debug APK 产物 `alas-maa-host-baseline-debug` 已生成，Artifact ID 为 `10919283791`，大小约 53 MB。
- 本地仍不能构建，原因是当前主机为 `aarch64`，SDK 的 `aidl` 是 `x86_64` ELF。
- Project Interface V2 资源骨架和地图回放夹具已通过 CI 校验。
- `pi-profile.yaml` 已接入宿主构建，`PI_PROFILE=... ./gradlew :app:syncPiAssets` 在本地成功完成，生成资源目录包含 `interface.json`、任务和 Pipeline。
- 下一轮 CI 将验证带资源 APK 的完整构建。

## 尚未验收

以下项目需要真实 Android 设备或后续设备测试：

- Shizuku/Root 状态检测
- 截图、点击和滑动
- MaaFramework 模板识别和 OCR
- 后台虚拟显示与任务取消
- 碧蓝航线实际画面上的 `AlasStartupCheck` Pipeline
