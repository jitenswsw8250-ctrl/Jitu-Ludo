package com.example.ui.screens

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Player
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val speed: Float,
    val swaySpeed: Float,
    val swayAmp: Float,
    val rotationSpeed: Float,
    val size: Float,
    val color: Color,
    val shape: Int // 0: rectangle ribbon, 1: circle, 2: star
)

@Composable
fun VictoryDialog(
    podium: List<Player>,
    allPlayers: List<Player>,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    onOpenStats: (() -> Unit)? = null
) {
    val winner = podium.firstOrNull() ?: allPlayers.firstOrNull()
    val confettiAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        confettiAnim.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "victoryGlow")
    val trophyBob by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyBob"
    )

    val auraRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auraRotate"
    )

    // Pre-calculate randomized confetti particles for stable smooth animation
    val particles = remember {
        val colors = listOf(
            Color(0xFFE53935), // Red
            Color(0xFF43A047), // Green
            Color(0xFFFBC02D), // Yellow
            Color(0xFF1E88E5), // Blue
            Color(0xFFF59E0B), // Gold
            Color(0xFFA855F7), // Purple
            Color(0xFFEC4899)  // Pink
        )
        val rng = Random(12345)
        List(60) {
            ConfettiParticle(
                initialX = rng.nextFloat(),
                initialY = rng.nextFloat(),
                speed = 0.6f + rng.nextFloat() * 0.9f,
                swaySpeed = 2f + rng.nextFloat() * 4f,
                swayAmp = 10f + rng.nextFloat() * 25f,
                rotationSpeed = 3f + rng.nextFloat() * 6f,
                size = 6f + rng.nextFloat() * 8f,
                color = colors[it % colors.size],
                shape = rng.nextInt(3)
            )
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF0F172A))
                .border(2.dp, Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFF78350F))), RoundedCornerShape(26.dp))
                .testTag("victory_dialog")
        ) {
            // Animated Fluttering Confetti Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val progress = confettiAnim.value
                for (p in particles) {
                    val y = (p.initialY * size.height + progress * size.height * p.speed) % size.height
                    val sway = sin(progress * p.swaySpeed * 2.0 * Math.PI + p.initialX * 10f).toFloat() * p.swayAmp
                    val x = (p.initialX * size.width + sway + size.width) % size.width
                    val flipScale = cos(progress * p.rotationSpeed * 2.0 * Math.PI).toFloat()

                    when (p.shape) {
                        0 -> {
                            // Fluttering Ribbon
                            drawRoundRect(
                                color = p.color,
                                topLeft = Offset(x - p.size, y - (p.size * 0.45f)),
                                size = Size(p.size * 2f * flipScale.coerceIn(-1f, 1f), p.size * 0.9f)
                            )
                        }
                        1 -> {
                            // Confetti Circle
                            drawCircle(color = p.color, radius = p.size * 0.45f, center = Offset(x, y))
                        }
                        else -> {
                            // Sparkle Star / Diamond
                            val path = Path().apply {
                                moveTo(x, y - p.size * 0.7f)
                                lineTo(x + p.size * 0.5f * flipScale, y)
                                lineTo(x, y + p.size * 0.7f)
                                lineTo(x - p.size * 0.5f * flipScale, y)
                                close()
                            }
                            drawPath(path, color = p.color, style = Fill)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Radiant Sunburst Aura & Floating Trophy
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Rotating sunburst rays
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = auraRotate }
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val rayCount = 12
                        val rayLength = size.width / 2f
                        for (i in 0 until rayCount) {
                            val angle = (i * 2.0 * Math.PI / rayCount).toFloat()
                            val path = Path().apply {
                                moveTo(center.x, center.y)
                                val a1 = angle - 0.12f
                                val a2 = angle + 0.12f
                                lineTo(center.x + cos(a1) * rayLength, center.y + sin(a1) * rayLength)
                                lineTo(center.x + cos(a2) * rayLength, center.y + sin(a2) * rayLength)
                                close()
                            }
                            drawPath(path, color = Color(0x33F59E0B))
                        }
                    }

                    // Floating Trophy with glowing backdrop
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier
                            .size(76.dp)
                            .graphicsLayer { translationY = trophyBob }
                            .shadow(16.dp, CircleShape, spotColor = Color(0xFFF59E0B)),
                        border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFF59E0B))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Trophy",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "🏆 VICTORY! 🏆",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFBBF24),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Winner Highlight Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = (winner?.color?.primaryColor ?: Color(0xFFF59E0B)).copy(alpha = 0.22f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        winner?.color?.lightColor ?: Color(0xFFFBBF24)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👑", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${winner?.name ?: "Player"} Takes The Crown!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "All 4 tokens reached home safely!",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ranked Podium List
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allPlayers.sortedBy { it.rank ?: 99 }.forEach { player ->
                            val isWinner = player.rank == 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isWinner) player.color.primaryColor.copy(alpha = 0.35f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (player.rank) {
                                            1 -> Color(0xFFF59E0B) // Gold
                                            2 -> Color(0xFF94A3B8) // Silver
                                            3 -> Color(0xFFB45309) // Bronze
                                            else -> Color(0xFF475569)
                                        }
                                    ) {
                                        Text(
                                            text = when (player.rank) {
                                                1 -> " 1st 🥇 "
                                                2 -> " 2nd 🥈 "
                                                3 -> " 3rd 🥉 "
                                                else -> " - "
                                            },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(player.color.primaryColor)
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = player.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "${player.finishedTokensCount}/4 Home",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (player.finishedTokensCount == 4) Color(0xFF4ADE80) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Home, Stats, and Play Again
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onHome,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("victory_home_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF475569))
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Home", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (onOpenStats != null) {
                        OutlinedButton(
                            onClick = onOpenStats,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("victory_stats_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Stats", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("victory_replay_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Play Again", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

