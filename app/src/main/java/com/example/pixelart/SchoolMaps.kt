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

data class HubInteractiveProp(
    val id: String,
    val label: String,
    val subtitle: String,
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
    data object OpenTrophyHall : PortalAction
    data object OpenDailyMissions : PortalAction
    data class WipDoor(val roomName: String) : PortalAction
}

data class SchoolMapData(
    val id: String,
    val width: Int,
    val height: Int,
    val tiles: Array<Array<TileKind>>,
    val spawns: Map<String, GridPos>,
    val npcs: List<HubNpc>,
    val portals: List<HubPortal>,
    val props: List<HubInteractiveProp> = emptyList()
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
    fun findPropAt(pos: GridPos): HubInteractiveProp? = props.firstOrNull { it.pos == pos }
}

object SchoolMaps {
    val schoolYard: SchoolMapData by lazy { buildSchoolYard() }
    val schoolHallway: SchoolMapData by lazy { buildSchoolHallway() }


    /**
     * Pianta della mappa da resources/maps/<id>.txt: una riga per fila, un carattere per piastrella
     * (vedi [TileKind.symbol]); le righe che iniziano con # sono commenti. Lo stesso file è usato da
     * tools/art/build_maps.py per dipingere lo sfondo, così grafica e collisioni coincidono sempre.
     */
    private fun loadGrid(mapId: String): Array<Array<TileKind>> {
        val text = SchoolMaps::class.java.getResourceAsStream("/maps/$mapId.txt")
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("Pianta mancante: resources/maps/$mapId.txt")
        val rows = text.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        require(rows.all { it.length == rows[0].length }) { "Righe di lunghezza diversa in $mapId.txt" }
        return Array(rows.size) { y -> Array(rows[y].length) { x -> TileKind.fromSymbol(rows[y][x]) } }
    }

    private fun buildSchoolYard(): SchoolMapData {
        val grid = loadGrid("school_yard")
        val w = grid[0].size
        val h = grid.size

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
        val grid = loadGrid("school_hallway")
        val w = grid[0].size
        val h = grid.size

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

        val hallwayNpcs = listOf(
            HubNpc(
                id = "npc_janitor",
                name = "Bidello Pippo",
                label = "Bidello Pippo",
                spriteId = "npc_janitor",
                subjectFilter = "janitor",
                pos = GridPos(8, 4),
                returnSpawnId = "default"
            ),
            HubNpc(
                id = "npc_classmate",
                name = "Gaia",
                label = "Compagna Gaia",
                spriteId = "npc_classmate",
                subjectFilter = "classmate",
                pos = GridPos(18, 4),
                returnSpawnId = "default"
            )
        )

        val hallwayProps = listOf(
            HubInteractiveProp(
                id = "prop_trophy_case",
                label = "Teca Trofei",
                subtitle = "Medaglie",
                pos = GridPos(13, 6),
                action = PortalAction.OpenTrophyHall
            ),
            HubInteractiveProp(
                id = "prop_missions",
                label = "Missioni",
                subtitle = "Premi Oggi",
                pos = GridPos(9, 2),
                action = PortalAction.OpenDailyMissions
            )
        )

        return SchoolMapData(
            id = "school_hallway",
            width = w,
            height = h,
            tiles = grid,
            spawns = spawns,
            npcs = hallwayNpcs,
            portals = portals,
            props = hallwayProps
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
