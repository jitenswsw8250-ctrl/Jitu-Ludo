package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Jitu Ludo", appName)
  }

  @Test
  fun `sound manager does not increase phone media volume when volume is zero`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 0, 0)
    val initialVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
    assertEquals(0, initialVol)

    val soundManager = com.example.sound.SoundManager(context)
    val volAfterInit = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
    assertEquals("SoundManager init must not change volume from zero", 0, volAfterInit)

    soundManager.playDiceRoll(soundEnabled = true, hapticsEnabled = false)
    soundManager.playMove(soundEnabled = true, hapticsEnabled = false)
    soundManager.playCapture(soundEnabled = true, hapticsEnabled = false)
    soundManager.playHomeSafe(soundEnabled = true, hapticsEnabled = false)
    soundManager.playVictory(soundEnabled = true, hapticsEnabled = false)
    soundManager.playButtonClick(soundEnabled = true)
    soundManager.playTurnChange(soundEnabled = true)

    val volAfterSounds = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
    assertEquals("Phone media volume must NEVER be increased automatically by game sounds", 0, volAfterSounds)
  }
}
