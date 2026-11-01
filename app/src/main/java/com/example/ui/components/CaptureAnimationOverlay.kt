package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CaptureAnimationEvent
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object CaptureAnimationDimensions {
    const val BADGE_DIAMETER_CELL_RATIO = 2.0f
    const val EMOJI_TO_BADGE_RATIO = 0.50f

    fun calculateBadgeDiameterPx(cellSize: Float): Float = cellSize * BADGE_DIAMETER_CELL_RATIO
    fun calculateEmojiFontSizePx(cellSize: Float): Float = calculateBadgeDiameterPx(cellSize) * EMOJI_TO_BADGE_RATIO
}

@Composable
fun CaptureAnimationOverlay(
    event: CaptureAnimationEvent,
    cellSize: Float,
    boardPixelSize: Float,
    modifier: Modifier = Modifier
) {
    var isLaunched by remember(event.eventId) { mutableStateOf(false) }

    val popScale by animateFloatAsState(
        targetValue = if (isLaunched) 1.0f else 0.2f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "popScale"
    )

    // Golden Burst Shockwave Progress (0f -> 1f over 650ms)
    val burstProgress by animateFloatAsState(
        targetValue = if (isLaunched) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "burstProgress"
    )

    // Notification Banner Fade Progress
    val bannerScale by animateFloatAsState(
        targetValue = if (isLaunched) 1.0f else 0.5f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "bannerScale"
    )

    LaunchedEffect(event.eventId) {
        isLaunched = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "captureBounce")

    val celebrateBounce by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebrateBounce"
    )

    val dizzyShake by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dizzyShake"
    )

    val density = LocalDensity.current
    val badgeDiameterPx = CaptureAnimationDimensions.calculateBadgeDiameterPx(cellSize)
    val badgeHalfPx = badgeDiameterPx / 2f
    val badgeSizeDp = with(density) { badgeDiameterPx.toDp() }
    val emojiFontSize = with(density) { CaptureAnimationDimensions.calculateEmojiFontSizePx(cellSize).toSp() }

    val capturePxX = event.captureBoardCoord.x * cellSize
    val capturePxY = event.captureBoardCoord.y * cellSize
    val basePxX = event.capturedBaseCoord.x * cellSize
    val basePxY = event.capturedBaseCoord.y * cellSize

    val capturingColor = event.capturingPlayerColor.primaryColor
    val capturedColor = event.capturedPlayerColor.primaryColor

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("capture_animation_overlay")
    ) {
        // 1. Golden Shockwave Burst & Colored Spark Particles Canvas at exact capture location
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (burstProgress > 0f) {
                val centerOffset = Offset(capturePxX, capturePxY)
                val shockwaveRadius = burstProgress * cellSize * 2.2f
                val shockwaveAlpha = (1f - burstProgress).coerceIn(0f, 1f)

                // Outer golden shockwave ring
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = shockwaveAlpha * 0.9f),
                    radius = shockwaveRadius,
                    center = centerOffset,
                    style = Stroke(width = (4f * (1f - burstProgress)).coerceAtLeast(1f))
                )

                // Secondary tinted shockwave ring
                drawCircle(
                    color = capturingColor.copy(alpha = shockwaveAlpha * 0.6f),
                    radius = shockwaveRadius * 0.75f,
                    center = centerOffset,
                    style = Stroke(width = (3f * (1f - burstProgress)).coerceAtLeast(1f))
                )

                // Central impact flash
                if (burstProgress < 0.45f) {
                    val flashAlpha = ((0.45f - burstProgress) / 0.45f).coerceIn(0f, 1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = flashAlpha), Color(0xFFFFD700).copy(alpha = flashAlpha * 0.6f), Color.Transparent),
                            center = centerOffset,
                            radius = cellSize * 1.2f
                        ),
                        radius = cellSize * 1.2f,
                        center = centerOffset
                    )
                }

                // 8 Radiating colored spark particles flying outward
                val particleDistance = burstProgress * cellSize * 1.8f
                val particleRadius = (1f - burstProgress * 0.4f) * (cellSize * 0.11f)
                val particleAlpha = (1f - burstProgress).coerceIn(0f, 1f)

                for (i in 0 until 8) {
                    val angle = (i * Math.PI / 4.0) + (burstProgress * 0.2)
                    val px = (capturePxX + cos(angle) * particleDistance).toFloat()
                    val py = (capturePxY + sin(angle) * particleDistance).toFloat()
                    val pColor = when (i % 3) {
                        0 -> Color(0xFFFFD700)
                        1 -> capturingColor
                        else -> Color(0xFFFFFBEB)
                    }
                    drawCircle(
                        color = pColor.copy(alpha = particleAlpha),
                        radius = particleRadius,
                        center = Offset(px, py)
                    )
                }
            }
        }

        // 2. Capturing Player's Celebratory Emoji at Exact Capture location
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (capturePxX - badgeHalfPx).roundToInt(),
                        (capturePxY - badgeHalfPx).roundToInt()
                    )
                }
                .size(badgeSizeDp)
                .graphicsLayer {
                    scaleX = popScale * celebrateBounce
                    scaleY = popScale * celebrateBounce
                }
                .background(Color(0xFF1E293B).copy(alpha = 0.95f), CircleShape)
                .border(2.5.dp, Color(0xFFFBBF24), CircleShape)
                .clip(CircleShape)
                .testTag("capturing_home_emoji"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = event.capturingEmoji,
                fontSize = emojiFontSize,
                textAlign = TextAlign.Center
            )
        }

        // 3. Captured Player's Returned Base Slot with Dizzy/Crying Emoji
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (basePxX - badgeHalfPx).roundToInt(),
                        (basePxY - badgeHalfPx).roundToInt()
                    )
                }
                .size(badgeSizeDp)
                .graphicsLayer {
                    scaleX = popScale
                    scaleY = popScale
                    rotationZ = dizzyShake
                }
                .background(Color(0xFF0F172A).copy(alpha = 0.95f), CircleShape)
                .border(2.5.dp, Color(0xFFEF4444), CircleShape)
                .clip(CircleShape)
                .testTag("captured_home_emoji"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = event.capturedEmoji,
                fontSize = emojiFontSize,
                textAlign = TextAlign.Center
            )
        }

        // 4. Floating Capture Notification Banner: "TOKEN CAPTURED!"
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = bannerScale
                        scaleY = bannerScale
                        alpha = popScale
                    }
                    .testTag("capture_notification_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF21C0D07)),
                border = androidx.compose.foundation.BorderStroke(
                    1.8.dp,
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFFD700), Color(0xFFF59E0B), Color(0xFFFFD700))
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "⚔️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TOKEN CAPTURED!",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${event.capturingPlayerName} sent ${event.capturedPlayerName} home!",
                            color = Color(0xFFFEF3C7),
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "💥", fontSize = 16.sp)
                }
            }
        }
    }
}
