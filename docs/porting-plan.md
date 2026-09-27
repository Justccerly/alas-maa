# alas-maa 移植规划

## 1. 项目目标

将 Alas 的碧蓝航线自动化能力迁移为一个基于 MaaFramework 的 Android 应用，最终运行形态接近 MaaMeow：

- Android 9+ 优先，首要架构为 `arm64-v8a`，后续考虑 `x86_64`
- 不依赖 PC 和外部模拟器作为产品前提
- 使用 Shizuku 或 Root 完成必要的截图、输入和后台控制
- 支持前台悬浮控制和后台虚拟显示
- 具备任务调度、日志、配置备份、资源更新和应用更新能力
- 保留 Alas 在地图、任务、寻路、心情和 7x24 调度方面的核心能力

## 2. 明确的技术路线

```text
MaaFwApp Android 宿主
        |
Compose UI / Service / Overlay / 权限 / 更新 / 日志
        |
Alas Application Layer
        |
任务编排 / 配置 / 调度 / 状态管理 / 业务用例
        |
Alas Domain Layer
        |
海图模型 / 识别语义 / 寻路 / 舰队 / 战斗 / 大世界 / 活动机制
        |
MaaFramework Adapter
        |
Pipeline / Custom Recognition / Custom Action / Task Callback
        |
MaaFramework Runtime
        |
截图 / 输入 / OCR / 模板匹配 / 任务执行
        |
Android Control Unit / Shizuku / Root
```

### 2.1 各层职责

#### Android 宿主层

参考 `Aliothmoon/MaaFwApp` 的通用 Android GUI，参考 `Aliothmoon/MAA-Meow` 的后台运行和权限处理，但不直接继承明日方舟专用业务模块。

负责：

- Compose 页面和配置表单
- 前台服务、后台任务和应用生命周期
- 悬浮窗、虚拟显示、前台/后台模式切换
- Shizuku/Root 状态检查和授权引导
- 配置持久化、备份与恢复
- 日志查看、导出和诊断信息
- 应用版本与资源版本更新
- 外部 Intent/API 自动化入口

#### Alas Application 层

负责将用户操作、调度器和领域用例连接起来，避免 UI 直接调用 MaaFramework。

负责：

- 启动、暂停、恢复和取消任务
- 配置校验与运行前检查
- 调度器和任务队列
- 任务状态、失败原因和恢复策略
- 将 Android 生命周期事件转换为领域可理解的事件
- 将 MaaFramework 回调转换为统一的运行状态和日志事件

#### Alas Domain 层

这是移植的核心，尽可能不依赖 Android 或 MaaFramework 的具体 API。

负责：

- 海图数据和地图状态模型
- 地图识别结果的语义化
- 舰队位置、敌人、障碍物、资源点和 Boss 状态
- 移动、索敌、伏击规避和战斗路线决策
- 主线、困难、活动、作战档案和大世界逻辑
- 委托、科研、后宅、商店、建造等收菜任务
- 心情控制和队伍选择
- 活动地图特殊机制
- 任务完成条件和异常状态判断

#### MaaFramework 适配层

负责把领域层需要的观察和动作映射到 MaaFramework。

包括：

- Pipeline 资源加载和任务入口
- 截图、坐标、触摸和文本输入适配
- 模板匹配、OCR、颜色和区域识别
- 自定义识别器
- 自定义动作
- 任务回调、超时、取消和重试
- MaaFramework 版本差异隔离

## 3. 为什么不能全部写成 Pipeline

MaaFramework Pipeline 适合描述确定性的界面流程，例如：

- 等待主界面
- 点击某个按钮
- 识别弹窗并关闭
- 读取 OCR 文本
- 进入关卡选择页面
- 判断结算页面并返回

但以下逻辑不应全部塞进 JSON：

- 海图建模
- 多目标寻路
- 舰队和敌方状态推理
- 大世界地图决策
- 心情和资源管理
- 任务调度
- 活动特殊机制
- 复杂异常恢复

这些逻辑应该放在 Alas Domain 中，通过 MaaFramework 的自定义识别、自定义动作或任务回调与 Pipeline 连接。

## 4. 建议的仓库结构

```text
alas-maa/
├── app/                    # Android 应用模块
├── alas-application/       # 用例、调度、任务生命周期和状态
├── alas-domain/            # 与平台无关的碧蓝航线业务逻辑
├── alas-maafw/             # MaaFramework 适配层
├── alas-native/             # C++ 自定义识别、动作和性能敏感逻辑
├── resources/
│   ├── pipeline/            # MaaFramework Pipeline JSON
│   ├── templates/           # 模板图片
│   ├── ocr/                 # OCR 和文字资源
│   ├── maps/                # 海图数据
│   └── game-data/            # 舰船、活动和其他数据
├── tools/
│   ├── sync-upstream/       # Alas 上游同步和资源转换工具
│   ├── validate-resources/  # 资源校验
│   └── replay/              # 截图回放和离线调试工具
├── docs/
│   ├── porting-plan.md
│   ├── architecture.md
│   ├── resource-format.md
│   └── development.md
├── .gitignore
└── README.md
```

初期可以根据实际模板调整目录，但必须保持 `Android 宿主`、`Alas 领域逻辑`、`MaaFramework 适配` 和 `资源` 的边界。

## 5. 分阶段实施计划

### Phase 0: 基线和验证

目标：证明 MaaFramework 能在目标 Android 设备上完成一条最小闭环。

任务：

- 固定 MaaFramework 版本和 MaaFwApp 基线
- 建立 Android arm64 Debug 构建
- 完成 Shizuku/Root 状态检查
- 完成截图获取
- 完成点击、滑动和返回操作
- 完成一个模板识别和一个 OCR 识别
- 建立 MaaFramework 日志回传
- 使用离线截图完成可重复测试

验收：

- 能在真实 Android 设备上截图
- 能识别一张固定的碧蓝航线界面截图
- 能执行一次点击和一次滑动
- 任务取消后不会残留前台服务或后台线程

### Phase 1: Android 宿主骨架

目标：先建立类似 MaaMeow 的产品外壳。

任务：

- Compose 主页面
- 运行模式选择：前台、后台虚拟显示
- 任务列表和任务详情
- 启动、暂停、停止和恢复
- 日志面板
- 配置持久化
- 运行权限引导
- 前台服务和进程保活
- 基础悬浮控制面板

验收：

- 应用重启后可以恢复配置
- 后台模式可以启动和停止任务
- 权限不足时能给出明确状态
- 任务日志能在 UI 和文件中同时查看
### 当前进度（截至最近一次本地与 CI 校验）
为避免把“领域契约已完成”和“真机识别已完成”混为一谈，当前地图切片按以下状态记录：

#### 已完成
- [x] `alas-domain` 接入 MaaFwApp 的 Gradle 构建
- [x] 创建 `alas-maafw` 最小适配模块并接入 MaaFwApp Gradle 构建
- [x] 建立 `RecognizedGrid` / `RecognizedMap` 识别语义模型
- [x] 实现 `MapSnapshotMapper`：识别语义 → `MapSnapshot`
- [x] 对未知格采用不可通行默认值，不凭空补全部分观察中的缺失格
- [x] 拒绝重复坐标、越界坐标和非法移动成本
- [x] 为映射契约补充 Kotlin/JVM 单元测试，并通过宿主 `:alas-domain:test`
- [x] 通过领域结构检查、夹具校验和资源校验
- [x] 轻量 CI 校验通过；当前策略下未因领域模块变更触发 APK 构建

#### 当前未完成
- [x] 创建并接入 `alas-maafw` 适配模块
- [x] 将领域路径转换为确定性的地图点击动作计划
- [x] 增加识别语义 JSON 解码与置信度过滤
- [x] 增加识别回放夹具和 CI 格式校验
- [x] 增加可注入的地图点击执行边界
- [x] 增加动作计划 JSON 的编码、解码和回放测试
- [x] 核对 v5.9.2 自定义 Action/Recognition 导出能力并记录宿主接入边界
- [x] 通过集成脚本向 MaaFwApp 注入 v5.9.2 Custom Recognition JNA 声明和资源注册
- [x] 增加 `AlasMapRecognition` 固定回放任务，验证 `out_box` / `out_detail` 数据契约
- [x] 将动作计划编码为 MaaTasker Pipeline override 的 Click 节点链
- [x] 串联识别、寻路和 RuntimeTask 规格生成的离线规划器
- [x] 将 MaaFramework Custom Recognition 的 `out_detail` 解析为可回放的 `RecognizedMap` 输入
- [x] 将 `PathResult.Found` 转换为宿主 `RuntimeTaskPayload` 的 Pipeline 点击动作
- [ ] 接入真实截图回放测试
- [ ] 构建 APK 并在真机验证启动、地图识别和地图操作闭环

#### 下一步执行顺序
1. 在 GitHub Actions x86_64 runner 上完成本次宿主桥接的 Kotlin/Android 编译。
2. 用固定回放任务验证 MaaFramework Custom Recognition 回调能返回地图详情。
3. 准备真实截图并替换回放参数，验证 OCR/模板或 native 地图格识别算法。
4. 构建 APK 后在真机完成启动、地图识别、寻路和点击闭环。

**边界声明：** 当前已完成 Custom Recognition 的 ABI 注册和固定 JSON 回放桥接，但回放仍使用预先提供的地图语义；真实游戏截图识别、坐标标定和真机地图流程仍未验收。



### Phase 2: Alas 运行时抽象
目标：把 Alas 原有设备依赖替换成稳定接口。

需要建立的接口：

```kotlin
interface DeviceController {
    suspend fun screenshot(): ImageFrame
    suspend fun click(point: Point)
    suspend fun swipe(gesture: SwipeGesture)
    suspend fun inputText(text: String)
    suspend fun pressBack()
}

interface Recognizer {
    suspend fun recognize(frame: ImageFrame, request: RecognitionRequest): RecognitionResult
}

interface TaskRuntime {
    suspend fun start(task: TaskRequest): TaskRunId
    suspend fun cancel(runId: TaskRunId)
}
```

具体接口名称可以根据 MaaFramework API 调整，但领域逻辑不得直接依赖 Android View、ADB 命令或 Compose 状态。

验收：

- 领域层可以在桌面离线测试环境中运行基本状态转换
- 设备实现可以替换为真实设备或截图回放
- 取消、超时、重试和异常都能被统一处理

### Phase 3: 基础游戏流程

目标：完成一条稳定的碧蓝航线操作链路。

建议顺序：

1. 启动和连接游戏
2. 等待并识别主界面
3. 自动处理公告、弹窗和登录状态
4. 进入一个固定关卡
5. 完成一次战斗
6. 识别结算页面
7. 返回并记录结果

这一阶段优先验证运行时和识别基础，不急于迁移所有 Alas 功能。

验收：

- 固定设备、固定关卡连续运行不少于 10 次
- 任务失败时能保存截图和结构化日志
- 游戏卡顿、弹窗和短暂识别失败具备有限重试

### Phase 4: 地图和战斗领域逻辑
目标：迁移 Alas 最有价值的核心能力。
当前切片：
- [x] 建立平台无关的坐标、格子、舰队和路径结果模型
- [x] 实现确定性四方向 A*，支持障碍、移动代价、敌方格和移动点限制
- [x] 建立首个地图回放夹具和 CI 校验
- [x] 接入 MaaFwApp 的 Gradle JVM 模块并运行 `:alas-domain:test`
- [x] 建立识别语义到 `MapSnapshot` 的领域映射契约
- [ ] 将 MaaFramework 实际识别结果转换为 `RecognizedMap`
- [ ] 将路径结果转换为 Pipeline 点击/滑动动作
任务：

- 海图识别结果模型
- 地图坐标和格子状态
- 舰队、敌人、Boss 和资源点模型
- 路径搜索和目标选择
- 方向移动和战斗触发
- 伏击、障碍、迷宫和移动限制
- 战斗结果和异常结果识别

建议先迁移一张固定地图，再扩展到主线和活动地图。

验收：

- 地图识别结果可以用固定截图回放测试
- 寻路结果在相同输入下稳定
- 关键地图状态有单元测试和回放测试

### Phase 5: 调度器和日常任务

目标：恢复 Alas 的 7x24 任务能力。

任务：

- 任务定义和启用状态
- 下次运行时间计算
- 委托、科研、后宅和商店任务
- 心情控制
- 任务优先级和互斥
- 失败退避和自动恢复
- 应用重启后的任务恢复
- 通知和外部 Webhook

验收：

- 调度器可在应用被杀后恢复任务状态
- 多任务不会同时操作同一游戏实例
- 长时间等待期间可以执行其他到期任务

### Phase 6: 资源同步和发布

目标：让后续 Alas 更新不再触发整项目重构。

任务：

- 保存 Alas 上游版本和资源版本
- 建立资源导入/转换脚本
- 对 Pipeline、模板、地图和游戏数据做格式校验
- 增加资源兼容版本
- 支持资源包增量更新
- 支持应用和资源分离发布
- 建立截图回放回归测试集

验收：

- 新增一个活动资源不需要修改 Android UI
- 资源格式错误能在 CI 中被发现
- 应用可以独立更新资源
- MaaFramework 升级不会直接扩散到 Alas Domain

## 6. Alas 更新时的同步策略

### 低风险更新

通常可以直接同步：

- 活动地图数据
- 模板图片
- OCR 文字资源
- 任务配置
- 舰船和游戏数据
- 不涉及设备 API 的纯业务规则

### 中风险更新

需要人工检查和适配：

- 任务状态和配置结构变化
- 地图识别模型变化
- 调度器行为变化
- 任务名称或运行参数变化

### 高风险更新

需要修改适配层或领域实现：

- ADB/uiautomator2 调用方式变化
- 截图和输入接口变化
- 图像识别框架更换
- 任务基类和模块继承关系大改
- 运行时异常和重试机制变化

同步原则：

1. `alas-upstream` 或上游镜像只记录来源，不在上游镜像中堆 Android 改动。
2. 资源转换通过脚本完成，不手工复制大量文件。
3. MaaFramework API 只在 `alas-maafw` 中出现。
4. Android API 只在 `app` 和平台实现中出现。
5. 每次同步都运行截图回放和领域层测试。
6. 给每个资源包记录对应的 Alas commit、MaaFramework 版本和资源 schema 版本。

## 7. 测试策略

### 单元测试

覆盖：

- 地图状态转换
- 寻路
- 目标选择
- 心情计算
- 任务调度
- 配置迁移
- 重试和退避

### 截图回放测试

保存脱敏截图和识别期望结果，用于测试：

- 登录和公告
- 主界面
- 地图界面
- 战斗结果
- 委托、科研和后宅
- 常见错误弹窗

### 真机集成测试

至少覆盖：

- arm64 Android 9+
- Shizuku 模式
- Root 模式
- 前台运行
- 后台虚拟显示
- 应用被系统回收后恢复
- 横竖屏和不同分辨率

## 8. 首批不做的内容

为了控制范围，第一阶段不包含：

- 全部活动地图一次性迁移
- 全部国际服和特殊客户端同时支持
- 完整的 PC GUI
- 直接兼容 Alas 的所有旧配置文件
- 通过 Python 嵌入运行全部 Alas 代码
- 一开始就实现完整 7x24 调度

先完成一个可重复的单地图战斗闭环，再扩展领域能力。

## 9. 主要风险和决策

### Python 兼容性

不建议把 Python 运行时作为最终核心方案。`uiautomator2`、桌面依赖和 Python 图像栈会增加 APK 体积、后台生命周期和原生权限处理复杂度。Python 可以用于离线工具、资源转换和验证脚本，但 Android 运行核心优先采用 MaaFramework + C++/Kotlin。

### 复杂逻辑迁移成本

Alas 的地图和任务逻辑不是简单的 UI 自动化脚本。需要先建立领域模型，再迁移行为，不能只依赖 JSON Pipeline。

### 游戏版本变化

截图、模板、OCR 和地图数据需要独立更新机制。应用代码和资源必须分开发布，否则每次游戏更新都需要发布完整 APK。

### Android 后台限制

不同厂商的后台限制、悬浮窗权限、屏幕锁定和虚拟显示行为需要真机验证。不能只在模拟器上验收。

### 许可证

Alas 使用 GPL-3.0，MaaFramework 使用 LGPL-3.0，MaaMeow 使用 AGPL-3.0。复用代码、资源和第三方库前需要保留许可证、版权声明和对应的第三方声明文件。

## 10. 当前首个里程碑

M0 的完成标准：
- [x] 仓库结构建立
- [x] MaaFramework 版本固定
- [x] MaaFwApp 基线完成评估
- [x] Android arm64 Debug 构建通过（GitHub Actions）
- [ ] Shizuku/Root 状态检测通过
- [ ] 真实设备截图通过
- [ ] 点击和滑动通过
- [ ] 一个模板识别通过
- [ ] 一个 OCR 识别通过
- [x] ADB 启动游戏 Pipeline 已建立（包名按服务器选项覆盖）
- [ ] 截图回放测试骨架建立
- [ ] 运行日志和错误截图落盘

M0 的宿主构建部分已完成。下一步是在真实 Android 设备上验证 `启动碧蓝航线` 的 ADB 启动行为，再将 MaaFramework 截图和识别结果转换为 `MapSnapshot`。
