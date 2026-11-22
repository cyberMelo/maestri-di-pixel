package com.example.core

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class LearningEngine(
    private val saveManager: SaveManager,
    private val packManager: PackManager
) {
    var sessionsInCurrentRun: Int = 0
        private set

    fun resetRunSessionCounter() {
        sessionsInCurrentRun = 0
    }

    fun recordAnswer(
        questionId: String,
        isCorrect: Boolean,
        isRetryInSession: Boolean,
        usedHelp: Boolean = false,
        nowUnix: Long = System.currentTimeMillis() / 1000L
    ) {
        saveManager.updateActiveProfile { profile ->
            val current = profile.leitner[questionId] ?: LeitnerRecord()
            val updated = when {
                !isCorrect -> current.copy(
                    box = 0,
                    lastSeenUnix = nowUnix,
                    wrongTotal = current.wrongTotal + 1,
                    lastWrongUnix = nowUnix
                )
                isRetryInSession -> current.copy(
                    box = 0,
                    lastSeenUnix = nowUnix,
                    correctTotal = current.correctTotal + 1
                )
                usedHelp -> current.copy(
                    box = max(current.box, 1),
                    lastSeenUnix = nowUnix,
                    correctTotal = current.correctTotal + 1
                )
                else -> current.copy(
                    box = min(current.box + 1, GameBalance.LEITNER_MAX_BOX),
                    lastSeenUnix = nowUnix,
                    correctTotal = current.correctTotal + 1
                )
            }
            val newMap = profile.leitner.toMutableMap()
            newMap[questionId] = updated
            profile.copy(leitner = newMap)
        }
    }

    fun getPackMasteryPercent(packId: String): Float {
        val pack = packManager.getPack(packId) ?: return 0f
        return getPackMasteryPercent(pack)
    }

    fun getPackMasteryPercent(pack: PackData): Float {
        val playable = pack.playableQuestions
        if (playable.isEmpty()) return 0f
        val leitner = saveManager.activeProfile?.leitner ?: emptyMap()
        val boxSum = playable.sumOf { q -> (leitner[q.id]?.box ?: 0).coerceIn(0, GameBalance.LEITNER_MAX_BOX) }
        return (boxSum.toFloat() / (GameBalance.LEITNER_MAX_BOX * playable.size).toFloat()) * 100f
    }

    fun isBossUnlocked(packId: String): Boolean =
        getPackMasteryPercent(packId) >= GameBalance.BOSS_UNLOCK_MASTERY_PCT

    fun hasMedal(packId: String): Boolean =
        saveManager.activeProfile?.medals?.contains(packId) == true

    fun buildSessionDeck(
        packId: String,
        count: Int,
        allowedTypes: Set<String>,
        nowUnix: Long = System.currentTimeMillis() / 1000L
    ): List<QuestionData> {
        val pack = packManager.getPack(packId) ?: return emptyList()
        val allCompatible = pack.playableQuestions.filter { it.type in allowedTypes }
        if (allCompatible.isEmpty()) return emptyList()

        val activeFocus = saveManager.config.focusTopics[packId]
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        val compatible = if (activeFocus.isNotEmpty()) {
            val focused = allCompatible.filter { it.topic.trim() in activeFocus }
            if (focused.isNotEmpty()) focused else allCompatible
        } else {
            allCompatible
        }

        val leitner = saveManager.activeProfile?.leitner ?: emptyMap()

        fun priorityRank(q: QuestionData): Int {
            val rec = leitner[q.id] ?: return 2 // New question
            if (rec.box == 0 && rec.wrongTotal > 0) return 0 // Recent mistake
            val cooldown = GameBalance.BOX_COOLDOWN_SECONDS[rec.box] ?: 0L
            val overdue = (nowUnix - rec.lastSeenUnix) >= cooldown
            return when {
                rec.box in 1..2 && overdue -> 1
                rec.box == 0 -> 2
                rec.box in 1..2 -> 3
                else -> 4 // Box 3
            }
        }

        val sorted = compatible.sortedWith(
            compareBy<QuestionData>(
                { priorityRank(it) },
                { leitner[it.id]?.lastSeenUnix ?: 0L },
                { it.difficulty },
                { it.id }
            )
        )
        return sorted.take(min(count.coerceAtLeast(1), sorted.size))
    }

    fun getShuffledOptions(q: QuestionData, rng: Random = Random.Default): ShuffledOptions {
        if (q.type != "multiple_choice" || q.options.isEmpty()) {
            return ShuffledOptions(q.options, q.correctIndex)
        }
        val indexed = q.options.mapIndexed { idx, text -> idx to text }.shuffled(rng)
        val newOptions = indexed.map { it.second }
        val newCorrectIdx = indexed.indexOfFirst { it.first == q.correctIndex }
        return ShuffledOptions(newOptions, newCorrectIdx)
    }

    fun getRecommendedActivity(): RecommendedActivity? {
        val activePacks = packManager.getActivePacks().filter { pack ->
            val allowed = allowedTypesForSubject(pack.subject)
            pack.playableQuestions.any { it.type in allowed }
        }
        if (activePacks.isEmpty()) return null

        // Priority 1: Priority pack (Modalità Verifica) not yet at 100%
        val prioPack = activePacks
            .filter { it.priority && getPackMasteryPercent(it) < 100f }
            .minByOrNull { getPackMasteryPercent(it) }
        if (prioPack != null) {
            val mastery = getPackMasteryPercent(prioPack)
            val bossReady = mastery >= GameBalance.BOSS_UNLOCK_MASTERY_PCT && !hasMedal(prioPack.packId)
            return buildRecommended(prioPack, bossReady)
        }

        // Priority 2: Boss unlocked (>= 80%) without medal
        val bossPack = activePacks.firstOrNull {
            getPackMasteryPercent(it) >= GameBalance.BOSS_UNLOCK_MASTERY_PCT && !hasMedal(it.packId)
        }
        if (bossPack != null) {
            return buildRecommended(bossPack, true)
        }

        // Priority 3: Active pack with lowest mastery
        val lowest = activePacks.minByOrNull { getPackMasteryPercent(it) } ?: return null
        return buildRecommended(lowest, false)
    }

    private fun buildRecommended(pack: PackData, isBoss: Boolean): RecommendedActivity {
        val minigameId = minigameIdForSubject(pack.subject)
        val location = when (pack.subject) {
            "storia" -> "Aula Storia / Cortile"
            "scienze" -> "Maestra Flora / Laboratorio"
            "matematica" -> "Palestra (Corridoio)"
            "italiano" -> "Biblioteca (Corridoio)"
            "geografia" -> "Aula Geografia (Corridoio)"
            "inglese" -> "Aula Inglese (Corridoio)"
            else -> "Preside Stella (Cortile)"
        }
        return RecommendedActivity(
            packId = pack.packId,
            title = pack.title,
            subject = pack.subject,
            isBoss = isBoss,
            targetLocationLabel = location,
            minigameId = minigameId
        )
    }

    fun getSessionsThisWeek(
        nowUnix: Long = System.currentTimeMillis() / 1000L,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        val profile = saveManager.activeProfile ?: return 0
        val weekStartUnix = computeMondayMidnightUnix(nowUnix, timeZone)
        return profile.recentSessions.count { it.timestampUnix >= weekStartUnix }
    }

    fun getTopWeakQuestions(limit: Int = 5): List<WeakQuestionItem> {
        val profile = saveManager.activeProfile ?: return emptyList()
        val allPacks = packManager.getAllPacks(includeArchived = true)
        val questionLookup = mutableMapOf<String, Pair<PackData, QuestionData>>()
        for (pack in allPacks) {
            for (q in pack.playableQuestions) {
                questionLookup[q.id] = pack to q
            }
        }
        return profile.leitner.entries
            .filter { it.value.wrongTotal > 0 && questionLookup.containsKey(it.key) }
            .sortedWith(
                compareByDescending<Map.Entry<String, LeitnerRecord>> { it.value.wrongTotal }
                    .thenByDescending { it.value.lastWrongUnix }
                    .thenBy { it.value.box }
            )
            .take(limit)
            .map { (qId, rec) ->
                val (pack, q) = questionLookup.getValue(qId)
                WeakQuestionItem(
                    questionId = qId,
                    packTitle = pack.title,
                    prompt = TextFormatter.stripMarkers(q.prompt),
                    correctAnswerText = q.getCorrectAnswerDisplay(),
                    wrongTotal = rec.wrongTotal,
                    box = rec.box
                )
            }
    }

    fun completeMinigameSession(
        packId: String,
        minigameId: String,
        isBoss: Boolean,
        victory: Boolean,
        correctFirstTry: Int,
        totalQuestions: Int,
        helpsUsed: Int,
        rawAnswerStars: Int,
        masteryBefore: Float,
        nowUnix: Long = System.currentTimeMillis() / 1000L
    ): MinigameResult {
        val pack = packManager.getPack(packId)
        val alreadyHasMedal = hasMedal(packId)
        val isFirstBossWin = isBoss && victory && !alreadyHasMedal

        val completionBonus = when {
            !victory -> 0
            isFirstBossWin -> GameBalance.STARS_BOSS_VICTORY_BONUS
            else -> GameBalance.STARS_SESSION_COMPLETE
        }
        val baseTotalStars = rawAnswerStars + completionBonus
        val priorityApplied = pack?.priority == true
        val finalStars = if (priorityApplied) {
            (baseTotalStars * GameBalance.PRIORITY_MULTIPLIER).roundToInt()
        } else {
            baseTotalStars
        }

        val medalUnlocked = isFirstBossWin
        val masteryAfter = getPackMasteryPercent(packId)

        if (victory) {
            sessionsInCurrentRun++
        }
        val suggestBreak = victory &&
            sessionsInCurrentRun > 0 &&
            sessionsInCurrentRun % GameBalance.BREAK_REMINDER_SESSIONS == 0

        saveManager.updateActiveProfile { profile ->
            val newMedals = if (medalUnlocked && packId !in profile.medals) {
                profile.medals + packId
            } else {
                profile.medals
            }
            val newSession = SessionRecord(
                timestampUnix = nowUnix,
                packId = packId,
                packTitle = pack?.title ?: packId,
                minigameId = minigameId,
                isBoss = isBoss,
                victory = victory,
                correctFirstTry = correctFirstTry,
                totalQuestions = totalQuestions,
                starsEarned = finalStars
            )
            profile.copy(
                stars = profile.stars + finalStars,
                medals = newMedals,
                recentSessions = profile.recentSessions + newSession
            )
        }

        return MinigameResult(
            packId = packId,
            minigameId = minigameId,
            isBoss = isBoss,
            victory = victory,
            correctFirstTry = correctFirstTry,
            totalQuestions = totalQuestions,
            helpsUsed = helpsUsed,
            starsEarned = finalStars,
            priorityBonusApplied = priorityApplied,
            medalUnlocked = medalUnlocked,
            suggestMicroBreak = suggestBreak,
            masteryBefore = masteryBefore,
            masteryAfter = masteryAfter
        )
    }

    fun awardEarlyExitStars(packId: String, rawAnswerStars: Int) {
        if (rawAnswerStars <= 0) {
            saveManager.flushAll()
            return
        }
        val pack = packManager.getPack(packId)
        val finalStars = if (pack?.priority == true) {
            (rawAnswerStars * GameBalance.PRIORITY_MULTIPLIER).roundToInt()
        } else {
            rawAnswerStars
        }
        saveManager.updateActiveProfile { profile ->
            profile.copy(stars = profile.stars + finalStars)
        }
        saveManager.flushAll()
    }

    fun getDailyMissions(
        nowUnix: Long = System.currentTimeMillis() / 1000L,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<DailyMissionItem> {
        val profile = saveManager.activeProfile
        val dayKey = computeDayKey(nowUnix, timeZone)
        val dayStartUnix = computeDayMidnightUnix(nowUnix, timeZone)
        val todaySessions = profile?.recentSessions
            ?.filter { it.timestampUnix >= dayStartUnix }
            .orEmpty()
        val claimed = profile?.claimedMissionKeys?.toSet().orEmpty()

        val sessionsCount = todaySessions.size
        val distinctSubjects = todaySessions.mapNotNull { s ->
            packManager.getPack(s.packId)?.subject ?: s.minigameId
        }.distinct().size
        val firstTryToday = todaySessions.sumOf { it.correctFirstTry }

        val m1Key = "${dayKey}_first_challenge"
        val m2Key = "${dayKey}_two_subjects"
        val m3Key = "${dayKey}_sharp_answers"

        return listOf(
            DailyMissionItem(
                missionKey = m1Key,
                title = "Esploratore del Giorno",
                description = "Completa 1 sfida in qualsiasi aula oggi",
                progress = min(sessionsCount, 1),
                target = 1,
                rewardStars = 25,
                claimed = m1Key in claimed
            ),
            DailyMissionItem(
                missionKey = m2Key,
                title = "Giro delle Aule",
                description = "Allenati in 2 materie diverse oggi",
                progress = min(distinctSubjects, 2),
                target = 2,
                rewardStars = 40,
                claimed = m2Key in claimed
            ),
            DailyMissionItem(
                missionKey = m3Key,
                title = "Colpo da Maestro",
                description = "Indovina 4 domande al primo colpo oggi",
                progress = min(firstTryToday, 4),
                target = 4,
                rewardStars = 35,
                claimed = m3Key in claimed
            )
        )
    }

    fun claimDailyMission(
        missionKey: String,
        nowUnix: Long = System.currentTimeMillis() / 1000L,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        val mission = getDailyMissions(nowUnix, timeZone).firstOrNull { it.missionKey == missionKey }
            ?: return 0
        if (!mission.isClaimable) return 0
        saveManager.updateActiveProfile { p ->
            if (missionKey in p.claimedMissionKeys) return@updateActiveProfile p
            p.copy(
                stars = p.stars + mission.rewardStars,
                claimedMissionKeys = (p.claimedMissionKeys + missionKey).takeLast(30)
            )
        }
        return mission.rewardStars
    }

    fun getSubjectMasteryMap(): Map<String, SubjectMasterySummary> {
        val activePacks = packManager.getActivePacks()
        val medals = saveManager.activeProfile?.medals?.toSet().orEmpty()
        val subjects = listOf("storia", "matematica", "scienze", "italiano", "geografia", "inglese")
        return subjects.associateWith { subj ->
            val subjPacks = activePacks.filter { it.subject == subj }
            val avgMastery = if (subjPacks.isEmpty()) {
                0f
            } else {
                subjPacks.map { getPackMasteryPercent(it) }.average().toFloat()
            }
            SubjectMasterySummary(
                subject = subj,
                averageMasteryPct = avgMastery,
                rank = rankForMastery(avgMastery),
                packCount = subjPacks.size,
                medalsEarned = subjPacks.count { it.packId in medals },
                hasPriority = subjPacks.any { it.priority }
            )
        }
    }

    fun getOverallExplorerRank(): MasteryRankInfo {
        val map = getSubjectMasteryMap()
        val activeSubjects = map.values.filter { it.packCount > 0 }
        val avg = if (activeSubjects.isEmpty()) 0f else activeSubjects.map { it.averageMasteryPct }.average().toFloat()
        val medalsCount = saveManager.activeProfile?.medals?.size ?: 0
        val boosted = (avg + medalsCount * 8f).coerceAtMost(100f)
        return rankForMastery(boosted)
    }

    companion object {
        fun rankForMastery(pct: Float): MasteryRankInfo = when {
            pct >= 80f -> MasteryRankInfo("Maestro di Pixel", 3, 80)
            pct >= 50f -> MasteryRankInfo("Esploratore", 2, 50)
            pct >= 20f -> MasteryRankInfo("Apprendista", 1, 20)
            else -> MasteryRankInfo("Curioso", 0, 0)
        }

        fun computeDayKey(
            nowUnix: Long,
            timeZone: TimeZone = TimeZone.getDefault()
        ): String {
            val cal = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowUnix * 1000L
            }
            return "%04d-%02d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun computeDayMidnightUnix(
            nowUnix: Long,
            timeZone: TimeZone = TimeZone.getDefault()
        ): Long {
            val cal = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowUnix * 1000L
            }
            val secondsSinceMidnight =
                cal.get(Calendar.HOUR_OF_DAY) * 3600L +
                    cal.get(Calendar.MINUTE) * 60L +
                    cal.get(Calendar.SECOND)
            return nowUnix - secondsSinceMidnight
        }

        fun minigameIdForSubject(subject: String): String = when (subject) {
            "matematica" -> "gym_rhythm"
            "scienze" -> "science_catapult"
            "italiano" -> "library_books"
            "geografia" -> "geo_balloon"
            "inglese" -> "english_match"
            else -> "yard_battle"
        }

        fun allowedTypesForSubject(subject: String): Set<String> =
            if (subject == "matematica") {
                setOf("numeric_input", "multiple_choice", "true_false")
            } else {
                setOf("multiple_choice", "true_false")
            }

        fun computeMondayMidnightUnix(
            nowUnix: Long,
            timeZone: TimeZone = TimeZone.getDefault()
        ): Long {
            val cal = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowUnix * 1000L
            }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2..Sat=7
            val daysSinceMonday = (dayOfWeek + 5) % 7
            val secondsSinceMidnight =
                cal.get(Calendar.HOUR_OF_DAY) * 3600L +
                    cal.get(Calendar.MINUTE) * 60L +
                    cal.get(Calendar.SECOND)
            return nowUnix - (daysSinceMonday * 86_400L) - secondsSinceMidnight
        }

        fun pickFiftyFiftyEliminations(
            optionCount: Int,
            correctDisplayIndex: Int,
            rng: Random = Random.Default
        ): Set<Int> {
            val wrongIndices = (0 until optionCount).filter { it != correctDisplayIndex }
            return wrongIndices.shuffled(rng).take(2).toSet()
        }

        fun isNumericAnswerCorrect(
            rawInput: String,
            correctValue: Double,
            tolerance: Double
        ): Boolean {
            val normalized = rawInput.trim().replace(',', '.')
            val parsed = normalized.toDoubleOrNull() ?: return false
            val eps = max(tolerance, GameBalance.NUMERIC_MIN_EPSILON)
            return abs(parsed - correctValue) <= eps
        }

        fun isBossGymVictory(
            allStepsCompleted: Boolean,
            correctFirstTry: Int,
            totalUniqueQuestions: Int
        ): Boolean {
            if (!allStepsCompleted || totalUniqueQuestions <= 0) return false
            val accuracy = correctFirstTry.toFloat() / totalUniqueQuestions.toFloat()
            return accuracy >= GameBalance.BOSS_MIN_FIRST_TRY_ACCURACY
        }

        /**
         * Builds 4 plausible, distinct, positive choices (1 correct + 3 distractors)
         * for numeric_input questions so Space Invaders works seamlessly on multiplication
         * tables, mental math, and unit conversions.
         */
        fun generateNumericChoices(
            q: QuestionData,
            rng: Random = Random.Default
        ): ShuffledOptions {
            val correct = q.correctValue
            val isInt = abs(correct - correct.roundToInt()) < 0.001
            val unitSuffix = if (q.unit.isNotBlank()) " ${q.unit}" else ""

            fun formatVal(v: Double): String {
                val base = if (isInt) {
                    v.roundToInt().toString()
                } else {
                    String.format(java.util.Locale.ITALY, "%.1f", v).removeSuffix(",0")
                }
                return "$base$unitSuffix"
            }

            val correctLabel = formatVal(correct)
            val distractors = linkedSetOf<String>()

            // Detect multiplication prompt like "7 x 8" or "7 × 8" for classic table distractors
            val cleanPrompt = TextFormatter.stripMarkers(q.prompt)
            val multMatch = Regex("(\\d+)\\s*[x×*]\\s*(\\d+)").find(cleanPrompt)
            val candidates = mutableListOf<Double>()
            if (multMatch != null && isInt) {
                val a = multMatch.groupValues[1].toIntOrNull() ?: 0
                val b = multMatch.groupValues[2].toIntOrNull() ?: 0
                if (a > 0 && b > 0) {
                    candidates.addAll(
                        listOf(
                            ((a - 1) * b).toDouble(),
                            ((a + 1) * b).toDouble(),
                            (a * (b - 1)).toDouble(),
                            (a * (b + 1)).toDouble(),
                            correct + 2.0,
                            correct - 2.0,
                            correct + 10.0,
                            correct - 10.0
                        )
                    )
                }
            }

            val mag = abs(correct)
            val offsets = when {
                !isInt -> listOf(0.5, -0.5, 1.0, -1.0, 1.5, 2.0)
                mag >= 500.0 -> listOf(100.0, -100.0, 500.0, -500.0, 1000.0, -200.0, 200.0)
                mag >= 100.0 -> listOf(10.0, -10.0, 50.0, -50.0, 100.0, -20.0, 20.0)
                mag >= 20.0 -> listOf(1.0, -1.0, 2.0, -2.0, 5.0, -5.0, 10.0, -10.0, 4.0, -4.0)
                else -> listOf(1.0, -1.0, 2.0, -2.0, 3.0, -3.0, 4.0, 5.0)
            }
            candidates.addAll(offsets.map { correct + it })
            candidates.shuffle(rng)

            for (cand in candidates) {
                if (distractors.size >= 3) break
                if (cand <= 0.0) continue
                if (abs(cand - correct) <= max(q.tolerance, 0.01)) continue
                val label = formatVal(cand)
                if (label != correctLabel) {
                    distractors.add(label)
                }
            }

            var fallbackStep = 1
            while (distractors.size < 3) {
                val cand = correct + fallbackStep * if (mag >= 100.0) 10.0 else 1.0
                val label = formatVal(cand)
                if (label != correctLabel) distractors.add(label)
                fallbackStep++
            }

            val all = (listOf(correctLabel) + distractors.take(3)).shuffled(rng)
            val correctIdx = all.indexOf(correctLabel).coerceAtLeast(0)
            return ShuffledOptions(options = all, correctDisplayIndex = correctIdx)
        }
    }
}
