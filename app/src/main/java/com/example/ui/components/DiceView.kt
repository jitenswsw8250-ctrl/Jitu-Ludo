package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DiceState
import com.example.model.GameSkin
import com.example.model.Player
import com.example.model.TurnPhase

/**
 * Compact Player-Specific Dice Control:
 * Positioned adjacent to each active player's colored home quadrant:
 * - Red at Bottom-Left
 * - Green at Top-Left
 * - Yellow at Top-Right
 * - Blue at Bottom-Right
 *
 * Only the active player can roll the dice. Highlights when it is their turn.
 */
@Composable
fun PlayerDiceControl(
    player: Player,
    isCurrentTurn: Boolean,
    turnPhase: TurnPhase,
    diceState: DiceState,
    isGameOver: Boolean,
    onRollDice: () -> Unit,
    modifier: Modifier = Modifier,
    alignRight: Boolean = false,
    skin: GameSkin = GameSkin.CLASSIC
) {
    val canRoll = isCurrentTurn && !isGameOver && (turnPhase == TurnPhase.WAITING_FOR_ROLL) && !player.isBot && !diceState.isRolling
    val isBotThinking = isCurrentTurn && !isGameOver && player.isBot

    val infiniteTransition = rememberInfiniteTransition(label = "handBounce_${player.color.name}")
    val animatedHandOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handBounceAnim"
    )
    val handOffsetX = if (canRoll) animatedHandOffset else 0f

    Card(
        modifier = modifier
            .testTag("player_dice_control_${player.color.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTurn) {
                Color(0xFF2E1710)
            } else {
                Color(0xFF1C0D07).copy(alpha = 0.65f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCurrentTurn) 2.dp else 1.dp,
            color = if (isCurrentTurn) Color(0xFFFBBF24) else Color(0xFF452213).copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentTurn) 6.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (!alignRight) {
                // Left aligned: [Kills/Deaths ⚔️💀] -> [Dice] -> [👈 Hand]
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚔️", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${player.captures}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💀", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${player.losses}",
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                DiceFace(
                    value = if (isCurrentTurn) diceState.value else 1,
                    isRolling = if (isCurrentTurn) diceState.isRolling else false,
                    accentColor = player.color.primaryColor,
                    enabled = canRoll,
                    onClick = onRollDice,
                    diceSizeDp = 46.dp,
                    skin = skin,
                    modifier = if (canRoll) Modifier.testTag("roll_dice_button") else Modifier
                )

                if (canRoll) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier.graphicsLayer { translationX = handOffsetX },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👈", fontSize = 22.sp)
                    }
                } else if (isBotThinking) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "🤖", fontSize = 18.sp)
                }
            } else {
                // Right aligned: [👉 Hand] <- [Dice] <- [Kills/Deaths ⚔️💀]
                if (canRoll) {
                    Box(
                        modifier = Modifier.graphicsLayer { translationX = -handOffsetX },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👉", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else if (isBotThinking) {
                    Text(text = "🤖", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                }

                DiceFace(
                    value = if (isCurrentTurn) diceState.value else 1,
                    isRolling = if (isCurrentTurn) diceState.isRolling else false,
                    accentColor = player.color.primaryColor,
                    enabled = canRoll,
                    onClick = onRollDice,
                    diceSizeDp = 46.dp,
                    skin = skin,
                    modifier = if (canRoll) Modifier.testTag("roll_dice_button") else Modifier
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${player.captures}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "⚔️", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${player.losses}",
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "💀", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DiceControlSection(
    player: Player?,
    diceState: DiceState,
    turnPhase: TurnPhase,
    isGameOver: Boolean,
    onRollDice: () -> Unit,
    modifier: Modifier = Modifier,
    skin: GameSkin = GameSkin.CLASSIC
) {
    val canRoll = !isGameOver && (turnPhase == TurnPhase.WAITING_FOR_ROLL) && (player?.isBot == false) && !diceState.isRolling

    val infiniteTransition = rememberInfiniteTransition(label = "handBounce")
    val handOffsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handBounceAnim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("dice_section_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF26130C).copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = Color(0xFF4A2818)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚔️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${player?.captures ?: 0}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💀", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${player?.losses ?: 0}",
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                DiceFace(
                    value = diceState.value,
                    isRolling = diceState.isRolling,
                    accentColor = player?.color?.primaryColor ?: Color(0xFFE53935),
                    enabled = canRoll,
                    onClick = onRollDice,
                    skin = skin
                )

                if (canRoll) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .graphicsLayer { translationX = handOffsetX }
                            .padding(start = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👈",
                            fontSize = 26.sp,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (diceState.consecutiveSixes > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFDC2626),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "SIX x${diceState.consecutiveSixes} 🔥",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = player?.name ?: "Player",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )

                Text(
                    text = when {
                        isGameOver -> "Game Over"
                        diceState.isRolling -> "Rolling..."
                        turnPhase == TurnPhase.WAITING_FOR_ROLL -> if (player?.isBot == true) "Bot thinking..." else "Your turn: Roll!"
                        turnPhase == TurnPhase.SELECTING_TOKEN -> if (player?.isBot == true) "Bot moving..." else "Select token"
                        else -> "Playing..."
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFBBF24)
                )

                if (canRoll) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onRollDice,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A)
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("roll_dice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Roll",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "ROLL", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DiceFace(
    value: Int,
    isRolling: Boolean,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diceSizeDp: Dp = 54.dp,
    skin: GameSkin = GameSkin.CLASSIC
) {
    val rotationZ = remember { Animatable(0f) }
    val rotationX = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "sixGlowPulse")
    val sixGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sixGlow"
    )

    LaunchedEffect(isRolling) {
        if (isRolling) {
            rotationZ.animateTo(
                targetValue = rotationZ.value + 360f,
                animationSpec = tween(durationMillis = 320, easing = LinearEasing)
            )
            rotationX.animateTo(25f, tween(160, easing = FastOutSlowInEasing))
            rotationX.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
            scale.animateTo(1.15f, tween(160))
            scale.animateTo(1.0f, tween(160))
        }
    }

    val isSixRolled = value == 6 && !isRolling

    Box(
        modifier = modifier
            .size(diceSizeDp)
            .scale(scale.value)
            .graphicsLayer {
                this.rotationZ = rotationZ.value
                this.rotationX = rotationX.value
                this.cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled && !isRolling,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .testTag("dice_face_button"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diceSizeDp)) {
            val dSize = size.width
            val pad = dSize * 0.06f
            val corner = dSize * 0.24f

            // 0. Golden Glow Halo when a six is rolled!
            if (isSixRolled) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = sixGlowAlpha),
                            Color(0xFFF59E0B).copy(alpha = sixGlowAlpha * 0.6f),
                            Color.Transparent
                        ),
                        center = Offset(dSize / 2f, dSize / 2f),
                        radius = dSize * 0.72f
                    ),
                    topLeft = Offset(-2f, -2f),
                    size = Size(dSize + 4f, dSize + 4f),
                    cornerRadius = CornerRadius(corner * 1.1f, corner * 1.1f)
                )
            }

            // 1. Outer Dark Bezel / Shadow
            drawRoundRect(
                color = Color(0x66000000),
                topLeft = Offset(2f, 4f),
                size = Size(dSize - 4f, dSize - 4f),
                cornerRadius = CornerRadius(corner, corner)
            )

            // 2. Outer Bezel Border (Skin customized)
            val bezelColors = when (skin) {
                GameSkin.CLASSIC -> listOf(Color(0xFF1E3A8A), Color(0xFF0F172A), Color(0xFF1E3A8A))
                GameSkin.RUBY -> listOf(Color(0xFF9F1239), Color(0xFF4C0519), Color(0xFF9F1239))
                GameSkin.EMERALD -> listOf(Color(0xFF065F46), Color(0xFF022C22), Color(0xFF065F46))
                GameSkin.SAPPHIRE -> listOf(Color(0xFF1E40AF), Color(0xFF0F172A), Color(0xFF1E40AF))
                GameSkin.GOLD -> listOf(Color(0xFFB45309), Color(0xFF451A03), Color(0xFFB45309))
                GameSkin.SILVER -> listOf(Color(0xFF475569), Color(0xFF0F172A), Color(0xFF475569))
            }
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = bezelColors,
                    start = Offset(0f, 0f),
                    end = Offset(dSize, dSize)
                ),
                topLeft = Offset(pad * 0.5f, pad * 0.5f),
                size = Size(dSize - pad, dSize - pad),
                cornerRadius = CornerRadius(corner, corner)
            )

            // 3. Die Face Base with 3D gradient
            val innerPad = pad * 1.8f
            val faceColors = when (skin) {
                GameSkin.CLASSIC -> listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706))
                GameSkin.RUBY -> listOf(Color(0xFFFFF1F2), Color(0xFFFDA4AF), Color(0xFFF43F5E), Color(0xFFBE123C))
                GameSkin.EMERALD -> listOf(Color(0xFFF0FDF4), Color(0xFFA7F3D0), Color(0xFF10B981), Color(0xFF047857))
                GameSkin.SAPPHIRE -> listOf(Color(0xFFEFF6FF), Color(0xFFBAE6FD), Color(0xFF38BDF8), Color(0xFF0284C7))
                GameSkin.GOLD -> listOf(Color(0xFFFEFCE8), Color(0xFFFEF08A), Color(0xFFFACC15), Color(0xFFCA8A04))
                GameSkin.SILVER -> listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF64748B))
            }

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = faceColors
                ),
                topLeft = Offset(innerPad, innerPad),
                size = Size(dSize - innerPad * 2, dSize - innerPad * 2),
                cornerRadius = CornerRadius(corner * 0.8f, corner * 0.8f)
            )

            // 4. Inner Rim Bevel Stroke
            val rimStrokeColor = if (isSixRolled) Color(0xFFFFD700) else Color.White.copy(alpha = 0.85f)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(rimStrokeColor, skin.rimTone),
                    start = Offset(innerPad, innerPad),
                    end = Offset(dSize - innerPad, dSize - innerPad)
                ),
                topLeft = Offset(innerPad, innerPad),
                size = Size(dSize - innerPad * 2, dSize - innerPad * 2),
                cornerRadius = CornerRadius(corner * 0.8f, corner * 0.8f),
                style = Stroke(width = if (isSixRolled || enabled) 2.5f else 1.5f)
            )

            // 5. Draw Golden Flower / Die Pips Motif
            drawPips(value, accentColor, dSize, isSix = (value == 6))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPips(
    value: Int,
    accentColor: Color,
    dieSize: Float,
    isSix: Boolean = false
) {
    val pipRadius = dieSize * 0.082f
    val c = dieSize / 2f
    val left = dieSize * 0.30f
    val right = dieSize * 0.70f
    val top = dieSize * 0.30f
    val bottom = dieSize * 0.70f

    val pipColor = if (isSix) Color(0xFFDC2626) else Color(0xFF1E293B)

    fun pip(x: Float, y: Float) {
        drawCircle(color = pipColor, radius = pipRadius, center = Offset(x, y))
        // Subtle shiny reflection in center of pip
        drawCircle(color = Color.White.copy(alpha = 0.45f), radius = pipRadius * 0.35f, center = Offset(x - pipRadius * 0.2f, y - pipRadius * 0.2f))
    }

    when (value) {
        1 -> {
            // Draw 6-petal flower motif
            val petalRadius = dieSize * 0.055f
            val petalDist = dieSize * 0.11f
            for (i in 0 until 6) {
                val angle = i * (Math.PI / 3.0)
                val px = (c + Math.cos(angle) * petalDist).toFloat()
                val py = (c + Math.sin(angle) * petalDist).toFloat()
                drawCircle(color = Color.White, radius = petalRadius, center = Offset(px, py))
            }
            // Center flower core
            drawCircle(color = Color(0xFFDC2626), radius = dieSize * 0.065f, center = Offset(c, c))
            drawCircle(color = Color.White, radius = dieSize * 0.025f, center = Offset(c, c))
        }
        2 -> {
            pip(left, top)
            pip(right, bottom)
        }
        3 -> {
            pip(left, top)
            pip(c, c)
            pip(right, bottom)
        }
        4 -> {
            pip(left, top)
            pip(right, top)
            pip(left, bottom)
            pip(right, bottom)
        }
        5 -> {
            pip(left, top)
            pip(right, top)
            pip(c, c)
            pip(left, bottom)
            pip(right, bottom)
        }
        6 -> {
            pip(left, top)
            pip(right, top)
            pip(left, c)
            pip(right, c)
            pip(left, bottom)
            pip(right, bottom)
        }
    }
}
