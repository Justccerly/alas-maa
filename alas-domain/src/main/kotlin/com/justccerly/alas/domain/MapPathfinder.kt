package com.justccerly.alas.domain

import java.util.PriorityQueue
import kotlin.math.abs

/**
 * Deterministic four-direction A* search for a single map turn.
 *
 * Recognition and enemy prediction stay outside this class. The caller supplies an immutable
 * snapshot, which makes the result replayable and keeps Android/MaaFramework APIs out of the domain.
 */
class MapPathfinder {
    fun findPath(
        snapshot: MapSnapshot,
        fleet: FleetState,
        target: MapCoordinate,
        allowEnemyGrid: Boolean = false,
    ): PathResult {
        val start = fleet.position
        if (snapshot.gridAt(start) == null) {
            return PathResult.Unreachable("fleet position is outside the map")
        }
        val targetGrid = snapshot.gridAt(target)
            ?: return PathResult.Unreachable("target is outside the map")
        if (!targetGrid.traversable && target != start) {
            return PathResult.Unreachable("target is not traversable")
        }
        if (start == target) {
            return PathResult.Found(MapPath(listOf(start), totalCost = 0))
        }

        data class QueueEntry(
            val coordinate: MapCoordinate,
            val cost: Int,
            val estimate: Int,
        )

        val queue = PriorityQueue<QueueEntry>(compareBy<QueueEntry> { it.estimate }
            .thenBy { it.cost }
            .thenBy { it.coordinate.y }
            .thenBy { it.coordinate.x })
        val bestCost = mutableMapOf(start to 0)
        val previous = mutableMapOf<MapCoordinate, MapCoordinate>()
        queue += QueueEntry(start, cost = 0, estimate = heuristic(start, target))

        while (queue.isNotEmpty()) {
            val current = queue.remove()
            if (current.cost != bestCost[current.coordinate]) continue
            if (current.coordinate == target) {
                val path = reconstruct(previous, target)
                return if (current.cost <= fleet.movementPoints) {
                    PathResult.Found(MapPath(path, current.cost))
                } else {
                    PathResult.Unreachable("path cost ${current.cost} exceeds movement points ${fleet.movementPoints}")
                }
            }

            for (neighbor in snapshot.neighbors(current.coordinate)) {
                if (!neighbor.traversable) continue
                if (neighbor.occupiedByEnemy && !allowEnemyGrid && neighbor.coordinate != target) continue
                val nextCost = current.cost + neighbor.moveCost
                if (nextCost > fleet.movementPoints) continue
                if (nextCost >= (bestCost[neighbor.coordinate] ?: Int.MAX_VALUE)) continue
                bestCost[neighbor.coordinate] = nextCost
                previous[neighbor.coordinate] = current.coordinate
                queue += QueueEntry(
                    coordinate = neighbor.coordinate,
                    cost = nextCost,
                    estimate = nextCost + heuristic(neighbor.coordinate, target),
                )
            }
        }

        return PathResult.Unreachable("no traversable route to target")
    }

    private fun heuristic(from: MapCoordinate, to: MapCoordinate): Int =
        abs(from.x - to.x) + abs(from.y - to.y)

    private fun reconstruct(
        previous: Map<MapCoordinate, MapCoordinate>,
        target: MapCoordinate,
    ): List<MapCoordinate> {
        val reversed = mutableListOf(target)
        var current = target
        while (current in previous) {
            current = previous.getValue(current)
            reversed += current
        }
        return reversed.asReversed()
    }
}
