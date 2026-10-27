package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CaptureAnimationEvent
import kotlin.math.roundToInt

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

    androidx.compose.runtime.LaunchedEffect(event.eventId) {
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

    val badgeSizeDp = with(LocalDensity.current) { (cellSize * 1.8f).toDp() }
    val emojiFontSize = (cellSize * 0.85f).sp

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("capture_animation_overlay")
    ) {
        // 1. Capturing Player's Celebratory Emoji at Capture location
        val capturePxX = event.captureBoardCoord.x * cellSize
        val capturePxY = event.captureBoardCoord.y * cellSize
        val badgeHalfPx = (cellSize * 1.8f) / 2f

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
                .clip(CircleShape)
                .background(Color(0xFF1E293B).copy(alpha = 0.92f))
                .border(2.dp, Color(0xFFFBBF24), CircleShape)
                .testTag("capturing_home_emoji"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = event.capturingEmoji,
                fontSize = emojiFontSize
            )
        }

        // 2. Captured Player's Crying / Dizzy Emoji at Base Slot location
        val basePxX = event.capturedBaseCoord.x * cellSize
        val basePxY = event.capturedBaseCoord.y * cellSize

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
                .clip(CircleShape)
                .background(Color(0xFF0F172A).copy(alpha = 0.90f))
                .border(2.dp, Color(0xFFEF4444), CircleShape)
                .testTag("captured_home_emoji"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = event.capturedEmoji,
                fontSize = emojiFontSize
            )
        }
    }
}
