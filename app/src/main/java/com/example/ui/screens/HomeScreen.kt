package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.model.PlayerColor

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Hero Logo Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_hero_card"),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF26130C))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3E2316), Color(0xFF1E100A))
                        )
                    )
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 4 Quadrant mini badge
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFE53935)))
                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF43A047)))
                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFFBC02D)))
                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF1E88E5)))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "JITU LUDO",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Color.White
                )

                Text(
                    text = "Classic Board Game • Offline & Smart AI",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Game Mode Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SELECT GAME MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeOptionCard(
                        title = "Pass & Play",
                        subtitle = "Local Offline",
                        icon = Icons.Default.Casino,
                        isSelected = selectedMode == GameMode.LOCAL_MULTIPLAYER,
                        onClick = { selectedMode = GameMode.LOCAL_MULTIPLAYER },
                        modifier = Modifier.weight(1f),
                        testTag = "mode_pass_and_play"
                    )

                    ModeOptionCard(
                        title = "vs Computer",
                        subtitle = "Solo vs AI Bots",
                        icon = Icons.Default.SmartToy,
                        isSelected = selectedMode == GameMode.VS_COMPUTER,
                        onClick = { selectedMode = GameMode.VS_COMPUTER },
                        modifier = Modifier.weight(1f),
                        testTag = "mode_vs_computer"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Player Count Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "NUMBER OF PLAYERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 3, 4).forEach { count ->
                        val isSelected = playerCount == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { playerCount = count }
                                .testTag("player_count_$count"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count Players",
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Color Picker (If vs Computer)
        if (selectedMode == GameMode.VS_COMPUTER) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CHOOSE YOUR COLOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE).forEach { color ->
                            val isSelected = humanColor == color
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(color.primaryColor)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(3.5.dp, Color.White, CircleShape)
                                        } else Modifier
                                    )
                                    .clickable { humanColor = color }
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
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4. Player Names Customization
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "PLAYER NAMES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

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
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = if (isBot) "Bot ${color.title}" else (playerNames[color] ?: "Player ${index + 1}"),
                            onValueChange = { if (!isBot) playerNames[color] = it },
                            enabled = !isBot,
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_name_input_${index + 1}"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Sound & Haptics Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Game Sound", fontSize = 14.sp)
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { onToggleSound() },
                    modifier = Modifier.testTag("sound_toggle")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Haptic Vibration", fontSize = 14.sp)
                }
                Switch(
                    checked = hapticsEnabled,
                    onCheckedChange = { onToggleHaptics() },
                    modifier = Modifier.testTag("haptics_toggle")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Action: START GAME
        Button(
            onClick = {
                onStartGame(selectedMode, playerCount, humanColor, playerNames)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_game_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "START GAME", fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Rules, Statistics, and Web Mode options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpenRules,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("home_rules_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Rules", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onOpenStats,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("home_statistics_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
            ) {
                Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Stats", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onOpenWebView,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("home_web_version_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Web", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
