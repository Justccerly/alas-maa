package com.justccerly.alas.maafw

/** Result of converting a route request into a host-submittable task. */
sealed interface MapRouteRuntimeResult {
    data class Ready(
        val task: MapRuntimeTask,
        val diagnostics: MapRouteDiagnostics,
    ) : MapRouteRuntimeResult

    data class Rejected(
        val reason: String,
        val reportJson: String,
        val diagnostics: MapRouteDiagnostics,
    ) : MapRouteRuntimeResult
}

/**
 * Host boundary for the offline route slice.
 *
 * The Android layer only needs to map [MapRuntimeTask] to its own
 * `RuntimeTaskPayload`; it does not need to know about map planning or Pipeline
 * node chaining. Unreachable routes remain data and never become empty tasks.
 */
class MapRouteRuntimePlanner(
    private val routePlanner: MapRoutePlanner = MapRoutePlanner(),
) {
    fun plan(
        request: MapRouteRequest,
        taskName: String = request.entry,
    ): MapRouteRuntimeResult = plan(routePlanner.plan(request), taskName)

    fun planRequest(
        document: String,
        taskName: String = "AlasMapAction",
    ): MapRouteRuntimeResult = plan(routePlanner.planRequest(document), taskName)

    private fun plan(
        result: MapRoutePlanResult,
        taskName: String,
    ): MapRouteRuntimeResult {
        require(taskName.isNotBlank()) { "taskName must not be blank" }
        return when (result) {
            is MapRoutePlanResult.Found -> MapRouteRuntimeResult.Ready(
                task = result.runtime.asRuntimeTask(taskName),
                diagnostics = result.diagnostics,
            )

            is MapRoutePlanResult.Unreachable -> MapRouteRuntimeResult.Rejected(
                reason = result.result.reason,
                reportJson = MapRouteReportCodec.encode(result),
                diagnostics = result.diagnostics,
            )
        }
    }
}
