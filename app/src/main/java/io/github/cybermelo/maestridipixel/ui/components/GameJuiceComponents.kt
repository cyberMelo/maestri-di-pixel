package io.github.cybermelo.maestridipixel.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cybermelo.maestridipixel.R
import io.github.cybermelo.maestridipixel.core.DailyMissionItem
import io.github.cybermelo.maestridipixel.core.GameBalance
import io.github.cybermelo.maestridipixel.pixelart.PortalAction
import io.github.cybermelo.maestridipixel.ui.theme.BackdropScrimDark
import io.github.cybermelo.maestridipixel.ui.theme.CreamBackground
import io.github.cybermelo.maestridipixel.ui.theme.CreamSurface
import io.github.cybermelo.maestridipixel.ui.theme.DarkCardBg
import io.github.cybermelo.maestridipixel.ui.theme.DeepBlueAccent
import io.github.cybermelo.maestridipixel.ui.theme.EmeraldSuccess
import io.github.cybermelo.maestridipixel.ui.theme.IceWhiteText
import io.github.cybermelo.maestridipixel.ui.theme.InkPrimary
import io.github.cybermelo.maestridipixel.ui.theme.InkSecondary
import io.github.cybermelo.maestridipixel.ui.theme.LocalDarkTheme
import io.github.cybermelo.maestridipixel.ui.theme.SilkscreenFontFamily
import io.github.cybermelo.maestridipixel.ui.theme.SoftGold
import io.github.cybermelo.maestridipixel.ui.theme.StarYellow
import io.github.cybermelo.maestridipixel.ui.theme.WarmAmber
import io.github.cybermelo.maestridipixel.viewmodel.AppUiState
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@DrawableRes
fun subjectBannerDrawable(subject: String): Int = when (subject.lowercase()) {
    "storia", "italiano" -> R.drawable.img_banner_humanities
    "matematica", "scienze" -> R.drawable.img_banner_stem
    "geografia", "inglese" -> R.drawable.img_banner_world
    else -> R.drawable.img_school_hero
}

private data class ConfettiPiece(
    val xFrac: Float,
    val startYFrac: Float,
    val speedY: Float,
    val driftX: Float,
    val sizePx: Float,
    val color: Color,
    val phase: Float
)

/**
 * Celebratory 16-bit pixel confetti burst overlay (honors reducedMotion).
 */
@Composable
fun PixelConfettiOverlay(
    active: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    if (!active || reducedMotion) return
    val pieces = remember {
        val rng = Random(2026)
        val palette = listOf(
            Color(0xFFFACC15),
            Color(0xFF34D399),
            Color(0xFF38BDF8),
            Color(0xFFF472B6),
            Color(0xFFFB923C),
            Color(0xFFA78BFA)
        )
        List(46) {
            ConfettiPiece(
                xFrac = rng.nextFloat(),
                startYFrac = -rng.nextFloat() * 0.45f,
                speedY = 0.55f + rng.nextFloat() * 0.85f,
                driftX = (rng.nextFloat() - 0.5f) * 0.22f,
                sizePx = 8f + rng.nextInt(3) * 4f,
                color = palette[rng.nextInt(palette.size)],
                phase = rng.nextFloat() * 6.28f
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 2600, easing = LinearEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        val alphaFade = (1f - ((t - 0.72f) / 0.28f).coerceIn(0f, 1f))
        for (p in pieces) {
            val y = (p.startYFrac + t * p.speedY * 1.4f) * size.height
            if (y < -20f || y > size.height + 20f) continue
            val sway = sin(t * 9f + p.phase) * 18f
            val x = p.xFrac * size.width + p.driftX * size.width * t + sway
            drawRect(
                color = p.color.copy(alpha = alphaFade),
                topLeft = Offset(x, y),
                size = Size(p.sizePx, p.sizePx)
            )
            // Tiny retro pixel highlight
            drawRect(
                color = Color.White.copy(alpha = alphaFade * 0.55f),
                topLeft = Offset(x, y),
                size = Size(p.sizePx * 0.4f, p.sizePx * 0.4f)
            )
        }
    }
}

/**
 * Floating arcade "+15 ★" or "COMBO x3!" popup that rises and fades out smoothly.
 */
@Composable
fun FloatingJuicePopup(
    text: String,
    accentColor: Color,
    triggerId: Int,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    if (triggerId <= 0 || text.isBlank()) return
    val anim = remember { Animatable(0f) }
    LaunchedEffect(triggerId) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = if (reducedMotion) 600 else 1050)
        )
    }
    val p = anim.value
    if (p >= 0.99f) return
    val risePx = if (reducedMotion) 0 else (-64f * p).roundToInt()
    val alpha = if (p < 0.7f) 1f else ((1f - p) / 0.3f).coerceIn(0f, 1f)
    val scale = if (reducedMotion) 1f else if (p < 0.2f) 0.75f + p * 1.6f else 1.07f - (p - 0.2f) * 0.12f

    Surface(
        modifier = modifier
            .offset { IntOffset(0, risePx) }
            .scale(scale)
            .alpha(alpha),
        shape = RoundedCornerShape(14.dp),
        color = accentColor,
        border = BorderStroke(2.5.dp, Color.White),
        shadowElevation = 6.dp
    ) {
        Text(
            text = text,
            fontFamily = SilkscreenFontFamily,
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/**
 * Interactive Trophy Hall & Explorer Mastery Passport opened from the Atrium Trophy Case or Quick Map.
 */
@Composable
fun TrophyHallModal(
    uiState: AppUiState,
    onDismiss: () -> Unit
) {
    val earnedIds = uiState.activeProfile?.medals?.toSet() ?: emptySet()
    val visiblePacks = uiState.allPacksIncludingArchived.filter { !it.archived || it.packId in earnedIds }
    val rank = uiState.explorerRank
    val subjectsOrder = listOf("storia", "matematica", "scienze", "italiano", "geografia", "inglese")

    val isDark = LocalDarkTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackdropScrimDark)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        GbaCard(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 760.dp)
                .heightIn(max = 600.dp)
                .clickable(enabled = false) {}
                .testTag("trophy_hall_modal"),
            borderColor = StarYellow
        ) {
            // Illustrated 16-bit Trophy Hall Banner Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_banner_trophy),
                    contentDescription = "Sala dei Trofei",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xDC0F172A), Color(0x980F172A), Color(0x4D0F172A))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sala Trofei & Maestria",
                            style = MaterialTheme.typography.titleLarge,
                            color = StarYellow
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StarYellow
                            ) {
                                Text(
                                    text = "Grado: ${rank.title}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF451A03),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "${earnedIds.size}/${visiblePacks.size} Medaglie",
                                style = MaterialTheme.typography.labelLarge,
                                color = IceWhiteText
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                            .testTag("trophy_hall_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Subject Mastery Passport
                item {
                    Text(
                        text = "Passaporto delle Materie",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjectsOrder.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { subj ->
                                    val summary = uiState.subjectMastery[subj]
                                    val pct = (summary?.averageMasteryPct ?: 0f).toInt().coerceIn(0, 100)
                                    val subjColor = GameBalance.subjectColor(subj)
                                    val starsLvl = summary?.rank?.starsLevel ?: 0
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.5.dp, subjColor.copy(alpha = 0.75f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = GameBalance.subjectLabel(subj),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (isDark) IceWhiteText else subjColor
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                                    repeat(3) { idx ->
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = if (idx < starsLvl) StarYellow else Color(0xFF64748B),
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { pct / 100f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = subjColor,
                                                trackColor = if (isDark) DarkCardBg else Color(0xFFE2E8F0)
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = "${summary?.rank?.title ?: "Curioso"} · $pct%",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Boss Medals Showcase
                item {
                    Text(
                        text = "Medaglie dei Boss",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        visiblePacks.chunked(3).forEach { rowPacks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPacks.forEach { pack ->
                                    val earned = pack.packId in earnedIds
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (earned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            width = 2.dp,
                                            color = if (earned) StarYellow else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (earned) Color(0xFFF59E0B) else if (isDark) DarkCardBg else Color(0xFFE2E8F0)
                                                    )
                                                    .border(
                                                        2.dp,
                                                        if (earned) Color(0xFF92400E) else Color(0xFF64748B),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (earned) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                                    contentDescription = pack.medalName,
                                                    tint = if (earned) IceWhiteText else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(Modifier.height(5.dp))
                                            Text(
                                                text = pack.medalName,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (earned) "Conquistata!" else pack.bossName,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (earned) (if (isDark) Color(0xFF34D399) else EmeraldSuccess) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowPacks.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Daily Missions Board opened from the Atrium Notice Boards or HUD badge.
 */
@Composable
fun DailyMissionsModal(
    missions: List<DailyMissionItem>,
    onClaim: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackdropScrimDark)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        GbaCard(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 620.dp)
                .heightIn(max = 560.dp)
                .clickable(enabled = false) {}
                .testTag("daily_missions_modal"),
            borderColor = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = null,
                        tint = StarYellow,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Missioni del Giorno",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Completa gli obiettivi di oggi per vincere stelle bonus!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("daily_missions_close")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(missions, key = { it.missionKey }) { mission ->
                    val ratio = (mission.progress.toFloat() / mission.target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            mission.isClaimable -> MaterialTheme.colorScheme.secondaryContainer
                            mission.claimed -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            width = 2.dp,
                            color = when {
                                mission.isClaimable -> StarYellow
                                mission.claimed -> EmeraldSuccess
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mission.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = mission.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp)),
                                        color = if (mission.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                                        trackColor = if (isDark) DarkCardBg else Color(0xFFE2E8F0)
                                    )
                                    Text(
                                        text = "${mission.progress}/${mission.target}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            when {
                                mission.claimed -> {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = EmeraldSuccess
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IceWhiteText, modifier = Modifier.size(18.dp))
                                            Text("Preso!", style = MaterialTheme.typography.labelLarge, color = IceWhiteText)
                                        }
                                    }
                                }
                                mission.isClaimable -> {
                                    Button(
                                        onClick = { onClaim(mission.missionKey) },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("claim_mission_${mission.missionKey}"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StarYellow,
                                            contentColor = Color(0xFF451A03)
                                        )
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("+${mission.rewardStars}", fontWeight = FontWeight.Bold)
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        border = BorderStroke(1.5.dp, StarYellow)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(16.dp))
                                            Text("+${mission.rewardStars}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class QuickMapDestination(
    val id: String,
    val title: String,
    val subtitle: String,
    val subjectKey: String?,
    val accentColor: Color,
    val action: PortalAction
)

/**
 * Tablet-friendly Quick Map Modal allowing instant jump to any classroom, Trophy Hall, Locker, or Yard.
 */
@Composable
fun QuickMapModal(
    uiState: AppUiState,
    currentHubId: String,
    onSelectAction: (PortalAction) -> Unit,
    onDismiss: () -> Unit
) {
    val destinations = remember(currentHubId) {
        listOf(
            QuickMapDestination("map_history", "Aula Storia", "Sfida GBA", "storia", GameBalance.subjectColor("storia"), PortalAction.OpenHistoryPicker),
            QuickMapDestination("map_gym", "Palestra", "Matematica", "matematica", GameBalance.subjectColor("matematica"), PortalAction.OpenGymPicker),
            QuickMapDestination("map_lab", "Laboratorio", "Scienze", "scienze", GameBalance.subjectColor("scienze"), PortalAction.OpenScienceLabPicker),
            QuickMapDestination("map_library", "Biblioteca", "Italiano", "italiano", GameBalance.subjectColor("italiano"), PortalAction.OpenLibraryPicker),
            QuickMapDestination("map_geo", "Geografia", "Mongolfiera", "geografia", GameBalance.subjectColor("geografia"), PortalAction.OpenGeoPicker),
            QuickMapDestination("map_english", "Inglese", "Parole EN", "inglese", GameBalance.subjectColor("inglese"), PortalAction.OpenEnglishPicker),
            QuickMapDestination("map_locker", "Armadietto", "Premi & Cappelli", null, Color(0xFFD97706), PortalAction.OpenLocker),
            QuickMapDestination("map_trophy", "Sala Trofei", "Medaglie & Gradi", null, Color(0xFFB45309), PortalAction.OpenTrophyHall),
            QuickMapDestination(
                id = "map_hub_switch",
                title = if (currentHubId == "school_hallway") "Cortile Scuola" else "Atrio Scuola",
                subtitle = if (currentHubId == "school_hallway") "Vai all'aperto" else "Vai alle aule",
                subjectKey = null,
                accentColor = DeepBlueAccent,
                action = if (currentHubId == "school_hallway") {
                    PortalAction.GoToHub("school_yard", "from_hallway")
                } else {
                    PortalAction.GoToHub("school_hallway", "from_yard")
                }
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackdropScrimDark)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        GbaCard(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 760.dp)
                .heightIn(max = 580.dp)
                .clickable(enabled = false) {}
                .testTag("quick_map_modal"),
            borderColor = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    Column {
                        Text(
                            text = "Mappa Rapida della Scuola",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tocca una stanza per andare subito lì!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("quick_map_close")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 190.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(destinations, key = { it.id }) { dest ->
                    val subjSummary = dest.subjectKey?.let { uiState.subjectMastery[it] }
                    val starsLvl = subjSummary?.rank?.starsLevel ?: 0
                    val hasPrio = subjSummary?.hasPriority == true

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onDismiss()
                                onSelectAction(dest.action)
                            }
                            .testTag("quick_map_${dest.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(2.5.dp, if (hasPrio) WarmAmber else dest.accentColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(dest.accentColor)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = dest.subtitle.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = IceWhiteText,
                                        maxLines = 1
                                    )
                                }
                                if (hasPrio) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(WarmAmber),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("!", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = IceWhiteText)
                                    }
                                } else if (dest.subjectKey != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                        repeat(3) { idx ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (idx < starsLvl) StarYellow else Color(0xFF64748B),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = null,
                                        tint = dest.accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = dest.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subjSummary != null) {
                                val pct = subjSummary.averageMasteryPct.toInt().coerceIn(0, 100)
                                Text(
                                    text = "${subjSummary.rank.title} · $pct%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Apri subito",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
