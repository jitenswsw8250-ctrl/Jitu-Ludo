package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Premium Offline How to Play & Ludo Rules Tutorial Dialog.
 *
 * Provides step-by-step illustrated guides for:
 * 1. Rolling a 6 & Releasing Tokens
 * 2. Token Movement & Track Circuit
 * 3. Capturing Opponents & Bonus Rolls
 * 4. Safe Squares (Stars & Starting Cells)
 * 5. Home Lane & Exact Finish
 * 6. Game Modes: Pass & Play vs. AI Computer Bots
 */
@Composable
fun RulesDialog(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Step-by-Step", "Game Modes", "Pro Tips")

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF140804))
                .border(
                    2.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFF78350F))
                    ),
                    RoundedCornerShape(24.dp)
                )
                .testTag("rules_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "HOW TO PLAY",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Complete Rules & Strategy Guide",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_rules_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF261208))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val tabModifier = if (isSelected) {
                            Modifier.background(Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF78350F))))
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .then(tabModifier)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp)
                                .testTag("tutorial_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) Color(0xFFFFFBEB) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content Area (Scrollable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "TutorialTabs"
                    ) { tabIndex ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (tabIndex) {
                                0 -> StepByStepTutorialContent()
                                1 -> GameModesTutorialContent()
                                2 -> ProTipsTutorialContent()
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Got it button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("rules_got_it_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = "READY TO PLAY!",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StepByStepTutorialContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Step 1: Getting Out of Base on 6
        TutorialStepCard(
            stepNumber = "1",
            icon = Icons.Default.Casino,
            iconTint = Color(0xFFDC2626),
            title = "Rolling a Six (6) to Enter the Board",
            description = "All 4 tokens start inside your colored base yard. You MUST roll a 6 on the dice to release a token onto your starting square.\n\nRolling a 6 also awards an immediate BONUS turn! (Note: rolling three consecutive sixes ends your turn to prevent unfair streaks)."
        ) {
            DiceSixMiniDiagram()
        }

        // Step 2: Token Movement
        TutorialStepCard(
            stepNumber = "2",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconTint = Color(0xFF38BDF8),
            title = "Clockwise Movement",
            description = "Tokens advance clockwise around the 52-square outer circuit according to the rolled number (1 to 6). Tap on any glowing token to move it forward."
        ) {
            TrackMovementMiniDiagram()
        }

        // Step 3: Capturing Opponents
        TutorialStepCard(
            stepNumber = "3",
            icon = Icons.Default.FlashOn,
            iconTint = Color(0xFFEF4444),
            title = "Capturing Opponent Tokens",
            description = "If your token lands on a square occupied by an opponent's token, the opponent's token is CAPTURED and sent all the way back to their base yard!\n\nCapturing an opponent grants you an EXTRA roll bonus!"
        ) {
            CaptureMiniDiagram()
        }

        // Step 4: Safe Squares
        TutorialStepCard(
            stepNumber = "4",
            icon = Icons.Default.Security,
            iconTint = Color(0xFFFBBF24),
            title = "Safe Squares (Stars & Start Cells)",
            description = "Squares marked with Stars and colored Player Starting squares are 100% SAFE. Tokens resting on safe squares cannot be captured by any player under any circumstances."
        ) {
            SafeSquareMiniDiagram()
        }

        // Step 5: Home Lane & Exact Finish
        TutorialStepCard(
            stepNumber = "5",
            icon = Icons.Default.EmojiEvents,
            iconTint = Color(0xFFFDE047),
            title = "Home Path & Exact Finish",
            description = "After completing a full lap of the board, tokens turn into their color's private Home Lane. To reach the center victory triangle, you must roll the EXACT number required."
        ) {
            HomeLaneMiniDiagram()
        }
    }
}

@Composable
private fun GameModesTutorialContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Mode 1: Pass & Play
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261208)),
            border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pass & Play (Local Multiplayer)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Play offline with friends & family",
                            fontSize = 11.sp,
                            color = Color(0xFF4ADE80)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Support for 2, 3, or 4 human players on a single device.\n• Custom player names and avatars for each player.\n• Pass the phone to each player when their colored dice lights up.\n• Send fun in-game animated emoji reactions during gameplay!",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // Mode 2: Vs Computer
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261208)),
            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Vs Computer (Solo Offline)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Choose from 3 smart AI bot difficulties",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• EASY (Casual): Relaxed bot play, picks among legal moves with simple choices.\n• MEDIUM (Balanced): Smart bot that seizes captures, enters safe cells, and advances lead tokens.\n• HARD (Tactical): Competitive bot using advanced strategy: hunts vulnerabilities, hides in safe cells, and optimizes home lane entry!",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // Auto Save Notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0E07)),
            border = BorderStroke(1.dp, Color(0xFFB45309).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFFFDE047),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Automatic Progress Saving: Matches are saved locally after every action. If interrupted, simply tap 'Resume Game' on the home screen!",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFFFDE047)
                )
            }
        }
    }
}

@Composable
private fun ProTipsTutorialContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProTipItem(
            title = "1. Spread Your Tokens Early",
            tip = "Don't just advance one single token. Bringing out 2 or 3 tokens gives you multiple move options when low numbers are rolled, and lets you set up traps for opponents!"
        )
        ProTipItem(
            title = "2. Camp on Safe Star Squares",
            tip = "When approaching dangerous opponent territory, park your token on a Star square. Opponents can roll past you without capturing your token."
        )
        ProTipItem(
            title = "3. Chain Extra Turns",
            tip = "Rolling a 6 gives a bonus turn. Capturing an opponent gives a bonus turn. Guiding a token into Home gives a bonus turn. Chain these together to dominate the board!"
        )
        ProTipItem(
            title = "4. Watch the 3-Six Rule",
            tip = "If you roll two consecutive sixes, your third roll will be limited to 1-5. Plan ahead knowing your third turn cannot be another 6!"
        )
        ProTipItem(
            title = "5. Protect Tokens Near the Finish",
            tip = "Tokens that have almost finished a lap are the most valuable. Prioritize moving them into your colored home lane where opponents cannot follow!"
        )
    }
}

@Composable
private fun ProTipItem(title: String, tip: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF261208)),
        border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFFFDE047)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tip,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
private fun TutorialStepCard(
    stepNumber: String,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    diagramContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF261208)),
        border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = iconTint
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(8.dp))

            diagramContent()
        }
    }
}

// -----------------------------------------------------------------------------
// Visual Diagrams for Rules & Tutorial
// -----------------------------------------------------------------------------

@Composable
private fun DiceSixMiniDiagram() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF180A04))
            .border(1.dp, Color(0xFFB45309).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Base Yard Token
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF991B1B))
                    .border(1.dp, Color(0xFFFCA5A5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )
            }
            Text("In Base", fontSize = 9.sp, color = Color(0xFFCBD5E1))
        }

        Text("➕", fontSize = 14.sp)

        // Dice 6
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFF59E0B)))
                    )
                    .border(1.5.dp, Color(0xFFFDE047), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                // 6 pips
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF78350F)))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF78350F)))
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF78350F)))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF78350F)))
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF78350F)))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF78350F)))
                    }
                }
            }
            Text("Roll 6", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
        }

        Text("➔", fontSize = 16.sp, color = Color(0xFF4ADE80))

        // Released onto Start Cell
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFDC2626))
                    .border(1.5.dp, Color(0xFFFDE047), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFFBEB))
                )
            }
            Text("Starts Track!", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
        }
    }
}

@Composable
private fun TrackMovementMiniDiagram() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF180A04))
            .border(1.dp, Color(0xFFB45309).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("Start", "+1", "+2", "+3", "+4").forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (index == 0) Color(0xFFDC2626) else Color(0xFF2E170E))
                        .border(
                            1.dp,
                            if (index == 3) Color(0xFF38BDF8) else Color(0xFF78350F),
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index == 3) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .border(1.dp, Color(0xFFFCA5A5), CircleShape)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Text(text = label, fontSize = 8.sp, color = Color(0xFFCBD5E1))
            }
            if (index < 4) {
                Text("➔", fontSize = 11.sp, color = Color(0xFFF59E0B))
            }
        }
    }
}

@Composable
private fun CaptureMiniDiagram() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF180A04))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Red Token attacking
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626))
                    .border(1.dp, Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("⚔️", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16A34A))
                    .border(1.dp, Color.White, CircleShape)
            )
        }

        Text("=", fontSize = 14.sp, color = Color.White)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "💥 CAPTURED!",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = Color(0xFFEF4444)
            )
            Text(
                text = "+1 Bonus Turn!",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color(0xFFFDE047)
            )
        }
    }
}

@Composable
private fun SafeSquareMiniDiagram() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF180A04))
            .border(1.dp, Color(0xFFFDE047).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Star square
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF332014))
                    .border(1.dp, Color(0xFFFDE047), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFDE047),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("STAR CELL", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFFDE047))
                Text("100% Protected", fontSize = 9.sp, color = Color(0xFFCBD5E1))
            }
        }

        // Shield
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF4ADE80),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "No Captures Allowed!",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4ADE80)
            )
        }
    }
}

@Composable
private fun HomeLaneMiniDiagram() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF180A04))
            .border(1.dp, Color(0xFFFDE047).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // 3 colored lane cells
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("1", "2", "3").forEach {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFDC2626).copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = it, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Text("➔", fontSize = 14.sp, color = Color(0xFFFDE047))

        // Center Home Trophy
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFDE047),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text("HOME!", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFFFDE047))
                Text("Exact roll wins", fontSize = 9.sp, color = Color(0xFFCBD5E1))
            }
        }
    }
}
