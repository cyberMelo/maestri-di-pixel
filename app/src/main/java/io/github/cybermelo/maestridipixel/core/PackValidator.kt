package io.github.cybermelo.maestridipixel.core

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.floor

object PackValidator {
    private val PACK_ID_REGEX = Regex("^[a-z0-9_]{3,48}$")
    private val QUESTION_ID_REGEX = Regex("^[a-z0-9_]{3,64}$")
    private val SAFE_MANIFEST_FILE_REGEX = Regex("^[a-zA-Z0-9_\\-/]+\\.json$")
    private val ALLOWED_QUESTION_TYPES = setOf(
        "multiple_choice", "true_false", "numeric_input", "classification"
    )
    private val ALLOWED_GENERATOR_TEMPLATES = setOf(
        "multiplication_table", "mental_add_sub", "unit_conversion"
    )

    private fun isWholeNumber(value: Any?): Int? {
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> if (value == floor(value) && value >= Int.MIN_VALUE && value <= Int.MAX_VALUE) value.toInt() else null
            is Float -> if (value == floor(value)) value.toInt() else null
            else -> null
        }
    }

    private fun toDoubleValue(value: Any?): Double? {
        return when (value) {
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Double -> if (!value.isNaN() && !value.isInfinite()) value else null
            is Float -> if (!value.isNaN() && !value.isInfinite()) value.toDouble() else null
            else -> null
        }
    }

    fun validateManifest(obj: JSONObject): ValidationResult {
        var result = ValidationResult()
        val schemaVersion = isWholeNumber(obj.opt("schema_version"))
        if (schemaVersion != 1) {
            result = result.addError("manifest.json: schema_version deve essere 1.")
        }

        val packsAny = obj.opt("packs")
        if (packsAny !is JSONArray) {
            result = result.addError("manifest.json: campo 'packs' mancante o non è un array.")
            return result
        }

        val seenIds = mutableSetOf<String>()
        for (i in 0 until packsAny.length()) {
            val entry = packsAny.opt(i)
            if (entry !is JSONObject) {
                result = result.addError("manifest.json: packs[$i] non è un oggetto valido.")
                continue
            }
            val packId = entry.opt("pack_id") as? String ?: ""
            if (!PACK_ID_REGEX.matches(packId)) {
                result = result.addError("manifest.json: packs[$i].pack_id '$packId' non valido.")
            } else if (!seenIds.add(packId)) {
                result = result.addError("manifest.json: pack_id duplicato '$packId'.")
            }

            val file = entry.opt("file") as? String ?: ""
            if (file.isBlank() ||
                file.startsWith("/") ||
                file.contains("..") ||
                file.contains("\\") ||
                file.contains(":") ||
                !SAFE_MANIFEST_FILE_REGEX.matches(file)
            ) {
                result = result.addError("manifest.json: packs[$i].file '$file' non consentito.")
            }

            val subject = entry.opt("subject") as? String ?: ""
            if (subject !in GameBalance.ALLOWED_SUBJECTS) {
                result = result.addError("manifest.json: packs[$i].subject '$subject' non valido.")
            }

            val title = (entry.opt("title") as? String ?: "").trim()
            if (title.length !in 3..60) {
                result = result.addError("manifest.json: packs[$i].title deve avere tra 3 e 60 caratteri.")
            }

            val version = isWholeNumber(entry.opt("version"))
            if (version == null || version < 1) {
                result = result.addError("manifest.json: packs[$i].version deve essere intero >= 1.")
            }

            if (entry.has("priority") && entry.opt("priority") !is Boolean) {
                result = result.addError("manifest.json: packs[$i].priority deve essere booleano.")
            }
            if (entry.has("archived") && entry.opt("archived") !is Boolean) {
                result = result.addError("manifest.json: packs[$i].archived deve essere booleano.")
            }
        }
        return result
    }

    fun validatePack(obj: JSONObject, filename: String = "pack.json"): ValidationResult {
        var result = ValidationResult()
        val schemaVersion = isWholeNumber(obj.opt("schema_version"))
        if (schemaVersion != 1) {
            result = result.addError("$filename: schema_version deve essere 1.")
        }

        val packId = obj.opt("pack_id") as? String ?: ""
        if (!PACK_ID_REGEX.matches(packId)) {
            result = result.addError("$filename: pack_id '$packId' non valido (usa 3-48 caratteri a-z, 0-9, _).")
        }

        val subject = obj.opt("subject") as? String ?: ""
        if (subject !in GameBalance.ALLOWED_SUBJECTS) {
            result = result.addError("$filename: subject '$subject' non supportato.")
        }

        val grade = isWholeNumber(obj.opt("grade"))
        if (grade == null || grade !in setOf(4, 5)) {
            result = result.addError("$filename: grade deve essere 4 oppure 5.")
        }

        val title = (obj.opt("title") as? String ?: "").trim()
        if (title.length !in 3..60) {
            result = result.addError("$filename: title deve avere tra 3 e 60 caratteri.")
        }

        val bossName = (obj.opt("boss_name") as? String ?: "").trim()
        if (bossName.isEmpty() || bossName.length > 40) {
            result = result.addError("$filename: boss_name obbligatorio (max 40 caratteri).")
        }

        val medalName = (obj.opt("medal_name") as? String ?: "").trim()
        if (medalName.isEmpty() || medalName.length > 40) {
            result = result.addError("$filename: medal_name obbligatorio (max 40 caratteri).")
        }

        val version = isWholeNumber(obj.opt("version"))
        if (version == null || version < 1) {
            result = result.addError("$filename: version deve essere intero >= 1.")
        }

        val seenQuestionIds = mutableSetOf<String>()
        var playableCount = 0
        var choiceOrTfCount = 0

        val questionsAny = obj.opt("questions")
        if (questionsAny != null && questionsAny !is JSONArray) {
            result = result.addError("$filename: 'questions' deve essere un array.")
        }
        val questionsArr = questionsAny as? JSONArray ?: JSONArray()

        for (i in 0 until questionsArr.length()) {
            val q = questionsArr.opt(i)
            if (q !is JSONObject) {
                result = result.addError("$filename: questions[$i] non è un oggetto valido.")
                continue
            }
            val qId = q.opt("id") as? String ?: ""
            if (!QUESTION_ID_REGEX.matches(qId)) {
                result = result.addError("$filename: questions[$i].id '$qId' non valido.")
            } else if (!seenQuestionIds.add(qId)) {
                result = result.addError("$filename: id domanda duplicato '$qId'.")
            }

            val qType = q.opt("type") as? String ?: ""
            if (qType !in ALLOWED_QUESTION_TYPES) {
                result = result.addError("$filename: questions[$i].type '$qType' non valido.")
                continue
            }
            if (qType == "classification") {
                result = result.addWarning("$filename: questions[$i] di tipo 'classification' non è ancora attiva nel Pilota 1.")
            }

            val difficulty = isWholeNumber(q.opt("difficulty"))
            if (difficulty == null || difficulty !in 1..3) {
                result = result.addError("$filename: questions[$i].difficulty deve essere 1, 2 o 3.")
            }

            val prompt = q.opt("prompt") as? String ?: ""
            val cleanPrompt = TextFormatter.stripMarkers(prompt).trim()
            if (cleanPrompt.isEmpty() || cleanPrompt.length > 140) {
                result = result.addError("$filename: questions[$i].prompt obbligatorio e max 140 caratteri (senza **).")
            }

            val explanation = q.opt("explanation") as? String ?: ""
            val cleanExpl = TextFormatter.stripMarkers(explanation).trim()
            if (cleanExpl.isEmpty() || cleanExpl.length > 160) {
                result = result.addError("$filename: questions[$i].explanation obbligatoria e max 160 caratteri (senza **).")
            }

            if (q.has("hint") && !q.isNull("hint")) {
                val hint = q.opt("hint") as? String
                if (hint == null) {
                    result = result.addError("$filename: questions[$i].hint deve essere una stringa.")
                } else {
                    val cleanHint = TextFormatter.stripMarkers(hint).trim()
                    if (cleanHint.length > 90) {
                        result = result.addError("$filename: questions[$i].hint supera 90 caratteri (senza **).")
                    }
                }
            }

            if (q.has("topic") && !q.isNull("topic")) {
                val topicVal = q.opt("topic") as? String
                if (topicVal == null || topicVal.trim().length > 40) {
                    result = result.addError("$filename: questions[$i].topic deve essere una stringa (max 40 caratteri).")
                }
            }

            when (qType) {
                "multiple_choice" -> {
                    val opts = q.opt("options") as? JSONArray
                    if (opts == null || opts.length() != 4) {
                        result = result.addError("$filename: questions[$i].options deve contenere esattamente 4 scelte.")
                    } else {
                        val seenOpts = mutableSetOf<String>()
                        for (k in 0 until 4) {
                            val optText = (opts.opt(k) as? String ?: "").trim()
                            if (optText.isEmpty() || optText.length > 45) {
                                result = result.addError("$filename: questions[$i].options[$k] deve avere 1-45 caratteri.")
                            } else if (!seenOpts.add(optText.lowercase())) {
                                result = result.addError("$filename: questions[$i] contiene opzioni duplicate.")
                            }
                        }
                    }
                    val correctIdx = isWholeNumber(q.opt("correct_index"))
                    if (correctIdx == null || correctIdx !in 0..3) {
                        result = result.addError("$filename: questions[$i].correct_index deve essere tra 0 e 3.")
                    }
                    playableCount++
                    choiceOrTfCount++
                }
                "true_false" -> {
                    if (q.opt("correct_bool") !is Boolean) {
                        result = result.addError("$filename: questions[$i].correct_bool deve essere booleano.")
                    }
                    playableCount++
                    choiceOrTfCount++
                }
                "numeric_input" -> {
                    val correctVal = toDoubleValue(q.opt("correct_value"))
                    if (correctVal == null || correctVal < 0.0) {
                        result = result.addError("$filename: questions[$i].correct_value deve essere un numero >= 0.")
                    }
                    if (q.has("tolerance") && !q.isNull("tolerance")) {
                        val tol = toDoubleValue(q.opt("tolerance"))
                        if (tol == null || tol < 0.0) {
                            result = result.addError("$filename: questions[$i].tolerance deve essere >= 0.")
                        }
                    }
                    if (q.has("unit") && !q.isNull("unit")) {
                        val unit = q.opt("unit") as? String
                        if (unit == null || unit.length > 12) {
                            result = result.addError("$filename: questions[$i].unit max 12 caratteri.")
                        }
                    }
                    playableCount++
                }
            }
        }

        val generatorsAny = obj.opt("generators")
        if (generatorsAny != null && generatorsAny !is JSONArray) {
            result = result.addError("$filename: 'generators' deve essere un array.")
        }
        val generatorsArr = generatorsAny as? JSONArray ?: JSONArray()
        for (j in 0 until generatorsArr.length()) {
            val g = generatorsArr.opt(j)
            if (g !is JSONObject) {
                result = result.addError("$filename: generators[$j] non è un oggetto valido.")
                continue
            }
            val gId = g.opt("id") as? String ?: ""
            if (!QUESTION_ID_REGEX.matches(gId)) {
                result = result.addError("$filename: generators[$j].id '$gId' non valido.")
            } else if (!seenQuestionIds.add(gId)) {
                result = result.addError("$filename: generators[$j].id '$gId' in collisione con altro ID.")
            }

            val template = g.opt("template") as? String ?: ""
            if (template !in ALLOWED_GENERATOR_TEMPLATES) {
                result = result.addError("$filename: generators[$j].template '$template' non valido.")
            }
            val diff = isWholeNumber(g.opt("difficulty"))
            if (diff == null || diff !in 1..3) {
                result = result.addError("$filename: generators[$j].difficulty deve essere 1, 2 o 3.")
            }
            val poolSize = isWholeNumber(g.opt("pool_size"))
            if (poolSize == null || poolSize !in 4..30) {
                result = result.addError("$filename: generators[$j].pool_size deve essere tra 4 e 30.")
            } else {
                playableCount += poolSize
            }
            if (g.has("topic") && !g.isNull("topic")) {
                val gTopic = g.opt("topic") as? String
                if (gTopic == null || gTopic.trim().length > 40) {
                    result = result.addError("$filename: generators[$j].topic deve essere una stringa (max 40 caratteri).")
                }
            }
        }

        if (playableCount < 5) {
            result = result.addError("$filename: servono almeno 5 domande giocabili (trovate $playableCount).")
        }

        if (subject != "matematica" && subject in GameBalance.ALLOWED_SUBJECTS && choiceOrTfCount < 5) {
            result = result.addWarning(
                "$filename: meno di 5 domande a scelta multipla o Vero/Falso per l'aula Cortile (trovate $choiceOrTfCount)."
            )
        }

        return result
    }

    fun parsePack(
        obj: JSONObject,
        source: String = "bundled",
        priority: Boolean = false,
        archived: Boolean = false
    ): PackData {
        val packId = obj.getString("pack_id")
        val subject = obj.getString("subject")
        val grade = obj.getInt("grade")
        val title = obj.getString("title").trim()
        val version = obj.getInt("version")
        val bossName = obj.getString("boss_name").trim()
        val bossTitle = obj.optString("boss_title", "Campione dell'Aula").trim()
        val medalName = obj.getString("medal_name").trim()
        val medalIcon = obj.optString("medal_icon", "medal_default").trim()

        val staticQuestions = mutableListOf<QuestionData>()
        val qArr = obj.optJSONArray("questions") ?: JSONArray()
        for (i in 0 until qArr.length()) {
            val q = qArr.getJSONObject(i)
            val type = q.getString("type")
            val opts = mutableListOf<String>()
            val optsArr = q.optJSONArray("options")
            if (optsArr != null) {
                for (k in 0 until optsArr.length()) {
                    opts.add(optsArr.getString(k).trim())
                }
            }
            staticQuestions.add(
                QuestionData(
                    id = q.getString("id"),
                    type = type,
                    difficulty = q.getInt("difficulty"),
                    prompt = q.getString("prompt"),
                    hint = q.optString("hint", ""),
                    options = opts,
                    correctIndex = q.optInt("correct_index", -1),
                    correctBool = q.optBoolean("correct_bool", false),
                    correctValue = q.optDouble("correct_value", 0.0),
                    unit = q.optString("unit", ""),
                    tolerance = q.optDouble("tolerance", 0.0),
                    explanation = q.getString("explanation"),
                    isGenerated = false,
                    topic = q.optString("topic", "").trim()
                )
            )
        }

        val generators = mutableListOf<GeneratorSpec>()
        val gArr = obj.optJSONArray("generators") ?: JSONArray()
        for (j in 0 until gArr.length()) {
            val g = gArr.getJSONObject(j)
            val paramsMap = mutableMapOf<String, Any>()
            val pObj = g.optJSONObject("params")
            if (pObj != null) {
                val keys = pObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    paramsMap[key] = pObj.get(key)
                }
            }
            generators.add(
                GeneratorSpec(
                    id = g.getString("id"),
                    template = g.getString("template"),
                    difficulty = g.getInt("difficulty"),
                    params = paramsMap,
                    poolSize = g.getInt("pool_size"),
                    topic = g.optString("topic", "").trim()
                )
            )
        }

        val generatedQuestions = generators.flatMap { spec ->
            MathGenerator.generatePool(packId, spec)
        }
        val playable = staticQuestions.filter { it.type != "classification" } + generatedQuestions

        return PackData(
            packId = packId,
            subject = subject,
            grade = grade,
            title = title,
            version = version,
            bossName = bossName,
            bossTitle = bossTitle,
            medalName = medalName,
            medalIcon = medalIcon,
            source = source,
            priority = priority,
            archived = archived,
            staticQuestions = staticQuestions,
            generators = generators,
            playableQuestions = playable
        )
    }

    fun parseManifestEntries(obj: JSONObject): List<ManifestEntry> {
        val list = mutableListOf<ManifestEntry>()
        val arr = obj.optJSONArray("packs") ?: return list
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            list.add(
                ManifestEntry(
                    packId = e.getString("pack_id"),
                    file = e.getString("file"),
                    subject = e.getString("subject"),
                    title = e.getString("title").trim(),
                    version = e.getInt("version"),
                    priority = e.optBoolean("priority", false),
                    archived = e.optBoolean("archived", false)
                )
            )
        }
        return list
    }
}
