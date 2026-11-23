package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Filter2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.GameBalance
import com.example.core.LearningEngine
import com.example.core.MinigameLaunchConfig
import com.example.core.MinigameResult
import com.example.core.QuestionData
import com.example.core.SfxType
import com.example.core.TextFormatter
import com.example.ui.theme.keywordHighlightColor
import com.example.pixelart.Facing
import com.example.pixelart.PixelArtAssets
import com.example.ui.components.ConfirmModal
import com.example.ui.components.ExplanationModal
import com.example.ui.components.FloatingJuicePopup
import com.example.ui.components.GbaCard
import com.example.ui.components.PixelSprite
import com.example.ui.components.ProgressStepper
import com.example.ui.components.ResultSummaryModal
import com.example.ui.components.StepDotState
import com.example.ui.theme.DeepBlueAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.SoftGold
import com.example.ui.theme.WarmAmber
import com.example.viewmodel.ScuolaRetroViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

private data class GeoCloudOption(
    val index: Int,
    val label: String,
    val isCorrect: Boolean
)

private data class GeoDeckItem(
    val question: QuestionData,
    val stepIdx: Int,
    val retryCount: Int = 0
)

/**
 * Aula di Geografia — "Volo in Mongolfiera" (Italia, Regioni, Monti, Fiumi e Mari).
 * Steer a 16-bit striped hot-air balloon over pixel Alps & sea into the right cloud-gate!
 * Supports direct Canvas tap on any cloud-gate OR bottom compass-altitude buttons.
 */
@Composable
fun GeoBalloonScreen(
    config: MinigameLaunchConfig,
    viewModel: ScuolaRetroViewModel
) {
    val pack = remember(config.packId) { viewModel.packManager.getPack(config.packId) }
    val prefs = viewModel.saveManager.config.studyPreferences
    val masteryBefore = remember(config.packId) {
        viewModel.learningEngine.getPackMasteryPercent(config.packId)
    }

    val initialQuestions = remember(config.packId, config.isBoss) {
        val count = viewModel.saveManager.getSessionLength() + if (config.isBoss) GameBalance.BOSS_EXTRA_QUESTIONS else 0
        viewModel.learningEngine.buildSessionDeck(
            packId = config.packId,
            count = count,
            allowedTypes = setOf("multiple_choice", "true_false")
        )
    }

    if (pack == null || initialQuestions.isEmpty()) {
        LaunchedEffect(Unit) { viewModel.returnFromMinigame(config) }
        return
    }

    val scope = rememberCoroutineScope()
    var queue by remember {
        mutableStateOf(initialQuestions.mapIndexed { idx, q -> GeoDeckItem(q, idx, 0) })
    }
    var stepStates by remember {
        mutableStateOf(
            List(initialQuestions.size) { i ->
                if (i == 0) StepDotState.CURRENT else StepDotState.PENDING
            }
        )
    }
    var currentQueuePos by remember { mutableIntStateOf(0) }
    var correctFirstTry by remember { mutableIntStateOf(0) }
    var helpsUsed by remember { mutableIntStateOf(0) }
    var rawAnswerStars by remember { mutableIntStateOf(0) }
    var milesFlown by remember { mutableIntStateOf(0) }
    var geoCombo by remember { mutableIntStateOf(0) }
    var juicePopupText by remember { mutableStateOf("") }
    var juicePopupColor by remember { mutableStateOf(EmeraldSuccess) }
    var juicePopupTrigger by remember { mutableIntStateOf(0) }

    var inputLocked by remember { mutableStateOf(false) }
    var readingPauseActive by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var usedHelpOnCurrent by remember { mutableStateOf(false) }
    var eliminatedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }
    var feedbackGood by remember { mutableStateOf(true) }

    // Balloon flight physics state
    var balloonYNorm by remember { mutableFloatStateOf(0.5f) } // 0..1 vertical altitude
    var targetYNorm by remember { mutableFloatStateOf(0.5f) }
    var balloonXNorm by remember { mutableFloatStateOf(0.16f) }
    var skyScroll by remember { mutableFloatStateOf(0f) }

    var showExplanation by remember { mutableStateOf(false) }
    var explanationHeader by remember { mutableStateOf("") }
    var explanationAnswer by remember { mutableStateOf("") }
    var explanationBody by remember { mutableStateOf("") }

    var showExitConfirm by remember { mutableStateOf(false) }
    var finalResult by remember { mutableStateOf<MinigameResult?>(null) }

    val currentItem = queue.getOrNull(currentQueuePos)
    val currentQuestion = currentItem?.question

    val cloudOptions = remember(currentQueuePos, currentQuestion?.id) {
        if (currentQuestion == null) {
            emptyList()
        } else if (currentQuestion.type == "true_false") {
            listOf(
                GeoCloudOption(0, "Vero", currentQuestion.correctBool),
                GeoCloudOption(1, "Falso", !currentQuestion.correctBool)
            )
        } else {
            val shuffled = viewModel.learningEngine.getShuffledOptions(currentQuestion)
            shuffled.options.mapIndexed { idx, label ->
                GeoCloudOption(idx, label, idx == shuffled.correctDisplayIndex)
            }
        }
    }

    LaunchedEffect(currentQueuePos) {
        showHint = false
        usedHelpOnCurrent = false
        eliminatedIndices = emptySet()
        feedbackBanner = null
        balloonXNorm = 0.16f
        targetYNorm = 0.5f
        if (currentItem != null) {
            stepStates = stepStates.mapIndexed { idx, st ->
                if (idx == currentItem.stepIdx && st != StepDotState.COMPLETED) StepDotState.CURRENT else st
            }
            if (prefs.autoTtsQuestions && currentQuestion != null) {
                viewModel.ttsManager.speak(currentQuestion.prompt)
            }
            if (prefs.readingPauseSec > 0.05f) {
                readingPauseActive = true
                delay((prefs.readingPauseSec * 1000L).toLong())
                readingPauseActive = false
            } else {
                readingPauseActive = false
            }
        }
    }

    // Smooth 60fps sky & balloon flight loop
    LaunchedEffect(prefs.calmMode, prefs.reducedMotion, showExplanation, finalResult != null, showExitConfirm) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    if (!showExplanation && finalResult == null && !showExitConfirm) {
                        val scrollSpeed = when {
                            prefs.reducedMotion -> 0.2f
                            prefs.calmMode -> 0.45f
                            else -> 1.0f
                        }
                        skyScroll += dt * scrollSpeed
                        balloonYNorm += (targetYNorm - balloonYNorm) * (dt * 9f).coerceAtMost(1f)
                    }
                }
                lastNanos = now
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.ttsManager.stop() }
    }

    BackHandler {
        if (finalResult == null) showExitConfirm = true
    }

    fun advanceOrFinish() {
        val nextPos = currentQueuePos + 1
        if (nextPos >= queue.size) {
            val victory = if (config.isBoss) {
                LearningEngine.isBossGymVictory(true, correctFirstTry, initialQuestions.size)
            } else {
                true
            }
            viewModel.audioManager.playSfx(if (victory) SfxType.VICTORY_FANFARE else SfxType.UI_CLICK)
            finalResult = viewModel.learningEngine.completeMinigameSession(
                packId = config.packId,
                minigameId = "geo_balloon",
                isBoss = config.isBoss,
                victory = victory,
                correctFirstTry = correctFirstTry,
                totalQuestions = initialQuestions.size,
                helpsUsed = helpsUsed,
                rawAnswerStars = rawAnswerStars,
                masteryBefore = masteryBefore
            )
            viewModel.refreshSnapshot()
        } else {
            currentQueuePos = nextPos
            inputLocked = false
        }
    }

    fun flyToCloud(slotIdx: Int) {
        val item = currentItem ?: return
        val q = currentQuestion ?: return
        if (inputLocked || readingPauseActive || showExplanation || finalResult != null) return
        if (slotIdx in eliminatedIndices) return
        val opt = cloudOptions.getOrNull(slotIdx) ?: return

        inputLocked = true
        viewModel.ttsManager.stop()

        // Steer balloon smoothly into chosen cloud altitude
        val laneCount = cloudOptions.size.coerceAtLeast(2)
        targetYNorm = ((slotIdx + 0.5f) / laneCount.toFloat()).coerceIn(0.14f, 0.86f)

        val isRetry = item.retryCount > 0
        viewModel.learningEngine.recordAnswer(
            questionId = q.id,
            isCorrect = opt.isCorrect,
            isRetryInSession = isRetry,
            usedHelp = usedHelpOnCurrent
        )

        scope.launch {
            // Brief dash forward toward the cloud gate
            val dashSteps = 8
            for (s in 1..dashSteps) {
                balloonXNorm = 0.16f + (0.48f * (s.toFloat() / dashSteps))
                delay(24)
            }
            if (opt.isCorrect) {
                viewModel.audioManager.playSfx(SfxType.ANSWER_CORRECT)
                milesFlown += 100
                geoCombo++
                feedbackGood = true
                feedbackBanner = "Rotta perfetta! +100 km di volo!"
                val gained = if (!isRetry) {
                    correctFirstTry++
                    GameBalance.STARS_PER_CORRECT + GameBalance.STARS_FIRST_TRY_BONUS
                } else {
                    GameBalance.STARS_PER_CORRECT
                }
                rawAnswerStars += gained
                juicePopupText = if (geoCombo >= 2) "COMBO x$geoCombo! +$gained ★" else "+$gained ★!"
                juicePopupColor = if (geoCombo >= 3) Color(0xFFD97706) else EmeraldSuccess
                juicePopupTrigger++
                stepStates = stepStates.mapIndexed { idx, st ->
                    if (idx == item.stepIdx) StepDotState.COMPLETED else st
                }
                delay(420)
                advanceOrFinish()
            } else {
                viewModel.audioManager.playSfx(SfxType.ANSWER_WRONG)
                geoCombo = 0
                feedbackGood = false
                feedbackBanner = "Vento contrario! Controlliamo la mappa!"
                val canRetry = item.retryCount < GameBalance.MAX_RETRIES_PER_QUESTION
                if (canRetry) {
                    stepStates = stepStates.mapIndexed { idx, st ->
                        if (idx == item.stepIdx) StepDotState.RETRY else st
                    }
                    queue = queue + item.copy(retryCount = item.retryCount + 1)
                } else {
                    stepStates = stepStates.mapIndexed { idx, st ->
                        if (idx == item.stepIdx) StepDotState.COMPLETED else st
                    }
                }
                delay(280)
                explanationHeader = if (canRetry) "Correggiamo la rotta! Riproveremo tra poco!" else "Ecco la meta giusta:"
                explanationAnswer = q.getCorrectAnswerDisplay()
                explanationBody = q.explanation
                showExplanation = true
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C4A6E))
            .safeDrawingPadding()
            .testTag("geo_balloon_screen")
    ) {
        val isShortLandscape = maxHeight < 520.dp && maxWidth > maxHeight
        val pad = if (isShortLandscape) 8.dp else 12.dp
        val vGap = if (isShortLandscape) 6.dp else 8.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            verticalArrangement = Arrangement.spacedBy(vGap)
        ) {
            // Top HUD bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { showExitConfirm = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("geo_exit_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1), contentColor = Color.White)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Esci")
                    if (!isShortLandscape) {
                        Spacer(Modifier.width(4.dp))
                        Text("Esci")
                    }
                }

                ProgressStepper(
                    states = stepStates,
                    currentIndex = currentItem?.stepIdx ?: 0
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0369A1),
                    border = BorderStroke(2.dp, SoftGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PixelSprite(
                            bitmap = PixelArtAssets.getCharacterSprite("npc_geo", Facing.DOWN, 0),
                            contentDescription = "Esploratore Marco",
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = if (config.isBoss) "BOSS · ${milesFlown}km" else "${milesFlown} km",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 11.sp,
                            color = SoftGold
                        )
                    }
                }
            }

            // Question + Helpers Card
            GbaCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFF0284C7),
                contentPadding = PaddingValues(
                    horizontal = if (isShortLandscape) 10.dp else 14.dp,
                    vertical = if (isShortLandscape) 6.dp else 10.dp
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (currentQuestion != null && currentQuestion.topic.isNotBlank()) {
                            Text(
                                text = "GEOGRAFIA · ${currentQuestion.topic.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF0284C7),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = TextFormatter.toAnnotatedString(highlightColor = keywordHighlightColor(), raw = currentQuestion?.prompt.orEmpty()),
                            style = if (isShortLandscape) {
                                MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, lineHeight = 19.sp)
                            } else {
                                MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 25.sp)
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (isShortLandscape) 2 else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("geo_question_prompt")
                        )
                    }

                    IconButton(
                        onClick = { currentQuestion?.let { viewModel.ttsManager.speak(it.prompt) } },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE0F2FE))
                            .testTag("geo_tts_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Ascolta domanda",
                            tint = Color(0xFF0369A1)
                        )
                    }

                    val hintText = currentQuestion?.getEffectiveHint().orEmpty()
                    if (hintText.isNotBlank()) {
                        Button(
                            onClick = {
                                if (!showHint) {
                                    showHint = true
                                    usedHelpOnCurrent = true
                                    helpsUsed++
                                }
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("geo_hint_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmAmber)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "Indizio", modifier = Modifier.size(18.dp))
                            if (!isShortLandscape) {
                                Spacer(Modifier.width(4.dp))
                                Text("Indizio")
                            }
                        }
                    }

                    if (prefs.enable5050 && cloudOptions.size == 4 && eliminatedIndices.isEmpty()) {
                        Button(
                            onClick = {
                                val correctIdx = cloudOptions.indexOfFirst { it.isCorrect }
                                if (correctIdx >= 0) {
                                    eliminatedIndices = LearningEngine.pickFiftyFiftyEliminations(4, correctIdx)
                                    usedHelpOnCurrent = true
                                    helpsUsed++
                                }
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("geo_5050_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlueAccent)
                        ) {
                            Icon(Icons.Default.Filter2, contentDescription = "50/50", modifier = Modifier.size(18.dp))
                            if (!isShortLandscape) {
                                Spacer(Modifier.width(4.dp))
                                Text("50/50")
                            }
                        }
                    }
                }

                if (showHint && currentQuestion != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Bussola: ${currentQuestion.getEffectiveHint()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF92400E),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("geo_hint_banner")
                    )
                }
            }

            // 16-bit Italian Sky & Hot-Air Balloon Arena Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(3.dp, Color(0xFF38BDF8), RoundedCornerShape(14.dp))
                    .testTag("geo_arena")
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(cloudOptions, eliminatedIndices, inputLocked, readingPauseActive) {
                            detectTapGestures { tap ->
                                if (inputLocked || readingPauseActive || cloudOptions.isEmpty()) return@detectTapGestures
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val laneIdx = ((tap.y / h) * cloudOptions.size).toInt().coerceIn(0, cloudOptions.size - 1)
                                if (laneIdx !in eliminatedIndices) {
                                    flyToCloud(laneIdx)
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Bright Mediterranean sky gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFBAE6FD))
                        )
                    )

                    // Warm sun on upper left
                    drawCircle(Color(0x44FEF08A), radius = 38f, center = Offset(w * 0.12f, h * 0.16f))
                    drawCircle(Color(0xFFFDE047), radius = 22f, center = Offset(w * 0.12f, h * 0.16f))

                    // Parallax snow-capped pixel Alps silhouettes along bottom
                    val mountainShift = (skyScroll * 28f) % (w * 0.4f).coerceAtLeast(1f)
                    for (m in -1..4) {
                        val baseX = m * (w * 0.28f) - mountainShift
                        val peakH = h * (0.28f + (m.mod(2)) * 0.08f)
                        val mPath = Path().apply {
                            moveTo(baseX, h)
                            lineTo(baseX + w * 0.15f, h - peakH)
                            lineTo(baseX + w * 0.30f, h)
                            close()
                        }
                        drawPath(mPath, color = Color(0xFF0F766E).copy(alpha = 0.55f))
                        // Snow cap
                        val snowPath = Path().apply {
                            moveTo(baseX + w * 0.15f, h - peakH)
                            lineTo(baseX + w * 0.11f, h - peakH * 0.72f)
                            lineTo(baseX + w * 0.19f, h - peakH * 0.72f)
                            close()
                        }
                        drawPath(snowPath, color = Color(0xFFF8FAFC).copy(alpha = 0.78f))
                    }

                    // Sparkling Mediterranean wave band at very bottom
                    drawRect(Color(0xFF0369A1), Offset(0f, h - 14f), Size(w, 14f))
                    drawRect(Color(0xFF7DD3FC), Offset(0f, h - 14f), Size(w, 3f))

                    // Draw numbered Cloud-Gates on right half
                    val laneCount = cloudOptions.size.coerceAtLeast(2)
                    val laneH = h / laneCount
                    val pillW = (w * 0.48f).coerceIn(130f, 360f)
                    val pillH = (laneH * 0.76f).coerceIn(32f, 64f)
                    val maxLeft = (w - pillW - 12f).coerceAtLeast(12f)

                    val cloudTextPaint = Paint().apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        textSize = (pillH * 0.44f).coerceIn(18f, 30f)
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val badgePaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = (pillH * 0.40f).coerceIn(16f, 24f)
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    cloudOptions.forEachIndexed { idx, opt ->
                        if (idx in eliminatedIndices) return@forEachIndexed
                        val waveX = if (prefs.calmMode || prefs.reducedMotion) {
                            0f
                        } else {
                            sin(skyScroll * 2.2f + idx * 1.4f).toFloat() * 10f
                        }
                        val left = (w * 0.46f + waveX).coerceIn(12f, maxLeft)
                        val maxTop = (h - pillH - 6f).coerceAtLeast(6f)
                        val top = (laneH * idx + (laneH - pillH) / 2f).coerceIn(6f, maxTop)
                        val cy = top + pillH / 2f

                        // Fluffy cloud puffs behind banner
                        drawCircle(Color.White.copy(alpha = 0.92f), radius = pillH * 0.44f, center = Offset(left + 12f, cy))
                        drawCircle(Color.White.copy(alpha = 0.92f), radius = pillH * 0.44f, center = Offset(left + pillW - 12f, cy))
                        drawRoundRect(
                            color = Color(0xFFF8FAFC),
                            topLeft = Offset(left, top),
                            size = Size(pillW, pillH),
                            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
                        )
                        drawRoundRect(
                            color = Color(0xFF0284C7),
                            topLeft = Offset(left, top),
                            size = Size(pillW, pillH),
                            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )

                        // Number badge + option text
                        val badgeR = (pillH * 0.34f).coerceIn(11f, 18f)
                        drawCircle(Color(0xFF0284C7), radius = badgeR, center = Offset(left + badgeR + 8f, cy))
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                "${idx + 1}",
                                left + badgeR + 8f,
                                cy + badgePaint.textSize * 0.35f,
                                badgePaint
                            )
                            val fitted = if (opt.label.length > 22) opt.label.take(20) + "…" else opt.label
                            canvas.nativeCanvas.drawText(
                                fitted,
                                left + pillW * 0.54f,
                                cy + cloudTextPaint.textSize * 0.34f,
                                cloudTextPaint
                            )
                        }
                    }

                    // 16-bit Colorful Hot-Air Balloon on left
                    val bx = (w * balloonXNorm).coerceIn(36f, (w - 36f).coerceAtLeast(36f))
                    val bob = sin(skyScroll * 3.5f).toFloat() * (if (prefs.reducedMotion) 1.5f else 5f)
                    val by = (h * balloonYNorm + bob).coerceIn(28f, (h - 32f).coerceAtLeast(28f))
                    val br = (h * 0.14f).coerceIn(20f, 34f)

                    // Wind trail ribbons behind balloon
                    for (t in 1..3) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.55f - t * 0.12f),
                            start = Offset(bx - br - t * 14f, by - 4f + t * 4f),
                            end = Offset(bx - br - t * 28f, by - 4f + t * 4f),
                            strokeWidth = 3f
                        )
                    }
                    // Wicker basket & ropes
                    drawLine(Color(0xFF78350F), Offset(bx - 7f, by + br * 0.8f), Offset(bx - 5f, by + br + 10f), 2.5f)
                    drawLine(Color(0xFF78350F), Offset(bx + 7f, by + br * 0.8f), Offset(bx + 5f, by + br + 10f), 2.5f)
                    drawRoundRect(
                        color = Color(0xFFB45309),
                        topLeft = Offset(bx - 9f, by + br + 8f),
                        size = Size(18f, 12f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    // Striped balloon envelope
                    drawCircle(Color(0xFFDC2626), radius = br, center = Offset(bx, by))
                    drawOval(
                        color = Color(0xFFFACC15),
                        topLeft = Offset(bx - br * 0.55f, by - br),
                        size = Size(br * 1.1f, br * 2f)
                    )
                    drawOval(
                        color = Color(0xFF2563EB),
                        topLeft = Offset(bx - br * 0.22f, by - br),
                        size = Size(br * 0.44f, br * 2f)
                    )
                    drawCircle(Color.White.copy(alpha = 0.35f), radius = br * 0.26f, center = Offset(bx - br * 0.34f, by - br * 0.34f))
                }

                feedbackBanner?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (feedbackGood) EmeraldSuccess else WarmAmber,
                        border = BorderStroke(2.dp, Color.White)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Bottom quick-route buttons (48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cloudOptions.forEachIndexed { idx, opt ->
                    val eliminated = idx in eliminatedIndices
                    Button(
                        onClick = { flyToCloud(idx) },
                        enabled = !inputLocked && !readingPauseActive && !eliminated,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("geo_cloud_$idx"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (eliminated) Color(0xFF475569) else Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(2.dp, if (eliminated) Color(0xFF64748B) else SoftGold)
                    ) {
                        Text(
                            text = if (eliminated) "—" else "${idx + 1}. ${opt.label}",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (showExplanation) {
            ExplanationModal(
                header = explanationHeader,
                correctAnswerDisplay = explanationAnswer,
                explanation = explanationBody,
                autoRead = prefs.autoTtsQuestions,
                onSpeak = { viewModel.ttsManager.speak(it) },
                onStopSpeak = { viewModel.ttsManager.stop() },
                onContinue = {
                    showExplanation = false
                    advanceOrFinish()
                }
            )
        }

        FloatingJuicePopup(
            text = juicePopupText,
            accentColor = juicePopupColor,
            triggerId = juicePopupTrigger,
            reducedMotion = prefs.reducedMotion,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 86.dp)
        )

        finalResult?.let { res ->
            ResultSummaryModal(
                result = res,
                packTitle = pack.title,
                medalName = pack.medalName,
                reducedMotion = prefs.reducedMotion,
                onContinue = { viewModel.returnFromMinigame(config) }
            )
        }

        if (showExitConfirm) {
            ConfirmModal(
                title = "Atterrare ora?",
                message = "Le stelle già guadagnate in questo volo verranno salvate.",
                confirmLabel = "Esci",
                onConfirm = {
                    showExitConfirm = false
                    viewModel.learningEngine.awardEarlyExitStars(config.packId, rawAnswerStars)
                    viewModel.refreshSnapshot()
                    viewModel.returnFromMinigame(config)
                },
                onCancel = { showExitConfirm = false }
            )
        }
    }
}
