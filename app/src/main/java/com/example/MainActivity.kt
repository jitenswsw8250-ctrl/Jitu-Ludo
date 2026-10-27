package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.LudoViewModel
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RulesDialog
import com.example.ui.screens.StatisticsDialog
import com.example.ui.screens.WebViewScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
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
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    LudoAppRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun LudoAppRoot(viewModel: LudoViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var showRulesModal by remember { mutableStateOf(false) }
    var showStatsModal by remember { mutableStateOf(false) }

    val gameState by viewModel.gameState.collectAsState()
    val playerStats by viewModel.playerStats.collectAsState()

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                soundEnabled = gameState.soundEnabled,
                hapticsEnabled = gameState.hapticsEnabled,
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onStartGame = { mode, playerCount, humanColor, names ->
                    viewModel.playButtonClick()
                    viewModel.startNewGame(
                        mode = mode,
                        playerCount = playerCount,
                        humanColor = humanColor,
                        customNames = names
                    )
                    currentScreen = AppScreen.GAME
                },
                onOpenRules = {
                    viewModel.playButtonClick()
                    showRulesModal = true
                },
                onOpenStats = {
                    viewModel.playButtonClick()
                    showStatsModal = true
                },
                onOpenWebView = {
                    viewModel.playButtonClick()
                    currentScreen = AppScreen.WEB_VIEW
                }
            )
        }
        AppScreen.GAME -> {
            GameScreen(
                gameState = gameState,
                onRollDice = { viewModel.rollDice() },
                onTokenClicked = { tokenId -> viewModel.selectToken(tokenId) },
                onRestartGame = {
                    viewModel.playButtonClick()
                    viewModel.startNewGame(
                        mode = gameState.gameMode,
                        playerCount = gameState.players.size,
                        humanColor = gameState.players.firstOrNull { !it.isBot }?.color ?: com.example.model.PlayerColor.RED
                    )
                },
                onNavigateHome = {
                    viewModel.playButtonClick()
                    currentScreen = AppScreen.HOME
                },
                onToggleSound = { viewModel.toggleSound() },
                onOpenStats = {
                    viewModel.playButtonClick()
                    showStatsModal = true
                }
            )
        }
        AppScreen.WEB_VIEW -> {
            WebViewScreen(
                onBackToNative = {
                    viewModel.playButtonClick()
                    currentScreen = AppScreen.HOME
                }
            )
        }
    }

    if (showRulesModal) {
        RulesDialog(onDismiss = {
            viewModel.playButtonClick()
            showRulesModal = false
        })
    }

    if (showStatsModal) {
        StatisticsDialog(
            statsList = playerStats,
            onResetStats = {
                viewModel.playButtonClick()
                viewModel.resetStats()
            },
            onDismiss = {
                viewModel.playButtonClick()
                showStatsModal = false
            }
        )
    }
}
