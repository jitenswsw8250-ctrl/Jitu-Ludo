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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
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
        }
    }
}

// -----------------------------------------------------------------------------
// Board Background & Corner Yards
// -----------------------------------------------------------------------------

private fun DrawScope.drawBoardBackground(cellSize: Float, players: List<Player>) {
    // Crisp porcelain white board background
    drawRect(color = Color(0xFFFAFAFA), size = size)

    val yardSize = cellSize * 6

    val greenPlayer = players.find { it.color == PlayerColor.GREEN }
    val yellowPlayer = players.find { it.color == PlayerColor.YELLOW }
    val bluePlayer = players.find { it.color == PlayerColor.BLUE }
    val redPlayer = players.find { it.color == PlayerColor.RED }

    // 1. Emerald Green Home Quadrant (Top-Left)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF10B981), // Vibrant emerald highlight
                Color(0xFF059669), // Classic rich emerald
                Color(0xFF047857), // Deep emerald
                Color(0xFF064E3B)  // Dark forest emerald shadow
            ),
            center = Offset(yardSize * 0.40f, yardSize * 0.40f),
            radius = yardSize * 0.90f
        ),
        topLeft = Offset(0f, 0f),
        size = Size(yardSize, yardSize)
    )
    // Subtle golden bevel border on home quadrant edge
    drawRect(
        color = Color(0xFFFBBF24).copy(alpha = 0.35f),
        topLeft = Offset(0f, 0f),
        size = Size(yardSize, yardSize),
        style = Stroke(width = cellSize * 0.06f)
    )
    drawYardBasePlate(
        topLeft = Offset(cellSize * 0.8f, cellSize * 0.8f),
        size = Size(cellSize * 4.4f, cellSize * 4.4f),
        accentColor = Color(0xFF059669),
        cellSize = cellSize,
        slotOffsets = LudoBoardMap.BASE_SLOT_POINTS[PlayerColor.GREEN]!!.map { Offset(it.x * cellSize, it.y * cellSize) },
        playerName = greenPlayer?.name,
        playerAvatar = greenPlayer?.avatar?.emoji,
        winChanceDisplay = greenPlayer?.formattedWinChance
    )

    // 2. Golden Yellow Home Quadrant (Top-Right)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFEF08A), // Sunny light gold highlight
                Color(0xFFFACC15), // Vibrant golden yellow
                Color(0xFFEAB308), // Rich deep amber
                Color(0xFF9A3412)  // Burnished bronze shadow
            ),
            center = Offset(cellSize * 12f, cellSize * 3f),
            radius = yardSize * 0.90f
        ),
        topLeft = Offset(cellSize * 9f, 0f),
        size = Size(yardSize, yardSize)
    )
    drawRect(
        color = Color(0xFFFBBF24).copy(alpha = 0.35f),
        topLeft = Offset(cellSize * 9f, 0f),
        size = Size(yardSize, yardSize),
        style = Stroke(width = cellSize * 0.06f)
    )
    drawYardBasePlate(
        topLeft = Offset(cellSize * 9.8f, cellSize * 0.8f),
        size = Size(cellSize * 4.4f, cellSize * 4.4f),
        accentColor = Color(0xFFD97706),
        cellSize = cellSize,
        slotOffsets = LudoBoardMap.BASE_SLOT_POINTS[PlayerColor.YELLOW]!!.map { Offset(it.x * cellSize, it.y * cellSize) },
        playerName = yellowPlayer?.name,
        playerAvatar = yellowPlayer?.avatar?.emoji,
        winChanceDisplay = yellowPlayer?.formattedWinChance
    )

    // 3. Sapphire Blue Home Quadrant (Bottom-Right)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF60A5FA), // Vivid sky highlight
                Color(0xFF2563EB), // Rich royal sapphire
                Color(0xFF1D4ED8), // Deep cobalt
                Color(0xFF1E3A8A)  // Deep navy shadow
            ),
            center = Offset(cellSize * 12f, cellSize * 12f),
            radius = yardSize * 0.90f
        ),
        topLeft = Offset(cellSize * 9f, cellSize * 9f),
        size = Size(yardSize, yardSize)
    )
    drawRect(
        color = Color(0xFFFBBF24).copy(alpha = 0.35f),
        topLeft = Offset(cellSize * 9f, cellSize * 9f),
        size = Size(yardSize, yardSize),
        style = Stroke(width = cellSize * 0.06f)
    )
    drawYardBasePlate(
        topLeft = Offset(cellSize * 9.8f, cellSize * 9.8f),
        size = Size(cellSize * 4.4f, cellSize * 4.4f),
        accentColor = Color(0xFF2563EB),
        cellSize = cellSize,
        slotOffsets = LudoBoardMap.BASE_SLOT_POINTS[PlayerColor.BLUE]!!.map { Offset(it.x * cellSize, it.y * cellSize) },
        playerName = bluePlayer?.name,
        playerAvatar = bluePlayer?.avatar?.emoji,
        winChanceDisplay = bluePlayer?.formattedWinChance
    )

    // 4. Royal Red Home Quadrant (Bottom-Left)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFB7185), // Brilliant rose highlight
                Color(0xFFF43F5E), // Vivid ruby
                Color(0xFFDC2626), // Royal crimson
                Color(0xFF881337)  // Deep burgundy wine shadow
            ),
            center = Offset(cellSize * 3f, cellSize * 12f),
            radius = yardSize * 0.90f
        ),
        topLeft = Offset(0f, cellSize * 9f),
        size = Size(yardSize, yardSize)
    )
    drawRect(
        color = Color(0xFFFBBF24).copy(alpha = 0.35f),
        topLeft = Offset(0f, cellSize * 9f),
        size = Size(yardSize, yardSize),
        style = Stroke(width = cellSize * 0.06f)
    )
    drawYardBasePlate(
        topLeft = Offset(cellSize * 0.8f, cellSize * 9.8f),
        size = Size(cellSize * 4.4f, cellSize * 4.4f),
        accentColor = Color(0xFFDC2626),
        cellSize = cellSize,
        slotOffsets = LudoBoardMap.BASE_SLOT_POINTS[PlayerColor.RED]!!.map { Offset(it.x * cellSize, it.y * cellSize) },
        playerName = redPlayer?.name,
        playerAvatar = redPlayer?.avatar?.emoji,
        winChanceDisplay = redPlayer?.formattedWinChance
    )
}

/**
 * Draws the inner rounded white plate with 4 colored socket rings for the 2x2 token layout.
 */
private fun DrawScope.drawYardBasePlate(
    topLeft: Offset,
    size: Size,
    accentColor: Color,
    cellSize: Float,
    slotOffsets: List<Offset>,
    playerName: String? = null,
    playerAvatar: String? = null,
    winChanceDisplay: String? = null
) {
    val cornerRadius = CornerRadius(cellSize * 0.70f, cellSize * 0.70f)

    // 1. Realistic layered drop shadow under the base plate
    drawRoundRect(
        color = Color(0x35000000),
        topLeft = Offset(topLeft.x + 3.5f, topLeft.y + 5.0f),
        size = size,
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        color = Color(0x20000000),
        topLeft = Offset(topLeft.x + 1.5f, topLeft.y + 2.0f),
        size = size,
        cornerRadius = cornerRadius
    )

    // 2. Pure porcelain satin white plate with subtle reflection gradient
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFC),
                Color(0xFFEFF6FF)
            ),
            startY = topLeft.y,
            endY = topLeft.y + size.height
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius
    )

    // 3. Dual-layered polished plate border (outer slate + inner luxury gold hairline)
    drawRoundRect(
        color = Color(0xFFCBD5E1),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = cellSize * 0.050f)
    )
    drawRoundRect(
        color = Color(0xFFF59E0B).copy(alpha = 0.40f),
        topLeft = Offset(topLeft.x + 1f, topLeft.y + 1f),
        size = Size(size.width - 2f, size.height - 2f),
        cornerRadius = cornerRadius,
        style = Stroke(width = cellSize * 0.025f)
    )

    // 4. 4 Sockets (Recessed circular wells with 3D colored metallic rings)
    for (slot in slotOffsets) {
        val socketRadius = cellSize * 0.46f

        // Outer soft socket shadow
        drawCircle(
            color = Color(0x25000000),
            radius = socketRadius + 2f,
            center = slot
        )

        // Socket well recessed porcelain fill
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    accentColor.copy(alpha = 0.08f),
                    accentColor.copy(alpha = 0.20f)
                ),
                center = Offset(slot.x - socketRadius * 0.2f, slot.y - socketRadius * 0.2f),
                radius = socketRadius
            ),
            radius = socketRadius,
            center = slot
        )

        // 3D Metallic Colored Ring with specular glint
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.90f),
                    accentColor,
                    accentColor.copy(alpha = 0.85f),
                    Color(0xFF0F172A)
                ),
                start = Offset(slot.x - socketRadius, slot.y - socketRadius),
                end = Offset(slot.x + socketRadius, slot.y + socketRadius)
            ),
            radius = socketRadius,
            center = slot,
            style = Stroke(width = cellSize * 0.085f)
        )

        // Inner subtle rim shadow
        drawCircle(
            color = Color(0x20000000),
            radius = socketRadius - cellSize * 0.085f,
            center = slot,
            style = Stroke(width = cellSize * 0.025f)
        )
    }

    // Centered Player Name & Estimated Live Win Chance % Badges
    if (playerName != null && winChanceDisplay != null) {
        val centerX = topLeft.x + size.width / 2f

        // Top Player Name Pill Badge
        val nameBadgeWidth = cellSize * 2.85f
        val nameBadgeHeight = cellSize * 0.52f
        val nameBadgeX = centerX - nameBadgeWidth / 2f
        val nameBadgeY = topLeft.y + cellSize * 0.16f
        val nameCorner = CornerRadius(nameBadgeHeight / 2f, nameBadgeHeight / 2f)

        drawRoundRect(
            color = Color(0xEE090D16), // Dark obsidian glass
            topLeft = Offset(nameBadgeX, nameBadgeY),
            size = Size(nameBadgeWidth, nameBadgeHeight),
            cornerRadius = nameCorner
        )
        drawRoundRect(
            color = accentColor.copy(alpha = 0.90f),
            topLeft = Offset(nameBadgeX, nameBadgeY),
            size = Size(nameBadgeWidth, nameBadgeHeight),
            cornerRadius = nameCorner,
            style = Stroke(width = cellSize * 0.035f)
        )

        val namePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = cellSize * 0.32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val displayName = if (!playerAvatar.isNullOrEmpty()) "$playerAvatar ${playerName.take(8)}" else playerName.take(11)
        drawContext.canvas.nativeCanvas.drawText(
            displayName,
            centerX,
            nameBadgeY + nameBadgeHeight * 0.72f,
            namePaint
        )

        // Bottom Estimated Win Chance Pill Badge (High contrast, dark badge, bold golden text)
        val winBadgeWidth = cellSize * 2.75f
        val winBadgeHeight = cellSize * 0.52f
        val winBadgeX = centerX - winBadgeWidth / 2f
        val winBadgeY = topLeft.y + size.height - cellSize * 0.68f
        val winCorner = CornerRadius(winBadgeHeight / 2f, winBadgeHeight / 2f)

        drawRoundRect(
            color = Color(0xEE090D16), // Premium dark translucent backing
            topLeft = Offset(winBadgeX, winBadgeY),
            size = Size(winBadgeWidth, winBadgeHeight),
            cornerRadius = winCorner
        )
        drawRoundRect(
            color = Color(0xFFFBBF24).copy(alpha = 0.95f), // Golden amber outline
            topLeft = Offset(winBadgeX, winBadgeY),
            size = Size(winBadgeWidth, winBadgeHeight),
            cornerRadius = winCorner,
            style = Stroke(width = cellSize * 0.040f)
        )

        val winPaint = Paint().apply {
            color = android.graphics.Color.argb(255, 253, 224, 71) // Crisp vibrant golden yellow (#FDE047)
            textSize = cellSize * 0.33f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        drawContext.canvas.nativeCanvas.drawText(
            "Est. $winChanceDisplay",
            centerX,
            winBadgeY + winBadgeHeight * 0.72f,
            winPaint
        )
    }
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

    // Crisp 3D divider lines: dark shadow line underneath and crisp white highlight line on top
    drawLine(Color(0x55000000), Offset(tl.x + 1f, tl.y + 1f), Offset(br.x + 1f, br.y + 1f), strokeWidth = cellSize * 0.08f)
    drawLine(Color(0x55000000), Offset(tr.x - 1f, tr.y + 1f), Offset(bl.x - 1f, bl.y + 1f), strokeWidth = cellSize * 0.08f)
    drawLine(Color.White, tl, br, strokeWidth = cellSize * 0.065f)
    drawLine(Color.White, tr, bl, strokeWidth = cellSize * 0.065f)

    // Premium 3D Golden Crown Medallion
    val medallionRadius = cellSize * 0.62f
    // Soft outer shadow
    drawCircle(
        color = Color(0x45000000),
        radius = medallionRadius + 2.5f,
        center = Offset(center.x + 1f, center.y + 2.5f)
    )
    // Multi-tier golden gradient fill
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFBEB), // Brilliant gold apex
                Color(0xFFFDE047), // Sunny gold
                Color(0xFFF59E0B), // Warm rich amber
                Color(0xFFD97706), // Burnished gold
                Color(0xFF78350F)  // Dark bronze rim
            ),
            center = Offset(center.x - medallionRadius * 0.30f, center.y - medallionRadius * 0.30f),
            radius = medallionRadius * 1.35f
        ),
        radius = medallionRadius,
        center = center
    )
    // Polished outer gold rim stroke
    drawCircle(
        color = Color(0xFFB45309),
        radius = medallionRadius,
        center = center,
        style = Stroke(cellSize * 0.085f)
    )
    // Inner specular highlight ring
    drawCircle(
        color = Color.White.copy(alpha = 0.50f),
        radius = medallionRadius - cellSize * 0.085f,
        center = center,
        style = Stroke(cellSize * 0.025f)
    )
    // Embossed 3D Crown / Star in center with faceted golden shading
    drawStar(center = Offset(center.x + 1f, center.y + 1.5f), radius = cellSize * 0.40f, color = Color(0x60000000))
    drawStar(center = center, radius = cellSize * 0.40f, color = Color(0xFF78350F))
    drawStar(center = Offset(center.x - 0.5f, center.y - 0.5f), radius = cellSize * 0.36f, color = Color(0xFFFEF3C7))
    drawStar(center = center, radius = cellSize * 0.32f, color = Color(0xFFF59E0B))
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
// Track Cells, Start Arrows, and Safe Stars (Sharp, High-Contrast Polished Path)
// -----------------------------------------------------------------------------

private fun DrawScope.drawTrackCells(cellSize: Float, boardTheme: com.example.model.BoardTheme) {
    val trackBorderColor = boardTheme.trackCellBorder
    val trackStrokeWidth = cellSize * 0.055f

    // 1. Draw all 52 outer track cells
    for ((index, coord) in LudoBoardMap.OUTER_TRACK.withIndex()) {
        val rectTopLeft = Offset(coord.col * cellSize, coord.row * cellSize)
        val rectSize = Size(cellSize, cellSize)
        val center = Offset(rectTopLeft.x + cellSize / 2f, rectTopLeft.y + cellSize / 2f)

        // Starting squares get exact matching colored path fills, safe star squares soft violet, others clean crisp off-white or themed fill
        val cellColor = when (index) {
            0 -> Color(0xFFDC2626)  // Red start (6, 13) matching red home-lane path
            13 -> Color(0xFF059669) // Green start (1, 6) matching green home-lane path
            26 -> Color(0xFFD97706) // Yellow start (8, 1) matching yellow home-lane path
            39 -> Color(0xFF2563EB) // Blue start (13, 8) matching blue home-lane path
            8, 21, 34, 47 -> if (boardTheme == com.example.model.BoardTheme.NIGHT) Color(0xFF1E1B4B) else Color(0xFFEDE9FE)
            else -> boardTheme.trackCellFill
        }

        // Cell background fill
        drawRect(color = cellColor, topLeft = rectTopLeft, size = rectSize)

        // Prominent circular movement spaces / stepping stone rings inside regular white cells
        if (index !in listOf(0, 13, 26, 39, 8, 21, 34, 47)) {
            val waypointRadius = cellSize * 0.35f
            val ringColor = when (boardTheme) {
                com.example.model.BoardTheme.NIGHT -> Color(0xFF475569)
                com.example.model.BoardTheme.ROYAL_GOLD -> Color(0xFFB45309)
                else -> Color(0xFF94A3B8)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (boardTheme == com.example.model.BoardTheme.NIGHT) {
                        listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A))
                    } else if (boardTheme == com.example.model.BoardTheme.ROYAL_GOLD) {
                        listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFFDE68A))
                    } else {
                        listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFFE2E8F0))
                    },
                    center = center,
                    radius = waypointRadius
                ),
                radius = waypointRadius,
                center = center
            )
            drawCircle(
                color = ringColor,
                radius = waypointRadius,
                center = center,
                style = Stroke(width = cellSize * 0.045f)
            )
        }

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

        // Safe Star squares (indices 8, 21, 34, 47) with prominent circular golden medallion and colored star
        if (index == 8 || index == 21 || index == 34 || index == 47) {
            val starColor = when (index) {
                8 -> Color(0xFF059669)   // Green safe star
                21 -> Color(0xFFD97706)  // Yellow safe star
                34 -> Color(0xFF2563EB)  // Blue safe star
                47 -> Color(0xFFDC2626)  // Red safe star
                else -> Color(0xFF7C3AED)
            }
            // Circular star medallion backing with golden rim
            drawCircle(
                color = Color(0xFFDDD6FE),
                radius = cellSize * 0.38f,
                center = center
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFDE047), Color(0xFFD97706)),
                    start = Offset(center.x - cellSize * 0.38f, center.y - cellSize * 0.38f),
                    end = Offset(center.x + cellSize * 0.38f, center.y + cellSize * 0.38f)
                ),
                radius = cellSize * 0.38f,
                center = center,
                style = Stroke(width = cellSize * 0.055f)
            )
            // Drop shadow for the star
            drawStar(center = Offset(center.x + 1f, center.y + 1f), radius = cellSize * 0.32f, color = Color(0x35000000))
            drawStar(center = center, radius = cellSize * 0.32f, color = starColor)
        }
    }

    // 2. Draw Home Lanes (5 cells per player with saturated jewel tones and dark dividing borders)
    drawHomeLane(LudoBoardMap.RED_HOME_LANE, Color(0xFFDC2626), cellSize, trackBorderColor) // Red (Bottom Arm)
    drawHomeLane(LudoBoardMap.GREEN_HOME_LANE, Color(0xFF059669), cellSize, trackBorderColor) // Green (Left Arm)
    drawHomeLane(LudoBoardMap.YELLOW_HOME_LANE, Color(0xFFD97706), cellSize, trackBorderColor) // Yellow (Top Arm)
    drawHomeLane(LudoBoardMap.BLUE_HOME_LANE, Color(0xFF2563EB), cellSize, trackBorderColor) // Blue (Right Arm)
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

            // --- 3D Pawn Rendering ---
            val skin = gameState.currentSkin
            val (bodyLight, bodyPrimary, bodyDark) = when (skin) {
                com.example.model.GameSkin.CLASSIC -> Triple(player.color.lightColor, player.color.primaryColor, player.color.darkColor)
                com.example.model.GameSkin.RUBY -> Triple(Color(0xFFFDA4AF), Color(0xFFE11D48), Color(0xFF881337))
                com.example.model.GameSkin.EMERALD -> Triple(Color(0xFFA7F3D0), Color(0xFF059669), Color(0xFF064E3B))
                com.example.model.GameSkin.SAPPHIRE -> Triple(Color(0xFFBAE6FD), Color(0xFF2563EB), Color(0xFF1E3A8A))
                com.example.model.GameSkin.GOLD -> Triple(Color(0xFFFEF08A), Color(0xFFEAB308), Color(0xFF78350F))
                com.example.model.GameSkin.SILVER -> Triple(Color(0xFFF8FAFC), Color(0xFF94A3B8), Color(0xFF334155))
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
