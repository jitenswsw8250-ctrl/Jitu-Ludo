package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.model.SoundPack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * SoundManager provides low-latency, reliable sound effects, sound packs (Classic, Royal, Fun),
 * volume scaling, talking token voices, and haptic feedback for Jitu Ludo.
 *
 * Implements a 3-tier audio playback pipeline:
 * 1. SoundPool with synthesized 44.1kHz PCM WAV files in app cache.
 * 2. Guaranteed-sized static AudioTrack with automatic buffer padding to prevent HAL errors.
 * 3. ToneGenerator fallback for hardware edge cases.
 */
class SoundManager(private val context: Context) {

    private val tag = "SoundManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Standard 44.1kHz sampling universally supported across Android audio HALs
    private val sampleRate = 44100

    private enum class SoundType {
        DICE_ROLL,
        TOKEN_MOVE,
        TOKEN_CAPTURE,
        TOKEN_HOME,
        VICTORY,
        BUTTON_CLICK,
        TURN_CHANGE,
        TALK_STEP,
        TALK_CAPTURE,
        TALK_SIX,
        TALK_HOME
    }

    private data class SoundKey(val pack: SoundPack, val type: SoundType)

    @Volatile
    var currentSoundPack: SoundPack = SoundPack.CLASSIC

    @Volatile
    var masterVolume: Float = 1.0f

    private var soundPool: SoundPool? = null
    private val soundIds = ConcurrentHashMap<SoundKey, Int>()
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val pcmBuffers = ConcurrentHashMap<SoundKey, ShortArray>()

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    init {
        initAudioEngine()
    }

    fun setSoundPack(pack: SoundPack) {
        currentSoundPack = pack
    }

    fun setVolume(volume: Float) {
        masterVolume = volume.coerceIn(0.0f, 1.0f)
    }

    /**
     * Checks if the device media volume is currently audible (> 0).
     */
    private fun isMediaVolumeAudible(): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return true
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            currentVol > 0
        } catch (_: Exception) {
            true
        }
    }

    private fun initAudioEngine() {
        scope.launch(Dispatchers.IO) {
            try {
                // 1. Synthesize all audio buffers in-memory for all sound packs
                for (pack in SoundPack.values()) {
                    pcmBuffers[SoundKey(pack, SoundType.DICE_ROLL)] = when (pack) {
                        SoundPack.CLASSIC -> synthesizeDiceRoll()
                        SoundPack.ROYAL -> synthesizeRoyalDiceRoll()
                        SoundPack.FUN -> synthesizeFunDiceRoll()
                    }
                    pcmBuffers[SoundKey(pack, SoundType.TOKEN_MOVE)] = when (pack) {
                        SoundPack.CLASSIC -> synthesizeTokenMove()
                        SoundPack.ROYAL -> synthesizeRoyalTokenMove()
                        SoundPack.FUN -> synthesizeFunTokenMove()
                    }
                    pcmBuffers[SoundKey(pack, SoundType.TOKEN_CAPTURE)] = when (pack) {
                        SoundPack.CLASSIC -> synthesizeTokenCapture()
                        SoundPack.ROYAL -> synthesizeRoyalTokenCapture()
                        SoundPack.FUN -> synthesizeFunTokenCapture()
                    }
                    pcmBuffers[SoundKey(pack, SoundType.TOKEN_HOME)] = when (pack) {
                        SoundPack.CLASSIC -> synthesizeTokenHome()
                        SoundPack.ROYAL -> synthesizeRoyalTokenHome()
                        SoundPack.FUN -> synthesizeFunTokenHome()
                    }
                    pcmBuffers[SoundKey(pack, SoundType.VICTORY)] = when (pack) {
                        SoundPack.CLASSIC -> synthesizeVictoryFanfare()
                        SoundPack.ROYAL -> synthesizeRoyalVictoryFanfare()
                        SoundPack.FUN -> synthesizeFunVictoryFanfare()
                    }
                    // Common UI
                    pcmBuffers[SoundKey(pack, SoundType.BUTTON_CLICK)] = synthesizeButtonClick()
                    pcmBuffers[SoundKey(pack, SoundType.TURN_CHANGE)] = synthesizeTurnChange()

                    // Talking token sounds
                    pcmBuffers[SoundKey(pack, SoundType.TALK_STEP)] = synthesizeTalkStep()
                    pcmBuffers[SoundKey(pack, SoundType.TALK_CAPTURE)] = synthesizeTalkCapture()
                    pcmBuffers[SoundKey(pack, SoundType.TALK_SIX)] = synthesizeTalkSix()
                    pcmBuffers[SoundKey(pack, SoundType.TALK_HOME)] = synthesizeTalkHome()
                }

                // 2. Build SoundPool
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val pool = SoundPool.Builder()
                    .setMaxStreams(12)
                    .setAudioAttributes(audioAttributes)
                    .build()

                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedSoundIds.add(sampleId)
                    }
                }
                soundPool = pool

                // 3. Write PCM WAV files to cache and load into SoundPool
                val soundDir = File(context.cacheDir, "ludo_audio").apply { mkdirs() }

                for ((key, pcm) in pcmBuffers) {
                    val file = File(soundDir, "${key.pack.name.lowercase()}_${key.type.name.lowercase()}.wav")
                    if (!file.exists() || file.length() == 0L) {
                        val wavBytes = createWavByteArray(pcm, sampleRate)
                        FileOutputStream(file).use { it.write(wavBytes) }
                    }
                    val id = pool.load(file.absolutePath, 1)
                    if (id != 0) {
                        soundIds[key] = id
                    }
                }
                Log.i(tag, "Audio engine initialized for all sound packs & talking tokens")
            } catch (e: Exception) {
                Log.w(tag, "SoundPool setup error, fallback enabled: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public Sound Triggers
    // -------------------------------------------------------------------------

    fun playDiceRoll(soundEnabled: Boolean, hapticsEnabled: Boolean, pack: SoundPack = currentSoundPack) {
        if (hapticsEnabled) vibrate(35)
        if (!soundEnabled || !isMediaVolumeAudible() || masterVolume <= 0.01f) return
        playSound(SoundKey(pack, SoundType.DICE_ROLL), volume = 0.95f)
    }

    fun playMove(soundEnabled: Boolean, hapticsEnabled: Boolean, pack: SoundPack = currentSoundPack) {
        if (hapticsEnabled) vibrate(22)
        if (!soundEnabled || !isMediaVolumeAudible() || masterVolume <= 0.01f) return
        playSound(SoundKey(pack, SoundType.TOKEN_MOVE), volume = 0.80f)
    }

    fun playCapture(soundEnabled: Boolean, hapticsEnabled: Boolean, pack: SoundPack = currentSoundPack) {
        if (hapticsEnabled) vibratePattern(longArrayOf(0, 50, 40, 90))
        if (!soundEnabled || !isMediaVolumeAudible() || masterVolume <= 0.01f) return
        playSound(SoundKey(pack, SoundType.TOKEN_CAPTURE), volume = 0.95f)
    }

    fun playHomeSafe(soundEnabled: Boolean, hapticsEnabled: Boolean, pack: SoundPack = currentSoundPack) {
        if (hapticsEnabled) vibrate(45)
        if (!soundEnabled || !isMediaVolumeAudible() || masterVolume <= 0.01f) return
        playSound(SoundKey(pack, SoundType.TOKEN_HOME), volume = 0.90f)
    }

    fun playVictory(soundEnabled: Boolean, hapticsEnabled: Boolean, pack: SoundPack = currentSoundPack) {
        if (hapticsEnabled) vibratePattern(longArrayOf(0, 80, 50, 80, 50, 220))
        if (!soundEnabled || !isMediaVolumeAudible() || masterVolume <= 0.01f) return
        playSound(SoundKey(pack, SoundType.VICTORY), volume = 0.95f)
    }

    fun playButtonClick(soundEnabled: Boolean) {
        if (!soundEnabled || !isMediaVolumeAudible() || masterVolume <= 0.01f) return
        playSound(SoundKey(currentSoundPack, SoundType.BUTTON_CLICK), volume = 0.60f)
    }

    fun playTurnChange(soundEnabled: Boolean) {
        if (!soundEnabled || !isMediaVolumeAudible() || masterVolume <= 0.01f) return
        playSound(SoundKey(currentSoundPack, SoundType.TURN_CHANGE), volume = 0.70f)
    }

    fun playTalkingToken(event: String, soundEnabled: Boolean) {
        if (!soundEnabled || !isMediaVolumeAudible() || masterVolume <= 0.01f) return
        val type = when {
            event.contains("step", ignoreCase = true) || event.contains("march", ignoreCase = true) -> SoundType.TALK_STEP
            event.contains("gotcha", ignoreCase = true) || event.contains("catch", ignoreCase = true) -> SoundType.TALK_CAPTURE
            event.contains("6", ignoreCase = true) || event.contains("six", ignoreCase = true) -> SoundType.TALK_SIX
            else -> SoundType.TALK_HOME
        }
        playSound(SoundKey(currentSoundPack, type), volume = 0.85f)
    }

    fun playSoundPreview(pack: SoundPack, soundEnabled: Boolean = true) {
        if (!soundEnabled || !isMediaVolumeAudible() || masterVolume <= 0.01f) return
        // Play home chime from that sound pack as an immediate preview
        playSound(SoundKey(pack, SoundType.TOKEN_HOME), volume = 0.90f)
    }

    // -------------------------------------------------------------------------
    // Playback Engine
    // -------------------------------------------------------------------------

    private fun playSound(key: SoundKey, volume: Float) {
        val effectiveVolume = (volume * masterVolume).coerceIn(0.0f, 1.0f)
        if (effectiveVolume <= 0.01f) return

        scope.launch {
            try {
                val pool = soundPool
                val soundId = soundIds[key] ?: soundIds[SoundKey(SoundPack.CLASSIC, key.type)]
                var played = false

                if (pool != null && soundId != null && loadedSoundIds.contains(soundId)) {
                    val streamId = pool.play(soundId, effectiveVolume, effectiveVolume, 1, 0, 1.0f)
                    if (streamId != 0) {
                        played = true
                    }
                }

                if (!played) {
                    val pcm = pcmBuffers[key] ?: pcmBuffers[SoundKey(SoundPack.CLASSIC, key.type)]
                    if (pcm != null) {
                        val trackSuccess = playAudioTrackFallback(pcm, effectiveVolume)
                        if (!trackSuccess) {
                            playToneGeneratorFallback(key.type)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Playback error for $key: ${e.message}")
            }
        }
    }

    private fun playAudioTrackFallback(pcm: ShortArray, volume: Float): Boolean {
        return try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val audioBytes = pcm.size * 2
            val finalBufSize = max(minBufSize, audioBytes)
            val neededShorts = finalBufSize / 2
            val paddedShorts = ShortArray(neededShorts)
            System.arraycopy(pcm, 0, paddedShorts, 0, pcm.size)

            val scaledShorts = if (volume < 0.98f) {
                ShortArray(paddedShorts.size) { i ->
                    (paddedShorts[i] * volume).toInt().toShort()
                }
            } else {
                paddedShorts
            }

            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(finalBufSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val written = track.write(scaledShorts, 0, scaledShorts.size)
            if (written > 0) {
                track.play()
                scope.launch {
                    val playMs = (scaledShorts.size.toLong() * 1000L) / sampleRate + 50L
                    delay(playMs)
                    try {
                        track.stop()
                        track.release()
                    } catch (_: Exception) {}
                }
                true
            } else {
                track.release()
                false
            }
        } catch (e: Exception) {
            Log.w(tag, "AudioTrack fallback failed: ${e.message}")
            false
        }
    }

    private fun playToneGeneratorFallback(type: SoundType) {
        if (!isMediaVolumeAudible() || masterVolume <= 0.01f) return
        try {
            val toneType = when (type) {
                SoundType.DICE_ROLL -> ToneGenerator.TONE_PROP_BEEP
                SoundType.TOKEN_MOVE -> ToneGenerator.TONE_PROP_ACK
                SoundType.TOKEN_CAPTURE -> ToneGenerator.TONE_PROP_BEEP2
                SoundType.TOKEN_HOME -> ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE
                SoundType.VICTORY -> ToneGenerator.TONE_CDMA_HIGH_L
                SoundType.BUTTON_CLICK -> ToneGenerator.TONE_PROP_PROMPT
                SoundType.TURN_CHANGE -> ToneGenerator.TONE_PROP_BEEP
                SoundType.TALK_STEP -> ToneGenerator.TONE_PROP_ACK
                SoundType.TALK_CAPTURE -> ToneGenerator.TONE_PROP_BEEP2
                SoundType.TALK_SIX -> ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE
                SoundType.TALK_HOME -> ToneGenerator.TONE_CDMA_HIGH_L
            }
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, (ToneGenerator.MAX_VOLUME * masterVolume).toInt())
            tg.startTone(toneType, 120)
            scope.launch {
                delay(150)
                try {
                    tg.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    // -------------------------------------------------------------------------
    // Haptics
    // -------------------------------------------------------------------------

    private fun vibrate(durationMs: Long) {
        try {
            val vib = vibrator ?: return
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern(pattern: LongArray) {
        try {
            val vib = vibrator ?: return
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }

    // -------------------------------------------------------------------------
    // Audio Synthesis Algorithms
    // -------------------------------------------------------------------------

    // --- CLASSIC PACK ---
    private fun synthesizeDiceRoll(): ShortArray {
        val durationMs = 280
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        val clickDelays = intArrayOf(0, 30, 68, 115, 170, 225)
        for (delayMs in clickDelays) {
            val startSample = (sampleRate * (delayMs / 1000.0)).toInt()
            val clickSamples = (sampleRate * 0.038).toInt()
            val baseFreq = Random.nextDouble(280.0, 480.0)
            for (i in 0 until clickSamples) {
                val targetIndex = startSample + i
                if (targetIndex >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val env = exp(-t * 90.0)
                val noise = Random.nextDouble(-0.35, 0.35)
                val sampleVal = (sin(2 * PI * baseFreq * t) + noise) * env * Short.MAX_VALUE * 0.40
                val mixed = (buffer[targetIndex].toInt() + sampleVal.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[targetIndex] = mixed.toShort()
            }
        }
        return buffer
    }

    private fun synthesizeTokenMove(): ShortArray {
        val durationMs = 65
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val freq = 420.0 + progress * 240.0
            val env = exp(-progress * 6.5)
            val sampleVal = sin(2 * PI * freq * t) * env * Short.MAX_VALUE * 0.55
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeTokenCapture(): ShortArray {
        val durationMs = 220
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val f1 = 820.0 - progress * 520.0
            val env1 = exp(-progress * 5.0)
            val s1 = sin(2 * PI * f1 * t)
            val noise = Random.nextDouble(-0.25, 0.25) * exp(-progress * 14.0)
            val s2 = sin(2 * PI * 220.0 * t) * exp(-progress * 4.0) * 0.6
            val sampleVal = (s1 + noise + s2) * env1 * Short.MAX_VALUE * 0.45
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeTokenHome(): ShortArray {
        val durationMs = 380
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteDurSamples = totalSamples / freqs.size
        for ((idx, f) in freqs.withIndex()) {
            val startSample = idx * (noteDurSamples * 0.70).toInt()
            val noteLen = minOf(noteDurSamples, totalSamples - startSample)
            for (i in 0 until noteLen) {
                val targetIndex = startSample + i
                if (targetIndex >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / noteLen
                val env = exp(-progress * 3.5)
                val s1 = sin(2 * PI * f * t)
                val s2 = sin(2 * PI * f * 2 * t) * 0.25
                val sampleVal = (s1 + s2) * env * Short.MAX_VALUE * 0.40
                val mixed = (buffer[targetIndex].toInt() + sampleVal.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[targetIndex] = mixed.toShort()
            }
        }
        return buffer
    }

    private fun synthesizeVictoryFanfare(): ShortArray {
        val durationMs = 1200
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        val melody = listOf(
            Pair(523.25, 140), Pair(659.25, 140), Pair(783.99, 140),
            Pair(1046.50, 220), Pair(880.0, 140), Pair(1046.50, 420)
        )
        var curSample = 0
        for ((freq, durMs) in melody) {
            val durSamples = (sampleRate * (durMs / 1000.0)).toInt()
            for (i in 0 until durSamples) {
                val targetIndex = curSample + i
                if (targetIndex >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / durSamples
                val env = exp(-progress * 2.2)
                val s1 = sin(2 * PI * freq * t)
                val s2 = sin(2 * PI * freq * 2.0 * t) * 0.30
                val s3 = sin(2 * PI * freq * 3.0 * t) * 0.15
                val sampleVal = (s1 + s2 + s3) * env * Short.MAX_VALUE * 0.45
                val mixed = (buffer[targetIndex].toInt() + sampleVal.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[targetIndex] = mixed.toShort()
            }
            curSample += durSamples
        }
        return buffer
    }

    // --- ROYAL PACK ---
    private fun synthesizeRoyalDiceRoll(): ShortArray {
        val durationMs = 320
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        val chimes = doubleArrayOf(1046.5, 1318.5, 1567.98, 2093.0)
        for ((idx, f) in chimes.withIndex()) {
            val startSample = (idx * 55 * sampleRate / 1000)
            val noteLen = minOf((sampleRate * 0.12).toInt(), totalSamples - startSample)
            for (i in 0 until noteLen) {
                val targetIndex = startSample + i
                if (targetIndex >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / noteLen
                val env = exp(-progress * 6.0)
                val s1 = sin(2 * PI * f * t)
                val s2 = sin(2 * PI * (f * 1.5) * t) * 0.4
                val sampleVal = (s1 + s2) * env * Short.MAX_VALUE * 0.35
                val mixed = (buffer[targetIndex].toInt() + sampleVal.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[targetIndex] = mixed.toShort()
            }
        }
        return buffer
    }

    private fun synthesizeRoyalTokenMove(): ShortArray {
        val durationMs = 120
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Regal harp pluck with rich harmonics
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val f = 659.25
            val env = exp(-progress * 5.0)
            val s1 = sin(2 * PI * f * t)
            val s2 = sin(2 * PI * f * 2.0 * t) * 0.35
            val s3 = sin(2 * PI * f * 3.0 * t) * 0.18
            val sampleVal = (s1 + s2 + s3) * env * Short.MAX_VALUE * 0.50
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeRoyalTokenCapture(): ShortArray {
        val durationMs = 350
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Imperial resonant gong / brass strike
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val env = exp(-progress * 3.0)
            val s1 = sin(2 * PI * 196.0 * t)
            val s2 = sin(2 * PI * 392.0 * t) * 0.5
            val s3 = sin(2 * PI * 587.33 * t) * 0.3
            val sampleVal = (s1 + s2 + s3) * env * Short.MAX_VALUE * 0.48
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeRoyalTokenHome(): ShortArray {
        val durationMs = 450
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Cathedral bell chime (G4, C5, E5, G5)
        val freqs = doubleArrayOf(392.0, 523.25, 659.25, 783.99)
        for ((idx, f) in freqs.withIndex()) {
            val startSample = idx * (sampleRate * 0.08).toInt()
            val noteLen = totalSamples - startSample
            for (i in 0 until noteLen) {
                val targetIndex = startSample + i
                if (targetIndex >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / noteLen
                val env = exp(-progress * 3.0)
                val s1 = sin(2 * PI * f * t)
                val s2 = sin(2 * PI * f * 2.0 * t) * 0.4
                val sampleVal = (s1 + s2) * env * Short.MAX_VALUE * 0.35
                val mixed = (buffer[targetIndex].toInt() + sampleVal.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[targetIndex] = mixed.toShort()
            }
        }
        return buffer
    }

    private fun synthesizeRoyalVictoryFanfare(): ShortArray {
        val durationMs = 1400
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Grand imperial coronation brass
        val melody = listOf(
            Pair(392.0, 160), Pair(523.25, 160), Pair(659.25, 160),
            Pair(783.99, 280), Pair(1046.50, 320), Pair(1318.51, 480)
        )
        var curSample = 0
        for ((freq, durMs) in melody) {
            val durSamples = (sampleRate * (durMs / 1000.0)).toInt()
            for (i in 0 until durSamples) {
                val targetIndex = curSample + i
                if (targetIndex >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / durSamples
                val env = exp(-progress * 2.0)
                val s1 = sin(2 * PI * freq * t)
                val s2 = sin(2 * PI * freq * 2.0 * t) * 0.35
                val s3 = sin(2 * PI * freq * 3.0 * t) * 0.20
                val sampleVal = (s1 + s2 + s3) * env * Short.MAX_VALUE * 0.48
                val mixed = (buffer[targetIndex].toInt() + sampleVal.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[targetIndex] = mixed.toShort()
            }
            curSample += (durSamples * 0.85).toInt()
        }
        return buffer
    }

    // --- FUN PACK ---
    private fun synthesizeFunDiceRoll(): ShortArray {
        val durationMs = 280
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Comic spring boing & pop
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val f = 300.0 + sin(2 * PI * 18.0 * t) * 160.0
            val env = exp(-progress * 4.5)
            val sampleVal = sin(2 * PI * f * t) * env * Short.MAX_VALUE * 0.50
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeFunTokenMove(): ShortArray {
        val durationMs = 80
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Playful bubbly bloop
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val f = 550.0 + progress * 700.0
            val env = exp(-progress * 8.0)
            val sampleVal = sin(2 * PI * f * t) * env * Short.MAX_VALUE * 0.60
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeFunTokenCapture(): ShortArray {
        val durationMs = 240
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Cartoon slide whistle down + boing
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val f = (1200.0 - progress * 800.0) + sin(2 * PI * 24.0 * t) * 90.0
            val env = exp(-progress * 4.0)
            val sampleVal = sin(2 * PI * f * t) * env * Short.MAX_VALUE * 0.50
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeFunTokenHome(): ShortArray {
        val durationMs = 360
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Upbeat 8-bit retro victory arpeggio
        val notes = doubleArrayOf(440.0, 554.37, 659.25, 880.0)
        val noteDur = totalSamples / notes.size
        for ((idx, f) in notes.withIndex()) {
            val startSample = idx * noteDur
            for (i in 0 until noteDur) {
                val targetIndex = startSample + i
                if (targetIndex >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / noteDur
                val env = exp(-progress * 5.0)
                val sampleVal = sin(2 * PI * f * t) * env * Short.MAX_VALUE * 0.45
                buffer[targetIndex] = sampleVal.toInt().toShort()
            }
        }
        return buffer
    }

    private fun synthesizeFunVictoryFanfare(): ShortArray {
        val durationMs = 1100
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Bouncy carnival victory tune
        val melody = listOf(
            Pair(523.25, 120), Pair(659.25, 120), Pair(783.99, 120),
            Pair(659.25, 120), Pair(783.99, 180), Pair(1046.50, 360)
        )
        var curSample = 0
        for ((freq, durMs) in melody) {
            val durSamples = (sampleRate * (durMs / 1000.0)).toInt()
            for (i in 0 until durSamples) {
                val targetIndex = curSample + i
                if (targetIndex >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / durSamples
                val env = exp(-progress * 3.0)
                val s1 = sin(2 * PI * freq * t)
                val sampleVal = s1 * env * Short.MAX_VALUE * 0.45
                val mixed = (buffer[targetIndex].toInt() + sampleVal.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[targetIndex] = mixed.toShort()
            }
            curSample += durSamples
        }
        return buffer
    }

    // --- TALKING TOKENS VOCAL VOICES ---
    private fun synthesizeTalkStep(): ShortArray {
        val durationMs = 110
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Cute double chirp: "pip-pop!"
        val half = totalSamples / 2
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = (i % half).toDouble() / half
            val f = if (i < half) 600.0 + progress * 200.0 else 800.0 + progress * 250.0
            val env = exp(-progress * 7.0)
            val sampleVal = sin(2 * PI * f * t) * env * Short.MAX_VALUE * 0.50
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeTalkCapture(): ShortArray {
        val durationMs = 210
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Energetic "Gotcha!" cheer
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val f = 450.0 + sin(2 * PI * 14.0 * t) * 120.0 + progress * 300.0
            val env = exp(-progress * 3.5)
            val s1 = sin(2 * PI * f * t)
            val s2 = sin(2 * PI * (f * 1.5) * t) * 0.35
            val sampleVal = (s1 + s2) * env * Short.MAX_VALUE * 0.50
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeTalkSix(): ShortArray {
        val durationMs = 250
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Excited rising sparkle cheer
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val f = 600.0 + progress * 800.0
            val env = exp(-progress * 3.0)
            val s1 = sin(2 * PI * f * t)
            val s2 = sin(2 * PI * (f * 2.0) * t) * 0.3
            val sampleVal = (s1 + s2) * env * Short.MAX_VALUE * 0.45
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeTalkHome(): ShortArray {
        val durationMs = 320
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        // Joyful home safe flourish
        val chord = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val env = exp(-progress * 3.0)
            var sum = 0.0
            for (f in chord) {
                sum += sin(2 * PI * f * t) * 0.25
            }
            val sampleVal = sum * env * Short.MAX_VALUE * 0.55
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    // --- COMMON UI SOUNDS ---
    private fun synthesizeButtonClick(): ShortArray {
        val durationMs = 35
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val freq = 1200.0 - progress * 600.0
            val env = exp(-progress * 8.0)
            val sampleVal = sin(2 * PI * freq * t) * env * Short.MAX_VALUE * 0.40
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeTurnChange(): ShortArray {
        val durationMs = 160
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val env = exp(-progress * 5.0)
            val s1 = sin(2 * PI * 880.0 * t)
            val s2 = sin(2 * PI * 1318.51 * t) * 0.35
            val sampleVal = (s1 + s2) * env * Short.MAX_VALUE * 0.45
            buffer[i] = sampleVal.toInt().toShort()
        }
        return buffer
    }

    // -------------------------------------------------------------------------
    // WAV Header Generation
    // -------------------------------------------------------------------------

    private fun createWavByteArray(samples: ShortArray, sRate: Int): ByteArray {
        val totalAudioLen = samples.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sRate and 0xff).toByte()
        header[25] = ((sRate shr 8) and 0xff).toByte()
        header[26] = ((sRate shr 16) and 0xff).toByte()
        header[27] = ((sRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val out = ByteArray(44 + totalAudioLen)
        System.arraycopy(header, 0, out, 0, 44)
        var idx = 44
        for (s in samples) {
            out[idx++] = (s.toInt() and 0xff).toByte()
            out[idx++] = ((s.toInt() shr 8) and 0xff).toByte()
        }
        return out
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
        } catch (_: Exception) {}
    }
}
