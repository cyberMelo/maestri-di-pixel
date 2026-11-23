package io.github.cybermelo.maestridipixel.ui.screens

import io.github.cybermelo.maestridipixel.ui.components.stageHeight
import io.github.cybermelo.maestridipixel.ui.components.PlantGrowthPanel
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Filter2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
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
import io.github.cybermelo.maestridipixel.ui.components.StepDotState
import io.github.cybermelo.maestridipixel.ui.theme.CreamBackground
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
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private enum class CatapultPhase {
    QUESTION,
    EXPLANATION,
    SUMMARY
}

private data class CatapultQueuedQuestion(
    val question: QuestionData,
    val originalIndex: Int,
    val isRetry: Boolean = false
)

private data class TowerSlot(
    val index: Int,
    val xNorm: Float,
    val baseYNorm: Float,
    val isFloatingShelf: Boolean,
    val accentColor: Color
)

private data class ProjectileState(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val intendedTargetIndex: Int? = null
)

private data class DebrisBlock(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val angle: Float,
    val vAngle: Float,
    val sizeW: Float,
    val sizeH: Float,
    val color: Color,
    val life: Float
)

private const val SLING_ANCHOR_X = 0.14f
private const val SLING_ANCHOR_Y = 0.64f
private const val GROUND_Y = 0.87f
private const val GRAVITY_NORM = 1.95f
private const val LAUNCH_POWER = 6.6f
private const val MAX_PULL_NORM = 0.145f

private val TOWER_ACCENTS = listOf(
    Color(0xFF0284C7), // Sky Science Blue
    Color(0xFF059669), // Leaf Green
    Color(0xFFD97706), // Amber Wood
    Color(0xFF7C3AED)  // Lab Violet
)

/**
 * Catapulta a Parabola stile Angry Birds (Scienze - Maestra Flora & Laboratorio):
 * Pull the pixel-art slingshot on the left to aim along live parabolic trajectory dots,
 * OR tap directly on a tower / quick-aim button to fire an arched shot!
 * Standard mode features lively physics, combo streak, and gently floating shelves on higher difficulty.
 * Ritmo Calmo keeps all targets stationary with zero rush.
 */
@Composable
fun ScienceCatapultScreen(
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
            allowedTypes = setOf("multiple_choice", "true_false")
        )
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.ttsManager.stop() }
    }

    if (pack == null || initialDeck.isEmpty()) {
        DialogBoxModal(
            title = "Catapulta di Scienze",
            message = "Questo argomento non ha domande pronte per questa aula!",
            onDismiss = { viewModel.returnFromMinigame(config) }
        )
        return
    }

    val queue = remember {
        mutableStateListOf<CatapultQueuedQuestion>().apply {
            initialDeck.forEachIndexed { idx, q -> add(CatapultQueuedQuestion(q, idx, false)) }
        }
    }
    val stepStates = remember {
        mutableStateListOf<StepDotState>().apply {
            repeat(initialDeck.size) { add(StepDotState.PENDING) }
        }
    }
    val retryCounts = remember { mutableMapOf<String, Int>() }
    val uniqueAnsweredIds = remember { mutableSetOf<String>() }

    var phase by remember { mutableStateOf(CatapultPhase.QUESTION) }
    var currentEntry by remember { mutableStateOf<CatapultQueuedQuestion?>(null) }
    var activeChoices by remember { mutableStateOf<ShuffledOptions?>(null) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var questionToken by remember { mutableIntStateOf(0) }

    var isInputLocked by remember { mutableStateOf(false) }
    var readingPauseActive by remember { mutableStateOf(false) }
    var readingPauseProgress by remember { mutableFloatStateOf(1f) }
    var hintVisible by remember { mutableStateOf(false) }
    var eliminatedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var collapsedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var usedHelpThisQuestion by remember { mutableStateOf(false) }

    // Catapult physics & visual state
    var isDraggingSling by remember { mutableStateOf(false) }
    var pullDxNorm by remember { mutableFloatStateOf(0f) }
    var pullDyNorm by remember { mutableFloatStateOf(0f) }
    var projectile by remember { mutableStateOf<ProjectileState?>(null) }
    val trailPoints = remember { mutableStateListOf<Offset>() }
    val debris = remember { mutableStateListOf<DebrisBlock>() }
    var animTimeSec by remember { mutableFloatStateOf(0f) }
    var statusBanner by remember { mutableStateOf("Tira la fionda o tocca una torre!") }

    var combo by remember { mutableIntStateOf(0) }
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
        if (phase != CatapultPhase.SUMMARY) showExitConfirm = true
    }

    fun buildTowers(count: Int): List<TowerSlot> = if (count <= 2) {
        listOf(
            TowerSlot(0, 0.56f, GROUND_Y, isFloatingShelf = false, accentColor = EmeraldSuccess),
            TowerSlot(1, 0.84f, GROUND_Y, isFloatingShelf = false, accentColor = WarmAmber)
        )
    } else {
        listOf(
            TowerSlot(0, 0.55f, 0.44f, isFloatingShelf = true, accentColor = TOWER_ACCENTS[0]),
            TowerSlot(1, 0.84f, 0.41f, isFloatingShelf = true, accentColor = TOWER_ACCENTS[1]),
            TowerSlot(2, 0.55f, GROUND_Y, isFloatingShelf = false, accentColor = TOWER_ACCENTS[2]),
            TowerSlot(3, 0.84f, GROUND_Y, isFloatingShelf = false, accentColor = TOWER_ACCENTS[3])
        )
    }

    fun effectiveTowerCenterY(slot: TowerSlot, difficulty: Int): Float {
        val bob = if (!prefs.calmMode && !prefs.reducedMotion && slot.isFloatingShelf && difficulty >= 2) {
            sin(animTimeSec * 2.1f + slot.index * 1.6f) * 0.026f
        } else {
            0f
        }
        return slot.baseYNorm - 0.11f + bob
    }

    fun spawnTowerDebris(cx: Float, cy: Float, color: Color, count: Int = 16) {
        val rng = Random.Default
        repeat(count) { i ->
            val angle = -Math.PI * (0.15 + rng.nextDouble() * 0.70)
            val spd = 0.22f + rng.nextFloat() * 0.45f
            val isWood = i % 3 != 0
            debris.add(
                DebrisBlock(
                    x = cx + (rng.nextFloat() - 0.5f) * 0.07f,
                    y = cy + (rng.nextFloat() - 0.5f) * 0.08f,
                    vx = (cos(angle) * spd).toFloat(),
                    vy = (sin(angle) * spd - 0.25f).toFloat(),
                    angle = rng.nextFloat() * 360f,
                    vAngle = (rng.nextFloat() - 0.5f) * 420f,
                    sizeW = if (isWood) 18f + rng.nextFloat() * 14f else 10f,
                    sizeH = if (isWood) 9f + rng.nextFloat() * 6f else 10f,
                    color = if (isWood) Color(0xFFB45309) else color,
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
            minigameId = "science_catapult",
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
        phase = CatapultPhase.SUMMARY
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
        activeChoices = if (next.question.type == "true_false") {
            ShuffledOptions(
                options = listOf("VERO", "FALSO"),
                correctDisplayIndex = if (next.question.correctBool) 0 else 1
            )
        } else {
            viewModel.learningEngine.getShuffledOptions(next.question)
        }
        hintVisible = false
        eliminatedIndices = emptySet()
        collapsedIndices = emptySet()
        usedHelpThisQuestion = false
        isInputLocked = false
        isDraggingSling = false
        pullDxNorm = 0f
        pullDyNorm = 0f
        projectile = null
        trailPoints.clear()
        statusBanner = "Tira la fionda o tocca una torre!"
        questionToken++
        phase = CatapultPhase.QUESTION

        if (prefs.autoTtsQuestions) {
            viewModel.ttsManager.speak(next.question.prompt)
        }
    }

    LaunchedEffect(Unit) {
        loadNextQuestion()
    }

    // Optional reading pause
    LaunchedEffect(phase, questionToken) {
        if (phase != CatapultPhase.QUESTION || currentEntry == null) return@LaunchedEffect
        val pauseSec = prefs.readingPauseSec
        if (pauseSec > 0.01f) {
            readingPauseActive = true
            readingPauseProgress = 0f
            val totalMs = (pauseSec * 1000f).toLong()
            val start = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - start
                readingPauseProgress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                if (elapsed >= totalMs) break
                delay(30)
            }
            readingPauseActive = false
            readingPauseProgress = 1f
        } else {
            readingPauseActive = false
            readingPauseProgress = 1f
        }
    }

    fun resolveTowerHit(targetIndex: Int, hitX: Float, hitY: Float) {
        val entry = currentEntry ?: return
        val choices = activeChoices ?: return
        val q = entry.question
        val isCorrect = targetIndex == choices.correctDisplayIndex

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
                collapsedIndices = collapsedIndices + targetIndex
                stepStates[entry.originalIndex] = StepDotState.COMPLETED
                combo++
                if (!entry.isRetry) correctFirstTry++
                val gained = GameBalance.STARS_PER_CORRECT +
                    if (!entry.isRetry && !usedHelpThisQuestion) GameBalance.STARS_FIRST_TRY_BONUS else 0
                rawAnswerStars += gained

                val superShot = combo >= GameBalance.GYM_COMBO_SUPER_THRESHOLD
                juicePopupText = if (superShot) "COMBO x$combo! +$gained ★" else "+$gained ★!"
                juicePopupColor = if (superShot) SoftGold else EmeraldSuccess
                juicePopupTrigger++
                spawnTowerDebris(hitX, hitY, EmeraldSuccess, count = if (superShot) 24 else 16)
                viewModel.audioManager.playSfx(SfxType.TOWER_CRASH)
                viewModel.audioManager.playSfx(if (superShot) SfxType.ATTACK_SUPER else SfxType.ANSWER_CORRECT)
                statusBanner = if (superShot) "SUPER LANCIO x$combo!" else "TORRE ABBATTUTA!"
                delay(460)
                isInputLocked = false
                loadNextQuestion()
            } else {
                combo = 0
                spawnTowerDebris(hitX, hitY, WarmAmber, count = 10)
                viewModel.audioManager.playSfx(SfxType.ANSWER_WRONG)
                statusBanner = "Quasi! Vediamo insieme perché."

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
                }
                explanationHeader = GameBalance.ENCOURAGING_ERROR_HEADERS[
                    (entry.originalIndex + nextRetries) % GameBalance.ENCOURAGING_ERROR_HEADERS.size
                ]
                delay(240)
                isInputLocked = false
                phase = CatapultPhase.EXPLANATION
            }
        }
    }

    fun launchWithVelocity(vx: Float, vy: Float, intendedTarget: Int? = null) {
        if (isInputLocked || readingPauseActive || phase != CatapultPhase.QUESTION) return
        isInputLocked = true
        viewModel.ttsManager.stop()
        trailPoints.clear()
        viewModel.audioManager.playSfx(SfxType.SLINGSHOT_LAUNCH)
        projectile = ProjectileState(
            x = SLING_ANCHOR_X,
            y = SLING_ANCHOR_Y,
            vx = vx,
            vy = vy,
            intendedTargetIndex = intendedTarget
        )
    }

    fun launchAtTower(targetIndex: Int) {
        val choices = activeChoices ?: return
        val entry = currentEntry ?: return
        if (isInputLocked || readingPauseActive || phase != CatapultPhase.QUESTION) return
        if (targetIndex !in choices.options.indices || targetIndex in eliminatedIndices) return

        val towers = buildTowers(choices.options.size)
        val slot = towers.getOrNull(targetIndex) ?: return
        val targetX = slot.xNorm
        val targetY = effectiveTowerCenterY(slot, entry.question.difficulty)
        val flightSec = 0.54f
        val vx = (targetX - SLING_ANCHOR_X) / flightSec
        val vy = (targetY - SLING_ANCHOR_Y - 0.5f * GRAVITY_NORM * flightSec * flightSec) / flightSec
        launchWithVelocity(vx, vy, intendedTarget = targetIndex)
    }

    // 60fps Catapult physics simulation
    LaunchedEffect(phase, questionToken, prefs.calmMode, prefs.reducedMotion) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) 0.016f else ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = frameNanos
                animTimeSec += dt

                // Update tumbling rubble debris
                var di = debris.size - 1
                while (di >= 0) {
                    val d = debris[di]
                    val nextLife = d.life - dt * 1.45f
                    if (nextLife <= 0f) {
                        debris.removeAt(di)
                    } else {
                        val nextVy = d.vy + GRAVITY_NORM * dt
                        val nextY = (d.y + nextVy * dt).coerceAtMost(GROUND_Y)
                        debris[di] = d.copy(
                            x = d.x + d.vx * dt,
                            y = nextY,
                            vy = if (nextY >= GROUND_Y) -nextVy * 0.35f else nextVy,
                            angle = d.angle + d.vAngle * dt,
                            life = nextLife
                        )
                    }
                    di--
                }

                // Update airborne projectile
                val proj = projectile
                if (proj != null) {
                    val nextVy = proj.vy + GRAVITY_NORM * dt
                    val nextX = proj.x + proj.vx * dt
                    val nextY = proj.y + nextVy * dt
                    if (trailPoints.isEmpty() || hypot(nextX - trailPoints.last().x, nextY - trailPoints.last().y) > 0.024f) {
                        trailPoints.add(Offset(nextX, nextY))
                        if (trailPoints.size > 24) trailPoints.removeAt(0)
                    }

                    val choices = activeChoices
                    val entry = currentEntry
                    var hitTowerIdx = -1
                    var hitX = nextX
                    var hitY = nextY
                    if (choices != null && entry != null) {
                        val towers = buildTowers(choices.options.size)
                        val hitHalfW = if (choices.options.size <= 2) 0.13f else 0.125f
                        val hitHalfH = 0.135f
                        for (slot in towers) {
                            if (slot.index in eliminatedIndices || slot.index in collapsedIndices) continue
                            // If launched with assisted tap on a specific tower, only collide with intended target
                            if (proj.intendedTargetIndex != null && slot.index != proj.intendedTargetIndex) continue
                            val cy = effectiveTowerCenterY(slot, entry.question.difficulty)
                            if (abs(nextX - slot.xNorm) <= hitHalfW && abs(nextY - cy) <= hitHalfH) {
                                hitTowerIdx = slot.index
                                hitX = slot.xNorm
                                hitY = cy
                                break
                            }
                        }
                    }

                    if (hitTowerIdx >= 0) {
                        projectile = null
                        resolveTowerHit(hitTowerIdx, hitX, hitY)
                    } else if (nextY >= GROUND_Y + 0.02f || nextX > 1.06f || nextX < -0.06f) {
                        // Missed all towers -> playful dust puff, reload orb with zero penalty
                        projectile = null
                        spawnTowerDebris(nextX.coerceIn(0.08f, 0.94f), GROUND_Y, Color(0xFF94A3B8), count = 6)
                        statusBanner = "Fuori bersaglio! Riprova il lancio!"
                        isInputLocked = false
                    } else {
                        projectile = proj.copy(x = nextX, y = nextY, vy = nextVy)
                    }
                }
            }
        }
    }

    val latestChoices by rememberUpdatedState(activeChoices)
    val latestEliminated by rememberUpdatedState(eliminatedIndices)
    val latestEntry by rememberUpdatedState(currentEntry)
    val latestLaunchAtTower by rememberUpdatedState(::launchAtTower)
    val latestLaunchWithVelocity by rememberUpdatedState(::launchWithVelocity)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFFBAE6FD))
                )
            )
            .safeDrawingPadding()
            .testTag("science_catapult_screen")
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
                        .testTag("catapult_exit_button"),
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

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (config.isBoss) SoftGold else GameBalance.subjectColor(pack.subject)
                ) {
                    Text(
                        text = if (config.isBoss) "BOSS" else "SCIENZE",
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 11.sp,
                        color = if (config.isBoss) Color(0xFF451A03) else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            val entry = currentEntry
            val choices = activeChoices
            if (entry != null && choices != null) {
                val q = entry.question
                val hintText = q.getEffectiveHint()
                val show5050 = prefs.enable5050 && choices.options.size >= 4
                val controlsEnabled = !readingPauseActive && !isInputLocked
                val towers = remember(choices.options.size) { buildTowers(choices.options.size) }

                // Compact Question Card (Focus Mode centered max 680dp)
                MinigameStage(
                    roomId = "lab",
                    teacherSpriteId = "npc_science",
                    teacherName = "Maestra Flora",
                    accent = Color(0xFF0D9488),
                    mood = teacherMood,
                    moodTrigger = moodTrigger,
                    reducedMotion = prefs.reducedMotion,
                    compact = isShortLandscape,
                    expandedHeight = stageHeight(screenH, 220.dp),
                    backdrop = null,
                    side = if (isShortLandscape) null else {
                        { PlantGrowthPanel(collected = stageCorrect.coerceAtMost(initialDeck.size), total = initialDeck.size) }
                    }
                ) {
                    val helperControls: @Composable () -> Unit = {
                        IconButton(
                            onClick = { viewModel.ttsManager.speak(q.prompt) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .testTag("catapult_tts_button")
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
                                    .testTag("catapult_hint_button"),
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
                                        viewModel.audioManager.playSfx(SfxType.TOWER_CRASH)
                                        val elim = LearningEngine.pickFiftyFiftyEliminations(
                                            optionCount = choices.options.size,
                                            correctDisplayIndex = choices.correctDisplayIndex
                                        )
                                        eliminatedIndices = elim
                                        elim.forEach { idx ->
                                            val s = towers.getOrNull(idx)
                                            if (s != null) {
                                                spawnTowerDebris(
                                                    s.xNorm,
                                                    effectiveTowerCenterY(s, q.difficulty),
                                                    s.accentColor,
                                                    count = 12
                                                )
                                            }
                                        }
                                        usedHelpThisQuestion = true
                                    }
                                },
                                enabled = eliminatedIndices.isEmpty() && !readingPauseActive,
                                modifier = Modifier
                                    .height(48.dp)
                                    .alpha(if (readingPauseActive) 0.65f else 1f)
                                    .testTag("catapult_5050_button"),
                                contentPadding = PaddingValues(horizontal = if (isShortLandscape) 10.dp else 14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                border = BorderStroke(2.dp, SoftGold)
                            ) {
                                Icon(Icons.Default.Filter2, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(4.dp))
                                Text("50/50", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
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
                                        .testTag("catapult_question_prompt")
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
                                modifier = Modifier.testTag("catapult_question_prompt")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                helperControls()
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = statusBanner,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
                                    .testTag("catapult_hint_banner")
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

                // 60FPS ANGRY BIRDS STYLE SLINGSHOT ARENA
                val boardBgPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.rgb(250, 248, 242)
                        isAntiAlias = true
                    }
                }
                val boardBorderPaint = remember {
                    Paint().apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 4f
                        isAntiAlias = true
                    }
                }
                val boardTextPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.rgb(30, 41, 59)
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(3.dp, GameBalance.GBA_FRAME_DARK, RoundedCornerShape(14.dp))
                        .testTag("catapult_arena")
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(questionToken) {
                                detectTapGestures { offset ->
                                    val c = latestChoices ?: return@detectTapGestures
                                    val e = latestEntry ?: return@detectTapGestures
                                    if (size.width <= 0 || size.height <= 0) return@detectTapGestures
                                    val tx = offset.x / size.width.toFloat()
                                    val ty = offset.y / size.height.toFloat()
                                    val slotList = buildTowers(c.options.size)
                                    var bestIdx = -1
                                    var bestDist = Float.MAX_VALUE
                                    for (s in slotList) {
                                        if (s.index in latestEliminated) continue
                                        val cy = effectiveTowerCenterY(s, e.question.difficulty)
                                        val d = hypot(tx - s.xNorm, ty - cy)
                                        if (d < 0.22f && d < bestDist) {
                                            bestDist = d
                                            bestIdx = s.index
                                        }
                                    }
                                    if (bestIdx >= 0) {
                                        latestLaunchAtTower(bestIdx)
                                    }
                                }
                            }
                            .pointerInput(questionToken) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        if (isInputLocked || readingPauseActive || size.width <= 0 || size.height <= 0) return@detectDragGestures
                                        val nx = startOffset.x / size.width.toFloat()
                                        if (nx <= 0.45f) {
                                            isDraggingSling = true
                                            pullDxNorm = 0f
                                            pullDyNorm = 0f
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        if (!isDraggingSling || size.width <= 0 || size.height <= 0) return@detectDragGestures
                                        val nx = change.position.x / size.width.toFloat()
                                        val ny = change.position.y / size.height.toFloat()
                                        var dx = nx - SLING_ANCHOR_X
                                        var dy = ny - SLING_ANCHOR_Y
                                        val len = hypot(dx, dy)
                                        if (len > MAX_PULL_NORM) {
                                            dx = dx / len * MAX_PULL_NORM
                                            dy = dy / len * MAX_PULL_NORM
                                        }
                                        pullDxNorm = dx
                                        pullDyNorm = dy
                                    },
                                    onDragCancel = {
                                        isDraggingSling = false
                                        pullDxNorm = 0f
                                        pullDyNorm = 0f
                                    },
                                    onDragEnd = {
                                        if (!isDraggingSling) return@detectDragGestures
                                        isDraggingSling = false
                                        val len = hypot(pullDxNorm, pullDyNorm)
                                        val vx = -pullDxNorm * LAUNCH_POWER
                                        val vy = -pullDyNorm * LAUNCH_POWER
                                        pullDxNorm = 0f
                                        pullDyNorm = 0f
                                        if (len >= 0.025f) {
                                            latestLaunchWithVelocity(vx, vy, null)
                                        }
                                    }
                                )
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        if (w <= 0f || h <= 0f) return@Canvas

                        // Daytime Sky
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF60A5FA), Color(0xFFBAE6FD), Color(0xFFE0F2FE))
                            )
                        )

                        // Warm morning pixel sun with soft corona
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x66FEF08A), Color.Transparent),
                                center = Offset(w * 0.14f, h * 0.16f),
                                radius = h * 0.24f
                            ),
                            radius = h * 0.24f,
                            center = Offset(w * 0.14f, h * 0.16f)
                        )
                        drawCircle(Color(0xFFFEF08A), radius = (h * 0.075f).coerceIn(12f, 24f), center = Offset(w * 0.14f, h * 0.16f))

                        // Drifting pixel clouds
                        val cloudDrift = if (prefs.reducedMotion) 0f else (animTimeSec * 0.015f) % 1.2f
                        listOf(0.18f to 0.14f, 0.56f to 0.10f, 0.88f to 0.18f).forEach { (cx0, cy0) ->
                            val cx = ((cx0 + cloudDrift) % 1.2f - 0.1f) * w
                            val cy = cy0 * h
                            drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(cx - 28f, cy), Size(56f, 16f), CornerRadius(8f, 8f))
                            drawRoundRect(Color.White.copy(alpha = 0.92f), Offset(cx - 16f, cy - 10f), Size(34f, 14f), CornerRadius(7f, 7f))
                        }

                        // Distant rolling pixel hills + tiny school laboratory observatory silhouette
                        val gY = GROUND_Y * h
                        drawCircle(Color(0xFF86EFAC), radius = w * 0.28f, center = Offset(w * 0.20f, gY + w * 0.12f))
                        drawCircle(Color(0xFF4ADE80), radius = w * 0.34f, center = Offset(w * 0.74f, gY + w * 0.15f))
                        // Cozy lab dome silhouette on horizon
                        val labX = w * 0.34f
                        val labBaseY = gY - h * 0.02f
                        drawRect(Color(0xFF94A3B8).copy(alpha = 0.65f), Offset(labX - 18f, labBaseY - 22f), Size(36f, 22f))
                        drawCircle(Color(0xFFE2E8F0).copy(alpha = 0.72f), radius = 14f, center = Offset(labX, labBaseY - 22f))
                        drawRect(Color(0xFF38BDF8).copy(alpha = 0.75f), Offset(labX - 6f, labBaseY - 14f), Size(12f, 8f))

                        // Grass & layered soil ground strip with tiny wildflowers
                        drawRect(Color(0xFF22C55E), Offset(0f, gY), Size(w, h * 0.042f))
                        drawRect(Color(0xFF16A34A), Offset(0f, gY + h * 0.032f), Size(w, h * 0.015f))
                        drawRect(Color(0xFF92400E), Offset(0f, gY + h * 0.045f), Size(w, h - gY))
                        for (fx in listOf(0.08f, 0.27f, 0.44f, 0.68f, 0.91f)) {
                            drawCircle(Color(0xFFFEF08A), radius = 3f, center = Offset(w * fx, gY + 3f))
                        }

                        // Slingshot Back Fork with wood highlight
                        val ax = SLING_ANCHOR_X * w
                        val ay = SLING_ANCHOR_Y * h
                        val orbX = if (isDraggingSling) (SLING_ANCHOR_X + pullDxNorm) * w else ax
                        val orbY = if (isDraggingSling) (SLING_ANCHOR_Y + pullDyNorm) * h else ay

                        // Ground shadow under slingshot
                        drawOval(Color(0x44000000), Offset(ax - 22f, gY - 3f), Size(44f, 10f))
                        // Wooden stem + highlight + back fork
                        drawLine(Color(0xFF78350F), Offset(ax, gY), Offset(ax, ay + 24f), strokeWidth = 15f, cap = StrokeCap.Round)
                        drawLine(Color(0xFFB45309), Offset(ax - 2f, gY - 2f), Offset(ax - 2f, ay + 24f), strokeWidth = 4f, cap = StrokeCap.Round)
                        drawLine(Color(0xFF78350F), Offset(ax, ay + 24f), Offset(ax - 16f, ay - 6f), strokeWidth = 10f, cap = StrokeCap.Round)
                        // Back elastic band
                        if (projectile == null) {
                            drawLine(Color(0xFF451A03), Offset(ax - 14f, ay - 4f), Offset(orbX, orbY), strokeWidth = 6f, cap = StrokeCap.Round)
                        }

                        // Dotted Parabolic Trajectory Preview while pulling slingshot
                        if (isDraggingSling && hypot(pullDxNorm, pullDyNorm) >= 0.02f) {
                            val vx = -pullDxNorm * LAUNCH_POWER
                            val vy = -pullDyNorm * LAUNCH_POWER
                            for (step in 1..15) {
                                val t = step * 0.048f
                                val px = (SLING_ANCHOR_X + vx * t) * w
                                val py = (SLING_ANCHOR_Y + vy * t + 0.5f * GRAVITY_NORM * t * t) * h
                                if (py > gY || px > w) break
                                drawCircle(
                                    color = Color.White.copy(alpha = (1f - step * 0.05f).coerceIn(0.2f, 0.95f)),
                                    radius = (6.5f - step * 0.25f).coerceAtLeast(2.5f),
                                    center = Offset(px, py)
                                )
                            }
                        }

                        // Draw Target Towers & Answer Signboards
                        val towerCount = choices.options.size
                        for (slot in towers) {
                            if (slot.index in eliminatedIndices || slot.index in collapsedIndices) continue
                            val tx = slot.xNorm * w
                            val cy = effectiveTowerCenterY(slot, q.difficulty) * h
                            val baseY = cy + 0.11f * h
                            val towerW = (w * (if (towerCount <= 2) 0.23f else 0.25f)).coerceIn(88f, 210f)
                            val towerH = (h * 0.21f).coerceIn(66f, 128f)

                            // Floating shelf stone platform with masonry lines
                            if (slot.isFloatingShelf) {
                                drawRoundRect(
                                    color = Color(0xFF475569),
                                    topLeft = Offset(tx - towerW * 0.56f, baseY),
                                    size = Size(towerW * 1.12f, 14f),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                                drawRect(
                                    color = Color(0xFF94A3B8),
                                    topLeft = Offset(tx - towerW * 0.52f, baseY + 3f),
                                    size = Size(towerW * 1.04f, 4f)
                                )
                                drawRect(
                                    color = Color(0xFF22C55E),
                                    topLeft = Offset(tx - towerW * 0.54f, baseY - 3f),
                                    size = Size(towerW * 1.08f, 5f)
                                )
                            } else {
                                // Ground shadow under lower towers
                                drawOval(
                                    color = Color(0x38000000),
                                    topLeft = Offset(tx - towerW * 0.50f, baseY - 3f),
                                    size = Size(towerW, 9f)
                                )
                            }

                            // Pixel-art wooden pillars with timber highlights + roof beam
                            val leftPillarX = tx - towerW * 0.44f
                            val rightPillarX = tx + towerW * 0.44f - 11f
                            val topY = baseY - towerH
                            drawRect(Color(0xFF92400E), Offset(leftPillarX, topY), Size(11f, towerH))
                            drawRect(Color(0xFFD97706), Offset(leftPillarX + 2f, topY), Size(3f, towerH))
                            drawRect(Color(0xFF92400E), Offset(rightPillarX, topY), Size(11f, towerH))
                            drawRect(Color(0xFFD97706), Offset(rightPillarX + 2f, topY), Size(3f, towerH))
                            drawRect(Color(0xFFB45309), Offset(leftPillarX - 5f, topY - 9f), Size(towerW * 0.88f + 10f, 11f))
                            drawRect(Color(0xFFF59E0B), Offset(leftPillarX - 4f, topY - 9f), Size(towerW * 0.88f + 8f, 3f))

                            // Expressive target lab critter perched on roof (blinks periodically)
                            val critterColor = if (config.isBoss) SoftGold else slot.accentColor
                            val blinking = !prefs.reducedMotion && ((animTimeSec * 0.9f + slot.index * 0.7f).toInt() % 5 == 0)
                            drawCircle(Color(0xFF0F172A), radius = 12.5f, center = Offset(tx, topY - 17f))
                            drawCircle(critterColor, radius = 10.5f, center = Offset(tx, topY - 17f))
                            if (!blinking) {
                                drawCircle(Color.White, radius = 3.2f, center = Offset(tx - 4f, topY - 19f))
                                drawCircle(Color.White, radius = 3.2f, center = Offset(tx + 4f, topY - 19f))
                                drawCircle(Color(0xFF0F172A), radius = 1.6f, center = Offset(tx - 4f, topY - 19f))
                                drawCircle(Color(0xFF0F172A), radius = 1.6f, center = Offset(tx + 4f, topY - 19f))
                            } else {
                                drawLine(Color(0xFF0F172A), Offset(tx - 6f, topY - 19f), Offset(tx - 2f, topY - 19f), strokeWidth = 2f)
                                drawLine(Color(0xFF0F172A), Offset(tx + 2f, topY - 19f), Offset(tx + 6f, topY - 19f), strokeWidth = 2f)
                            }

                            // Crisp Answer Signboard centered inside tower
                            val boardW = towerW * 0.94f
                            val boardH = (towerH * 0.68f).coerceAtLeast(44f)
                            val maxBLeft = (w - boardW - 6f).coerceAtLeast(6f)
                            val bLeft = (tx - boardW / 2f).coerceIn(6f, maxBLeft)
                            val bTop = cy - boardH / 2f
                            val label = choices.options[slot.index]

                            drawIntoCanvas { canvas ->
                                boardBorderPaint.color = android.graphics.Color.rgb(
                                    (slot.accentColor.red * 255).toInt(),
                                    (slot.accentColor.green * 255).toInt(),
                                    (slot.accentColor.blue * 255).toInt()
                                )
                                canvas.nativeCanvas.drawRoundRect(
                                    bLeft, bTop, bLeft + boardW, bTop + boardH,
                                    10f, 10f, boardBgPaint
                                )
                                canvas.nativeCanvas.drawRoundRect(
                                    bLeft, bTop, bLeft + boardW, bTop + boardH,
                                    10f, 10f, boardBorderPaint
                                )

                                // Wrap into 1 or 2 crisp lines so longer science words always fit cleanly
                                val lines = wrapToMaxTwoLines(label)
                                var fontSize = (if (lines.size == 1) boardH * 0.42f else boardH * 0.34f).coerceIn(14f, 28f)
                                boardTextPaint.textSize = fontSize
                                val maxW = boardW - 14f
                                val widest = lines.maxOf { boardTextPaint.measureText(it) }
                                if (widest > maxW && widest > 0f) {
                                    fontSize = (fontSize * (maxW / widest)).coerceAtLeast(11.5f)
                                    boardTextPaint.textSize = fontSize
                                }
                                val lineHeight = fontSize * 1.18f
                                val totalTextH = lineHeight * lines.size
                                val firstBaseline = bTop + (boardH - totalTextH) / 2f + fontSize * 0.88f
                                lines.forEachIndexed { li, line ->
                                    canvas.nativeCanvas.drawText(
                                        line,
                                        bLeft + boardW / 2f,
                                        firstBaseline + li * lineHeight,
                                        boardTextPaint
                                    )
                                }
                            }
                        }

                        // Projectile Trail
                        trailPoints.forEachIndexed { ti, pt ->
                            val frac = (ti + 1).toFloat() / trailPoints.size.coerceAtLeast(1)
                            drawCircle(
                                color = Color.White.copy(alpha = frac * 0.75f),
                                radius = 3.5f + frac * 3f,
                                center = Offset(pt.x * w, pt.y * h)
                            )
                        }

                        // Expressive Science Mascot Orb (with goggles & sparkle)
                        val activeProj = projectile
                        val drawOrbX = if (activeProj != null) activeProj.x * w else orbX
                        val drawOrbY = if (activeProj != null) activeProj.y * h else orbY
                        drawCircle(Color(0xFF0F172A), radius = 17.5f, center = Offset(drawOrbX, drawOrbY))
                        drawCircle(Color(0xFF0284C7), radius = 15f, center = Offset(drawOrbX, drawOrbY))
                        drawCircle(Color(0xFF38BDF8), radius = 11.5f, center = Offset(drawOrbX - 2f, drawOrbY - 2f))
                        // Little scientist goggles + happy eyes
                        drawCircle(SoftGold, radius = 5.5f, center = Offset(drawOrbX - 4f, drawOrbY - 2f))
                        drawCircle(SoftGold, radius = 5.5f, center = Offset(drawOrbX + 5f, drawOrbY - 2f))
                        drawCircle(Color.White, radius = 3.8f, center = Offset(drawOrbX - 4f, drawOrbY - 2f))
                        drawCircle(Color.White, radius = 3.8f, center = Offset(drawOrbX + 5f, drawOrbY - 2f))
                        drawCircle(Color(0xFF0F172A), radius = 1.8f, center = Offset(drawOrbX - 3f, drawOrbY - 2f))
                        drawCircle(Color(0xFF0F172A), radius = 1.8f, center = Offset(drawOrbX + 6f, drawOrbY - 2f))

                        // Front elastic band + Front Slingshot Fork (drawn over orb for depth)
                        if (activeProj == null) {
                            drawLine(Color(0xFF451A03), Offset(ax + 14f, ay - 2f), Offset(orbX, orbY), strokeWidth = 6f, cap = StrokeCap.Round)
                        }
                        drawLine(Color(0xFF92400E), Offset(ax, ay + 24f), Offset(ax + 16f, ay - 4f), strokeWidth = 10f, cap = StrokeCap.Round)
                        drawLine(Color(0xFFD97706), Offset(ax + 1f, ay + 22f), Offset(ax + 14f, ay - 3f), strokeWidth = 3f, cap = StrokeCap.Round)

                        // Tumbling debris blocks & impact stars
                        for (d in debris) {
                            val cx = d.x * w
                            val cy = d.y * h
                            rotate(degrees = d.angle, pivot = Offset(cx, cy)) {
                                drawRect(
                                    color = d.color.copy(alpha = d.life.coerceIn(0f, 1f)),
                                    topLeft = Offset(cx - d.sizeW / 2f, cy - d.sizeH / 2f),
                                    size = Size(d.sizeW, d.sizeH)
                                )
                            }
                        }
                    }

                    // Pull hint badge near slingshot when idle
                    if (projectile == null && !isDraggingSling && !isInputLocked) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xCC1E293B),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Tira la fionda!",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Accessible 1-tap quick-launch bar below arena (48dp touch targets & reliable on small screens)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (readingPauseActive) 0.65f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    choices.options.forEachIndexed { idx, opt ->
                        val eliminated = idx in eliminatedIndices
                        val accent = towers.getOrNull(idx)?.accentColor ?: DeepBlueAccent
                        Button(
                            onClick = { launchAtTower(idx) },
                            enabled = controlsEnabled && !eliminated,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .alpha(if (eliminated) 0.25f else 1f)
                                .testTag("catapult_target_$idx"),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CreamBackground,
                                contentColor = InkPrimary
                            ),
                            border = BorderStroke(2.5.dp, accent)
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (phase == CatapultPhase.EXPLANATION && currentEntry != null) {
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

        if (phase == CatapultPhase.SUMMARY && finalResult != null) {
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

private fun wrapToMaxTwoLines(text: String): List<String> {
    val clean = text.trim()
    if (clean.length <= 11 || !clean.contains(' ')) return listOf(clean)
    val words = clean.split(' ')
    if (words.size == 2) return words
    val mid = clean.length / 2
    var bestSplit = 1
    var bestDiff = Int.MAX_VALUE
    var acc = 0
    for (i in 0 until words.size - 1) {
        acc += words[i].length + (if (i > 0) 1 else 0)
        val diff = abs(acc - mid)
        if (diff < bestDiff) {
            bestDiff = diff
            bestSplit = i + 1
        }
    }
    return listOf(
        words.subList(0, bestSplit).joinToString(" "),
        words.subList(bestSplit, words.size).joinToString(" ")
    )
}
