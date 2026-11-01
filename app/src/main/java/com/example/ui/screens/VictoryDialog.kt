package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.GameMode
import com.example.model.Player
import com.example.model.PlayerTeam
import com.example.model.TokenPosition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val speedY: Float,
    val speedX: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float,
    val isCircle: Boolean
)

@Composable
fun VictoryDialog(
    podium: List<Player>,
    allPlayers: List<Player>,
    matchDurationSeconds: Long = 0L,
    gameMode: GameMode = GameMode.LOCAL_MULTIPLAYER,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val winner = podium.firstOrNull() ?: allPlayers.firstOrNull()
    val minutes = matchDurationSeconds / 60
    val seconds = matchDurationSeconds % 60
    val durationText = "${minutes}m ${seconds}s"

    val infiniteTransition = rememberInfiniteTransition(label = "victoryGlow")
    val trophyBob by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyBob"
    )

    val sunburstRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunburstRotate"
    )

    val starPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starPulse"
    )

    Dialog(
        onDismissRequest = { /* Modal must be closed via explicit buttons */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("victory_dialog")
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140804)),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFFFFBEB),
                            Color(0xFFFDE047),
                            Color(0xFFF59E0B),
                            Color(0xFFB45309),
                            Color(0xFFFDE047)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Confetti celebration particle background
                    CelebrationConfettiCanvas(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(26.dp))
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xF02B1309),
                                        Color(0xFA1E0C05),
                                        Color(0xFD120603)
                                    )
                                )
                            )
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Golden Trophy with Sunburst Ray Effect
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Rotating Sunburst Rays
                            Canvas(modifier = Modifier.size(110.dp)) {
                                rotate(sunburstRotate) {
                                    val rayCount = 12
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.width / 2f
                                    for (i in 0 until rayCount) {
                                        val angle = (i * 2.0 * PI / rayCount).toFloat()
                                        val endX = center.x + radius * cos(angle)
                                        val endY = center.y + radius * sin(angle)
                                        drawLine(
                                            brush = Brush.radialGradient(
                                                listOf(Color(0xFFFBBF24).copy(alpha = 0.55f), Color.Transparent),
                                                center = center,
                                                radius = radius
                                            ),
                                            start = center,
                                            end = Offset(endX, endY),
                                            strokeWidth = 14f
                                        )
                                    }
                                }
                            }

                            // Golden circular backing
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFFFFFBEB),
                                                Color(0xFFFDE047),
                                                Color(0xFFD97706),
                                                Color(0xFF78350F)
                                            )
                                        )
                                    )
                                    .border(2.dp, Color(0xFFFFFBEB), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Victory Trophy",
                                    tint = Color(0xFFFFFBEB),
                                    modifier = Modifier
                                        .size(52.dp)
                                        .graphicsLayer { translationY = trophyBob }
                                )
                            }

                            // Sparkling mini stars
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFFBEB),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .size(18.dp)
                                    .graphicsLayer {
                                        scaleX = starPulse
                                        scaleY = starPulse
                                    }
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFDE047),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = starPulse
                                        scaleY = starPulse
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val isTeamMode = gameMode == GameMode.TEAM_2V2
                        val winningTeam = winner?.team ?: PlayerTeam.TEAM_A
                        val runnerUpTeam = if (winningTeam == PlayerTeam.TEAM_A) PlayerTeam.TEAM_B else PlayerTeam.TEAM_A
                        val winningTeammates = allPlayers.filter { it.team == winningTeam }
                        val runnerUpTeammates = allPlayers.filter { it.team == runnerUpTeam }
                        val winningTeamTokensHome = winningTeammates.sumOf { p -> p.tokens.count { it.position is TokenPosition.Finished } }
                        val runnerUpTokensHome = runnerUpTeammates.sumOf { p -> p.tokens.count { it.position is TokenPosition.Finished } }
                        val winningTeamCaptures = winningTeammates.sumOf { it.captures }
                        val runnerUpCaptures = runnerUpTeammates.sumOf { it.captures }

                        Text(
                            text = if (isTeamMode) "TEAM VICTORY! 🏆" else "VICTORY!",
                            fontSize = if (isTeamMode) 26.sp else 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color(0xFFFDE047)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (isTeamMode) {
                            Text(
                                text = "${winningTeam.title} Wins The Match!",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val winnerName = winner?.name ?: "Player"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = winner?.avatar?.emoji ?: "👑",
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$winnerName Wins The Match!",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Match Duration Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF26130C))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "⏱️", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Game Duration: $durationText",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFDE68A),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isTeamMode) {
                            // 10. Special Team Victory Screen: Showing both winning teammates
                            Text(
                                text = "WINNING TEAMMATES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE047),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Both winning teammates presented side-by-side
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                winningTeammates.forEach { teammate ->
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = teammate.color.darkColor.copy(alpha = 0.55f)),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, teammate.color.primaryColor)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(text = teammate.avatar.emoji, fontSize = 28.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = teammate.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = teammate.color.primaryColor,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = teammate.color.title,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val tHome = teammate.tokens.count { it.position is TokenPosition.Finished }
                                            Text(
                                                text = "$tHome/4 Home 🏠",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF86EFAC)
                                            )
                                            Text(
                                                text = "⚔️ ${teammate.captures} Captures",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFFBBF24)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Team Rankings Summary
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0E07).copy(alpha = 0.9f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1st Place Team
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF38150A))
                                            .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🥇", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${winningTeam.title} (Champions)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFFFDE047)
                                            )
                                            Text(
                                                text = winningTeammates.joinToString(" & ") { it.name },
                                                fontSize = 11.sp,
                                                color = Color(0xFFE2E8F0)
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$winningTeamTokensHome/8 Home",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF4ADE80)
                                            )
                                            Text(
                                                text = "⚔️ $winningTeamCaptures Captures",
                                                fontSize = 10.sp,
                                                color = Color(0xFFFBBF24)
                                            )
                                        }
                                    }

                                    // 2nd Place Team
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF140804).copy(alpha = 0.7f))
                                            .border(1.dp, Color(0xFF451A03), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🥈", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${runnerUpTeam.title} (Runners-Up)",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = Color(0xFFCBD5E1)
                                            )
                                            Text(
                                                text = runnerUpTeammates.joinToString(" & ") { it.name },
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$runnerUpTokensHome/8 Home",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                            Text(
                                                text = "⚔️ $runnerUpCaptures Captures",
                                                fontSize = 10.sp,
                                                color = Color(0xFFCBD5E1)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Podium Rankings Card for Free-For-All
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0E07).copy(alpha = 0.9f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val displayPlayers = (podium + (allPlayers - podium.toSet())).distinctBy { it.color }

                                    displayPlayers.forEachIndexed { index, player ->
                                        val rankLabel = when (index) {
                                            0 -> "1st"
                                            1 -> "2nd"
                                            2 -> "3rd"
                                            else -> "4th"
                                        }
                                        val rankColor = when (index) {
                                            0 -> Color(0xFFFBBF24)
                                            1 -> Color(0xFFCBD5E1)
                                            2 -> Color(0xFFCD7F32)
                                            else -> Color(0xFF94A3B8)
                                        }
                                        val homeCount = player.tokens.count { it.position is TokenPosition.Finished }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (index == 0) player.color.darkColor.copy(alpha = 0.45f) else Color(0xFF140804).copy(alpha = 0.7f))
                                            .border(
                                                1.dp,
                                                if (index == 0) player.color.primaryColor else Color(0xFF451A03),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(rankColor.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = rankLabel,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = rankColor
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Player Avatar Emoji
                                        Text(
                                            text = player.avatar.emoji,
                                            fontSize = 16.sp
                                        )

                                        Spacer(modifier = Modifier.width(5.dp))

                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(player.color.primaryColor)
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = player.name,
                                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Total captures
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text(text = "⚔️", fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "${player.captures}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF1F5F9)
                                            )
                                        }

                                        Text(
                                            text = "$homeCount/4 Home",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (homeCount == 4) Color(0xFF4ADE80) else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons: Play Again (Primary Green), Stats, and Home
                        Button(
                            onClick = onPlayAgain,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("victory_replay_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF86EFAC))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PLAY AGAIN",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenStats,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("victory_stats_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "STATS",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = onHome,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("victory_home_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "HOME",
                                    color = Color(0xFFFDE047),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationConfettiCanvas(modifier: Modifier = Modifier) {
    val confettiColors = listOf(
        Color(0xFFFDE047), // Gold
        Color(0xFFEF4444), // Ruby Red
        Color(0xFF10B981), // Emerald Green
        Color(0xFF3B82F6), // Royal Blue
        Color(0xFFA855F7), // Purple
        Color(0xFFFFFFFF), // White
        Color(0xFFFB923C)  // Orange
    )

    val particles = remember {
        List(50) {
            ConfettiParticle(
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat() * -1f,
                speedY = 0.003f + Random.nextFloat() * 0.005f,
                speedX = (Random.nextFloat() - 0.5f) * 0.002f,
                size = 6f + Random.nextFloat() * 8f,
                color = confettiColors[Random.nextInt(confettiColors.size)],
                rotationSpeed = (Random.nextFloat() - 0.5f) * 8f,
                isCircle = Random.nextBoolean()
            )
        }
    }

    // Golden coins
    val coins = remember {
        List(14) {
            ConfettiParticle(
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat() * -1.2f,
                speedY = 0.004f + Random.nextFloat() * 0.003f,
                speedX = (Random.nextFloat() - 0.5f) * 0.0015f,
                size = 14f + Random.nextFloat() * 8f,
                color = Color(0xFFFFD700),
                rotationSpeed = (Random.nextFloat() - 0.5f) * 6f,
                isCircle = true
            )
        }
    }

    val elapsedFrames by produceState(initialValue = 0L) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                value = frameTimeNanos / 16_000_000L
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Fireworks Bursts
        val fireworkCycle = (elapsedFrames % 180) / 180f
        val burstCount = 12
        val burstCenters = listOf(
            Offset(w * 0.22f, h * 0.20f),
            Offset(w * 0.78f, h * 0.25f),
            Offset(w * 0.50f, h * 0.15f)
        )
        for ((idx, burstCenter) in burstCenters.withIndex()) {
            val offsetCycle = ((fireworkCycle + idx * 0.33f) % 1.0f)
            val burstRadius = offsetCycle * (cellSizeFallback() * 2.2f)
            val burstAlpha = (1f - offsetCycle).coerceIn(0f, 1f)
            val burstColor = if (idx == 0) Color(0xFFFFD700) else if (idx == 1) Color(0xFF38BDF8) else Color(0xFFF43F5E)

            for (i in 0 until burstCount) {
                val angle = (i * 2.0 * PI / burstCount).toFloat()
                val sx = burstCenter.x + burstRadius * cos(angle)
                val sy = burstCenter.y + burstRadius * sin(angle)
                drawCircle(
                    color = burstColor.copy(alpha = burstAlpha),
                    radius = 3.5f * (1f - offsetCycle * 0.5f),
                    center = Offset(sx, sy)
                )
            }
        }

        // 2. Confetti Particles
        for (p in particles) {
            val totalY = (p.initialY + (elapsedFrames * p.speedY)) % 1.2f
            val curY = totalY * h
            val curX = ((p.initialX + (elapsedFrames * p.speedX) + sin(elapsedFrames * 0.05f + p.initialX * 10f) * 0.03f) % 1.0f) * w
            val normalizedX = if (curX < 0) curX + w else curX

            if (curY >= 0 && curY <= h) {
                val rot = (elapsedFrames * p.rotationSpeed) % 360f
                rotate(rot, pivot = Offset(normalizedX, curY)) {
                    if (p.isCircle) {
                        drawCircle(
                            color = p.color,
                            radius = p.size / 2f,
                            center = Offset(normalizedX, curY)
                        )
                    } else {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(normalizedX - p.size / 2f, curY - p.size / 3f),
                            size = Size(p.size, p.size * 0.7f)
                        )
                    }
                }
            }
        }

        // 3. Falling Golden Coins
        for (coin in coins) {
            val totalY = (coin.initialY + (elapsedFrames * coin.speedY)) % 1.3f
            val curY = totalY * h
            val curX = ((coin.initialX + (elapsedFrames * coin.speedX) + sin(elapsedFrames * 0.04f + coin.initialX * 8f) * 0.02f) % 1.0f) * w
            val normalizedX = if (curX < 0) curX + w else curX

            if (curY >= 0 && curY <= h) {
                val rot = (elapsedFrames * coin.rotationSpeed) % 360f
                rotate(rot, pivot = Offset(normalizedX, curY)) {
                    // Coin outer rim
                    drawCircle(
                        color = Color(0xFFFDE047),
                        radius = coin.size / 2f,
                        center = Offset(normalizedX, curY)
                    )
                    // Coin inner face
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = coin.size * 0.40f,
                        center = Offset(normalizedX, curY)
                    )
                    // Coin center specular glint
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = coin.size * 0.16f,
                        center = Offset(normalizedX - coin.size * 0.12f, curY - coin.size * 0.12f)
                    )
                }
            }
        }
    }
}

private fun cellSizeFallback(): Float = 36f
