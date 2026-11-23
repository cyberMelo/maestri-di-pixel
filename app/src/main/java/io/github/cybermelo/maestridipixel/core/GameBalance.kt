package io.github.cybermelo.maestridipixel.core

import androidx.compose.ui.graphics.Color

object GameBalance {
    // Sessions & Adaptive Length
    val SESSION_LENGTH_OPTIONS: List<Int> = listOf(4, 6, 8)
    const val DEFAULT_SESSION_QUESTIONS: Int = 6
    const val STUDIO_SERENO_SESSION_QUESTIONS: Int = 4
    const val BOSS_EXTRA_QUESTIONS: Int = 2
    const val BOSS_UNLOCK_MASTERY_PCT: Float = 80.0f
    const val BOSS_MIN_FIRST_TRY_ACCURACY: Float = 0.75f
    const val REQUEUE_DELAY_MIN: Int = 2
    const val REQUEUE_DELAY_MAX: Int = 3
    const val MAX_RETRIES_PER_QUESTION: Int = 2
    const val BREAK_REMINDER_SESSIONS: Int = 2

    // Leitner 4-Box (0, 1, 2, 3)
    const val LEITNER_MAX_BOX: Int = 3
    val BOX_COOLDOWN_SECONDS: Map<Int, Long> = mapOf(
        0 to 0L,
        1 to 0L,
        2 to 172_800L,  // 2 days
        3 to 604_800L   // 7 days
    )

    // Star Economy
    const val STARS_PER_CORRECT: Int = 10
    const val STARS_FIRST_TRY_BONUS: Int = 5
    const val STARS_SESSION_COMPLETE: Int = 25
    const val STARS_BOSS_VICTORY_BONUS: Int = 60
    const val PRIORITY_MULTIPLIER: Float = 1.5f
    const val WEEKLY_GOAL_SESSIONS: Int = 3
    const val MAX_RECENT_SESSIONS: Int = 30

    // Yard Battle
    const val PLAYER_MAX_HP: Int = 100
    const val BASE_ATTACK_DAMAGE: Int = 20
    const val SPECIAL_ATTACK_DAMAGE: Int = 36
    const val ENEMY_COUNTER_DAMAGE: Int = 20
    const val BOSS_COUNTER_DAMAGE: Int = 25

    fun enemyMaxHpForDeck(deckSize: Int): Int = (deckSize.coerceAtLeast(1)) * BASE_ATTACK_DAMAGE

    // Gym Space Invaders & Rhythm
    const val GYM_TIME_NUMERIC_SEC: Float = 14.0f
    const val GYM_TIME_CHOICE_SEC: Float = 12.0f
    const val INVADERS_DESCENT_SEC_STANDARD: Float = 13.5f
    const val INVADERS_DESCENT_SEC_BOSS: Float = 11.5f
    const val GYM_FAST_BONUS_RATIO: Float = 0.45f
    const val GYM_COMBO_SUPER_THRESHOLD: Int = 3
    const val NUMPAD_MAX_CHARS: Int = 8
    const val NUMERIC_MIN_EPSILON: Double = 0.0001

    // UI, Safe Area, Visual Comfort & Study Options
    const val SAFE_MARGIN_X_DP: Int = 16
    const val SAFE_MARGIN_Y_DP: Int = 12
    const val SCENE_TRANSITION_MS: Long = 260L
    const val EXPLANATION_UNLOCK_DELAY_MS: Long = 550L
    const val DEFAULT_READING_PAUSE_SEC: Float = 0.0f
    val READING_PAUSE_OPTIONS_SEC: List<Float> = listOf(0.0f, 1.0f, 2.0f)
    const val TTS_MUSIC_DUCK_DB: Float = -12.0f
    const val PARENT_HOLD_DURATION_MS: Long = 2000L
    const val DEFAULT_PARENT_PIN: String = "1234"
    const val PORTAL_COOLDOWN_MS: Long = 400L

    // World
    const val TILE_SIZE: Int = 16
    const val PLAYER_SPEED_TILES_PER_SEC: Float = 6.2f

    // Visual Comfort Palette
    val CARD_BG_COLOR: Color = Color(0xFFFAF8F2)
    val CARD_SURFACE_ALT: Color = Color(0xFFF2EDE2)
    val CARD_TEXT_COLOR: Color = Color(0xFF1E293B)
    val CARD_TEXT_MUTED: Color = Color(0xFF475569)
    val KEYWORD_HIGHLIGHT_COLOR: Color = Color(0xFF0F4C81)
    const val KEYWORD_HIGHLIGHT_HEX: String = "#0F4C81"
    val ERROR_WARM_COLOR: Color = Color(0xFFD97706)
    val SUCCESS_COLOR: Color = Color(0xFF059669)
    val HINT_BG_COLOR: Color = Color(0xFFE0F2FE)
    val HINT_BORDER_COLOR: Color = Color(0xFF0284C7)
    val GOLD_COLOR: Color = Color(0xFFF59E0B)
    val GBA_FRAME_DARK: Color = Color(0xFF1E293B)

    val ENCOURAGING_ERROR_HEADERS: List<String> = listOf(
        "Quasi! Scopriamo il trucco:",
        "Ci sei vicino! Guarda qui:",
        "Nessun problema, impariamo questo:",
        "Ottimo tentativo! Ecco il segreto:"
    )

    val ALLOWED_SUBJECTS: Set<String> = setOf(
        "storia", "geografia", "scienze", "matematica", "italiano", "inglese"
    )

    val SUBJECT_COLORS: Map<String, Color> = mapOf(
        "storia" to Color(0xFFD97706),
        "geografia" to Color(0xFF059669),
        "scienze" to Color(0xFF0284C7),
        "matematica" to Color(0xFFDC2626),
        "italiano" to Color(0xFF7C3AED),
        "inglese" to Color(0xFFDB2777)
    )

    val SUBJECT_LABELS: Map<String, String> = mapOf(
        "storia" to "Storia",
        "geografia" to "Geografia",
        "scienze" to "Scienze",
        "matematica" to "Matematica",
        "italiano" to "Italiano",
        "inglese" to "Inglese"
    )

    val FRAME_COLORS: Map<String, Color> = mapOf(
        "frame_classic" to Color(0xFF1E293B),
        "frame_emerald" to Color(0xFF059669),
        "frame_gold" to Color(0xFFD97706),
        "frame_space" to Color(0xFF7C3AED)
    )

    fun subjectColor(subject: String): Color =
        SUBJECT_COLORS[subject.lowercase()] ?: Color(0xFF334155)

    fun subjectLabel(subject: String): String =
        SUBJECT_LABELS[subject.lowercase()] ?: subject.replaceFirstChar { it.uppercase() }

    fun frameColor(frameId: String): Color =
        FRAME_COLORS[frameId] ?: GBA_FRAME_DARK
}
