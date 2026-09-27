package com.justccerly.alas.maafw

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Encodes a movement plan as a MaaFramework Pipeline override. */
object MapActionPipelineEncoder {
    /**
     * Returns an object suitable for the `pipelineOverride` argument of
     * `MaaTaskerPostTask`. Each tap is a fixed-coordinate Click node.
     */
    fun encode(plan: MapActionPlan, entry: String = "AlasMapAction"): String {
        require(entry.matches(ENTRY_PATTERN)) { "entry must contain only letters, digits, _, ., or -" }
        require(plan.status == ActionPlanStatus.FOUND) {
            "cannot encode an unreachable action plan"
        }

        val nodes = buildJsonObject {
            if (plan.actions.isEmpty()) {
                putJsonObject(entry) { put("action", "DoNothing") }
            } else {
                plan.actions.forEachIndexed { index, action ->
                    val nodeName = nodeName(entry, index)
                    putJsonObject(nodeName) {
                        put("action", "Click")
                        putJsonArray("target") {
                            add(action.x)
                            add(action.y)
                        }
                        put("post_delay", action.postDelayMs)
                        if (index + 1 < plan.actions.size) {
                            putJsonArray("next") { add(nodeName(entry, index + 1)) }
                        }
                    }
                }
            }
        }
        return JsonObject(nodes).toString()
    }

    fun firstNode(entry: String = "AlasMapAction"): String {
        require(entry.matches(ENTRY_PATTERN)) { "entry must contain only letters, digits, _, ., or -" }
        return entry
    }

    /** Index zero reuses the declared task entry; later steps are added nodes. */
    private fun nodeName(entry: String, index: Int): String =
        if (index == 0) entry else "$entry.$index"

    private val ENTRY_PATTERN = Regex("[A-Za-z0-9_.-]+")
}
