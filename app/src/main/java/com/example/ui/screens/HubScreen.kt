package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.GameBalance
import com.example.core.SfxType
import com.example.pixelart.Facing
import com.example.pixelart.GridPos
import com.example.pixelart.HubNpc
import com.example.pixelart.HubPortal
import com.example.pixelart.PixelArtAssets
import com.example.pixelart.SchoolMapData
import com.example.pixelart.SchoolMaps
import com.example.ui.components.PixelSprite
import com.example.ui.components.VirtualDpad
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.DeepBlueAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.SoftGold
import com.example.ui.theme.WarmAmber
import com.example.viewmodel.ActiveModal
import com.example.viewmodel.AppUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun HubScreen(
    hubId: String,
    spawnId: String,
    uiState: AppUiState,
    onNpcInteract: (HubNpc) -> Unit,
    onPortalTrigger: (com.example.pixelart.PortalAction) -> Unit,
    onGuidanceClick: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenParentGate: () -> Unit,
    onPlayStepSfx: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val map: SchoolMapData = remember(hubId) {
        if (hubId == "school_hallway") SchoolMaps.schoolHallway else SchoolMaps.schoolYard
    }
    val initialSpawn = remember(hubId, spawnId) {
        map.spawns[spawnId] ?: map.spawns["default"] ?: GridPos(map.width / 2, map.height / 2)
    }

    // Player & Companion grid positions
    var playerGrid by remember(hubId, spawnId) { mutableStateOf(initialSpawn) }
    var playerRenderX by remember(hubId, spawnId) { mutableFloatStateOf(initialSpawn.x.toFloat()) }
    var playerRenderY by remember(hubId, spawnId) { mutableFloatStateOf(initialSpawn.y.toFloat()) }
    var facing by remember(hubId, spawnId) { mutableStateOf(Facing.DOWN) }
    var walkFrame by remember(hubId, spawnId) { mutableIntStateOf(0) }
    var companionGrid by remember(hubId, spawnId) {
        val cPos = GridPos(initialSpawn.x, (initialSpawn.y + 1).coerceAtMost(map.height - 2))
        mutableStateOf(if (map.isWalkable(cPos.x, cPos.y)) cPos else initialSpawn)
    }
    var companionRenderX by remember(hubId, spawnId) { mutableFloatStateOf(companionGrid.x.toFloat()) }
    var companionRenderY by remember(hubId, spawnId) { mutableFloatStateOf(companionGrid.y.toFloat()) }

    var pathQueue by remember(hubId, spawnId) { mutableStateOf<List<GridPos>>(emptyList()) }
    var pendingNpc by remember(hubId, spawnId) { mutableStateOf<HubNpc?>(null) }
    var pendingPortal by remember(hubId, spawnId) { mutableStateOf<HubPortal?>(null) }
    val enterTimeMs = remember(hubId, spawnId) { mutableLongStateOf(System.currentTimeMillis()) }

    var ambientTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(450)
            ambientTick = (ambientTick + 1) % 4
        }
    }

    // Priority badge evaluation
    val activePacks = uiState.activePacks
    val storiaPriority = remember(activePacks) { activePacks.any { it.subject == "storia" && it.priority } }
    val scienzePriority = remember(activePacks) { activePacks.any { it.subject == "scienze" && it.priority } }
    val mathPriority = remember(activePacks) { activePacks.any { it.subject == "matematica" && it.priority } }
    val italianoPriority = remember(activePacks) { activePacks.any { it.subject == "italiano" && it.priority } }
    val geografiaPriority = remember(activePacks) { activePacks.any { it.subject == "geografia" && it.priority } }
    val inglesePriority = remember(activePacks) { activePacks.any { it.subject == "inglese" && it.priority } }
    val presidePriority = remember(activePacks) {
        activePacks.any { it.subject != "matematica" && it.priority }
    }

    val isBlocked = uiState.isTransitioning || uiState.activeModal != ActiveModal.None

    // Smooth tile-by-tile movement loop
    LaunchedEffect(pathQueue, isBlocked) {
        if (isBlocked) return@LaunchedEffect
        while (pathQueue.isNotEmpty()) {
            val next = pathQueue.first()
            val startX = playerRenderX
            val startY = playerRenderY
            val dx = next.x - playerGrid.x
            val dy = next.y - playerGrid.y
            facing = when {
                dx < 0 -> Facing.LEFT
                dx > 0 -> Facing.RIGHT
                dy < 0 -> Facing.UP
                else -> Facing.DOWN
            }
            val prevPlayerTile = playerGrid
            val compStartX = companionRenderX
            val compStartY = companionRenderY

            onPlayStepSfx()
            val steps = 8
            val stepMs = (1000f / (GameBalance.PLAYER_SPEED_TILES_PER_SEC * steps)).toLong().coerceAtLeast(16L)
            for (i in 1..steps) {
                val t = i.toFloat() / steps.toFloat()
                playerRenderX = startX + (next.x - startX) * t
                playerRenderY = startY + (next.y - startY) * t
                companionRenderX = compStartX + (prevPlayerTile.x - compStartX) * t
                companionRenderY = compStartY + (prevPlayerTile.y - compStartY) * t
                walkFrame = if (i % 4 < 2) 1 else 2
                delay(stepMs)
            }
            companionGrid = prevPlayerTile
            playerGrid = next
            playerRenderX = next.x.toFloat()
            playerRenderY = next.y.toFloat()
            companionRenderX = prevPlayerTile.x.toFloat()
            companionRenderY = prevPlayerTile.y.toFloat()
            pathQueue = pathQueue.drop(1)

            // Check portal step-in with anti-loop cooldown
            val steppedPortal = map.findPortalAt(next)
            if (steppedPortal != null && (System.currentTimeMillis() - enterTimeMs.longValue) >= GameBalance.PORTAL_COOLDOWN_MS) {
                pathQueue = emptyList()
                pendingPortal = null
                pendingNpc = null
                walkFrame = 0
                onPortalTrigger(steppedPortal.action)
                return@LaunchedEffect
            }
        }
        walkFrame = 0

        // Trigger arrived interaction if pending
        pendingNpc?.let { npc ->
            val dist = abs(playerGrid.x - npc.pos.x) + abs(playerGrid.y - npc.pos.y)
            pendingNpc = null
            if (dist <= 1) {
                onNpcInteract(npc)
            }
        }
        pendingPortal?.let { portal ->
            val dist = abs(playerGrid.x - portal.pos.x) + abs(playerGrid.y - portal.pos.y)
            pendingPortal = null
            if (dist <= 1 && (System.currentTimeMillis() - enterTimeMs.longValue) >= GameBalance.PORTAL_COOLDOWN_MS) {
                onPortalTrigger(portal.action)
            }
        }
    }

    fun stepManual(dx: Int, dy: Int) {
        if (isBlocked) return
        pendingNpc = null
        pendingPortal = null
        val target = GridPos(playerGrid.x + dx, playerGrid.y + dy)
        facing = when {
            dx < 0 -> Facing.LEFT
            dx > 0 -> Facing.RIGHT
            dy < 0 -> Facing.UP
            else -> Facing.DOWN
        }
        val npc = map.findNpcAt(target)
        if (npc != null) {
            onNpcInteract(npc)
            return
        }
        val portal = map.findPortalAt(target)
        if (portal != null) {
            if ((System.currentTimeMillis() - enterTimeMs.longValue) >= GameBalance.PORTAL_COOLDOWN_MS) {
                onPortalTrigger(portal.action)
            }
            return
        }
        if (map.isWalkable(target.x, target.y)) {
            pathQueue = listOf(target)
        }
    }

    val adjacentNpc = remember(playerGrid, map) {
        map.npcs.firstOrNull { abs(it.pos.x - playerGrid.x) + abs(it.pos.y - playerGrid.y) == 1 }
    }
    val adjacentPortal = remember(playerGrid, map) {
        map.portals.firstOrNull { abs(it.pos.x - playerGrid.x) + abs(it.pos.y - playerGrid.y) == 1 }
    }

    val profile = uiState.activeProfile
    val avatarSpriteId = "avatar_${profile?.avatarId ?: 0}"
    val companionId = profile?.equippedCompanion.orEmpty()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B))
            .testTag("hub_screen_$hubId")
    ) {
        val density = LocalDensity.current
        val canvasWidthPx = with(density) { maxWidth.toPx() }
        val canvasHeightPx = with(density) { maxHeight.toPx() }
        val isCompactWidth = maxWidth < 680.dp

        // Crisp integer pixel scale (3x on tablet, 2.5-3x on phone)
        val tileDrawPx = remember(maxWidth) {
            if (maxWidth >= 840.dp) 52f else if (maxWidth >= 600.dp) 46f else 42f
        }
        val mapPixelW = map.width * tileDrawPx
        val mapPixelH = map.height * tileDrawPx

        val camX = remember(playerRenderX, canvasWidthPx, mapPixelW) {
            if (mapPixelW <= canvasWidthPx) {
                -(canvasWidthPx - mapPixelW) / 2f
            } else {
                val desired = (playerRenderX + 0.5f) * tileDrawPx - canvasWidthPx / 2f
                desired.coerceIn(0f, mapPixelW - canvasWidthPx)
            }
        }
        val camY = remember(playerRenderY, canvasHeightPx, mapPixelH) {
            if (mapPixelH <= canvasHeightPx) {
                -(canvasHeightPx - mapPixelH) / 2f
            } else {
                val desired = (playerRenderY + 0.5f) * tileDrawPx - canvasHeightPx / 2f
                desired.coerceIn(0f, mapPixelH - canvasHeightPx)
            }
        }

        // 2D Pixel-Art World Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(hubId, isBlocked, camX, camY, tileDrawPx) {
                    detectTapGestures { offset ->
                        if (isBlocked) return@detectTapGestures
                        val worldX = offset.x + camX
                        val worldY = offset.y + camY
                        val tx = floor(worldX / tileDrawPx).toInt()
                        val ty = floor(worldY / tileDrawPx).toInt()
                        if (tx !in 0 until map.width || ty !in 0 until map.height) return@detectTapGestures

                        val tappedPos = GridPos(tx, ty)
                        // Check direct tap on NPC (with 1-tile forgiving halo for kids' fingers)
                        val tappedNpc = map.npcs.firstOrNull {
                            abs(it.pos.x - tx) <= 1 && abs(it.pos.y - ty) <= 1 &&
                                (it.pos == tappedPos || !map.isWalkable(tx, ty))
                        } ?: map.findNpcAt(tappedPos)

                        if (tappedNpc != null) {
                            val dist = abs(playerGrid.x - tappedNpc.pos.x) + abs(playerGrid.y - tappedNpc.pos.y)
                            if (dist <= 1) {
                                onNpcInteract(tappedNpc)
                            } else {
                                pendingNpc = tappedNpc
                                pendingPortal = null
                                pathQueue = SchoolMaps.findPath4Dir(map, playerGrid, tappedNpc.pos, allowPartialPath = true)
                            }
                            return@detectTapGestures
                        }

                        // Check direct tap on Portal/Door
                        val tappedPortal = map.findPortalAt(tappedPos)
                            ?: map.portals.firstOrNull { abs(it.pos.x - tx) == 0 && abs(it.pos.y - ty) <= 1 }
                        if (tappedPortal != null) {
                            val dist = abs(playerGrid.x - tappedPortal.pos.x) + abs(playerGrid.y - tappedPortal.pos.y)
                            if (dist <= 1) {
                                onPortalTrigger(tappedPortal.action)
                            } else {
                                pendingPortal = tappedPortal
                                pendingNpc = null
                                pathQueue = SchoolMaps.findPath4Dir(map, playerGrid, tappedPortal.pos, allowPartialPath = true)
                            }
                            return@detectTapGestures
                        }

                        // Walk to tapped tile via A*
                        pendingNpc = null
                        pendingPortal = null
                        pathQueue = SchoolMaps.findPath4Dir(map, playerGrid, tappedPos, allowPartialPath = true)
                    }
                }
        ) {
            val tileSizeInt = tileDrawPx.roundToInt()

            // Draw visible tiles
            val startCol = floor(camX / tileDrawPx).toInt().coerceIn(0, map.width - 1)
            val endCol = floor((camX + size.width) / tileDrawPx).toInt().coerceIn(0, map.width - 1)
            val startRow = floor(camY / tileDrawPx).toInt().coerceIn(0, map.height - 1)
            val endRow = floor((camY + size.height) / tileDrawPx).toInt().coerceIn(0, map.height - 1)

            val reducedMotion = uiState.config.studyPreferences.reducedMotion
            for (y in startRow..endRow) {
                for (x in startCol..endCol) {
                    val kind = map.tileAt(x, y)
                    val tileBmp = PixelArtAssets.tiles[kind] ?: continue
                    val dx = (x * tileDrawPx - camX).roundToInt()
                    val dy = (y * tileDrawPx - camY).roundToInt()
                    drawImage(
                        image = tileBmp,
                        dstOffset = IntOffset(dx, dy),
                        dstSize = IntSize(tileSizeInt + 1, tileSizeInt + 1),
                        filterQuality = FilterQuality.None
                    )
                    // Subtle animated sunlight shimmer on flowers and windows
                    if (!reducedMotion) {
                        val phase = (x * 7 + y * 13 + ambientTick) % 6
                        if (kind == com.example.pixelart.TileKind.GRASS_FLOWERS && phase == 0) {
                            drawCircle(
                                color = Color(0xBBFEF08A),
                                radius = tileDrawPx * 0.07f,
                                center = Offset(dx + tileDrawPx * 0.25f, dy + tileDrawPx * 0.28f)
                            )
                        } else if ((kind == com.example.pixelart.TileKind.WINDOW_EXT || kind == com.example.pixelart.TileKind.WINDOW_INT) && phase <= 1) {
                            drawRect(
                                color = Color(0x44E0F2FE),
                                topLeft = Offset(dx + tileDrawPx * 0.22f, dy + tileDrawPx * 0.22f),
                                size = androidx.compose.ui.geometry.Size(tileDrawPx * 0.20f, tileDrawPx * 0.16f)
                            )
                        }
                    }
                }
            }

            // Highlight destination tile with glowing retro ring while walking
            pathQueue.lastOrNull()?.let { dest ->
                val dx = dest.x * tileDrawPx - camX
                val dy = dest.y * tileDrawPx - camY
                val center = Offset(dx + tileDrawPx / 2f, dy + tileDrawPx / 2f)
                drawCircle(color = Color(0x4438BDF8), radius = tileDrawPx * 0.34f, center = center)
                drawCircle(color = Color(0xBB0F4C81), radius = tileDrawPx * 0.16f, center = center)
            }

            // Draw Companion Follower with soft elliptical ground shadow
            if (companionId.isNotBlank()) {
                val petBmp = PixelArtAssets.getCompanionSprite(companionId, ambientTick % 2)
                val px = companionRenderX * tileDrawPx - camX
                val py = companionRenderY * tileDrawPx - camY
                drawOval(
                    color = Color(0x44000000),
                    topLeft = Offset(px + tileDrawPx * 0.18f, py + tileDrawPx * 0.78f),
                    size = androidx.compose.ui.geometry.Size(tileDrawPx * 0.64f, tileDrawPx * 0.20f)
                )
                drawImage(
                    image = petBmp,
                    dstOffset = IntOffset(px.roundToInt(), py.roundToInt()),
                    dstSize = IntSize(tileSizeInt, tileSizeInt),
                    filterQuality = FilterQuality.None
                )
            }

            // Draw NPCs with soft shadow & gentle idle bob
            for (npc in map.npcs) {
                val bmp = PixelArtAssets.getCharacterSprite(npc.spriteId, Facing.DOWN, 0)
                val nx = npc.pos.x * tileDrawPx - camX
                val ny = npc.pos.y * tileDrawPx - camY
                val idleBob = if (!reducedMotion && (ambientTick + npc.pos.x) % 4 == 0) -2 else 0
                drawOval(
                    color = Color(0x4A000000),
                    topLeft = Offset(nx + tileDrawPx * 0.14f, ny + tileDrawPx * 0.80f),
                    size = androidx.compose.ui.geometry.Size(tileDrawPx * 0.72f, tileDrawPx * 0.20f)
                )
                drawImage(
                    image = bmp,
                    dstOffset = IntOffset(nx.roundToInt(), ny.roundToInt() + idleBob),
                    dstSize = IntSize(tileSizeInt, tileSizeInt),
                    filterQuality = FilterQuality.None
                )
            }

            // Draw Player Character with soft shadow
            val playerBmp = PixelArtAssets.getCharacterSprite(avatarSpriteId, facing, walkFrame)
            val px = playerRenderX * tileDrawPx - camX
            val py = playerRenderY * tileDrawPx - camY
            drawOval(
                color = Color(0x52000000),
                topLeft = Offset(px + tileDrawPx * 0.14f, py + tileDrawPx * 0.80f),
                size = androidx.compose.ui.geometry.Size(tileDrawPx * 0.72f, tileDrawPx * 0.20f)
            )
            drawImage(
                image = playerBmp,
                dstOffset = IntOffset(px.roundToInt(), py.roundToInt()),
                dstSize = IntSize(tileSizeInt, tileSizeInt),
                filterQuality = FilterQuality.None
            )

            // Draw 16-bit colorful overhead signs + priority "!" badges
            drawIntoCanvas { canvas ->
                val bgPaint = Paint().apply { isAntiAlias = true }
                val borderPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    color = android.graphics.Color.rgb(250, 248, 242)
                    isAntiAlias = true
                }
                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = tileDrawPx * 0.29f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val badgeBg = Paint().apply {
                    color = android.graphics.Color.rgb(245, 158, 11)
                    isAntiAlias = true
                }
                val badgeBorder = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                }
                val badgeText = Paint().apply {
                    color = android.graphics.Color.rgb(69, 26, 3)
                    textSize = tileDrawPx * 0.34f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                fun drawTag(cx: Float, topY: Float, label: String, hasPriorityBadge: Boolean, accentRgb: Int) {
                    val tw = textPaint.measureText(label)
                    val padH = 11f
                    val pillH = tileDrawPx * 0.45f
                    val rectLeft = cx - tw / 2f - padH
                    val rectRight = cx + tw / 2f + padH
                    val rectTop = topY - pillH
                    bgPaint.color = accentRgb
                    canvas.nativeCanvas.drawRoundRect(
                        rectLeft, rectTop, rectRight, topY,
                        10f, 10f, bgPaint
                    )
                    canvas.nativeCanvas.drawRoundRect(
                        rectLeft, rectTop, rectRight, topY,
                        10f, 10f, borderPaint
                    )
                    canvas.nativeCanvas.drawText(
                        label,
                        cx,
                        topY - pillH * 0.27f,
                        textPaint
                    )
                    if (hasPriorityBadge) {
                        val bounce = if (reducedMotion) 0f else if (ambientTick % 2 == 0) -3.5f else 0f
                        val bx = rectRight + 7f
                        val by = rectTop + pillH * 0.5f + bounce
                        val r = tileDrawPx * 0.24f
                        canvas.nativeCanvas.drawCircle(bx, by, r, badgeBg)
                        canvas.nativeCanvas.drawCircle(bx, by, r, badgeBorder)
                        canvas.nativeCanvas.drawText("!", bx, by + tileDrawPx * 0.12f, badgeText)
                    }
                }

                for (npc in map.npcs) {
                    val cx = (npc.pos.x + 0.5f) * tileDrawPx - camX
                    val topY = npc.pos.y * tileDrawPx - camY - 5f
                    val hasPrio = when (npc.subjectFilter) {
                        "storia" -> storiaPriority
                        "scienze" -> scienzePriority
                        "preside" -> presidePriority
                        else -> false
                    }
                    val signColor = when (npc.subjectFilter) {
                        "storia" -> android.graphics.Color.argb(235, 180, 83, 9)
                        "scienze" -> android.graphics.Color.argb(235, 15, 118, 110)
                        "preside" -> android.graphics.Color.argb(235, 109, 40, 217)
                        else -> android.graphics.Color.argb(230, 30, 41, 59)
                    }
                    drawTag(cx, topY, npc.label, hasPrio, signColor)
                }

                for (portal in map.portals) {
                    val cx = (portal.pos.x + 0.5f) * tileDrawPx - camX
                    val topY = portal.pos.y * tileDrawPx - camY - 5f
                    val hasPrio = when (portal.id) {
                        "portal_school_entrance" -> mathPriority || scienzePriority || italianoPriority || geografiaPriority || inglesePriority
                        "portal_gym" -> mathPriority
                        "portal_lab" -> scienzePriority
                        "portal_library" -> italianoPriority
                        "portal_geo" -> geografiaPriority
                        "portal_english" -> inglesePriority
                        else -> false
                    }
                    val signColor = when (portal.id) {
                        "portal_gym" -> android.graphics.Color.argb(238, 185, 28, 28)
                        "portal_lab" -> android.graphics.Color.argb(238, 13, 148, 136)
                        "portal_library" -> android.graphics.Color.argb(238, 124, 58, 237)
                        "portal_geo" -> android.graphics.Color.argb(238, 2, 132, 199)
                        "portal_english" -> android.graphics.Color.argb(238, 37, 99, 235)
                        "portal_locker" -> android.graphics.Color.argb(238, 217, 119, 6)
                        "portal_school_entrance", "portal_to_yard" -> android.graphics.Color.argb(235, 15, 76, 129)
                        else -> android.graphics.Color.argb(225, 71, 85, 105)
                    }
                    drawTag(cx, topY, portal.label, hasPrio, signColor)
                }
            }
        }

        // Top HUD Overlay with Safe Area Insets
        HubHudOverlay(
            uiState = uiState,
            isCompactWidth = isCompactWidth,
            hubTitle = if (hubId == "school_hallway") "Corridoio" else "Cortile",
            onGuidanceClick = onGuidanceClick,
            onToggleMute = onToggleMute,
            onOpenParentGate = onOpenParentGate,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = GameBalance.SAFE_MARGIN_X_DP.dp, vertical = GameBalance.SAFE_MARGIN_Y_DP.dp)
        )

        // Bottom Controls: Optional Virtual D-Pad + Context Action Button
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = GameBalance.SAFE_MARGIN_X_DP.dp, vertical = GameBalance.SAFE_MARGIN_Y_DP.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            if (uiState.config.studyPreferences.showVirtualDpad) {
                VirtualDpad(onStep = { dx, dy -> stepManual(dx, dy) })
            } else {
                Spacer(Modifier.size(1.dp))
            }

            // Quick contextual Interact Button when next to an NPC or Portal
            if (adjacentNpc != null || adjacentPortal != null) {
                val label = adjacentNpc?.let { "Parla con ${it.name}" }
                    ?: adjacentPortal?.let { "Entra: ${it.label}" }
                    ?: ""
                Button(
                    onClick = {
                        if (adjacentNpc != null) onNpcInteract(adjacentNpc)
                        else if (adjacentPortal != null) onPortalTrigger(adjacentPortal.action)
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("hub_interact_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    border = BorderStroke(2.5.dp, Color.White)
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun HubHudOverlay(
    uiState: AppUiState,
    isCompactWidth: Boolean,
    hubTitle: String,
    onGuidanceClick: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenParentGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = uiState.activeProfile
    val frameColor = GameBalance.frameColor(profile?.equippedFrame ?: "frame_classic")
    val stampsCount = uiState.sessionsThisWeek.coerceIn(0, GameBalance.WEEKLY_GOAL_SESSIONS)

    Column(
        modifier = modifier.testTag("hub_hud"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Top-Left: Profile Badge + 3 Weekly Star Stamps
            Surface(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 6.dp),
                shape = RoundedCornerShape(14.dp),
                color = CreamBackground.copy(alpha = 0.96f),
                border = BorderStroke(3.dp, frameColor),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (isCompactWidth) 8.dp else 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 6.dp else 8.dp)
                ) {
                    PixelSprite(
                        bitmap = PixelArtAssets.getCharacterSprite("avatar_${profile?.avatarId ?: 0}", Facing.DOWN, 0),
                        contentDescription = profile?.name ?: "Profilo",
                        modifier = Modifier.size(if (isCompactWidth) 32.dp else 38.dp)
                    )
                    Column {
                        Text(
                            text = "${profile?.name ?: "Studente"} · $hubTitle",
                            style = MaterialTheme.typography.labelLarge,
                            color = InkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            repeat(GameBalance.WEEKLY_GOAL_SESSIONS) { idx ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Timbro settimanale ${idx + 1}",
                                    tint = if (idx < stampsCount) SoftGold else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (!isCompactWidth) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$stampsCount/${GameBalance.WEEKLY_GOAL_SESSIONS} sett.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Center Guidance Pill on wide screens
            if (!isCompactWidth && uiState.config.studyPreferences.showGuidancePill && uiState.recommendedActivity != null) {
                GuidancePill(
                    title = uiState.recommendedActivity.title,
                    isBoss = uiState.recommendedActivity.isBoss,
                    onClick = onGuidanceClick,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 10.dp)
                )
            }

            // Top-Right: Stars, Medals, Mute, 2s Hold Parent Area Gear
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HudStatChip(
                    icon = Icons.Default.Star,
                    iconTint = SoftGold,
                    value = "${profile?.stars ?: 0}",
                    contentDesc = "Stelle"
                )
                HudStatChip(
                    icon = Icons.Default.EmojiEvents,
                    iconTint = WarmAmber,
                    value = "${profile?.medals?.size ?: 0}",
                    contentDesc = "Medaglie"
                )
                Surface(
                    shape = CircleShape,
                    color = CreamBackground.copy(alpha = 0.95f),
                    border = BorderStroke(2.dp, GameBalance.GBA_FRAME_DARK)
                ) {
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hud_mute_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.config.audio.masterMuted) {
                                Icons.AutoMirrored.Filled.VolumeOff
                            } else {
                                Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = if (uiState.config.audio.masterMuted) "Attiva audio" else "Silenzia audio",
                            tint = InkPrimary
                        )
                    }
                }
                ParentHoldGearButton(onUnlocked = onOpenParentGate)
            }
        }

        // Compact phone guidance pill on second row so nothing clips
        if (isCompactWidth && uiState.config.studyPreferences.showGuidancePill && uiState.recommendedActivity != null) {
            GuidancePill(
                title = uiState.recommendedActivity.title,
                isBoss = uiState.recommendedActivity.isBoss,
                onClick = onGuidanceClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun GuidancePill(
    title: String,
    isBoss: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("guidance_pill"),
        shape = RoundedCornerShape(22.dp),
        color = if (isBoss) Color(0xFFFEF3C7) else Color(0xFFDBEAFE),
        border = BorderStroke(2.dp, if (isBoss) WarmAmber else DeepBlueAccent),
        shadowElevation = 3.dp
    ) {
        Text(
            text = if (isBoss) "Sfida Boss consigliata: $title" else "Sfida consigliata: $title",
            style = MaterialTheme.typography.labelLarge,
            color = if (isBoss) Color(0xFF78350F) else DeepBlueAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun HudStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    contentDesc: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CreamBackground.copy(alpha = 0.95f),
        border = BorderStroke(2.dp, GameBalance.GBA_FRAME_DARK)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = contentDesc, tint = iconTint, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, color = InkPrimary)
        }
    }
}

/**
 * Parent Area Gear button with progressive 0% -> 100% fill over 2.0s hold.
 * Cancels only if pointer leaves button bounds or is released early.
 */
@Composable
private fun ParentHoldGearButton(onUnlocked: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val latestOnUnlocked by rememberUpdatedState(onUnlocked)

    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(CreamBackground.copy(alpha = 0.96f))
            .border(2.dp, GameBalance.GBA_FRAME_DARK, CircleShape)
            .testTag("parent_gear_button")
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val holdJob: Job = scope.launch {
                        val startMs = System.currentTimeMillis()
                        while (true) {
                            val elapsed = System.currentTimeMillis() - startMs
                            progress = (elapsed.toFloat() / GameBalance.PARENT_HOLD_DURATION_MS).coerceIn(0f, 1f)
                            if (elapsed >= GameBalance.PARENT_HOLD_DURATION_MS) {
                                progress = 0f
                                latestOnUnlocked()
                                break
                            }
                            delay(25)
                        }
                    }
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            val inside = change.position.x in 0f..size.width.toFloat() &&
                                change.position.y in 0f..size.height.toFloat()
                            if (!change.pressed || !inside) {
                                holdJob.cancel()
                                progress = 0f
                                break
                            }
                        }
                    } finally {
                        holdJob.cancel()
                        progress = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (progress > 0f) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(46.dp),
                color = DeepBlueAccent,
                strokeWidth = 4.dp,
                trackColor = Color(0xFFE2E8F0)
            )
        }
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Tieni premuto 2 secondi per Area Genitori",
            tint = InkPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}
