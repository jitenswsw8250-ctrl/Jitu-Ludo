package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
    skin: GameSkin = GameSkin.CLASSIC,
    extraTurnReason: String? = null
) {
    val canRoll = isCurrentTurn && !isGameOver && (turnPhase == TurnPhase.WAITING_FOR_ROLL) && !player.isBot && !diceState.isRolling
    val isBotThinking = isCurrentTurn && !isGameOver && player.isBot

    val infiniteTransition = rememberInfiniteTransition(label = "playerTurn_${player.color.name}")
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

    val borderGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

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
            width = if (isCurrentTurn) 2.5.dp else 1.dp,
            brush = if (isCurrentTurn) {
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFD700).copy(alpha = borderGlowAlpha),
                        player.color.primaryColor.copy(alpha = borderGlowAlpha),
                        Color(0xFFFDE047).copy(alpha = borderGlowAlpha)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(Color(0xFF452213).copy(alpha = 0.5f), Color(0xFF2A150A).copy(alpha = 0.5f))
                )
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentTurn) 8.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start
        ) {
            // Header: Avatar, Name & Optional Bonus Turn Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (alignRight) Arrangement.End else Arrangement.Start
            ) {
                if (alignRight) {
                    Text(
                        text = player.name.take(7),
                        color = if (isCurrentTurn) Color(0xFFFFD700) else Color(0xFFE2E8F0),
                        fontWeight = if (isCurrentTurn) FontWeight.Black else FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = player.avatar.emoji, fontSize = 13.sp)
                } else {
                    Text(text = player.avatar.emoji, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = player.name.take(7),
                        color = if (isCurrentTurn) Color(0xFFFFD700) else Color(0xFFE2E8F0),
                        fontWeight = if (isCurrentTurn) FontWeight.Black else FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            // Prominent Bonus Turn Notification Chip when active
            if (isCurrentTurn && !extraTurnReason.isNullOrEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE11D48),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047)),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "BONUS TURN! 🎲",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (!alignRight) {
                    // Left aligned: [Kills/Deaths ⚔️💀] -> [DiceFace] -> [👈 Hand / Turn Status]
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚔️", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${player.captures}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💀", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${player.losses}",
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    DiceFace(
                        value = if (isCurrentTurn) diceState.value else 1,
                        isRolling = if (isCurrentTurn) diceState.isRolling else false,
                        accentColor = player.color.primaryColor,
                        enabled = canRoll,
                        onClick = onRollDice,
                        diceSizeDp = 52.dp,
                        skin = skin,
                        modifier = Modifier.testTag(if (canRoll) "roll_dice_button" else "player_dice_${player.color.name.lowercase()}")
                    )

                    if (canRoll) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier.graphicsLayer { translationX = handOffsetX },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "👈", fontSize = 18.sp)
                        }
                    } else if (isBotThinking) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "🤖",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isCurrentTurn && turnPhase == TurnPhase.SELECTING_TOKEN) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F766E),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "♟️",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    // Right aligned: [👉 Hand / Turn Status] <- [DiceFace] <- [Kills/Deaths ⚔️💀]
                    if (canRoll) {
                        Box(
                            modifier = Modifier.graphicsLayer { translationX = -handOffsetX },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "👉", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (isBotThinking) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "🤖",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (isCurrentTurn && turnPhase == TurnPhase.SELECTING_TOKEN) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F766E),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "♟️",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    DiceFace(
                        value = if (isCurrentTurn) diceState.value else 1,
                        isRolling = if (isCurrentTurn) diceState.isRolling else false,
                        accentColor = player.color.primaryColor,
                        enabled = canRoll,
                        onClick = onRollDice,
                        diceSizeDp = 52.dp,
                        skin = skin,
                        modifier = Modifier.testTag(if (canRoll) "roll_dice_button" else "player_dice_${player.color.name.lowercase()}")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${player.captures}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "⚔️", fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${player.losses}",
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "💀", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3D Vector for real spatial transformations.
 */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)

    fun rotateX(radians: Float): Vec3 {
        val c = cos(radians)
        val s = sin(radians)
        return Vec3(x, y * c - z * s, y * s + z * c)
    }

    fun rotateY(radians: Float): Vec3 {
        val c = cos(radians)
        val s = sin(radians)
        return Vec3(x * c + z * s, y, -x * s + z * c)
    }

    fun rotateZ(radians: Float): Vec3 {
        val c = cos(radians)
        val s = sin(radians)
        return Vec3(x * c - y * s, x * s + y * c, z)
    }

    fun rotate(rotXRad: Float, rotYRad: Float, rotZRad: Float): Vec3 {
        return this.rotateX(rotXRad).rotateY(rotYRad).rotateZ(rotZRad)
    }
}

/**
 * Geometric definition of one of the 6 faces on a 3D dice cube.
 */
private data class CubeFaceDef(
    val faceNumber: Int,
    val center: Vec3,
    val uAxis: Vec3,
    val vAxis: Vec3,
    val normal: Vec3,
    val pipCoords: List<Pair<Float, Float>>
)

private fun getCubeFaces(s: Float): List<CubeFaceDef> {
    val left = -0.52f
    val right = 0.52f
    val top = -0.52f
    val bottom = 0.52f

    val pips1 = listOf(0f to 0f)
    val pips2 = listOf(left to top, right to bottom)
    val pips3 = listOf(left to top, 0f to 0f, right to bottom)
    val pips4 = listOf(left to top, right to top, left to bottom, right to bottom)
    val pips5 = listOf(left to top, right to top, 0f to 0f, left to bottom, right to bottom)
    val pips6 = listOf(left to top, right to top, left to 0f, right to 0f, left to bottom, right to bottom)

    return listOf(
        // Face 1: Front (Z = +s, normal points directly toward camera)
        CubeFaceDef(1, Vec3(0f, 0f, s), Vec3(1f, 0f, 0f), Vec3(0f, 1f, 0f), Vec3(0f, 0f, 1f), pips1),
        // Face 2: Top (Y = -s, normal points upward)
        CubeFaceDef(2, Vec3(0f, -s, 0f), Vec3(1f, 0f, 0f), Vec3(0f, 0f, 1f), Vec3(0f, -1f, 0f), pips2),
        // Face 3: Right (X = +s, normal points right)
        CubeFaceDef(3, Vec3(s, 0f, 0f), Vec3(0f, 0f, -1f), Vec3(0f, 1f, 0f), Vec3(1f, 0f, 0f), pips3),
        // Face 4: Left (X = -s, normal points left)
        CubeFaceDef(4, Vec3(-s, 0f, 0f), Vec3(0f, 0f, 1f), Vec3(0f, 1f, 0f), Vec3(-1f, 0f, 0f), pips4),
        // Face 5: Bottom (Y = +s, normal points downward)
        CubeFaceDef(5, Vec3(0f, s, 0f), Vec3(1f, 0f, 0f), Vec3(0f, 0f, -1f), Vec3(0f, 1f, 0f), pips5),
        // Face 6: Back (Z = -s, normal points into screen)
        CubeFaceDef(6, Vec3(0f, 0f, -s), Vec3(-1f, 0f, 0f), Vec3(0f, 1f, 0f), Vec3(0f, 0f, -1f), pips6)
    )
}

/**
 * Returns (targetRotXDeg, targetRotYDeg) to bring the specified face exactly
 * forward (flat, level, facing camera, upright).
 */
private fun getTargetAngles(face: Int): Pair<Float, Float> = when (face) {
    1 -> 0f to 0f
    2 -> -90f to 0f
    3 -> 0f to -90f
    4 -> 0f to 90f
    5 -> 90f to 0f
    6 -> 0f to 180f
    else -> 0f to 0f
}

/**
 * RECOGNIZABLE PREMIUM 3D METALLIC GOLDEN CASINO DICE:
 * - Real 3D cube with 6 clearly visible golden faces.
 * - Elegant dark royal-blue accents ONLY on the edges/perimeter.
 * - Traditional circular jet-black dice dots (pips) in authentic 1 to 6 arrangements.
 * - Bold, prominent size filling the player dice area (no tiny box/tile).
 * - Full 3D tumbling rotation around both X and Y axes over 800ms.
 * - Settles squarely, completely flat, upright on the target face without tilt or distortion.
 * - Single fixed dice object per player turn; anti-spam touch handling.
 */
@Composable
fun DiceFace(
    value: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diceSizeDp: Dp = 52.dp,
    accentColor: Color = Color(0xFFF59E0B),
    isRolling: Boolean = false,
    skin: GameSkin = GameSkin.CLASSIC
) {
    val rollProgress = remember { Animatable(1f) }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            rollProgress.snapTo(0f)
            rollProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        } else {
            rollProgress.snapTo(1f)
        }
    }

    val targetValue = value.coerceIn(1, 6)
    val (targetX, targetY) = getTargetAngles(targetValue)

    val p = rollProgress.value
    val remaining = 1f - p

    // 2-3 full 360° rotations on both X and Y with natural 3D tumble
    val currentRotX = targetX + 720f * remaining
    val currentRotY = targetY + 1080f * remaining
    val currentRotZ = sin(p * PI.toFloat()) * 25f * remaining
    val bounceY = sin(p * PI.toFloat()) * -14f

    Box(
        modifier = modifier
            .size(diceSizeDp)
            .clickable(
                enabled = enabled && !isRolling,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diceSizeDp)) {
            val dSize = size.width
            val originX = dSize / 2f
            val originY = dSize / 2f
            val s = dSize * 0.38f
            val camDist = dSize * 4.0f

            // 1. Soft 3D drop shadow reacting to physical bounce
            val shadowLiftFactor = (bounceY / -14f).coerceIn(0f, 1f)
            val shadowAlpha = 0.36f - shadowLiftFactor * 0.16f
            val shadowRadiusX = s * (1.15f + shadowLiftFactor * 0.18f)
            val shadowRadiusY = s * 0.28f

            drawOval(
                color = Color.Black.copy(alpha = shadowAlpha.coerceIn(0.10f, 0.40f)),
                topLeft = Offset(originX - shadowRadiusX, dSize * 0.84f),
                size = Size(shadowRadiusX * 2f, shadowRadiusY * 2f)
            )

            // Convert angles to radians
            val rotXRad = currentRotX * (PI.toFloat() / 180f)
            val rotYRad = currentRotY * (PI.toFloat() / 180f)
            val rotZRad = currentRotZ * (PI.toFloat() / 180f)

            fun project(point3D: Vec3): Offset {
                val factor = camDist / (camDist - point3D.z)
                return Offset(
                    originX + point3D.x * factor,
                    originY + point3D.y * factor + bounceY
                )
            }

            // Directional lighting from top-left-front
            val lightDir = Vec3(-0.35f, -0.60f, 0.70f)

            // Data class for visible face rendering with depth sorting
            data class VisibleFace(
                val faceDef: CubeFaceDef,
                val rotCenter: Vec3,
                val rotU: Vec3,
                val rotV: Vec3,
                val rotNormal: Vec3,
                val corners: List<Offset>,
                val lightFactor: Float
            )

            val visibleFaces = mutableListOf<VisibleFace>()

            val faces = getCubeFaces(s)
            for (face in faces) {
                val rotCenter = face.center.rotate(rotXRad, rotYRad, rotZRad)
                val rotU = face.uAxis.rotate(rotXRad, rotYRad, rotZRad)
                val rotV = face.vAxis.rotate(rotXRad, rotYRad, rotZRad)
                val rotNormal = face.normal.rotate(rotXRad, rotYRad, rotZRad)

                // 4 corners of the face quad
                val c0 = rotCenter - rotU * s - rotV * s
                val c1 = rotCenter + rotU * s - rotV * s
                val c2 = rotCenter + rotU * s + rotV * s
                val c3 = rotCenter - rotU * s + rotV * s

                val p0 = project(c0)
                val p1 = project(c1)
                val p2 = project(c2)
                val p3 = project(c3)

                // Back-face culling via 2D cross-product
                val cross = (p1.x - p0.x) * (p2.y - p0.y) - (p1.y - p0.y) * (p2.x - p0.x)
                if (cross <= 0.05f) continue // Face is angled away from viewer

                // Compute directional illumination
                val dot = (rotNormal.x * lightDir.x + rotNormal.y * lightDir.y + rotNormal.z * lightDir.z) / 0.986f
                val lightFactor = ((dot + 1f) / 2f).coerceIn(0.42f, 1.0f)

                visibleFaces.add(
                    VisibleFace(
                        faceDef = face,
                        rotCenter = rotCenter,
                        rotU = rotU,
                        rotV = rotV,
                        rotNormal = rotNormal,
                        corners = listOf(p0, p1, p2, p3),
                        lightFactor = lightFactor
                    )
                )
            }

            // Painter's algorithm: sort back-to-front (smaller Z is farther, larger Z is closer to camera)
            val sortedFaces = visibleFaces.sortedBy { it.rotCenter.z }

            for (vf in sortedFaces) {
                val lf = vf.lightFactor
                val p0 = vf.corners[0]
                val p1 = vf.corners[1]
                val p2 = vf.corners[2]
                val p3 = vf.corners[3]

                // --- 1. Beautiful Metallic Golden Face Body ---
                val outerPath = Path().apply {
                    moveTo(p0.x, p0.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    close()
                }

                val goldHighlight = Color(
                    red = (1.0f * lf).coerceIn(0.58f, 1.0f),
                    green = (0.92f * lf).coerceIn(0.50f, 0.98f),
                    blue = (0.50f * lf).coerceIn(0.20f, 0.70f),
                    alpha = 1.0f
                )
                val goldCore = Color(
                    red = (1.0f * lf).coerceIn(0.52f, 1.0f),
                    green = (0.84f * lf).coerceIn(0.42f, 0.92f),
                    blue = (0.16f * lf).coerceIn(0.06f, 0.35f),
                    alpha = 1.0f
                )
                val goldMid = Color(
                    red = (0.92f * lf).coerceIn(0.45f, 0.95f),
                    green = (0.70f * lf).coerceIn(0.32f, 0.78f),
                    blue = (0.10f * lf).coerceIn(0.04f, 0.25f),
                    alpha = 1.0f
                )
                val goldShade = Color(
                    red = (0.75f * lf).coerceIn(0.35f, 0.80f),
                    green = (0.52f * lf).coerceIn(0.22f, 0.60f),
                    blue = (0.05f * lf).coerceIn(0.02f, 0.15f),
                    alpha = 1.0f
                )

                // Lustrous metallic gold gradient across the entire face
                drawPath(
                    path = outerPath,
                    brush = Brush.linearGradient(
                        colors = listOf(goldHighlight, goldCore, goldMid, goldShade),
                        start = p0,
                        end = p2
                    )
                )

                // --- 2. Elegant Dark Royal-Blue Accents ONLY on the Edges ---
                val edgeBlue = Color(
                    red = (0.06f * lf).coerceIn(0.02f, 0.18f),
                    green = (0.10f * lf).coerceIn(0.04f, 0.26f),
                    blue = (0.32f * lf).coerceIn(0.12f, 0.52f),
                    alpha = 0.95f
                )
                drawPath(outerPath, color = edgeBlue, style = Stroke(width = 2.2f))

                // Fine inner champagne gold bevel line along the perimeter
                val innerBevelGold = Color(
                    red = (1.0f * lf).coerceIn(0.55f, 1.0f),
                    green = (0.94f * lf).coerceIn(0.48f, 0.96f),
                    blue = (0.65f * lf).coerceIn(0.28f, 0.78f),
                    alpha = 0.65f
                )
                drawPath(outerPath, color = innerBevelGold, style = Stroke(width = 0.9f))

                // --- 3. Traditional Deep Black Circular Pips (Dice Dots) ---
                for ((pu, pv) in vf.faceDef.pipCoords) {
                    val pip3D = vf.rotCenter + vf.rotU * (s * pu) + vf.rotV * (s * pv)
                    val pipScreen = project(pip3D)
                    val pipPerspectiveFactor = camDist / (camDist - pip3D.z)
                    // Face 1 has a slightly larger classic casino center dot
                    val basePipRadius = if (vf.faceDef.faceNumber == 1) s * 0.17f else s * 0.13f
                    val pipRadius = basePipRadius * pipPerspectiveFactor

                    // Recessed carved drop shadow at top-left
                    drawCircle(
                        color = Color(0x45000000),
                        radius = pipRadius * 1.10f,
                        center = Offset(pipScreen.x - 0.7f, pipScreen.y - 0.7f)
                    )
                    // High-contrast solid deep black circular pip core
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = pipRadius,
                        center = pipScreen
                    )
                    // Subtle specular glint on the glossy pip
                    drawCircle(
                        color = Color.White.copy(alpha = (0.42f * lf).coerceIn(0.15f, 0.60f)),
                        radius = pipRadius * 0.28f,
                        center = Offset(pipScreen.x + pipRadius * 0.25f, pipScreen.y + pipRadius * 0.25f)
                    )
                }
            }

            // 4. Vibrant glowing border when waiting for the active player's roll
            if (enabled && !isRolling) {
                drawRoundRect(
                    color = Color(0xFFFFD700).copy(alpha = 0.55f),
                    topLeft = Offset(1.5f, 1.5f),
                    size = Size(dSize - 3f, dSize - 3f),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    style = Stroke(width = 1.8f)
                )
            }
        }
    }
}



