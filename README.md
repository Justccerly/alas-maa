# alas-maa

将 Alas 的碧蓝航线自动化能力迁移到 MaaFramework 生态，并提供类似 MaaMeow 的 Android 原生运行体验。

## 当前定位

本项目采用以下组合：

- `MaaFwApp`：Android/Kotlin/Compose 宿主和通用控制界面
- `MaaFramework`：截图、输入、识别、Pipeline 和自定义任务扩展
- `Alas Domain`：碧蓝航线地图、任务、寻路、战斗和调度逻辑
- `MaaMeow`：后台虚拟显示、悬浮窗、Shizuku/Root、更新和 Android 生命周期实现参考

本项目不是把 Alas 的 Python 文件逐个翻译成 Kotlin，也不是直接把 MaaMeow 改名为碧蓝航线助手。目标是建立稳定的领域层与平台层边界，让后续 Alas 更新主要表现为资源同步、Pipeline 更新和有限的适配层修改。

## 上游项目

- Alas: https://github.com/lordbernkastel/Alas
- Alas upstream: https://github.com/LmeSzinc/AzurLaneAutoScript
- MaaFramework: https://github.com/MaaXYZ/MaaFramework
- MaaFwApp: https://github.com/Aliothmoon/MaaFwApp
- MaaMeow: https://github.com/Aliothmoon/MAA-Meow

## 当前状态

M0 基线已建立，版本和构建前置条件见 [M0 基线记录](docs/baseline.md)。本地上游源码位于 `upstream/MaaFwApp`，该目录被 `.gitignore` 排除，版本通过 `baseline.json` 锁定。

当前状态：M0 宿主基线已建立并通过 GitHub Actions Debug APK 构建验证；PI V2 资源骨架、ADB 启动游戏任务、Alas Domain 地图路径模块和 `alas-maafw` 适配模块已落盘。适配模块目前可以将识别语义或 JSON 回放转换为 `MapSnapshot`，并把路径结果转换成可序列化的点击计划。真实 MaaFramework OCR/模板回调、设备输入和真机地图流程仍未完成验收。详细进度、已完成项和下一步顺序见 [移植规划](docs/porting-plan.md)。

## 当前开发切片

`alas-maafw` 位于 [alas-maafw](alas-maafw)，保持 Android 和 MaaFramework native handle 不进入领域层：

- `MapRecognitionAdapter`：识别语义 → `MapSnapshot`
- `MapRecognitionJson`：可回放 JSON → 识别语义，支持置信度过滤
- `MapActionPlanner`：路径 → 屏幕点击动作
- `MapActionPlanCodec`：动作计划 → 宿主可消费的 JSON
- `MapActionExecutor`：通过宿主提供的 `TapSink` 执行点击计划
- `MapActionPipelineEncoder`：动作计划 → MaaFramework Pipeline override
- `MapRuntimeTask`：经过校验的 taskName、entry 和 pipelineOverrides 宿主边界
- `MapRouteRuntimePlanner`：路线结果 → 可提交运行任务或可记录的拒绝报告
- `MapRoutePlanner`：识别 JSON → 寻路 → RuntimeTask 规格
- `MapRouteRequestCodec`：完整路线请求 → 可回放 JSON
- `MapRouteReportCodec`：路线结果 → 稳定诊断 JSON
- `MapRouteRequestCodec`：完整路线请求 → 可回放 JSON

识别、动作和完整路线请求回放夹具位于 [resources/replays](resources/replays)，资源和夹具校验由 CI 执行。动作执行通过 `TapSink` 隔离，测试可以使用记录器，Android 宿主再接入实际 MaaFramework 输入通道。宿主当前的远程触摸接口主要服务预览交互，自动任务输入仍需单独确认 MaaFramework action/custom action 的正式入口。当前还没有真实游戏截图，因此动作几何参数仍需通过目标设备截图标定。

MaaFramework 自定义 Action/Recognition 的本地核对结果和接入顺序见 [适配层核对记录](docs/maafw-adapter.md)。动作计划现在可以直接编码为 `MaaTaskerPostTask` 的 Pipeline override，资源包提供 `AlasMapAction` 入口，宿主可在已有 Runner 上以有序 override 提交这个 JSON。

## 开发和验证

在具备 Java 17、Android SDK 和网络访问的环境中，CI 会依次执行资源校验、地图/识别/动作回放校验、`alas-domain` 与 `alas-maafw` 单元测试，并按条件构建 Debug APK。当前 Termux 终端没有 Java，不能在本地执行 Gradle 测试。

本地可运行不依赖 Gradle 的检查：

```bash
./tools/validate-resources.sh
python3 tools/validate-domain-fixtures.py
python3 tools/validate-recognition-fixtures.py
python3 tools/validate-action-fixtures.py
```
