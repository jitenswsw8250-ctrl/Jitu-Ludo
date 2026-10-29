package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.model.GameMode
import com.example.model.GameState
import com.example.model.PlayerColor
import com.example.ui.components.DiceControlSection
import com.example.ui.components.LudoBoardView
import com.example.ui.components.PlayerDiceControl
import com.example.ui.components.PlayerStatusBar
import com.example.ui.components.TopPlayerProfileCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    gameState: GameState,
    onRollDice: () -> Unit,
    onTokenClicked: (Int) -> Unit,
    onRestartGame: () -> Unit,
    onNavigateHome: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit = {},
    onSetAnimationSpeed: (com.example.model.AnimationSpeed) -> Unit = {},
    onSetSkin: (com.example.model.GameSkin) -> Unit = {},
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRulesDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showSkinsDialog by remember { mutableStateOf(false) }
    var boardBounds by remember { mutableStateOf<Rect?>(null) }
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    BackHandler {
        onNavigateHome()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoordinates = it }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF381F13), // Warm golden bronze top
                        Color(0xFF241209), // Warm chocolate brown middle
                        Color(0xFF140804)  // Deep espresso brown bottom
                    )
                )
            )
    ) {
        val screenHeight = maxHeight
        // Calculate max available board height so that the board, dice controls, bottom actions, and reserved banner fit comfortably
        val maxBoardDimension = (screenHeight - 200.dp).coerceAtLeast(240.dp)

        // Subtle Glowing Star Sparkles in Background (matching reference photo)
        BackgroundSparklesCanvas(
            boardBounds = boardBounds,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val greenPlayer = gameState.players.find { it.color == PlayerColor.GREEN }
            val yellowPlayer = gameState.players.find { it.color == PlayerColor.YELLOW }
            val redPlayer = gameState.players.find { it.color == PlayerColor.RED }
            val bluePlayer = gameState.players.find { it.color == PlayerColor.BLUE }

            // Balanced top spacer for vertical screen centering on tall Android screens
            Spacer(modifier = Modifier.weight(1f))

            // 1. TOP PLAYER DICE CONTROLS (Green at Top-Left, Yellow at Top-Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (greenPlayer != null) {
                    PlayerDiceControl(
                        player = greenPlayer,
                        isCurrentTurn = gameState.currentPlayer?.color == PlayerColor.GREEN,
                        turnPhase = gameState.phase,
                        diceState = gameState.diceState,
                        isGameOver = gameState.isGameOver,
                        onRollDice = onRollDice,
                        alignRight = false,
                        skin = gameState.currentSkin
                    )
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                if (yellowPlayer != null) {
                    PlayerDiceControl(
                        player = yellowPlayer,
                        isCurrentTurn = gameState.currentPlayer?.color == PlayerColor.YELLOW,
                        turnPhase = gameState.phase,
                        diceState = gameState.diceState,
                        isGameOver = gameState.isGameOver,
                        onRollDice = onRollDice,
                        alignRight = true,
                        skin = gameState.currentSkin
                    )
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. THE LUDO BOARD (Responsive, square, centered, golden framed with 3D tokens)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = minOf(440.dp, maxBoardDimension))
                    .onGloballyPositioned { boardCoords ->
                        val root = rootCoordinates
                        if (root != null && root.isAttached && boardCoords.isAttached) {
                            val topLeft = root.localPositionOf(boardCoords, Offset.Zero)
                            val boardSize = boardCoords.size
                            boardBounds = Rect(
                                offset = topLeft,
                                size = Size(boardSize.width.toFloat(), boardSize.height.toFloat())
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                LudoBoardView(
                    gameState = gameState,
                    onTokenClicked = onTokenClicked
                )
            }

            // Direct 20–30 dp spacing positioning the active player's dice control directly below the board
            Spacer(modifier = Modifier.height(24.dp))

            // 3. BOTTOM PLAYER DICE CONTROLS (Red at Bottom-Left, Blue at Bottom-Right)
            // Positioned directly below the board with comfortable spacing and easy thumb access
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .testTag("active_dice_controls_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (redPlayer != null) {
                    PlayerDiceControl(
                        player = redPlayer,
                        isCurrentTurn = gameState.currentPlayer?.color == PlayerColor.RED,
                        turnPhase = gameState.phase,
                        diceState = gameState.diceState,
                        isGameOver = gameState.isGameOver,
                        onRollDice = onRollDice,
                        alignRight = false,
                        skin = gameState.currentSkin
                    )
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Center turn bonus badge if consecutive sixes
                if (gameState.diceState.consecutiveSixes > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFDC2626),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "SIX x${gameState.diceState.consecutiveSixes} 🔥",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (bluePlayer != null) {
                    PlayerDiceControl(
                        player = bluePlayer,
                        isCurrentTurn = gameState.currentPlayer?.color == PlayerColor.BLUE,
                        turnPhase = gameState.phase,
                        diceState = gameState.diceState,
                        isGameOver = gameState.isGameOver,
                        onRollDice = onRollDice,
                        alignRight = true,
                        skin = gameState.currentSkin
                    )
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // Balanced spacing to push bottom action bar down responsively while centering game content
            Spacer(modifier = Modifier.weight(1f))

            // 4. BOTTOM ACTION BAR (Circular vibrant green buttons matching reference photo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Green Back Button
                CircularGreenButton(
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = onNavigateHome,
                    contentDescription = "Back to Home",
                    testTag = "game_back_home_button"
                )

                // Center Game Mode Label (e.g. "Solo Classic" or "Pass & Play")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (gameState.gameMode) {
                            GameMode.VS_COMPUTER -> "Solo"
                            GameMode.LOCAL_MULTIPLAYER -> "Pass & Play"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = when (gameState.gameMode) {
                            GameMode.VS_COMPUTER -> "Classic"
                            GameMode.LOCAL_MULTIPLAYER -> "${gameState.players.size} Players"
                        },
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color(0xFF93C5FD)
                    )
                }

                // Right action buttons: Sound Mute/Unmute, Statistics, and Settings Gear
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularGreenButton(
                        icon = {
                            Icon(
                                imageVector = if (gameState.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = if (gameState.soundEnabled) "Mute Sound" else "Unmute Sound",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        onClick = onToggleSound,
                        contentDescription = "Sound Toggle",
                        testTag = "game_quick_sound_button"
                    )

                    CircularGreenButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Statistics",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        onClick = onOpenStats,
                        contentDescription = "Statistics",
                        testTag = "game_statistics_button"
                    )

                    CircularGreenButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        onClick = { showSettingsMenu = true },
                        contentDescription = "Settings",
                        testTag = "game_settings_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5. RESERVED BANNER ADVERTISEMENT AREA
            // Dedicated empty space near the bottom for future banner ads.
            // Separated completely from bottom navigation buttons to prevent overlap.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("reserved_banner_ad_area"),
                contentAlignment = Alignment.Center
            ) {
                // Future banner advertisement space
            }
        }
    }

    // Settings Modal
    if (showSettingsMenu) {
        SettingsDialog(
            soundEnabled = gameState.soundEnabled,
            hapticsEnabled = gameState.hapticsEnabled,
            animationSpeed = gameState.animationSpeed,
            currentSkin = gameState.currentSkin,
            onToggleSound = onToggleSound,
            onToggleHaptics = onToggleHaptics,
            onSetAnimationSpeed = onSetAnimationSpeed,
            onOpenSkins = {
                showSettingsMenu = false
                showSkinsDialog = true
            },
            onOpenRules = {
                showSettingsMenu = false
                showRulesDialog = true
            },
            onRestartMatch = {
                showSettingsMenu = false
                onRestartGame()
            },
            onDismiss = { showSettingsMenu = false }
        )
    }

    // Skins Customization Modal
    if (showSkinsDialog) {
        SkinsDialog(
            currentSkin = gameState.currentSkin,
            onSkinSelected = onSetSkin,
            onDismiss = { showSkinsDialog = false }
        )
    }

    // Victory Celebration Modal
    if (gameState.isGameOver) {
        VictoryDialog(
            podium = gameState.winnerPodium,
            allPlayers = gameState.players,
            onPlayAgain = onRestartGame,
            onHome = onNavigateHome,
            onOpenStats = onOpenStats
        )
    }

    // Rules Modal
    if (showRulesDialog) {
        RulesDialog(onDismiss = { showRulesDialog = false })
    }
}

private enum class SparkleZone {
    TOP,
    BOTTOM,
    LEFT_FLANK,
    RIGHT_FLANK,
    CORNER_TOP_LEFT,
    CORNER_TOP_RIGHT,
    CORNER_BOTTOM_LEFT,
    CORNER_BOTTOM_RIGHT
}

private data class SparkleItem(
    val zone: SparkleZone,
    val relX: Float,
    val relY: Float,
    val speedX: Float,
    val speedY: Float,
    val phaseX: Float,
    val phaseY: Float,
    val speedHarmonic: Float,
    val phaseHarmonic: Float,
    val glowSpeed: Float,
    val glowPhase: Float,
    val isLarge: Boolean
)

private val SPARKLE_ITEMS = listOf(
    // 0: TOP - Upper Left (Large)
    SparkleItem(
        zone = SparkleZone.TOP,
        relX = 0.16f, relY = 0.40f,
        speedX = 0.22f, speedY = 0.18f,
        phaseX = 0.4f, phaseY = 1.1f,
        speedHarmonic = 0.13f, phaseHarmonic = 0.8f,
        glowSpeed = 0.95f, glowPhase = 0.0f,
        isLarge = true
    ),
    // 1: TOP - Center (Small)
    SparkleItem(
        zone = SparkleZone.TOP,
        relX = 0.50f, relY = 0.65f,
        speedX = 0.28f, speedY = 0.24f,
        phaseX = 2.4f, phaseY = 0.6f,
        speedHarmonic = 0.16f, phaseHarmonic = 2.1f,
        glowSpeed = 1.20f, glowPhase = 1.7f,
        isLarge = false
    ),
    // 2: TOP - Upper Right (Large)
    SparkleItem(
        zone = SparkleZone.TOP,
        relX = 0.84f, relY = 0.35f,
        speedX = 0.19f, speedY = 0.26f,
        phaseX = 4.2f, phaseY = 3.0f,
        speedHarmonic = 0.11f, phaseHarmonic = 4.5f,
        glowSpeed = 0.85f, glowPhase = 3.2f,
        isLarge = true
    ),
    // 3: CORNER - Top Left Orbiter (Small)
    SparkleItem(
        zone = SparkleZone.CORNER_TOP_LEFT,
        relX = 0f, relY = 0f,
        speedX = 0.17f, speedY = 0.21f,
        phaseX = 1.2f, phaseY = 2.5f,
        speedHarmonic = 0.10f, phaseHarmonic = 0.5f,
        glowSpeed = 1.10f, glowPhase = 4.1f,
        isLarge = false
    ),
    // 4: CORNER - Top Right Orbiter (Small)
    SparkleItem(
        zone = SparkleZone.CORNER_TOP_RIGHT,
        relX = 0f, relY = 0f,
        speedX = 0.21f, speedY = 0.16f,
        phaseX = 3.5f, phaseY = 0.9f,
        speedHarmonic = 0.12f, phaseHarmonic = 3.2f,
        glowSpeed = 1.30f, glowPhase = 1.2f,
        isLarge = false
    ),
    // 5: LEFT FLANK - Mid-Upper (Large)
    SparkleItem(
        zone = SparkleZone.LEFT_FLANK,
        relX = 0.5f, relY = 0.35f,
        speedX = 0.20f, speedY = 0.15f,
        phaseX = 0.8f, phaseY = 4.4f,
        speedHarmonic = 0.11f, phaseHarmonic = 1.9f,
        glowSpeed = 0.90f, glowPhase = 2.7f,
        isLarge = true
    ),
    // 6: LEFT FLANK - Mid-Lower (Small)
    SparkleItem(
        zone = SparkleZone.LEFT_FLANK,
        relX = 0.5f, relY = 0.70f,
        speedX = 0.25f, speedY = 0.19f,
        phaseX = 5.1f, phaseY = 1.7f,
        speedHarmonic = 0.14f, phaseHarmonic = 4.0f,
        glowSpeed = 1.05f, glowPhase = 5.3f,
        isLarge = false
    ),
    // 7: RIGHT FLANK - Mid-Upper (Large)
    SparkleItem(
        zone = SparkleZone.RIGHT_FLANK,
        relX = 0.5f, relY = 0.30f,
        speedX = 0.23f, speedY = 0.17f,
        phaseX = 2.9f, phaseY = 5.2f,
        speedHarmonic = 0.12f, phaseHarmonic = 2.8f,
        glowSpeed = 1.00f, glowPhase = 0.8f,
        isLarge = true
    ),
    // 8: RIGHT FLANK - Mid-Lower (Small)
    SparkleItem(
        zone = SparkleZone.RIGHT_FLANK,
        relX = 0.5f, relY = 0.65f,
        speedX = 0.18f, speedY = 0.22f,
        phaseX = 4.6f, phaseY = 3.6f,
        speedHarmonic = 0.09f, phaseHarmonic = 5.1f,
        glowSpeed = 1.15f, glowPhase = 3.9f,
        isLarge = false
    ),
    // 9: CORNER - Bottom Left Orbiter (Small)
    SparkleItem(
        zone = SparkleZone.CORNER_BOTTOM_LEFT,
        relX = 0f, relY = 0f,
        speedX = 0.19f, speedY = 0.23f,
        phaseX = 2.1f, phaseY = 4.0f,
        speedHarmonic = 0.11f, phaseHarmonic = 1.5f,
        glowSpeed = 0.92f, glowPhase = 2.0f,
        isLarge = false
    ),
    // 10: CORNER - Bottom Right Orbiter (Small)
    SparkleItem(
        zone = SparkleZone.CORNER_BOTTOM_RIGHT,
        relX = 0f, relY = 0f,
        speedX = 0.22f, speedY = 0.18f,
        phaseX = 0.5f, phaseY = 2.2f,
        speedHarmonic = 0.13f, phaseHarmonic = 3.8f,
        glowSpeed = 1.25f, glowPhase = 4.6f,
        isLarge = false
    ),
    // 11: BOTTOM - Lower Left (Large)
    SparkleItem(
        zone = SparkleZone.BOTTOM,
        relX = 0.18f, relY = 0.45f,
        speedX = 0.24f, speedY = 0.19f,
        phaseX = 3.7f, phaseY = 1.4f,
        speedHarmonic = 0.14f, phaseHarmonic = 0.9f,
        glowSpeed = 1.00f, glowPhase = 1.5f,
        isLarge = true
    ),
    // 12: BOTTOM - Center (Large)
    SparkleItem(
        zone = SparkleZone.BOTTOM,
        relX = 0.52f, relY = 0.35f,
        speedX = 0.20f, speedY = 0.25f,
        phaseX = 1.8f, phaseY = 5.0f,
        speedHarmonic = 0.10f, phaseHarmonic = 2.4f,
        glowSpeed = 0.88f, glowPhase = 3.4f,
        isLarge = true
    ),
    // 13: BOTTOM - Lower Right (Small)
    SparkleItem(
        zone = SparkleZone.BOTTOM,
        relX = 0.82f, relY = 0.55f,
        speedX = 0.26f, speedY = 0.21f,
        phaseX = 5.4f, phaseY = 2.9f,
        speedHarmonic = 0.15f, phaseHarmonic = 4.7f,
        glowSpeed = 1.18f, glowPhase = 5.8f,
        isLarge = false
    )
)

@Composable
fun BackgroundSparklesCanvas(
    boardBounds: Rect? = null,
    modifier: Modifier = Modifier
) {
    // Continuous, monotonic time stream across entire play session with zero reset/loop jumps
    val timeSeconds by produceState(0.0) {
        val startNanos = System.nanoTime()
        while (true) {
            withFrameNanos { nowNanos ->
                value = (nowNanos - startNanos) / 1_000_000_000.0
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val t = timeSeconds.toFloat()
        val screenPad = 8.dp.toPx()
        val boardSafetyClearance = 15.dp.toPx()

        // Calculate dynamic exclusion zone for the Ludo board
        val exclLeft: Float
        val exclTop: Float
        val exclRight: Float
        val exclBottom: Float

        if (boardBounds != null && boardBounds.width > 50f && boardBounds.height > 50f) {
            exclLeft = boardBounds.left - boardSafetyClearance
            exclTop = boardBounds.top - boardSafetyClearance
            exclRight = boardBounds.right + boardSafetyClearance
            exclBottom = boardBounds.bottom + boardSafetyClearance
        } else {
            val estSize = minOf(w - 24.dp.toPx(), 440.dp.toPx()).coerceAtLeast(100f)
            val left = (w - estSize) / 2f
            val top = h * 0.20f
            exclLeft = left - boardSafetyClearance
            exclTop = top - boardSafetyClearance
            exclRight = left + estSize + boardSafetyClearance
            exclBottom = top + estSize + boardSafetyClearance
        }

        for (item in SPARKLE_ITEMS) {
            val r = if (item.isLarge) 5.dp.toPx() else 3.5.dp.toPx()
            val totalRadius = r * 2.5f

            var smoothX: Float
            var smoothY: Float

            when (item.zone) {
                SparkleZone.TOP -> {
                    val minX = screenPad + totalRadius
                    val maxX = w - screenPad - totalRadius
                    val minY = screenPad + totalRadius
                    val maxY = exclTop - totalRadius
                    val availH = (maxY - minY).coerceAtLeast(0f)

                    if (availH <= 4.dp.toPx()) {
                        smoothX = minX + (maxX - minX) * item.relX
                        smoothY = (screenPad + exclTop) / 2f
                    } else {
                        val anchorY = minY + availH * item.relY
                        val ampY = minOf(availH * 0.35f, 16.dp.toPx())
                        val anchorX = minX + (maxX - minX) * item.relX
                        val ampX = minOf((maxX - minX) * 0.18f, 42.dp.toPx())

                        smoothX = anchorX + ampX * (0.70f * sin(t * item.speedX + item.phaseX) + 0.30f * cos(t * item.speedHarmonic + item.phaseHarmonic))
                        smoothY = anchorY + ampY * (0.75f * cos(t * item.speedY + item.phaseY) + 0.25f * sin(t * item.speedHarmonic * 0.8f + item.phaseHarmonic))
                    }
                }
                SparkleZone.BOTTOM -> {
                    val minX = screenPad + totalRadius
                    val maxX = w - screenPad - totalRadius
                    val minY = exclBottom + totalRadius
                    val maxY = h - screenPad - totalRadius
                    val availH = (maxY - minY).coerceAtLeast(0f)

                    if (availH <= 4.dp.toPx()) {
                        smoothX = minX + (maxX - minX) * item.relX
                        smoothY = (exclBottom + h - screenPad) / 2f
                    } else {
                        val anchorY = minY + availH * item.relY
                        val ampY = minOf(availH * 0.35f, 18.dp.toPx())
                        val anchorX = minX + (maxX - minX) * item.relX
                        val ampX = minOf((maxX - minX) * 0.20f, 45.dp.toPx())

                        smoothX = anchorX + ampX * (0.65f * sin(t * item.speedX + item.phaseX) + 0.35f * cos(t * item.speedHarmonic + item.phaseHarmonic))
                        smoothY = anchorY + ampY * (0.70f * cos(t * item.speedY + item.phaseY) + 0.30f * sin(t * item.speedHarmonic * 0.75f + item.phaseHarmonic))
                    }
                }
                SparkleZone.LEFT_FLANK -> {
                    val minX = screenPad + totalRadius
                    val maxX = exclLeft - totalRadius
                    val availW = (maxX - minX).coerceAtLeast(0f)
                    val minY = exclTop + totalRadius
                    val maxY = exclBottom - totalRadius
                    val availH = (maxY - minY).coerceAtLeast(0f)

                    if (availW <= 4.dp.toPx()) {
                        smoothX = (screenPad + exclLeft) / 2f
                    } else {
                        val anchorX = minX + availW * 0.5f
                        val ampX = minOf(availW * 0.35f, 8.dp.toPx())
                        smoothX = anchorX + ampX * (0.70f * sin(t * item.speedX + item.phaseX) + 0.30f * cos(t * item.speedHarmonic + item.phaseHarmonic))
                    }

                    if (availH <= 4.dp.toPx()) {
                        smoothY = (exclTop + exclBottom) / 2f
                    } else {
                        val anchorY = minY + availH * item.relY
                        val ampY = minOf(availH * 0.25f, 32.dp.toPx())
                        smoothY = anchorY + ampY * (0.75f * cos(t * item.speedY + item.phaseY) + 0.25f * sin(t * item.speedHarmonic + item.phaseHarmonic))
                    }
                }
                SparkleZone.RIGHT_FLANK -> {
                    val minX = exclRight + totalRadius
                    val maxX = w - screenPad - totalRadius
                    val availW = (maxX - minX).coerceAtLeast(0f)
                    val minY = exclTop + totalRadius
                    val maxY = exclBottom - totalRadius
                    val availH = (maxY - minY).coerceAtLeast(0f)

                    if (availW <= 4.dp.toPx()) {
                        smoothX = (exclRight + w - screenPad) / 2f
                    } else {
                        val anchorX = minX + availW * 0.5f
                        val ampX = minOf(availW * 0.35f, 8.dp.toPx())
                        smoothX = anchorX + ampX * (0.70f * sin(t * item.speedX + item.phaseX) + 0.30f * cos(t * item.speedHarmonic + item.phaseHarmonic))
                    }

                    if (availH <= 4.dp.toPx()) {
                        smoothY = (exclTop + exclBottom) / 2f
                    } else {
                        val anchorY = minY + availH * item.relY
                        val ampY = minOf(availH * 0.25f, 32.dp.toPx())
                        smoothY = anchorY + ampY * (0.75f * cos(t * item.speedY + item.phaseY) + 0.25f * sin(t * item.speedHarmonic + item.phaseHarmonic))
                    }
                }
                SparkleZone.CORNER_TOP_LEFT -> {
                    val radLeft = (exclLeft - screenPad - totalRadius).coerceAtLeast(4.dp.toPx())
                    val radTop = (exclTop - screenPad - totalRadius).coerceAtLeast(10.dp.toPx())
                    val baseRadX = minOf(radLeft * 0.75f, 30.dp.toPx())
                    val baseRadY = minOf(radTop * 0.75f, 30.dp.toPx())

                    if (baseRadX <= 3.dp.toPx()) {
                        smoothX = (screenPad + exclLeft) / 2f
                        smoothY = screenPad + totalRadius + 12.dp.toPx()
                    } else {
                        // Smoothly sweeps between upper space and left flank around the top-left corner
                        val angleSweep = 1.35f * PI.toFloat() + 0.60f * (sin(t * item.speedX + item.phaseX) * 0.7f + cos(t * item.speedHarmonic + item.phaseHarmonic) * 0.3f)
                        val rX = baseRadX * (0.85f + 0.15f * sin(t * item.speedY + item.phaseY))
                        val rY = baseRadY * (0.85f + 0.15f * cos(t * item.speedY + item.phaseY))
                        smoothX = exclLeft + rX * cos(angleSweep)
                        smoothY = exclTop + rY * sin(angleSweep)
                    }
                }
                SparkleZone.CORNER_TOP_RIGHT -> {
                    val radRight = (w - screenPad - totalRadius - exclRight).coerceAtLeast(4.dp.toPx())
                    val radTop = (exclTop - screenPad - totalRadius).coerceAtLeast(10.dp.toPx())
                    val baseRadX = minOf(radRight * 0.75f, 30.dp.toPx())
                    val baseRadY = minOf(radTop * 0.75f, 30.dp.toPx())

                    if (baseRadX <= 3.dp.toPx()) {
                        smoothX = (exclRight + w - screenPad) / 2f
                        smoothY = screenPad + totalRadius + 12.dp.toPx()
                    } else {
                        // Sweeps between upper space and right flank around top-right corner
                        val angleSweep = 1.65f * PI.toFloat() + 0.60f * (sin(t * item.speedX + item.phaseX) * 0.7f + cos(t * item.speedHarmonic + item.phaseHarmonic) * 0.3f)
                        val rX = baseRadX * (0.85f + 0.15f * sin(t * item.speedY + item.phaseY))
                        val rY = baseRadY * (0.85f + 0.15f * cos(t * item.speedY + item.phaseY))
                        smoothX = exclRight + rX * cos(angleSweep)
                        smoothY = exclTop + rY * sin(angleSweep)
                    }
                }
                SparkleZone.CORNER_BOTTOM_LEFT -> {
                    val radLeft = (exclLeft - screenPad - totalRadius).coerceAtLeast(4.dp.toPx())
                    val radBottom = (h - screenPad - totalRadius - exclBottom).coerceAtLeast(10.dp.toPx())
                    val baseRadX = minOf(radLeft * 0.75f, 30.dp.toPx())
                    val baseRadY = minOf(radBottom * 0.75f, 30.dp.toPx())

                    if (baseRadX <= 3.dp.toPx()) {
                        smoothX = (screenPad + exclLeft) / 2f
                        smoothY = exclBottom + totalRadius + 12.dp.toPx()
                    } else {
                        // Sweeps between left flank and bottom space around bottom-left corner
                        val angleSweep = 0.65f * PI.toFloat() + 0.60f * (sin(t * item.speedX + item.phaseX) * 0.7f + cos(t * item.speedHarmonic + item.phaseHarmonic) * 0.3f)
                        val rX = baseRadX * (0.85f + 0.15f * sin(t * item.speedY + item.phaseY))
                        val rY = baseRadY * (0.85f + 0.15f * cos(t * item.speedY + item.phaseY))
                        smoothX = exclLeft + rX * cos(angleSweep)
                        smoothY = exclBottom + rY * sin(angleSweep)
                    }
                }
                SparkleZone.CORNER_BOTTOM_RIGHT -> {
                    val radRight = (w - screenPad - totalRadius - exclRight).coerceAtLeast(4.dp.toPx())
                    val radBottom = (h - screenPad - totalRadius - exclBottom).coerceAtLeast(10.dp.toPx())
                    val baseRadX = minOf(radRight * 0.75f, 30.dp.toPx())
                    val baseRadY = minOf(radBottom * 0.75f, 30.dp.toPx())

                    if (baseRadX <= 3.dp.toPx()) {
                        smoothX = (exclRight + w - screenPad) / 2f
                        smoothY = exclBottom + totalRadius + 12.dp.toPx()
                    } else {
                        // Sweeps between right flank and bottom space around bottom-right corner
                        val angleSweep = 0.35f * PI.toFloat() + 0.60f * (sin(t * item.speedX + item.phaseX) * 0.7f + cos(t * item.speedHarmonic + item.phaseHarmonic) * 0.3f)
                        val rX = baseRadX * (0.85f + 0.15f * sin(t * item.speedY + item.phaseY))
                        val rY = baseRadY * (0.85f + 0.15f * cos(t * item.speedY + item.phaseY))
                        smoothX = exclRight + rX * cos(angleSweep)
                        smoothY = exclBottom + rY * sin(angleSweep)
                    }
                }
            }

            // Screen boundary clamping
            var finalX = smoothX.coerceIn(screenPad + totalRadius, w - screenPad - totalRadius)
            var finalY = smoothY.coerceIn(screenPad + totalRadius, h - screenPad - totalRadius)

            // Dynamic Deflection: Never allow overlap onto the board or border
            val safeLeft = exclLeft - totalRadius
            val safeRight = exclRight + totalRadius
            val safeTop = exclTop - totalRadius
            val safeBottom = exclBottom + totalRadius

            if (finalX in safeLeft..safeRight && finalY in safeTop..safeBottom) {
                val dL = finalX - safeLeft
                val dR = safeRight - finalX
                val dT = finalY - safeTop
                val dB = safeBottom - finalY
                val minD = minOf(dL, dR, dT, dB)
                when (minD) {
                    dT -> finalY = safeTop
                    dB -> finalY = safeBottom
                    dL -> finalX = safeLeft
                    else -> finalX = safeRight
                }
            }

            val finalPt = Offset(finalX, finalY)

            // Subtle glowing shimmer (independent gentle pulse for each sparkle)
            val shimmer = ((sin(t * item.glowSpeed + item.glowPhase) + 1f) * 0.5f).coerceIn(0f, 1f)

            // 1. Soft glowing outer halo
            val haloRadius = r * (2.1f + 0.45f * shimmer)
            val haloAlpha = (0.16f + 0.16f * shimmer).coerceIn(0.12f, 0.35f)
            drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = haloAlpha),
                radius = haloRadius,
                center = finalPt
            )

            // 2. Subtle warm inner golden halo
            drawCircle(
                color = Color(0xFFFEF3C7).copy(alpha = haloAlpha * 0.6f),
                radius = r * 1.15f,
                center = finalPt
            )

            // 3. 4-point golden star cross with subtle breathing arm length & brightness
            val armLen = r * (1.5f + 0.3f * shimmer)
            val armThick = 1.4.dp.toPx()
            val crossColor = Color(0xFFFFFBEB).copy(alpha = (0.82f + 0.18f * shimmer).coerceIn(0.75f, 1f))
            drawLine(
                color = crossColor,
                start = Offset(finalPt.x - armLen, finalPt.y),
                end = Offset(finalPt.x + armLen, finalPt.y),
                strokeWidth = armThick
            )
            drawLine(
                color = crossColor,
                start = Offset(finalPt.x, finalPt.y - armLen),
                end = Offset(finalPt.x, finalPt.y + armLen),
                strokeWidth = armThick
            )

            // 4. Center pin dot
            val centerDotAlpha = (0.90f + 0.10f * shimmer).coerceIn(0.85f, 1f)
            drawCircle(
                color = Color.White.copy(alpha = centerDotAlpha),
                radius = 1.5.dp.toPx(),
                center = finalPt
            )
        }
    }
}

@Composable
fun CircularGreenButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF22C55E), // Light vibrant green
                        Color(0xFF16A34A), // Classic emerald green
                        Color(0xFF15803D)  // Deep rich green
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.85f), Color(0xFF14532D))
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
