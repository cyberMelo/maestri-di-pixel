package io.github.cybermelo.maestridipixel.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import io.github.cybermelo.maestridipixel.R
import io.github.cybermelo.maestridipixel.core.GameBalance
import io.github.cybermelo.maestridipixel.pixelart.Facing
import io.github.cybermelo.maestridipixel.pixelart.GridPos
import io.github.cybermelo.maestridipixel.pixelart.HubInteractiveProp
import io.github.cybermelo.maestridipixel.pixelart.HubNpc
import io.github.cybermelo.maestridipixel.pixelart.HubPortal
import io.github.cybermelo.maestridipixel.pixelart.PixelArtAssets
import io.github.cybermelo.maestridipixel.pixelart.SchoolMapData
import io.github.cybermelo.maestridipixel.pixelart.SchoolMaps
import io.github.cybermelo.maestridipixel.ui.components.PixelSprite
import io.github.cybermelo.maestridipixel.ui.components.VirtualDpad
import io.github.cybermelo.maestridipixel.ui.theme.CreamBackground
import io.github.cybermelo.maestridipixel.ui.theme.DarkBorderSlate
import io.github.cybermelo.maestridipixel.ui.theme.DarkCardBg
import io.github.cybermelo.maestridipixel.ui.theme.DeepBlueAccent
import io.github.cybermelo.maestridipixel.ui.theme.EmeraldSuccess
import io.github.cybermelo.maestridipixel.ui.theme.HudNightBg
import io.github.cybermelo.maestridipixel.ui.theme.IceWhiteText
import io.github.cybermelo.maestridipixel.ui.theme.InkPrimary
import io.github.cybermelo.maestridipixel.ui.theme.LocalDarkTheme
import io.github.cybermelo.maestridipixel.ui.theme.SkyHighlight
import io.github.cybermelo.maestridipixel.ui.theme.SoftGold
import io.github.cybermelo.maestridipixel.ui.theme.StarYellow
import io.github.cybermelo.maestridipixel.ui.theme.WarmAmber
import io.github.cybermelo.maestridipixel.viewmodel.ActiveModal
import io.github.cybermelo.maestridipixel.viewmodel.AppUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private data class StepDustPuff(
    val tileX: Float,
    val tileY: Float,
    val bornMs: Long
)

@Composable
fun HubScreen(
    hubId: String,
    spawnId: String,
    uiState: AppUiState,
    onNpcInteract: (HubNpc) -> Unit,
    onPortalTrigger: (io.github.cybermelo.maestridipixel.pixelart.PortalAction) -> Unit,
    onGuidanceClick: () -> Unit,
    onOpenQuickMap: () -> Unit = {},
    onOpenDailyMissions: () -> Unit = {},
    onOpenTrophyHall: () -> Unit = {},
    onPetTap: () -> Unit = {},
    onToggleMute: () -> Unit,
    onOpenParentGate: () -> Unit,
    onPlayStepSfx: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val isDark = LocalDarkTheme.current
    val readablePlaqueTypeface = remember(context) {
        val base = try {
            ResourcesCompat.getFont(context, R.font.atkinson_hyperlegible)
        } catch (_: Exception) {
            null
        } ?: Typeface.SANS_SERIF
        Typeface.create(base, Typeface.BOLD)
    }

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
    var pendingProp by remember(hubId, spawnId) { mutableStateOf<HubInteractiveProp?>(null) }
    val enterTimeMs = remember(hubId, spawnId) { mutableLongStateOf(System.currentTimeMillis()) }

    // Living world animation state (dust puffs, ambient butterflies/sunbeams, pet love hearts)
    var dustPuffs by remember(hubId, spawnId) { mutableStateOf<List<StepDustPuff>>(emptyList()) }
    var petHappyUntilMs by remember(hubId, spawnId) { mutableLongStateOf(0L) }
    var animClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var ambientTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.config.studyPreferences.reducedMotion) {
        val reduced = uiState.config.studyPreferences.reducedMotion
        val stepMs = if (reduced) 450L else 60L
        var sub = 0
        while (true) {
            delay(stepMs)
            val now = System.currentTimeMillis()
            animClockMs = now
            sub++
            if (reduced || sub % 7 == 0) {
                ambientTick = (ambientTick + 1) % 4
            }
            if (dustPuffs.isNotEmpty()) {
                dustPuffs = dustPuffs.filter { now - it.bornMs < 440L }
            }
        }
    }

    // Priority & claimable missions evaluation
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
    val claimableMissionsCount = remember(uiState.dailyMissions) {
        uiState.dailyMissions.count { it.isClaimable }
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
            if (!uiState.config.studyPreferences.reducedMotion) {
                val now = System.currentTimeMillis()
                dustPuffs = (dustPuffs + StepDustPuff(prevPlayerTile.x + 0.5f, prevPlayerTile.y + 0.82f, now)).takeLast(8)
            }
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
                pendingProp = null
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
        pendingProp?.let { prop ->
            val dist = abs(playerGrid.x - prop.pos.x) + abs(playerGrid.y - prop.pos.y)
            pendingProp = null
            if (dist <= 2) {
                onPortalTrigger(prop.action)
            }
        }
    }

    fun stepManual(dx: Int, dy: Int) {
        if (isBlocked) return
        pendingNpc = null
        pendingPortal = null
        pendingProp = null
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
        val prop = map.findPropAt(target)
        if (prop != null) {
            onPortalTrigger(prop.action)
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
    val adjacentProp = remember(playerGrid, map) {
        map.props.firstOrNull { abs(it.pos.x - playerGrid.x) + abs(it.pos.y - playerGrid.y) <= 2 }
    }

    val profile = uiState.activeProfile
    val avatarSpriteId = "avatar_${profile?.avatarId ?: 0}"
    val equippedHatId = profile?.equippedHat.orEmpty()
    val companionId = profile?.equippedCompanion.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) HudNightBg else CreamBackground)
            .testTag("hub_screen_$hubId")
    ) {
        // Dedicated thin separated Top HUD Bar — never overlaps school roof, windows, or door plaques
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) HudNightBg else CreamBackground)
                .statusBarsPadding()
                .displayCutoutPadding()
        ) {
            val isCompactHeader = maxWidth < 680.dp
            Column(modifier = Modifier.fillMaxWidth()) {
                HubHudOverlay(
                    uiState = uiState,
                    isCompactWidth = isCompactHeader,
                    hubTitle = if (hubId == "school_hallway") "Atrio Scuola" else "Cortile",
                    claimableMissionsCount = claimableMissionsCount,
                    onGuidanceClick = onGuidanceClick,
                    onOpenQuickMap = onOpenQuickMap,
                    onOpenDailyMissions = onOpenDailyMissions,
                    onOpenTrophyHall = onOpenTrophyHall,
                    onToggleMute = onToggleMute,
                    onOpenParentGate = onOpenParentGate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
                HorizontalDivider(
                    thickness = 1.5.dp,
                    color = if (isDark) DarkBorderSlate else Color(0xFFCBD5E1)
                )
            }
        }

        // Edge-to-Edge Resolution-Adaptive 16-bit Map Viewport
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // Il Canvas disegna una riga di margine fuori vista: senza ritaglio coprirebbe la barra HUD
                .clipToBounds()
                .background(if (isDark) HudNightBg else Color(0xFF1E293B))
        ) {
            val density = LocalDensity.current
            val canvasWidthPx = with(density) { maxWidth.toPx() }
            val canvasHeightPx = with(density) { maxHeight.toPx() }
            val isTablet = maxWidth >= 680.dp

            // Ingrandimento intero dei disegni a 32 px (x2, x3, x4): i pixel restano tutti uguali e nitidi.
            // La piastrella mira a ~52 dp; lo sfondo dipinto ha un bordo extra, quindi non servono bande vuote.
            val tileDrawPx = remember(density) {
                val targetPx = with(density) { 52.dp.toPx() }
                val scale = (targetPx / PixelArtAssets.TILE_PX).roundToInt().coerceIn(2, 4)
                (PixelArtAssets.TILE_PX * scale).toFloat()
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

                        // Interactive Pet tap: happy hop & floating hearts!
                        if (companionId.isNotBlank() && tappedPos == companionGrid) {
                            petHappyUntilMs = System.currentTimeMillis() + 1200L
                            onPetTap()
                            return@detectTapGestures
                        }

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
                                pendingProp = null
                                pathQueue = SchoolMaps.findPath4Dir(map, playerGrid, tappedNpc.pos, allowPartialPath = true)
                            }
                            return@detectTapGestures
                        }

                        // Check direct tap on Interactive Prop (Trophy Case / Notice Boards)
                        val tappedProp = map.props.firstOrNull {
                            abs(it.pos.x - tx) <= 1 && abs(it.pos.y - ty) <= 1
                        }
                        if (tappedProp != null) {
                            val dist = abs(playerGrid.x - tappedProp.pos.x) + abs(playerGrid.y - tappedProp.pos.y)
                            if (dist <= 2) {
                                onPortalTrigger(tappedProp.action)
                            } else {
                                pendingProp = tappedProp
                                pendingNpc = null
                                pendingPortal = null
                                pathQueue = SchoolMaps.findPath4Dir(map, playerGrid, tappedProp.pos, allowPartialPath = true)
                            }
                            return@detectTapGestures
                        }

                        // Check direct tap on Portal/Door or its overhead classroom plaque
                        val tappedPortal = map.findPortalAt(tappedPos)
                            ?: map.portals.firstOrNull {
                                (abs(it.pos.x - tx) == 0 && abs(it.pos.y - ty) <= 1) ||
                                    (abs(it.pos.x - tx) <= 1 && ty == it.pos.y - 1)
                            }
                        if (tappedPortal != null) {
                            val dist = abs(playerGrid.x - tappedPortal.pos.x) + abs(playerGrid.y - tappedPortal.pos.y)
                            if (dist <= 1) {
                                onPortalTrigger(tappedPortal.action)
                            } else {
                                pendingPortal = tappedPortal
                                pendingNpc = null
                                pendingProp = null
                                pathQueue = SchoolMaps.findPath4Dir(map, playerGrid, tappedPortal.pos, allowPartialPath = true)
                            }
                            return@detectTapGestures
                        }

                        // Walk to tapped tile via A*
                        pendingNpc = null
                        pendingPortal = null
                        pendingProp = null
                        pathQueue = SchoolMaps.findPath4Dir(map, playerGrid, tappedPos, allowPartialPath = true)
                    }
                }
        ) {
            val tileSizeInt = tileDrawPx.roundToInt()
            val reducedMotion = uiState.config.studyPreferences.reducedMotion
            val nowMs = animClockMs
            val timeSec = (nowMs % 60_000L) / 1000f

            // Sfondo dipinto (tools/art/build_maps.py): la griglia logica parte dopo il bordo extra
            PixelArtAssets.getMapBackground(hubId)?.let { bg ->
                val scale = tileDrawPx / PixelArtAssets.TILE_PX
                val margin = (bg.width / PixelArtAssets.TILE_PX - map.width) / 2
                drawImage(
                    image = bg,
                    dstOffset = IntOffset((-margin * tileDrawPx - camX).roundToInt(), (-margin * tileDrawPx - camY).roundToInt()),
                    dstSize = IntSize((bg.width * scale).roundToInt(), (bg.height * scale).roundToInt()),
                    filterQuality = FilterQuality.None
                )
            }

            // Personaggi LPC: fotogramma quadrato più grande di una piastrella, piedi sul fondo della cella
            val characterPx = tileDrawPx * PixelArtAssets.CHARACTER_PX / PixelArtAssets.TILE_PX
            fun characterOffset(x: Float, y: Float, bob: Int = 0) = IntOffset(
                (x + 0.5f - characterPx / tileDrawPx / 2f).let { it * tileDrawPx - camX }.roundToInt(),
                ((y + 1f) * tileDrawPx - characterPx - camY).roundToInt() + bob
            )
            val characterSize = IntSize(characterPx.roundToInt(), characterPx.roundToInt())

            // Door proximity welcome glow when player approaches any door
            for (portal in map.portals) {
                val dist = abs(playerGrid.x - portal.pos.x) + abs(playerGrid.y - portal.pos.y)
                if (dist <= 2) {
                    val pulse = if (reducedMotion) 0.32f else 0.22f + 0.16f * sin(timeSec * 5f)
                    val gx = portal.pos.x * tileDrawPx - camX
                    val gy = portal.pos.y * tileDrawPx - camY
                    drawRect(
                        color = Color(0xFFFDE047).copy(alpha = pulse),
                        topLeft = Offset(gx - 3f, gy - 3f),
                        size = androidx.compose.ui.geometry.Size(tileDrawPx + 6f, tileDrawPx + 6f)
                    )
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

            // Walking dust puffs behind player's shoes
            if (!reducedMotion) {
                for (puff in dustPuffs) {
                    val age = ((nowMs - puff.bornMs).toFloat() / 440f).coerceIn(0f, 1f)
                    val px = puff.tileX * tileDrawPx - camX
                    val py = puff.tileY * tileDrawPx - camY - age * 8f
                    drawCircle(
                        color = Color(0xFFF5F0E6).copy(alpha = (1f - age) * 0.65f),
                        radius = tileDrawPx * (0.08f + age * 0.10f),
                        center = Offset(px, py)
                    )
                }
            }

            // Draw Companion Follower with soft shadow, happy hop & floating love hearts on tap
            if (companionId.isNotBlank()) {
                val petBmp = PixelArtAssets.getCompanionSprite(companionId, ambientTick % 2)
                val px = companionRenderX * tileDrawPx - camX
                val py = companionRenderY * tileDrawPx - camY
                val happyRemaining = (petHappyUntilMs - nowMs).coerceAtLeast(0L)
                val happyHop = if (!reducedMotion && happyRemaining > 0L) {
                    (-abs(sin(happyRemaining / 120f)) * tileDrawPx * 0.28f).roundToInt()
                } else {
                    0
                }
                drawOval(
                    color = Color(0x44000000),
                    topLeft = Offset(px + tileDrawPx * 0.18f, py + tileDrawPx * 0.78f),
                    size = androidx.compose.ui.geometry.Size(tileDrawPx * 0.64f, tileDrawPx * 0.20f)
                )
                drawImage(
                    image = petBmp,
                    dstOffset = IntOffset(px.roundToInt(), py.roundToInt() + happyHop),
                    dstSize = IntSize(tileSizeInt, tileSizeInt),
                    filterQuality = FilterQuality.None
                )
                if (happyRemaining > 0L) {
                    val heartProg = 1f - (happyRemaining / 1200f)
                    val hx = px + tileDrawPx * 0.5f + sin(heartProg * 8f) * 6f
                    val hy = py - tileDrawPx * (0.12f + heartProg * 0.55f)
                    val hAlpha = (1f - heartProg).coerceIn(0f, 1f)
                    val hs = tileDrawPx * 0.12f
                    // Cute pixel heart
                    drawRect(Color(0xFFEC4899).copy(alpha = hAlpha), Offset(hx - hs, hy), androidx.compose.ui.geometry.Size(hs, hs))
                    drawRect(Color(0xFFEC4899).copy(alpha = hAlpha), Offset(hx + hs * 0.1f, hy), androidx.compose.ui.geometry.Size(hs, hs))
                    drawRect(Color(0xFFEC4899).copy(alpha = hAlpha), Offset(hx - hs * 0.5f, hy + hs * 0.8f), androidx.compose.ui.geometry.Size(hs * 1.1f, hs))
                }
            }

            // Draw NPCs with soft shadow & gentle idle bob
            for (npc in map.npcs) {
                val bmp = PixelArtAssets.getCharacterSprite(npc.spriteId, Facing.DOWN, 0)
                val nx = npc.pos.x * tileDrawPx - camX
                val ny = npc.pos.y * tileDrawPx - camY
                val idleBob = if (!reducedMotion && (ambientTick + npc.pos.x) % 4 == 0) -2 else 0
                drawOval(
                    color = Color(0x4A000000),
                    topLeft = Offset(nx + tileDrawPx * 0.20f, ny + tileDrawPx * 0.82f),
                    size = androidx.compose.ui.geometry.Size(tileDrawPx * 0.60f, tileDrawPx * 0.20f)
                )
                drawImage(
                    image = bmp,
                    dstOffset = characterOffset(npc.pos.x.toFloat(), npc.pos.y.toFloat(), idleBob),
                    dstSize = characterSize,
                    filterQuality = FilterQuality.None
                )
            }

            // Draw Player Character (wearing equipped pixel hat!) with soft shadow
            val playerBmp = PixelArtAssets.getCharacterSprite(avatarSpriteId, facing, walkFrame, equippedHatId)
            val px = playerRenderX * tileDrawPx - camX
            val py = playerRenderY * tileDrawPx - camY
            drawOval(
                color = Color(0x52000000),
                topLeft = Offset(px + tileDrawPx * 0.20f, py + tileDrawPx * 0.82f),
                size = androidx.compose.ui.geometry.Size(tileDrawPx * 0.60f, tileDrawPx * 0.20f)
            )
            drawImage(
                image = playerBmp,
                dstOffset = characterOffset(playerRenderX, playerRenderY),
                dstSize = characterSize,
                filterQuality = FilterQuality.None
            )

            // Ambient living creatures: colorful pixel butterflies in the Yard, sunlit golden motes in the Atrium
            if (!reducedMotion) {
                if (hubId == "school_yard") {
                    val bColors = listOf(Color(0xFFFDE047), Color(0xFFF472B6), Color(0xFF38BDF8), Color(0xFFFB923C))
                    for (i in 0..3) {
                        val baseX = (4f + i * 5.2f) * tileDrawPx - camX
                        val baseY = (7.5f + (i % 2) * 5.5f) * tileDrawPx - camY
                        val bx = baseX + sin(timeSec * (0.9f + i * 0.2f) + i * 1.7f) * tileDrawPx * 1.6f
                        val by = baseY + cos(timeSec * (1.3f + i * 0.15f) + i) * tileDrawPx * 0.85f
                        val wingSpan = if ((nowMs / 160L + i) % 2 == 0L) 5f else 2.5f
                        drawRect(bColors[i], Offset(bx - wingSpan, by - 2f), androidx.compose.ui.geometry.Size(wingSpan, 4f))
                        drawRect(bColors[i], Offset(bx + 1f, by - 2f), androidx.compose.ui.geometry.Size(wingSpan, 4f))
                    }
                } else {
                    for (i in 0..5) {
                        val mx = ((3.5f + i * 3.8f) * tileDrawPx - camX) + sin(timeSec * 0.7f + i * 1.3f) * 18f
                        val my = ((4.2f + (i % 3) * 3.4f) * tileDrawPx - camY) + cos(timeSec * 0.9f + i) * 14f
                        val mAlpha = 0.22f + 0.22f * sin(timeSec * 2.2f + i)
                        drawRect(
                            color = Color(0xFFFEF08A).copy(alpha = mAlpha),
                            topLeft = Offset(mx, my),
                            size = androidx.compose.ui.geometry.Size(4f, 4f)
                        )
                    }
                }
            }

            // Draw 16-bit colorful overhead signs, mastery stars + priority "!" badges
            drawIntoCanvas { canvas ->
                val bgPaint = Paint().apply { isAntiAlias = true }
                val borderPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    color = android.graphics.Color.rgb(250, 248, 242)
                    isAntiAlias = true
                }
                val textPaint = Paint().apply {
                    color = android.graphics.Color.rgb(241, 245, 249)
                    textSize = tileDrawPx * 0.27f
                    typeface = readablePlaqueTypeface
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
                    typeface = readablePlaqueTypeface
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                val shadowPaint = Paint().apply {
                    color = android.graphics.Color.argb(110, 15, 23, 42)
                    isAntiAlias = true
                }
                val subBgPaint = Paint().apply {
                    color = if (isDark) android.graphics.Color.rgb(30, 41, 59) else android.graphics.Color.rgb(254, 249, 238)
                    isAntiAlias = true
                }
                val subTextPaint = Paint().apply {
                    color = if (isDark) android.graphics.Color.rgb(241, 245, 249) else android.graphics.Color.rgb(30, 41, 59)
                    textSize = tileDrawPx * 0.23f
                    typeface = readablePlaqueTypeface
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val bracketPaint = Paint().apply {
                    color = android.graphics.Color.rgb(51, 65, 85)
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

                fun drawDoorPlaque(
                    cx: Float,
                    doorTopY: Float,
                    label: String,
                    subtitle: String,
                    hasPriorityBadge: Boolean,
                    accentRgb: Int,
                    masteryStars: Int? = null
                ) {
                    val cleanSub = if (masteryStars != null) {
                        val filled = "★".repeat(masteryStars.coerceIn(0, 3))
                        val empty = "☆".repeat((3 - masteryStars).coerceIn(0, 3))
                        "${subtitle.uppercase()} $filled$empty"
                    } else {
                        subtitle.uppercase()
                    }
                    val titleW = textPaint.measureText(label)
                    val subW = if (cleanSub.isNotEmpty()) subTextPaint.measureText(cleanSub) else 0f
                    val padH = 12f
                    val plaqueW = maxOf(titleW, subW, tileDrawPx * 1.6f) + padH * 2f
                    val titleH = tileDrawPx * 0.42f
                    val subH = if (cleanSub.isNotEmpty()) tileDrawPx * 0.34f else 0f
                    val totalH = titleH + subH
                    val bottomY = doorTopY - 4f
                    val topY = bottomY - totalH
                    val left = cx - plaqueW / 2f
                    val right = cx + plaqueW / 2f

                    // Small wall-mount pins connecting plaque to door lintel
                    canvas.nativeCanvas.drawRect(cx - 10f, bottomY - 1f, cx - 6f, doorTopY + 2f, bracketPaint)
                    canvas.nativeCanvas.drawRect(cx + 6f, bottomY - 1f, cx + 10f, doorTopY + 2f, bracketPaint)

                    // Drop shadow
                    canvas.nativeCanvas.drawRoundRect(
                        left + 2.5f, topY + 3f, right + 2.5f, bottomY + 3f,
                        10f, 10f, shadowPaint
                    )

                    // Cream lower body (full plaque background)
                    canvas.nativeCanvas.drawRoundRect(
                        left, topY, right, bottomY,
                        10f, 10f, subBgPaint
                    )

                    // Colored top header band for room name
                    bgPaint.color = accentRgb
                    canvas.nativeCanvas.drawRoundRect(
                        left, topY, right, topY + titleH + if (cleanSub.isEmpty()) 0f else 4f,
                        10f, 10f, bgPaint
                    )
                    if (cleanSub.isNotEmpty()) {
                        canvas.nativeCanvas.drawRect(
                            left + 2f, topY + titleH, right - 2f, topY + titleH + 4f,
                            subBgPaint
                        )
                    }

                    // Outer crisp retro border
                    canvas.nativeCanvas.drawRoundRect(
                        left, topY, right, bottomY,
                        10f, 10f, borderPaint
                    )

                    // Room title text (white retro font)
                    canvas.nativeCanvas.drawText(
                        label,
                        cx,
                        topY + titleH * 0.72f,
                        textPaint
                    )

                    // Subject / room purpose text + mastery stars
                    if (cleanSub.isNotEmpty()) {
                        canvas.nativeCanvas.drawText(
                            cleanSub,
                            cx,
                            topY + titleH + subH * 0.72f,
                            subTextPaint
                        )
                    }

                    if (hasPriorityBadge) {
                        val bounce = if (reducedMotion) 0f else if (ambientTick % 2 == 0) -3.5f else 0f
                        val bx = right + 6f
                        val by = topY + titleH * 0.5f + bounce
                        val r = tileDrawPx * 0.24f
                        canvas.nativeCanvas.drawCircle(bx, by, r, badgeBg)
                        canvas.nativeCanvas.drawCircle(bx, by, r, badgeBorder)
                        canvas.nativeCanvas.drawText("!", bx, by + tileDrawPx * 0.12f, badgeText)
                    }
                }

                for (npc in map.npcs) {
                    val cx = (npc.pos.x + 0.5f) * tileDrawPx - camX
                    val topY = (npc.pos.y + 1) * tileDrawPx - characterPx - camY + 2f
                    val hasPrio = when (npc.subjectFilter) {
                        "storia" -> storiaPriority
                        "scienze" -> scienzePriority
                        "preside" -> presidePriority
                        "janitor" -> claimableMissionsCount > 0
                        else -> false
                    }
                    val signColor = when (npc.subjectFilter) {
                        "storia" -> android.graphics.Color.argb(235, 180, 83, 9)
                        "scienze" -> android.graphics.Color.argb(235, 15, 118, 110)
                        "preside" -> android.graphics.Color.argb(235, 109, 40, 217)
                        "janitor" -> android.graphics.Color.argb(235, 29, 78, 216)
                        "classmate" -> android.graphics.Color.argb(235, 13, 148, 136)
                        else -> android.graphics.Color.argb(230, 30, 41, 59)
                    }
                    drawTag(cx, topY, npc.label, hasPrio, signColor)
                }

                // Draw interactive Atrium prop signs (Trophy Case + Mission Boards)
                for (prop in map.props) {
                    val cx = (prop.pos.x + 0.5f) * tileDrawPx - camX
                    val propRows = if (map.tileAt(prop.pos.x, prop.pos.y).symbol == 'C') 1 else 0 // teca alta 2 piastrelle
                    val topY = (prop.pos.y - propRows) * tileDrawPx - camY - 4f
                    val isMissions = prop.action == io.github.cybermelo.maestridipixel.pixelart.PortalAction.OpenDailyMissions
                    val badge = isMissions && claimableMissionsCount > 0
                    val propColor = if (isMissions) {
                        android.graphics.Color.argb(235, 15, 76, 129)
                    } else {
                        android.graphics.Color.argb(240, 180, 83, 9)
                    }
                    drawTag(cx, topY, prop.label, badge, propColor)
                }

                for (portal in map.portals) {
                    val cx = (portal.pos.x + 0.5f) * tileDrawPx - camX
                    // Le porte dipinte sono alte 2 piastrelle (tranne il varco nel muro sud): la targa va sopra
                    val doorRows = if (portal.pos.y == map.height - 1) 0 else 1
                    val doorTopY = (portal.pos.y - doorRows) * tileDrawPx - camY
                    val hasPrio = when (portal.id) {
                        "portal_school_entrance" -> storiaPriority || mathPriority || scienzePriority || italianoPriority || geografiaPriority || inglesePriority
                        "portal_history" -> storiaPriority
                        "portal_gym" -> mathPriority
                        "portal_lab" -> scienzePriority
                        "portal_library" -> italianoPriority
                        "portal_geo" -> geografiaPriority
                        "portal_english" -> inglesePriority
                        else -> false
                    }
                    val subjectForPortal = when (portal.id) {
                        "portal_history" -> "storia"
                        "portal_gym" -> "matematica"
                        "portal_lab" -> "scienze"
                        "portal_library" -> "italiano"
                        "portal_geo" -> "geografia"
                        "portal_english" -> "inglese"
                        else -> null
                    }
                    val masteryStars = subjectForPortal?.let { uiState.subjectMastery[it]?.rank?.starsLevel ?: 0 }
                    val signColor = when (portal.id) {
                        "portal_history" -> android.graphics.Color.argb(245, 180, 83, 9)
                        "portal_gym" -> android.graphics.Color.argb(245, 185, 28, 28)
                        "portal_lab" -> android.graphics.Color.argb(245, 13, 148, 136)
                        "portal_library" -> android.graphics.Color.argb(245, 124, 58, 237)
                        "portal_geo" -> android.graphics.Color.argb(245, 2, 132, 199)
                        "portal_english" -> android.graphics.Color.argb(245, 37, 99, 235)
                        "portal_locker" -> android.graphics.Color.argb(245, 217, 119, 6)
                        "portal_school_entrance", "portal_to_yard" -> android.graphics.Color.argb(240, 15, 76, 129)
                        else -> android.graphics.Color.argb(230, 71, 85, 105)
                    }
                    drawDoorPlaque(cx, doorTopY, portal.label, portal.subtitle, hasPrio, signColor, masteryStars)
                }
            }
        }

            // Bottom Controls: Semi-transparent Floating Virtual D-Pad (bottom-left) + Contextual Action Button (bottom-right)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (uiState.config.studyPreferences.showVirtualDpad) {
                    VirtualDpad(
                        onStep = { dx, dy -> stepManual(dx, dy) },
                        isTablet = isTablet,
                        reducedMotion = uiState.config.studyPreferences.reducedMotion
                    )
                } else {
                    Spacer(Modifier.size(1.dp))
                }

                // Quick contextual Interact Button when next to an NPC, Portal, or Interactive Prop
                if (adjacentNpc != null || adjacentPortal != null || adjacentProp != null) {
                    val label = adjacentNpc?.let { "Parla con ${it.name}" }
                        ?: adjacentPortal?.let {
                            if (it.subtitle.isNotBlank() && !it.label.equals(it.subtitle, ignoreCase = true)) {
                                "Entra: ${it.label} (${it.subtitle})"
                            } else {
                                "Entra: ${it.label}"
                            }
                        }
                        ?: adjacentProp?.let { "Apri: ${it.label}" }
                        ?: ""
                    val interactSource = remember { MutableInteractionSource() }
                    val interactPressed by interactSource.collectIsPressedAsState()
                    val interactScale by animateFloatAsState(
                        targetValue = if (interactPressed && !uiState.config.studyPreferences.reducedMotion) 0.95f else 1f,
                        label = "interact_scale"
                    )
                    Button(
                        onClick = {
                            when {
                                adjacentNpc != null -> onNpcInteract(adjacentNpc)
                                adjacentPortal != null -> onPortalTrigger(adjacentPortal.action)
                                adjacentProp != null -> onPortalTrigger(adjacentProp.action)
                            }
                        },
                        interactionSource = interactSource,
                        modifier = Modifier
                            .scale(interactScale)
                            .height(56.dp)
                            .testTag("hub_interact_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        border = BorderStroke(2.5.dp, if (interactPressed) SkyHighlight else IceWhiteText)
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = IceWhiteText)
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.titleMedium, color = IceWhiteText)
                    }
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
    claimableMissionsCount: Int,
    onGuidanceClick: () -> Unit,
    onOpenQuickMap: () -> Unit,
    onOpenDailyMissions: () -> Unit,
    onOpenTrophyHall: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenParentGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = uiState.activeProfile
    val isDark = LocalDarkTheme.current
    val chipBg = if (isDark) DarkCardBg else CreamBackground
    val chipBorder = if (isDark) DarkBorderSlate else GameBalance.GBA_FRAME_DARK
    val textMain = if (isDark) IceWhiteText else InkPrimary
    val accentCol = if (isDark) SkyHighlight else DeepBlueAccent
    val starTint = if (isDark) StarYellow else SoftGold
    val frameColor = GameBalance.frameColor(profile?.equippedFrame ?: "frame_classic")
    val stampsCount = uiState.sessionsThisWeek.coerceIn(0, GameBalance.WEEKLY_GOAL_SESSIONS)
    // Senza pacchetti (primo avvio offline) la pillola resta sempre visibile e al tocco riprova il download
    val recommended = uiState.recommendedActivity
    val noPacks = uiState.activePacks.isEmpty()
    val pillText = when {
        noPacks && uiState.isSyncing -> "Scarico le domande..."
        noPacks -> "Domande da scaricare: tocca per riprovare"
        uiState.config.studyPreferences.showGuidancePill && recommended != null ->
            if (recommended.isBoss) "Sfida Boss: ${recommended.title}" else "Consigliata: ${recommended.title}"
        else -> null
    }
    val pillHighlight = noPacks || recommended?.isBoss == true

    Column(
        modifier = modifier.testTag("hub_hud"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Top-Left: Profile Badge (with equipped Hat!) + Explorer Rank + 3 Weekly Star Stamps
            Surface(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenTrophyHall),
                shape = RoundedCornerShape(12.dp),
                color = chipBg,
                border = BorderStroke(2.5.dp, frameColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (isCompactWidth) 8.dp else 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 6.dp else 8.dp)
                ) {
                    PixelSprite(
                        bitmap = PixelArtAssets.getCharacterSprite(
                            id = "avatar_${profile?.avatarId ?: 0}",
                            facing = Facing.DOWN,
                            stepFrame = 0,
                            hatId = profile?.equippedHat.orEmpty()
                        ),
                        contentDescription = profile?.name ?: "Profilo",
                        modifier = Modifier.size(if (isCompactWidth) 30.dp else 34.dp)
                    )
                    Column {
                        Text(
                            text = "${profile?.name ?: "Studente"} · $hubTitle",
                            style = MaterialTheme.typography.labelLarge,
                            color = textMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            repeat(GameBalance.WEEKLY_GOAL_SESSIONS) { idx ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Timbro settimanale ${idx + 1}",
                                    tint = if (idx < stampsCount) starTint else Color(0xFF64748B),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            if (!isCompactWidth) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = uiState.explorerRank.title,
                                    fontSize = 11.sp,
                                    color = accentCol,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Center Guidance Pill on wide screens
            if (!isCompactWidth && pillText != null) {
                GuidancePill(
                    text = pillText,
                    highlight = pillHighlight,
                    onClick = onGuidanceClick,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 6.dp)
                )
            }

            // Top-Right: Quick Map, Daily Missions, Stars, Medals (opens Trophy Hall), Mute, Parent Gear
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenQuickMap)
                        .testTag("hub_quick_map_button"),
                    shape = RoundedCornerShape(10.dp),
                    color = DeepBlueAccent,
                    border = BorderStroke(1.5.dp, if (isDark) SkyHighlight else IceWhiteText)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = if (isCompactWidth) 8.dp else 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Mappa rapida", tint = IceWhiteText, modifier = Modifier.size(19.dp))
                        if (!isCompactWidth) {
                            Text("Mappa", style = MaterialTheme.typography.labelLarge, color = IceWhiteText)
                        }
                    }
                }

                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenDailyMissions)
                            .testTag("hub_missions_button"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (claimableMissionsCount > 0) MaterialTheme.colorScheme.secondaryContainer else chipBg,
                        border = BorderStroke(1.5.dp, if (claimableMissionsCount > 0) WarmAmber else chipBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = if (isCompactWidth) 8.dp else 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.TaskAlt,
                                contentDescription = "Missioni del giorno",
                                tint = if (claimableMissionsCount > 0) starTint else accentCol,
                                modifier = Modifier.size(19.dp)
                            )
                            if (!isCompactWidth) {
                                Text("Missioni", style = MaterialTheme.typography.labelLarge, color = textMain)
                            }
                        }
                    }
                    if (claimableMissionsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(17.dp)
                                .clip(CircleShape)
                                .background(WarmAmber)
                                .border(1.5.dp, IceWhiteText, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$claimableMissionsCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                HudStatChip(
                    icon = Icons.Default.Star,
                    iconTint = starTint,
                    value = "${profile?.stars ?: 0}",
                    contentDesc = "Stelle"
                )
                HudStatChip(
                    icon = Icons.Default.EmojiEvents,
                    iconTint = WarmAmber,
                    value = "${profile?.medals?.size ?: 0}",
                    contentDesc = "Medaglie",
                    onClick = onOpenTrophyHall,
                    testTag = "hud_medals_button"
                )
                Surface(
                    shape = CircleShape,
                    color = chipBg,
                    border = BorderStroke(1.5.dp, chipBorder)
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
                            tint = textMain
                        )
                    }
                }
                ParentHoldGearButton(onUnlocked = onOpenParentGate)
            }
        }

        // Compact phone guidance pill on second row so nothing clips
        if (isCompactWidth && pillText != null) {
            GuidancePill(
                text = pillText,
                highlight = pillHighlight,
                onClick = onGuidanceClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun GuidancePill(
    text: String,
    highlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("guidance_pill"),
        shape = RoundedCornerShape(20.dp),
        color = if (highlight) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.5.dp, if (highlight) WarmAmber else if (isDark) SkyHighlight else DeepBlueAccent)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlight) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun HudStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    contentDesc: String,
    onClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    val isDark = LocalDarkTheme.current
    val baseMod = Modifier
        .let { m -> if (testTag != null) m.testTag(testTag) else m }
        .let { m -> if (onClick != null) m.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick) else m }
    Surface(
        modifier = baseMod,
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) DarkCardBg else CreamBackground,
        border = BorderStroke(1.5.dp, if (isDark) DarkBorderSlate else GameBalance.GBA_FRAME_DARK)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = contentDesc, tint = iconTint, modifier = Modifier.size(19.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, color = if (isDark) IceWhiteText else InkPrimary)
        }
    }
}

/**
 * Parent Area Gear button with progressive 0% -> 100% fill over 2.0s hold.
 * Cancels only if pointer leaves button bounds or is released early.
 */
@Composable
private fun ParentHoldGearButton(onUnlocked: () -> Unit) {
    val isDark = LocalDarkTheme.current
    var progress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val latestOnUnlocked by rememberUpdatedState(onUnlocked)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isDark) DarkCardBg else CreamBackground)
            .border(1.5.dp, if (isDark) DarkBorderSlate else GameBalance.GBA_FRAME_DARK, CircleShape)
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
                modifier = Modifier.size(42.dp),
                color = if (isDark) SkyHighlight else DeepBlueAccent,
                strokeWidth = 4.dp,
                trackColor = if (isDark) DarkBorderSlate else Color(0xFFE2E8F0)
            )
        }
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Tieni premuto 2 secondi per Area Genitori",
            tint = if (isDark) IceWhiteText else InkPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}
