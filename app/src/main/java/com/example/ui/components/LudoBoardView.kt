package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.example.model.TalkingTokenEvent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.logic.FloatPoint
import com.example.logic.GridCoord
import com.example.logic.LudoBoardMap
import com.example.model.GameState
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.model.Token
import com.example.model.TokenPosition
import com.example.model.TurnPhase
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Premium 3D Ludo Board Component.
 * Features deep saturated home quadrants, polished white base plates with colored socket rings,
 * crisp track squares, and glossy 3D tokens with realistic highlights and contact shadows.
 */
@Composable
fun LudoBoardView(
    gameState: GameState,
    onTokenClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelectingToken = gameState.phase == TurnPhase.SELECTING_TOKEN && gameState.movableTokenIds.isNotEmpty()
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val animatedPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tokenPulse"
    )
    val pulseScale = if (isSelectingToken) animatedPulse else 1.0f

    val currentGameState by rememberUpdatedState(gameState)
    val currentOnTokenClicked by rememberUpdatedState(onTokenClicked)

    // Luxury 3D Bevel Frame themed according to selected BoardTheme
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp)
            .testTag("ludo_board_card"),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 4.dp,
            brush = Brush.linearGradient(gameState.boardTheme.frameBorderBrushColors)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        colors = CardDefaults.cardColors(containerColor = gameState.boardTheme.outerBackground)
    ) {
        BoxWithConstraints(modifier = Modifier.aspectRatio(1f)) {
            val boardPixelSize = constraints.maxWidth.toFloat()
            val cellSize = boardPixelSize / 15f

            Canvas(
                modifier = Modifier
                    .aspectRatio(1f)
                    .testTag("ludo_board_canvas")
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val state = currentGameState
                            if (state.phase == TurnPhase.SELECTING_TOKEN) {
                                val curPlayer = state.currentPlayer
                                if (curPlayer != null && !curPlayer.isBot) {
                                    val allTokens = state.players.flatMap { it.tokens }
                                    val movableTokens = curPlayer.tokens.filter {
                                        state.movableTokenIds.contains(it.id)
                                    }

                                    if (movableTokens.isEmpty()) return@detectTapGestures

                                    val tapCol = (tapOffset.x / cellSize).toInt().coerceIn(0, 14)
                                    val tapRow = (tapOffset.y / cellSize).toInt().coerceIn(0, 14)

                                    var chosenToken: Token? = null
                                    var closestDist = Float.MAX_VALUE

                                    // 1. Priority check: Tokens on the board (track cells, starting cell, home lanes)
                                    val onBoardMovable = movableTokens.filter { it.position is TokenPosition.OnBoard }
                                    for (token in onBoardMovable) {
                                        val displayCenter = getTokenDisplayCenter(token, allTokens, cellSize)
                                        val headCenter = Offset(displayCenter.x, displayCenter.y - cellSize * 0.22f)
                                        val distCenter = hypot(tapOffset.x - displayCenter.x, tapOffset.y - displayCenter.y)
                                        val distHead = hypot(tapOffset.x - headCenter.x, tapOffset.y - headCenter.y)
                                        val minTokenDist = minOf(distCenter, distHead)

                                        val step = (token.position as TokenPosition.OnBoard).step
                                        val coord = if (step < 51) {
                                            val trackIdx = (token.color.startTrackIndex + step) % LudoBoardMap.TRACK_LENGTH
                                            LudoBoardMap.OUTER_TRACK.getOrNull(trackIdx)
                                        } else {
                                            val laneIdx = step - 51
                                            when (token.color) {
                                                PlayerColor.RED -> LudoBoardMap.RED_HOME_LANE
                                                PlayerColor.GREEN -> LudoBoardMap.GREEN_HOME_LANE
                                                PlayerColor.YELLOW -> LudoBoardMap.YELLOW_HOME_LANE
                                                PlayerColor.BLUE -> LudoBoardMap.BLUE_HOME_LANE
                                            }.getOrNull(laneIdx)
                                        }

                                        val isCellHit = coord != null && (tapCol == coord.col && tapRow == coord.row)
                                        if (isCellHit || minTokenDist <= cellSize * 1.35f) {
                                            if (minTokenDist < closestDist) {
                                                closestDist = minTokenDist
                                                chosenToken = token
                                            }
                                        }
                                    }

                                    // 2. Base yard tokens check (bringing a token out of base)
                                    if (chosenToken == null) {
                                        val baseMovable = movableTokens.filter { it.position is TokenPosition.InBase }
                                        if (baseMovable.isNotEmpty()) {
                                            // Check if user tapped inside this player's colored 6x6 home yard quadrant
                                            val inHomeYard = when (curPlayer.color) {
                                                PlayerColor.GREEN -> tapRow in 0..5 && tapCol in 0..5
                                                PlayerColor.YELLOW -> tapRow in 0..5 && tapCol in 9..14
                                                PlayerColor.RED -> tapRow in 9..14 && tapCol in 0..5
                                                PlayerColor.BLUE -> tapRow in 9..14 && tapCol in 9..14
                                            }

                                            if (inHomeYard) {
                                                // Pick closest base slot among movable base tokens, or first available
                                                var closestBaseToken: Token? = null
                                                var closestBaseDist = Float.MAX_VALUE
                                                for (token in baseMovable) {
                                                    val slotCenter = getTokenDisplayCenter(token, allTokens, cellSize)
                                                    val dist = hypot(tapOffset.x - slotCenter.x, tapOffset.y - slotCenter.y)
                                                    if (dist < closestBaseDist) {
                                                        closestBaseDist = dist
                                                        closestBaseToken = token
                                                    }
                                                }
                                                chosenToken = closestBaseToken ?: baseMovable.firstOrNull()
                                            } else {
                                                // Check proximity to base sockets even if tapped slightly outside
                                                for (token in baseMovable) {
                                                    val slotCenter = getTokenDisplayCenter(token, allTokens, cellSize)
                                                    val dist = hypot(tapOffset.x - slotCenter.x, tapOffset.y - slotCenter.y)
                                                    if (dist <= cellSize * 1.35f && dist < closestDist) {
                                                        closestDist = dist
                                                        chosenToken = token
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    chosenToken?.let { currentOnTokenClicked(it.id) }
                                }
                            }
                        }
                    }
            ) {
                // 1. Board Background & Deep Saturated Home Quadrants
                drawBoardBackground(cellSize, gameState.players)

                // 2. Center Home Triangles
                drawHomeTriangles(cellSize)

                // 3. Track Cells, Start Arrows, and Safe Stars
                drawTrackCells(cellSize, gameState.boardTheme)

                // 4. Glossy 3D Tokens
                drawTokens(gameState, cellSize, pulseScale)
            }

            // Capture Emoji Animation Overlay
            gameState.captureEvent?.let { event ->
                CaptureAnimationOverlay(
                    event = event,
                    cellSize = cellSize,
                    boardPixelSize = boardPixelSize
                )
            }

            // Talking Token Speech Bubble Overlay
            gameState.talkingTokenEvent?.let { talkEvent ->
                val talkPlayer = gameState.players.find { it.color == talkEvent.color }
                val talkToken = talkPlayer?.tokens?.find { it.id == talkEvent.tokenId }
                if (talkToken != null) {
                    val allTokens = gameState.players.flatMap { it.tokens }
                    val tokenCenter = getTokenDisplayCenter(talkToken, allTokens, cellSize)
                    TalkingTokenBubbleOverlay(
                        event = talkEvent,
                        tokenCenter = tokenCenter,
                        cellSize = cellSize
                    )
                }
            }
        }
    }
}

@Composable
private fun TalkingTokenBubbleOverlay(
    event: TalkingTokenEvent,
    tokenCenter: Offset,
    cellSize: Float
) {
    val bubbleWidthPx = cellSize * 2.8f
    val bubbleHeightPx = cellSize * 0.95f
    val bubbleX = (tokenCenter.x - bubbleWidthPx / 2f).coerceAtLeast(4f)
    val bubbleY = (tokenCenter.y - cellSize * 1.55f).coerceAtLeast(4f)

    Box(
        modifier = Modifier
            .offset { IntOffset(bubbleX.toInt(), bubbleY.toInt()) }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xF21E0C04))
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(Color(0xFFFDE047), event.color.primaryColor, Color(0xFFFFD700))),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = event.emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = event.phrase,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Board Background & Corner Yards
// -----------------------------------------------------------------------------

private fun DrawScope.drawBoardBackground(cellSize: Float, players: List<Player>) {
    // Crisp light porcelain board background matching reference photo
    drawRect(color = Color(0xFFF1F5F9), size = size)

    val yardSize = cellSize * 6

    val greenPlayer = players.find { it.color == PlayerColor.GREEN }
    val yellowPlayer = players.find { it.color == PlayerColor.YELLOW }
    val bluePlayer = players.find { it.color == PlayerColor.BLUE }
    val redPlayer = players.find { it.color == PlayerColor.RED }

    // 1. Emerald Green Home Quadrant (Top-Left)
    drawRect(
        color = Color(0xFF0F9D58),
        topLeft = Offset(0f, 0f),
        size = Size(yardSize, yardSize)
    )
    drawYardBasePlate(
        topLeft = Offset(cellSize * 0.75f, cellSize * 0.75f),
        size = Size(cellSize * 4.5f, cellSize * 4.5f),
        baseColor = Color(0xFF0F9D58),
        recessColor = Color(0xFF0B8043),
        socketColor = Color(0xFF075B30),
        cellSize = cellSize,
        slotOffsets = LudoBoardMap.BASE_SLOT_POINTS[PlayerColor.GREEN]!!.map { Offset(it.x * cellSize, it.y * cellSize) },
        playerName = greenPlayer?.name ?: "Green",
        winChanceDisplay = greenPlayer?.formattedWinChance ?: "0%"
    )

    // 2. Golden Yellow Home Quadrant (Top-Right)
    drawRect(
        color = Color(0xFFF4B400),
        topLeft = Offset(cellSize * 9f, 0f),
        size = Size(yardSize, yardSize)
    )
    drawYardBasePlate(
        topLeft = Offset(cellSize * 9.75f, cellSize * 0.75f),
        size = Size(cellSize * 4.5f, cellSize * 4.5f),
        baseColor = Color(0xFFF4B400),
        recessColor = Color(0xFFD89B00),
        socketColor = Color(0xFF9E6E00),
        cellSize = cellSize,
        slotOffsets = LudoBoardMap.BASE_SLOT_POINTS[PlayerColor.YELLOW]!!.map { Offset(it.x * cellSize, it.y * cellSize) },
        playerName = yellowPlayer?.name ?: "Yellow",
        winChanceDisplay = yellowPlayer?.formattedWinChance ?: "0%"
    )

    // 3. Sapphire Blue Home Quadrant (Bottom-Right)
    drawRect(
        color = Color(0xFF1A73E8),
        topLeft = Offset(cellSize * 9f, cellSize * 9f),
        size = Size(yardSize, yardSize)
    )
    drawYardBasePlate(
        topLeft = Offset(cellSize * 9.75f, cellSize * 9.75f),
        size = Size(cellSize * 4.5f, cellSize * 4.5f),
        baseColor = Color(0xFF1A73E8),
        recessColor = Color(0xFF1557B0),
        socketColor = Color(0xFF0D3E7D),
        cellSize = cellSize,
        slotOffsets = LudoBoardMap.BASE_SLOT_POINTS[PlayerColor.BLUE]!!.map { Offset(it.x * cellSize, it.y * cellSize) },
        playerName = bluePlayer?.name ?: "Blue",
        winChanceDisplay = bluePlayer?.formattedWinChance ?: "0%"
    )

    // 4. Royal Red Home Quadrant (Bottom-Left)
    drawRect(
        color = Color(0xFFDB4437),
        topLeft = Offset(0f, cellSize * 9f),
        size = Size(yardSize, yardSize)
    )
    drawYardBasePlate(
        topLeft = Offset(cellSize * 0.75f, cellSize * 9.75f),
        size = Size(cellSize * 4.5f, cellSize * 4.5f),
        baseColor = Color(0xFFDB4437),
        recessColor = Color(0xFFB32D23),
        socketColor = Color(0xFF7E1B13),
        cellSize = cellSize,
        slotOffsets = LudoBoardMap.BASE_SLOT_POINTS[PlayerColor.RED]!!.map { Offset(it.x * cellSize, it.y * cellSize) },
        playerName = redPlayer?.name ?: "Red",
        winChanceDisplay = redPlayer?.formattedWinChance ?: "0%"
    )
}

/**
 * Draws the monochromatic recessed plate with 4 circular socket depressions,
 * exactly matching the reference photo.
 */
private fun DrawScope.drawYardBasePlate(
    topLeft: Offset,
    size: Size,
    baseColor: Color,
    recessColor: Color,
    socketColor: Color,
    cellSize: Float,
    slotOffsets: List<Offset>,
    playerName: String,
    winChanceDisplay: String
) {
    val cornerRadius = CornerRadius(cellSize * 0.65f, cellSize * 0.65f)

    // Inner recessed plate with soft bevel
    drawRoundRect(
        color = recessColor,
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius
    )
    // Darker inner border shadow
    drawRoundRect(
        color = socketColor.copy(alpha = 0.55f),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = cellSize * 0.045f)
    )

    // 4 Sockets (Recessed circular depressions matching reference photo)
    for (slot in slotOffsets) {
        val socketRadius = cellSize * 0.48f

        // Soft outer shadow
        drawCircle(
            color = Color(0x35000000),
            radius = socketRadius + 1.5f,
            center = Offset(slot.x + 1f, slot.y + 1.5f)
        )

        // Sunken socket fill
        drawCircle(
            color = socketColor,
            radius = socketRadius,
            center = slot
        )

        // Inner upper shadow for realistic depth
        drawCircle(
            color = Color(0x40000000),
            radius = socketRadius - cellSize * 0.04f,
            center = slot,
            style = Stroke(width = cellSize * 0.05f)
        )

        // Lower subtle rim highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = socketRadius,
            center = slot,
            style = Stroke(width = cellSize * 0.025f)
        )
    }

    // Direct clean white text labels matching reference photo exactly
    val centerX = topLeft.x + size.width / 2f

    // Top: Player Name in bold clean white text
    val namePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = cellSize * 0.38f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(140, 0, 0, 0))
    }
    drawContext.canvas.nativeCanvas.drawText(
        playerName.take(10),
        centerX,
        topLeft.y + cellSize * 0.46f,
        namePaint
    )

    // Bottom: Win percentage in bold clean white text
    val winPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = cellSize * 0.36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(140, 0, 0, 0))
    }
    drawContext.canvas.nativeCanvas.drawText(
        winChanceDisplay,
        centerX,
        topLeft.y + size.height - cellSize * 0.16f,
        winPaint
    )
}

// -----------------------------------------------------------------------------
// Center Home Triangles & Golden Crown Medallion
// -----------------------------------------------------------------------------

private fun DrawScope.drawHomeTriangles(cellSize: Float) {
    val center = Offset(cellSize * 7.5f, cellSize * 7.5f)
    val tl = Offset(cellSize * 6f, cellSize * 6f)
    val tr = Offset(cellSize * 9f, cellSize * 6f)
    val br = Offset(cellSize * 9f, cellSize * 9f)
    val bl = Offset(cellSize * 6f, cellSize * 9f)

    // Top Triangle (Yellow, entering from top arm)
    drawTriangleWithGradient(
        p1 = tl, p2 = tr, p3 = center,
        cStart = Color(0xFFFEF08A), cMid = Color(0xFFFACC15), cEnd = Color(0xFF9A3412)
    )

    // Right Triangle (Blue, entering from right arm)
    drawTriangleWithGradient(
        p1 = tr, p2 = br, p3 = center,
        cStart = Color(0xFF60A5FA), cMid = Color(0xFF2563EB), cEnd = Color(0xFF1E3A8A)
    )

    // Bottom Triangle (Red, entering from bottom arm)
    drawTriangleWithGradient(
        p1 = bl, p2 = br, p3 = center,
        cStart = Color(0xFFFB7185), cMid = Color(0xFFDC2626), cEnd = Color(0xFF881337)
    )

    // Left Triangle (Green, entering from left arm)
    drawTriangleWithGradient(
        p1 = tl, p2 = bl, p3 = center,
        cStart = Color(0xFF34D399), cMid = Color(0xFF059669), cEnd = Color(0xFF064E3B)
    )

    // Clean dividing seams between the four center triangles meeting directly at center
    drawLine(Color.White.copy(alpha = 0.85f), tl, br, strokeWidth = cellSize * 0.05f)
    drawLine(Color.White.copy(alpha = 0.85f), tr, bl, strokeWidth = cellSize * 0.05f)
}

private fun DrawScope.drawTriangleWithGradient(p1: Offset, p2: Offset, p3: Offset, cStart: Color, cMid: Color, cEnd: Color) {
    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(cStart, cMid, cEnd),
            center = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f),
            radius = (p1 - p3).getDistance() * 1.05f
        )
    )
}

// -----------------------------------------------------------------------------
// Track Cells, Start Arrows, and Safe Stars (Sharp, Clean Tiles Matching Reference Photo)
// -----------------------------------------------------------------------------

private fun DrawScope.drawTrackCells(cellSize: Float, boardTheme: com.example.model.BoardTheme) {
    val trackBorderColor = Color(0xFFCBD5E1)
    val trackStrokeWidth = cellSize * 0.040f

    // 1. Draw all 52 outer track cells
    for ((index, coord) in LudoBoardMap.OUTER_TRACK.withIndex()) {
        val rectTopLeft = Offset(coord.col * cellSize, coord.row * cellSize)
        val rectSize = Size(cellSize, cellSize)
        val center = Offset(rectTopLeft.x + cellSize / 2f, rectTopLeft.y + cellSize / 2f)

        // Starting squares get exact matching colored path fills, safe star squares soft tint, others clean light tile fill
        val cellColor = when (index) {
            0 -> Color(0xFFDB4437)  // Red start (6, 13)
            13 -> Color(0xFF0F9D58) // Green start (1, 6)
            26 -> Color(0xFFF4B400) // Yellow start (8, 1)
            39 -> Color(0xFF1A73E8) // Blue start (13, 8)
            8, 21, 34, 47 -> Color(0xFFFFFFFF)
            else -> Color(0xFFFFFFFF)
        }

        // Cell background fill
        drawRect(color = cellColor, topLeft = rectTopLeft, size = rectSize)

        // Colored entry cells (0, 13, 26, 39): Styled identically to the colored home-lane path squares
        if (index in listOf(0, 13, 26, 39)) {
            drawCircle(
                color = Color.White.copy(alpha = 0.30f),
                radius = cellSize * 0.28f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = cellSize * 0.28f,
                center = center,
                style = Stroke(cellSize * 0.035f)
            )
        }

        // Sharper, darker cell grid border
        drawRect(
            color = trackBorderColor,
            topLeft = rectTopLeft,
            size = rectSize,
            style = Stroke(width = trackStrokeWidth)
        )

        // Safe Star squares (indices 8, 21, 34, 47) matching reference photo
        if (index == 8 || index == 21 || index == 34 || index == 47) {
            val starColor = when (index) {
                8 -> Color(0xFF0F9D58)   // Green safe star
                21 -> Color(0xFFF4B400)  // Yellow safe star
                34 -> Color(0xFF1A73E8)  // Blue safe star
                47 -> Color(0xFFDB4437)  // Red safe star
                else -> Color(0xFFFBBF24)
            }
            // Crisp 5-point star with subtle shadow matching reference photo
            drawStar(center = Offset(center.x + 0.8f, center.y + 1f), radius = cellSize * 0.35f, color = Color(0x35000000))
            drawStar(center = center, radius = cellSize * 0.35f, color = starColor)
        }

        // Starting squares white star / chevron
        if (index in listOf(0, 13, 26, 39)) {
            drawStar(center = Offset(center.x + 0.8f, center.y + 1f), radius = cellSize * 0.32f, color = Color(0x35000000))
            drawStar(center = center, radius = cellSize * 0.32f, color = Color.White)
        }
    }

    // 2. Draw Home Lanes (5 cells per player with saturated tones and crisp borders)
    drawHomeLane(LudoBoardMap.RED_HOME_LANE, Color(0xFFDB4437), cellSize, trackBorderColor) // Red (Bottom Arm)
    drawHomeLane(LudoBoardMap.GREEN_HOME_LANE, Color(0xFF0F9D58), cellSize, trackBorderColor) // Green (Left Arm)
    drawHomeLane(LudoBoardMap.YELLOW_HOME_LANE, Color(0xFFF4B400), cellSize, trackBorderColor) // Yellow (Top Arm)
    drawHomeLane(LudoBoardMap.BLUE_HOME_LANE, Color(0xFF1A73E8), cellSize, trackBorderColor) // Blue (Right Arm)
}

private fun DrawScope.drawHomeLane(
    coords: List<GridCoord>,
    color: Color,
    cellSize: Float,
    strokeColor: Color
) {
    for (coord in coords) {
        val topLeft = Offset(coord.col * cellSize, coord.row * cellSize)
        val cellCenter = Offset(topLeft.x + cellSize / 2f, topLeft.y + cellSize / 2f)

        drawRect(color = color, topLeft = topLeft, size = Size(cellSize, cellSize))

        // Inner circular stepping guide on home lane
        drawCircle(
            color = Color.White.copy(alpha = 0.30f),
            radius = cellSize * 0.28f,
            center = cellCenter
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = cellSize * 0.28f,
            center = cellCenter,
            style = Stroke(cellSize * 0.035f)
        )

        drawRect(
            color = strokeColor,
            topLeft = topLeft,
            size = Size(cellSize, cellSize),
            style = Stroke(cellSize * 0.055f)
        )
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val innerRadius = radius * 0.42f
    val numPoints = 5

    for (i in 0 until numPoints * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = (i * Math.PI / numPoints) - (Math.PI / 2.0)
        val x = (center.x + r * cos(angle)).toFloat()
        val y = (center.y + r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color, style = Fill)
}

// -----------------------------------------------------------------------------
// 3D Glossy Tokens
// -----------------------------------------------------------------------------

internal fun getTokenDisplayCenter(token: Token, allTokens: List<Token>, cellSize: Float): Offset {
    val point = LudoBoardMap.getTokenCoordinate(token)
    val base = Offset(point.x * cellSize, point.y * cellSize)

    if (token.position is TokenPosition.OnBoard) {
        val sharingTokens = allTokens.filter { other ->
            other.position is TokenPosition.OnBoard && {
                val pt = LudoBoardMap.getTokenCoordinate(other)
                hypot(pt.x - point.x, pt.y - point.y) < 0.05f
            }()
        }

        if (sharingTokens.size > 1) {
            val idx = sharingTokens.indexOfFirst { it.color == token.color && it.id == token.id }
            if (idx >= 0) {
                val n = sharingTokens.size
                val angle = (idx * 2.0 * Math.PI / n) - (Math.PI / 2.0)
                val spreadRadius = cellSize * 0.18f
                return Offset(
                    base.x + (cos(angle) * spreadRadius).toFloat(),
                    base.y + (sin(angle) * spreadRadius).toFloat()
                )
            }
        }
    }
    return base
}

private fun DrawScope.drawTokens(gameState: GameState, cellSize: Float, pulseScale: Float) {
    val isSelecting = gameState.phase == TurnPhase.SELECTING_TOKEN
    val movableIds = gameState.movableTokenIds
    val curPlayerColor = gameState.currentPlayer?.color
    val allTokens = gameState.players.flatMap { it.tokens }

    for (player in gameState.players) {
        for (token in player.tokens) {
            val center = getTokenDisplayCenter(token, allTokens, cellSize)
            val isMovable = isSelecting && (player.color == curPlayerColor) && movableIds.contains(token.id)

            val sharingCount = if (token.position is TokenPosition.OnBoard) {
                allTokens.count {
                    it.position is TokenPosition.OnBoard &&
                    hypot(LudoBoardMap.getTokenCoordinate(it).x - LudoBoardMap.getTokenCoordinate(token).x,
                          LudoBoardMap.getTokenCoordinate(it).y - LudoBoardMap.getTokenCoordinate(token).y) < 0.05f
                }
            } else 1

            val baseRadius = if (sharingCount > 1) cellSize * 0.29f else cellSize * 0.39f
            val tokenRadius = if (isMovable) baseRadius * pulseScale else baseRadius

            // Draw glowing highlight ring if token can move
            if (isMovable) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFBBF24).copy(alpha = 0.55f), Color.Transparent),
                        center = center,
                        radius = tokenRadius * 1.7f
                    ),
                    radius = tokenRadius * 1.7f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = tokenRadius * 1.25f,
                    center = center,
                    style = Stroke(width = cellSize * 0.08f)
                )
            }

            // Motion trail when token is animating movement
            if (gameState.phase == TurnPhase.ANIMATING_MOVE && isMovable) {
                drawCircle(
                    color = player.color.primaryColor.copy(alpha = 0.28f),
                    radius = tokenRadius * 1.55f,
                    center = Offset(center.x, center.y + cellSize * 0.15f)
                )
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.35f),
                    radius = tokenRadius * 1.30f,
                    center = Offset(center.x, center.y + cellSize * 0.08f)
                )
            }

            // Home path entrance and home arrival celebration glow
            if (token.position is TokenPosition.OnBoard && (token.position as TokenPosition.OnBoard).step >= 51) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD700).copy(alpha = 0.45f), Color.Transparent),
                        center = center,
                        radius = tokenRadius * 1.8f
                    ),
                    radius = tokenRadius * 1.8f,
                    center = center
                )
            } else if (token.isFinished) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFDE047).copy(alpha = 0.55f), Color(0xFFF59E0B).copy(alpha = 0.25f), Color.Transparent),
                        center = center,
                        radius = tokenRadius * 1.9f
                    ),
                    radius = tokenRadius * 1.9f,
                    center = center
                )
            }

            // --- 3D Pawn Rendering ---
            val skin = gameState.currentSkin
            val (bodyLight, bodyPrimary, bodyDark) = when (skin) {
                com.example.model.GameSkin.CLASSIC -> Triple(player.color.lightColor, player.color.primaryColor, player.color.darkColor)
                com.example.model.GameSkin.RUBY -> Triple(Color(0xFFFDA4AF), Color(0xFFE11D48), Color(0xFF881337))
                com.example.model.GameSkin.EMERALD -> Triple(Color(0xFFA7F3D0), Color(0xFF059669), Color(0xFF064E3B))
                com.example.model.GameSkin.SAPPHIRE -> Triple(Color(0xFFBAE6FD), Color(0xFF2563EB), Color(0xFF1E3A8A))
                com.example.model.GameSkin.GOLD -> Triple(Color(0xFFFEF08A), Color(0xFFEAB308), Color(0xFF78350F))
                com.example.model.GameSkin.SILVER -> Triple(Color(0xFFF8FAFC), Color(0xFF94A3B8), Color(0xFF334155))
                com.example.model.GameSkin.DIAMOND -> Triple(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF0284C7))
                com.example.model.GameSkin.FIRE -> Triple(Color(0xFFFDE047), Color(0xFFEA580C), Color(0xFF9A3412))
                com.example.model.GameSkin.ICE -> Triple(Color(0xFFCFFAFE), Color(0xFF06B6D4), Color(0xFF164E63))
                com.example.model.GameSkin.ROYAL_GOLD -> Triple(Color(0xFFFFFBEB), Color(0xFFF59E0B), Color(0xFF78350F))
                com.example.model.GameSkin.RAINBOW -> Triple(Color(0xFFF472B6), Color(0xFFA855F7), Color(0xFF4338CA))
            }

            // 1. Realistic Directional Drop Shadow
            drawOval(
                color = Color(0x35000000),
                topLeft = Offset(center.x - tokenRadius * 0.85f + 1.5f, center.y + tokenRadius * 0.22f + 2f),
                size = Size(tokenRadius * 1.7f, tokenRadius * 0.80f)
            )
            // Darker contact shadow
            drawOval(
                color = Color(0x45000000),
                topLeft = Offset(center.x - tokenRadius * 0.65f, center.y + tokenRadius * 0.30f),
                size = Size(tokenRadius * 1.3f, tokenRadius * 0.55f)
            )

            // 2. Bell Skirt / Pedestal (Glossy curved base of the pawn)
            val bellTop = center.y - tokenRadius * 0.12f
            val bellHeight = tokenRadius * 0.95f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bodyLight,
                        bodyPrimary,
                        bodyDark
                    ),
                    center = Offset(center.x - tokenRadius * 0.35f, bellTop + bellHeight * 0.25f),
                    radius = tokenRadius * 1.2f
                ),
                topLeft = Offset(center.x - tokenRadius * 0.82f, bellTop),
                size = Size(tokenRadius * 1.64f, bellHeight)
            )
            // Base player-color rim identification stroke
            drawOval(
                color = player.color.primaryColor,
                topLeft = Offset(center.x - tokenRadius * 0.82f, bellTop),
                size = Size(tokenRadius * 1.64f, bellHeight),
                style = Stroke(width = cellSize * 0.04f)
            )

            // 3. Collar neck ring (Player identification team band)
            val collarCenter = Offset(center.x, center.y - tokenRadius * 0.18f)
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        player.color.primaryColor,
                        player.color.darkColor
                    ),
                    start = Offset(collarCenter.x - tokenRadius * 0.45f, collarCenter.y),
                    end = Offset(collarCenter.x + tokenRadius * 0.45f, collarCenter.y)
                ),
                topLeft = Offset(collarCenter.x - tokenRadius * 0.48f, collarCenter.y - tokenRadius * 0.16f),
                size = Size(tokenRadius * 0.96f, tokenRadius * 0.32f)
            )

            // 4. Glossy Spherical 3D Crown / Head
            val headCenter = Offset(center.x, center.y - tokenRadius * 0.44f)
            val headRadius = tokenRadius * 0.60f

            // Head shadow cast onto collar
            drawCircle(
                color = Color(0x35000000),
                radius = headRadius * 1.05f,
                center = Offset(headCenter.x, headCenter.y + 2f)
            )

            // Glossy 3D Head Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        bodyLight,
                        bodyPrimary,
                        bodyDark
                    ),
                    center = Offset(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.35f),
                    radius = headRadius * 1.3f
                ),
                radius = headRadius,
                center = headCenter
            )

            // Outer sphere definition stroke
            drawCircle(
                color = Color.White.copy(alpha = 0.30f),
                radius = headRadius,
                center = headCenter,
                style = Stroke(width = cellSize * 0.03f)
            )

            // Crisp White Specular Reflection Glint
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = headRadius * 0.25f,
                center = Offset(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.35f)
            )

            // Soft secondary bounce ambient reflection on bottom-right rim
            drawCircle(
                color = player.color.lightColor.copy(alpha = 0.35f),
                radius = headRadius * 0.30f,
                center = Offset(headCenter.x + headRadius * 0.38f, headCenter.y + headRadius * 0.38f)
            )
        }
    }
}
