package com.justccerly.alas.maafw

/** Host supplied input boundary. The implementation may call MaaFramework or an Android bridge. */
fun interface TapSink {
    fun tap(point: ScreenPoint)
}

/** Injectable delay boundary so replay tests never need to sleep. */
fun interface ActionDelay {
    fun await(milliseconds: Int)
}

data class ActionExecutionResult(
    val status: ActionPlanStatus,
    val executedActions: Int,
    val reason: String? = null,
)

/** Executes a validated plan without knowing anything about Android or MaaFramework handles. */
class MapActionExecutor(
    private val tapSink: TapSink,
    private val delay: ActionDelay = ActionDelay { milliseconds ->
        if (milliseconds > 0) Thread.sleep(milliseconds.toLong())
    },
) {
    fun execute(plan: MapActionPlan): ActionExecutionResult {
        if (plan.status == ActionPlanStatus.UNREACHABLE) {
            return ActionExecutionResult(
                status = plan.status,
                executedActions = 0,
                reason = plan.reason,
            )
        }

        var executed = 0
        plan.actions.forEach { action ->
            tapSink.tap(ScreenPoint(action.x, action.y))
            executed += 1
            delay.await(action.postDelayMs)
        }
        return ActionExecutionResult(
            status = plan.status,
            executedActions = executed,
        )
    }
}
