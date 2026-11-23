package io.github.cybermelo.maestridipixel.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Filter2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import io.github.cybermelo.maestridipixel.pixelart.Facing
import io.github.cybermelo.maestridipixel.pixelart.PixelArtAssets
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import io.github.cybermelo.maestridipixel.pixelart.SchoolMaps
import io.github.cybermelo.maestridipixel.ui.components.ConfirmModal
import io.github.cybermelo.maestridipixel.ui.components.DialogBoxModal
import io.github.cybermelo.maestridipixel.ui.components.ExplanationModal
import io.github.cybermelo.maestridipixel.ui.components.GbaCard
import io.github.cybermelo.maestridipixel.ui.components.PixelSprite
import io.github.cybermelo.maestridipixel.ui.components.ProgressStepper
import io.github.cybermelo.maestridipixel.ui.components.FloatingJuicePopup
import io.github.cybermelo.maestridipixel.ui.components.ResultSummaryModal
import io.github.cybermelo.maestridipixel.ui.components.RetroHpBar
import io.github.cybermelo.maestridipixel.ui.components.StepDotState
import io.github.cybermelo.maestridipixel.ui.theme.DarkCardBg
import io.github.cybermelo.maestridipixel.ui.theme.DeepBlueAccent
import io.github.cybermelo.maestridipixel.ui.theme.EmeraldSuccess
import io.github.cybermelo.maestridipixel.ui.theme.HudNightBg
import io.github.cybermelo.maestridipixel.ui.theme.IceWhiteText
import io.github.cybermelo.maestridipixel.ui.theme.LocalDarkTheme
import io.github.cybermelo.maestridipixel.ui.theme.SilkscreenFontFamily
import io.github.cybermelo.maestridipixel.ui.theme.SkyHighlight
import io.github.cybermelo.maestridipixel.ui.theme.SoftGold
import io.github.cybermelo.maestridipixel.ui.theme.WarmAmber
import io.github.cybermelo.maestridipixel.viewmodel.ScuolaRetroViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

private enum class BattlePhase {
    CHOOSE_MOVE,
    SHOW_QUESTION,
    EXPLANATION,
    SUMMARY
}

private data class QueuedEntry(
    val question: QuestionData,
    val originalIndex: Int,
    val isRetry: Boolean = false
)

@Composable
fun YardBattleScreen(
    config: MinigameLaunchConfig,
    viewModel: ScuolaRetroViewModel
) {
    val scope = rememberCoroutineScope()
    val pack = remember(config.packId) { viewModel.packManager.getPack(config.packId) }
    val prefs = viewModel.saveManager.config.studyPreferences
    val profile = viewModel.saveManager.activeProfile
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

    // Empty-deck guard
    if (pack == null || initialDeck.isEmpty()) {
        DialogBoxModal(
            title = "Aula Cortile",
            message = "Questo argomento non ha domande pronte per questa aula!",
            onDismiss = { viewModel.returnFromMinigame(config) }
        )
        return
    }

    val enemyMaxHp = remember(initialDeck) { GameBalance.enemyMaxHpForDeck(initialDeck.size) }
    var playerHp by remember { mutableIntStateOf(GameBalance.PLAYER_MAX_HP) }
    var enemyHp by remember { mutableIntStateOf(enemyMaxHp) }

    val queue = remember {
        mutableStateListOf<QueuedEntry>().apply {
            initialDeck.forEachIndexed { idx, q -> add(QueuedEntry(q, idx, false)) }
        }
    }
    val stepStates = remember {
        mutableStateListOf<StepDotState>().apply {
            repeat(initialDeck.size) { add(StepDotState.PENDING) }
        }
    }
    val retryCounts = remember { mutableMapOf<String, Int>() }
    val uniqueAnsweredIds = remember { mutableSetOf<String>() }

    var phase by remember { mutableStateOf(BattlePhase.CHOOSE_MOVE) }
    var currentEntry by remember { mutableStateOf<QueuedEntry?>(null) }
    var activeMoveIsSpecial by remember { mutableStateOf(false) }
    var shuffledOptions by remember { mutableStateOf<ShuffledOptions?>(null) }
    var currentStepIndex by remember { mutableIntStateOf(0) }

    // Turn support states
    var isInputLocked by remember { mutableStateOf(false) }
    var readingPauseProgress by remember { mutableFloatStateOf(1f) }
    var readingPauseActive by remember { mutableStateOf(false) }
    var hintVisible by remember { mutableStateOf(false) }
    var eliminatedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var usedHelpThisQuestion by remember { mutableStateOf(false) }

    // Feedback & animations
    var battleBanner by remember { mutableStateOf("Scegli la tua mossa per iniziare!") }
    var enemyShake by remember { mutableFloatStateOf(0f) }
    var playerShake by remember { mutableFloatStateOf(0f) }

    // Metrics
    var correctFirstTry by remember { mutableIntStateOf(0) }
    var helpsUsedTotal by remember { mutableIntStateOf(0) }
    var rawAnswerStars by remember { mutableIntStateOf(0) }
    var battleCombo by remember { mutableIntStateOf(0) }
    var juicePopupText by remember { mutableStateOf("") }
    var juicePopupColor by remember { mutableStateOf(EmeraldSuccess) }
    var juicePopupTrigger by remember { mutableIntStateOf(0) }
    var explanationHeader by remember { mutableStateOf("") }
    var finalResult by remember { mutableStateOf<MinigameResult?>(null) }
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler {
        if (phase != BattlePhase.SUMMARY) {
            showExitConfirm = true
        }
    }

    fun finishSession(victory: Boolean) {
        viewModel.ttsManager.stop()
        if (victory && enemyHp <= 0) {
            for (i in stepStates.indices) {
                stepStates[i] = StepDotState.COMPLETED
            }
        }
        val totalUnique = uniqueAnsweredIds.size.coerceAtLeast(1)
        val effectiveVictory = if (config.isBoss) {
            enemyHp <= 0 || (playerHp > 0 && (correctFirstTry.toFloat() / totalUnique) >= GameBalance.BOSS_MIN_FIRST_TRY_ACCURACY)
        } else {
            victory && playerHp > 0
        }
        val res = viewModel.learningEngine.completeMinigameSession(
            packId = config.packId,
            minigameId = "yard_battle",
            isBoss = config.isBoss,
            victory = effectiveVictory,
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
        phase = BattlePhase.SUMMARY
    }

    fun advanceOrFinish() {
        if (enemyHp <= 0) {
            finishSession(victory = true)
            return
        }
        if (playerHp <= 0) {
            finishSession(victory = false)
            return
        }
        if (queue.isEmpty()) {
            finishSession(victory = true)
            return
        }
        phase = BattlePhase.CHOOSE_MOVE
        battleBanner = "Tocca a te! Scegli la prossima mossa."
    }

    fun pickMove(special: Boolean) {
        if (isInputLocked) return
        val idxInQueue = queue.indexOfFirst { entry ->
            if (special) entry.question.difficulty >= 2 else entry.question.difficulty == 1
        }
        if (idxInQueue < 0) return

        viewModel.ttsManager.stop()
        viewModel.audioManager.playSfx(SfxType.UI_CLICK)
        val entry = queue.removeAt(idxInQueue)
        currentEntry = entry
        activeMoveIsSpecial = special
        currentStepIndex = entry.originalIndex
        if (stepStates[entry.originalIndex] == StepDotState.PENDING) {
            stepStates[entry.originalIndex] = StepDotState.CURRENT
        }

        shuffledOptions = if (entry.question.type == "multiple_choice") {
            viewModel.learningEngine.getShuffledOptions(entry.question)
        } else {
            null
        }
        hintVisible = false
        eliminatedIndices = emptySet()
        usedHelpThisQuestion = false
        isInputLocked = false
        battleBanner = if (special) "Mossa Speciale (36 danni)!" else "Attacco Base (20 danni)!"
        phase = BattlePhase.SHOW_QUESTION

        if (prefs.autoTtsQuestions) {
            viewModel.ttsManager.speak(entry.question.prompt)
        }
    }

    // Reading pause effect per question
    LaunchedEffect(phase, currentEntry) {
        if (phase == BattlePhase.SHOW_QUESTION && currentEntry != null) {
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
    }

    fun submitAnswer(isCorrect: Boolean) {
        val entry = currentEntry ?: return
        if (isInputLocked || readingPauseActive) return
        isInputLocked = true
        viewModel.ttsManager.stop()

        val q = entry.question
        uniqueAnsweredIds.add(q.id)
        viewModel.learningEngine.recordAnswer(
            questionId = q.id,
            isCorrect = isCorrect,
            isRetryInSession = entry.isRetry,
            usedHelp = usedHelpThisQuestion
        )
        if (usedHelpThisQuestion) helpsUsedTotal++

        scope.launch {
            if (isCorrect) {
                val dmg = if (activeMoveIsSpecial) GameBalance.SPECIAL_ATTACK_DAMAGE else GameBalance.BASE_ATTACK_DAMAGE
                enemyHp = (enemyHp - dmg).coerceAtLeast(0)
                stepStates[entry.originalIndex] = StepDotState.COMPLETED
                if (!entry.isRetry) {
                    correctFirstTry++
                }
                val gained = GameBalance.STARS_PER_CORRECT +
                    if (!entry.isRetry && !usedHelpThisQuestion) GameBalance.STARS_FIRST_TRY_BONUS else 0
                rawAnswerStars += gained
                battleCombo++
                juicePopupText = if (battleCombo >= 3) "COMBO x$battleCombo! +$gained ★" else "+$gained ★!"
                juicePopupColor = if (battleCombo >= 3) SoftGold else EmeraldSuccess
                juicePopupTrigger++

                viewModel.audioManager.playSfx(if (activeMoveIsSpecial) SfxType.ATTACK_SUPER else SfxType.ATTACK_HIT)
                battleBanner = "Colpo riuscito! -$dmg HP!"
                if (!prefs.reducedMotion) {
                    enemyShake = 14f
                    delay(90)
                    enemyShake = -12f
                    delay(90)
                    enemyShake = 6f
                    delay(70)
                    enemyShake = 0f
                }
                delay(320)
                isInputLocked = false
                advanceOrFinish()
            } else {
                battleCombo = 0
                val counter = if (config.isBoss) GameBalance.BOSS_COUNTER_DAMAGE else GameBalance.ENEMY_COUNTER_DAMAGE
                playerHp = (playerHp - counter).coerceAtLeast(0)
                viewModel.audioManager.playSfx(SfxType.ANSWER_WRONG)
                battleBanner = "Risposta da rivedere! Scopriamo il trucco."

                val prevRetries = retryCounts[q.id] ?: 0
                val nextRetries = prevRetries + 1
                retryCounts[q.id] = nextRetries

                if (nextRetries < GameBalance.MAX_RETRIES_PER_QUESTION && playerHp > 0) {
                    stepStates[entry.originalIndex] = StepDotState.RETRY
                    val delaySlots = Random.nextInt(GameBalance.REQUEUE_DELAY_MIN, GameBalance.REQUEUE_DELAY_MAX + 1)
                    val insertPos = min(delaySlots, queue.size)
                    queue.add(insertPos, entry.copy(isRetry = true))
                } else {
                    stepStates[entry.originalIndex] = StepDotState.COMPLETED
                }

                if (!prefs.reducedMotion) {
                    playerShake = 10f
                    delay(80)
                    playerShake = -8f
                    delay(80)
                    playerShake = 0f
                }
                explanationHeader = GameBalance.ENCOURAGING_ERROR_HEADERS[
                    (entry.originalIndex + nextRetries) % GameBalance.ENCOURAGING_ERROR_HEADERS.size
                ]
                isInputLocked = false
                phase = BattlePhase.EXPLANATION
            }
        }
    }

    val enemySpriteId = when {
        config.isBoss -> "npc_boss"
        pack.subject == "storia" -> "npc_history"
        pack.subject == "scienze" -> "npc_science"
        else -> "npc_boss"
    }
    val enemyDisplayName = if (config.isBoss) pack.bossName else "Sfida: ${pack.title}"
    val animEnemyHp by animateFloatAsState(enemyHp.toFloat() / enemyMaxHp.toFloat(), tween(260), label = "enemy_hp")
    val animPlayerHp by animateFloatAsState(playerHp.toFloat() / GameBalance.PLAYER_MAX_HP.toFloat(), tween(260), label = "player_hp")

    val hasBaseMoves = queue.any { it.question.difficulty == 1 }
    val hasSpecialMoves = queue.any { it.question.difficulty >= 2 }

    val isDark = LocalDarkTheme.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) listOf(HudNightBg, DarkCardBg) else listOf(Color(0xFFBAE6FD), Color(0xFF86EFAC))
                )
            )
            .safeDrawingPadding()
            .testTag("yard_battle_screen")
    ) {
        val isCompactHeight = maxHeight < 620.dp
        val isShortLandscape = maxHeight < 520.dp && maxWidth > maxHeight
        val isCompactWidth = maxWidth < 640.dp
        val screenMaxHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isShortLandscape) 10.dp else 14.dp, vertical = if (isShortLandscape) 4.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (isShortLandscape) 4.dp else 8.dp)
        ) {
            // Top Bar (64dp budget)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { showExitConfirm = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("battle_exit_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Esci")
                    Spacer(Modifier.width(6.dp))
                    Text("Esci", style = MaterialTheme.typography.labelLarge)
                }

                ProgressStepper(
                    states = stepStates,
                    currentIndex = currentStepIndex
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GameBalance.subjectColor(pack.subject)
                ) {
                    Text(
                        text = if (config.isBoss) "BOSS" else GameBalance.subjectLabel(pack.subject),
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Pokémon GBA-style Illustrated Battle Stage Panel
            val isEnemyHit = enemyShake != 0f
            val isPlayerHit = playerShake != 0f
            val arenaHeight = when {
                isShortLandscape -> 76.dp
                else -> (screenMaxHeight * 0.38f).coerceIn(76.dp, 330.dp)
            }
            val yardBackdrop = remember { PixelArtAssets.getMapBackground("school_yard") }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(arenaHeight),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(3.dp, if (config.isBoss) SoftGold else GameBalance.GBA_FRAME_DARK),
                shadowElevation = 6.dp
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    // Personaggi ingranditi di un fattore intero: pixel tutti uguali
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val spriteScale = with(density) { (maxHeight.toPx() * 0.62f / PixelArtAssets.CHARACTER_PX).toInt().coerceAtLeast(1) }
                    val spritePx = with(density) { (PixelArtAssets.CHARACTER_PX * spriteScale).toDp() }
                    val hpPanelWidth = minOf(340.dp, maxWidth * 0.34f)
                    // Sfondo: il vero cortile della scuola (facciata e prato), ingrandito a fattore intero
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        if (w <= 0f || h <= 0f) return@Canvas
                        val bg = yardBackdrop
                        if (bg != null) {
                            val tile = PixelArtAssets.TILE_PX
                            val scale = maxOf(1, (h / (tile * 7f)).roundToInt())
                            val cropW = (w / scale).roundToInt().coerceAtMost(bg.width)
                            val cropH = (h / scale).roundToInt().coerceAtMost(bg.height)
                            val margin = (bg.width / tile - SchoolMaps.schoolYard.width) / 2
                            val doorX = ((margin + 13.5f) * tile).roundToInt()
                            val srcX = (doorX - cropW / 2).coerceIn(0, bg.width - cropW)
                            val srcY = ((margin + 0.6f) * tile).roundToInt().coerceIn(0, bg.height - cropH)
                            drawImage(
                                image = bg,
                                srcOffset = IntOffset(srcX, srcY),
                                srcSize = IntSize(cropW, cropH),
                                dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                                filterQuality = FilterQuality.None
                            )
                        }
                        // Boss: il cielo si fa notte viola e dorata
                        if (config.isBoss) {
                            drawRect(Color(0xFF312E81).copy(alpha = 0.55f))
                            for (i in 0..7) {
                                drawCircle(
                                    SoftGold.copy(alpha = 0.7f),
                                    radius = 3.5f,
                                    center = androidx.compose.ui.geometry.Offset(w * (0.08f + i * 0.12f), h * (0.10f + (i % 3) * 0.08f))
                                )
                            }
                        }
                        // Pedane ovali sotto i due sfidanti
                        val padColor = if (config.isBoss) Color(0xFFA78BFA) else Color(0xFF4ADE80)
                        val padRim = if (config.isBoss) SoftGold else Color(0xFF166534)
                        val padW = spritePx.toPx() * 1.25f
                        val padH = padW * 0.26f
                        val padY = h * 0.86f - padH / 2f
                        for (cx in listOf(w * 0.22f, w * 0.78f)) {
                            drawOval(padRim.copy(alpha = 0.75f), topLeft = androidx.compose.ui.geometry.Offset(cx - padW / 2f, padY + 4f), size = androidx.compose.ui.geometry.Size(padW, padH))
                            drawOval(padColor.copy(alpha = 0.9f), topLeft = androidx.compose.ui.geometry.Offset(cx - padW * 0.47f, padY), size = androidx.compose.ui.geometry.Size(padW * 0.94f, padH * 0.85f))
                        }
                        // Lampo dell'impatto
                        if (isEnemyHit) {
                            val c = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.55f)
                            drawCircle(SoftGold.copy(alpha = 0.55f), radius = h * 0.30f, center = c)
                            drawCircle(Color.White.copy(alpha = 0.85f), radius = h * 0.14f, center = c)
                        }
                        if (isPlayerHit) {
                            drawCircle(WarmAmber.copy(alpha = 0.50f), radius = h * 0.28f, center = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.55f))
                        }
                    }

                    // Sfidanti grandi sulle pedane
                    PixelSprite(
                        bitmap = PixelArtAssets.getCharacterSprite(
                            id = "avatar_${profile?.avatarId ?: 0}",
                            facing = Facing.RIGHT,
                            stepFrame = if (isEnemyHit) 1 else 0,
                            hatId = profile?.equippedHat.orEmpty()
                        ),
                        contentDescription = profile?.name ?: "Tu",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = maxWidth * 0.22f - spritePx / 2 + playerShake.dp, y = -(maxHeight * 0.10f))
                            .size(spritePx)
                    )
                    PixelSprite(
                        bitmap = PixelArtAssets.getCharacterSprite(enemySpriteId, Facing.LEFT, 0),
                        contentDescription = enemyDisplayName,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = maxWidth * 0.78f - spritePx / 2 + enemyShake.dp, y = -(maxHeight * 0.10f))
                            .size(spritePx)
                    )

                    // Pannelli vita in alto, come nei giochi di ruolo portatili
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isShortLandscape) 8.dp else 12.dp, vertical = if (isShortLandscape) 6.dp else 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BattleHpPanel(
                            name = profile?.name ?: "Tu",
                            hp = playerHp,
                            maxHp = GameBalance.PLAYER_MAX_HP,
                            progress = animPlayerHp,
                            barColor = if (playerHp > 35) EmeraldSuccess else WarmAmber,
                            borderColor = MaterialTheme.colorScheme.outline,
                            alignEnd = false,
                            compact = isShortLandscape,
                            modifier = Modifier.width(hpPanelWidth)
                        )
                        BattleHpPanel(
                            name = enemyDisplayName,
                            hp = enemyHp,
                            maxHp = enemyMaxHp,
                            progress = animEnemyHp,
                            barColor = if (config.isBoss) SoftGold else Color(0xFFDC2626),
                            borderColor = if (config.isBoss) SoftGold else MaterialTheme.colorScheme.outline,
                            alignEnd = true,
                            compact = isShortLandscape,
                            modifier = Modifier.width(hpPanelWidth)
                        )
                    }

                    if (!isShortLandscape || phase == BattlePhase.CHOOSE_MOVE || isEnemyHit || isPlayerHit) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
                        ) {
                            Text(
                                text = battleBanner,
                                style = if (isShortLandscape) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Interactive Card (Focus Mode centered max 680dp on wide tablets)
            GbaCard(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = if (isShortLandscape) 10.dp else 16.dp,
                    vertical = if (isShortLandscape) 6.dp else 14.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (phase == BattlePhase.CHOOSE_MOVE) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Quale mossa vuoi usare?",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "L'Attacco Base propone domande facili (20 danni). La Mossa Speciale propone sfide più intense (36 danni)!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Button(
                                    onClick = { pickMove(special = false) },
                                    enabled = hasBaseMoves,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 96.dp)
                                        .testTag("move_base_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlueAccent, contentColor = Color.White)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(28.dp))
                                        Spacer(Modifier.height(4.dp))
                                        Text("Attacco Base", style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            text = if (hasBaseMoves) "20 danni · Difficoltà 1" else "Solo mosse speciali rimaste",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                Button(
                                    onClick = { pickMove(special = true) },
                                    enabled = hasSpecialMoves,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 96.dp)
                                        .testTag("move_special_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmAmber)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(28.dp))
                                        Spacer(Modifier.height(4.dp))
                                        Text("Mossa Speciale", style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            text = if (hasSpecialMoves) "36 danni · Difficoltà 2-3" else "Solo attacchi base rimasti",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    } else if (currentEntry != null) {
                        val q = currentEntry!!.question
                        val hintText = q.getEffectiveHint()
                        val show5050Button = prefs.enable5050 && q.type == "multiple_choice"

                        Column(verticalArrangement = Arrangement.spacedBy(if (isShortLandscape) 6.dp else 10.dp)) {
                            val battleHelpers: @Composable () -> Unit = {
                                IconButton(
                                    onClick = { viewModel.ttsManager.speak(q.prompt) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .testTag("battle_tts_button")
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
                                            .testTag("battle_hint_button"),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (isShortLandscape) 10.dp else 14.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Indizio", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                if (show5050Button) {
                                    OutlinedButton(
                                        onClick = {
                                            val shuf = shuffledOptions ?: return@OutlinedButton
                                            if (eliminatedIndices.isEmpty() && !readingPauseActive) {
                                                viewModel.audioManager.playSfx(SfxType.UI_CLICK)
                                                eliminatedIndices = LearningEngine.pickFiftyFiftyEliminations(
                                                    optionCount = shuf.options.size,
                                                    correctDisplayIndex = shuf.correctDisplayIndex
                                                )
                                                usedHelpThisQuestion = true
                                            }
                                        },
                                        enabled = eliminatedIndices.isEmpty() && !readingPauseActive,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .alpha(if (readingPauseActive) 0.65f else 1f)
                                            .testTag("battle_5050_button"),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (isShortLandscape) 10.dp else 14.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                        border = BorderStroke(2.dp, SoftGold)
                                    ) {
                                        Icon(Icons.Default.Filter2, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Spacer(Modifier.width(4.dp))
                                        Text("50/50", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }

                            // Question Header: single horizontal row on short landscape so all 4 answers fit cleanly without scrolling
                            if (isShortLandscape) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = TextFormatter.toAnnotatedString(highlightColor = keywordHighlightColor(), raw = q.prompt),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            lineHeight = 20.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("battle_question_prompt")
                                    )
                                    battleHelpers()
                                }
                            } else {
                                Text(
                                    text = TextFormatter.toAnnotatedString(highlightColor = keywordHighlightColor(), raw = q.prompt),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = if (isCompactWidth) 19.sp else 20.sp,
                                        lineHeight = if (isCompactWidth) 26.sp else 30.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("battle_question_prompt")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    battleHelpers()
                                }
                            }

                            // Discrete reading pause bar
                            if (readingPauseActive) {
                                LinearProgressIndicator(
                                    progress = { readingPauseProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .testTag("reading_pause_bar"),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            }

                            // Hint Banner
                            if (hintVisible && hintText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = if (isShortLandscape) 5.dp else 8.dp)
                                        .testTag("battle_hint_banner")
                                ) {
                                    Text(
                                        text = TextFormatter.toAnnotatedString(highlightColor = keywordHighlightColor(), raw = "Indizio: $hintText"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Answer Buttons (2x2 grid for multiple_choice, 2 large buttons for true_false)
                            val answersEnabled = !readingPauseActive && !isInputLocked
                            val answersAlpha = if (readingPauseActive) 0.65f else 1f

                            if (q.type == "true_false") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .alpha(answersAlpha),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { submitAnswer(q.correctBool) },
                                        enabled = answersEnabled,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = if (isShortLandscape) 58.dp else 72.dp)
                                            .testTag("answer_true_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = IceWhiteText)
                                    ) {
                                        Text("VERO", style = MaterialTheme.typography.titleLarge, color = IceWhiteText)
                                    }
                                    Button(
                                        onClick = { submitAnswer(!q.correctBool) },
                                        enabled = answersEnabled,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = if (isShortLandscape) 58.dp else 72.dp)
                                            .testTag("answer_false_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WarmAmber, contentColor = IceWhiteText)
                                    ) {
                                        Text("FALSO", style = MaterialTheme.typography.titleLarge, color = IceWhiteText)
                                    }
                                }
                            } else if (shuffledOptions != null) {
                                val opts = shuffledOptions!!.options
                                val correctIdx = shuffledOptions!!.correctDisplayIndex
                                Column(verticalArrangement = Arrangement.spacedBy(if (isShortLandscape) 6.dp else 12.dp)) {
                                    for (rowIdx in 0 until 2) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(if (isShortLandscape) 8.dp else 12.dp)
                                        ) {
                                            for (colIdx in 0 until 2) {
                                                val idx = rowIdx * 2 + colIdx
                                                if (idx < opts.size) {
                                                    val eliminated = idx in eliminatedIndices
                                                    val btnSource = remember { MutableInteractionSource() }
                                                    val btnPressed by btnSource.collectIsPressedAsState()
                                                    val btnScale by animateFloatAsState(
                                                        targetValue = if (btnPressed && !prefs.reducedMotion) 0.96f else 1f,
                                                        label = "opt_scale_$idx"
                                                    )
                                                    Button(
                                                        onClick = { submitAnswer(idx == correctIdx) },
                                                        enabled = answersEnabled && !eliminated,
                                                        interactionSource = btnSource,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .scale(btnScale)
                                                            .heightIn(min = if (isShortLandscape) 52.dp else 64.dp)
                                                            .alpha(if (eliminated) 0.25f else answersAlpha)
                                                            .testTag("answer_option_$idx"),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                                            horizontal = if (isShortLandscape) 10.dp else 14.dp,
                                                            vertical = if (isShortLandscape) 5.dp else 8.dp
                                                        ),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                            contentColor = MaterialTheme.colorScheme.onSurface
                                                        ),
                                                        border = BorderStroke(
                                                            width = if (btnPressed) 2.5.dp else 2.dp,
                                                            color = if (btnPressed) SkyHighlight else MaterialTheme.colorScheme.outline
                                                        )
                                                    ) {
                                                        Text(
                                                            text = opts[idx],
                                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = if (isShortLandscape) 15.sp else 17.sp,
                                                                lineHeight = if (isShortLandscape) 19.sp else 23.sp
                                                            ),
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
        }

        // Modals
        if (phase == BattlePhase.EXPLANATION && currentEntry != null) {
            val q = currentEntry!!.question
            ExplanationModal(
                header = explanationHeader,
                correctAnswerDisplay = q.getCorrectAnswerDisplay(),
                explanation = q.explanation,
                autoRead = prefs.autoTtsQuestions,
                onSpeak = { viewModel.ttsManager.speak(it) },
                onStopSpeak = { viewModel.ttsManager.stop() },
                onContinue = { advanceOrFinish() }
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

        if (phase == BattlePhase.SUMMARY && finalResult != null) {
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


/** Pannello nome + barra vita di uno sfidante, sovrapposto all'arena. */
@Composable
private fun BattleHpPanel(
    name: String,
    hp: Int,
    maxHp: Int,
    progress: Float,
    barColor: Color,
    borderColor: Color,
    alignEnd: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (compact) 3.dp else 6.dp),
            horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
        ) {
            Text(
                text = name,
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            RetroHpBar(progress = progress, barColor = barColor, modifier = Modifier.fillMaxWidth())
            Text(
                "HP $hp / $maxHp",
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
