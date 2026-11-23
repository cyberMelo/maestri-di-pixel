package com.example.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import com.example.ui.components.stageHeight
import com.example.ui.components.ChalkboardPanel
import com.example.ui.components.TeacherMood
import com.example.ui.components.MinigameStage
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private data class EnglishTileOption(
    val index: Int,
    val label: String,
    val isCorrect: Boolean
)

private data class EnglishDeckItem(
    val question: QuestionData,
    val stepIdx: Int,
    val retryCount: Int = 0
)

/**
 * Aula di Inglese — "Abbinamento Lampo" (Vocabolario, Colori, Animali e Frasi Base).
 * Cheerful bilingual card-match board with Teacher Emma, English pronunciation TTS button,
 * streak stars, and crisp tactile 2x2 flashcard tiles.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnglishMatchScreen(
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
        mutableStateOf(initialQuestions.mapIndexed { idx, q -> EnglishDeckItem(q, idx, 0) })
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
    var comboStreak by remember { mutableIntStateOf(0) }
    var juicePopupText by remember { mutableStateOf("") }
    var juicePopupColor by remember { mutableStateOf(EmeraldSuccess) }
    var juicePopupTrigger by remember { mutableIntStateOf(0) }

    var inputLocked by remember { mutableStateOf(false) }
    var readingPauseActive by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var usedHelpOnCurrent by remember { mutableStateOf(false) }
    var eliminatedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedTileIdx by remember { mutableIntStateOf(-1) }
    var selectedTileCorrect by remember { mutableStateOf<Boolean?>(null) }

    var showExplanation by remember { mutableStateOf(false) }
    var explanationHeader by remember { mutableStateOf("") }
    var explanationAnswer by remember { mutableStateOf("") }
    var explanationBody by remember { mutableStateOf("") }

    var showExitConfirm by remember { mutableStateOf(false) }
    var finalResult by remember { mutableStateOf<MinigameResult?>(null) }
    var teacherMood by remember { mutableStateOf(TeacherMood.IDLE) }
    var moodTrigger by remember { mutableIntStateOf(0) }
    var learnedWords by remember { mutableStateOf(listOf<String>()) }

    val currentItem = queue.getOrNull(currentQueuePos)
    val currentQuestion = currentItem?.question

    val tileOptions = remember(currentQueuePos, currentQuestion?.id) {
        if (currentQuestion == null) {
            emptyList()
        } else if (currentQuestion.type == "true_false") {
            listOf(
                EnglishTileOption(0, "True (Vero)", currentQuestion.correctBool),
                EnglishTileOption(1, "False (Falso)", !currentQuestion.correctBool)
            )
        } else {
            val shuffled = viewModel.learningEngine.getShuffledOptions(currentQuestion)
            shuffled.options.mapIndexed { idx, label ->
                EnglishTileOption(idx, label, idx == shuffled.correctDisplayIndex)
            }
        }
    }

    // Extract highlighted English phrase inside **...** if present, else fallback to prompt
    val englishSpeakPhrase = remember(currentQuestion?.id) {
        val raw = currentQuestion?.prompt.orEmpty()
        val boldMatches = Regex("\\*\\*([^*]+)\\*\\*").findAll(raw).map { it.groupValues[1].trim() }.toList()
        boldMatches.firstOrNull { it.isNotEmpty() } ?: TextFormatter.stripMarkers(raw)
    }

    LaunchedEffect(currentQueuePos) {
        showHint = false
        usedHelpOnCurrent = false
        eliminatedIndices = emptySet()
        selectedTileIdx = -1
        selectedTileCorrect = null
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
                minigameId = "english_match",
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

    fun pickTile(tileIdx: Int) {
        val item = currentItem ?: return
        val q = currentQuestion ?: return
        if (inputLocked || readingPauseActive || showExplanation || finalResult != null) return
        if (tileIdx in eliminatedIndices) return
        val tile = tileOptions.getOrNull(tileIdx) ?: return

        inputLocked = true
        viewModel.ttsManager.stop()
        selectedTileIdx = tileIdx
        selectedTileCorrect = tile.isCorrect

        val isRetry = item.retryCount > 0
        viewModel.learningEngine.recordAnswer(
            questionId = q.id,
            isCorrect = tile.isCorrect,
            isRetryInSession = isRetry,
            usedHelp = usedHelpOnCurrent
        )

        teacherMood = if (tile.isCorrect) TeacherMood.HAPPY else TeacherMood.SAD
        moodTrigger++
        if (tile.isCorrect) {
            // Sulla lavagna la coppia parola chiave = risposta (es. "farfalla = Butterfly")
            val answer = q.getCorrectAnswerDisplay()
            val keyword = englishSpeakPhrase.trim()
            val entry = if (keyword.isNotBlank() && !keyword.equals(answer, ignoreCase = true) && keyword.length <= 24) {
                "$keyword = $answer"
            } else {
                answer
            }
            if (entry.isNotBlank() && entry !in learnedWords) learnedWords = learnedWords + entry
        }
        scope.launch {
            if (tile.isCorrect) {
                viewModel.audioManager.playSfx(SfxType.ANSWER_CORRECT)
                comboStreak++
                val gained = if (!isRetry) {
                    correctFirstTry++
                    GameBalance.STARS_PER_CORRECT + GameBalance.STARS_FIRST_TRY_BONUS
                } else {
                    GameBalance.STARS_PER_CORRECT
                }
                rawAnswerStars += gained
                juicePopupText = if (comboStreak >= 2) "COMBO x$comboStreak! +$gained ★" else "+$gained ★!"
                juicePopupColor = if (comboStreak >= 3) Color(0xFFD97706) else EmeraldSuccess
                juicePopupTrigger++
                stepStates = stepStates.mapIndexed { idx, st ->
                    if (idx == item.stepIdx) StepDotState.COMPLETED else st
                }
                delay(480)
                advanceOrFinish()
            } else {
                viewModel.audioManager.playSfx(SfxType.ANSWER_WRONG)
                comboStreak = 0
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
                explanationHeader = if (canRetry) "Almost there! Riproveremo tra poco!" else "Ecco l'abbinamento giusto:"
                explanationAnswer = q.getCorrectAnswerDisplay()
                explanationBody = q.explanation
                showExplanation = true
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF0F172A)))
            )
            .safeDrawingPadding()
            .testTag("english_match_screen")
    ) {
        val isShortLandscape = maxHeight < 520.dp && maxWidth > maxHeight
        val screenH = maxHeight
        val pad = if (isShortLandscape) 8.dp else 14.dp
        val vGap = if (isShortLandscape) 6.dp else 10.dp

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
                        .testTag("english_exit_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF), contentColor = Color.White)
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
                    color = Color(0xFF1E40AF),
                    border = BorderStroke(2.dp, SoftGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = SoftGold, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (config.isBoss) "BOSS · x$comboStreak" else "Combo x$comboStreak",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 11.sp,
                            color = SoftGold
                        )
                    }
                }
            }

            // Palco: aula d'inglese, Teacher Emma che reagisce, domanda nel fumetto, lavagna delle parole
            val hintText = currentQuestion?.getEffectiveHint().orEmpty()
            MinigameStage(
                roomId = "english",
                teacherSpriteId = "npc_english",
                teacherName = "Teacher Emma",
                accent = Color(0xFFDC2626),
                mood = teacherMood,
                moodTrigger = moodTrigger,
                reducedMotion = prefs.reducedMotion,
                compact = isShortLandscape,
                expandedHeight = stageHeight(screenH, 280.dp),
                side = if (isShortLandscape) null else {
                    { ChalkboardPanel(title = "Words learned", words = learnedWords) }
                }
            ) {
                if (!currentQuestion?.topic.isNullOrBlank()) {
                    Text(
                        text = currentQuestion!!.topic.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = TextFormatter.toAnnotatedString(highlightColor = keywordHighlightColor(), raw = currentQuestion?.prompt.orEmpty()),
                    style = if (isShortLandscape) {
                        MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 20.sp)
                    } else {
                        MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp)
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isShortLandscape) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("english_question_prompt")
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.ttsManager.speakEnglish(englishSpeakPhrase) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("english_speak_en_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "Pronuncia inglese", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("EN", fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { currentQuestion?.let { viewModel.ttsManager.speak(it.prompt) } },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDBEAFE))
                            .testTag("english_tts_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Ascolta domanda", tint = DeepBlueAccent)
                    }
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
                                .testTag("english_hint_button"),
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
                    if (prefs.enable5050 && tileOptions.size == 4 && eliminatedIndices.isEmpty()) {
                        Button(
                            onClick = {
                                val correctIdx = tileOptions.indexOfFirst { it.isCorrect }
                                if (correctIdx >= 0) {
                                    eliminatedIndices = LearningEngine.pickFiftyFiftyEliminations(4, correctIdx)
                                    usedHelpOnCurrent = true
                                    helpsUsed++
                                }
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("english_5050_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlueAccent, contentColor = Color.White)
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
                        text = "Tip: ${currentQuestion.getEffectiveHint()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF92400E),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("english_hint_banner")
                    )
                }
            }

            // Flashcard Match Grid (2x2 for multiple_choice, 1x2 for true_false)
            val cardPalette = listOf(
                Color(0xFF2563EB),
                Color(0xFF7C3AED),
                Color(0xFF0D9488),
                Color(0xFFD97706)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isShortLandscape) 8.dp else 12.dp)
            ) {
                val rows = tileOptions.chunked(2)
                rows.forEach { rowTiles ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(if (isShortLandscape) 8.dp else 12.dp)
                    ) {
                        rowTiles.forEach { tile ->
                            val eliminated = tile.index in eliminatedIndices
                            val isSelected = selectedTileIdx == tile.index
                            val baseColor = if (eliminated) Color(0xFF475569) else cardPalette[tile.index % cardPalette.size]
                            val targetColor = when {
                                isSelected && selectedTileCorrect == true -> EmeraldSuccess
                                isSelected && selectedTileCorrect == false -> WarmAmber
                                else -> baseColor
                            }
                            val bgColor by animateColorAsState(
                                targetValue = targetColor,
                                animationSpec = tween(160),
                                label = "eng_tile_bg"
                            )
                            val tileScale by animateFloatAsState(
                                targetValue = if (isSelected && !prefs.reducedMotion) 1.03f else 1f,
                                animationSpec = tween(140),
                                label = "eng_tile_scale"
                            )
                            val letterTag = ('A' + tile.index).toString()
                            val flip = remember(currentQueuePos, tile.index) {
                                Animatable(if (prefs.reducedMotion) 0f else 90f)
                            }
                            LaunchedEffect(currentQueuePos, tile.index) {
                                if (flip.value != 0f) {
                                    delay(tile.index * 70L)
                                    flip.animateTo(0f, tween(260))
                                }
                            }

                            Button(
                                onClick = { pickTile(tile.index) },
                                enabled = !inputLocked && !readingPauseActive && !eliminated,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .scale(tileScale)
                                    .graphicsLayer {
                                        rotationY = flip.value
                                        cameraDistance = 14f * density
                                    }
                                    .testTag("english_tile_${tile.index}"),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) bgColor else Color(0xFFFFFBEB),
                                    contentColor = InkPrimary,
                                    disabledContainerColor = if (isSelected) bgColor else Color(0xFFFFFBEB).copy(alpha = 0.7f),
                                    disabledContentColor = InkPrimary.copy(alpha = 0.7f)
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 6.dp else 5.dp,
                                    color = if (isSelected) SoftGold else bgColor
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isShortLandscape) 30.dp else 36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = letterTag,
                                            fontFamily = SilkscreenFontFamily,
                                            fontSize = if (isShortLandscape) 13.sp else 15.sp,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = if (eliminated) "—" else tile.label,
                                        style = if (isShortLandscape) {
                                            MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                                        } else {
                                            MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                        },
                                        color = if (isSelected) Color.White else InkPrimary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
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
                title = "Uscire dall'Aula di Inglese?",
                message = "Le stelle già guadagnate in questa sfida verranno salvate.",
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
