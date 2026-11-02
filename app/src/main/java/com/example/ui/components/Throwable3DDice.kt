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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.model.DiceState
import com.example.model.GameSkin
import com.example.model.PlayerColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Realistic 3D Throwable Dice Component.
 * Supports:
 * 1. Tap-to-roll (instant in-place roll with realistic 3D tumble)
 * 2. Touch, Pick Up, Drag & Flick / Throw in ANY direction across the playable area
 * 3. Physics simulation: velocity, boundary bouncing, 3D spin rotation on X/Y/Z, height elevation, friction damping
 * 4. Settles cleanly on the upper face matching the fair engine dice value
 * 5. Smooth return to rest slot after roll resolution
 */
@Composable
fun Throwable3DDice(
    value: Int,
    isRolling: Boolean,
    canRoll: Boolean,
    playerColor: PlayerColor,
    onRollDice: () -> Unit,
    modifier: Modifier = Modifier,
    diceSizeDp: Dp = 48.dp,
    skin: GameSkin = GameSkin.CLASSIC,
    testTag: String = "roll_dice_button"
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val currentOnRollDice by rememberUpdatedState(onRollDice)
    val currentCanRoll by rememberUpdatedState(canRoll)
    val currentIsRolling by rememberUpdatedState(isRolling)

    // Animated Physics State (in pixels)
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val rotZ = remember { Animatable(0f) }
    val elevationZ = remember { Animatable(0f) }
    val diceScale = remember { Animatable(1f) }

    var isBeingDragged by remember { mutableStateOf(false) }
    var isInFlight by remember { mutableStateOf(false) }
    var isReturningHome by remember { mutableStateOf(false) }

    val velocityTracker = remember { VelocityTracker() }

    // Bounds for boundary bounces (relative to home anchor in px)
    val maxHorizontalPx = with(density) { 260.dp.toPx() }
    val maxVerticalPx = with(density) { 360.dp.toPx() }

    // Glow pulse transition when 6 is rolled
    val infiniteTransition = rememberInfiniteTransition(label = "throwableSixGlow")
    val sixGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sixGlow"
    )

    // Handle in-place rolling animation if triggered via tap or external roll
    LaunchedEffect(isRolling) {
        if (isRolling && !isInFlight) {
            launch {
                rotZ.animateTo(rotZ.value + 720f, tween(700, easing = FastOutSlowInEasing))
                rotZ.snapTo(0f)
            }
            launch {
                rotX.animateTo(rotX.value + 540f, tween(700, easing = FastOutSlowInEasing))
                rotX.snapTo(0f)
            }
            launch {
                rotY.animateTo(rotY.value + 360f, tween(700, easing = FastOutSlowInEasing))
                rotY.snapTo(0f)
            }
            launch {
                elevationZ.animateTo(16f, tween(180, easing = FastOutSlowInEasing))
                elevationZ.animateTo(6f, tween(200, easing = LinearEasing))
                elevationZ.animateTo(10f, tween(120, easing = FastOutSlowInEasing))
                elevationZ.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
            }
            launch {
                diceScale.animateTo(1.15f, tween(200, easing = FastOutSlowInEasing))
                diceScale.animateTo(0.95f, tween(300, easing = LinearEasing))
                diceScale.animateTo(1.0f, tween(200, easing = FastOutSlowInEasing))
            }
        }
    }

    val isSixRolled = value == 6 && !isRolling && !isInFlight

    Box(
        modifier = modifier
            .zIndex(if (isBeingDragged || isInFlight || isReturningHome) 100f else 1f)
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationX = rotX.value
                rotationY = rotY.value
                rotationZ = rotZ.value
                scaleX = diceScale.value
                scaleY = diceScale.value
                cameraDistance = 18f * density.density
            }
            .pointerInput(canRoll, isRolling) {
                if (!currentCanRoll || currentIsRolling) return@pointerInput

                detectTapGestures(
                    onTap = {
                        if (currentCanRoll && !currentIsRolling) {
                            currentOnRollDice()
                        }
                    }
                )
            }
            .pointerInput(canRoll, isRolling) {
                if (!currentCanRoll || currentIsRolling) return@pointerInput

                detectDragGestures(
                    onDragStart = {
                        if (!currentCanRoll || currentIsRolling) return@detectDragGestures
                        isBeingDragged = true
                        velocityTracker.resetTracking()
                        coroutineScope.launch {
                            elevationZ.animateTo(22f, tween(150))
                            diceScale.animateTo(1.16f, tween(150))
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (!isBeingDragged) return@detectDragGestures
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                            // Subtle 3D tilt tracking finger drag direction
                            rotX.snapTo((-offsetY.value * 0.12f).coerceIn(-30f, 30f))
                            rotY.snapTo((offsetX.value * 0.12f).coerceIn(-30f, 30f))
                        }
                    },
                    onDragEnd = {
                        if (!isBeingDragged) return@detectDragGestures
                        isBeingDragged = false

                        val velocity = velocityTracker.calculateVelocity()
                        val vx = velocity.x.coerceIn(-2800f, 2800f)
                        val vy = velocity.y.coerceIn(-2800f, 2800f)
                        val totalDistance = kotlin.math.hypot(offsetX.value, offsetY.value)

                        val isFlicked = (abs(vx) > 350f || abs(vy) > 350f || totalDistance > 40f)

                        if (isFlicked && currentCanRoll && !currentIsRolling) {
                            isInFlight = true
                            // Trigger the fair engine roll
                            currentOnRollDice()

                            coroutineScope.launch {
                                // Physics Flight Simulation
                                var currentVx = if (abs(vx) > 100f) vx else (if (offsetX.value >= 0) 650f else -650f)
                                var currentVy = if (abs(vy) > 100f) vy else -850f
                                var currentX = offsetX.value
                                var currentY = offsetY.value
                                var currentRotX = rotX.value
                                var currentRotY = rotY.value
                                var currentRotZ = rotZ.value

                                val dt = 0.016f // 60 FPS step
                                val minX = -maxHorizontalPx
                                val maxX = maxHorizontalPx
                                val minY = -maxVerticalPx
                                val maxY = maxVerticalPx

                                repeat(36) { step ->
                                    currentX += currentVx * dt
                                    currentY += currentVy * dt

                                    // Bounce off boundaries
                                    if (currentX < minX) {
                                        currentX = minX
                                        currentVx = -currentVx * 0.65f
                                    } else if (currentX > maxX) {
                                        currentX = maxX
                                        currentVx = -currentVx * 0.65f
                                    }

                                    if (currentY < minY) {
                                        currentY = minY
                                        currentVy = -currentVy * 0.65f
                                    } else if (currentY > maxY) {
                                        currentY = maxY
                                        currentVy = -currentVy * 0.65f
                                    }

                                    // Friction damping
                                    currentVx *= 0.94f
                                    currentVy *= 0.94f

                                    // 3D spinning
                                    currentRotX += currentVy * 0.15f
                                    currentRotY += currentVx * 0.15f
                                    currentRotZ += (currentVx - currentVy) * 0.08f

                                    offsetX.snapTo(currentX)
                                    offsetY.snapTo(currentY)
                                    rotX.snapTo(currentRotX)
                                    rotY.snapTo(currentRotY)
                                    rotZ.snapTo(currentRotZ)

                                    // Simulated height bounce
                                    val bouncePhase = (step % 12) / 12f * kotlin.math.PI.toFloat()
                                    val decay = max(0f, 1f - step / 36f)
                                    elevationZ.snapTo(kotlin.math.sin(bouncePhase) * 18f * decay)

                                    delay(16)
                                }

                                // Smoothly settle face upright
                                rotX.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                                rotY.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                                rotZ.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                                elevationZ.animateTo(0f, tween(150))
                                diceScale.animateTo(1.0f, tween(150))
                                isInFlight = false

                                // Settle on the board so the user clearly sees their rolled value
                                delay(850)

                                // Smoothly return home to the corner dock
                                isReturningHome = true
                                launch { offsetX.animateTo(0f, tween(450, easing = FastOutSlowInEasing)) }
                                launch { offsetY.animateTo(0f, tween(450, easing = FastOutSlowInEasing)) }
                                delay(460)
                                isReturningHome = false
                            }
                        } else {
                            // Cancelled drag: return to rest
                            coroutineScope.launch {
                                launch { offsetX.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) }
                                launch { offsetY.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) }
                                launch { rotX.animateTo(0f, tween(250)) }
                                launch { rotY.animateTo(0f, tween(250)) }
                                launch { rotZ.animateTo(0f, tween(250)) }
                                launch { elevationZ.animateTo(0f, tween(200)) }
                                launch { diceScale.animateTo(1f, tween(200)) }
                            }
                        }
                    },
                    onDragCancel = {
                        isBeingDragged = false
                        coroutineScope.launch {
                            offsetX.animateTo(0f, tween(250))
                            offsetY.animateTo(0f, tween(250))
                            elevationZ.animateTo(0f, tween(200))
                            diceScale.animateTo(1f, tween(200))
                        }
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        val elevation = elevationZ.value

        Canvas(modifier = Modifier.size(diceSizeDp)) {
            val dSize = size.width
            val pad = dSize * 0.06f
            val corner = dSize * 0.24f

            // 0. Realistic 3D Soft Drop Shadow that expands with elevation
            val shadowOffset = Offset(2f + elevation * 0.25f, 4f + elevation * 0.55f)
            val shadowAlpha = (0.50f - (elevation / 40f) * 0.25f).coerceIn(0.15f, 0.50f)
            drawRoundRect(
                color = Color.Black.copy(alpha = shadowAlpha),
                topLeft = shadowOffset,
                size = Size(dSize - 4f + elevation * 0.2f, dSize - 4f + elevation * 0.2f),
                cornerRadius = CornerRadius(corner * 1.1f, corner * 1.1f)
            )

            // 1. Golden Glow Halo when a 6 is rolled
            if (isSixRolled) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = sixGlowAlpha),
                            Color(0xFFF59E0B).copy(alpha = sixGlowAlpha * 0.6f),
                            Color.Transparent
                        ),
                        center = Offset(dSize / 2f, dSize / 2f),
                        radius = dSize * 0.75f
                    ),
                    topLeft = Offset(-4f, -4f),
                    size = Size(dSize + 8f, dSize + 8f),
                    cornerRadius = CornerRadius(corner * 1.2f, corner * 1.2f)
                )
            }

            // 2. 3D Outer Bezel Border
            val bezelColors = when (skin) {
                GameSkin.CLASSIC -> listOf(Color(0xFF1E3A8A), Color(0xFF0F172A), Color(0xFF1E3A8A))
                GameSkin.RUBY -> listOf(Color(0xFF9F1239), Color(0xFF4C0519), Color(0xFF9F1239))
                GameSkin.EMERALD -> listOf(Color(0xFF065F46), Color(0xFF022C22), Color(0xFF065F46))
                GameSkin.SAPPHIRE -> listOf(Color(0xFF1E40AF), Color(0xFF0F172A), Color(0xFF1E40AF))
                GameSkin.GOLD -> listOf(Color(0xFFB45309), Color(0xFF451A03), Color(0xFFB45309))
                GameSkin.SILVER -> listOf(Color(0xFF475569), Color(0xFF0F172A), Color(0xFF475569))
                GameSkin.DIAMOND -> listOf(Color(0xFF0369A1), Color(0xFF082F49), Color(0xFF0369A1))
                GameSkin.FIRE -> listOf(Color(0xFF9A3412), Color(0xFF431407), Color(0xFF9A3412))
                GameSkin.ICE -> listOf(Color(0xFF155E75), Color(0xFF083344), Color(0xFF155E75))
                GameSkin.ROYAL_GOLD -> listOf(Color(0xFF78350F), Color(0xFF451A03), Color(0xFF78350F))
                GameSkin.RAINBOW -> listOf(Color(0xFF6B21A8), Color(0xFF312E81), Color(0xFF6B21A8))
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

            // 3. Dice Face Base with 3D gradient
            val innerPad = pad * 1.8f
            val faceColors = when (skin) {
                GameSkin.CLASSIC -> listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706))
                GameSkin.RUBY -> listOf(Color(0xFFFFF1F2), Color(0xFFFDA4AF), Color(0xFFF43F5E), Color(0xFFBE123C))
                GameSkin.EMERALD -> listOf(Color(0xFFF0FDF4), Color(0xFFA7F3D0), Color(0xFF10B981), Color(0xFF047857))
                GameSkin.SAPPHIRE -> listOf(Color(0xFFEFF6FF), Color(0xFFBAE6FD), Color(0xFF38BDF8), Color(0xFF0284C7))
                GameSkin.GOLD -> listOf(Color(0xFFFEFCE8), Color(0xFFFEF08A), Color(0xFFFACC15), Color(0xFFCA8A04))
                GameSkin.SILVER -> listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF64748B))
                GameSkin.DIAMOND -> listOf(Color(0xFFF0F9FF), Color(0xFFBAE6FD), Color(0xFF38BDF8), Color(0xFF0284C7))
                GameSkin.FIRE -> listOf(Color(0xFFFFFBEB), Color(0xFFFDE047), Color(0xFFEA580C), Color(0xFF9A3412))
                GameSkin.ICE -> listOf(Color(0xFFECFEFF), Color(0xFFA5F3FC), Color(0xFF06B6D4), Color(0xFF0E7490))
                GameSkin.ROYAL_GOLD -> listOf(Color(0xFFFFFBEB), Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFB45309))
                GameSkin.RAINBOW -> listOf(Color(0xFFFDF4FF), Color(0xFFF472B6), Color(0xFFA855F7), Color(0xFF6366F1))
            }

            drawRoundRect(
                brush = Brush.verticalGradient(faceColors),
                topLeft = Offset(innerPad, innerPad),
                size = Size(dSize - innerPad * 2, dSize - innerPad * 2),
                cornerRadius = CornerRadius(corner * 0.8f, corner * 0.8f)
            )

            // 4. Inner Polished Rim Bevel
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
                style = Stroke(width = if (isSixRolled || canRoll) 2.5f else 1.5f)
            )

            // 5. Draw 3D Pips
            drawRealisticPips(value, playerColor.primaryColor, dSize, isSix = (value == 6))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRealisticPips(
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
        if (!isSix) {
            drawCircle(
                color = accentColor.copy(alpha = 0.25f),
                radius = pipRadius * 1.35f,
                center = Offset(x, y)
            )
        }
        drawCircle(color = pipColor, radius = pipRadius, center = Offset(x, y))
        drawCircle(
            color = Color.White.copy(alpha = 0.60f),
            radius = pipRadius * 0.35f,
            center = Offset(x - pipRadius * 0.25f, y - pipRadius * 0.25f)
        )
    }

    when (value) {
        1 -> {
            // Traditional flower motif for pip 1
            val petalRadius = dieSize * 0.055f
            val petalDist = dieSize * 0.11f
            for (i in 0 until 6) {
                val angle = i * (Math.PI / 3.0)
                val px = (c + Math.cos(angle) * petalDist).toFloat()
                val py = (c + Math.sin(angle) * petalDist).toFloat()
                drawCircle(color = Color.White, radius = petalRadius, center = Offset(px, py))
            }
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
