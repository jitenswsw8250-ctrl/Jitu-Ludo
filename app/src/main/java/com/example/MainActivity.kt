package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.model.PlayerColor
import com.example.ui.LudoViewModel
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HistoryDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerProfileDialog
import com.example.ui.screens.RulesDialog
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.SkinsDialog
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StatisticsDialog
import com.example.ui.screens.WebViewScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    SPLASH,
    HOME,
    GAME,
    WEB_VIEW
}

class MainActivity : ComponentActivity() {

    private val viewModel: LudoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                LudoAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun LudoAppRoot(
    viewModel: LudoViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSkinsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val gameState by viewModel.gameState.collectAsState()
    val playerStats by viewModel.playerStats.collectAsState()
    val matchHistory by viewModel.matchHistory.collectAsState()
    val hasSavedGame by viewModel.hasSavedGame.collectAsState()
    val context = LocalContext.current

    when (currentScreen) {
        AppScreen.SPLASH -> {
            SplashScreen(
                onSplashFinished = { currentScreen = AppScreen.HOME },
                modifier = modifier
            )
        }

        AppScreen.HOME -> {
            HomeScreen(
                soundEnabled = gameState.soundEnabled,
                hapticsEnabled = gameState.hapticsEnabled,
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                hasSavedGame = hasSavedGame,
                onResumeGame = {
                    if (viewModel.resumeSavedGame()) {
                        currentScreen = AppScreen.GAME
                    }
                },
                selectedDifficulty = gameState.aiDifficulty,
                onDifficultySelected = { viewModel.setAiDifficulty(it) },
                boardTheme = gameState.boardTheme,
                onSetBoardTheme = { viewModel.setBoardTheme(it) },
                onStartGame = { mode, count, color, names, avatars ->
                    viewModel.startNewGame(
                        mode = mode,
                        playerCount = count,
                        humanColor = color,
                        customNames = names,
                        customAvatars = avatars,
                        difficulty = gameState.aiDifficulty
                    )
                    currentScreen = AppScreen.GAME
                },
                onOpenRules = { showRulesDialog = true },
                onOpenStats = { showStatsDialog = true },
                onOpenHistory = { showHistoryDialog = true },
                onOpenWebView = { currentScreen = AppScreen.WEB_VIEW },
                onShareGame = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Play Jitu Ludo!")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Play Jitu Ludo - the classic board game with friends and smart AI!"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Jitu Ludo via"))
                },
                onOpenSettings = { showSettingsDialog = true },
                onOpenSkins = { showSkinsDialog = true },
                onOpenProfile = { showProfileDialog = true },
                modifier = modifier
            )
        }

        AppScreen.GAME -> {
            GameScreen(
                gameState = gameState,
                onRollDice = { viewModel.rollDice() },
                onTokenClicked = { tokenId -> viewModel.selectToken(tokenId) },
                onRestartGame = {
                    viewModel.clearActiveReaction()
                    val p1 = gameState.players.firstOrNull()
                    viewModel.startNewGame(
                        mode = gameState.gameMode,
                        playerCount = gameState.players.size,
                        humanColor = p1?.color ?: PlayerColor.RED,
                        customNames = gameState.players.associate { it.color to it.name },
                        customAvatars = gameState.players.associate { it.color to it.avatar },
                        difficulty = gameState.aiDifficulty
                    )
                },
                onNavigateHome = {
                    viewModel.clearActiveReaction()
                    currentScreen = AppScreen.HOME
                },
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onSetAnimationSpeed = { viewModel.setAnimationSpeed(it) },
                onSetSkin = { viewModel.setSkin(it) },
                onOpenStats = { showStatsDialog = true },
                onOpenProfile = { showProfileDialog = true },
                onSendEmojiReaction = { viewModel.sendEmojiReaction(it) },
                onClearReaction = { viewModel.clearActiveReaction() },
                onResetSettingsToDefaults = { viewModel.resetSettingsToDefaults() },
                onSetSoundPack = { viewModel.setSoundPack(it) },
                onSetSoundVolume = { viewModel.setSoundVolume(it) },
                onToggleTalkingTokens = { viewModel.toggleTalkingTokens() },
                onSetAnimatedBackground = { viewModel.setAnimatedBackground(it) },
                onPreviewSoundPack = { viewModel.previewSoundPack(it) },
                modifier = modifier
            )
        }

        AppScreen.WEB_VIEW -> {
            WebViewScreen(
                onBackToNative = { currentScreen = AppScreen.HOME },
                modifier = modifier
            )
        }
    }

    if (showRulesDialog) {
        RulesDialog(onDismiss = { showRulesDialog = false })
    }

    if (showStatsDialog) {
        StatisticsDialog(
            statsList = playerStats,
            onResetStats = { viewModel.resetStats() },
            onDismiss = { showStatsDialog = false }
        )
    }

    if (showHistoryDialog) {
        HistoryDialog(
            historyList = matchHistory,
            onClearHistory = { viewModel.clearMatchHistory() },
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            soundEnabled = gameState.soundEnabled,
            hapticsEnabled = gameState.hapticsEnabled,
            animationSpeed = gameState.animationSpeed,
            currentSkin = gameState.currentSkin,
            aiDifficulty = gameState.aiDifficulty,
            boardTheme = gameState.boardTheme,
            soundPack = gameState.soundPack,
            soundVolume = gameState.soundVolume,
            talkingTokensEnabled = gameState.talkingTokensEnabled,
            animatedBackground = gameState.animatedBackground,
            onToggleSound = { viewModel.toggleSound() },
            onToggleHaptics = { viewModel.toggleHaptics() },
            onSetAnimationSpeed = { viewModel.setAnimationSpeed(it) },
            onSetAiDifficulty = { viewModel.setAiDifficulty(it) },
            onSetBoardTheme = { viewModel.setBoardTheme(it) },
            onSetSoundPack = { viewModel.setSoundPack(it) },
            onSetSoundVolume = { viewModel.setSoundVolume(it) },
            onToggleTalkingTokens = { viewModel.toggleTalkingTokens() },
            onSetAnimatedBackground = { viewModel.setAnimatedBackground(it) },
            onPreviewSoundPack = { viewModel.previewSoundPack(it) },
            onOpenSkins = {
                showSettingsDialog = false
                showSkinsDialog = true
            },
            onOpenRules = {
                showSettingsDialog = false
                showRulesDialog = true
            },
            onResetStats = { viewModel.resetStats() },
            onResetSettingsToDefaults = { viewModel.resetSettingsToDefaults() },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showSkinsDialog) {
        SkinsDialog(
            currentSkin = gameState.currentSkin,
            onSkinSelected = { viewModel.setSkin(it) },
            onDismiss = { showSkinsDialog = false }
        )
    }

    if (showProfileDialog) {
        val activeColor = gameState.currentPlayer?.color ?: PlayerColor.RED
        val settingsRepo = remember { com.example.data.SettingsRepository(context) }
        PlayerProfileDialog(
            initialColor = activeColor,
            getPlayerName = { color ->
                val fallback = if (gameState.players.find { it.color == color }?.isBot == true) "Bot ${color.title}" else "${color.title} Player"
                settingsRepo.getPlayerName(color, fallback)
            },
            getPlayerAvatar = { color -> settingsRepo.getPlayerAvatar(color).id },
            getPlayerStats = { color, name -> viewModel.getProfileStats(color, name) },
            onSaveProfile = { color, name, avatar ->
                viewModel.updatePlayerProfile(color, name, avatar)
            },
            onDismiss = { showProfileDialog = false }
        )
    }
}
