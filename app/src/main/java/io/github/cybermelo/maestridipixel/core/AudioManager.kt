package io.github.cybermelo.maestridipixel.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import java.util.concurrent.atomic.AtomicInteger

enum class SfxType {
    UI_CLICK,
    UI_BACK,
    STEP,
    DOOR_OPEN,
    ENCOUNTER_START,
    ATTACK_HIT,
    ATTACK_SUPER,
    ANSWER_CORRECT,
    ANSWER_WRONG,
    STAR_GAIN,
    VICTORY_FANFARE,
    MEDAL_UNLOCK,
    LASER_SHOT,
    ALIEN_BOOM,
    SLINGSHOT_LAUNCH,
    TOWER_CRASH
}

enum class MusicTrack {
    NONE,
    HUB_THEME,
    BATTLE_THEME
}

class AudioManager(private val saveManager: SaveManager) {
    private val sampleRate = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val audioLock = Any()
    private val activeSfxCount = AtomicInteger(0)
    private var currentTrack: MusicTrack = MusicTrack.NONE
    private var musicJob: Job? = null
    private var musicAudioTrack: AudioTrack? = null
    private var isDucked: Boolean = false

    private val sfxCache: Map<SfxType, ShortArray> by lazy {
        SfxType.entries.associateWith { synthesizeSfx(it) }
    }

    private val musicCache: Map<MusicTrack, ShortArray> by lazy {
        mapOf(
            MusicTrack.HUB_THEME to synthesizeHubTheme(),
            MusicTrack.BATTLE_THEME to synthesizeBattleTheme()
        )
    }

    fun isMuted(): Boolean = saveManager.config.audio.masterMuted

    fun toggleMute(): Boolean {
        val next = !isMuted()
        saveManager.updateConfig { it.copy(audio = it.audio.copy(masterMuted = next)) }
        applyMusicVolume()
        return next
    }

    fun setVolumes(musicVol: Float, sfxVol: Float) {
        saveManager.updateConfig {
            it.copy(
                audio = it.audio.copy(
                    musicVolume = musicVol.coerceIn(0f, 1f),
                    sfxVolume = sfxVol.coerceIn(0f, 1f)
                )
            )
        }
        applyMusicVolume()
    }

    fun setMusicDucked(ducked: Boolean) {
        if (isDucked == ducked) return
        isDucked = ducked
        applyMusicVolume()
    }

    private fun effectiveMusicGain(): Float {
        val cfg = saveManager.config.audio
        if (cfg.masterMuted) return 0f
        val duckFactor = if (isDucked) {
            10.0.pow(GameBalance.TTS_MUSIC_DUCK_DB.toDouble() / 20.0).toFloat()
        } else {
            1.0f
        }
        return (cfg.musicVolume * duckFactor * 0.35f).coerceIn(0f, 1f)
    }

    private fun applyMusicVolume() {
        synchronized(audioLock) {
            try {
                val track = musicAudioTrack
                if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                    track.setVolume(effectiveMusicGain())
                }
            } catch (_: Throwable) {
                // Safe no-op in headless/test environment
            }
        }
    }

    fun playMusic(track: MusicTrack) {
        if (track == currentTrack && musicJob?.isActive == true) return
        stopMusic()
        currentTrack = track
        if (track == MusicTrack.NONE) return

        musicJob = scope.launch {
            try {
                val pcm = musicCache[track] ?: return@launch
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuf <= 0) return@launch
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, pcm.size * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                    audioTrack.release()
                    return@launch
                }

                audioTrack.write(pcm, 0, pcm.size)
                audioTrack.setLoopPoints(0, pcm.size, -1)
                audioTrack.setVolume(effectiveMusicGain())
                synchronized(audioLock) {
                    if (!isActive) {
                        audioTrack.release()
                        return@launch
                    }
                    musicAudioTrack = audioTrack
                    audioTrack.play()
                }

                while (isActive) {
                    delay(250)
                }
            } catch (_: Throwable) {
                // Safe no-op on Robolectric / devices without audio output
            }
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
        currentTrack = MusicTrack.NONE
        synchronized(audioLock) {
            try {
                musicAudioTrack?.let { t ->
                    if (t.state == AudioTrack.STATE_INITIALIZED && t.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        t.stop()
                    }
                    t.release()
                }
            } catch (_: Throwable) {
            }
            musicAudioTrack = null
        }
    }

    fun playSfx(type: SfxType) {
        val cfg = saveManager.config.audio
        if (cfg.masterMuted || cfg.sfxVolume <= 0.01f) return
        if (activeSfxCount.get() >= 4) return
        val gain = (cfg.sfxVolume * 0.55f).coerceIn(0f, 1f)
        activeSfxCount.incrementAndGet()
        scope.launch {
            var track: AudioTrack? = null
            try {
                val pcm = sfxCache[type] ?: return@launch
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuf <= 0) return@launch
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, pcm.size * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                if (track.state != AudioTrack.STATE_INITIALIZED) {
                    track.release()
                    return@launch
                }
                track.write(pcm, 0, pcm.size)
                track.setVolume(gain)
                track.play()
                val durationMs = ((pcm.size.toFloat() / sampleRate) * 1000f).toLong() + 40L
                delay(durationMs)
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
                track.release()
            } catch (_: Throwable) {
                try {
                    track?.release()
                } catch (_: Throwable) {
                }
            } finally {
                activeSfxCount.decrementAndGet()
            }
        }
    }

    fun pause() {
        synchronized(audioLock) {
            try {
                val track = musicAudioTrack
                if (track != null && track.state == AudioTrack.STATE_INITIALIZED && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun resume() {
        synchronized(audioLock) {
            try {
                val track = musicAudioTrack
                if (currentTrack != MusicTrack.NONE && track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                    track.setVolume(effectiveMusicGain())
                    track.play()
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun release() {
        stopMusic()
    }

    // Procedural 16-bit PCM Chiptune Synthesis
    private fun tone(
        freqHz: Double,
        durationSec: Double,
        square: Boolean = true,
        amp: Double = 0.45
    ): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = when {
                i < count * 0.08 -> i / (count * 0.08)
                i > count * 0.78 -> ((count - i) / (count * 0.22)).coerceAtLeast(0.0)
                else -> 1.0
            }
            val wave = if (square) {
                val s = sin(2.0 * PI * freqHz * t)
                if (s >= 0) 0.75 else -0.75
            } else {
                sin(2.0 * PI * freqHz * t)
            }
            out[i] = (wave * env * amp * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun concat(vararg arrays: ShortArray): ShortArray {
        val total = arrays.sumOf { it.size }
        val out = ShortArray(total)
        var pos = 0
        for (a in arrays) {
            a.copyInto(out, pos)
            pos += a.size
        }
        return out
    }

    private fun synthesizeSfx(type: SfxType): ShortArray = when (type) {
        SfxType.UI_CLICK -> tone(660.0, 0.06, square = true, amp = 0.35)
        SfxType.UI_BACK -> concat(tone(440.0, 0.05, true, 0.3), tone(330.0, 0.07, true, 0.3))
        SfxType.STEP -> tone(180.0, 0.035, square = false, amp = 0.22)
        SfxType.DOOR_OPEN -> concat(tone(392.0, 0.07, true, 0.35), tone(523.25, 0.1, true, 0.35))
        SfxType.ENCOUNTER_START -> concat(
            tone(330.0, 0.07, true, 0.4),
            tone(440.0, 0.07, true, 0.4),
            tone(554.37, 0.07, true, 0.4),
            tone(659.25, 0.16, true, 0.45)
        )
        SfxType.ATTACK_HIT -> concat(tone(260.0, 0.05, true, 0.45), tone(170.0, 0.09, true, 0.4))
        SfxType.ATTACK_SUPER -> concat(
            tone(523.25, 0.06, true, 0.45),
            tone(659.25, 0.06, true, 0.45),
            tone(783.99, 0.14, true, 0.5)
        )
        SfxType.ANSWER_CORRECT -> concat(
            tone(523.25, 0.08, false, 0.42),
            tone(659.25, 0.08, false, 0.42),
            tone(783.99, 0.14, false, 0.45)
        )
        // Warm, soft non-jarring low tone for errors
        SfxType.ANSWER_WRONG -> concat(
            tone(293.66, 0.11, square = false, amp = 0.32),
            tone(246.94, 0.16, square = false, amp = 0.28)
        )
        SfxType.STAR_GAIN -> concat(tone(880.0, 0.06, false, 0.38), tone(1174.66, 0.11, false, 0.42))
        SfxType.VICTORY_FANFARE -> concat(
            tone(523.25, 0.11, true, 0.4),
            tone(659.25, 0.11, true, 0.4),
            tone(783.99, 0.11, true, 0.4),
            tone(1046.5, 0.28, true, 0.45)
        )
        SfxType.MEDAL_UNLOCK -> concat(
            tone(587.33, 0.09, false, 0.42),
            tone(739.99, 0.09, false, 0.42),
            tone(880.0, 0.09, false, 0.42),
            tone(1174.66, 0.26, false, 0.48)
        )
        SfxType.LASER_SHOT -> sweepTone(startHz = 1150.0, endHz = 280.0, durationSec = 0.095, square = true, amp = 0.36)
        SfxType.ALIEN_BOOM -> concat(
            sweepTone(startHz = 320.0, endHz = 95.0, durationSec = 0.09, square = true, amp = 0.44),
            tone(140.0, 0.07, square = true, amp = 0.32)
        )
        SfxType.SLINGSHOT_LAUNCH -> sweepTone(startHz = 240.0, endHz = 720.0, durationSec = 0.12, square = false, amp = 0.40)
        SfxType.TOWER_CRASH -> concat(
            sweepTone(startHz = 260.0, endHz = 110.0, durationSec = 0.08, square = true, amp = 0.42),
            tone(440.0, 0.06, square = false, amp = 0.36),
            tone(660.0, 0.10, square = false, amp = 0.40)
        )
    }

    private fun sweepTone(
        startHz: Double,
        endHz: Double,
        durationSec: Double,
        square: Boolean,
        amp: Double
    ): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val frac = i.toDouble() / count.toDouble()
            val freq = startHz + (endHz - startHz) * frac
            phase += 2.0 * PI * freq / sampleRate
            val env = (1.0 - frac * 0.65).coerceAtLeast(0.0)
            val wave = if (square) {
                if (sin(phase) >= 0) 0.75 else -0.75
            } else {
                sin(phase)
            }
            out[i] = (wave * env * amp * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthesizeHubTheme(): ShortArray {
        // Warm, cheerful GBA school melody (8-bar loop, gentle sine/soft square blend)
        val notes = listOf(
            523.25, 659.25, 783.99, 659.25,
            587.33, 698.46, 880.00, 698.46,
            659.25, 783.99, 1046.50, 783.99,
            587.33, 659.25, 523.25, 392.00
        )
        return concat(*notes.map { tone(it, 0.26, square = false, amp = 0.26) }.toTypedArray())
    }

    private fun synthesizeBattleTheme(): ShortArray {
        // Energetic yet friendly 16-bit study challenge loop
        val notes = listOf(
            440.00, 523.25, 659.25, 523.25,
            493.88, 587.33, 739.99, 587.33,
            523.25, 659.25, 783.99, 659.25,
            587.33, 659.25, 523.25, 440.00
        )
        return concat(*notes.map { tone(it, 0.20, square = true, amp = 0.20) }.toTypedArray())
    }
}
