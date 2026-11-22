package com.example.pixelart

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

enum class TileKind(val solid: Boolean) {
    GRASS_PLAIN(false),
    GRASS_FLOWERS(false),
    PATH_STONE(false),
    FENCE_H(true),
    FENCE_V(true),
    TREE_BUSH(true),
    SCHOOL_WALL_EXT(true),
    SCHOOL_ROOF(true),
    HALL_FLOOR_A(false),
    HALL_FLOOR_B(false),
    HALL_CARPET(false),
    HALL_WALL(true),
    HALL_WALL_TOP(true),
    DOOR_ACTIVE(false),
    DOOR_WIP(true),
    DOOR_GYM(false),
    DOOR_HISTORY(false),
    DOOR_LOCKER(false),
    DOOR_LIBRARY(false),
    DOOR_GEO(false),
    DOOR_ENGLISH(false),
    DOORMAT(false),
    WINDOW_EXT(true),
    WINDOW_INT(true),
    LOCKER_PROP(true),
    NOTICE_BOARD(true),
    BENCH(true),
    INDOOR_BENCH(true),
    TROPHY_CASE(true),
    BOOKSHELF(true),
    STUDY_DESK(true),
    POTTED_PLANT(true)
}

enum class Facing(val row: Int) {
    DOWN(0),
    LEFT(1),
    RIGHT(2),
    UP(3)
}

object PixelArtAssets {
    private fun hex(c: Long): Int = c.toInt()

    private fun makeBitmap(
        size: Int = 16,
        draw: (px: (Int, Int, Int) -> Unit, rect: (Int, Int, Int, Int, Int) -> Unit) -> Unit
    ): ImageBitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val px: (Int, Int, Int) -> Unit = { x, y, color ->
            if (x in 0 until size && y in 0 until size) bmp.setPixel(x, y, color)
        }
        val rect: (Int, Int, Int, Int, Int) -> Unit = { x0, y0, w, h, color ->
            for (y in y0 until (y0 + h)) {
                for (x in x0 until (x0 + w)) {
                    px(x, y, color)
                }
            }
        }
        draw(px, rect)
        return bmp.asImageBitmap()
    }

    val tiles: Map<TileKind, ImageBitmap> by lazy {
        TileKind.entries.associateWith { generateTile(it) }
    }

    private val spriteCache = mutableMapOf<String, ImageBitmap>()

    fun getCharacterSprite(id: String, facing: Facing, stepFrame: Int, hatId: String = ""): ImageBitmap {
        val frame = stepFrame.mod(3) // 0=idle, 1=left, 2=right
        val key = "char_${id}_${facing.row}_${frame}_$hatId"
        return spriteCache.getOrPut(key) { generateCharacter(id, facing, frame, hatId) }
    }

    fun getHatIcon(id: String): ImageBitmap {
        val key = "hat_icon_$id"
        return spriteCache.getOrPut(key) { generateHatIcon(id) }
    }

    fun getCompanionSprite(id: String, frame: Int): ImageBitmap {
        val f = frame.mod(2)
        val key = "pet_${id}_$f"
        return spriteCache.getOrPut(key) { generateCompanion(id, f) }
    }

    fun getStickerIcon(id: String): ImageBitmap {
        val key = "sticker_$id"
        return spriteCache.getOrPut(key) { generateSticker(id) }
    }

    private fun generateTile(kind: TileKind): ImageBitmap = makeBitmap(16) { px, rect ->
        when (kind) {
            TileKind.GRASS_PLAIN -> {
                rect(0, 0, 16, 16, hex(0xFF75C66A))
                // Sunlit grass tufts + soft shadow blades
                px(2, 3, hex(0xFF59AB4E)); px(3, 4, hex(0xFF59AB4E)); px(4, 3, hex(0xFF93DC84))
                px(10, 11, hex(0xFF59AB4E)); px(11, 10, hex(0xFF59AB4E)); px(12, 11, hex(0xFF93DC84))
                px(13, 3, hex(0xFF93DC84)); px(14, 4, hex(0xFF59AB4E))
                px(5, 13, hex(0xFF93DC84)); px(6, 12, hex(0xFF59AB4E))
                px(8, 6, hex(0xFF83D276)); px(1, 10, hex(0xFF83D276))
            }
            TileKind.GRASS_FLOWERS -> {
                rect(0, 0, 16, 16, hex(0xFF75C66A))
                px(1, 8, hex(0xFF59AB4E)); px(14, 13, hex(0xFF59AB4E))
                // Yellow marigold
                px(3, 3, hex(0xFFFEF08A)); px(4, 3, hex(0xFFF59E0B)); px(3, 4, hex(0xFFFDE047))
                px(3, 5, hex(0xFF3F8E36)); px(4, 5, hex(0xFF59AB4E))
                // White daisy
                px(11, 9, hex(0xFFFFFFFF)); px(12, 10, hex(0xFFFFFFFF)); px(10, 10, hex(0xFFFFFFFF))
                px(11, 10, hex(0xFFFACC15)); px(11, 11, hex(0xFF3F8E36))
                // Pink blossom
                px(8, 2, hex(0xFFF472B6)); px(9, 2, hex(0xFFFBCFE8)); px(8, 3, hex(0xFF3F8E36))
                px(6, 12, hex(0xFF93DC84))
            }
            TileKind.PATH_STONE -> {
                rect(0, 0, 16, 16, hex(0xFFD5C8B0))
                // Beveled warm cobblestones
                rect(1, 1, 6, 5, hex(0xFFF3ECE0)); rect(1, 1, 6, 1, hex(0xFFFAF6EE)); rect(1, 5, 6, 1, hex(0xFFC5B59B))
                rect(8, 1, 7, 5, hex(0xFFEDE4D2)); rect(8, 1, 7, 1, hex(0xFFF7F2E8)); rect(8, 5, 7, 1, hex(0xFFC5B59B))
                rect(1, 8, 7, 6, hex(0xFFEDE4D2)); rect(1, 8, 7, 1, hex(0xFFF7F2E8)); rect(1, 13, 7, 1, hex(0xFFC5B59B))
                rect(9, 8, 6, 6, hex(0xFFF3ECE0)); rect(9, 8, 6, 1, hex(0xFFFAF6EE)); rect(9, 13, 6, 1, hex(0xFFC5B59B))
                // Grout lines + speckles
                px(4, 3, hex(0xFFE0D5C0)); px(11, 10, hex(0xFFE0D5C0))
            }
            TileKind.FENCE_H -> {
                rect(0, 0, 16, 16, hex(0xFF75C66A))
                rect(0, 14, 16, 2, hex(0xFF55A24B)) // cast shadow
                rect(0, 5, 16, 3, hex(0xFF92400E)); rect(0, 5, 16, 1, hex(0xFFD97706))
                rect(0, 10, 16, 3, hex(0xFF92400E)); rect(0, 10, 16, 1, hex(0xFFD97706))
                rect(2, 2, 3, 13, hex(0xFFB45309)); rect(2, 2, 2, 12, hex(0xFFD97706)); rect(2, 2, 3, 1, hex(0xFFFDE68A))
                rect(11, 2, 3, 13, hex(0xFFB45309)); rect(11, 2, 2, 12, hex(0xFFD97706)); rect(11, 2, 3, 1, hex(0xFFFDE68A))
            }
            TileKind.FENCE_V -> {
                rect(0, 0, 16, 16, hex(0xFF75C66A))
                rect(10, 0, 2, 16, hex(0xFF55A24B))
                rect(6, 0, 4, 16, hex(0xFF92400E)); rect(6, 0, 2, 16, hex(0xFFD97706))
                rect(5, 2, 6, 3, hex(0xFFB45309)); rect(5, 2, 6, 1, hex(0xFFFDE68A))
                rect(5, 10, 6, 3, hex(0xFFB45309)); rect(5, 10, 6, 1, hex(0xFFFDE68A))
            }
            TileKind.TREE_BUSH -> {
                rect(0, 0, 16, 16, hex(0xFF75C66A))
                // Ground shadow oval
                rect(3, 13, 10, 3, hex(0xFF478F3E))
                // Shaded trunk
                rect(6, 10, 4, 5, hex(0xFF78350F)); rect(6, 10, 2, 5, hex(0xFF92400E))
                // Multi-layered foliage canopy
                rect(2, 3, 12, 9, hex(0xFF14532D))
                rect(2, 2, 12, 8, hex(0xFF15803D))
                rect(3, 1, 10, 7, hex(0xFF16A34A))
                rect(4, 2, 5, 3, hex(0xFF4ADE80))
                rect(9, 4, 3, 3, hex(0xFF22C55E))
                px(5, 2, hex(0xFF86EFAC)); px(6, 2, hex(0xFF86EFAC)); px(10, 4, hex(0xFF86EFAC))
                // Tiny red apple/berry accents
                px(4, 7, hex(0xFFF87171)); px(11, 7, hex(0xFFF87171))
            }
            TileKind.SCHOOL_WALL_EXT -> {
                rect(0, 0, 16, 16, hex(0xFFD97757))
                // Subtle sunlit brick highlights
                rect(1, 1, 3, 2, hex(0xFFE58C6E))
                rect(9, 5, 3, 2, hex(0xFFE58C6E))
                rect(5, 9, 3, 2, hex(0xFFE58C6E))
                for (y in listOf(0, 4, 8, 12)) {
                    rect(0, y, 16, 1, hex(0xFFA84A32))
                }
                for (x in listOf(4, 12)) {
                    rect(x, 1, 1, 3, hex(0xFFA84A32))
                    rect(x, 9, 1, 3, hex(0xFFA84A32))
                }
                for (x in listOf(0, 8)) {
                    rect(x, 5, 1, 3, hex(0xFFA84A32))
                    rect(x, 13, 1, 3, hex(0xFFA84A32))
                }
                // Stone base plinth
                rect(0, 13, 16, 3, hex(0xFF94A3B8))
                rect(0, 13, 16, 1, hex(0xFFCBD5E1))
                rect(0, 15, 16, 1, hex(0xFF64748B))
            }
            TileKind.SCHOOL_ROOF -> {
                rect(0, 0, 16, 16, hex(0xFFB91C1C))
                for (y in listOf(0, 4, 8, 12)) {
                    rect(0, y, 16, 1, hex(0xFFEF4444))
                    rect(0, y + 1, 16, 2, hex(0xFFDC2626))
                    rect(0, y + 3, 16, 1, hex(0xFF7F1D1D))
                }
                // Scalloped tile vertical ridges
                for (x in listOf(3, 7, 11, 15)) {
                    px(x, 1, hex(0xFF991B1B)); px(x, 2, hex(0xFF991B1B))
                    px(x, 9, hex(0xFF991B1B)); px(x, 10, hex(0xFF991B1B))
                }
                for (x in listOf(1, 5, 9, 13)) {
                    px(x, 5, hex(0xFF991B1B)); px(x, 6, hex(0xFF991B1B))
                    px(x, 13, hex(0xFF991B1B)); px(x, 14, hex(0xFF991B1B))
                }
            }
            TileKind.HALL_FLOOR_A -> {
                rect(0, 0, 16, 16, hex(0xFFF6F1E7))
                rect(1, 1, 14, 1, hex(0xFFFFFCF5)) // glossy tile sheen
                rect(0, 0, 16, 1, hex(0xFFE2D7C5))
                rect(0, 0, 1, 16, hex(0xFFE2D7C5))
                px(3, 3, hex(0xFFFFFFFF)); px(4, 3, hex(0xFFFFFFFF))
            }
            TileKind.HALL_FLOOR_B -> {
                rect(0, 0, 16, 16, hex(0xFFEBE2D1))
                rect(1, 1, 14, 1, hex(0xFFF4EDE0))
                rect(0, 0, 16, 1, hex(0xFFD8CCB6))
                rect(0, 0, 1, 16, hex(0xFFD8CCB6))
            }
            TileKind.HALL_CARPET -> {
                // Royal-blue school atrium carpet with warm gold stitched border & diamond weave
                rect(0, 0, 16, 16, hex(0xFF1D4ED8))
                rect(0, 0, 16, 1, hex(0xFFFBBF24))
                rect(0, 15, 16, 1, hex(0xFFFBBF24))
                rect(0, 0, 1, 16, hex(0xFFD97706))
                rect(15, 0, 1, 16, hex(0xFFD97706))
                rect(2, 2, 12, 12, hex(0xFF2563EB))
                // Subtle woven center motif
                px(7, 6, hex(0xFF60A5FA)); px(8, 6, hex(0xFF60A5FA))
                px(6, 7, hex(0xFF60A5FA)); px(9, 7, hex(0xFF60A5FA))
                px(7, 8, hex(0xFFFDE047)); px(8, 8, hex(0xFFFDE047))
                px(6, 9, hex(0xFF60A5FA)); px(9, 9, hex(0xFF60A5FA))
                px(7, 10, hex(0xFF60A5FA)); px(8, 10, hex(0xFF60A5FA))
            }
            TileKind.HALL_WALL -> {
                rect(0, 0, 16, 11, hex(0xFFE2E8F0))
                rect(0, 0, 16, 2, hex(0xFFF1F5F9)) // crown trim
                rect(0, 10, 16, 1, hex(0xFF94A3B8))
                rect(0, 11, 16, 4, hex(0xFFCBD5E1)) // wainscoting
                rect(0, 15, 16, 1, hex(0xFF64748B))
            }
            TileKind.HALL_WALL_TOP -> {
                // 2.5D top-down architectural wall cap separating protruding classroom wings
                rect(0, 0, 16, 16, hex(0xFF334155))
                rect(0, 0, 16, 2, hex(0xFF1E293B))
                rect(1, 2, 14, 11, hex(0xFF475569))
                rect(1, 2, 14, 2, hex(0xFF64748B))
                rect(0, 14, 16, 2, hex(0xFF1E293B))
            }
            TileKind.DOOR_ACTIVE -> {
                rect(0, 0, 16, 16, hex(0xFF334155))
                rect(1, 1, 14, 15, hex(0xFF0F766E)) // teal frame
                rect(2, 2, 12, 14, hex(0xFF92400E))
                rect(3, 3, 10, 12, hex(0xFFB45309))
                rect(3, 3, 2, 12, hex(0xFFD97706)) // wood highlight
                // Window pane with diagonal gleam
                rect(4, 4, 8, 5, hex(0xFF0284C7))
                rect(5, 5, 6, 3, hex(0xFF38BDF8))
                px(5, 5, hex(0xFFE0F2FE)); px(6, 5, hex(0xFFE0F2FE)); px(5, 6, hex(0xFFE0F2FE))
                // Brass doorknob
                rect(11, 10, 2, 2, hex(0xFFFBBF24)); px(11, 10, hex(0xFFFEF08A))
            }
            TileKind.DOOR_WIP -> {
                rect(0, 0, 16, 16, hex(0xFF334155))
                rect(2, 2, 12, 14, hex(0xFF64748B))
                rect(3, 3, 10, 12, hex(0xFF475569))
                for (i in 0 until 12) {
                    val col = if ((i / 3) % 2 == 0) hex(0xFFFACC15) else hex(0xFF1E293B)
                    rect(2 + i, 6, 1, 4, col)
                }
            }
            TileKind.DOOR_GYM -> {
                rect(0, 0, 16, 16, hex(0xFF991B1B))
                rect(1, 1, 14, 15, hex(0xFFDC2626))
                rect(2, 2, 12, 14, hex(0xFFEF4444))
                rect(7, 2, 2, 14, hex(0xFF7F1D1D)) // double-door split
                // Porthole windows with reflection
                rect(3, 4, 3, 5, hex(0xFF38BDF8)); px(4, 5, hex(0xFFE0F2FE))
                rect(10, 4, 3, 5, hex(0xFF38BDF8)); px(11, 5, hex(0xFFE0F2FE))
                // Metallic pushbars
                rect(3, 11, 3, 1, hex(0xFFE2E8F0)); rect(10, 11, 3, 1, hex(0xFFE2E8F0))
            }
            TileKind.DOOR_HISTORY -> {
                // Warm antique terracotta & bronze museum door with parchment scroll emblem
                rect(0, 0, 16, 16, hex(0xFF78350F))
                rect(1, 1, 14, 15, hex(0xFFB45309))
                rect(2, 2, 12, 14, hex(0xFFD97706))
                rect(3, 3, 10, 12, hex(0xFF92400E))
                // Golden scroll / ancient temple plaque on upper door
                rect(4, 4, 8, 5, hex(0xFFFEF3C7))
                rect(4, 4, 8, 1, hex(0xFFF59E0B))
                rect(4, 8, 8, 1, hex(0xFFF59E0B))
                rect(6, 6, 4, 1, hex(0xFF92400E))
                // Brass doorknob
                rect(11, 11, 2, 2, hex(0xFFFDE047))
            }
            TileKind.DOOR_LOCKER -> {
                rect(0, 0, 16, 16, hex(0xFFB45309))
                rect(2, 2, 12, 14, hex(0xFFD97706))
                rect(3, 3, 10, 12, hex(0xFFF59E0B))
                rect(4, 4, 8, 4, hex(0xFFFEF3C7)) // golden plaque
                rect(6, 5, 4, 2, hex(0xFFFBBF24)) // star crest
                rect(11, 10, 2, 2, hex(0xFF1E293B))
            }
            TileKind.DOOR_LIBRARY -> {
                // Warm plum & oak library door with golden open-book emblem
                rect(0, 0, 16, 16, hex(0xFF4C1D95))
                rect(1, 1, 14, 15, hex(0xFF6D28D9))
                rect(2, 2, 12, 14, hex(0xFF7C3AED))
                rect(3, 3, 10, 12, hex(0xFF5B21B6))
                // Open book crest
                rect(4, 4, 3, 4, hex(0xFFFEF9C3)); rect(9, 4, 3, 4, hex(0xFFFEF9C3))
                rect(7, 4, 2, 4, hex(0xFFF59E0B))
                // Golden handle
                rect(11, 10, 2, 2, hex(0xFFFBBF24))
            }
            TileKind.DOOR_GEO -> {
                // Sky & emerald geography door with pixel globe icon
                rect(0, 0, 16, 16, hex(0xFF0C4A6E))
                rect(1, 1, 14, 15, hex(0xFF0284C7))
                rect(2, 2, 12, 14, hex(0xFF0369A1))
                rect(3, 3, 10, 12, hex(0xFF0EA5E9))
                // Little pixel globe
                rect(5, 4, 6, 5, hex(0xFF38BDF8))
                rect(6, 5, 2, 2, hex(0xFF22C55E)); rect(9, 6, 2, 2, hex(0xFF16A34A))
                rect(11, 11, 2, 2, hex(0xFFFEF08A))
            }
            TileKind.DOOR_ENGLISH -> {
                // Royal-indigo & crimson English classroom door with bright speech/flag motif
                rect(0, 0, 16, 16, hex(0xFF1E3A8A))
                rect(1, 1, 14, 15, hex(0xFF1D4ED8))
                rect(2, 2, 12, 14, hex(0xFF2563EB))
                // Classic red/white cross badge on upper door panel
                rect(4, 4, 8, 5, hex(0xFFFFFFFF))
                rect(7, 4, 2, 5, hex(0xFFDC2626))
                rect(4, 6, 8, 1, hex(0xFFDC2626))
                // Brass doorknob
                rect(11, 11, 2, 2, hex(0xFFFBBF24))
            }
            TileKind.DOORMAT -> {
                rect(0, 0, 16, 16, hex(0xFFE7DEC8))
                rect(2, 3, 12, 10, hex(0xFF78350F))
                rect(3, 4, 10, 8, hex(0xFFD97706))
                rect(4, 5, 8, 6, hex(0xFFF59E0B))
                rect(6, 7, 4, 2, hex(0xFFFEF3C7))
            }
            TileKind.WINDOW_EXT -> {
                rect(0, 0, 16, 16, hex(0xFFD97757))
                rect(1, 2, 14, 13, hex(0xFF94A3B8)) // sill shadow
                rect(2, 2, 12, 12, hex(0xFFFAF8F2))
                rect(3, 3, 10, 10, hex(0xFF0284C7))
                rect(3, 3, 10, 5, hex(0xFF38BDF8))
                // Diagonal glass reflection
                px(4, 4, hex(0xFFE0F2FE)); px(5, 4, hex(0xFFE0F2FE)); px(4, 5, hex(0xFFE0F2FE))
                px(9, 9, hex(0xFF7DD3FC)); px(10, 8, hex(0xFF7DD3FC))
                rect(7, 3, 2, 10, hex(0xFFFAF8F2))
                rect(3, 7, 10, 2, hex(0xFFFAF8F2))
                rect(1, 13, 14, 2, hex(0xFFE2E8F0)) // window ledge
            }
            TileKind.WINDOW_INT -> {
                rect(0, 0, 16, 16, hex(0xFFE2E8F0))
                rect(2, 2, 12, 10, hex(0xFFFAF8F2))
                rect(3, 3, 10, 8, hex(0xFF38BDF8))
                px(4, 4, hex(0xFFE0F2FE)); px(5, 4, hex(0xFFE0F2FE)); px(4, 5, hex(0xFFE0F2FE))
                rect(7, 3, 2, 8, hex(0xFFFAF8F2))
                rect(0, 12, 16, 4, hex(0xFFCBD5E1))
            }
            TileKind.LOCKER_PROP -> {
                rect(0, 0, 16, 16, hex(0xFFE2E8F0))
                rect(1, 1, 14, 14, hex(0xFF1E3A8A))
                rect(2, 2, 5, 12, hex(0xFF2563EB)); rect(2, 2, 2, 12, hex(0xFF3B82F6))
                rect(9, 2, 5, 12, hex(0xFF2563EB)); rect(9, 2, 2, 12, hex(0xFF3B82F6))
                // Louvered vents
                rect(3, 4, 3, 1, hex(0xFF1E3A8A)); rect(3, 6, 3, 1, hex(0xFF1E3A8A))
                rect(10, 4, 3, 1, hex(0xFF1E3A8A)); rect(10, 6, 3, 1, hex(0xFF1E3A8A))
                px(6, 9, hex(0xFFFEF08A)); px(13, 9, hex(0xFFFEF08A))
            }
            TileKind.NOTICE_BOARD -> {
                rect(0, 0, 16, 16, hex(0xFFE2E8F0))
                rect(1, 2, 14, 11, hex(0xFF78350F))
                rect(2, 3, 12, 9, hex(0xFFD97706))
                rect(3, 4, 4, 4, hex(0xFFFAF8F2)); px(5, 4, hex(0xFFDC2626))
                rect(8, 4, 5, 5, hex(0xFFFEF08A)); px(10, 4, hex(0xFF0284C7))
                rect(4, 9, 4, 2, hex(0xFFBAE6FD))
            }
            TileKind.BENCH -> {
                rect(0, 0, 16, 16, hex(0xFF75C66A))
                rect(2, 13, 12, 2, hex(0xFF478F3E)) // shadow
                rect(1, 4, 14, 3, hex(0xFF92400E)); rect(1, 4, 14, 1, hex(0xFFD97706))
                rect(1, 8, 14, 3, hex(0xFFB45309)); rect(1, 8, 14, 1, hex(0xFFF59E0B))
                rect(2, 11, 2, 4, hex(0xFF334155))
                rect(12, 11, 2, 4, hex(0xFF334155))
            }
            TileKind.INDOOR_BENCH -> {
                rect(0, 0, 16, 16, hex(0xFFF6F1E7))
                rect(2, 13, 12, 2, hex(0xFFD6CCBA)) // soft indoor floor shadow
                rect(1, 4, 14, 3, hex(0xFF92400E)); rect(1, 4, 14, 1, hex(0xFFD97706))
                rect(1, 8, 14, 3, hex(0xFFB45309)); rect(1, 8, 14, 1, hex(0xFFF59E0B))
                rect(2, 11, 2, 4, hex(0xFF334155))
                rect(12, 11, 2, 4, hex(0xFF334155))
            }
            TileKind.TROPHY_CASE -> {
                // School atrium golden trophy & medal display cabinet
                rect(0, 0, 16, 16, hex(0xFF1D4ED8)) // sits on carpet
                rect(1, 1, 14, 14, hex(0xFF78350F)) // mahogany frame
                rect(2, 2, 12, 8, hex(0xFF0F172A)) // velvet glass interior
                rect(2, 2, 12, 2, hex(0xFF1E3A8A)) // top glass reflection
                // Golden trophy cup in center
                rect(6, 3, 4, 3, hex(0xFFFACC15))
                px(5, 4, hex(0xFFF59E0B)); px(10, 4, hex(0xFFF59E0B)) // handles
                rect(7, 6, 2, 2, hex(0xFFD97706))
                rect(6, 8, 4, 1, hex(0xFFFDE047))
                // Mini side medals
                px(3, 6, hex(0xFF38BDF8)); px(3, 7, hex(0xFFFBBF24))
                px(12, 6, hex(0xFFEF4444)); px(12, 7, hex(0xFFFBBF24))
                // Wooden base pedestal with gold plaque
                rect(2, 10, 12, 4, hex(0xFFB45309))
                rect(5, 11, 6, 2, hex(0xFFFDE68A))
            }
            TileKind.BOOKSHELF -> {
                // Cozy oak school bookshelf packed with colorful books
                rect(0, 0, 16, 16, hex(0xFF1D4ED8))
                rect(1, 1, 14, 14, hex(0xFF78350F))
                rect(2, 2, 12, 12, hex(0xFF92400E))
                // Top shelf books
                rect(2, 3, 2, 4, hex(0xFFDC2626))
                rect(4, 2, 2, 5, hex(0xFF2563EB))
                rect(6, 3, 2, 4, hex(0xFF16A34A))
                rect(8, 2, 2, 5, hex(0xFFF59E0B))
                rect(10, 3, 2, 4, hex(0xFF7C3AED))
                rect(12, 3, 2, 4, hex(0xFF0284C7))
                rect(2, 7, 12, 1, hex(0xFF451A03))
                // Bottom shelf books
                rect(2, 9, 2, 4, hex(0xFF0D9488))
                rect(4, 8, 3, 5, hex(0xFFE11D48))
                rect(7, 9, 2, 4, hex(0xFF2563EB))
                rect(9, 8, 2, 5, hex(0xFFFACC15))
                rect(11, 9, 3, 4, hex(0xFF15803D))
                rect(2, 13, 12, 1, hex(0xFF451A03))
            }
            TileKind.STUDY_DESK -> {
                // Wooden study/reading desk with open notebook & tiny globe
                rect(0, 0, 16, 16, hex(0xFF1D4ED8))
                rect(2, 13, 12, 2, hex(0xFF1E3A8A)) // cast shadow
                rect(1, 3, 14, 8, hex(0xFFB45309))
                rect(2, 4, 12, 6, hex(0xFFD97706))
                rect(2, 4, 12, 1, hex(0xFFF59E0B))
                // Open white notebook
                rect(3, 5, 5, 4, hex(0xFFFFFFFF))
                rect(5, 5, 1, 4, hex(0xFFCBD5E1))
                px(4, 6, hex(0xFF334155)); px(6, 7, hex(0xFF334155))
                // Mini teal/green globe + yellow pencil
                rect(10, 5, 3, 3, hex(0xFF0EA5E9)); px(11, 6, hex(0xFF22C55E))
                rect(9, 9, 4, 1, hex(0xFFFACC15))
                // Desk legs
                rect(2, 11, 2, 3, hex(0xFF78350F))
                rect(12, 11, 2, 3, hex(0xFF78350F))
            }
            TileKind.POTTED_PLANT -> {
                rect(0, 0, 16, 16, hex(0xFFF6F1E7))
                rect(4, 14, 8, 2, hex(0xFFD6CCBA))
                rect(5, 10, 6, 5, hex(0xFFB45309)); rect(5, 10, 2, 5, hex(0xFFD97706))
                rect(4, 9, 8, 2, hex(0xFFD97706))
                rect(3, 3, 10, 6, hex(0xFF15803D))
                rect(5, 2, 6, 6, hex(0xFF22C55E))
                rect(6, 2, 3, 3, hex(0xFF86EFAC))
            }
        }
    }

    private data class CharPalette(
        val hair: Int,
        val hairHi: Int,
        val shirt: Int,
        val shirtShade: Int,
        val accent: Int,
        val skin: Int
    )

    private fun generateCharacter(id: String, facing: Facing, frame: Int, hatId: String = ""): ImageBitmap = makeBitmap(16) { px, rect ->
        val (hair, hairHi, shirt, shirtShade, accent, skin) = when (id) {
            "avatar_0" -> CharPalette(hex(0xFF5C3A21), hex(0xFF855635), hex(0xFF2563EB), hex(0xFF1D4ED8), hex(0xFFFBBF24), hex(0xFFFED7AA))
            "avatar_1" -> CharPalette(hex(0xFF92400E), hex(0xFFC26421), hex(0xFFDB2777), hex(0xFFBE185D), hex(0xFFFEF08A), hex(0xFFFDBA74))
            "avatar_2" -> CharPalette(hex(0xFF1E293B), hex(0xFF475569), hex(0xFF059669), hex(0xFF047857), hex(0xFF38BDF8), hex(0xFFFED7AA))
            "avatar_3" -> CharPalette(hex(0xFFD97706), hex(0xFFFBBF24), hex(0xFF7C3AED), hex(0xFF6D28D9), hex(0xFFFDE047), hex(0xFFFEE2E2))
            "npc_history" -> CharPalette(hex(0xFFCBD5E1), hex(0xFFF1F5F9), hex(0xFFD97706), hex(0xFFB45309), hex(0xFFFEF3C7), hex(0xFFFED7AA))
            "npc_science" -> CharPalette(hex(0xFF7C2D12), hex(0xFFB45309), hex(0xFFF8FAFC), hex(0xFFCBD5E1), hex(0xFF10B981), hex(0xFFFED7AA))
            "npc_math" -> CharPalette(hex(0xFF1E293B), hex(0xFF475569), hex(0xFFDC2626), hex(0xFFB91C1C), hex(0xFFFEF08A), hex(0xFFFDBA74))
            "npc_library" -> CharPalette(hex(0xFF581C87), hex(0xFF7E22CE), hex(0xFF9333EA), hex(0xFF6B21A8), hex(0xFFFEF08A), hex(0xFFFED7AA))
            "npc_geo" -> CharPalette(hex(0xFF78350F), hex(0xFFB45309), hex(0xFF0284C7), hex(0xFF0369A1), hex(0xFFFDE047), hex(0xFFFDBA74))
            "npc_english" -> CharPalette(hex(0xFFD97706), hex(0xFFFBBF24), hex(0xFF1D4ED8), hex(0xFF1E3A8A), hex(0xFFEF4444), hex(0xFFFEE2E2))
            "npc_boss" -> CharPalette(hex(0xFF312E81), hex(0xFF4F46E5), hex(0xFF7C3AED), hex(0xFF5B21B6), hex(0xFFFBBF24), hex(0xFFFED7AA))
            "npc_janitor" -> CharPalette(hex(0xFF64748B), hex(0xFF94A3B8), hex(0xFF1D4ED8), hex(0xFF1E3A8A), hex(0xFF93C5FD), hex(0xFFFDBA74))
            "npc_classmate" -> CharPalette(hex(0xFF7C2D12), hex(0xFFC2410C), hex(0xFF0D9488), hex(0xFF0F766E), hex(0xFFFDE047), hex(0xFFFED7AA))
            else -> CharPalette(hex(0xFF334155), hex(0xFF64748B), hex(0xFF0284C7), hex(0xFF0369A1), hex(0xFFFBBF24), hex(0xFFFED7AA))
        }
        val outline = hex(0xFF0F172A)
        val pants = hex(0xFF334155)
        val shoes = hex(0xFF1E293B)

        // Ground contact shadow
        rect(3, 14, 10, 2, hex(0x55000000))

        // Head with crisp dark outline
        rect(4, 1, 8, 7, outline)
        rect(5, 2, 6, 5, skin)

        // Hair + highlight streak
        rect(4, 1, 8, 3, hair)
        rect(5, 1, 4, 1, hairHi)
        when (facing) {
            Facing.UP -> {
                rect(5, 2, 6, 5, hair)
                rect(6, 2, 3, 1, hairHi)
            }
            Facing.DOWN -> {
                // Eyes with crisp catchlights
                px(6, 4, outline); px(9, 4, outline)
                px(5, 5, hex(0xFFFDA4AF)); px(10, 5, hex(0xFFFDA4AF)) // warm cheek tint
                // Smile or beard for Maestro Erodoto
                if (id == "npc_history") {
                    rect(6, 6, 4, 2, hex(0xFFE2E8F0)) // scholar beard
                    px(6, 4, hex(0xFF0284C7)); px(9, 4, hex(0xFF0284C7)) // spectacles glint
                } else {
                    px(7, 6, hex(0xFFB45309)); px(8, 6, hex(0xFFB45309))
                }
            }
            Facing.LEFT -> {
                rect(8, 2, 3, 4, hair)
                px(6, 4, outline)
                px(5, 5, hex(0xFFFDA4AF))
            }
            Facing.RIGHT -> {
                rect(5, 2, 3, 4, hair)
                px(9, 4, outline)
                px(10, 5, hex(0xFFFDA4AF))
            }
        }

        // Role accessories & wearable avatar hats
        if (id == "npc_boss") {
            // Golden 3-point crown
            rect(5, 0, 6, 2, hex(0xFFFBBF24))
            px(5, 0, hex(0xFFFEF08A)); px(8, 0, hex(0xFFEF4444)); px(10, 0, hex(0xFFFEF08A))
        } else if (id == "npc_math") {
            // Athletic headband
            rect(4, 2, 8, 1, hex(0xFFFEF08A))
        } else if (id == "npc_janitor") {
            // Friendly janitor blue cap
            rect(4, 0, 8, 2, hex(0xFF1D4ED8))
            rect(3, 2, 10, 1, hex(0xFF1E3A8A))
            px(7, 1, hex(0xFFFDE047)); px(8, 1, hex(0xFFFDE047))
        } else if (hatId.isNotBlank()) {
            when (hatId) {
                "hat_cap" -> {
                    rect(4, 0, 8, 2, hex(0xFFDC2626))
                    rect(5, 0, 4, 1, hex(0xFFEF4444))
                    rect(3, 2, 10, 1, hex(0xFFB91C1C))
                    px(7, 1, hex(0xFFFFFFFF)); px(8, 1, hex(0xFFFFFFFF))
                }
                "hat_wizard" -> {
                    rect(7, 0, 2, 1, hex(0xFF7C3AED))
                    rect(5, 1, 6, 1, hex(0xFF6D28D9))
                    rect(3, 2, 10, 1, hex(0xFF4C1D95))
                    px(7, 1, hex(0xFFFDE047)); px(8, 1, hex(0xFFFDE047))
                }
                "hat_astro" -> {
                    rect(3, 0, 10, 3, hex(0xFFF8FAFC))
                    rect(4, 1, 8, 2, hex(0xFF38BDF8))
                    px(5, 1, hex(0xFFE0F2FE)); px(6, 1, hex(0xFFE0F2FE))
                }
                "hat_crown" -> {
                    rect(4, 0, 8, 2, hex(0xFFF59E0B))
                    px(4, 0, hex(0xFFFEF08A)); px(6, 0, hex(0xFF0F172A)); px(9, 0, hex(0xFF0F172A)); px(11, 0, hex(0xFFFEF08A))
                    px(7, 1, hex(0xFFEF4444)); px(8, 1, hex(0xFFEF4444))
                }
            }
        }

        // Torso with 16-bit shoulder highlight & bottom shadow
        rect(3, 8, 10, 5, outline)
        rect(4, 8, 8, 4, shirt)
        rect(4, 11, 8, 1, shirtShade)
        rect(6, 9, 4, 2, accent)
        if (id == "npc_science") {
            // Green leaf lab pin + inner teal shirt
            rect(7, 8, 2, 4, hex(0xFF0284C7))
            px(5, 9, hex(0xFF22C55E)); px(5, 10, hex(0xFF16A34A))
        }

        // Animated legs & shoes
        val leftLegY = if (frame == 1) 12 else 13
        val rightLegY = if (frame == 2) 12 else 13
        rect(5, leftLegY, 2, 1, pants); rect(5, leftLegY + 1, 2, 1, shoes)
        rect(9, rightLegY, 2, 1, pants); rect(9, rightLegY + 1, 2, 1, shoes)

        // Swinging arms
        rect(3, 8 + if (frame == 2) 1 else 0, 1, 3, skin)
        rect(12, 8 + if (frame == 1) 1 else 0, 1, 3, skin)
    }

    private fun generateCompanion(id: String, frame: Int): ImageBitmap = makeBitmap(16) { px, rect ->
        val bob = if (frame == 1) -1 else 0
        val outline = hex(0xFF0F172A)
        rect(4, 14, 8, 2, hex(0x55000000))
        when (id) {
            "pet_owl" -> {
                rect(3, 3 + bob, 10, 10, outline)
                rect(4, 4 + bob, 8, 8, hex(0xFF92400E))
                rect(5, 6 + bob, 6, 5, hex(0xFFFEF3C7))
                rect(5, 5 + bob, 2, 2, hex(0xFFFFFFFF)); px(6, 6 + bob, outline)
                rect(9, 5 + bob, 2, 2, hex(0xFFFFFFFF)); px(9, 6 + bob, outline)
                rect(7, 7 + bob, 2, 2, hex(0xFFF59E0B))
            }
            "pet_cat" -> {
                rect(3, 4 + bob, 10, 9, outline)
                rect(4, 5 + bob, 8, 7, hex(0xFFF59E0B))
                rect(4, 3 + bob, 2, 2, hex(0xFFD97706)); rect(10, 3 + bob, 2, 2, hex(0xFFD97706))
                rect(6, 9 + bob, 4, 3, hex(0xFFFEF3C7))
                px(6, 7 + bob, outline); px(9, 7 + bob, outline)
                px(7, 8 + bob, hex(0xFFEC4899)); px(8, 8 + bob, hex(0xFFEC4899))
            }
            "pet_dog" -> {
                rect(3, 4 + bob, 10, 9, outline)
                rect(4, 5 + bob, 8, 7, hex(0xFFB45309))
                rect(2, 6 + bob, 2, 4, hex(0xFF78350F)); rect(12, 6 + bob, 2, 4, hex(0xFF78350F))
                rect(6, 8 + bob, 4, 4, hex(0xFFFDE68A))
                px(6, 7 + bob, outline); px(9, 7 + bob, outline)
                rect(7, 9 + bob, 2, 1, outline)
            }
            "pet_fox" -> {
                // Cute orange pixel fox with fluffy white-tipped tail
                rect(12, 8 + bob, 3, 4, hex(0xFFEA580C))
                rect(13, 8 + bob, 2, 2, hex(0xFFFFFFFF))
                rect(3, 4 + bob, 9, 9, outline)
                rect(4, 5 + bob, 7, 7, hex(0xFFEA580C))
                rect(4, 3 + bob, 2, 2, hex(0xFF9A3412)); rect(9, 3 + bob, 2, 2, hex(0xFF9A3412))
                rect(5, 8 + bob, 5, 4, hex(0xFFFEF3C7))
                px(5, 7 + bob, outline); px(9, 7 + bob, outline)
                px(7, 9 + bob, outline)
            }
            "pet_dragon" -> {
                // Emerald pixel dragon with golden horns & wings
                rect(1, 6 + bob, 3, 4, hex(0xFF15803D)); rect(12, 6 + bob, 3, 4, hex(0xFF15803D))
                rect(4, 2 + bob, 2, 2, hex(0xFFFBBF24)); rect(10, 2 + bob, 2, 2, hex(0xFFFBBF24))
                rect(3, 3 + bob, 10, 10, outline)
                rect(4, 4 + bob, 8, 8, hex(0xFF16A34A))
                rect(5, 8 + bob, 6, 4, hex(0xFFFEF08A))
                px(5, 6 + bob, hex(0xFFFEF08A)); px(6, 6 + bob, outline)
                px(10, 6 + bob, hex(0xFFFEF08A)); px(9, 6 + bob, outline)
                px(7, 8 + bob, hex(0xFFEF4444)); px(8, 8 + bob, hex(0xFFEF4444))
            }
            else -> { // pet_robot
                rect(7, 1 + bob, 2, 2, hex(0xFF38BDF8)); px(7, 1 + bob, hex(0xFFE0F2FE))
                rect(3, 3 + bob, 10, 10, outline)
                rect(4, 4 + bob, 8, 8, hex(0xFF64748B))
                rect(4, 4 + bob, 8, 1, hex(0xFF94A3B8))
                rect(5, 5 + bob, 6, 4, outline)
                rect(6, 6 + bob, 2, 2, hex(0xFF38BDF8)); rect(9, 6 + bob, 2, 2, hex(0xFF38BDF8))
                rect(6, 10 + bob, 4, 1, hex(0xFF4ADE80))
            }
        }
    }

    private fun generateHatIcon(id: String): ImageBitmap = makeBitmap(16) { px, rect ->
        val outline = hex(0xFF0F172A)
        when (id) {
            "hat_cap" -> {
                rect(3, 5, 10, 6, outline)
                rect(4, 6, 8, 4, hex(0xFFDC2626))
                rect(5, 6, 4, 2, hex(0xFFEF4444))
                rect(2, 10, 12, 2, hex(0xFFB91C1C))
                rect(7, 7, 2, 2, hex(0xFFFFFFFF))
            }
            "hat_wizard" -> {
                rect(7, 2, 2, 3, hex(0xFF7C3AED))
                rect(5, 5, 6, 3, hex(0xFF6D28D9))
                rect(4, 8, 8, 3, hex(0xFF5B21B6))
                rect(2, 11, 12, 2, hex(0xFF4C1D95))
                rect(7, 6, 2, 2, hex(0xFFFDE047))
                rect(4, 10, 8, 1, hex(0xFFF59E0B))
            }
            "hat_astro" -> {
                rect(2, 3, 12, 10, outline)
                rect(3, 4, 10, 8, hex(0xFFF8FAFC))
                rect(4, 5, 8, 5, hex(0xFF0284C7))
                rect(5, 6, 6, 3, hex(0xFF38BDF8))
                rect(5, 6, 2, 1, hex(0xFFE0F2FE))
            }
            else -> { // hat_crown
                rect(2, 4, 12, 8, outline)
                rect(3, 5, 2, 6, hex(0xFFF59E0B))
                rect(7, 4, 2, 7, hex(0xFFFBBF24))
                rect(11, 5, 2, 6, hex(0xFFF59E0B))
                rect(3, 8, 10, 3, hex(0xFFF59E0B))
                rect(4, 9, 8, 1, hex(0xFFFEF08A))
                px(7, 8, hex(0xFFEF4444)); px(8, 8, hex(0xFFEF4444))
            }
        }
    }

    private fun generateSticker(id: String): ImageBitmap = makeBitmap(16) { px, rect ->
        val outline = hex(0xFF0F172A)
        when (id) {
            "sticker_rocket" -> {
                rect(5, 1, 6, 11, outline)
                rect(6, 2, 4, 9, hex(0xFFF8FAFC)); rect(8, 2, 2, 9, hex(0xFFCBD5E1))
                rect(7, 1, 2, 2, hex(0xFFDC2626))
                rect(4, 8, 2, 4, hex(0xFFDC2626)); rect(10, 8, 2, 4, hex(0xFFDC2626))
                rect(7, 5, 2, 2, hex(0xFF38BDF8)); px(7, 5, hex(0xFFE0F2FE))
                rect(6, 11, 4, 3, hex(0xFFF59E0B)); rect(7, 12, 2, 2, hex(0xFFFEF08A))
            }
            "sticker_dino" -> {
                rect(4, 2, 8, 12, outline)
                rect(5, 3, 6, 4, hex(0xFF16A34A)); rect(5, 3, 3, 2, hex(0xFF4ADE80))
                rect(4, 7, 7, 5, hex(0xFF22C55E))
                rect(11, 9, 3, 2, hex(0xFF16A34A))
                rect(5, 12, 2, 3, hex(0xFF15803D)); rect(8, 12, 2, 3, hex(0xFF15803D))
                px(7, 4, outline); px(8, 4, hex(0xFFFFFFFF))
            }
            "sticker_planet" -> {
                rect(3, 3, 10, 10, outline)
                rect(4, 4, 8, 8, hex(0xFFF59E0B))
                rect(5, 5, 6, 6, hex(0xFFFBBF24))
                rect(5, 5, 2, 2, hex(0xFFFEF08A))
                rect(1, 7, 14, 2, hex(0xFF7C3AED)); rect(1, 7, 14, 1, hex(0xFFA78BFA))
            }
            "sticker_book" -> {
                rect(1, 2, 14, 12, outline)
                rect(2, 3, 12, 10, hex(0xFF7C3AED))
                rect(3, 4, 4, 8, hex(0xFFFAF8F2)); rect(9, 4, 4, 8, hex(0xFFFAF8F2))
                rect(7, 3, 2, 10, hex(0xFF4C1D95))
            }
            "sticker_trophy" -> {
                rect(3, 1, 10, 8, outline)
                rect(4, 2, 8, 6, hex(0xFFF59E0B)); rect(5, 3, 3, 4, hex(0xFFFEF08A))
                rect(2, 3, 2, 3, hex(0xFFD97706)); rect(12, 3, 2, 3, hex(0xFFD97706))
                rect(7, 8, 2, 3, hex(0xFFD97706))
                rect(4, 11, 8, 3, hex(0xFF92400E)); rect(5, 11, 6, 1, hex(0xFFFBBF24))
            }
            "sticker_lightning" -> {
                rect(8, 1, 4, 4, hex(0xFFFEF08A))
                rect(5, 5, 6, 3, hex(0xFFFACC15))
                rect(6, 8, 4, 4, hex(0xFFF59E0B))
                rect(4, 12, 4, 3, hex(0xFFD97706))
            }
            else -> {
                rect(3, 3, 10, 10, outline)
                rect(4, 4, 8, 8, hex(0xFFFBBF24))
                rect(5, 5, 3, 3, hex(0xFFFEF08A))
            }
        }
    }
}
