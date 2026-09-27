package com.justccerly.alas.maafw

import kotlinx.serialization.json.JsonObject

/** The platform-neutral subset needed to create MaaFwApp's RuntimeTask. */
data class MapActionRuntimeSpec(
    val entry: String,
    val pipelineOverrides: List<JsonObject>,
)

object MapActionRuntimeSpecFactory {
    fun fromPlan(plan: MapActionPlan, entry: String = "AlasMapAction"): MapActionRuntimeSpec =
        MapActionRuntimeSpec(
            entry = MapActionPipelineEncoder.firstNode(entry),
            pipelineOverrides = MapActionPipelineEncoder.encodeOverrides(plan, entry),
        )
}
