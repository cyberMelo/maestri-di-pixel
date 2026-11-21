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
import com.example.pixelart.Facing
import com.example.pixelart.PixelArtAssets
import com.example.ui.components.ConfirmModal
import com.example.ui.components.ExplanationModal
import com.example.ui.components.GbaCard
import com.example.ui.components.PixelSprite
import com.example.ui.components.ProgressStepper
import com.example.ui.components.ResultSummaryModal
import com.example.ui.components.StepDotState
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.DeepBlueAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.SoftGold
import com.example.ui.theme.WarmAmber
import com.example.viewmodel.ScuolaRetroViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class LibraryBookSlot(
    val index: Int,
    val label: String,
    val isCorrect: Boolean,
    val coverColor: Int,
    val spineColor: Int
)

private data class LibraryDeckItem(
    val question: QuestionData,
    val stepIdx: Int,
    val retryCount: Int = 0
)

/**
 * Biblioteca — "Caccia ai Libri Volanti" (Italiano: Grammatica, Verbi, Ortografia).
 * 16-bit cozy library with winged floating books gliding across wooden shelves.
 * Tap a flying book directly on the Canvas or use the quick-catch buttons below.
 * Supports Ritmo Calmo (books hover gently in place), Indizio, 50/50, and TTS.
 */
@Composable
fun LibraryBooksScreen(
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
        mutableStateOf(initialQuestions.mapIndexed { idx, q -> LibraryDeckItem(q, idx, 0) })
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
    var booksCaught by remember { mutableIntStateOf(0) }

    var inputLocked by remember { mutableStateOf(false) }
    var readingPauseActive by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var usedHelpOnCurrent by remember { mutableStateOf(false) }
    var eliminatedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }
    var caughtSlotIdx by remember { mutableIntStateOf(-1) }
    var burstProgress by remember { mutableFloatStateOf(0f) } // 0..1 sparkle burst
    var burstIsGood by remember { mutableStateOf(true) }

    var showExplanation by remember { mutableStateOf(false) }
    var explanationHeader by remember { mutableStateOf("") }
    var explanationAnswer by remember { mutableStateOf("") }
    var explanationBody by remember { mutableStateOf("") }

    var showExitConfirm by remember { mutableStateOf(false) }
    var finalResult by remember { mutableStateOf<MinigameResult?>(null) }

    // Continuous smooth animation clock for hovering/gliding winged books
    var animTime by remember { mutableFloatStateOf(0f) }

    val currentItem = queue.getOrNull(currentQueuePos)
    val currentQuestion = currentItem?.question

    val bookSlots = remember(currentQueuePos, currentQuestion?.id) {
        if (currentQuestion == null) {
            emptyList()
        } else if (currentQuestion.type == "true_false") {
            listOf(
                LibraryBookSlot(0, "Vero", currentQuestion.correctBool, 0xFF059669.toInt(), 0xFF065F46.toInt()),
                LibraryBookSlot(1, "Falso", !currentQuestion.correctBool, 0xFF7C3AED.toInt(), 0xFF4C1D95.toInt())
            )
        } else {
            val shuffled = viewModel.learningEngine.getShuffledOptions(currentQuestion)
            val covers = listOf(
                0xFF7C3AED.toInt() to 0xFF4C1D95.toInt(),
                0xFF0284C7.toInt() to 0xFF075985.toInt(),
                0xFFD97706.toInt() to 0xFF92400E.toInt(),
                0xFF059669.toInt() to 0xFF065F46.toInt()
            )
            shuffled.options.mapIndexed { idx, label ->
                val (cov, spine) = covers[idx % covers.size]
                LibraryBookSlot(idx, label, idx == shuffled.correctDisplayIndex, cov, spine)
            }
        }
    }

    LaunchedEffect(currentQueuePos) {
        showHint = false
        usedHelpOnCurrent = false
        eliminatedIndices = emptySet()
        feedbackBanner = null
        caughtSlotIdx = -1
        burstProgress = 0f
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

    // 60fps library book float/glide loop
    LaunchedEffect(prefs.calmMode, prefs.reducedMotion, showExplanation, finalResult != null, showExitConfirm) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    if (!showExplanation && finalResult == null && !showExitConfirm) {
                        val speedMult = when {
                            prefs.reducedMotion -> 0.25f
                            prefs.calmMode -> 0.45f
                            else -> 1.15f
                        }
                        animTime += dt * speedMult
                        if (burstProgress > 0f && burstProgress < 1f) {
                            burstProgress = (burstProgress + dt * 2.4f).coerceAtMost(1f)
                        }
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
                minigameId = "library_books",
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

    fun catchBook(slotIdx: Int) {
        val item = currentItem ?: return
        val q = currentQuestion ?: return
        if (inputLocked || readingPauseActive || showExplanation || finalResult != null) return
        if (slotIdx in eliminatedIndices) return
        val slot = bookSlots.getOrNull(slotIdx) ?: return

        inputLocked = true
        viewModel.ttsManager.stop()
        caughtSlotIdx = slotIdx
        burstIsGood = slot.isCorrect
        burstProgress = 0.01f

        val isRetry = item.retryCount > 0
        viewModel.learningEngine.recordAnswer(
            questionId = q.id,
            isCorrect = slot.isCorrect,
            isRetryInSession = isRetry,
            usedHelp = usedHelpOnCurrent
        )

        scope.launch {
            if (slot.isCorrect) {
                viewModel.audioManager.playSfx(SfxType.ANSWER_CORRECT)
                booksCaught++
                if (!isRetry) {
                    correctFirstTry++
                    rawAnswerStars += GameBalance.STARS_PER_CORRECT + GameBalance.STARS_FIRST_TRY_BONUS
                } else {
                    rawAnswerStars += GameBalance.STARS_PER_CORRECT
                }
                feedbackBanner = "Libro catturato! Risposta esatta!"
                stepStates = stepStates.mapIndexed { idx, st ->
                    if (idx == item.stepIdx) StepDotState.COMPLETED else st
                }
                delay(520)
                advanceOrFinish()
            } else {
                viewModel.audioManager.playSfx(SfxType.ANSWER_WRONG)
                feedbackBanner = "Ops! Quel libro era un altro argomento!"
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
                delay(320)
                explanationHeader = if (canRetry) "Quasi preso! Riproveremo tra poco!" else "Ecco il libro giusto:"
                explanationAnswer = q.getCorrectAnswerDisplay()
                explanationBody = q.explanation
                showExplanation = true
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E1065))
            .safeDrawingPadding()
            .testTag("library_books_screen")
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
                        .testTag("library_exit_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C1D95))
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
                    color = Color(0xFF4C1D95),
                    border = BorderStroke(2.dp, SoftGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PixelSprite(
                            bitmap = PixelArtAssets.getCharacterSprite("npc_library", Facing.DOWN, 0),
                            contentDescription = "Bibliotecaria Sofia",
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = if (config.isBoss) "BOSS · $booksCaught" else "Libri: $booksCaught",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 11.sp,
                            color = SoftGold
                        )
                    }
                }
            }

            // Question Prompt + Helpers Card (compact single row on phone landscape)
            GbaCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFF7C3AED),
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
                                text = "BIBLIOTECA · ${currentQuestion.topic.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF7C3AED),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = TextFormatter.toAnnotatedString(currentQuestion?.prompt.orEmpty()),
                            style = if (isShortLandscape) {
                                MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, lineHeight = 19.sp)
                            } else {
                                MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 25.sp)
                            },
                            color = InkPrimary,
                            maxLines = if (isShortLandscape) 2 else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("library_question_prompt")
                        )
                    }

                    IconButton(
                        onClick = { currentQuestion?.let { viewModel.ttsManager.speak(it.prompt) } },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEDE9FE))
                            .testTag("library_tts_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Ascolta domanda",
                            tint = Color(0xFF5B21B6)
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
                                .testTag("library_hint_button"),
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

                    if (prefs.enable5050 && bookSlots.size == 4 && eliminatedIndices.isEmpty()) {
                        Button(
                            onClick = {
                                val correctIdx = bookSlots.indexOfFirst { it.isCorrect }
                                if (correctIdx >= 0) {
                                    eliminatedIndices = LearningEngine.pickFiftyFiftyEliminations(4, correctIdx)
                                    usedHelpOnCurrent = true
                                    helpsUsed++
                                }
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("library_5050_button"),
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
                        text = "Indizio: ${currentQuestion.getEffectiveHint()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF92400E),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("library_hint_banner")
                    )
                }
            }

            // 16-bit Flying Books Library Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(3.dp, Color(0xFFF59E0B), RoundedCornerShape(14.dp))
                    .testTag("library_arena")
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(bookSlots, eliminatedIndices, inputLocked, readingPauseActive) {
                            detectTapGestures { tap ->
                                if (inputLocked || readingPauseActive || bookSlots.isEmpty()) return@detectTapGestures
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val cols = if (bookSlots.size == 2) 2 else 2
                                val rows = if (bookSlots.size == 2) 1 else 2
                                val col = ((tap.x / w) * cols).toInt().coerceIn(0, cols - 1)
                                val row = ((tap.y / h) * rows).toInt().coerceIn(0, rows - 1)
                                val tappedIdx = row * cols + col
                                if (tappedIdx in bookSlots.indices && tappedIdx !in eliminatedIndices) {
                                    catchBook(tappedIdx)
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Warm sunlit oak library backdrop
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF3B1D5E), Color(0xFF581C87), Color(0xFF451A03))
                        )
                    )

                    // Background wooden bookshelves
                    val shelfCount = 3
                    for (s in 0 until shelfCount) {
                        val sy = h * (0.24f + s * 0.28f)
                        drawRect(Color(0xFF78350F), Offset(0f, sy), Size(w, 8f))
                        drawRect(Color(0xFFB45309), Offset(0f, sy), Size(w, 3f))
                        // Decorative background book spines
                        val spineColors = listOf(Color(0xFF9333EA), Color(0xFF2563EB), Color(0xFF059669), Color(0xFFD97706), Color(0xFFDC2626))
                        var bx = 14f
                        var bIdx = s * 7
                        while (bx < w - 20f) {
                            val bw = 12f + (bIdx % 3) * 4f
                            val bh = 20f + (bIdx % 4) * 5f
                            drawRect(
                                color = spineColors[bIdx % spineColors.size].copy(alpha = 0.28f),
                                topLeft = Offset(bx, sy - bh),
                                size = Size(bw, bh)
                            )
                            bx += bw + 6f
                            bIdx++
                        }
                    }

                    // Floating golden dust motes
                    for (m in 0..9) {
                        val mx = ((m * 97f + animTime * 18f) % w)
                        val my = (h * (0.15f + (m % 5) * 0.17f)) + sin(animTime * 2f + m).toFloat() * 6f
                        drawCircle(Color(0x55FEF08A), radius = 3f, center = Offset(mx, my))
                    }

                    // Draw winged floating books (2x2 grid or 1x2 grid)
                    val cols = 2
                    val rows = if (bookSlots.size <= 2) 1 else 2
                    val cellW = w / cols
                    val cellH = h / rows

                    val textPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = (h * 0.095f).coerceIn(22f, 34f)
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val badgePaint = Paint().apply {
                        color = android.graphics.Color.rgb(69, 26, 3)
                        textSize = (h * 0.08f).coerceIn(18f, 26f)
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    bookSlots.forEachIndexed { idx, slot ->
                        if (idx in eliminatedIndices) return@forEachIndexed
                        val col = idx % cols
                        val row = idx / cols
                        val baseCx = cellW * (col + 0.5f)
                        val baseCy = cellH * (row + 0.5f)
                        val driftX = if (prefs.calmMode || prefs.reducedMotion) {
                            0f
                        } else {
                            sin(animTime * 1.4f + idx * 1.7f).toFloat() * (cellW * 0.09f)
                        }
                        val bobY = sin(animTime * 2.6f + idx * 1.3f).toFloat() * (if (prefs.reducedMotion) 2f else 6f)
                        val wingFlap = sin(animTime * 7.5f + idx).toFloat() * 7f

                        val bookW = (cellW * 0.78f).coerceIn(110f, 340f)
                        val bookH = (cellH * 0.62f).coerceIn(48f, 92f)
                        val maxLeft = (w - bookW - 10f).coerceAtLeast(10f)
                        val maxTop = (h - bookH - 8f).coerceAtLeast(8f)
                        val left = (baseCx - bookW / 2f + driftX).coerceIn(10f, maxLeft)
                        val top = (baseCy - bookH / 2f + bobY).coerceIn(8f, maxTop)
                        val cx = left + bookW / 2f
                        val cy = top + bookH / 2f

                        // Golden pixel wings on left & right of each flying book
                        val wingColor = Color(0xFFFEF08A)
                        val wingOutline = Color(0xFFF59E0B)
                        drawOval(
                            color = wingColor,
                            topLeft = Offset((left - 20f).coerceAtLeast(2f), cy - 12f - wingFlap),
                            size = Size(22f, 14f)
                        )
                        drawOval(
                            color = wingOutline,
                            topLeft = Offset((left - 18f).coerceAtLeast(2f), cy - 9f - wingFlap),
                            size = Size(16f, 8f)
                        )
                        drawOval(
                            color = wingColor,
                            topLeft = Offset((left + bookW - 2f).coerceAtMost(w - 22f), cy - 12f - wingFlap),
                            size = Size(22f, 14f)
                        )
                        drawOval(
                            color = wingOutline,
                            topLeft = Offset((left + bookW + 2f).coerceAtMost(w - 18f), cy - 9f - wingFlap),
                            size = Size(16f, 8f)
                        )

                        // Ground shadow + hardcover book body
                        drawRoundRect(
                            color = Color(0x55000000),
                            topLeft = Offset(left + 4f, top + 6f),
                            size = Size(bookW, bookH),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        drawRoundRect(
                            color = Color(slot.coverColor),
                            topLeft = Offset(left, top),
                            size = Size(bookW, bookH),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        // Spine band on left + golden pages strip on bottom
                        drawRoundRect(
                            color = Color(slot.spineColor),
                            topLeft = Offset(left, top),
                            size = Size(16f, bookH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        drawRect(
                            color = Color(0xFFFEF3C7),
                            topLeft = Offset(left + 16f, top + bookH - 7f),
                            size = Size((bookW - 22f).coerceAtLeast(10f), 4f)
                        )
                        // Subtle top bevel highlight
                        drawRect(
                            color = Color.White.copy(alpha = 0.25f),
                            topLeft = Offset(left + 18f, top + 3f),
                            size = Size((bookW - 26f).coerceAtLeast(10f), 4f)
                        )

                        // Number badge + answer text
                        val numLabel = "${idx + 1}"
                        drawCircle(SoftGold, radius = 14f, center = Offset(left + 26f, top + 20f))
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(numLabel, left + 26f, top + 26f, badgePaint)
                            val fitted = if (slot.label.length > 22) slot.label.take(20) + "…" else slot.label
                            canvas.nativeCanvas.drawText(fitted, cx + 8f, cy + textPaint.textSize * 0.34f, textPaint)
                        }

                        // Sparkle catch burst on chosen book
                        if (caughtSlotIdx == idx && burstProgress > 0f) {
                            val burstCol = if (burstIsGood) Color(0xFF4ADE80) else WarmAmber
                            for (p in 0 until 8) {
                                val angle = (p * PI / 4.0).toFloat()
                                val dist = burstProgress * 46f
                                drawCircle(
                                    color = burstCol.copy(alpha = (1f - burstProgress).coerceIn(0f, 1f)),
                                    radius = 7f * (1f - burstProgress * 0.4f),
                                    center = Offset(cx + cos(angle) * dist, cy + sin(angle) * dist)
                                )
                            }
                        }
                    }
                }

                // Feedback toast inside arena
                feedbackBanner?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (burstIsGood) EmeraldSuccess else WarmAmber,
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

            // Accessible quick-catch row at bottom (48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bookSlots.forEachIndexed { idx, slot ->
                    val eliminated = idx in eliminatedIndices
                    Button(
                        onClick = { catchBook(idx) },
                        enabled = !inputLocked && !readingPauseActive && !eliminated,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("library_book_$idx"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (eliminated) Color(0xFF475569) else Color(slot.coverColor),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(2.dp, if (eliminated) Color(0xFF64748B) else SoftGold)
                    ) {
                        Text(
                            text = if (eliminated) "—" else "${idx + 1}. ${slot.label}",
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

        finalResult?.let { res ->
            ResultSummaryModal(
                result = res,
                packTitle = pack.title,
                medalName = pack.medalName,
                onContinue = { viewModel.returnFromMinigame(config) }
            )
        }

        if (showExitConfirm) {
            ConfirmModal(
                title = "Uscire dalla Biblioteca?",
                message = "Le stelle già guadagnate in questa partita verranno salvate.",
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
