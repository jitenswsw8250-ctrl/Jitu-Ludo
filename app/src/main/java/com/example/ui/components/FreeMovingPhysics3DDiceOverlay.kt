package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.DiceState
import com.example.model.GameSkin
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.model.TurnPhase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Real Free-Moving 3D Physics Dice Overlay.
 *
 * Provides a genuine physics-simulated dice that moves freely across the entire screen:
 * - Can be picked up, dragged, and thrown in ANY direction with finger flick speed.
 * - Tap-to-roll fallback provides a natural toss towards the board center.
 * - Dynamic 3D tumbling rotation around X, Y, and Z axes proportional to trajectory and momentum.
 * - Vertical height elevation and gravity bounces with realistic contact shadow scaling.
 * - Boundary collisions with screen borders (elastic bounces with angular spin impulses).
 * - Automatic realistic throws for Computer Bot players.
 * - Settles upright on the fair engine result with golden celebration sparkle effects.
 * - Prevents double rolls during active flight.
 */
@Composable
fun FreeMovingPhysics3DDiceOverlay(
    diceState: DiceState,
    currentPlayer: Player?,
    phase: TurnPhase,
    isGameOver: Boolean,
    canRoll: Boolean,
    onRollDice: () -> Unit,
    skin: GameSkin = GameSkin.CLASSIC,
    screenWidthPx: Float,
    screenHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val diceSizeDp = 56.dp
    val diceSizePx = with(density) { diceSizeDp.toPx() }

    val currentOnRollDice by rememberUpdatedState(onRollDice)
    val currentCanRoll by rememberUpdatedState(canRoll)
    val currentIsRolling by rememberUpdatedState(diceState.isRolling)

    // Compute player's home dock position on screen in pixels
    fun getDockPosition(color: PlayerColor?): Offset {
        val pad = with(density) { 20.dp.toPx() }
        val topY = with(density) { 62.dp.toPx() }
        val bottomY = screenHeightPx - with(density) { 185.dp.toPx() }
        val leftX = pad
        val rightX = screenWidthPx - pad - diceSizePx

        return when (color) {
            PlayerColor.GREEN -> Offset(leftX, topY)
            PlayerColor.YELLOW -> Offset(rightX, topY)
            PlayerColor.RED -> Offset(leftX, bottomY)
            PlayerColor.BLUE -> Offset(rightX, bottomY)
            else -> Offset(leftX, bottomY)
        }
    }

    val activeDock = remember(currentPlayer?.color, screenWidthPx, screenHeightPx) {
        getDockPosition(currentPlayer?.color)
    }

    // Physics State
    var posX by remember { mutableFloatStateOf(activeDock.x) }
    var posY by remember { mutableFloatStateOf(activeDock.y) }
    var posZ by remember { mutableFloatStateOf(0f) } // Elevation height in px

    var velX by remember { mutableFloatStateOf(0f) }
    var velY by remember { mutableFloatStateOf(0f) }
    var velZ by remember { mutableFloatStateOf(0f) }

    var rotX by remember { mutableFloatStateOf(0f) }
    var rotY by remember { mutableFloatStateOf(0f) }
    var rotZ by remember { mutableFloatStateOf(0f) }

    var angVelX by remember { mutableFloatStateOf(0f) }
    var angVelY by remember { mutableFloatStateOf(0f) }
    var angVelZ by remember { mutableFloatStateOf(0f) }

    var isDragging by remember { mutableStateOf(false) }
    var isInFlight by remember { mutableStateOf(false) }
    var displayValue by remember { mutableIntStateOf(diceState.value) }

    // Dock snap when turn changes and dice is idle
    LaunchedEffect(currentPlayer?.color, phase) {
        if (!isInFlight && !isDragging) {
            val target = getDockPosition(currentPlayer?.color)
            posX = target.x
            posY = target.y
            posZ = 0f
            velX = 0f
            velY = 0f
            velZ = 0f
            rotX = 0f
            rotY = 0f
            rotZ = 0f
        }
    }

    // Keep display value in sync with engine state when idle
    LaunchedEffect(diceState.value) {
        if (!isInFlight) {
            displayValue = diceState.value
        }
    }

    // Velocity tracker for touch gestures
    val velocityTracker = remember { VelocityTracker() }

    // Boundaries for boundary bounces
    val boundLeft = with(density) { 16.dp.toPx() }
    val boundRight = screenWidthPx - with(density) { 16.dp.toPx() } - diceSizePx
    val boundTop = with(density) { 60.dp.toPx() }
    val boundBottom = screenHeightPx - with(density) { 80.dp.toPx() } - diceSizePx

    // Main Physics Simulation Loop
    LaunchedEffect(isInFlight) {
        if (isInFlight) {
            var lastNanos = System.nanoTime()
            var flightTime = 0f
            var hasTriggeredEngineRoll = false

            while (isInFlight) {
                withFrameNanos { nowNanos ->
                    val dt = ((nowNanos - lastNanos) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.033f)
                    lastNanos = nowNanos
                    flightTime += dt

                    // Update Position
                    posX += velX * dt
                    posY += velY * dt
                    posZ += velZ * dt
                    velZ -= 3200f * dt // Gravity

                    // Ground Bounce
                    if (posZ <= 0f) {
                        posZ = 0f
                        if (abs(velZ) > 80f) {
                            velZ = -velZ * 0.52f // Restitution
                        } else {
                            velZ = 0f
                        }
                    }

                    // Screen Boundary Collisions (Elastic Bounces)
                    if (posX < boundLeft) {
                        posX = boundLeft
                        velX = -velX * 0.68f
                        angVelY = -angVelY * 0.7f + Random.nextFloat() * 200f
                    } else if (posX > boundRight) {
                        posX = boundRight
                        velX = -velX * 0.68f
                        angVelY = -angVelY * 0.7f - Random.nextFloat() * 200f
                    }

                    if (posY < boundTop) {
                        posY = boundTop
                        velY = -velY * 0.68f
                        angVelX = -angVelX * 0.7f + Random.nextFloat() * 200f
                    } else if (posY > boundBottom) {
                        posY = boundBottom
                        velY = -velY * 0.68f
                        angVelX = -angVelX * 0.7f - Random.nextFloat() * 200f
                    }

                    // Friction & Drag
                    velX *= (1f - 2.6f * dt).coerceAtLeast(0f)
                    velY *= (1f - 2.6f * dt).coerceAtLeast(0f)

                    // 3D Rotations
                    rotX += (velY * 0.45f + angVelX) * dt
                    rotY += (-velX * 0.45f + angVelY) * dt
                    rotZ += angVelZ * dt

                    angVelX *= (1f - 3.2f * dt).coerceAtLeast(0f)
                    angVelY *= (1f - 3.2f * dt).coerceAtLeast(0f)
                    angVelZ *= (1f - 3.2f * dt).coerceAtLeast(0f)

                    // Tumble face values during active rolling
                    if (flightTime > 0.08f && !hasTriggeredEngineRoll) {
                        hasTriggeredEngineRoll = true
                        currentOnRollDice()
                    }

                    val totalSpeed = hypot(velX, velY) + abs(velZ)
                    if (totalSpeed > 60f) {
                        displayValue = Random.nextInt(1, 7)
                    }

                    // Settling check: Stopped moving and engine finished rolling
                    if (flightTime > 0.75f && totalSpeed < 35f && posZ <= 1f && !currentIsRolling) {
                        velX = 0f
                        velY = 0f
                        velZ = 0f
                        posZ = 0f
                        displayValue = diceState.value
                        isInFlight = false
                    }
                }
            }

            // Smooth upright settling animation
            val startRotX = rotX % 360f
            val startRotY = rotY % 360f
            val startRotZ = rotZ % 360f
            val settleAnim = Animatable(0f)
            settleAnim.animateTo(1f, tween(180, easing = FastOutSlowInEasing)) {
                rotX = startRotX * (1f - value)
                rotY = startRotY * (1f - value)
                rotZ = startRotZ * (1f - value)
            }
            rotX = 0f
            rotY = 0f
            rotZ = 0f
            displayValue = diceState.value
        }
    }

    // Bot Automatic Throw Handling
    LaunchedEffect(currentPlayer?.isBot, canRoll, isInFlight, isDragging) {
        if (currentPlayer?.isBot == true && canRoll && !isInFlight && !isDragging && !isGameOver) {
            delay(550) // Realistic contemplation pause before throw
            if (canRoll && !isInFlight && !isDragging) {
                // Aim throw towards the board center with realistic variations
                val targetX = screenWidthPx / 2f + Random.nextFloat() * 120f - 60f
                val targetY = screenHeightPx / 2f + Random.nextFloat() * 120f - 60f
                val dx = targetX - posX
                val dy = targetY - posY
                val dist = hypot(dx, dy).coerceAtLeast(10f)

                val throwSpeed = Random.nextFloat() * 450f + 850f // px/sec
                velX = (dx / dist) * throwSpeed
                velY = (dy / dist) * throwSpeed
                velZ = Random.nextFloat() * 300f + 750f // High natural toss
                angVelX = Random.nextFloat() * 800f - 400f
                angVelY = Random.nextFloat() * 800f - 400f
                angVelZ = Random.nextFloat() * 700f - 350f
                isInFlight = true
            }
        }
    }

    // Glow effect for 6 celebration
    val infiniteTransition = rememberInfiniteTransition(label = "sixSparkle")
    val sixGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Hand indicator floating offset
    val handBob by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handBob"
    )

    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(posX.toInt(), posY.toInt()) }
                .size(diceSizeDp)
                .zIndex(100f)
            .pointerInput(canRoll, isInFlight) {
                if (!canRoll || isInFlight) return@pointerInput

                detectTapGestures(
                    onTap = {
                        // Natural toss towards center of the board
                        val targetX = screenWidthPx / 2f + Random.nextFloat() * 80f - 40f
                        val targetY = screenHeightPx / 2f + Random.nextFloat() * 80f - 40f
                        val dx = targetX - posX
                        val dy = targetY - posY
                        val dist = hypot(dx, dy).coerceAtLeast(10f)

                        val tapSpeed = 950f
                        velX = (dx / dist) * tapSpeed
                        velY = (dy / dist) * tapSpeed
                        velZ = 900f
                        angVelX = 720f
                        angVelY = 540f
                        angVelZ = 420f
                        isInFlight = true
                    }
                )
            }
            .pointerInput(canRoll, isInFlight) {
                if (!canRoll || isInFlight) return@pointerInput

                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        posZ = with(density) { 24.dp.toPx() } // Lift off ground
                        velocityTracker.resetTracking()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        posX = (posX + dragAmount.x).coerceIn(boundLeft, boundRight)
                        posY = (posY + dragAmount.y).coerceIn(boundTop, boundBottom)
                        rotX += dragAmount.y * 0.35f
                        rotY -= dragAmount.x * 0.35f
                    },
                    onDragEnd = {
                        isDragging = false
                        val velocity = velocityTracker.calculateVelocity()
                        val speed = hypot(velocity.x, velocity.y)

                        if (speed > 120f) {
                            // Launch with user finger flick velocity
                            velX = velocity.x.coerceIn(-2200f, 2200f)
                            velY = velocity.y.coerceIn(-2200f, 2200f)
                            velZ = (speed * 0.40f).coerceIn(350f, 1000f)
                            angVelX = velocity.y * 0.7f
                            angVelY = -velocity.x * 0.7f
                            angVelZ = (Random.nextFloat() - 0.5f) * speed * 0.8f
                            isInFlight = true
                        } else {
                            // Low-speed release: natural roll towards center
                            val targetX = screenWidthPx / 2f
                            val targetY = screenHeightPx / 2f
                            val dx = targetX - posX
                            val dy = targetY - posY
                            val dist = hypot(dx, dy).coerceAtLeast(10f)

                            velX = (dx / dist) * 750f
                            velY = (dy / dist) * 750f
                            velZ = 650f
                            angVelX = 600f
                            angVelY = 450f
                            angVelZ = 300f
                            isInFlight = true
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        posZ = 0f
                    }
                )
            }
            .testTag("free_moving_3d_dice")
    ) {
        // Drop Shadow (scaled and blurred dynamically with elevation Z)
        val shadowScale = (1f + posZ / 120f).coerceIn(1f, 2.2f)
        val shadowAlpha = (0.42f - posZ / 350f).coerceIn(0.10f, 0.42f)
        val shadowOffsetY = with(density) { (8.dp.toPx() + posZ * 0.45f) }

        Canvas(
            modifier = Modifier
                .size(diceSizeDp)
                .offset { IntOffset(0, shadowOffsetY.toInt()) }
                .graphicsLayer {
                    scaleX = shadowScale
                    scaleY = shadowScale * 0.65f
                    alpha = shadowAlpha
                }
        ) {
            drawRoundRect(
                color = Color.Black,
                size = size,
                cornerRadius = CornerRadius(size.width * 0.30f, size.width * 0.30f)
            )
        }

        // Golden Sparkle Aura when rolling a 6
        if (displayValue == 6 && !isInFlight) {
            Canvas(
                modifier = Modifier
                    .size(diceSizeDp * 1.5f)
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = sixGlowAlpha }
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color.Transparent),
                        center = center,
                        radius = size.width / 2f
                    )
                )
            }
        }

        // 3D Rendered Dice Body with Real Rotations
        Box(
            modifier = Modifier
                .size(diceSizeDp)
                .graphicsLayer {
                    rotationX = rotX
                    rotationY = rotY
                    rotationZ = rotZ
                    cameraDistance = 16f * density.density
                    translationY = -posZ
                    scaleX = if (isDragging) 1.08f else 1.0f
                    scaleY = if (isDragging) 1.08f else 1.0f
                }
                .shadow(
                    elevation = if (isDragging || isInFlight) 12.dp else 6.dp,
                    shape = RoundedCornerShape(14.dp)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF8FAFC),
                            skin.primaryTone.copy(alpha = 0.12f),
                            Color(0xFFE2E8F0)
                        ),
                        center = Offset(diceSizePx * 0.35f, diceSizePx * 0.30f),
                        radius = diceSizePx * 0.95f
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White, Color(0xFFE2E8F0), skin.primaryTone, Color(0xFF94A3B8))
                    ),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Render 3D Pips for the current face
            Canvas(modifier = Modifier.size(diceSizeDp * 0.85f)) {
                drawRealistic3DPips(
                    value = displayValue,
                    pipColor = if (displayValue == 6) Color(0xFFDC2626) else Color(0xFF0F172A)
                )
            }
        }

        // Pointing Hand Indicator (👉 Tap or Throw!) when idle and waiting for roll
        if (canRoll && !isInFlight && !isDragging && currentPlayer?.isBot != true) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(with(density) { (diceSizeDp.toPx() + 6.dp.toPx() + handBob).toInt() }, with(density) { 14.dp.toPx().toInt() }) }
                    .zIndex(105f)
            ) {
                Text(
                    text = "👈",
                    fontSize = 24.sp
                )
            }
        }
    }
    }
}

/**
 * Draws concave, shaded 3D pips on the active dice face with specular glints.
 */
private fun DrawScope.drawRealistic3DPips(value: Int, pipColor: Color) {
    val pipRadius = size.width * 0.115f

    val positions = when (value) {
        1 -> listOf(Offset(0.5f, 0.5f))
        2 -> listOf(Offset(0.28f, 0.28f), Offset(0.72f, 0.72f))
        3 -> listOf(Offset(0.25f, 0.25f), Offset(0.5f, 0.5f), Offset(0.75f, 0.75f))
        4 -> listOf(Offset(0.28f, 0.28f), Offset(0.72f, 0.28f), Offset(0.28f, 0.72f), Offset(0.72f, 0.72f))
        5 -> listOf(Offset(0.25f, 0.25f), Offset(0.75f, 0.25f), Offset(0.5f, 0.5f), Offset(0.25f, 0.75f), Offset(0.75f, 0.75f))
        6 -> listOf(
            Offset(0.28f, 0.22f), Offset(0.72f, 0.22f),
            Offset(0.28f, 0.50f), Offset(0.72f, 0.50f),
            Offset(0.28f, 0.78f), Offset(0.72f, 0.78f)
        )
        else -> listOf(Offset(0.5f, 0.5f))
    }

    for (pos in positions) {
        val centerPt = Offset(size.width * pos.x, size.height * pos.y)

        // Concave pip drop shadow
        drawCircle(
            color = Color(0x35000000),
            radius = pipRadius + 1.2f,
            center = Offset(centerPt.x + 0.8f, centerPt.y + 0.8f)
        )

        // Recessed pip gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    pipColor,
                    pipColor.copy(alpha = 0.90f),
                    Color(0xFF000000)
                ),
                center = Offset(centerPt.x - pipRadius * 0.25f, centerPt.y - pipRadius * 0.25f),
                radius = pipRadius
            ),
            radius = pipRadius,
            center = centerPt
        )

        // White specular glint
        drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = pipRadius * 0.28f,
            center = Offset(centerPt.x - pipRadius * 0.32f, centerPt.y - pipRadius * 0.32f)
        )
    }
}
