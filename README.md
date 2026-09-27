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

当前状态：M0 宿主基线已建立并通过 GitHub Actions Debug APK 构建验证；PI V2 资源骨架、ADB 启动游戏任务和已接入宿主的 Alas Domain 地图路径模块与识别语义快照映射已落盘。真机控制、截图、模板识别和 OCR 尚未完成验收。
