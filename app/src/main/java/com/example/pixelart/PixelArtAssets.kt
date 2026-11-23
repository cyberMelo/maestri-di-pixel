package com.example.pixelart

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Tipo di piastrella della griglia logica; [symbol] è il carattere usato in resources/maps/<id>.txt. */
enum class TileKind(val solid: Boolean, val symbol: Char) {
    GRASS_PLAIN(false, 'G'),
    GRASS_FLOWERS(false, 'f'),
    PATH_STONE(false, 'p'),
    FENCE_H(true, '='),
    FENCE_V(true, '|'),
    TREE_BUSH(true, 'T'),
    SCHOOL_WALL_EXT(true, 'W'),
    SCHOOL_ROOF(true, 'R'),
    HALL_FLOOR_A(false, 'a'),
    HALL_FLOOR_B(false, 'b'),
    HALL_CARPET(false, 'c'),
    HALL_WALL(true, 'w'),
    HALL_WALL_TOP(true, 't'),
    DOOR_ACTIVE(false, 'D'),
    DOOR_WIP(true, 'X'),
    DOOR_GYM(false, '1'),
    DOOR_HISTORY(false, '2'),
    DOOR_LOCKER(false, '3'),
    DOOR_LIBRARY(false, '4'),
    DOOR_GEO(false, '5'),
    DOOR_ENGLISH(false, '6'),
    DOORMAT(false, 'm'),
    WINDOW_EXT(true, 'O'),
    WINDOW_INT(true, 'o'),
    LOCKER_PROP(true, 'L'),
    NOTICE_BOARD(true, 'N'),
    BENCH(true, 'B'),
    INDOOR_BENCH(true, 'n'),
    TROPHY_CASE(true, 'C'),
    BOOKSHELF(true, 'S'),
    STUDY_DESK(true, 'd'),
    POTTED_PLANT(true, 'P');

    companion object {
        private val bySymbol = entries.associateBy { it.symbol }
        fun fromSymbol(c: Char): TileKind = bySymbol[c] ?: error("Simbolo di piastrella sconosciuto: '$c'")
    }
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


    private val spriteCache = mutableMapOf<String, ImageBitmap>()
    private val sheetCache = mutableMapOf<String, Bitmap?>()

    /** Lato del fotogramma LPC e ritaglio quadrato che contiene tutti i personaggi, piedi sul bordo. */
    private const val LPC_FRAME = 64
    private const val CROP_X = 3
    private const val CROP_Y = 6
    const val CHARACTER_PX = 58

    /** Piastrella dei disegni LPC: un fotogramma personaggio di [CHARACTER_PX] px equivale a 1,8 piastrelle. */
    const val TILE_PX = 32

    /** Fogli PNG in src/main/resources (classpath): disponibili sia nell'app sia nei test JVM. */
    private fun sheet(path: String): Bitmap? = sheetCache.getOrPut(path) {
        PixelArtAssets::class.java.getResourceAsStream("/sprites/$path")?.use { BitmapFactory.decodeStream(it) }
    }

    /** Sfondo dipinto della mappa (tools/art/build_maps.py), con un bordo extra oltre la griglia. */
    fun getMapBackground(hubId: String): ImageBitmap? {
        val key = "map_$hubId"
        spriteCache[key]?.let { return it }
        val bmp = sheet("maps/$hubId.png") ?: return null
        return bmp.asImageBitmap().also { spriteCache[key] = it }
    }

    /** Sfondo dell'aula per il palco dei mini-giochi (english, library, lab), senza bordo extra. */
    fun getRoomBackground(roomId: String): ImageBitmap? {
        val key = "room_$roomId"
        spriteCache[key]?.let { return it }
        val bmp = sheet("rooms/$roomId.png") ?: return null
        return bmp.asImageBitmap().also { spriteCache[key] = it }
    }

    /** Righe dei cicli di camminata LPC: 0 su, 1 sinistra, 2 giù, 3 destra. */
    private fun lpcRow(facing: Facing): Int = when (facing) {
        Facing.UP -> 0
        Facing.LEFT -> 1
        Facing.DOWN -> 2
        Facing.RIGHT -> 3
    }

    /**
     * Fotogramma quadrato del personaggio ([CHARACTER_PX] px, piedi sul bordo inferiore).
     * stepFrame 0 = fermo, 1 e 2 = i due passi del ciclo di camminata.
     */
    fun getCharacterSprite(id: String, facing: Facing, stepFrame: Int, hatId: String = ""): ImageBitmap {
        val col = when (stepFrame.mod(3)) {
            1 -> 2
            2 -> 6
            else -> 0
        }
        val row = lpcRow(facing)
        val key = "char_${id}_${row}_${col}_$hatId"
        return spriteCache.getOrPut(key) {
            val base = sheet("characters/$id.png") ?: sheet("characters/avatar_0.png")
                ?: error("Sprite personaggi mancanti in resources/sprites/characters")
            val out = Bitmap.createBitmap(CHARACTER_PX, CHARACTER_PX, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(out)
            val src = android.graphics.Rect(
                col * LPC_FRAME + CROP_X, row * LPC_FRAME + CROP_Y,
                col * LPC_FRAME + CROP_X + CHARACTER_PX, row * LPC_FRAME + CROP_Y + CHARACTER_PX
            )
            val dst = android.graphics.Rect(0, 0, CHARACTER_PX, CHARACTER_PX)
            canvas.drawBitmap(base, src, dst, null)
            if (hatId.isNotBlank()) {
                sheet("hats/$hatId.png")?.let { canvas.drawBitmap(it, src, dst, null) }
            }
            out.asImageBitmap()
        }
    }

    /** Icona del cappello per l'armadietto: il cappello visto di fronte, ritagliato sul contenuto. */
    fun getHatIcon(id: String): ImageBitmap {
        val key = "hat_icon_$id"
        return spriteCache.getOrPut(key) {
            val hat = sheet("hats/$id.png") ?: return@getOrPut makeBitmap(16) { _, _ -> }
            val row = lpcRow(Facing.DOWN) * LPC_FRAME
            var minX = LPC_FRAME; var minY = LPC_FRAME; var maxX = -1; var maxY = -1
            for (y in 0 until LPC_FRAME) for (x in 0 until LPC_FRAME) {
                if ((hat.getPixel(x, row + y) ushr 24) != 0) {
                    if (x < minX) minX = x; if (x > maxX) maxX = x
                    if (y < minY) minY = y; if (y > maxY) maxY = y
                }
            }
            if (maxX < 0) return@getOrPut makeBitmap(16) { _, _ -> }
            val side = maxOf(maxX - minX, maxY - minY) + 3
            val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
            val ox = (side - (maxX - minX + 1)) / 2
            val oy = (side - (maxY - minY + 1)) / 2
            android.graphics.Canvas(out).drawBitmap(
                hat,
                android.graphics.Rect(minX, row + minY, maxX + 1, row + maxY + 1),
                android.graphics.Rect(ox, oy, ox + maxX - minX + 1, oy + maxY - minY + 1),
                null
            )
            out.asImageBitmap()
        }
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
