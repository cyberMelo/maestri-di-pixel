package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixelart.Facing
import com.example.pixelart.PixelArtAssets
import com.example.ui.theme.SilkscreenFontFamily
import kotlin.math.roundToInt

/** Reazione del maestro all'ultima risposta. */
enum class TeacherMood { IDLE, HAPPY, SAD }

/**
 * Palco in cima ai mini-giochi: l'aula dipinta, il maestro della materia che reagisce alle risposte,
 * la domanda in un fumetto e (opzionale) la "collezione" della sessione a destra.
 *
 * [moodTrigger] va incrementato a ogni risposta: fa ripartire l'animazione anche se l'umore non cambia.
 * Con [compact] (telefono in orizzontale) resta solo una scheda con mini-ritratto e fumetto.
 */
@Composable
fun MinigameStage(
    roomId: String?,
    teacherSpriteId: String,
    teacherName: String,
    accent: Color,
    mood: TeacherMood,
    moodTrigger: Int,
    reducedMotion: Boolean,
    compact: Boolean,
    expandedHeight: Dp,
    modifier: Modifier = Modifier,
    backdrop: (DrawScope.() -> Unit)? = null,
    side: (@Composable BoxScope.() -> Unit)? = null,
    bubble: @Composable ColumnScope.() -> Unit
) {
    val hop = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    var walkFrame by remember { mutableIntStateOf(0) }
    LaunchedEffect(moodTrigger) {
        if (moodTrigger == 0 || reducedMotion) return@LaunchedEffect
        when (mood) {
            TeacherMood.HAPPY -> repeat(2) { i ->
                walkFrame = 1 + i % 2
                hop.animateTo(-1f, tween(130))
                hop.animateTo(0f, tween(130))
                walkFrame = 0
            }
            TeacherMood.SAD -> for (x in listOf(-1f, 1f, -0.6f, 0.6f, 0f)) shake.animateTo(x, tween(60))
            TeacherMood.IDLE -> Unit
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Sotto i 600 dp di larghezza il palco grande non ci sta: scheda compatta
        if (compact || maxWidth < 600.dp) {
            CompactStage(teacherSpriteId, teacherName, accent, walkFrame, shake.value, hop.value, bubble)
        } else {
            ExpandedStage(
                roomId, teacherSpriteId, teacherName, accent, walkFrame, shake.value, hop.value,
                Modifier.height(expandedHeight), backdrop, side, bubble
            )
        }
    }
}

@Composable
private fun CompactStage(
    teacherSpriteId: String,
    teacherName: String,
    accent: Color,
    walkFrame: Int,
    shake: Float,
    hop: Float,
    bubble: @Composable ColumnScope.() -> Unit
) {
    GbaCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = accent,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelSprite(
                bitmap = PixelArtAssets.getCharacterSprite(teacherSpriteId, Facing.DOWN, walkFrame),
                contentDescription = teacherName,
                modifier = Modifier.size(40.dp).offset(x = (shake * 4).dp, y = (hop * 6).dp)
            )
            Column(modifier = Modifier.weight(1f), content = bubble)
        }
    }
}

@Composable
private fun ExpandedStage(
    roomId: String?,
    teacherSpriteId: String,
    teacherName: String,
    accent: Color,
    walkFrame: Int,
    shake: Float,
    hop: Float,
    modifier: Modifier,
    backdrop: (DrawScope.() -> Unit)?,
    side: (@Composable BoxScope.() -> Unit)?,
    bubble: @Composable ColumnScope.() -> Unit
) {
    val room = roomId?.let { remember(it) { PixelArtAssets.getRoomBackground(it) } }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(3.dp, accent),
        shadowElevation = 6.dp,
        color = Color(0xFF1E293B)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            // Fattori interi (pixel nitidi): l'aula quanto basta per mostrare parete e pavimento e
            // coprire la larghezza; il maestro un gradino più grande, entro il 75% dell'altezza.
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val roomScale = room?.let { bg ->
                maxOf(
                    kotlin.math.ceil(wPx / bg.width).toInt(),
                    (hPx / bg.height).roundToInt()
                ).coerceAtLeast(1)
            } ?: 1
            val maxSpriteScale = ((hPx * 0.75f) / PixelArtAssets.CHARACTER_PX).toInt().coerceAtLeast(1)
            // Senza aula dipinta non c'è una scala da rispettare: maestro grande (x3)
            val spriteScale = (if (room == null) 3 else roomScale + 1).coerceAtMost(maxSpriteScale)
            val spriteDp = with(density) { (PixelArtAssets.CHARACTER_PX * spriteScale).toDp() }
            val stageH = maxHeight
            val sideW = maxWidth * 0.27f
            Canvas(modifier = Modifier.matchParentSize()) {
                val bg = room
                if (bg != null) {
                    val cropW = (size.width / roomScale).roundToInt().coerceAtMost(bg.width)
                    val cropH = (size.height / roomScale).roundToInt().coerceAtMost(bg.height)
                    drawImage(
                        image = bg,
                        srcOffset = IntOffset((bg.width - cropW) / 2, (bg.height - cropH) / 2),
                        srcSize = IntSize(cropW, cropH),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        filterQuality = FilterQuality.None
                    )
                }
                backdrop?.invoke(this)
            }
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.fillMaxHeight().width(spriteDp), contentAlignment = Alignment.BottomCenter) {
                    PixelSprite(
                        bitmap = PixelArtAssets.getCharacterSprite(teacherSpriteId, Facing.DOWN, walkFrame),
                        contentDescription = teacherName,
                        modifier = Modifier
                            .size(spriteDp)
                            .offset(x = (shake * 6).dp, y = (hop * 14).dp - stageH * 0.04f)
                    )
                }
                SpeechBubble(
                    name = teacherName,
                    accent = accent,
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                    content = bubble
                )
                if (side != null) {
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier.fillMaxHeight().width(sideW).padding(vertical = 10.dp),
                        content = side
                    )
                }
            }
        }
    }
}

/** Fumetto con codina verso sinistra (il maestro) e il nome in alto. */
@Composable
private fun SpeechBubble(
    name: String,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val fill = MaterialTheme.colorScheme.surface
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(width = 14.dp, height = 22.dp)) {
            val tail = Path().apply {
                moveTo(size.width, 0f)
                lineTo(0f, size.height / 2f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(tail, accent)
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = fill.copy(alpha = 0.96f),
            border = BorderStroke(2.5.dp, accent),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = name.uppercase(),
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 11.sp,
                    color = accent
                )
                Spacer(Modifier.height(2.dp))
                content()
            }
        }
    }
}

// ---------------------------------------------------------------- Collezioni della sessione

/** Lavagna con le parole imparate in questa sessione, scritte "col gesso". */
@Composable
fun ChalkboardPanel(title: String, words: List<String>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF2F4F3E),
        border = BorderStroke(6.dp, Color(0xFF8B5A2B))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, fontFamily = SilkscreenFontFamily, fontSize = 11.sp, color = Color(0xFFFDE68A))
            Spacer(Modifier.height(4.dp))
            if (words.isEmpty()) {
                Text("...", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp)
            }
            words.takeLast(6).forEach { w ->
                Text(
                    text = w,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val BOOK_COLORS = listOf(
    Color(0xFFDC2626), Color(0xFF2563EB), Color(0xFF16A34A), Color(0xFFD97706),
    Color(0xFF7C3AED), Color(0xFF0D9488), Color(0xFFDB2777), Color(0xFF4B5563)
)

/** Scaffale che si riempie di un libro a ogni risposta giusta. */
@Composable
fun BookshelfPanel(collected: Int, total: Int, modifier: Modifier = Modifier) {
    StageCard(title = "Libri salvati: $collected/$total", modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val slots = total.coerceAtLeast(1)
            val shelfH = size.height * 0.14f
            val bookW = (size.width / (slots + 1)).coerceAtMost(size.width / 6f)
            val baseY = size.height - shelfH
            drawRect(Color(0xFF6B3F1D), Offset(0f, baseY), Size(size.width, shelfH))
            drawRect(Color(0xFF8B5A2B), Offset(0f, baseY), Size(size.width, shelfH * 0.35f))
            val startX = (size.width - bookW * slots) / 2f
            for (i in 0 until slots) {
                val h = size.height * (0.55f + 0.08f * ((i * 5) % 3))
                val x = startX + i * bookW
                if (i < collected) {
                    val c = BOOK_COLORS[i % BOOK_COLORS.size]
                    drawRect(c, Offset(x + 2f, baseY - h), Size(bookW - 4f, h))
                    drawRect(Color.White.copy(alpha = 0.6f), Offset(x + 2f, baseY - h * 0.75f), Size(bookW - 4f, h * 0.06f))
                    drawRect(Color.Black.copy(alpha = 0.25f), Offset(x + bookW - 6f, baseY - h), Size(4f, h))
                } else {
                    drawRect(
                        Color.White.copy(alpha = 0.35f),
                        Offset(x + 2f, baseY - h), Size(bookW - 4f, h),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
            }
        }
    }
}

/** Piantina che cresce dal seme al fiore con le risposte giuste. */
@Composable
fun PlantGrowthPanel(collected: Int, total: Int, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = if (total <= 0) 0f else collected.toFloat() / total,
        animationSpec = tween(500),
        label = "plant_growth"
    )
    val label = when {
        progress >= 1f -> "Fiorita!"
        progress >= 0.6f -> "Quasi in fiore"
        progress > 0f -> "Sta crescendo"
        else -> "Un piccolo seme"
    }
    StageCard(title = "La tua piantina: $label", modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val px = (size.minDimension / 24f).coerceAtLeast(2f) // "pixel" del disegno
            val cx = size.width / 2f
            val potTop = size.height - px * 6
            fun block(x: Float, y: Float, w: Int, h: Int, c: Color) =
                drawRect(c, Offset(x, y), Size(w * px, h * px))
            // Vaso
            block(cx - px * 5, potTop, 10, 2, Color(0xFFB45309))
            block(cx - px * 4, potTop + px * 2, 8, 4, Color(0xFF92400E))
            block(cx - px * 4, potTop, 8, 1, Color(0xFF3F2A14))
            // Stelo e foglie proporzionali alla crescita
            val maxStem = ((potTop - px * 4) / px).toInt().coerceAtLeast(4)
            val stem = (maxStem * progress).toInt()
            if (stem <= 0) {
                block(cx - px, potTop - px, 2, 1, Color(0xFF78350F))
            } else {
                block(cx - px / 2, potTop - stem * px, 1, stem, Color(0xFF15803D))
                var leafY = 2
                var side = -1
                while (leafY < stem - 1) {
                    val lx = if (side < 0) cx - px * 3.5f else cx + px / 2
                    block(lx, potTop - leafY * px, 3, 1, Color(0xFF22C55E))
                    block(lx + (if (side < 0) px else 0f), potTop - (leafY + 1) * px, 2, 1, Color(0xFF4ADE80))
                    leafY += 3
                    side = -side
                }
                if (progress >= 1f) {
                    val fy = potTop - (stem + 2) * px
                    for ((dx, dy) in listOf(0 to -1, -1 to 0, 1 to 0, 0 to 1)) {
                        block(cx - px / 2 + dx * px * 1.2f, fy + dy * px * 1.2f, 1, 1, Color(0xFFF472B6))
                    }
                    block(cx - px / 2, fy, 1, 1, Color(0xFFFDE047))
                }
            }
        }
    }
}

/** Viaggio in mongolfiera tra le città: avanza di una tappa a ogni risposta giusta. */
@Composable
fun JourneyPanel(collected: Int, total: Int, modifier: Modifier = Modifier) {
    val cities = listOf("Milano", "Bologna", "Firenze", "Roma", "Napoli", "Palermo")
    val progress by animateFloatAsState(
        targetValue = if (total <= 0) 0f else (collected.toFloat() / total).coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "journey"
    )
    val reached = cities[((cities.size - 1) * progress).toInt().coerceIn(0, cities.size - 1)]
    StageCard(title = "Tappa: $reached", modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pts = cities.indices.map { i ->
                val t = i / (cities.size - 1f)
                // Percorso a "stivale": giù verso destra, poi l'ultima tappa a sinistra (Sicilia)
                val x = size.width * (0.25f + 0.45f * t - if (i == cities.size - 1) 0.25f else 0f)
                Offset(x, size.height * (0.08f + 0.84f * t))
            }
            for (i in 0 until pts.size - 1) {
                drawLine(Color.White.copy(alpha = 0.5f), pts[i], pts[i + 1], strokeWidth = 4f)
            }
            pts.forEachIndexed { i, p ->
                val done = i <= (cities.size - 1) * progress + 0.001f
                drawCircle(if (done) Color(0xFFFDE047) else Color.White.copy(alpha = 0.35f), radius = 7f, center = p)
            }
            val seg = (cities.size - 1) * progress
            val i0 = seg.toInt().coerceIn(0, cities.size - 2)
            val f = seg - i0
            val pos = Offset(
                pts[i0].x + (pts[i0 + 1].x - pts[i0].x) * f,
                pts[i0].y + (pts[i0 + 1].y - pts[i0].y) * f
            )
            // Mongolfiera
            drawCircle(Color(0xFFEF4444), radius = 13f, center = pos - Offset(0f, 18f))
            drawRect(Color(0xFFFDE047), Offset(pos.x - 3f, pos.y - 31f), Size(6f, 26f))
            drawRect(Color(0xFF92400E), Offset(pos.x - 5f, pos.y - 4f), Size(10f, 7f))
        }
    }
}

/** Razzo che fa il pieno di carburante con le risposte giuste. */
@Composable
fun RocketFuelPanel(collected: Int, total: Int, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = if (total <= 0) 0f else (collected.toFloat() / total).coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "rocket_fuel"
    )
    StageCard(title = if (progress >= 1f) "Pronto al decollo!" else "Carburante: ${(progress * 100).roundToInt()}%", modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tankW = size.width * 0.22f
            val tankH = size.height * 0.92f
            val tx = size.width * 0.62f
            val ty = (size.height - tankH) / 2f
            drawRoundRect(Color.White.copy(alpha = 0.15f), Offset(tx, ty), Size(tankW, tankH), CornerRadius(8f))
            drawRoundRect(
                Color(0xFF22D3EE), Offset(tx, ty + tankH * (1 - progress)), Size(tankW, tankH * progress), CornerRadius(8f)
            )
            // Razzo pixel
            val px = (size.height / 22f).coerceAtLeast(2f)
            val rx = size.width * 0.28f
            val ry = size.height * 0.12f - (if (progress >= 1f) px * 2 else 0f)
            fun block(x: Int, y: Int, w: Int, h: Int, c: Color) =
                drawRect(c, Offset(rx + x * px, ry + y * px), Size(w * px, h * px))
            block(1, 0, 2, 1, Color(0xFFEF4444))
            block(0, 1, 4, 10, Color(0xFFE2E8F0))
            block(1, 3, 2, 2, Color(0xFF38BDF8))
            block(-1, 8, 1, 4, Color(0xFFEF4444))
            block(4, 8, 1, 4, Color(0xFFEF4444))
            if (progress > 0f) block(1, 11, 2, (2 + 4 * progress).toInt(), Color(0xFFF59E0B))
        }
    }
}

@Composable
private fun StageCard(title: String, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                title,
                fontFamily = SilkscreenFontFamily,
                fontSize = 10.sp,
                color = Color(0xFFFDE68A),
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}

/** Altezza consigliata del palco: ~30% dello schermo, mai sotto 150 dp né sopra [max]. */
fun stageHeight(screenHeight: Dp, max: Dp = 260.dp): Dp = (screenHeight * 0.30f).coerceIn(150.dp, max)
