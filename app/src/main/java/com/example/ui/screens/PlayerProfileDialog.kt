package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.PlayerStatsRecord
import com.example.model.PlayerAvatar
import com.example.model.PlayerColor

/**
 * Dedicated Player Profile Screen / Dialog for JITU LUDO.
 *
 * Allows viewing & editing player name, selecting from all 10 avatars,
 * and viewing locally stored statistics:
 * 1. Total matches played
 * 2. Matches won
 * 3. Matches lost
 * 4. Win percentage
 * 5. Total tokens captured
 * 6. Total tokens safely brought home
 * 7. Best winning streak
 *
 * All data is saved and restored locally without requiring an account.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerProfileDialog(
    initialColor: PlayerColor = PlayerColor.RED,
    getPlayerName: (PlayerColor) -> String,
    getPlayerAvatar: (PlayerColor) -> String,
    getPlayerStats: (PlayerColor, String) -> PlayerStatsRecord,
    onSaveProfile: (PlayerColor, String, PlayerAvatar) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    var currentName by remember(selectedColor) {
        mutableStateOf(getPlayerName(selectedColor))
    }
    var currentAvatar by remember(selectedColor) {
        mutableStateOf(PlayerAvatar.fromId(getPlayerAvatar(selectedColor)))
    }
    var showSavedToast by remember { mutableStateOf(false) }

    val stats = getPlayerStats(selectedColor, currentName)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF261208),
                            Color(0xFF190B04),
                            Color(0xFF0F0602)
                        )
                    )
                )
                .border(
                    2.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFD700), Color(0xFFF59E0B), Color(0xFF78350F))
                    ),
                    RoundedCornerShape(26.dp)
                )
                .testTag("player_profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Icon, Title, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = BorderStroke(1.5.dp, Color(0xFFFFD700)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = currentAvatar.emoji, fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PLAYER PROFILE",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFFFFD700),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Personal Stats & Customization",
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("profile_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Color Selector Tabs (Red, Green, Yellow, Blue)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF140804))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PlayerColor.values().forEach { color ->
                        val isSelected = (color == selectedColor)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) color.primaryColor else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) Color(0xFFFFD700) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedColor = color
                                    showSavedToast = false
                                }
                                .testTag("profile_color_tab_${color.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = color.title,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content: Name Edit, Avatar Grid, and 7 Statistics Cards
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Player Name Edit Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF220E06)),
                        border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "PLAYER NAME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = currentName,
                                onValueChange = {
                                    if (it.length <= 16) {
                                        currentName = it
                                        showSavedToast = false
                                    }
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                placeholder = {
                                    Text(text = "Enter player name", color = Color(0xFF64748B))
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFD700),
                                    unfocusedBorderColor = Color(0xFF9A3412),
                                    focusedContainerColor = Color(0xFF140804),
                                    unfocusedContainerColor = Color(0xFF140804)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input")
                            )
                        }
                    }

                    // 2. Select Avatar (10 Avatars)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF220E06)),
                        border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CHOOSE AVATAR (10 CHOICES)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${currentAvatar.title} • ${currentAvatar.subtitle}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlayerAvatar.values().forEach { avatar ->
                                    val isSelected = (avatar == currentAvatar)
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) Color(0xFF58230D) else Color(0xFF140804)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFFFFD700) else Color(0xFF451A03),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                currentAvatar = avatar
                                                showSavedToast = false
                                            }
                                            .testTag("avatar_choice_${avatar.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = avatar.emoji, fontSize = 24.sp)
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(2.dp)
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFFD700)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF140804),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Complete Player Statistics Grid (7 requested statistics)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF220E06)),
                        border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "LOCALLY SAVED CAREER STATS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 1: Matches Played & Matches Won
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileStatCard(
                                    title = "Matches Played",
                                    value = "${stats.completedGames}",
                                    icon = Icons.Default.Timeline,
                                    accentColor = Color(0xFF38BDF8),
                                    modifier = Modifier.weight(1f),
                                    testTag = "stat_matches_played"
                                )
                                ProfileStatCard(
                                    title = "Matches Won",
                                    value = "${stats.wins}",
                                    icon = Icons.Default.EmojiEvents,
                                    accentColor = Color(0xFF22C55E),
                                    modifier = Modifier.weight(1f),
                                    testTag = "stat_matches_won"
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 2: Matches Lost & Win Rate
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileStatCard(
                                    title = "Matches Lost",
                                    value = "${stats.losses}",
                                    icon = Icons.Default.Close,
                                    accentColor = Color(0xFFF87171),
                                    modifier = Modifier.weight(1f),
                                    testTag = "stat_matches_lost"
                                )
                                ProfileStatCard(
                                    title = "Win Rate",
                                    value = stats.winPercentageFormatted,
                                    icon = Icons.Default.MilitaryTech,
                                    accentColor = Color(0xFFFFD700),
                                    modifier = Modifier.weight(1f),
                                    testTag = "stat_win_rate"
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 3: Total Captures & Tokens Safely Home
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileStatCard(
                                    title = "Tokens Captured",
                                    value = "${stats.captures}",
                                    icon = Icons.Default.SportsKabaddi,
                                    accentColor = Color(0xFFFB923C),
                                    modifier = Modifier.weight(1f),
                                    testTag = "stat_tokens_captured"
                                )
                                ProfileStatCard(
                                    title = "Safely Home",
                                    value = "${stats.tokensHome}",
                                    icon = Icons.Default.Home,
                                    accentColor = Color(0xFF34D399),
                                    modifier = Modifier.weight(1f),
                                    testTag = "stat_tokens_home"
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 4: Best Winning Streak
                            ProfileStatCard(
                                title = "Best Winning Streak",
                                value = "${stats.bestStreak} wins",
                                subtitle = if (stats.currentStreak > 0) "Current streak: ${stats.currentStreak} 🔥" else "Current streak: 0",
                                icon = Icons.Default.Whatshot,
                                accentColor = Color(0xFFE11D48),
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "stat_winning_streak"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Save Profile Button
                Button(
                    onClick = {
                        val trimmedName = currentName.trim().ifEmpty { "${selectedColor.title} Player" }
                        onSaveProfile(selectedColor, trimmedName, currentAvatar)
                        showSavedToast = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showSavedToast) Color(0xFF15803D) else Color(0xFF16A34A)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFFFD700))
                ) {
                    Icon(
                        imageVector = if (showSavedToast) Icons.Default.Check else Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showSavedToast) "PROFILE SAVED!" else "SAVE PROFILE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    testTag: String
) {
    Surface(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF160A05),
        border = BorderStroke(1.dp, Color(0xFF451A03))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
