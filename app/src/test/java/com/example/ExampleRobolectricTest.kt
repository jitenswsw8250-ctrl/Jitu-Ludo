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

  @Test
  fun `reset settings restores defaults without deleting player names or avatars`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsRepo = com.example.data.SettingsRepository(context)

    // Set custom values
    settingsRepo.setSoundEnabled(false)
    settingsRepo.setHapticsEnabled(false)
    settingsRepo.setAnimationSpeed(com.example.model.AnimationSpeed.FAST)
    settingsRepo.setAiDifficulty(com.example.model.AiDifficulty.HARD)
    settingsRepo.setBoardTheme(com.example.model.BoardTheme.NIGHT)
    settingsRepo.setSelectedSkin(com.example.model.GameSkin.EMERALD)
    settingsRepo.setPlayerName(com.example.model.PlayerColor.RED, "Champion Alex")
    settingsRepo.setPlayerAvatar(com.example.model.PlayerColor.RED, com.example.model.PlayerAvatar.DRAGON)

    // Reset settings to defaults
    settingsRepo.resetToDefaults()

    // Configurable settings must be back to default
    org.junit.Assert.assertTrue(settingsRepo.isSoundEnabled())
    org.junit.Assert.assertTrue(settingsRepo.isHapticsEnabled())
    assertEquals(com.example.model.AnimationSpeed.NORMAL, settingsRepo.getAnimationSpeed())
    assertEquals(com.example.model.AiDifficulty.MEDIUM, settingsRepo.getAiDifficulty())
    assertEquals(com.example.model.BoardTheme.CLASSIC, settingsRepo.getBoardTheme())
    assertEquals(com.example.model.GameSkin.CLASSIC, settingsRepo.getSelectedSkin())

    // Player names and avatars must NOT be deleted
    assertEquals("Champion Alex", settingsRepo.getPlayerName(com.example.model.PlayerColor.RED, "Red"))
    assertEquals(com.example.model.PlayerAvatar.DRAGON, settingsRepo.getPlayerAvatar(com.example.model.PlayerColor.RED))
  }

  @Test
  fun `reset stats clears player lifetime records`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val statsRepo = com.example.data.StatsRepository(context)

    val redPlayer = com.example.model.Player(color = com.example.model.PlayerColor.RED, name = "Red")
    val yellowPlayer = com.example.model.Player(color = com.example.model.PlayerColor.YELLOW, name = "Yellow")
    statsRepo.recordCompletedGame(
        players = listOf(redPlayer, yellowPlayer),
        winner = redPlayer
    )

    org.junit.Assert.assertTrue(statsRepo.getAllStats().isNotEmpty())

    // Reset
    statsRepo.resetAllStats()
    org.junit.Assert.assertTrue(statsRepo.getAllStats().isEmpty())
  }

  @Test
  fun `emoji reaction auto-hides after 1500ms and clearActiveReaction works`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.LudoViewModel(application)

    // Trigger reaction
    viewModel.sendEmojiReaction("🔥")
    val reaction = viewModel.gameState.value.activeReaction
    org.junit.Assert.assertNotNull("Active reaction must be visible after triggering", reaction)
    assertEquals("🔥", reaction?.emoji)

    // Wait 1650ms for the 1.5s timer to auto-dismiss
    org.robolectric.shadows.ShadowLooper.idleMainLooper(1650, java.util.concurrent.TimeUnit.MILLISECONDS)
    org.junit.Assert.assertNull("Active reaction must be automatically cleared after 1.5s timeout", viewModel.gameState.value.activeReaction)

    // Trigger second reaction and test manual clear (e.g. Return to Home or Restart)
    viewModel.sendEmojiReaction("😎")
    org.junit.Assert.assertNotNull(viewModel.gameState.value.activeReaction)
    viewModel.clearActiveReaction()
    org.junit.Assert.assertNull("clearActiveReaction must immediately clear overlay", viewModel.gameState.value.activeReaction)
  }

  @Test
  fun `rapid consecutive emoji reactions restart fresh 1500ms timer and produce unique IDs`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.LudoViewModel(application)

    val triggeredIds = mutableSetOf<Long>()
    // Rapidly tap 5 emojis
    listOf("😂", "😎", "👏", "🔥", "🎉").forEach { emoji ->
      viewModel.sendEmojiReaction(emoji)
      val currentReaction = viewModel.gameState.value.activeReaction
      org.junit.Assert.assertNotNull(currentReaction)
      assertEquals(emoji, currentReaction?.emoji)
      triggeredIds.add(currentReaction!!.id)
    }

    // Every reaction must have a distinct unique nanoTime ID
    assertEquals(5, triggeredIds.size)

    // Final reaction should be "🎉"
    assertEquals("🎉", viewModel.gameState.value.activeReaction?.emoji)

    // Wait 1650ms from final reaction
    org.robolectric.shadows.ShadowLooper.idleMainLooper(1650, java.util.concurrent.TimeUnit.MILLISECONDS)
    org.junit.Assert.assertNull("Rapid taps must not leave stuck reaction after 1.5s", viewModel.gameState.value.activeReaction)
  }

  @Test
  fun `player profile editing and career statistics tracking persist locally`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.LudoViewModel(application)
    val settingsRepo = com.example.data.SettingsRepository(application)
    val statsRepo = com.example.data.StatsRepository(application)

    // 1. Update Red player's profile with custom name and avatar
    viewModel.updatePlayerProfile(
      color = com.example.model.PlayerColor.RED,
      name = "King Arthur",
      avatar = com.example.model.PlayerAvatar.DRAGON
    )

    // Verify saved locally in SettingsRepository
    assertEquals("King Arthur", settingsRepo.getPlayerName(com.example.model.PlayerColor.RED, ""))
    assertEquals(com.example.model.PlayerAvatar.DRAGON, settingsRepo.getPlayerAvatar(com.example.model.PlayerColor.RED))

    // 2. Simulate match completion: Red wins, with 2 tokens safely brought home and 1 capture
    val redTokens = listOf(
      com.example.model.Token(0, com.example.model.PlayerColor.RED, com.example.model.TokenPosition.Finished),
      com.example.model.Token(1, com.example.model.PlayerColor.RED, com.example.model.TokenPosition.Finished),
      com.example.model.Token(2, com.example.model.PlayerColor.RED, com.example.model.TokenPosition.OnBoard(20)),
      com.example.model.Token(3, com.example.model.PlayerColor.RED, com.example.model.TokenPosition.InBase)
    )
    val redPlayer = com.example.model.Player(
      color = com.example.model.PlayerColor.RED,
      name = "King Arthur",
      tokens = redTokens,
      captures = 1
    )
    val yellowPlayer = com.example.model.Player(
      color = com.example.model.PlayerColor.YELLOW,
      name = "Opponent",
      tokens = List(4) { com.example.model.Token(it, com.example.model.PlayerColor.YELLOW) }
    )

    statsRepo.recordCompletedGame(listOf(redPlayer, yellowPlayer), winner = redPlayer)

    val redStats = statsRepo.getStatsForPlayer(com.example.model.PlayerColor.RED, "King Arthur")
    assertEquals(1, redStats.completedGames)
    assertEquals(1, redStats.wins)
    assertEquals(0, redStats.losses)
    assertEquals("100%", redStats.winPercentageFormatted)
    assertEquals(1, redStats.captures)
    assertEquals(2, redStats.tokensHome)
    assertEquals(1, redStats.bestStreak)
    assertEquals(1, redStats.currentStreak)

    // Win second match in a row -> streak becomes 2
    statsRepo.recordCompletedGame(listOf(redPlayer, yellowPlayer), winner = redPlayer)
    val updatedStats = statsRepo.getStatsForPlayer(com.example.model.PlayerColor.RED, "King Arthur")
    assertEquals(2, updatedStats.completedGames)
    assertEquals(2, updatedStats.wins)
    assertEquals(2, updatedStats.bestStreak)
    assertEquals(2, updatedStats.currentStreak)
  }
}


