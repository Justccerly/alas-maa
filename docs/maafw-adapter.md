# MaaFramework 适配层核对记录

## 已确认的运行时事实

CI/本地构建会运行 `tools/integrate-maafw-recognition.sh` 和 `tools/check-maafw-recognition-integration.sh`，向 MaaFwApp 注入并检查已核对的 JNA 声明、回调实现和资源注册调用。

本地 MaaFramework `v5.10.5` arm64 库导出了以下能力：

- `MaaResourceRegisterCustomAction`
- `MaaResourceRegisterCustomRecognition`
- `MaaContextRunAction`
- `MaaContextRunRecognition`
- `MaaTaskerPostAction`
- `MaaTaskerPostRecognition`

`MaaFwApp` 的 JNA 声明和 `MaaRunner` 现在通过集成脚本暴露并注册 v5.10.5 Custom Recognition；自动点击继续通过 MaaFramework Pipeline `Click` 进入控制器，预览用的 `RemoteService.touchDown/touchUp` 不属于自动任务接口。

## 当前边界

`alas-maafw` 保持平台无关，只输出：

- `MapSnapshot`
- `MapActionPlan`
- `TapSink` 执行边界

动作计划还可以通过 `MapActionPipelineEncoder` 编码为 Pipeline override。资源包预置了 `AlasMapAction` 入口节点；编码器复用该入口并追加 `AlasMapAction.1`、`AlasMapAction.2` 等后续 Click 节点。宿主应把编码结果作为该任务的有序 `pipelineOverrides` 项提交，不能把它当作新的 Task entry。

`resources/replays/map-action-plan.json` 同时保存预期的 Pipeline 节点链，CI 会重新生成并比较该结构，检查入口、固定坐标、延迟和 `next` 连续性。

宿主接线时优先使用 `MapActionPipelineEncoder.encodeOverrides`，它直接返回 MaaFwApp `RuntimeTask.pipelineOverrides` 所需的 `List<JsonObject>`；只有跨 native/JNA 边界时才使用字符串形式的 `encode`。

如果调用方只需要一个可传递的中间值，使用 `MapActionRuntimeSpecFactory.fromPlan`，它返回 `entry` 和 `pipelineOverrides`，不引入 MaaFwApp 的 Android 类型。

宿主组装运行计划时可以调用 `MapActionRuntimeSpec.asRuntimeTask(taskName)`，得到经过校验的 `MapRuntimeTask`。这个值与 MaaFwApp `RuntimeTaskPayload` 的字段一一对应，宿主只需在自己的线格式转换处复制字段，不需要让 Android 类型反向进入适配模块。

如果输入是完整的 `MapRouteRequest`，使用 `MapRouteRuntimePlanner`。成功时返回包含 `taskName`、`entry` 和有序 `pipelineOverrides` 的 `MapRuntimeTask`；不可达时返回稳定的路线报告 JSON，调用方不应把不可达结果转换成空任务提交。

`MapRoutePlanner` 的结果还带有 `MapRouteDiagnostics`，可直接写入运行日志：识别覆盖数量、未知/阻挡格数量、舰队位置、目标、移动点、路径成本和点击数都在其中。

使用 `MapRouteReportCodec.encode` 可将成功路线或不可达原因编码为稳定 JSON，适合随运行日志保存。

使用 `MapRouteRequestCodec` 可以保存完整规划输入：识别地图、舰队位置、目标、屏幕网格几何、置信度阈值和敌方格策略都在同一份 JSON 中。

示例请求见 `resources/replays/map-route-request.json`。

下一步接入需要在宿主或 native bridge 中完成：

1. 在 `MaaResource` 创建后、加载 bundle 前注册 v5.10.5 Custom Recognition callback。
2. 在 callback 中运行实际的地图格识别算法，并将结果按 `MaaMapRecognitionCallbackCodec` 的 schema 写入 `out_detail`、将地图屏幕边界写入 `out_box`。
3. 将 callback 产生的地图结果交给路线规划器，再把结果任务接到 `MaaFwApp` 的 `RuntimeTaskPayload`。
4. 用真实截图回放验证格子分类、舰队位置和坐标标定。

ABI 已按 `MAA-android-aarch64-v5.10.5.zip` 的 `include/MaaFramework/MaaDef.h` 与 `Instance/MaaResource.h` 核对。识别 callback 的参数顺序为 `context, task_id, node_name, custom_recognition_name, custom_recognition_param, image, roi, trans_arg, out_box, out_detail`；当前仍未在 MaaFwApp/JNA 注册回调，因为本仓库没有真实截图来验证实际地图格识别算法。`MaaMapRecognitionCallbackCodec` 现在固定了 out 参数的数据契约，避免后续 native/JNA bridge 自行发明格式。

该参数顺序在 `v5.9.2`、`v5.10.0`、`v5.10.5`、`v5.14.1` 之间逐参数比对一致，`MaaCustomActionCallback` 同样一致，因此本次版本上移不影响回调契约。

## 版本上移原因

基线原先锁定 MaaFramework `v5.9.2`，但 MaaFwApp `v0.1.0` 在 `MaaFrameworkLibrary.kt` 声明并在 `MaaRunner.kt` 调用 `MaaAndroidNativeControllerCreate`。该符号在 `v5.9.2` 中不存在：官方 `MaaController.h` 没有声明，全部 arm64 产物也没有导出。真机（MuMu / Android 15）运行任务时因此失败：

```
MaaRunner: run failed: UnsatisfiedLinkError: Error looking up function
'MaaAndroidNativeControllerCreate': undefined symbol: MaaAndroidNativeControllerCreate
```

逐版本核对 `include/MaaFramework/Instance/MaaController.h`，该函数自 `v5.10.0` 起出现。对 MaaFwApp 声明/调用的 38 个 `Maa*` 符号做全量比对，`v5.9.2` 恰好缺这 1 个，`v5.10.0` 及以后全部齐备。因此选择 `v5.10.5`——引入该 API 的 v5.10 线最新补丁版，既满足符号要求，又不引入 v5.11–v5.14 的额外行为变更。

注意 `libMaaAndroidNativeControlUnit.so` 不是根因。该库同样自 `v5.10.0` 起随产物发布，其导出的是 `MaaAndroidNativeControlUnitCreate`（`ControlUnit`，非 `Controller`）；宿主调用的 `MaaAndroidNativeControllerCreate` 实际由 `libMaaFramework.so` 导出。`setup_maa_framework.py` 的 `REQUIRED_SO` 把前者列为必需项，是它作为版本偏旧的伴随信号。

部署时需注意该脚本按字母序遍历 `.maa-cache` 中的全部 `MAA-android-*.zip` 并逐个铺开，后铺者覆盖先铺者。`v5.10.5` 按字母序排在 `v5.9.2` 之前，因此缓存里若同时存在两个版本，最终落盘的会是 `v5.9.2`。切换版本前必须清掉旧 zip，或使用 `--skip-download` 只保留目标版本。

在 ABI 完整核对前，不把 `MaaResourceRegisterCustomAction` 的猜测签名写入 JNA 或 C++，避免 native 崩溃和版本错配。
