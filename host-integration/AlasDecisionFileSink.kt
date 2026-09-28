package com.aliothmoon.maafw.remote

import com.justccerly.alas.maafw.AlasDecision
import com.justccerly.alas.maafw.AlasDecisionKind
import com.justccerly.alas.maafw.AlasDecisionSink
import com.justccerly.alas.maafw.AlasDecisionSinkHolder
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 把 Alas 侧决策追加写入宿主日志目录，一行一条 JSON。
 *
 * 宿主已有的 `RunSessionLogStore` 只记 MaaFramework 事件（`Controller.Action.*`、任务起止），
 * 看不到 Alas 层决定了什么——识别回调收到什么参数、解出多大范围的地图、写回了哪个 box、
 * 路线规划成功还是不可达。真机排查恰恰缺这几行，所以单独落一份而不是混进框架事件流。
 *
 * 路径来自 `setup()` 已传入并验证可写的 logDir，**不读 AppPaths**：本对象跑在特权进程里，
 * 而 AppPaths.init() 只在 app 进程的 Application.onCreate 调用，特权进程里是未初始化的
 * lateinit，读它会抛异常。
 *
 * 一行一条而不是整体数组：跑到一半被杀时，已落下的行照样解得出来（与 RunSessionLog 同取舍）。
 * 追加写、超限轮转，任何失败都只降级为无日志，绝不影响运行。
 */
internal object AlasDecisionFileSink : AlasDecisionSink {

    private const val DIR_NAME = "alas"
    private const val FILE_NAME = "alas_decisions.jsonl"
    private const val MAX_BYTES = 2L * 1024 * 1024
    private val json = Json { explicitNulls = false }
    private val lock = Any()

    @Volatile
    private var target: File? = null

    /**
     * 幂等；在 logDir 已确认可写之后、注册识别回调之前调用。
     *
     * [logDir] 由 `RemoteService.setup()` 传入，是 app 与特权进程都读得到的外部私有目录。
     */
    fun install(logDir: String) {
        if (target != null) return
        synchronized(lock) {
            if (target != null) return
            val dir = runCatching { File(logDir, DIR_NAME).apply { mkdirs() } }.getOrNull()
            if (dir == null || !dir.isDirectory || !dir.canWrite()) {
                // 诊断不是主流程：建不出目录就保持默认的丢弃 sink
                return
            }
            target = File(dir, FILE_NAME)
            AlasDecisionSinkHolder.sink = this
        }
    }

    override fun record(decision: AlasDecision) {
        val file = target ?: return
        synchronized(lock) {
            runCatching {
                rotateIfNeeded(file)
                file.appendText(json.encodeToString(decision) + "\n")
            }
        }
    }

    private fun rotateIfNeeded(file: File) {
        if (!file.exists() || file.length() <= MAX_BYTES) return
        val bak = File(file.parentFile, "$FILE_NAME.1")
        if (bak.exists()) bak.delete()
        file.renameTo(bak)
    }

    /** 单测与宿主拆除时使用 */
    fun uninstall() {
        synchronized(lock) {
            AlasDecisionSinkHolder.reset()
            target = null
        }
    }
}

/** 便于宿主按级别分流时判断是否需要额外告警 */
internal fun AlasDecision.isFailure(): Boolean =
    kind == AlasDecisionKind.RECOGNITION_FAILURE
