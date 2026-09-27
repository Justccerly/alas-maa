# MaaFramework 适配层核对记录

## 已确认的运行时事实

本地 MaaFramework `v5.9.2` arm64 库导出了以下能力：

- `MaaResourceRegisterCustomAction`
- `MaaResourceRegisterCustomRecognition`
- `MaaContextRunAction`
- `MaaContextRunRecognition`
- `MaaTaskerPostAction`
- `MaaTaskerPostRecognition`

`MaaFwApp` 当前的 JNA 声明和 `MaaRunner` 只使用资源加载、Controller、Tasker 和事件回调，尚未把这些自定义 Action/Recognition 注册函数暴露到 Kotlin。自动点击应通过 MaaFramework 的 Pipeline `Click` 或注册后的 Custom Action 进入控制器；预览用的 `RemoteService.touchDown/touchUp` 不属于自动任务接口。

## 当前边界

`alas-maafw` 保持平台无关，只输出：

- `MapSnapshot`
- `MapActionPlan`
- `TapSink` 执行边界

动作计划还可以通过 `MapActionPipelineEncoder` 编码为 Pipeline override。资源包预置了 `AlasMapAction` 入口节点；编码器复用该入口并追加 `AlasMapAction.1`、`AlasMapAction.2` 等后续 Click 节点。宿主应把编码结果作为该任务的有序 `pipelineOverrides` 项提交，不能把它当作新的 Task entry。

`resources/replays/map-action-plan.json` 同时保存预期的 Pipeline 节点链，CI 会重新生成并比较该结构，检查入口、固定坐标、延迟和 `next` 连续性。

宿主接线时优先使用 `MapActionPipelineEncoder.encodeOverrides`，它直接返回 MaaFwApp `RuntimeTask.pipelineOverrides` 所需的 `List<JsonObject>`；只有跨 native/JNA 边界时才使用字符串形式的 `encode`。

如果调用方只需要一个可传递的中间值，使用 `MapActionRuntimeSpecFactory.fromPlan`，它返回 `entry` 和 `pipelineOverrides`，不引入 MaaFwApp 的 Android 类型。

下一步接入需要在宿主或 native bridge 中完成：

1. 按 v5.9.2 头文件核对自定义 Action/Recognition 的完整 C ABI 签名。
2. 在 native bridge 注册一个最小 Custom Action，并把 JSON 参数转成 `MapActionPlan`。
3. 用 MaaFramework 的 Context/Controller 执行点击，回传成功、失败和取消状态。
4. 用真实截图回放验证识别结果和坐标标定。

在 ABI 完整核对前，不把 `MaaResourceRegisterCustomAction` 的猜测签名写入 JNA 或 C++，避免 native 崩溃和版本错配。
