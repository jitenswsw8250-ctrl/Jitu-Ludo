package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.logic.FloatPoint
import com.example.logic.LudoBoardMap
import com.example.model.CaptureAnimationEvent

/**
 * Renders capture emojis strictly INSIDE each player's colored home/base area near their tokens:
 * - 😎 or 🔥 in the capturing player's home base
 * - 😭 or 😵‍💫 in the captured player's home base
 * Never rendered on the board track or outside the home areas.
 */
@Composable
fun CaptureAnimationOverlay(
    event: CaptureAnimationEvent,
    cellSize: Float,
    boardPixelSize: Float,
    modifier: Modifier = Modifier
) {
    var isStarted by remember(event.eventId) { mutableStateOf(false) }

    LaunchedEffect(event.eventId) {
        isStarted = true
    }

    // Pop-in spring scale
    val popScale by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0.1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "popScale"
    )

    // Infinite gentle bounce for celebratory emoji inside capturing home area
    val infiniteTransition = rememberInfiniteTransition(label = "captureBounce")
    val celebrateBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebrateBounce"
    )

    // Infinite wobble / dizzy shake for crying emoji inside captured home area
    val dizzyShake by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(180, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dizzyShake"
    )

    val bubblePixelSize = cellSize * 1.8f
    val density = LocalDensity.current
    val bubbleDp = with(density) { bubblePixelSize.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("capture_animation_overlay")
    ) {
        // 1. Celebratory Emoji (😎 / 🔥) strictly INSIDE Capturing Player's Home Base
        val capturingCenter = LudoBoardMap.BASE_CENTERS[event.capturingPlayerColor] ?: FloatPoint(3f, 3f)
        val capturingCenterX = capturingCenter.x * cellSize
        val capturingCenterY = capturingCenter.y * cellSize

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = capturingCenterX - bubblePixelSize / 2f
                    translationY = capturingCenterY - bubblePixelSize / 2f + celebrateBounce
                    scaleX = popScale
                    scaleY = popScale
                }
                .size(bubbleDp)
                .testTag("capturing_home_emoji"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                event.capturingPlayerColor.lightColor.copy(alpha = 0.95f),
                                event.capturingPlayerColor.primaryColor
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = Color(0xFFFDE68A),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = event.capturingEmoji,
                    fontSize = with(density) { (bubblePixelSize * 0.55f).toSp() }
                )
            }
        }

        // 2. Crying / Dizzy Emoji (😭 / 😵‍💫) strictly INSIDE Captured Player's Home Base
        val capturedCenter = LudoBoardMap.BASE_CENTERS[event.capturedPlayerColor] ?: FloatPoint(12f, 12f)
        val capturedCenterX = capturedCenter.x * cellSize
        val capturedCenterY = capturedCenter.y * cellSize

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = capturedCenterX - bubblePixelSize / 2f
                    translationY = capturedCenterY - bubblePixelSize / 2f
                    rotationZ = dizzyShake
                    scaleX = popScale
                    scaleY = popScale
                }
                .size(bubbleDp)
                .testTag("captured_home_emoji"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                event.capturedPlayerColor.lightColor.copy(alpha = 0.95f),
                                event.capturedPlayerColor.darkColor
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = Color(0xFFFCA5A5),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = event.capturedEmoji,
                    fontSize = with(density) { (bubblePixelSize * 0.55f).toSp() }
                )
            }
        }
    }
}
