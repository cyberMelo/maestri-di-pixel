package com.example.core

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object MathGenerator {
    private fun deterministicSeed(packId: String, specId: String): Long {
        val key = "$packId::$specId"
        var h = 1125899906842597L
        for (ch in key) {
            h = 31L * h + ch.code.toLong()
        }
        return h
    }

    private fun paramInt(params: Map<String, Any>, key: String, default: Int): Int {
        return when (val v = params[key]) {
            is Int -> v
            is Long -> v.toInt()
            is Double -> v.toInt()
            is Number -> v.toInt()
            else -> default
        }
    }

    private fun paramBool(params: Map<String, Any>, key: String, default: Boolean): Boolean {
        return (params[key] as? Boolean) ?: default
    }

    private fun paramString(params: Map<String, Any>, key: String, default: String): String {
        return (params[key] as? String) ?: default
    }

    fun generatePool(packId: String, spec: GeneratorSpec): List<QuestionData> {
        val rng = Random(deterministicSeed(packId, spec.id))
        val result = mutableListOf<QuestionData>()
        for (idx in 0 until spec.poolSize) {
            val qId = String.format("%s_%02d", spec.id, idx)
            val question = when (spec.template) {
                "multiplication_table" -> generateMultiplication(qId, spec, rng)
                "mental_add_sub" -> generateMentalAddSub(qId, spec, rng)
                "unit_conversion" -> generateUnitConversion(qId, spec, rng)
                else -> generateMultiplication(qId, spec, rng)
            }
            result.add(question)
        }
        return result
    }

    private fun generateMultiplication(
        id: String,
        spec: GeneratorSpec,
        rng: Random
    ): QuestionData {
        val minA = max(1, paramInt(spec.params, "min_a", 2))
        val maxA = max(minA, paramInt(spec.params, "max_a", 9))
        val minB = max(1, paramInt(spec.params, "min_b", 2))
        val maxB = max(minB, paramInt(spec.params, "max_b", 9))
        val a = rng.nextInt(minA, maxA + 1)
        val b = rng.nextInt(minB, maxB + 1)
        val product = a * b
        val helperB = max(1, b - 1)
        return QuestionData(
            id = id,
            type = "numeric_input",
            difficulty = spec.difficulty,
            prompt = "Quanto fa **$a x $b**?",
            hint = "Parti da $a x $helperB e aggiungi ancora $a.",
            correctValue = product.toDouble(),
            unit = "",
            tolerance = 0.0,
            explanation = "**$a x $b** fa **$product**.",
            isGenerated = true,
            topic = spec.topic.ifBlank { "Tabelline" }
        )
    }

    private fun generateMentalAddSub(
        id: String,
        spec: GeneratorSpec,
        rng: Random
    ): QuestionData {
        val minVal = max(1, paramInt(spec.params, "min_val", 10))
        val maxVal = max(minVal + 5, paramInt(spec.params, "max_val", 90))
        val allowSub = paramBool(spec.params, "allow_sub", true)
        val isSub = allowSub && rng.nextBoolean()

        val rawA = rng.nextInt(minVal, maxVal + 1)
        val rawB = rng.nextInt(minVal, maxVal + 1)
        return if (isSub) {
            val high = max(rawA, rawB)
            val low = min(rawA, rawB)
            val diff = high - low
            QuestionData(
                id = id,
                type = "numeric_input",
                difficulty = spec.difficulty,
                prompt = "Quanto fa **$high - $low**?",
                hint = "Togli prima le decine di $low e poi le unità.",
                correctValue = diff.toDouble(),
                unit = "",
                tolerance = 0.0,
                explanation = "Calcolando per passi, **$high - $low = $diff**.",
                isGenerated = true,
                topic = spec.topic.ifBlank { "Calcolo Mentale" }
            )
        } else {
            val sum = rawA + rawB
            QuestionData(
                id = id,
                type = "numeric_input",
                difficulty = spec.difficulty,
                prompt = "Quanto fa **$rawA + $rawB**?",
                hint = "Somma prima le decine e poi aggiungi le unità.",
                correctValue = sum.toDouble(),
                unit = "",
                tolerance = 0.0,
                explanation = "Unendo decine e unità ottieni **$rawA + $rawB = $sum**.",
                isGenerated = true,
                topic = spec.topic.ifBlank { "Calcolo Mentale" }
            )
        }
    }

    private fun generateUnitConversion(
        id: String,
        spec: GeneratorSpec,
        rng: Random
    ): QuestionData {
        val family = paramString(spec.params, "family", "length")
        val value = rng.nextInt(1, 10)
        return when (family) {
            "mass" -> {
                val grams = value * 1000
                QuestionData(
                    id = id,
                    type = "numeric_input",
                    difficulty = spec.difficulty,
                    prompt = "Quanti **grammi** ci sono in **$value kg**?",
                    hint = "Ricorda: 1 chilogrammo vale 1000 grammi.",
                    correctValue = grams.toDouble(),
                    unit = "g",
                    tolerance = 0.0,
                    explanation = "Moltiplichi per 1000: **$value kg = $grams g**.",
                    isGenerated = true,
                    topic = spec.topic.ifBlank { "Misure" }
                )
            }
            "capacity" -> {
                val ml = value * 1000
                QuestionData(
                    id = id,
                    type = "numeric_input",
                    difficulty = spec.difficulty,
                    prompt = "Quanti **millilitri** ci sono in **$value litri**?",
                    hint = "Ricorda: 1 litro vale 1000 millilitri.",
                    correctValue = ml.toDouble(),
                    unit = "ml",
                    tolerance = 0.0,
                    explanation = "Ogni litro vale 1000 ml, quindi **$value l = $ml ml**.",
                    isGenerated = true,
                    topic = spec.topic.ifBlank { "Misure" }
                )
            }
            else -> {
                val cm = value * 100
                QuestionData(
                    id = id,
                    type = "numeric_input",
                    difficulty = spec.difficulty,
                    prompt = "Quanti **centimetri** ci sono in **$value metri**?",
                    hint = "Ricorda: 1 metro vale 100 centimetri.",
                    correctValue = cm.toDouble(),
                    unit = "cm",
                    tolerance = 0.0,
                    explanation = "Ogni metro vale 100 cm, quindi **$value m = $cm cm**.",
                    isGenerated = true,
                    topic = spec.topic.ifBlank { "Misure" }
                )
            }
        }
    }
}
