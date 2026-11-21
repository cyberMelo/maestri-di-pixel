package com.example.core

import java.util.Locale

data class QuestionData(
    val id: String,
    val type: String, // "multiple_choice", "true_false", "numeric_input", "classification"
    val difficulty: Int, // 1, 2, 3
    val prompt: String,
    val hint: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val correctBool: Boolean = false,
    val correctValue: Double = 0.0,
    val unit: String = "",
    val tolerance: Double = 0.0,
    val explanation: String,
    val isGenerated: Boolean = false,
    val topic: String = ""
) {
    fun getCorrectAnswerDisplay(): String = when (type) {
        "multiple_choice" -> options.getOrElse(correctIndex) { "" }
        "true_false" -> if (correctBool) "Vero" else "Falso"
        "numeric_input" -> {
            val formatted = if (correctValue == correctValue.toLong().toDouble()) {
                correctValue.toLong().toString()
            } else {
                String.format(Locale.ITALY, "%.2f", correctValue).trimEnd('0').trimEnd(',')
            }
            if (unit.isNotBlank()) "$formatted $unit" else formatted
        }
        else -> ""
    }

    fun getEffectiveHint(): String {
        if (hint.isNotBlank()) return hint
        if (type == "numeric_input") {
            val digits = if (correctValue == correctValue.toLong().toDouble()) {
                correctValue.toLong().toString().length
            } else {
                correctValue.toString().replace(".", "").length
            }
            return "Suggerimento: il numero ha $digits cifre."
        }
        return ""
    }
}

data class GeneratorSpec(
    val id: String,
    val template: String, // "multiplication_table", "mental_add_sub", "unit_conversion"
    val difficulty: Int,
    val params: Map<String, Any>,
    val poolSize: Int,
    val topic: String = ""
)

data class PackData(
    val packId: String,
    val subject: String,
    val grade: Int,
    val title: String,
    val version: Int,
    val bossName: String,
    val bossTitle: String,
    val medalName: String,
    val medalIcon: String,
    val source: String = "bundled", // "bundled", "github", "local"
    val priority: Boolean = false,
    val archived: Boolean = false,
    val staticQuestions: List<QuestionData> = emptyList(),
    val generators: List<GeneratorSpec> = emptyList(),
    val playableQuestions: List<QuestionData> = emptyList()
) {
    fun getTopics(): List<String> = playableQuestions
        .map { it.topic.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}

data class ManifestEntry(
    val packId: String,
    val file: String,
    val subject: String,
    val title: String,
    val version: Int,
    val priority: Boolean = false,
    val archived: Boolean = false
)

data class ValidationResult(
    val isValid: Boolean = true,
    val errors: MutableList<String> = mutableListOf(),
    val warnings: MutableList<String> = mutableListOf()
) {
    fun addError(msg: String): ValidationResult {
        errors.add(msg)
        return copy(isValid = false, errors = errors, warnings = warnings)
    }

    fun addWarning(msg: String): ValidationResult {
        warnings.add(msg)
        return copy(isValid = isValid, errors = errors, warnings = warnings)
    }
}

data class MinigameLaunchConfig(
    val packId: String,
    val minigameId: String, // "yard_battle", "gym_rhythm", "science_catapult", "library_books", "geo_balloon", "english_match"
    val isBoss: Boolean = false,
    val returnScene: String = "school_yard",
    val returnSpawnId: String = "default"
)

data class MinigameResult(
    val packId: String,
    val minigameId: String,
    val isBoss: Boolean,
    val victory: Boolean,
    val correctFirstTry: Int,
    val totalQuestions: Int,
    val helpsUsed: Int,
    val starsEarned: Int,
    val priorityBonusApplied: Boolean,
    val medalUnlocked: Boolean,
    val suggestMicroBreak: Boolean,
    val masteryBefore: Float,
    val masteryAfter: Float
)

data class ShopItem(
    val id: String,
    val category: String, // "companion", "sticker", "frame"
    val name: String,
    val price: Int,
    val icon: String = "",
    val colorHex: String = ""
)

data class AudioConfig(
    val masterMuted: Boolean = false,
    val musicVolume: Float = 0.7f,
    val sfxVolume: Float = 0.9f
)

data class StudyPreferences(
    val calmMode: Boolean = false,
    val reducedMotion: Boolean = false,
    val readingPauseSec: Float = GameBalance.DEFAULT_READING_PAUSE_SEC,
    val sessionLength: Int = GameBalance.DEFAULT_SESSION_QUESTIONS,
    val enable5050: Boolean = false,
    val showVirtualDpad: Boolean = true,
    val showGuidancePill: Boolean = true,
    val autoTtsQuestions: Boolean = false
)

data class GitHubConfig(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val basePath: String = "packs",
    val pat: String = ""
)

data class SyncState(
    val lastSyncUnix: Long = 0L,
    val lastSyncStatus: String = "never", // "never", "ok", "offline", "error", "not_configured"
    val lastSyncMessage: String = "",
    val lastSyncErrors: List<String> = emptyList()
)

data class AppConfig(
    val schemaVersion: Int = 1,
    val activeProfileId: String = "",
    val parentPin: String = GameBalance.DEFAULT_PARENT_PIN,
    val audio: AudioConfig = AudioConfig(),
    val studyPreferences: StudyPreferences = StudyPreferences(),
    val priorityOverrides: Map<String, Boolean> = emptyMap(),
    val focusTopics: Map<String, List<String>> = emptyMap(),
    val github: GitHubConfig = GitHubConfig(),
    val syncState: SyncState = SyncState()
)

data class LeitnerRecord(
    val box: Int = 0,
    val lastSeenUnix: Long = 0L,
    val correctTotal: Int = 0,
    val wrongTotal: Int = 0,
    val lastWrongUnix: Long = 0L
)

data class SessionRecord(
    val timestampUnix: Long,
    val packId: String,
    val packTitle: String,
    val minigameId: String,
    val isBoss: Boolean,
    val victory: Boolean,
    val correctFirstTry: Int,
    val totalQuestions: Int,
    val starsEarned: Int
)

data class ProfileData(
    val schemaVersion: Int = 1,
    val profileId: String,
    val name: String,
    val avatarId: Int = 0,
    val stars: Int = 0,
    val equippedCompanion: String = "",
    val equippedFrame: String = "frame_classic",
    val ownedItems: List<String> = listOf("frame_classic"),
    val lockerStickers: List<String> = listOf("", "", "", "", "", ""),
    val medals: List<String> = emptyList(),
    val leitner: Map<String, LeitnerRecord> = emptyMap(),
    val recentSessions: List<SessionRecord> = emptyList()
)

data class ProfileSummary(
    val profileId: String,
    val name: String,
    val avatarId: Int,
    val stars: Int
)

data class RecommendedActivity(
    val packId: String,
    val title: String,
    val subject: String,
    val isBoss: Boolean,
    val targetLocationLabel: String,
    val minigameId: String
)

data class WeakQuestionItem(
    val questionId: String,
    val packTitle: String,
    val prompt: String,
    val correctAnswerText: String,
    val wrongTotal: Int,
    val box: Int
)

data class ShuffledOptions(
    val options: List<String>,
    val correctDisplayIndex: Int
)

data class SyncResult(
    val status: String, // "ok", "offline", "error", "not_configured", "busy"
    val message: String,
    val errors: List<String> = emptyList()
)
