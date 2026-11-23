package io.github.cybermelo.maestridipixel.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object TextFormatter {
    private val KEYWORD_REGEX = Regex("\\*\\*([^*]+)\\*\\*")
    private val BBCODE_REGEX = Regex("\\[(/?[a-zA-Z0-9=#_]+)\\]")

    fun stripMarkers(raw: String): String {
        return raw
            .replace("**", "")
            .replace(BBCODE_REGEX, "")
    }

    fun toBbcode(raw: String, highlightHex: String = GameBalance.KEYWORD_HIGHLIGHT_HEX): String {
        val escaped = raw.replace("[", "[lb]")
        return KEYWORD_REGEX.replace(escaped) { match ->
            "[b][color=$highlightHex]${match.groupValues[1]}[/color][/b]"
        }
    }

    fun toAnnotatedString(
        raw: String,
        highlightColor: Color = GameBalance.KEYWORD_HIGHLIGHT_COLOR
    ): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0
            for (match in KEYWORD_REGEX.findAll(raw)) {
                if (match.range.first > currentIndex) {
                    append(raw.substring(currentIndex, match.range.first))
                }
                withStyle(
                    SpanStyle(
                        color = highlightColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                ) {
                    append(match.groupValues[1])
                }
                currentIndex = match.range.last + 1
            }
            if (currentIndex < raw.length) {
                append(raw.substring(currentIndex))
            }
        }
    }
}
