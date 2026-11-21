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
    val subtitle: String = "",
    val pos: GridPos,
    val action: PortalAction
)

sealed interface PortalAction {
    data class GoToHub(val targetHubId: String, val targetSpawnId: String) : PortalAction
    data object OpenHistoryPicker : PortalAction
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
                label = "Scuola",
                subtitle = "Aule & Corridoio",
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
        val w = 26
        val h = 16
        val grid = Array(h) { y ->
            Array(w) { x ->
                if ((x + y) % 2 == 0) TileKind.HALL_FLOOR_A else TileKind.HALL_FLOOR_B
            }
        }

        // Outer perimeter walls
        for (x in 0 until w) {
            grid[0][x] = TileKind.HALL_WALL_TOP
            grid[1][x] = TileKind.HALL_WALL
            grid[2][x] = TileKind.HALL_WALL
            grid[h - 1][x] = TileKind.HALL_WALL
        }
        for (y in 0 until h) {
            grid[y][0] = TileKind.HALL_WALL_TOP
            grid[y][w - 1] = TileKind.HALL_WALL_TOP
        }

        // Royal blue & gold carpet runners connecting the foyer, central atrium plaza, and side wings
        for (y in 3..14) {
            for (x in 12..14) grid[y][x] = TileKind.HALL_CARPET
        }
        for (y in 5..9) {
            for (x in 10..16) grid[y][x] = TileKind.HALL_CARPET
        }
        // Carpet welcome strips toward mid & lower side classrooms
        for (x in 4..10) grid[8][x] = TileKind.HALL_CARPET
        for (x in 16..21) grid[8][x] = TileKind.HALL_CARPET
        for (x in 4..12) grid[13][x] = TileKind.HALL_CARPET
        for (x in 14..21) grid[13][x] = TileKind.HALL_CARPET

        // Central Atrium Trophy & Reading Island (3x2 centerpiece on blue-gold carpet)
        grid[6][12] = TileKind.BOOKSHELF
        grid[6][13] = TileKind.TROPHY_CASE
        grid[6][14] = TileKind.BOOKSHELF
        grid[7][12] = TileKind.STUDY_DESK
        grid[7][13] = TileKind.STUDY_DESK
        grid[7][14] = TileKind.STUDY_DESK

        // Corner plants & cozy indoor benches on tiled floor framing the central carpet plaza & foyer
        grid[5][9] = TileKind.POTTED_PLANT
        grid[5][17] = TileKind.POTTED_PLANT
        grid[7][9] = TileKind.INDOOR_BENCH
        grid[7][17] = TileKind.INDOOR_BENCH
        grid[9][9] = TileKind.POTTED_PLANT
        grid[9][17] = TileKind.POTTED_PLANT
        grid[11][9] = TileKind.INDOOR_BENCH
        grid[11][17] = TileKind.INDOOR_BENCH
        grid[14][9] = TileKind.POTTED_PLANT
        grid[14][17] = TileKind.POTTED_PLANT
        grid[3][1] = TileKind.POTTED_PLANT
        grid[3][24] = TileKind.POTTED_PLANT

        // North Wall props strictly BETWEEN the 3 wide-spaced North doors (x = 5, 13, 21)
        grid[1][2] = TileKind.WINDOW_INT
        grid[1][9] = TileKind.NOTICE_BOARD
        grid[1][17] = TileKind.NOTICE_BOARD
        grid[1][23] = TileKind.WINDOW_INT

        grid[2][2] = TileKind.LOCKER_PROP
        grid[2][9] = TileKind.LOCKER_PROP
        grid[2][17] = TileKind.LOCKER_PROP
        grid[2][23] = TileKind.LOCKER_PROP

        // Mid-West Wing (Palestra · Matematica) alcove: x = 1..5, y = 5..7
        for (x in 1..5) {
            grid[5][x] = TileKind.HALL_WALL_TOP
            grid[6][x] = TileKind.HALL_WALL
            grid[7][x] = TileKind.HALL_WALL
        }
        grid[6][1] = TileKind.WINDOW_INT
        grid[7][3] = TileKind.DOOR_GYM
        grid[8][3] = TileKind.DOORMAT

        // Mid-East Wing (Laboratorio · Scienze) alcove: x = 20..24, y = 5..7
        for (x in 20..24) {
            grid[5][x] = TileKind.HALL_WALL_TOP
            grid[6][x] = TileKind.HALL_WALL
            grid[7][x] = TileKind.HALL_WALL
        }
        grid[6][24] = TileKind.WINDOW_INT
        grid[7][22] = TileKind.DOOR_ACTIVE
        grid[8][22] = TileKind.DOORMAT

        // Lower-West Wing (Geografia · Mongolfiera) alcove: x = 1..5, y = 10..12
        for (x in 1..5) {
            grid[10][x] = TileKind.HALL_WALL_TOP
            grid[11][x] = TileKind.HALL_WALL
            grid[12][x] = TileKind.HALL_WALL
        }
        grid[11][1] = TileKind.WINDOW_INT
        grid[12][3] = TileKind.DOOR_GEO
        grid[13][3] = TileKind.DOORMAT

        // Lower-East Wing (Inglese · Parole EN) alcove: x = 20..24, y = 10..12
        for (x in 20..24) {
            grid[10][x] = TileKind.HALL_WALL_TOP
            grid[11][x] = TileKind.HALL_WALL
            grid[12][x] = TileKind.HALL_WALL
        }
        grid[11][24] = TileKind.WINDOW_INT
        grid[12][22] = TileKind.DOOR_ENGLISH
        grid[13][22] = TileKind.DOORMAT

        // North Wing Doors (spaced 8 tiles apart: x = 5, 13, 21)
        // 1. Aula di Storia at (5, 2)
        grid[2][5] = TileKind.DOOR_HISTORY
        grid[3][5] = TileKind.DOORMAT

        // 2. Armadietto & Spaccio (grand center) at (13, 2)
        grid[2][13] = TileKind.DOOR_LOCKER
        grid[3][13] = TileKind.DOORMAT

        // 3. Biblioteca (Italiano) at (21, 2)
        grid[2][21] = TileKind.DOOR_LIBRARY
        grid[3][21] = TileKind.DOORMAT

        // South Exit Door back to Yard centered at (13, 15)
        grid[15][13] = TileKind.DOOR_ACTIVE
        grid[14][13] = TileKind.DOORMAT

        val portals = listOf(
            HubPortal(
                id = "portal_history",
                label = "Aula Storia",
                subtitle = "Storia",
                pos = GridPos(5, 2),
                action = PortalAction.OpenHistoryPicker
            ),
            HubPortal(
                id = "portal_locker",
                label = "Armadietto",
                subtitle = "Premi",
                pos = GridPos(13, 2),
                action = PortalAction.OpenLocker
            ),
            HubPortal(
                id = "portal_library",
                label = "Biblioteca",
                subtitle = "Italiano",
                pos = GridPos(21, 2),
                action = PortalAction.OpenLibraryPicker
            ),
            HubPortal(
                id = "portal_gym",
                label = "Palestra",
                subtitle = "Matematica",
                pos = GridPos(3, 7),
                action = PortalAction.OpenGymPicker
            ),
            HubPortal(
                id = "portal_lab",
                label = "Laboratorio",
                subtitle = "Scienze",
                pos = GridPos(22, 7),
                action = PortalAction.OpenScienceLabPicker
            ),
            HubPortal(
                id = "portal_geo",
                label = "Geografia",
                subtitle = "Mongolfiera",
                pos = GridPos(3, 12),
                action = PortalAction.OpenGeoPicker
            ),
            HubPortal(
                id = "portal_english",
                label = "Inglese",
                subtitle = "Parole EN",
                pos = GridPos(22, 12),
                action = PortalAction.OpenEnglishPicker
            ),
            HubPortal(
                id = "portal_to_yard",
                label = "Cortile",
                subtitle = "Uscita",
                pos = GridPos(13, 15),
                action = PortalAction.GoToHub("school_yard", "from_hallway")
            )
        )

        // Spawns strictly >= 1 walkable tile away from doors (anti-loop rule)
        val spawns = mapOf(
            "from_yard" to GridPos(13, 13),
            "from_history" to GridPos(5, 4),
            "from_locker" to GridPos(13, 4),
            "from_library" to GridPos(21, 4),
            "from_gym" to GridPos(3, 9),
            "from_lab" to GridPos(22, 9),
            "from_geo" to GridPos(3, 14),
            "from_english" to GridPos(22, 14),
            "default" to GridPos(13, 10)
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
