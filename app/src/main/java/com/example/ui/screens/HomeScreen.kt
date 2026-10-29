package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.model.PlayerColor

/**
 * Premium Game Setup Screen for JITU LUDO.
 *
 * Designed with a luxury dark brown & deep emerald theme, golden borders,
 * glowing electric blue highlights, responsive vertical scrolling, and
 * a docked "START GAME" button that is ALWAYS visible and directly accessible
 * above the system navigation bar across all phone sizes (360x800, 375x812, 412x915).
 */
@Composable
fun HomeScreen(
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onStartGame: (mode: GameMode, playerCount: Int, humanColor: PlayerColor, names: Map<PlayerColor, String>) -> Unit,
    onOpenRules: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenWebView: () -> Unit,
    onShareGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(GameMode.LOCAL_MULTIPLAYER) }
    var playerCount by remember { mutableIntStateOf(2) }
    var humanColor by remember { mutableStateOf(PlayerColor.RED) }

    val playerNames = remember {
        mutableStateMapOf(
            PlayerColor.RED to "Player 1",
            PlayerColor.YELLOW to "Player 2",
            PlayerColor.GREEN to "Player 3",
            PlayerColor.BLUE to "Player 4"
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2B140A), // Rich warm dark bronze
                        Color(0xFF1B0B04), // Deep chocolate espresso
                        Color(0xFF101C12), // Subtle deep emerald green undertone
                        Color(0xFF0A120D)  // Deep obsidian base
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // =========================================================================
            // SCROLLABLE CONTENT AREA
            // Uses Modifier.weight(1f) so all setup cards fit cleanly or scroll gracefully,
            // while the START GAME button remains pinned and 100% visible at the bottom.
            // =========================================================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. REFINED JITU LUDO TITLE PANEL / HERO CARD
                TitleHeroCard()

                // 2. SELECT GAME MODE
                GameModeSelectionCard(
                    selectedMode = selectedMode,
                    onModeSelected = { selectedMode = it }
                )

                // 3. NUMBER OF PLAYERS
                PlayerCountSelectionCard(
                    playerCount = playerCount,
                    onCountSelected = { playerCount = it }
                )

                // 4. CHOOSE YOUR COLOR (Only in Vs Computer mode)
                if (selectedMode == GameMode.VS_COMPUTER) {
                    ColorPickerCard(
                        selectedColor = humanColor,
                        onColorSelected = { humanColor = it }
                    )
                }

                // 5. PLAYER NAMES INPUTS
                PlayerNamesCard(
                    selectedMode = selectedMode,
                    playerCount = playerCount,
                    humanColor = humanColor,
                    playerNames = playerNames
                )

                // 6. GAME SETTINGS (Sound & Vibration)
                GameSettingsCard(
                    soundEnabled = soundEnabled,
                    hapticsEnabled = hapticsEnabled,
                    onToggleSound = onToggleSound,
                    onToggleHaptics = onToggleHaptics
                )

                // 7. COMPACT SECONDARY UTILITY BAR (Share, Rules, Stats, Web)
                QuickUtilityBar(
                    onShareGame = onShareGame,
                    onOpenRules = onOpenRules,
                    onOpenStats = onOpenStats,
                    onOpenWebView = onOpenWebView
                )

                // Bottom spacer inside scroll container to ensure comfortable clearance
                Spacer(modifier = Modifier.height(16.dp))
            }

            // =========================================================================
            // DOCKED / PINNED BOTTOM CONTAINER FOR "START GAME"
            // Pinned above the Android navigation bar and system safe area so it is
            // ALWAYS visible and easily accessible with the user's thumb without scrolling.
            // =========================================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xF8150A05),
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, Color(0xFFB45309).copy(alpha = 0.40f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StartGameButton(
                        onClick = {
                            onStartGame(selectedMode, playerCount, humanColor, playerNames)
                        }
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 1. Title Hero Card
// -----------------------------------------------------------------------------

@Composable
private fun TitleHeroCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_hero_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 2.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFFFFFBEB), // Pale gold glint
                    Color(0xFFFDE047), // Vibrant gold
                    Color(0xFFF59E0B), // Warm rich amber
                    Color(0xFFB45309), // Burnished bronze
                    Color(0xFFFDE047)  // Vibrant gold
                )
            )
        ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF26120A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF3D1E10), // Warm golden chocolate top
                            Color(0xFF241008), // Rich espresso mid
                            Color(0xFF140804)  // Dark deep brown base
                        )
                    )
                )
                .padding(vertical = 12.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left decorative 3D pawn + die accent
                DecorativePawnsAndDie(
                    color1 = Color(0xFFDC2626), // Red pawn
                    color2 = Color(0xFF059669), // Green pawn
                    dieValue = 6,
                    isLeft = true
                )

                // Center Title & Details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // Small golden crown icon
                    GoldenCrownIcon(modifier = Modifier.size(24.dp))

                    Spacer(modifier = Modifier.height(2.dp))

                    // "JITU LUDO" Title
                    Text(
                        text = "JITU LUDO",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color(0xFFFFFBEB),
                        style = TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFF78350F),
                                offset = Offset(0f, 3f),
                                blurRadius = 6f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // 4 Quadrant Colored Gems (Red, Green, Yellow, Blue)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Color(0xFFDC2626), // Red
                            Color(0xFF059669), // Green
                            Color(0xFFEAB308), // Yellow
                            Color(0xFF2563EB)  // Blue
                        ).forEach { gemColor ->
                            Box(
                                modifier = Modifier
                                    .size(12.dp, 8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(gemColor)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Classic Board Game • Offline & Smart AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFEF3C7).copy(alpha = 0.85f),
                        letterSpacing = 0.3.sp
                    )
                }

                // Right decorative 3D pawn + die accent
                DecorativePawnsAndDie(
                    color1 = Color(0xFF2563EB), // Blue pawn
                    color2 = Color(0xFFEAB308), // Yellow pawn
                    dieValue = 3,
                    isLeft = false
                )
            }
        }
    }
}

/**
 * Draws miniature decorative 3D pawns and a die matching the reference artwork style.
 */
@Composable
private fun DecorativePawnsAndDie(
    color1: Color,
    color2: Color,
    dieValue: Int,
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 38.dp, height = 52.dp)) {
        val w = size.width
        val h = size.height

        val p1Center = if (isLeft) Offset(w * 0.30f, h * 0.32f) else Offset(w * 0.70f, h * 0.32f)
        val p2Center = if (isLeft) Offset(w * 0.75f, h * 0.22f) else Offset(w * 0.25f, h * 0.22f)
        val dieCenter = if (isLeft) Offset(w * 0.52f, h * 0.72f) else Offset(w * 0.48f, h * 0.72f)

        fun drawMiniPawn(center: Offset, color: Color, scale: Float) {
            val r = 6.5f * scale
            // Drop shadow
            drawCircle(Color(0x30000000), radius = r * 1.1f, center = Offset(center.x, center.y + 2f))
            // Body skirt
            drawOval(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = 0.9f), color, Color(0xFF0F172A)),
                    center = center,
                    radius = r * 1.6f
                ),
                topLeft = Offset(center.x - r * 1.1f, center.y - r * 0.2f),
                size = Size(r * 2.2f, r * 1.5f)
            )
            // Head
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, color, Color(0xFF0F172A)),
                    center = Offset(center.x - r * 0.3f, center.y - r * 0.7f),
                    radius = r * 1.2f
                ),
                radius = r,
                center = Offset(center.x, center.y - r * 0.5f)
            )
            // Glint
            drawCircle(Color.White, radius = r * 0.25f, center = Offset(center.x - r * 0.3f, center.y - r * 0.8f))
        }

        drawMiniPawn(p2Center, color2, scale = 0.9f)
        drawMiniPawn(p1Center, color1, scale = 1.0f)

        // Mini 3D White Die
        val dieSize = 15f
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color.White, Color(0xFFF1F5F9), Color(0xFFCBD5E1))),
            topLeft = Offset(dieCenter.x - dieSize / 2f, dieCenter.y - dieSize / 2f),
            size = Size(dieSize, dieSize),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Die outline
        drawRoundRect(
            color = Color(0xFF94A3B8),
            topLeft = Offset(dieCenter.x - dieSize / 2f, dieCenter.y - dieSize / 2f),
            size = Size(dieSize, dieSize),
            cornerRadius = CornerRadius(3f, 3f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f)
        )
        // Mini Pip
        drawCircle(
            color = if (dieValue == 6) Color(0xFFDC2626) else Color(0xFF1E293B),
            radius = 1.8f,
            center = dieCenter
        )
    }
}

@Composable
private fun GoldenCrownIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.82f)
            lineTo(w * 0.10f, h * 0.30f)
            lineTo(w * 0.35f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.65f, h * 0.52f)
            lineTo(w * 0.90f, h * 0.30f)
            lineTo(w * 0.85f, h * 0.82f)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFFFBEB), Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFD97706))
            )
        )
        // Crown jewels
        drawCircle(Color(0xFFDC2626), radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.18f))
        drawCircle(Color(0xFF2563EB), radius = w * 0.05f, center = Offset(w * 0.10f, h * 0.30f))
        drawCircle(Color(0xFF2563EB), radius = w * 0.05f, center = Offset(w * 0.90f, h * 0.30f))
    }
}

// -----------------------------------------------------------------------------
// 2. Select Game Mode Card
// -----------------------------------------------------------------------------

@Composable
private fun GameModeSelectionCard(
    selectedMode: GameMode,
    onModeSelected: (GameMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFB45309).copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1008)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SELECT GAME MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE047),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pass & Play Mode
                ModeCard(
                    title = "Pass & Play",
                    subtitle = "Local Offline",
                    isSelected = selectedMode == GameMode.LOCAL_MULTIPLAYER,
                    onClick = { onModeSelected(GameMode.LOCAL_MULTIPLAYER) },
                    modifier = Modifier.weight(1f),
                    testTag = "mode_pass_and_play",
                    icon = {
                        // 3D White Die Icon
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = if (selectedMode == GameMode.LOCAL_MULTIPLAYER) Color.White else Color(0xFF94A3B8)
                        )
                    }
                )

                // Vs Computer Mode
                ModeCard(
                    title = "Vs Computer",
                    subtitle = "Solo vs AI Bots",
                    isSelected = selectedMode == GameMode.VS_COMPUTER,
                    onClick = { onModeSelected(GameMode.VS_COMPUTER) },
                    modifier = Modifier.weight(1f),
                    testTag = "mode_vs_computer",
                    icon = {
                        // 3D Friendly Robot Icon
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = if (selectedMode == GameMode.VS_COMPUTER) Color.White else Color(0xFF38BDF8)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0284C7), // Bright electric blue highlight
                            Color(0xFF0369A1), // Deep rich blue
                            Color(0xFF075985)  // Royal sapphire base
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF26130A), Color(0xFF1B0B04))
                    )
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) {
                    Brush.linearGradient(listOf(Color(0xFF7DD3FC), Color(0xFF0284C7), Color(0xFF38BDF8)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFF78350F).copy(alpha = 0.5f), Color(0xFF451A03).copy(alpha = 0.5f)))
                },
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Selected indicator checkmark badge at top right
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 2.dp, top = 0.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0369A1))
                    .border(1.5.dp, Color(0xFFFDE047), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFFFDE047),
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) Color.White else Color(0xFFE2E8F0)
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFF94A3B8)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 3. Number of Players Card
// -----------------------------------------------------------------------------

@Composable
private fun PlayerCountSelectionCard(
    playerCount: Int,
    onCountSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFB45309).copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1008)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NUMBER OF PLAYERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE047),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2, 3, 4).forEach { count ->
                    val isSelected = playerCount == count
                    val countAccentColor = when (count) {
                        2 -> Color(0xFF38BDF8) // Sky blue
                        3 -> Color(0xFFFBBF24) // Warm amber
                        else -> Color(0xFF4ADE80) // Emerald green
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF26130A), Color(0xFF1B0B04))
                                    )
                                }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF78350F).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onCountSelected(count) }
                            .testTag("player_count_$count"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else countAccentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$count Players",
                                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. Color Picker Card (Vs Computer)
// -----------------------------------------------------------------------------

@Composable
private fun ColorPickerCard(
    selectedColor: PlayerColor,
    onColorSelected: (PlayerColor) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFB45309).copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1008)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CHOOSE YOUR COLOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE047),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE).forEach { color ->
                    val isSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(color.lightColor, color.primaryColor, color.darkColor)
                                )
                            )
                            .border(
                                width = if (isSelected) 3.5.dp else 1.5.dp,
                                color = if (isSelected) Color.White else Color(0x66FFFFFF),
                                shape = CircleShape
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onColorSelected(color) }
                            .testTag("color_pick_${color.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text(
                                text = "YOU",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 5. Player Names Card
// -----------------------------------------------------------------------------

@Composable
private fun PlayerNamesCard(
    selectedMode: GameMode,
    playerCount: Int,
    humanColor: PlayerColor,
    playerNames: MutableMap<PlayerColor, String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFB45309).copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1008)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAYER NAMES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE047),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val activeColors = if (selectedMode == GameMode.VS_COMPUTER) {
                when (playerCount) {
                    2 -> {
                        val opposite = when (humanColor) {
                            PlayerColor.RED -> PlayerColor.YELLOW
                            PlayerColor.YELLOW -> PlayerColor.RED
                            PlayerColor.GREEN -> PlayerColor.BLUE
                            PlayerColor.BLUE -> PlayerColor.GREEN
                        }
                        listOf(humanColor, opposite)
                    }
                    3 -> {
                        val all = listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
                        val others = all.filter { it != humanColor }.take(2)
                        listOf(humanColor) + others
                    }
                    else -> {
                        val all = listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
                        listOf(humanColor) + all.filter { it != humanColor }
                    }
                }
            } else {
                when (playerCount) {
                    2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW)
                    3 -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
                    else -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
                }
            }

            activeColors.forEachIndexed { index, color ->
                val isBot = (selectedMode == GameMode.VS_COMPUTER && color != humanColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Numbered colored badge matching screenshot
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(color.lightColor, color.primaryColor, color.darkColor)
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedTextField(
                        value = if (isBot) "Bot ${color.title}" else (playerNames[color] ?: "Player ${index + 1}"),
                        onValueChange = { if (!isBot) playerNames[color] = it },
                        enabled = !isBot,
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("player_name_input_${index + 1}"),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (!isBot) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color(0xFF94A3B8),
                            focusedContainerColor = Color(0xFF140804),
                            unfocusedContainerColor = Color(0xFF140804),
                            disabledContainerColor = Color(0xFF1B0B04),
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0xFF78350F).copy(alpha = 0.5f),
                            disabledBorderColor = Color(0xFF451A03).copy(alpha = 0.4f)
                        ),
                        textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 6. Game Settings Card
// -----------------------------------------------------------------------------

@Composable
private fun GameSettingsCard(
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFB45309).copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1008)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GAME SETTINGS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE047),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two side-by-side pill cards matching reference screenshot!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sound Toggle Pill Card
                SettingTogglePill(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconTint = Color(0xFF38BDF8),
                    title = "Game Sound",
                    subtitle = "Play game sounds",
                    checked = soundEnabled,
                    onCheckedChange = { onToggleSound() },
                    testTag = "sound_toggle",
                    modifier = Modifier.weight(1f)
                )

                // Haptic Vibration Pill Card
                SettingTogglePill(
                    icon = Icons.Default.Vibration,
                    iconTint = Color(0xFFFBBF24),
                    title = "Haptic Vibration",
                    subtitle = "Feel every move",
                    checked = hapticsEnabled,
                    onCheckedChange = { onToggleHaptics() },
                    testTag = "haptics_toggle",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SettingTogglePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF26130A), Color(0xFF1B0B04))
                )
            )
            .border(1.dp, Color(0xFF78350F).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange() },
                modifier = Modifier.testTag(testTag),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF0284C7),
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFF334155)
                )
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 7. Compact Secondary Utility Bar (Share, Rules, Stats, Web)
// -----------------------------------------------------------------------------

@Composable
private fun QuickUtilityBar(
    onShareGame: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenWebView: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Share Game Button
        UtilityButton(
            text = "Share",
            icon = Icons.Default.Share,
            onClick = onShareGame,
            modifier = Modifier.weight(1f),
            testTag = "home_share_button",
            accentColor = Color(0xFF38BDF8)
        )

        // Rules Button
        UtilityButton(
            text = "Rules",
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            onClick = onOpenRules,
            modifier = Modifier.weight(1f),
            testTag = "home_rules_button",
            accentColor = Color(0xFFFDE047)
        )

        // Statistics Button
        UtilityButton(
            text = "Stats",
            icon = Icons.Default.BarChart,
            onClick = onOpenStats,
            modifier = Modifier.weight(1f),
            testTag = "home_statistics_button",
            accentColor = Color(0xFF34D399)
        )

        // Web Version Button
        UtilityButton(
            text = "Web",
            icon = Icons.Default.Language,
            onClick = onOpenWebView,
            modifier = Modifier.weight(1f),
            testTag = "home_web_version_button",
            accentColor = Color(0xFFA78BFA)
        )
    }
}

@Composable
private fun UtilityButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E1008))
            .border(1.dp, Color(0xFF78350F).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 8. Docked / Pinned START GAME Button
// -----------------------------------------------------------------------------

@Composable
private fun StartGameButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(12.dp, RoundedCornerShape(27.dp), ambientColor = Color(0xFF16A34A), spotColor = Color(0xFF22C55E))
            .clip(RoundedCornerShape(27.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF22C55E), // Vivid green top highlight
                        Color(0xFF16A34A), // Classic emerald green
                        Color(0xFF15803D), // Deep green
                        Color(0xFF14532D)  // Dark forest base
                    )
                )
            )
            .border(
                width = 2.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFFFFBEB), // Pale golden glint
                        Color(0xFFFDE047), // Brilliant gold
                        Color(0xFFF59E0B), // Warm rich amber
                        Color(0xFF78350F), // Bronze rim
                        Color(0xFFFDE047)  // Brilliant gold
                    )
                ),
                shape = RoundedCornerShape(27.dp)
            )
            .clickable { onClick() }
            .testTag("start_game_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Golden Play Arrow
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFFFFFBEB),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "START GAME",
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color(0xFFFFFBEB),
                style = TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF064E3B),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}
