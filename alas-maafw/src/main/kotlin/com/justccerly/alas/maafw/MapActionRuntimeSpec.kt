package com.justccerly.alas.maafw

import kotlinx.serialization.json.JsonObject

/** The platform-neutral subset needed to create MaaFwApp's RuntimeTask. */
data class MapActionRuntimeSpec(
    val entry: String,
    val pipelineOverrides: List<JsonObject>,
) {
    /** Shape shared by the host's RuntimeTaskPayload without importing Android classes. */
    fun asRuntimeTask(taskName: String = entry): MapRuntimeTask {
        require(taskName.isNotBlank()) { "taskName must not be blank" }
        return MapRuntimeTask(
            taskName = taskName,
            entry = entry,
            pipelineOverrides = pipelineOverrides,
        )
    }
}

data class MapRuntimeTask(
    val taskName: String,
    val entry: String,
    val pipelineOverrides: List<JsonObject>,
) {
    init {
        require(taskName.isNotBlank()) { "taskName must not be blank" }
        require(entry.isNotBlank()) { "entry must not be blank" }
    }
}

object MapActionRuntimeSpecFactory {
    fun fromPlan(plan: MapActionPlan, entry: String = "AlasMapAction"): MapActionRuntimeSpec =
        MapActionRuntimeSpec(
            entry = MapActionPipelineEncoder.firstNode(entry),
            pipelineOverrides = MapActionPipelineEncoder.encodeOverrides(plan, entry),
        )
}
