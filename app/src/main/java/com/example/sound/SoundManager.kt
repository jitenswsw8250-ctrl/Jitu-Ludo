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
 * SoundManager provides low-latency, reliable sound effects and haptic feedback
 * for Jitu Ludo on Android devices and the cloud streaming preview.
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
        TURN_CHANGE
    }

    private var soundPool: SoundPool? = null
    private val soundIds = ConcurrentHashMap<SoundType, Int>()
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val pcmBuffers = ConcurrentHashMap<SoundType, ShortArray>()

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

    /**
     * Checks if the device media volume is currently audible (> 0).
     * If the user sets media volume to 0 or mutes the stream, game sounds remain inaudible
     * without ever modifying or raising the user's system volume settings.
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
                // 1. Synthesize all audio buffers in-memory
                pcmBuffers[SoundType.DICE_ROLL] = synthesizeDiceRoll()
                pcmBuffers[SoundType.TOKEN_MOVE] = synthesizeTokenMove()
                pcmBuffers[SoundType.TOKEN_CAPTURE] = synthesizeTokenCapture()
                pcmBuffers[SoundType.TOKEN_HOME] = synthesizeTokenHome()
                pcmBuffers[SoundType.VICTORY] = synthesizeVictoryFanfare()
                pcmBuffers[SoundType.BUTTON_CLICK] = synthesizeButtonClick()
                pcmBuffers[SoundType.TURN_CHANGE] = synthesizeTurnChange()

                // 2. Build SoundPool with USAGE_GAME and CONTENT_TYPE_SONIFICATION
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val pool = SoundPool.Builder()
                    .setMaxStreams(8)
                    .setAudioAttributes(audioAttributes)
                    .build()

                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedSoundIds.add(sampleId)
                        Log.d(tag, "SoundPool sample $sampleId loaded successfully")
                    } else {
                        Log.w(tag, "SoundPool sample $sampleId failed with status $status")
                    }
                }
                soundPool = pool

                // 3. Write PCM WAV files to cache and load into SoundPool
                val soundDir = File(context.cacheDir, "ludo_audio").apply { mkdirs() }

                for ((type, pcm) in pcmBuffers) {
                    val file = File(soundDir, "${type.name.lowercase()}.wav")
                    if (!file.exists() || file.length() == 0L) {
                        val wavBytes = createWavByteArray(pcm, sampleRate)
                        FileOutputStream(file).use { it.write(wavBytes) }
                    }
                    val id = pool.load(file.absolutePath, 1)
                    if (id != 0) {
                        soundIds[type] = id
                    }
                }
                Log.i(tag, "Audio engine initialized with 7 synthesized sounds (44.1kHz)")
            } catch (e: Exception) {
                Log.w(tag, "SoundPool setup error, fallback enabled: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public Sound Triggers
    // -------------------------------------------------------------------------

    fun playDiceRoll(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibrate(35)
        if (!soundEnabled || !isMediaVolumeAudible()) return
        playSound(SoundType.DICE_ROLL, volume = 0.95f)
    }

    fun playMove(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibrate(22)
        if (!soundEnabled || !isMediaVolumeAudible()) return
        playSound(SoundType.TOKEN_MOVE, volume = 0.80f)
    }

    fun playCapture(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibratePattern(longArrayOf(0, 50, 40, 90))
        if (!soundEnabled || !isMediaVolumeAudible()) return
        playSound(SoundType.TOKEN_CAPTURE, volume = 0.95f)
    }

    fun playHomeSafe(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibrate(45)
        if (!soundEnabled || !isMediaVolumeAudible()) return
        playSound(SoundType.TOKEN_HOME, volume = 0.90f)
    }

    fun playVictory(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibratePattern(longArrayOf(0, 80, 50, 80, 50, 220))
        if (!soundEnabled || !isMediaVolumeAudible()) return
        playSound(SoundType.VICTORY, volume = 0.95f)
    }

    fun playButtonClick(soundEnabled: Boolean) {
        if (!soundEnabled || !isMediaVolumeAudible()) return
        playSound(SoundType.BUTTON_CLICK, volume = 0.60f)
    }

    fun playTurnChange(soundEnabled: Boolean) {
        if (!soundEnabled || !isMediaVolumeAudible()) return
        playSound(SoundType.TURN_CHANGE, volume = 0.70f)
    }

    // -------------------------------------------------------------------------
    // Multi-tier Playback Engine
    // -------------------------------------------------------------------------

    private fun playSound(type: SoundType, volume: Float) {
        scope.launch {
            try {
                val pool = soundPool
                val soundId = soundIds[type]
                var played = false

                // 1. Try SoundPool first
                if (pool != null && soundId != null && loadedSoundIds.contains(soundId)) {
                    val streamId = pool.play(soundId, volume, volume, 1, 0, 1.0f)
                    if (streamId != 0) {
                        played = true
                    }
                }

                // 2. Fallback to direct static AudioTrack with padded buffer
                if (!played) {
                    val pcm = pcmBuffers[type] ?: when (type) {
                        SoundType.DICE_ROLL -> synthesizeDiceRoll()
                        SoundType.TOKEN_MOVE -> synthesizeTokenMove()
                        SoundType.TOKEN_CAPTURE -> synthesizeTokenCapture()
                        SoundType.TOKEN_HOME -> synthesizeTokenHome()
                        SoundType.VICTORY -> synthesizeVictoryFanfare()
                        SoundType.BUTTON_CLICK -> synthesizeButtonClick()
                        SoundType.TURN_CHANGE -> synthesizeTurnChange()
                    }
                    val trackSuccess = playAudioTrackFallback(pcm, volume)
                    if (!trackSuccess) {
                        // 3. Fallback to ToneGenerator for extreme HAL constraints
                        playToneGeneratorFallback(type)
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Playback error for $type: ${e.message}")
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
                .setBufferSizeInBytes(scaledShorts.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val written = track.write(scaledShorts, 0, scaledShorts.size)
            if (written > 0) {
                track.play()
                scope.launch {
                    val durationMs = (pcm.size * 1000L) / sampleRate + 150
                    delay(durationMs)
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
        if (!isMediaVolumeAudible()) return
        try {
            val toneType = when (type) {
                SoundType.DICE_ROLL -> ToneGenerator.TONE_PROP_BEEP
                SoundType.TOKEN_MOVE -> ToneGenerator.TONE_PROP_ACK
                SoundType.TOKEN_CAPTURE -> ToneGenerator.TONE_PROP_BEEP2
                SoundType.TOKEN_HOME -> ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE
                SoundType.VICTORY -> ToneGenerator.TONE_CDMA_HIGH_L
                SoundType.BUTTON_CLICK -> ToneGenerator.TONE_PROP_PROMPT
                SoundType.TURN_CHANGE -> ToneGenerator.TONE_PROP_BEEP
            }
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
        } catch (_: Exception) {}
    }

    // -------------------------------------------------------------------------
    // Sound Synthesis (Natural, Rich 44.1kHz Game Audio)
    // -------------------------------------------------------------------------

    /**
     * Dice Roll: Realistic wooden tumbling rattle with micro-impacts and woody resonance (~340ms).
     */
    private fun synthesizeDiceRoll(): ShortArray {
        val durationMs = 340
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        val clickOffsetsMs = intArrayOf(0, 40, 85, 140, 205, 270)
        val clickFrequencies = doubleArrayOf(740.0, 680.0, 620.0, 540.0, 480.0, 430.0)

        for (idx in clickOffsetsMs.indices) {
            val startSample = (clickOffsetsMs[idx] * sampleRate) / 1000
            val baseFreq = clickFrequencies[idx]
            val clickDuration = (sampleRate * 0.045).toInt()

            for (i in 0 until clickDuration) {
                val targetIndex = startSample + i
                if (targetIndex >= totalSamples) break
                val t = i.toDouble() / sampleRate

                val pitchDecay = baseFreq * exp(-i * 12.0 / clickDuration)
                val envelope = exp(-i * 9.0 / clickDuration)
                val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.25

                val sampleVal = (sin(2 * PI * pitchDecay * t) + noise) * envelope * Short.MAX_VALUE * 0.50
                val current = buffer[targetIndex].toInt()
                val mixed = (current + sampleVal.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[targetIndex] = mixed.toShort()
            }
        }
        return buffer
    }

    /**
     * Token Move: Pleasant, smooth wooden board tap / plop (~110ms).
     */
    private fun synthesizeTokenMove(): ShortArray {
        val durationMs = 110
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val freq = 480.0 - progress * 190.0
            val envelope = when {
                progress < 0.05 -> progress / 0.05
                else -> exp(-(progress - 0.05) * 6.5)
            }
            val s1 = sin(2 * PI * freq * t)
            val s2 = sin(4 * PI * freq * t) * 0.28
            val sampleVal = (s1 + s2) * envelope * Short.MAX_VALUE * 0.55
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /**
     * Token Capture: Punchy impact with dramatic downward swoosh and low thud (~260ms).
     */
    private fun synthesizeTokenCapture(): ShortArray {
        val durationMs = 260
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val freq = 140.0 + 740.0 * exp(-progress * 7.0)
            val envelope = when {
                progress < 0.03 -> progress / 0.03
                else -> exp(-(progress - 0.03) * 5.0)
            }
            val impactNoise = if (progress < 0.15) (Random.nextDouble() * 2.0 - 1.0) * 0.20 else 0.0
            val subBass = sin(2 * PI * 90.0 * t) * 0.40
            val mainTone = sin(2 * PI * freq * t)

            val sampleVal = (mainTone + subBass + impactNoise) * envelope * Short.MAX_VALUE * 0.60
            buffer[i] = sampleVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /**
     * Token Home: Cheerful bright 4-note ascending chime C5 -> E5 -> G5 -> C6 (~420ms).
     */
    private fun synthesizeTokenHome(): ShortArray {
        val durationMs = 420
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteDuration = totalSamples / notes.size

        for (n in notes.indices) {
            val start = n * noteDuration
            val freq = notes[n]
            val ringLength = totalSamples - start

            for (i in 0 until ringLength) {
                val idx = start + i
                if (idx >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val env = exp(-i.toDouble() / (sampleRate * 0.12))
                val fundamental = sin(2 * PI * freq * t)
                val overtone = sin(2 * PI * freq * 2.76 * t) * 0.25
                val sampleVal = (fundamental + overtone) * env * Short.MAX_VALUE * 0.40
                val mixed = (buffer[idx].toInt() + sampleVal.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[idx] = mixed.toShort()
            }
        }
        return buffer
    }

    /**
     * Victory Fanfare: Triumphant brass/chime fanfare celebration (~850ms).
     */
    private fun synthesizeVictoryFanfare(): ShortArray {
        val durationMs = 850
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        val notes = doubleArrayOf(392.00, 523.25, 659.25, 783.99, 1046.50)
        val noteDurationsMs = intArrayOf(110, 110, 110, 150, 370)

        var curSample = 0
        for (idx in notes.indices) {
            val freq = notes[idx]
            val durSamples = (noteDurationsMs[idx] * sampleRate) / 1000
            val ringSamples = (durSamples * 1.5).toInt()

            for (i in 0 until ringSamples) {
                val targetIndex = curSample + i
                if (targetIndex >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / durSamples
                val env = when {
                    progress < 0.08 -> progress / 0.08
                    progress <= 1.0 -> 1.0 - progress * 0.15
                    else -> exp(-(progress - 1.0) * 4.0)
                }
                val s1 = sin(2 * PI * freq * t)
                val s2 = sin(4 * PI * freq * t) * 0.35
                val s3 = sin(6 * PI * freq * t) * 0.15
                val sampleVal = (s1 + s2 + s3) * env * Short.MAX_VALUE * 0.45
                val mixed = (buffer[targetIndex].toInt() + sampleVal.toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[targetIndex] = mixed.toShort()
            }
            curSample += durSamples
        }
        return buffer
    }

    /**
     * Button Click: Subtle crisp UI tap (~35ms).
     */
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

    /**
     * Turn Change: Soft pleasant notification ding (~160ms).
     */
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
}
