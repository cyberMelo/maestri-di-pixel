package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.GameBalance
import com.example.core.MinigameResult
import com.example.core.PackData
import com.example.core.TextFormatter
import com.example.pixelart.Facing
import com.example.pixelart.PixelArtAssets
import com.example.ui.theme.BackdropScrimDark
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.CreamSurface
import com.example.ui.theme.DarkBorderSlate
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepBlueAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IceWhiteText
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.SkyHighlight
import com.example.ui.theme.SlateMutedText
import com.example.ui.theme.SoftGold
import com.example.ui.theme.StarYellow
import com.example.ui.theme.WarmAmber
import com.example.viewmodel.PinMode
import kotlinx.coroutines.delay

@Composable
fun PixelSprite(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        filterQuality = FilterQuality.None,
        modifier = modifier
    )
}

@Composable
fun GbaCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current
    val outerShape = RoundedCornerShape(14.dp)
    val innerShape = RoundedCornerShape(11.dp)
    Surface(
        modifier = modifier,
        shape = outerShape,
        color = backgroundColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(2.5.dp, borderColor),
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .border(
                    width = 1.5.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.65f),
                    shape = innerShape
                )
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun RetroHpBar(
    progress: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF1E293B))
            .border(2.dp, GameBalance.GBA_FRAME_DARK, RoundedCornerShape(7.dp))
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF334155))
        )
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .background(barColor)
            ) {
                // Glossy 16-bit top highlight stripe
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.36f))
                )
            }
        }
    }
}

enum class StepDotState {
    PENDING,
    CURRENT,
    RETRY,
    COMPLETED
}

@Composable
fun ProgressStepper(
    states: List<StepDotState>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val total = states.size.coerceAtLeast(1)
    val displayCurrent = (currentIndex + 1).coerceIn(1, total)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("progress_stepper"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Domanda $displayCurrent di $total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            states.forEach { st ->
                val color = when (st) {
                    StepDotState.PENDING -> if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                    StepDotState.CURRENT -> MaterialTheme.colorScheme.primary
                    StepDotState.RETRY -> WarmAmber
                    StepDotState.COMPLETED -> EmeraldSuccess
                }
                val size = if (st == StepDotState.CURRENT) 14.dp else 11.dp
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), CircleShape)
                )
            }
        }
    }
}

@Composable
fun DialogBoxModal(
    title: String,
    message: String,
    speakerSpriteId: String? = null,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackdropScrimDark)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        GbaCard(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 580.dp)
                .clickable(enabled = false) {}
                .testTag("dialog_box_modal")
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (speakerSpriteId != null) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelSprite(
                            bitmap = PixelArtAssets.getCharacterSprite(speakerSpriteId, Facing.DOWN, 0),
                            contentDescription = title,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = TextFormatter.toAnnotatedString(message),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 30.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 52.dp)
                    .testTag("dialog_ok_button")
            ) {
                Text("Va bene!", style = MaterialTheme.typography.labelLarge)
            }
            }
        }
    }
}

@Composable
fun ConfirmModal(
    title: String,
    message: String,
    confirmLabel: String = "Conferma",
    cancelLabel: String = "Annulla",
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackdropScrimDark),
        contentAlignment = Alignment.Center
    ) {
        GbaCard(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 520.dp)
                .testTag("confirm_modal")
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            Text(text = message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("confirm_cancel_button")
                ) {
                    Text(cancelLabel, style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("confirm_ok_button")
                ) {
                    Text(confirmLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ExplanationModal(
    header: String,
    correctAnswerDisplay: String,
    explanation: String,
    autoRead: Boolean,
    onSpeak: (String) -> Unit,
    onStopSpeak: () -> Unit,
    onContinue: () -> Unit
) {
    var unlocked by remember { mutableStateOf(false) }

    LaunchedEffect(explanation) {
        onStopSpeak()
        if (autoRead) {
            onSpeak("$header Risposta esatta: $correctAnswerDisplay. $explanation")
        }
        delay(GameBalance.EXPLANATION_UNLOCK_DELAY_MS)
        unlocked = true
    }

    DisposableEffect(Unit) {
        onDispose { onStopSpeak() }
    }

    val isDark = LocalDarkTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackdropScrimDark),
        contentAlignment = Alignment.Center
    ) {
        GbaCard(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 580.dp)
                .testTag("explanation_modal"),
            borderColor = WarmAmber,
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isDark) StarYellow else WarmAmber,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onSpeak("Risposta esatta: $correctAnswerDisplay. $explanation") },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("explanation_tts_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Ascolta spiegazione",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(1.5.dp, WarmAmber, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Risposta giusta: $correctAnswerDisplay",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = TextFormatter.toAnnotatedString(explanation),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 30.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    onStopSpeak()
                    onContinue()
                },
                enabled = unlocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("explanation_continue_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (unlocked) "Ho capito!" else "Leggi con calma...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            }
        }
    }
}

@Composable
fun ResultSummaryModal(
    result: MinigameResult,
    packTitle: String,
    medalName: String,
    reducedMotion: Boolean = false,
    onContinue: () -> Unit
) {
    val starsAnim = remember(result.starsEarned) { Animatable(0f) }
    LaunchedEffect(result.starsEarned, reducedMotion) {
        if (reducedMotion) {
            starsAnim.snapTo(result.starsEarned.toFloat())
        } else {
            starsAnim.snapTo(0f)
            starsAnim.animateTo(
                targetValue = result.starsEarned.toFloat(),
                animationSpec = tween(durationMillis = 750)
            )
        }
    }
    val displayedStars = starsAnim.value.toInt()

    val isDark = LocalDarkTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackdropScrimDark),
        contentAlignment = Alignment.Center
    ) {
        GbaCard(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 580.dp)
                .testTag("result_summary_modal"),
            borderColor = if (result.medalUnlocked) StarYellow else if (result.victory) EmeraldSuccess else WarmAmber
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = when {
                    result.medalUnlocked -> "Medaglia Conquistata!"
                    result.victory -> "Sfida Completata!"
                    else -> "Ottimo Allenamento!"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = when {
                    result.medalUnlocked -> if (isDark) StarYellow else Color(0xFFB45309)
                    result.victory -> if (isDark) Color(0xFF34D399) else EmeraldSuccess
                    else -> if (isDark) StarYellow else WarmAmber
                }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = packTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Al primo colpo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${result.correctFirstTry} / ${result.totalQuestions}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(1.5.dp, StarYellow)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "+$displayedStars",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (result.priorityBonusApplied) {
                            Text(
                                text = "x1.5 Verifica!",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDark) StarYellow else WarmAmber
                            )
                        } else {
                            Text("Stelle vinte", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            val beforePct = result.masteryBefore.toInt().coerceIn(0, 100)
            val afterPct = result.masteryAfter.toInt().coerceIn(0, 100)
            Text(
                text = "Padronanza: $beforePct% -> $afterPct%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (afterPct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = EmeraldSuccess,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (result.medalUnlocked) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(2.dp, StarYellow, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = StarYellow, modifier = Modifier.size(32.dp))
                    Text(
                        text = "Nuova medaglia in bacheca: $medalName!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (result.suggestMicroBreak) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .border(2.dp, EmeraldSuccess, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .testTag("micro_break_card")
                ) {
                    Text(
                        text = "Hai fatto 2 sfide di fila, grande! Prenditi 2 minuti di pausa per muoverti e bere un bicchiere d'acqua, poi torna quando vuoi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("result_continue_button")
            ) {
                Text("Torna a scuola!", style = MaterialTheme.typography.titleMedium)
            }
            }
        }
        PixelConfettiOverlay(
            active = result.victory || result.medalUnlocked,
            reducedMotion = reducedMotion
        )
    }
}

@Composable
fun PackPickerModal(
    title: String,
    subtitle: String,
    packs: List<PackData>,
    masteryProvider: (String) -> Float,
    hasMedalProvider: (String) -> Boolean,
    focusTopicsProvider: (String) -> List<String> = { emptyList() },
    onSelectPack: (pack: PackData, isBoss: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val bannerSubject = packs.firstOrNull()?.subject ?: ""
    val bannerRes = subjectBannerDrawable(bannerSubject)
    val bannerTint = GameBalance.subjectColor(bannerSubject)

    val isDark = LocalDarkTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackdropScrimDark),
        contentAlignment = Alignment.Center
    ) {
        GbaCard(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 640.dp)
                .heightIn(max = 620.dp)
                .testTag("pack_picker_modal"),
            borderColor = bannerTint
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            ) {
                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xE00F172A), Color(0xAC0F172A), Color(0x550F172A))
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
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = StarYellow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IceWhiteText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                            .testTag("pack_picker_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(packs, key = { it.packId }) { pack ->
                    val mastery = masteryProvider(pack.packId)
                    val masteryPct = mastery.toInt().coerceIn(0, 100)
                    val bossUnlocked = mastery >= GameBalance.BOSS_UNLOCK_MASTERY_PCT
                    val medalWon = hasMedalProvider(pack.packId)
                    val subjColor = GameBalance.subjectColor(pack.subject)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(2.dp, if (pack.priority) WarmAmber else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(subjColor)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = GameBalance.subjectLabel(pack.subject),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = IceWhiteText
                                    )
                                }
                                if (pack.priority) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(WarmAmber)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "VERIFICA! (x1.5)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IceWhiteText
                                        )
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                if (medalWon) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Medaglia conquistata",
                                        tint = StarYellow,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = pack.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val focusedTopics = focusTopicsProvider(pack.packId)
                            if (focusedTopics.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Focus argomenti: ${focusedTopics.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { masteryPct / 100f },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = subjColor,
                                    trackColor = if (isDark) DarkCardBg else Color(0xFFE2E8F0)
                                )
                                Text(
                                    text = "$masteryPct%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onSelectPack(pack, false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp)
                                        .testTag("train_pack_${pack.packId}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Allenati", style = MaterialTheme.typography.labelLarge)
                                }
                                Button(
                                    onClick = { onSelectPack(pack, true) },
                                    enabled = bossUnlocked,
                                    modifier = Modifier
                                        .weight(1.25f)
                                        .heightIn(min = 52.dp)
                                        .testTag("boss_pack_${pack.packId}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StarYellow,
                                        contentColor = Color(0xFF451A03),
                                        disabledContainerColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    if (!bossUnlocked) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (medalWon) "Rivincita Boss" else "Sfida Boss: ${pack.bossName}",
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!bossUnlocked) {
                                            Text(
                                                text = "Raggiungi 80% per sbloccare",
                                                fontSize = 11.sp
                                            )
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

@Composable
fun PinModal(
    mode: PinMode,
    showDefaultPinHint: Boolean,
    onVerifyPin: (String) -> Boolean,
    onSetNewPin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var digits by remember { mutableStateOf("") }
    var firstNewPin by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val titleText = when {
        mode == PinMode.ENTER_PARENT_AREA -> "Accesso Area Genitori"
        firstNewPin == null -> "Scegli nuovo PIN (4 cifre)"
        else -> "Ripeti il nuovo PIN"
    }

    fun submitDigit(d: String) {
        errorMsg = null
        if (digits.length >= 4) return
        val next = digits + d
        digits = next
        if (next.length == 4) {
            if (mode == PinMode.ENTER_PARENT_AREA) {
                val ok = onVerifyPin(next)
                if (!ok) {
                    errorMsg = "PIN errato, riprova."
                    digits = ""
                }
            } else {
                if (firstNewPin == null) {
                    firstNewPin = next
                    digits = ""
                } else if (firstNewPin == next) {
                    onSetNewPin(next)
                } else {
                    errorMsg = "I due PIN non corrispondono. Riprova."
                    firstNewPin = null
                    digits = ""
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackdropScrimDark),
        contentAlignment = Alignment.Center
    ) {
        GbaCard(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 380.dp)
                .testTag("pin_modal")
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            if (mode == PinMode.ENTER_PARENT_AREA && showDefaultPinHint) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "PIN iniziale: 1234 (modificabile nelle Impostazioni)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(14.dp))

            // 4 masked dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
            ) {
                repeat(4) { idx ->
                    val filled = idx < digits.length
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (filled) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMsg!!,
                    style = MaterialTheme.typography.labelLarge,
                    color = WarmAmber,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            // Custom on-screen numeric keypad (ZERO Android keyboard)
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("CANCEL", "0", "BACK")
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "CANCEL" -> OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .testTag("pin_cancel_button"),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Text("Annulla", style = MaterialTheme.typography.labelMedium)
                                }
                                "BACK" -> OutlinedButton(
                                    onClick = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .testTag("pin_backspace_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Cancella cifra")
                                }
                                else -> Button(
                                    onClick = { submitDigit(key) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .testTag("pin_key_$key"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(key, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun RetroNumpad(
    value: String,
    unit: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var emptyPulse by remember { mutableStateOf(false) }
    val pulseScale by animateFloatAsState(
        targetValue = if (emptyPulse) 1.04f else 1.0f,
        animationSpec = tween(120),
        label = "numpad_pulse"
    )
    val boxBorder by animateColorAsState(
        targetValue = if (emptyPulse) WarmAmber else GameBalance.GBA_FRAME_DARK,
        animationSpec = tween(150),
        label = "numpad_border"
    )

    LaunchedEffect(emptyPulse) {
        if (emptyPulse) {
            delay(200)
            emptyPulse = false
        }
    }

    fun appendChar(ch: String) {
        if (!enabled) return
        if (value.length >= GameBalance.NUMPAD_MAX_CHARS) return
        if (ch == "," && value.contains(",")) return
        onValueChange(value + ch)
    }

    fun trySubmit() {
        if (!enabled) return
        val clean = value.trim()
        if (clean.isEmpty() || clean == "," || clean == ".") {
            emptyPulse = true
            return
        }
        onSubmit(clean)
    }

    Column(
        modifier = modifier.testTag("retro_numpad"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Input readout box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(pulseScale)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(2.5.dp, boxBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value.ifEmpty { "Scrivi qui..." },
                style = MaterialTheme.typography.headlineSmall,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("numpad_display")
            )
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Exact 4x4 grid per Section 9.5
        val keyHeight: Dp = 60.dp
        val rows = listOf(
            listOf("7", "8", "9", "BS"),
            listOf("4", "5", "6", "C"),
            listOf("1", "2", "3", ",")
        )
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isAction = key == "BS" || key == "C"
                    Button(
                        onClick = {
                            when (key) {
                                "BS" -> if (enabled && value.isNotEmpty()) onValueChange(value.dropLast(1))
                                "C" -> if (enabled) onValueChange("")
                                else -> appendChar(key)
                            }
                        },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight)
                            .testTag("numpad_key_$key"),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAction) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isAction) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        if (key == "BS") {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Cancella")
                        } else {
                            Text(key, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        // Row 4: 0 (span 2) | OK (span 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { appendChar("0") },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight)
                    .testTag("numpad_key_0"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text("0", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { trySubmit() },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight)
                    .testTag("numpad_key_OK"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = IceWhiteText),
                border = BorderStroke(2.dp, Color(0xFF064E3B))
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("OK", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VirtualDpad(
    onStep: (dx: Int, dy: Int) -> Unit,
    isTablet: Boolean = false,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val btnSize = if (isTablet) 56.dp else 52.dp
    val gap = if (isTablet) 6.dp else 5.dp
    val upInteraction = remember { MutableInteractionSource() }
    val leftInteraction = remember { MutableInteractionSource() }
    val rightInteraction = remember { MutableInteractionSource() }
    val downInteraction = remember { MutableInteractionSource() }
    val upPressed by upInteraction.collectIsPressedAsState()
    val leftPressed by leftInteraction.collectIsPressedAsState()
    val rightPressed by rightInteraction.collectIsPressedAsState()
    val downPressed by downInteraction.collectIsPressedAsState()
    val anyPressed = upPressed || leftPressed || rightPressed || downPressed
    val dpadAlpha by animateFloatAsState(
        targetValue = if (anyPressed) 1f else 0.75f,
        animationSpec = tween(durationMillis = if (reducedMotion) 40 else 140),
        label = "dpad_alpha"
    )

    Column(
        modifier = modifier
            .alpha(dpadAlpha)
            .testTag("virtual_dpad"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        DpadButton(
            icon = Icons.Default.KeyboardArrowUp,
            description = "Su",
            size = btnSize,
            interactionSource = upInteraction,
            pressed = upPressed,
            reducedMotion = reducedMotion,
            onClick = { onStep(0, -1) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            DpadButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                description = "Sinistra",
                size = btnSize,
                interactionSource = leftInteraction,
                pressed = leftPressed,
                reducedMotion = reducedMotion,
                onClick = { onStep(-1, 0) }
            )
            Box(
                modifier = Modifier
                    .size(btnSize)
                    .clip(CircleShape)
                    .background(Color(0x990F172A))
                    .border(1.5.dp, DarkBorderSlate.copy(alpha = 0.7f), CircleShape)
            )
            DpadButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                description = "Destra",
                size = btnSize,
                interactionSource = rightInteraction,
                pressed = rightPressed,
                reducedMotion = reducedMotion,
                onClick = { onStep(1, 0) }
            )
        }
        DpadButton(
            icon = Icons.Default.KeyboardArrowDown,
            description = "Giù",
            size = btnSize,
            interactionSource = downInteraction,
            pressed = downPressed,
            reducedMotion = reducedMotion,
            onClick = { onStep(0, 1) }
        )
    }
}

@Composable
private fun DpadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    size: Dp,
    interactionSource: MutableInteractionSource,
    pressed: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit
) {
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.94f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "dpad_btn_scale"
    )
    Surface(
        modifier = Modifier
            .size(size)
            .scale(pressScale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (pressed) Color(0xEE1E293B) else Color(0xC70F172A),
        border = BorderStroke(2.dp, if (pressed) SkyHighlight else DarkBorderSlate)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (pressed) SkyHighlight else IceWhiteText,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
