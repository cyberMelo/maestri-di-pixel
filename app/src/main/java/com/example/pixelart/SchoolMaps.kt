package com.example.pixelart

import java.util.PriorityQueue
import kotlin.math.abs

data class GridPos(val x: Int, val y: Int)

data class HubNpc(
    val id: String,
    val name: String,
    val label: String,
    val spriteId: String,
    val subjectFilter: String, // "storia", "scienze", "preside"
    val pos: GridPos,
    val returnSpawnId: String
)

data class HubPortal(
    val id: String,
    val label: String,
    val pos: GridPos,
    val action: PortalAction
)

sealed interface PortalAction {
    data class GoToHub(val targetHubId: String, val targetSpawnId: String) : PortalAction
    data object OpenGymPicker : PortalAction
    data object OpenScienceLabPicker : PortalAction
    data object OpenLibraryPicker : PortalAction
    data object OpenGeoPicker : PortalAction
    data object OpenEnglishPicker : PortalAction
    data object OpenLocker : PortalAction
    data class WipDoor(val roomName: String) : PortalAction
}

data class SchoolMapData(
    val id: String,
    val width: Int,
    val height: Int,
    val tiles: Array<Array<TileKind>>,
    val spawns: Map<String, GridPos>,
    val npcs: List<HubNpc>,
    val portals: List<HubPortal>
) {
    fun tileAt(x: Int, y: Int): TileKind {
        if (x !in 0 until width || y !in 0 until height) return TileKind.TREE_BUSH
        return tiles[y][x]
    }

    fun isWalkable(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        if (tiles[y][x].solid) return false
        if (npcs.any { it.pos.x == x && it.pos.y == y }) return false
        return true
    }

    fun findNpcAt(pos: GridPos): HubNpc? = npcs.firstOrNull { it.pos == pos }
    fun findPortalAt(pos: GridPos): HubPortal? = portals.firstOrNull { it.pos == pos }
}

object SchoolMaps {
    val schoolYard: SchoolMapData by lazy { buildSchoolYard() }
    val schoolHallway: SchoolMapData by lazy { buildSchoolHallway() }

    private fun buildSchoolYard(): SchoolMapData {
        val w = 26
        val h = 18
        val grid = Array(h) { Array(w) { TileKind.GRASS_PLAIN } }

        // Scattered flowers
        val flowerSpots = listOf(
            GridPos(3, 8), GridPos(6, 13), GridPos(20, 9), GridPos(22, 13),
            GridPos(8, 7), GridPos(18, 14), GridPos(4, 15), GridPos(21, 7)
        )
        flowerSpots.forEach { grid[it.y][it.x] = TileKind.GRASS_FLOWERS }

        // Perimeter trees and fences
        for (x in 0 until w) {
            grid[0][x] = TileKind.TREE_BUSH
            grid[h - 1][x] = TileKind.FENCE_H
        }
        for (y in 0 until h) {
            grid[y][0] = TileKind.TREE_BUSH
            grid[y][w - 1] = TileKind.TREE_BUSH
        }

        // School building facade along top (rows 1..4, cols 5..20)
        for (x in 5..20) {
            grid[1][x] = TileKind.SCHOOL_ROOF
            grid[2][x] = TileKind.SCHOOL_ROOF
            grid[3][x] = TileKind.SCHOOL_WALL_EXT
            grid[4][x] = TileKind.SCHOOL_WALL_EXT
        }
        // Facade windows
        for (wx in listOf(7, 9, 16, 18)) {
            grid[3][wx] = TileKind.WINDOW_EXT
        }
        // Main School Entrance door at (13, 4) & doormat at (13, 5)
        grid[4][12] = TileKind.SCHOOL_WALL_EXT
        grid[4][13] = TileKind.DOOR_ACTIVE
        grid[5][13] = TileKind.DOORMAT

        // Stone paths connecting entrance and NPC plazas
        for (y in 6..15) {
            grid[y][12] = TileKind.PATH_STONE
            grid[y][13] = TileKind.PATH_STONE
        }
        for (x in 5..21) {
            grid[10][x] = TileKind.PATH_STONE
            grid[11][x] = TileKind.PATH_STONE
        }
        // Plazas around NPCs
        for (x in 4..7) for (y in 8..11) grid[y][x] = TileKind.PATH_STONE
        for (x in 18..21) for (y in 8..11) grid[y][x] = TileKind.PATH_STONE

        // Yard benches
        grid[7][8] = TileKind.BENCH
        grid[7][17] = TileKind.BENCH
        grid[14][7] = TileKind.BENCH
        grid[14][18] = TileKind.BENCH

        val npcs = listOf(
            HubNpc(
                id = "npc_history",
                name = "Maestro Erodoto",
                label = "Storia",
                spriteId = "npc_history",
                subjectFilter = "storia",
                pos = GridPos(5, 9),
                returnSpawnId = "from_npc_history"
            ),
            HubNpc(
                id = "npc_science",
                name = "Maestra Flora",
                label = "Scienze",
                spriteId = "npc_science",
                subjectFilter = "scienze",
                pos = GridPos(20, 9),
                returnSpawnId = "from_npc_science"
            ),
            HubNpc(
                id = "npc_preside",
                name = "Preside Stella",
                label = "Preside",
                spriteId = "npc_boss",
                subjectFilter = "preside",
                pos = GridPos(13, 14),
                returnSpawnId = "from_npc_preside"
            )
        )

        val portals = listOf(
            HubPortal(
                id = "portal_school_entrance",
                label = "Ingresso",
                pos = GridPos(13, 4),
                action = PortalAction.GoToHub("school_hallway", "from_yard")
            )
        )

        // Spawns strictly >= 1 tile outside portal/NPC bounds (anti-loop rule)
        val spawns = mapOf(
            "default" to GridPos(13, 10),
            "from_hallway" to GridPos(13, 6),
            "from_npc_history" to GridPos(6, 10),
            "from_npc_science" to GridPos(19, 10),
            "from_npc_preside" to GridPos(13, 13)
        )

        return SchoolMapData(
            id = "school_yard",
            width = w,
            height = h,
            tiles = grid,
            spawns = spawns,
            npcs = npcs,
            portals = portals
        )
    }

    private fun buildSchoolHallway(): SchoolMapData {
        val w = 28
        val h = 14
        val grid = Array(h) { y ->
            Array(w) { x ->
                if ((x + y) % 2 == 0) TileKind.HALL_FLOOR_A else TileKind.HALL_FLOOR_B
            }
        }

        // Walls around hallway
        for (x in 0 until w) {
            grid[0][x] = TileKind.HALL_WALL
            grid[1][x] = TileKind.HALL_WALL
            grid[2][x] = TileKind.HALL_WALL
            grid[h - 1][x] = TileKind.HALL_WALL
        }
        for (y in 0 until h) {
            grid[y][0] = TileKind.HALL_WALL
            grid[y][w - 1] = TileKind.HALL_WALL
        }

        // Interior hallway props along top wall
        grid[1][2] = TileKind.WINDOW_INT
        grid[1][6] = TileKind.NOTICE_BOARD
        grid[1][14] = TileKind.NOTICE_BOARD
        grid[1][22] = TileKind.NOTICE_BOARD
        grid[1][25] = TileKind.WINDOW_INT

        grid[2][2] = TileKind.LOCKER_PROP
        grid[2][10] = TileKind.LOCKER_PROP
        grid[2][18] = TileKind.LOCKER_PROP
        grid[2][26] = TileKind.LOCKER_PROP

        // Potted plants
        grid[3][1] = TileKind.POTTED_PLANT
        grid[3][26] = TileKind.POTTED_PLANT
        grid[12][1] = TileKind.POTTED_PLANT
        grid[12][26] = TileKind.POTTED_PLANT

        // 6 Active Doors evenly spaced on North wall (row 2) with doormats (row 3)
        // 1. Palestra (Matematica) at (4, 2)
        grid[2][4] = TileKind.DOOR_GYM
        grid[3][4] = TileKind.DOORMAT

        // 2. Armadietto & Spaccio (Locker) at (8, 2)
        grid[2][8] = TileKind.DOOR_LOCKER
        grid[3][8] = TileKind.DOORMAT

        // 3. Laboratorio (Scienze) at (12, 2)
        grid[2][12] = TileKind.DOOR_ACTIVE
        grid[3][12] = TileKind.DOORMAT

        // 4. Biblioteca (Italiano) at (16, 2)
        grid[2][16] = TileKind.DOOR_LIBRARY
        grid[3][16] = TileKind.DOORMAT

        // 5. Aula di Geografia at (20, 2)
        grid[2][20] = TileKind.DOOR_GEO
        grid[3][20] = TileKind.DOORMAT

        // 6. Aula di Inglese at (24, 2)
        grid[2][24] = TileKind.DOOR_ENGLISH
        grid[3][24] = TileKind.DOORMAT

        // South Exit Door back to Yard at (14, 13)
        grid[13][14] = TileKind.DOOR_ACTIVE
        grid[12][14] = TileKind.DOORMAT

        val portals = listOf(
            HubPortal(
                id = "portal_gym",
                label = "Palestra",
                pos = GridPos(4, 2),
                action = PortalAction.OpenGymPicker
            ),
            HubPortal(
                id = "portal_locker",
                label = "Armadietto",
                pos = GridPos(8, 2),
                action = PortalAction.OpenLocker
            ),
            HubPortal(
                id = "portal_lab",
                label = "Laboratorio",
                pos = GridPos(12, 2),
                action = PortalAction.OpenScienceLabPicker
            ),
            HubPortal(
                id = "portal_library",
                label = "Biblioteca",
                pos = GridPos(16, 2),
                action = PortalAction.OpenLibraryPicker
            ),
            HubPortal(
                id = "portal_geo",
                label = "Geografia",
                pos = GridPos(20, 2),
                action = PortalAction.OpenGeoPicker
            ),
            HubPortal(
                id = "portal_english",
                label = "Inglese",
                pos = GridPos(24, 2),
                action = PortalAction.OpenEnglishPicker
            ),
            HubPortal(
                id = "portal_to_yard",
                label = "Cortile",
                pos = GridPos(14, 13),
                action = PortalAction.GoToHub("school_yard", "from_hallway")
            )
        )

        val spawns = mapOf(
            "from_yard" to GridPos(14, 11),
            "from_gym" to GridPos(4, 4),
            "from_locker" to GridPos(8, 4),
            "from_lab" to GridPos(12, 4),
            "from_library" to GridPos(16, 4),
            "from_geo" to GridPos(20, 4),
            "from_english" to GridPos(24, 4),
            "default" to GridPos(14, 8)
        )

        return SchoolMapData(
            id = "school_hallway",
            width = w,
            height = h,
            tiles = grid,
            spawns = spawns,
            npcs = emptyList(),
            portals = portals
        )
    }

    /**
     * 4-direction A* pathfinding (DIAGONAL_MODE_NEVER) with allowPartialPath = true.
     * If target is an NPC or solid door, walks right up to the closest adjacent walkable tile.
     */
    fun findPath4Dir(
        map: SchoolMapData,
        start: GridPos,
        goal: GridPos,
        allowPartialPath: Boolean = true
    ): List<GridPos> {
        if (start == goal) return emptyList()

        data class Node(val pos: GridPos, val g: Int, val f: Int)

        val dirs = listOf(
            GridPos(0, -1), GridPos(0, 1), GridPos(-1, 0), GridPos(1, 0)
        )
        fun heuristic(a: GridPos, b: GridPos): Int = abs(a.x - b.x) + abs(a.y - b.y)

        val open = PriorityQueue<Node>(compareBy { it.f })
        val cameFrom = mutableMapOf<GridPos, GridPos>()
        val gScore = mutableMapOf(start to 0)
        val closed = mutableSetOf<GridPos>()

        var closestNode = start
        var closestDist = heuristic(start, goal)

        open.add(Node(start, 0, closestDist))

        while (open.isNotEmpty()) {
            val current = open.poll() ?: break
            if (current.pos == goal) {
                return reconstructPath(cameFrom, current.pos, start)
            }
            if (!closed.add(current.pos)) continue

            val distToGoal = heuristic(current.pos, goal)
            if (distToGoal < closestDist) {
                closestDist = distToGoal
                closestNode = current.pos
            }

            for (d in dirs) {
                val next = GridPos(current.pos.x + d.x, current.pos.y + d.y)
                if (next in closed) continue
                val portalAtNext = map.findPortalAt(next)
                val canStep = next == goal && portalAtNext != null && portalAtNext.action !is PortalAction.WipDoor && !map.tileAt(next.x, next.y).solid
                if (!map.isWalkable(next.x, next.y) && !canStep) continue

                val tentativeG = current.g + 1
                if (tentativeG < (gScore[next] ?: Int.MAX_VALUE)) {
                    cameFrom[next] = current.pos
                    gScore[next] = tentativeG
                    open.add(Node(next, tentativeG, tentativeG + heuristic(next, goal)))
                }
            }
        }

        return if (allowPartialPath && closestNode != start) {
            reconstructPath(cameFrom, closestNode, start)
        } else {
            emptyList()
        }
    }

    private fun reconstructPath(
        cameFrom: Map<GridPos, GridPos>,
        end: GridPos,
        start: GridPos
    ): List<GridPos> {
        val path = mutableListOf<GridPos>()
        var cur: GridPos? = end
        while (cur != null && cur != start) {
            path.add(cur)
            cur = cameFrom[cur]
        }
        return path.reversed()
    }
}
