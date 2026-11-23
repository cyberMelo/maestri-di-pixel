package io.github.cybermelo.maestridipixel.ui.screens

import io.github.cybermelo.maestridipixel.ui.components.stageHeight
import io.github.cybermelo.maestridipixel.ui.components.RocketFuelPanel
import io.github.cybermelo.maestridipixel.ui.components.TeacherMood
import io.github.cybermelo.maestridipixel.ui.components.MinigameStage
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Filter2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Spa
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cybermelo.maestridipixel.core.GameBalance
import io.github.cybermelo.maestridipixel.core.LearningEngine
import io.github.cybermelo.maestridipixel.core.MinigameLaunchConfig
import io.github.cybermelo.maestridipixel.core.MinigameResult
import io.github.cybermelo.maestridipixel.core.QuestionData
import io.github.cybermelo.maestridipixel.core.SfxType
import io.github.cybermelo.maestridipixel.core.ShuffledOptions
import io.github.cybermelo.maestridipixel.core.TextFormatter
import io.github.cybermelo.maestridipixel.ui.theme.keywordHighlightColor
import io.github.cybermelo.maestridipixel.ui.components.ConfirmModal
import io.github.cybermelo.maestridipixel.ui.components.DialogBoxModal
import io.github.cybermelo.maestridipixel.ui.components.ExplanationModal
import io.github.cybermelo.maestridipixel.ui.components.GbaCard
import io.github.cybermelo.maestridipixel.ui.components.ProgressStepper
import io.github.cybermelo.maestridipixel.ui.components.FloatingJuicePopup
import io.github.cybermelo.maestridipixel.ui.components.ResultSummaryModal
import io.github.cybermelo.maestridipixel.ui.components.RetroNumpad
import io.github.cybermelo.maestridipixel.ui.components.StepDotState
import io.github.cybermelo.maestridipixel.ui.theme.CreamBackground
import io.github.cybermelo.maestridipixel.ui.theme.CreamSurface
import io.github.cybermelo.maestridipixel.ui.theme.DeepBlueAccent
import io.github.cybermelo.maestridipixel.ui.theme.EmeraldSuccess
import io.github.cybermelo.maestridipixel.ui.theme.InkPrimary
import io.github.cybermelo.maestridipixel.ui.theme.SilkscreenFontFamily
import io.github.cybermelo.maestridipixel.ui.theme.SoftGold
import io.github.cybermelo.maestridipixel.ui.theme.WarmAmber
import io.github.cybermelo.maestridipixel.viewmodel.ScuolaRetroViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private enum class GymPhase {
    QUESTION,
    EXPLANATION,
    SUMMARY
}

private data class GymQueuedQuestion(
    val question: QuestionData,
    val originalIndex: Int,
    val isRetry: Boolean = false
)

private data class StarDot(
    val x: Float,
    var y: Float,
    val speed: Float,
    val radius: Float,
    val alpha: Float
)

private data class LaserBolt(
    val xNorm: Float,
    val yNorm: Float,
    val targetIndex: Int,
    val isSuper: Boolean
)

private data class SpaceParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val life: Float
)

private val INVADER_COLORS = listOf(
    Color(0xFF38BDF8), // Cyan
    Color(0xFF4ADE80), // Lime
    Color(0xFFF472B6), // Pink
    Color(0xFFFACC15)  // Gold
)

// Multi-color 16-bit 11x8 pixel-art invader sprite frames (X=body, H=highlight, E=glowing eye)
private val ALIEN_FRAME_0 = arrayOf(
    "..H.....H..",
    "...X...X...",
    "..XXHHHXX..",
    ".XXEXXXEXX.",
    "XXXXXXXXXXX",
    "X.XHHHHHX.X",
    "X.X.....X.X",
    "...XX.XX..."
)

private val ALIEN_FRAME_1 = arrayOf(
    "..H.....H..",
    "X..X...X..X",
    "X.XXHHHXX.X",
    "XXXEXXXEXXX",
    "XXXXXXXXXXX",
    ".XXHHHHHXX.",
    "..X.....X..",
    ".X.......X."
)

private val SHIP_PIXELS = arrayOf(
    "....W....",
    "...WCW...",
    "...CBC...",
    "..BBHBB..",
    ".GGBBBGG.",
    "GGBBBBBGG",
    "G.B...B.G"
)

/**
 * Palestra Spaziale (Matematica & Tabelline):
 * Default mode is 16-bit Space Invaders Arcade (shoot the alien carrying the right result),
 * with a 1-tap toggle to classic RetroNumpad ("Tastierino") at any time.
 * Standard mode has a lively, balanced wave descent; Ritmo Calmo freezes descent & timers.
 */
@Composable
fun GymRhythmScreen(
    config: MinigameLaunchConfig,
    viewModel: ScuolaRetroViewModel
) {
    val scope = rememberCoroutineScope()
    val pack = remember(config.packId) { viewModel.packManager.getPack(config.packId) }
    val prefs = viewModel.saveManager.config.studyPreferences
    val masteryBefore = remember(config.packId) { viewModel.learningEngine.getPackMasteryPercent(config.packId) }

    val initialDeck = remember(config.packId, config.isBoss, prefs.sessionLength) {
        val target = prefs.sessionLength + if (config.isBoss) GameBalance.BOSS_EXTRA_QUESTIONS else 0
        viewModel.learningEngine.buildSessionDeck(
            packId = config.packId,
            count = target,
            allowedTypes = setOf("numeric_input", "multiple_choice", "true_false")
        )
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.ttsManager.stop() }
    }

    if (pack == null || initialDeck.isEmpty()) {
        DialogBoxModal(
            title = "Palestra Spaziale",
            message = "Questo argomento non ha domande pronte per questa aula!",
            onDismiss = { viewModel.returnFromMinigame(config) }
        )
        return
    }

    val queue = remember {
        mutableStateListOf<GymQueuedQuestion>().apply {
            initialDeck.forEachIndexed { idx, q -> add(GymQueuedQuestion(q, idx, false)) }
        }
    }
    val stepStates = remember {
        mutableStateListOf<StepDotState>().apply {
            repeat(initialDeck.size) { add(StepDotState.PENDING) }
        }
    }
    val retryCounts = remember { mutableMapOf<String, Int>() }
    val uniqueAnsweredIds = remember { mutableSetOf<String>() }

    var phase by remember { mutableStateOf(GymPhase.QUESTION) }
    var useKeypadMode by remember { mutableStateOf(false) }
    var currentEntry by remember { mutableStateOf<GymQueuedQuestion?>(null) }
    var arcadeChoices by remember { mutableStateOf<ShuffledOptions?>(null) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var questionToken by remember { mutableIntStateOf(0) }

    var numpadInput by remember { mutableStateOf("") }
    var isInputLocked by remember { mutableStateOf(false) }
    var readingPauseActive by remember { mutableStateOf(false) }
    var readingPauseProgress by remember { mutableFloatStateOf(1f) }
    var hintVisible by remember { mutableStateOf(false) }
    var eliminatedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var usedHelpThisQuestion by remember { mutableStateOf(false) }

    // Space Invaders live simulation state
    var shipXNorm by remember { mutableFloatStateOf(0.5f) }
    var shipTargetXNorm by remember { mutableFloatStateOf(0.5f) }
    var waveYNorm by remember { mutableFloatStateOf(0.18f) }
    var animTimeSec by remember { mutableFloatStateOf(0f) }
    var activeLaser by remember { mutableStateOf<LaserBolt?>(null) }
    var shieldFlash by remember { mutableFloatStateOf(0f) }
    var empWaveRadius by remember { mutableFloatStateOf(-1f) }
    var floatingFeedback by remember { mutableStateOf("") }
    val particles = remember { mutableStateListOf<SpaceParticle>() }
    val stars = remember {
        val rng = Random(42)
        List(38) {
            StarDot(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                speed = 0.03f + rng.nextFloat() * 0.09f,
                radius = if (rng.nextFloat() > 0.7f) 2.6f else 1.5f,
                alpha = 0.35f + rng.nextFloat() * 0.6f
            )
        }
    }

    var combo by remember { mutableIntStateOf(0) }
    var completedSteps by remember { mutableIntStateOf(0) }
    var correctFirstTry by remember { mutableIntStateOf(0) }
    var teacherMood by remember { mutableStateOf(TeacherMood.IDLE) }
    var moodTrigger by remember { mutableIntStateOf(0) }
    var stageCorrect by remember { mutableIntStateOf(0) }
    var helpsUsedTotal by remember { mutableIntStateOf(0) }
    var rawAnswerStars by remember { mutableIntStateOf(0) }
    var juicePopupText by remember { mutableStateOf("") }
    var juicePopupColor by remember { mutableStateOf(EmeraldSuccess) }
    var juicePopupTrigger by remember { mutableIntStateOf(0) }
    var explanationHeader by remember { mutableStateOf("") }
    var finalResult by remember { mutableStateOf<MinigameResult?>(null) }
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler {
        if (phase != GymPhase.SUMMARY) showExitConfirm = true
    }

    fun spawnExplosion(xNorm: Float, yNorm: Float, baseColor: Color, count: Int = 18) {
        val rng = Random.Default
        repeat(count) {
            val angle = rng.nextFloat() * (Math.PI * 2.0)
            val speed = 0.14f + rng.nextFloat() * 0.34f
            particles.add(
                SpaceParticle(
                    x = xNorm,
                    y = yNorm,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    color = if (it % 3 == 0) SoftGold else baseColor,
                    size = 4f + rng.nextFloat() * 5f,
                    life = 1f
                )
            )
        }
    }

    fun finishSession() {
        viewModel.ttsManager.stop()
        val totalUnique = uniqueAnsweredIds.size.coerceAtLeast(1)
        val victory = if (config.isBoss) {
            LearningEngine.isBossGymVictory(
                allStepsCompleted = true,
                correctFirstTry = correctFirstTry,
                totalUniqueQuestions = totalUnique
            )
        } else {
            true
        }
        val res = viewModel.learningEngine.completeMinigameSession(
            packId = config.packId,
            minigameId = "gym_rhythm",
            isBoss = config.isBoss,
            victory = victory,
            correctFirstTry = correctFirstTry,
            totalQuestions = totalUnique,
            helpsUsed = helpsUsedTotal,
            rawAnswerStars = rawAnswerStars,
            masteryBefore = masteryBefore
        )
        viewModel.audioManager.playSfx(
            when {
                res.medalUnlocked -> SfxType.MEDAL_UNLOCK
                res.victory -> SfxType.VICTORY_FANFARE
                else -> SfxType.UI_BACK
            }
        )
        viewModel.refreshSnapshot()
        finalResult = res
        phase = GymPhase.SUMMARY
    }

    fun buildArcadeOptionsFor(q: QuestionData): ShuffledOptions = when (q.type) {
        "true_false" -> ShuffledOptions(
            options = listOf("VERO", "FALSO"),
            correctDisplayIndex = if (q.correctBool) 0 else 1
        )
        "multiple_choice" -> viewModel.learningEngine.getShuffledOptions(q)
        else -> LearningEngine.generateNumericChoices(q)
    }

    fun loadNextQuestion() {
        viewModel.ttsManager.stop()
        if (queue.isEmpty()) {
            finishSession()
            return
        }
        val next = queue.removeAt(0)
        currentEntry = next
        currentStepIndex = next.originalIndex
        if (stepStates[next.originalIndex] == StepDotState.PENDING) {
            stepStates[next.originalIndex] = StepDotState.CURRENT
        }
        arcadeChoices = buildArcadeOptionsFor(next.question)
        numpadInput = ""
        hintVisible = false
        eliminatedIndices = emptySet()
        usedHelpThisQuestion = false
        isInputLocked = false
        activeLaser = null
        waveYNorm = if (prefs.calmMode) 0.24f else 0.16f
        floatingFeedback = ""
        questionToken++
        phase = GymPhase.QUESTION

        if (prefs.autoTtsQuestions) {
            viewModel.ttsManager.speak(next.question.prompt)
        }
    }

    LaunchedEffect(Unit) {
        loadNextQuestion()
    }

    // Reading pause effect (0s by default in lively mode; active when configured or in Studio Sereno)
    LaunchedEffect(phase, questionToken) {
        if (phase != GymPhase.QUESTION || currentEntry == null) return@LaunchedEffect
        val pauseSec = prefs.readingPauseSec
        if (pauseSec > 0.01f) {
            readingPauseActive = true
            readingPauseProgress = 0f
            val totalPauseMs = (pauseSec * 1000f).toLong()
            val startPause = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startPause
                readingPauseProgress = (elapsed.toFloat() / totalPauseMs).coerceIn(0f, 1f)
                if (elapsed >= totalPauseMs) break
                delay(30)
            }
            readingPauseActive = false
            readingPauseProgress = 1f
        } else {
            readingPauseActive = false
            readingPauseProgress = 1f
        }
    }

    fun resolveAnswer(isCorrect: Boolean, hitX: Float = 0.5f, hitY: Float = 0.3f) {
        val entry = currentEntry ?: return
        val q = entry.question
        uniqueAnsweredIds.add(q.id)
        viewModel.learningEngine.recordAnswer(
            questionId = q.id,
            isCorrect = isCorrect,
            isRetryInSession = entry.isRetry,
            usedHelp = usedHelpThisQuestion
        )
        if (usedHelpThisQuestion) helpsUsedTotal++

        teacherMood = if (isCorrect) TeacherMood.HAPPY else TeacherMood.SAD

        moodTrigger++

        if (isCorrect) stageCorrect++

        scope.launch {
            if (isCorrect) {
                stepStates[entry.originalIndex] = StepDotState.COMPLETED
                completedSteps++
                combo++
                if (!entry.isRetry) correctFirstTry++
                val fastBonus = !prefs.calmMode && waveYNorm < 0.42f
                val gained = GameBalance.STARS_PER_CORRECT +
                    if (!entry.isRetry && !usedHelpThisQuestion) GameBalance.STARS_FIRST_TRY_BONUS else 0
                rawAnswerStars += gained

                val superBoost = combo >= GameBalance.GYM_COMBO_SUPER_THRESHOLD
                juicePopupText = if (superBoost) "COMBO x$combo! +$gained ★" else "+$gained ★!"
                juicePopupColor = if (superBoost) SoftGold else EmeraldSuccess
                juicePopupTrigger++
                spawnExplosion(hitX, hitY, EmeraldSuccess, count = if (superBoost) 26 else 18)
                viewModel.audioManager.playSfx(SfxType.ALIEN_BOOM)
                viewModel.audioManager.playSfx(if (superBoost) SfxType.ATTACK_SUPER else SfxType.ANSWER_CORRECT)
                floatingFeedback = when {
                    superBoost -> "SUPER LASER x$combo!"
                    fastBonus -> "COLPO LAMPO!"
                    else -> "BERSAGLIO COLPITO!"
                }
                delay(380)
                isInputLocked = false
                loadNextQuestion()
            } else {
                combo = 0
                spawnExplosion(hitX, hitY, WarmAmber, count = 12)
                viewModel.audioManager.playSfx(SfxType.ANSWER_WRONG)

                val prevRetries = retryCounts[q.id] ?: 0
                val nextRetries = prevRetries + 1
                retryCounts[q.id] = nextRetries
                if (nextRetries < GameBalance.MAX_RETRIES_PER_QUESTION) {
                    stepStates[entry.originalIndex] = StepDotState.RETRY
                    val delaySlots = Random.nextInt(GameBalance.REQUEUE_DELAY_MIN, GameBalance.REQUEUE_DELAY_MAX + 1)
                    val insertPos = min(delaySlots, queue.size)
                    queue.add(insertPos, entry.copy(isRetry = true))
                } else {
                    stepStates[entry.originalIndex] = StepDotState.COMPLETED
                    completedSteps++
                }
                explanationHeader = GameBalance.ENCOURAGING_ERROR_HEADERS[
                    (entry.originalIndex + nextRetries) % GameBalance.ENCOURAGING_ERROR_HEADERS.size
                ]
                isInputLocked = false
                phase = GymPhase.EXPLANATION
            }
        }
    }

    fun laneCenterX(index: Int, count: Int, sway: Float): Float {
        val base = if (count <= 2) {
            if (index == 0) 0.28f else 0.72f
        } else {
            (index + 0.5f) / count.toFloat()
        }
        return (base + sway).coerceIn(0.10f, 0.90f)
    }

    fun currentSway(): Float {
        if (prefs.reducedMotion) return 0f
        val amp = if (prefs.calmMode) 0.015f else 0.032f
        val freq = if (prefs.calmMode) 1.3f else 2.2f
        return sin(animTimeSec * freq) * amp
    }

    fun fireAtLane(laneIndex: Int) {
        val choices = arcadeChoices ?: return
        if (isInputLocked || readingPauseActive || phase != GymPhase.QUESTION) return
        if (laneIndex !in choices.options.indices || laneIndex in eliminatedIndices) return

        isInputLocked = true
        viewModel.ttsManager.stop()
        val tx = laneCenterX(laneIndex, choices.options.size, currentSway())
        shipTargetXNorm = tx
        shipXNorm = tx
        val isSuper = combo >= GameBalance.GYM_COMBO_SUPER_THRESHOLD
        viewModel.audioManager.playSfx(SfxType.LASER_SHOT)
        activeLaser = LaserBolt(
            xNorm = tx,
            yNorm = 0.84f,
            targetIndex = laneIndex,
            isSuper = isSuper
        )
    }

    // 60fps Space Invaders animation loop
    LaunchedEffect(useKeypadMode, phase, questionToken, prefs.calmMode, prefs.reducedMotion) {
        if (useKeypadMode) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) 0.016f else ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = frameNanos
                animTimeSec += dt

                // Scroll starfield
                val starMult = if (prefs.reducedMotion) 0.25f else 1f
                for (s in stars) {
                    s.y += s.speed * dt * starMult
                    if (s.y > 1f) s.y -= 1f
                }

                // Smooth ship movement
                shipXNorm += (shipTargetXNorm - shipXNorm) * min(1f, dt * 16f)

                // Shield flash decay
                if (shieldFlash > 0f) shieldFlash = (shieldFlash - dt * 2.2f).coerceAtLeast(0f)

                // EMP ring expansion
                if (empWaveRadius >= 0f) {
                    empWaveRadius += dt * 1.9f
                    if (empWaveRadius > 1.25f) empWaveRadius = -1f
                }

                // Particle updates
                var i = particles.size - 1
                while (i >= 0) {
                    val p = particles[i]
                    val nextLife = p.life - dt * 2.1f
                    if (nextLife <= 0f) {
                        particles.removeAt(i)
                    } else {
                        particles[i] = p.copy(
                            x = p.x + p.vx * dt,
                            y = p.y + p.vy * dt,
                            life = nextLife
                        )
                    }
                    i--
                }

                // Wave descent in lively standard mode (completely paused in Ritmo Calmo or modals/hints)
                val shouldDescend = phase == GymPhase.QUESTION &&
                    !prefs.calmMode &&
                    !readingPauseActive &&
                    !isInputLocked &&
                    !hintVisible &&
                    !showExitConfirm

                if (shouldDescend) {
                    val baseSec = if (config.isBoss) {
                        GameBalance.INVADERS_DESCENT_SEC_BOSS
                    } else {
                        GameBalance.INVADERS_DESCENT_SEC_STANDARD
                    }
                    val comboBoost = 1f + min(combo, 4) * 0.05f
                    val travelDist = 0.72f - 0.16f
                    waveYNorm += (travelDist / baseSec) * comboBoost * dt
                    if (waveYNorm >= 0.72f) {
                        // Shield deflects wave back up gently (keeps excitement without unfair loss)
                        waveYNorm = 0.16f
                        shieldFlash = 1f
                        combo = 0
                        floatingFeedback = "Scudo attivo! Nuova ondata!"
                        viewModel.audioManager.playSfx(SfxType.UI_BACK)
                    }
                }

                // Laser bolt travel & hit check
                val bolt = activeLaser
                if (bolt != null) {
                    val nextY = bolt.yNorm - dt * 3.8f
                    if (nextY <= waveYNorm + 0.05f) {
                        activeLaser = null
                        val choices = arcadeChoices
                        if (choices != null) {
                            val correct = bolt.targetIndex == choices.correctDisplayIndex
                            resolveAnswer(correct, bolt.xNorm, waveYNorm)
                        } else {
                            isInputLocked = false
                        }
                    } else {
                        activeLaser = bolt.copy(yNorm = nextY)
                    }
                }
            }
        }
    }

    val latestChoices by rememberUpdatedState(arcadeChoices)
    val latestEliminated by rememberUpdatedState(eliminatedIndices)
    val latestFireAtLane by rememberUpdatedState(::fireAtLane)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B1026), Color(0xFF1E1B4B))
                )
            )
            .safeDrawingPadding()
            .testTag("gym_rhythm_screen")
    ) {
        val isWide = maxWidth >= 760.dp
        val isShortLandscape = maxHeight < 520.dp && maxWidth > maxHeight
        val screenH = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = if (isShortLandscape) 4.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (isShortLandscape) 4.dp else 8.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showExitConfirm = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("gym_exit_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CreamBackground)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Esci", tint = InkPrimary)
                    if (isWide) {
                        Spacer(Modifier.width(4.dp))
                        Text("Esci", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                    }
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ProgressStepper(states = stepStates, currentIndex = currentStepIndex)
                }

                // Mode Switch: Space Invaders <-> Tastierino
                OutlinedButton(
                    onClick = {
                        viewModel.audioManager.playSfx(SfxType.UI_CLICK)
                        useKeypadMode = !useKeypadMode
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("gym_mode_toggle_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (useKeypadMode) Color(0xFFDBEAFE) else Color(0xFF312E81),
                        contentColor = if (useKeypadMode) DeepBlueAccent else Color.White
                    ),
                    border = BorderStroke(2.dp, if (useKeypadMode) DeepBlueAccent else Color(0xFF818CF8))
                ) {
                    Icon(
                        imageVector = if (useKeypadMode) Icons.Default.RocketLaunch else Icons.Default.Dialpad,
                        contentDescription = if (useKeypadMode) "Modalità Space Invaders" else "Modalità Tastierino",
                        modifier = Modifier.size(18.dp)
                    )
                    if (isWide) {
                        Spacer(Modifier.width(4.dp))
                        Text(if (useKeypadMode) "Space" else "Tastierino", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Combo / Ritmo Calmo badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (prefs.calmMode) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                    border = BorderStroke(2.dp, if (prefs.calmMode) EmeraldSuccess else WarmAmber)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (prefs.calmMode) Icons.Default.Spa else Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (prefs.calmMode) EmeraldSuccess else WarmAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (prefs.calmMode) "Calmo" else "x$combo",
                            style = MaterialTheme.typography.labelLarge,
                            color = InkPrimary
                        )
                    }
                }
            }

            val entry = currentEntry
            val choices = arcadeChoices
            if (entry != null && choices != null) {
                val q = entry.question
                val hintText = q.getEffectiveHint()
                val show5050 = prefs.enable5050 && choices.options.size >= 4 && !useKeypadMode
                val controlsEnabled = !readingPauseActive && !isInputLocked

                // Compact Question Header Card (Focus Mode centered max 680dp)
                MinigameStage(
                    roomId = null,
                    teacherSpriteId = "npc_math",
                    teacherName = "Coach Pitagora",
                    accent = Color(0xFFDC2626),
                    mood = teacherMood,
                    moodTrigger = moodTrigger,
                    reducedMotion = prefs.reducedMotion,
                    compact = isShortLandscape,
                    expandedHeight = stageHeight(screenH, 220.dp),
                    backdrop = spaceBackdrop,
                    side = if (isShortLandscape) null else {
                        { RocketFuelPanel(collected = stageCorrect.coerceAtMost(initialDeck.size), total = initialDeck.size) }
                    }
                ) {
                    val helperControls: @Composable () -> Unit = {
                        IconButton(
                            onClick = { viewModel.ttsManager.speak(q.prompt) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .testTag("gym_tts_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Ascolta domanda",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (hintText.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    if (!hintVisible) {
                                        viewModel.audioManager.playSfx(SfxType.UI_CLICK)
                                        hintVisible = true
                                        usedHelpThisQuestion = true
                                    }
                                },
                                enabled = !hintVisible,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("gym_hint_button"),
                                contentPadding = PaddingValues(horizontal = if (isShortLandscape) 10.dp else 14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text("Indizio", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        if (show5050) {
                            OutlinedButton(
                                onClick = {
                                    if (eliminatedIndices.isEmpty() && !readingPauseActive) {
                                        viewModel.audioManager.playSfx(SfxType.ALIEN_BOOM)
                                        val elim = LearningEngine.pickFiftyFiftyEliminations(
                                            optionCount = choices.options.size,
                                            correctDisplayIndex = choices.correctDisplayIndex
                                        )
                                        eliminatedIndices = elim
                                        empWaveRadius = 0.05f
                                        elim.forEach { idx ->
                                            spawnExplosion(
                                                laneCenterX(idx, choices.options.size, currentSway()),
                                                waveYNorm,
                                                Color(0xFF38BDF8),
                                                count = 14
                                            )
                                        }
                                        usedHelpThisQuestion = true
                                    }
                                },
                                enabled = eliminatedIndices.isEmpty() && !readingPauseActive,
                                modifier = Modifier
                                    .height(48.dp)
                                    .alpha(if (readingPauseActive) 0.65f else 1f)
                                    .testTag("gym_5050_button"),
                                contentPadding = PaddingValues(horizontal = if (isShortLandscape) 10.dp else 14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                border = BorderStroke(2.dp, SoftGold)
                            ) {
                                Icon(Icons.Default.Filter2, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(4.dp))
                                Text(if (isShortLandscape) "50/50" else "EMP 50/50", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(if (isShortLandscape) 4.dp else 8.dp)) {
                        if (isShortLandscape) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = TextFormatter.toAnnotatedString(highlightColor = keywordHighlightColor(), raw = q.prompt),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("gym_question_prompt")
                                )
                                helperControls()
                            }
                        } else {
                            Text(
                                text = TextFormatter.toAnnotatedString(highlightColor = keywordHighlightColor(), raw = q.prompt),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = if (isWide) 20.sp else 19.sp,
                                    lineHeight = if (isWide) 28.sp else 25.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("gym_question_prompt")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                helperControls()
                                Spacer(Modifier.weight(1f))
                                if (floatingFeedback.isNotBlank()) {
                                    Text(
                                        text = floatingFeedback,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldSuccess,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (readingPauseActive) {
                            LinearProgressIndicator(
                                progress = { readingPauseProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        if (hintVisible && hintText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = TextFormatter.toAnnotatedString(highlightColor = keywordHighlightColor(), raw = "Indizio: $hintText"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                if (!useKeypadMode) {
                    // SPACE INVADERS 60FPS ARCADE ARENA
                    val labelPaint = remember {
                        Paint().apply {
                            color = android.graphics.Color.rgb(15, 23, 42)
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                        }
                    }
                    val shieldBgPaint = remember {
                        Paint().apply {
                            color = android.graphics.Color.rgb(250, 248, 242)
                            isAntiAlias = true
                        }
                    }
                    val shieldBorderPaint = remember {
                        Paint().apply {
                            style = Paint.Style.STROKE
                            strokeWidth = 4f
                            isAntiAlias = true
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(3.dp, if (config.isBoss) SoftGold else Color(0xFF4F46E5), RoundedCornerShape(14.dp))
                            .testTag("space_invaders_arena")
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(questionToken) {
                                    detectTapGestures { offset ->
                                        val c = latestChoices ?: return@detectTapGestures
                                        val count = c.options.size
                                        if (count == 0 || size.width <= 0) return@detectTapGestures
                                        val tapX = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        val sway = currentSway()
                                        var bestIdx = -1
                                        var bestDist = Float.MAX_VALUE
                                        for (idx in 0 until count) {
                                            if (idx in latestEliminated) continue
                                            val cx = laneCenterX(idx, count, sway)
                                            val d = abs(tapX - cx)
                                            if (d < bestDist) {
                                                bestDist = d
                                                bestIdx = idx
                                            }
                                        }
                                        if (bestIdx >= 0) {
                                            latestFireAtLane(bestIdx)
                                        }
                                    }
                                }
                                .pointerInput(questionToken) {
                                    detectDragGestures(
                                        onDrag = { change, _ ->
                                            if (size.width > 0) {
                                                shipTargetXNorm = (change.position.x / size.width.toFloat()).coerceIn(0.08f, 0.92f)
                                            }
                                        },
                                        onDragEnd = {
                                            val c = latestChoices ?: return@detectDragGestures
                                            val count = c.options.size
                                            if (count == 0) return@detectDragGestures
                                            val sway = currentSway()
                                            var bestIdx = -1
                                            var bestDist = Float.MAX_VALUE
                                            for (idx in 0 until count) {
                                                if (idx in latestEliminated) continue
                                                val d = abs(shipTargetXNorm - laneCenterX(idx, count, sway))
                                                if (d < bestDist) {
                                                    bestDist = d
                                                    bestIdx = idx
                                                }
                                            }
                                            if (bestIdx >= 0) latestFireAtLane(bestIdx)
                                        }
                                    )
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            if (w <= 0f || h <= 0f) return@Canvas

                            // Deep Space background gradient
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF050817), Color(0xFF13163B), Color(0xFF1E1B4B))
                                )
                            )

                            // Parallax Nebula glow clouds + Ringed Pixel Planet
                            val nebulaDrift = if (prefs.reducedMotion) 0f else sin(animTimeSec * 0.25f) * w * 0.025f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x3838BDF8), Color.Transparent),
                                    center = Offset(w * 0.22f + nebulaDrift, h * 0.28f),
                                    radius = w * 0.28f
                                ),
                                radius = w * 0.28f,
                                center = Offset(w * 0.22f + nebulaDrift, h * 0.28f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x34A855F7), Color.Transparent),
                                    center = Offset(w * 0.78f - nebulaDrift, h * 0.44f),
                                    radius = w * 0.30f
                                ),
                                radius = w * 0.30f,
                                center = Offset(w * 0.78f - nebulaDrift, h * 0.44f)
                            )
                            // Distant Saturn-like ringed planet
                            val planetCenter = Offset(w * 0.84f - nebulaDrift * 0.6f, h * 0.22f)
                            val planetR = (min(w, h) * 0.075f).coerceIn(14f, 34f)
                            drawCircle(Color(0xFF6366F1).copy(alpha = 0.45f), radius = planetR, center = planetCenter)
                            drawCircle(Color(0xFFA5B4FC).copy(alpha = 0.35f), radius = planetR * 0.48f, center = Offset(planetCenter.x - planetR * 0.25f, planetCenter.y - planetR * 0.25f))
                            drawOval(
                                color = Color(0x8838BDF8),
                                topLeft = Offset(planetCenter.x - planetR * 1.65f, planetCenter.y - planetR * 0.26f),
                                size = Size(planetR * 3.3f, planetR * 0.52f),
                                style = Stroke(width = 3f)
                            )

                            // Twinkling stars
                            for (s in stars) {
                                drawCircle(
                                    color = Color.White.copy(alpha = s.alpha),
                                    radius = s.radius,
                                    center = Offset(s.x * w, s.y * h)
                                )
                            }

                            // Energy shield line above player ship
                            val shieldY = 0.78f * h
                            val shieldColor = if (shieldFlash > 0.1f) {
                                Color(0xFFF59E0B).copy(alpha = 0.45f + shieldFlash * 0.55f)
                            } else {
                                Color(0xFF38BDF8).copy(alpha = 0.38f)
                            }
                            drawLine(
                                color = shieldColor,
                                start = Offset(w * 0.04f, shieldY),
                                end = Offset(w * 0.96f, shieldY),
                                strokeWidth = if (shieldFlash > 0.1f) 7f else 3f
                            )

                            // EMP Shockwave ring when 50/50 triggers
                            if (empWaveRadius >= 0f) {
                                val r = empWaveRadius * maxOf(w, h) * 0.7f
                                drawCircle(
                                    color = Color(0xFF38BDF8).copy(alpha = (1f - empWaveRadius * 0.8f).coerceIn(0f, 0.8f)),
                                    radius = r,
                                    center = Offset(shipXNorm * w, 0.86f * h),
                                    style = Stroke(width = 7f)
                                )
                            }

                            // Boss mothership header glow when Boss fight
                            if (config.isBoss) {
                                drawRoundRect(
                                    color = SoftGold.copy(alpha = 0.22f),
                                    topLeft = Offset(w * 0.22f, h * 0.02f),
                                    size = Size(w * 0.56f, h * 0.055f),
                                    cornerRadius = CornerRadius(14f, 14f)
                                )
                            }

                            // Draw Alien Invaders + Answer Shield Labels
                            val sway = currentSway()
                            val alienFrame = if ((animTimeSec * 3.0f).toInt() % 2 == 0) ALIEN_FRAME_0 else ALIEN_FRAME_1
                            val count = choices.options.size
                            val laneWidthPx = w / count.coerceAtLeast(2)
                            val alienPixel = (min(laneWidthPx * 0.055f, h * 0.015f)).coerceIn(2.6f, 6.5f)
                            val alienW = 11f * alienPixel
                            val alienH = 8f * alienPixel

                            for (idx in 0 until count) {
                                if (idx in eliminatedIndices) continue
                                val cx = laneCenterX(idx, count, sway) * w
                                val bobY = if (prefs.reducedMotion) 0f else sin(animTimeSec * 4.0f + idx * 1.3f) * 5f
                                val cy = waveYNorm * h + bobY
                                val alienColor = INVADER_COLORS[idx % INVADER_COLORS.size]

                                // Subtle target lane guide beam
                                drawLine(
                                    color = alienColor.copy(alpha = 0.12f),
                                    start = Offset(cx, cy + alienH),
                                    end = Offset(cx, shieldY),
                                    strokeWidth = 2f
                                )

                                // Multi-color 16-bit Pixel-art Alien sprite
                                drawPixelMatrix(
                                    matrix = alienFrame,
                                    origin = Offset(cx - alienW / 2f, cy - alienH / 2f),
                                    pixelSize = alienPixel,
                                    colorMap = mapOf(
                                        'X' to alienColor,
                                        'H' to Color.White.copy(alpha = 0.72f),
                                        'E' to Color(0xFF0F172A)
                                    )
                                )

                                // High-contrast Answer Pill right under each alien
                                val label = choices.options[idx]
                                val pillW = (laneWidthPx * 0.88f).coerceIn(72f, 210f)
                                val pillH = (h * 0.115f).coerceIn(38f, 56f)
                                val pillTop = cy + alienH * 0.62f
                                val maxPillLeft = (w - pillW - 6f).coerceAtLeast(6f)
                                val pillLeft = (cx - pillW / 2f).coerceIn(6f, maxPillLeft)

                                drawIntoCanvas { canvas ->
                                    shieldBorderPaint.color = android.graphics.Color.rgb(
                                        (alienColor.red * 255).toInt(),
                                        (alienColor.green * 255).toInt(),
                                        (alienColor.blue * 255).toInt()
                                    )
                                    canvas.nativeCanvas.drawRoundRect(
                                        pillLeft,
                                        pillTop,
                                        pillLeft + pillW,
                                        pillTop + pillH,
                                        12f,
                                        12f,
                                        shieldBgPaint
                                    )
                                    canvas.nativeCanvas.drawRoundRect(
                                        pillLeft,
                                        pillTop,
                                        pillLeft + pillW,
                                        pillTop + pillH,
                                        12f,
                                        12f,
                                        shieldBorderPaint
                                    )
                                    // Fit font cleanly inside pill width
                                    var textSize = (pillH * 0.50f).coerceIn(18f, 34f)
                                    labelPaint.textSize = textSize
                                    val maxTextW = pillW - 14f
                                    val measured = labelPaint.measureText(label)
                                    if (measured > maxTextW && measured > 0f) {
                                        textSize = (textSize * (maxTextW / measured)).coerceAtLeast(13f)
                                        labelPaint.textSize = textSize
                                    }
                                    val textY = pillTop + pillH / 2f - (labelPaint.descent() + labelPaint.ascent()) / 2f
                                    canvas.nativeCanvas.drawText(label, pillLeft + pillW / 2f, textY, labelPaint)
                                }
                            }

                            // Active Laser Bolt
                            activeLaser?.let { bolt ->
                                val bx = bolt.xNorm * w
                                val by = bolt.yNorm * h
                                val boltColor = if (bolt.isSuper) SoftGold else Color(0xFF38BDF8)
                                drawRoundRect(
                                    color = boltColor.copy(alpha = 0.35f),
                                    topLeft = Offset(bx - 8f, by - 22f),
                                    size = Size(16f, 44f),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(bx - 3.5f, by - 18f),
                                    size = Size(7f, 36f),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }

                            // Pixel Explosion Particles
                            for (p in particles) {
                                drawRect(
                                    color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                                    topLeft = Offset(p.x * w - p.size / 2f, p.y * h - p.size / 2f),
                                    size = Size(p.size, p.size)
                                )
                            }

                            // Player Pixel Starfighter
                            val shipPixel = (min(w, h) * 0.0125f).coerceIn(3.5f, 7f)
                            val shipW = 9f * shipPixel
                            val shipH = 7f * shipPixel
                            val sx = shipXNorm * w
                            val sy = 0.88f * h
                            // Animated dual plasma thruster flame + muzzle flash
                            val flameH = if ((animTimeSec * 14f).toInt() % 2 == 0) shipPixel * 2.6f else shipPixel * 1.5f
                            drawRect(
                                color = Color(0xFF38BDF8),
                                topLeft = Offset(sx - shipPixel * 1.1f, sy + shipH * 0.45f),
                                size = Size(shipPixel * 2.2f, flameH)
                            )
                            drawRect(
                                color = SoftGold,
                                topLeft = Offset(sx - shipPixel * 0.55f, sy + shipH * 0.45f),
                                size = Size(shipPixel * 1.1f, flameH * 0.7f)
                            )
                            val boltNow = activeLaser
                            if (boltNow != null && boltNow.yNorm > 0.65f) {
                                drawCircle(
                                    color = Color(0x8838BDF8),
                                    radius = shipPixel * 2.6f,
                                    center = Offset(sx, sy - shipH * 0.55f)
                                )
                            }
                            drawPixelMatrix(
                                matrix = SHIP_PIXELS,
                                origin = Offset(sx - shipW / 2f, sy - shipH / 2f),
                                pixelSize = shipPixel,
                                colorMap = mapOf(
                                    'W' to Color.White,
                                    'C' to Color(0xFF38BDF8),
                                    'B' to Color(0xFF2563EB),
                                    'H' to Color(0xFF93C5FD),
                                    'G' to SoftGold
                                )
                            )
                        }

                        // Subtle boss badge overlay
                        if (config.isBoss) {
                            Text(
                                text = "BOSS: ${pack.bossName}",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 11.sp,
                                color = SoftGold,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 4.dp)
                            )
                        }
                    }

                    // Bottom tactile arcade fire strip (48dp+ touch targets & accessibility + direct tap support)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (readingPauseActive) 0.65f else 1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        choices.options.forEachIndexed { idx, opt ->
                            val eliminated = idx in eliminatedIndices
                            val laneColor = INVADER_COLORS[idx % INVADER_COLORS.size]
                            Button(
                                onClick = { fireAtLane(idx) },
                                enabled = controlsEnabled && !eliminated,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .alpha(if (eliminated) 0.25f else 1f)
                                    .testTag("gym_fire_lane_$idx"),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CreamBackground,
                                    contentColor = InkPrimary
                                ),
                                border = BorderStroke(2.5.dp, laneColor)
                            ) {
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    // OPTIONAL CLASSIC KEYPAD / TASTIERINO MODE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        GbaCard(modifier = Modifier.fillMaxWidth()) {
                            val alpha = if (readingPauseActive) 0.65f else 1f
                            when (q.type) {
                                "numeric_input" -> {
                                    RetroNumpad(
                                        value = numpadInput,
                                        unit = q.unit,
                                        enabled = controlsEnabled,
                                        onValueChange = { numpadInput = it },
                                        onSubmit = { entered ->
                                            isInputLocked = true
                                            val ok = LearningEngine.isNumericAnswerCorrect(
                                                rawInput = entered,
                                                correctValue = q.correctValue,
                                                tolerance = q.tolerance
                                            )
                                            resolveAnswer(ok)
                                        },
                                        modifier = Modifier.alpha(alpha)
                                    )
                                }
                                "true_false" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(alpha),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                isInputLocked = true
                                                resolveAnswer(q.correctBool)
                                            },
                                            enabled = controlsEnabled,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(72.dp)
                                                .testTag("gym_true_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                                        ) {
                                            Text("VERO", style = MaterialTheme.typography.titleLarge, color = Color.White)
                                        }
                                        Button(
                                            onClick = {
                                                isInputLocked = true
                                                resolveAnswer(!q.correctBool)
                                            },
                                            enabled = controlsEnabled,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(72.dp)
                                                .testTag("gym_false_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = WarmAmber)
                                        ) {
                                            Text("FALSO", style = MaterialTheme.typography.titleLarge, color = Color.White)
                                        }
                                    }
                                }
                                else -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        choices.options.forEachIndexed { idx, opt ->
                                            val eliminated = idx in eliminatedIndices
                                            Button(
                                                onClick = {
                                                    isInputLocked = true
                                                    resolveAnswer(idx == choices.correctDisplayIndex)
                                                },
                                                enabled = controlsEnabled && !eliminated,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 60.dp)
                                                    .alpha(if (eliminated) 0.25f else alpha)
                                                    .testTag("gym_option_$idx"),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = CreamSurface,
                                                    contentColor = InkPrimary
                                                ),
                                                border = BorderStroke(2.dp, GameBalance.GBA_FRAME_DARK)
                                            ) {
                                                Text(
                                                    text = opt,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
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

        if (phase == GymPhase.EXPLANATION && currentEntry != null) {
            val q = currentEntry!!.question
            ExplanationModal(
                header = explanationHeader,
                correctAnswerDisplay = q.getCorrectAnswerDisplay(),
                explanation = q.explanation,
                autoRead = prefs.autoTtsQuestions,
                onSpeak = { viewModel.ttsManager.speak(it) },
                onStopSpeak = { viewModel.ttsManager.stop() },
                onContinue = { loadNextQuestion() }
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

        if (phase == GymPhase.SUMMARY && finalResult != null) {
            ResultSummaryModal(
                result = finalResult!!,
                packTitle = pack.title,
                medalName = pack.medalName,
                reducedMotion = prefs.reducedMotion,
                onContinue = { viewModel.returnFromMinigame(config) }
            )
        }

        if (showExitConfirm) {
            ConfirmModal(
                title = "Vuoi tornare a scuola?",
                message = "I progressi e le stelle delle risposte già date restano salvati.",
                confirmLabel = "Torna a scuola",
                onConfirm = {
                    showExitConfirm = false
                    viewModel.ttsManager.stop()
                    viewModel.learningEngine.awardEarlyExitStars(config.packId, rawAnswerStars)
                    viewModel.returnFromMinigame(config)
                },
                onCancel = { showExitConfirm = false }
            )
        }
    }
}

private fun DrawScope.drawPixelMatrix(
    matrix: Array<String>,
    origin: Offset,
    pixelSize: Float,
    colorMap: Map<Char, Color>
) {
    for (r in matrix.indices) {
        val row = matrix[r]
        for (c in row.indices) {
            val col = colorMap[row[c]] ?: continue
            drawRect(
                color = col,
                topLeft = Offset(origin.x + c * pixelSize, origin.y + r * pixelSize),
                size = Size(pixelSize + 0.5f, pixelSize + 0.5f)
            )
        }
    }
}

/** Cielo stellato per il palco della Palestra Spaziale. */
private val spaceBackdrop: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit = {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1026), Color(0xFF312E81))))
    for (i in 0 until 60) {
        val x = ((i * 97) % 1000) / 1000f * size.width
        val y = ((i * 53) % 1000) / 1000f * size.height
        drawCircle(Color.White.copy(alpha = 0.35f + (i % 3) * 0.2f), radius = 1.5f + (i % 2), center = Offset(x, y))
    }
    drawCircle(Color(0xFFA78BFA), radius = size.height * 0.16f, center = Offset(size.width * 0.82f, size.height * 0.30f))
    drawCircle(Color(0xFF7C3AED), radius = size.height * 0.16f, center = Offset(size.width * 0.84f, size.height * 0.27f))
}
