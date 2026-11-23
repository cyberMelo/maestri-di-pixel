package io.github.cybermelo.maestridipixel.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class TTSManager(
    context: Context,
    private val audioManager: AudioManager
) {
    private var tts: TextToSpeech? = null
    private var isReady: Boolean = false
    private var italianResolved: Boolean = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isReady = true
                    resolveItalianVoiceLazy()
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            audioManager.setMusicDucked(true)
                        }

                        override fun onDone(utteranceId: String?) {
                            audioManager.setMusicDucked(false)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            audioManager.setMusicDucked(false)
                        }
                    })
                }
            }
        } catch (_: Throwable) {
            isReady = false
        }
    }

    private fun resolveItalianVoiceLazy() {
        val engine = tts ?: return
        if (italianResolved) return
        try {
            val voices = engine.voices
            if (voices != null && voices.isNotEmpty()) {
                val itVoice = voices.firstOrNull { v ->
                    v.locale?.language?.lowercase()?.startsWith("it") == true &&
                        !v.isNetworkConnectionRequired
                } ?: voices.firstOrNull { v ->
                    v.locale?.language?.lowercase()?.startsWith("it") == true
                }
                if (itVoice != null) {
                    engine.voice = itVoice
                    italianResolved = true
                    return
                }
            }
            val res = engine.setLanguage(Locale.ITALY)
            if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                italianResolved = true
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    private fun resolveVoiceForLocale(languagePrefix: String, fallbackLocale: Locale) {
        val engine = tts ?: return
        try {
            val voices = engine.voices
            if (voices != null && voices.isNotEmpty()) {
                val chosen = voices.firstOrNull { v ->
                    v.locale?.language?.lowercase()?.startsWith(languagePrefix) == true &&
                        !v.isNetworkConnectionRequired
                } ?: voices.firstOrNull { v ->
                    v.locale?.language?.lowercase()?.startsWith(languagePrefix) == true
                }
                if (chosen != null) {
                    engine.voice = chosen
                    return
                }
            }
            engine.setLanguage(fallbackLocale)
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    fun speak(rawText: String) {
        val clean = TextFormatter.stripMarkers(rawText).trim()
        if (clean.isEmpty()) return
        val engine = tts ?: return
        if (!isReady) return
        try {
            resolveVoiceForLocale("it", Locale.ITALY)
            italianResolved = true
            engine.setSpeechRate(0.95f)
            audioManager.setMusicDucked(true)
            val id = "sr_${UUID.randomUUID()}"
            engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null, id)
        } catch (_: Throwable) {
            audioManager.setMusicDucked(false)
        }
    }

    fun speakEnglish(rawText: String) {
        val clean = TextFormatter.stripMarkers(rawText).trim()
        if (clean.isEmpty()) return
        val engine = tts ?: return
        if (!isReady) return
        try {
            resolveVoiceForLocale("en", Locale.UK)
            italianResolved = false
            engine.setSpeechRate(0.88f)
            audioManager.setMusicDucked(true)
            val id = "sr_en_${UUID.randomUUID()}"
            engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null, id)
        } catch (_: Throwable) {
            audioManager.setMusicDucked(false)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
        audioManager.setMusicDucked(false)
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        tts = null
        isReady = false
    }
}
