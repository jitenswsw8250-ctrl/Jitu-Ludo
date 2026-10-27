package com.example.logic

import com.example.model.PlayerColor
import com.example.model.Token
import com.example.model.TokenPosition

data class GridCoord(val row: Int, val col: Int)

data class FloatPoint(val x: Float, val y: Float)

object LudoBoardMap {
    const val GRID_SIZE = 15
    const val TRACK_LENGTH = 52
    const val MAX_STEPS = 56 // 0..50 outer, 51..55 home lane, 56 finished

    // Safe track indices (0-indexed on 0..51 outer track)
    val SAFE_TRACK_INDICES = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    // Outer 52-cell track coordinates clockwise starting from Red's start square (row 13, col 6)
    val OUTER_TRACK: List<GridCoord> = listOf(
        // Bottom arm left col moving up (0..4)
        GridCoord(13, 6), // 0: Red Start (Safe)
        GridCoord(12, 6), // 1
        GridCoord(11, 6), // 2
        GridCoord(10, 6), // 3
        GridCoord(9, 6),  // 4
        // Left arm bottom row moving left (5..10)
        GridCoord(8, 5),  // 5
        GridCoord(8, 4),  // 6
        GridCoord(8, 3),  // 7
        GridCoord(8, 2),  // 8: Star Safe Square
        GridCoord(8, 1),  // 9
        GridCoord(8, 0),  // 10
        // Left arm left tip (11)
        GridCoord(7, 0),  // 11
        // Left arm top row moving right (12..17)
        GridCoord(6, 0),  // 12
        GridCoord(6, 1),  // 13: Green Start (Safe)
        GridCoord(6, 2),  // 14
        GridCoord(6, 3),  // 15
        GridCoord(6, 4),  // 16
        GridCoord(6, 5),  // 17
        // Top arm left col moving up (18..23)
        GridCoord(5, 6),  // 18
        GridCoord(4, 6),  // 19
        GridCoord(3, 6),  // 20
        GridCoord(2, 6),  // 21: Star Safe Square
        GridCoord(1, 6),  // 22
        GridCoord(0, 6),  // 23
        // Top arm top tip (24)
        GridCoord(0, 7),  // 24
        // Top arm right col moving down (25..30)
        GridCoord(0, 8),  // 25
        GridCoord(1, 8),  // 26: Yellow Start (Safe)
        GridCoord(2, 8),  // 27
        GridCoord(3, 8),  // 28
        GridCoord(4, 8),  // 29
        GridCoord(5, 8),  // 30
        // Right arm top row moving right (31..36)
        GridCoord(6, 9),  // 31
        GridCoord(6, 10), // 32
        GridCoord(6, 11), // 33
        GridCoord(6, 12), // 34: Star Safe Square
        GridCoord(6, 13), // 35
        GridCoord(6, 14), // 36
        // Right arm right tip (37)
        GridCoord(7, 14), // 37
        // Right arm bottom row moving left (38..43)
        GridCoord(8, 14), // 38
        GridCoord(8, 13), // 39: Blue Start (Safe)
        GridCoord(8, 12), // 40
        GridCoord(8, 11), // 41
        GridCoord(8, 10), // 42
        GridCoord(8, 9),  // 43
        // Bottom arm right col moving down (44..49)
        GridCoord(9, 8),  // 44
        GridCoord(10, 8), // 45
        GridCoord(11, 8), // 46
        GridCoord(12, 8), // 47: Star Safe Square
        GridCoord(13, 8), // 48
        GridCoord(14, 8), // 49
        // Bottom arm bottom tip (50)
        GridCoord(14, 7), // 50: Entrance to Red Home Lane
        // Bottom arm bottom-left corner (51)
        GridCoord(14, 6)  // 51
    )

    // Home lanes for each player (steps 51..55)
    private val RED_HOME_LANE = listOf(
        GridCoord(13, 7),
        GridCoord(12, 7),
        GridCoord(11, 7),
        GridCoord(10, 7),
        GridCoord(9, 7)
    )

    private val GREEN_HOME_LANE = listOf(
        GridCoord(7, 1),
        GridCoord(7, 2),
        GridCoord(7, 3),
        GridCoord(7, 4),
        GridCoord(7, 5)
    )

    private val YELLOW_HOME_LANE = listOf(
        GridCoord(1, 7),
        GridCoord(2, 7),
        GridCoord(3, 7),
        GridCoord(4, 7),
        GridCoord(5, 7)
    )

    private val BLUE_HOME_LANE = listOf(
        GridCoord(7, 13),
        GridCoord(7, 12),
        GridCoord(7, 11),
        GridCoord(7, 10),
        GridCoord(7, 9)
    )

    // Geometric center of each colored home area (cols 0..15, rows 0..15)
    val BASE_CENTERS = mapOf(
        PlayerColor.GREEN to FloatPoint(3.0f, 3.0f),
        PlayerColor.YELLOW to FloatPoint(12.0f, 3.0f),
        PlayerColor.RED to FloatPoint(3.0f, 12.0f),
        PlayerColor.BLUE to FloatPoint(12.0f, 12.0f)
    )

    // Base yard slots (x, y) for 4 tokens matching the 4-socket layout in the reference photo
    val BASE_SLOT_POINTS = mapOf(
        PlayerColor.GREEN to listOf(
            FloatPoint(2.15f, 2.15f), // Top-Left
            FloatPoint(3.85f, 2.15f), // Top-Right
            FloatPoint(2.15f, 3.85f), // Bottom-Left
            FloatPoint(3.85f, 3.85f)  // Bottom-Right
        ),
        PlayerColor.YELLOW to listOf(
            FloatPoint(11.15f, 2.15f), // Top-Left
            FloatPoint(12.85f, 2.15f), // Top-Right
            FloatPoint(11.15f, 3.85f), // Bottom-Left
            FloatPoint(12.85f, 3.85f)  // Bottom-Right
        ),
        PlayerColor.RED to listOf(
            FloatPoint(2.15f, 11.15f), // Top-Left
            FloatPoint(3.85f, 11.15f), // Top-Right
            FloatPoint(2.15f, 12.85f), // Bottom-Left
            FloatPoint(3.85f, 12.85f)  // Bottom-Right
        ),
        PlayerColor.BLUE to listOf(
            FloatPoint(11.15f, 11.15f), // Top-Left
            FloatPoint(12.85f, 11.15f), // Top-Right
            FloatPoint(11.15f, 12.85f), // Bottom-Left
            FloatPoint(12.85f, 12.85f)  // Bottom-Right
        )
    )

    // Home triangle target centers (row, col floating)
    val HOME_CENTERS = mapOf(
        PlayerColor.RED to FloatPoint(7.5f, 8.4f),     // Bottom triangle
        PlayerColor.GREEN to FloatPoint(6.6f, 7.5f),   // Left triangle
        PlayerColor.YELLOW to FloatPoint(7.5f, 6.6f),  // Top triangle
        PlayerColor.BLUE to FloatPoint(8.4f, 7.5f)     // Right triangle
    )

    /**
     * Converts a token's current state to its board cell or center coordinates.
     */
    fun getTokenCoordinate(token: Token): FloatPoint {
        return when (val pos = token.position) {
            is TokenPosition.InBase -> {
                getBaseSlotCoordinate(token.color, token.id)
            }
            is TokenPosition.OnBoard -> {
                val step = pos.step
                if (step < 51) {
                    val trackIndex = (token.color.startTrackIndex + step) % TRACK_LENGTH
                    val coord = OUTER_TRACK[trackIndex]
                    FloatPoint(coord.col + 0.5f, coord.row + 0.5f)
                } else if (step in 51..55) {
                    val homeIndex = step - 51
                    val lane = when (token.color) {
                        PlayerColor.RED -> RED_HOME_LANE
                        PlayerColor.GREEN -> GREEN_HOME_LANE
                        PlayerColor.YELLOW -> YELLOW_HOME_LANE
                        PlayerColor.BLUE -> BLUE_HOME_LANE
                    }
                    val coord = lane[homeIndex]
                    FloatPoint(coord.col + 0.5f, coord.row + 0.5f)
                } else {
                    HOME_CENTERS[token.color] ?: FloatPoint(7.5f, 7.5f)
                }
            }
            is TokenPosition.Finished -> {
                // Offset multiple finished tokens slightly in the home triangle
                val center = HOME_CENTERS[token.color] ?: FloatPoint(7.5f, 7.5f)
                val angle = (token.id * 90.0) * (Math.PI / 180.0)
                val offset = 0.22f
                FloatPoint(
                    (center.x + Math.cos(angle) * offset).toFloat(),
                    (center.y + Math.sin(angle) * offset).toFloat()
                )
            }
        }
    }

    /**
     * Checks if a step on a player's track lands on a global safe square.
     */
    fun isSafeSquare(color: PlayerColor, step: Int): Boolean {
        if (step in 51..56) return true // Home lane & Goal are always safe
        val trackIndex = (color.startTrackIndex + step) % TRACK_LENGTH
        return SAFE_TRACK_INDICES.contains(trackIndex)
    }

    /**
     * Returns track index (0..51) for a token on the common track, or null if in base/home-lane.
     */
    fun getTrackIndex(color: PlayerColor, step: Int): Int? {
        return if (step < 51) {
            (color.startTrackIndex + step) % TRACK_LENGTH
        } else {
            null
        }
    }

    /**
     * Returns floating center coordinates of a player's base yard slot.
     */
    fun getBaseSlotCoordinate(color: PlayerColor, tokenId: Int): FloatPoint {
        val slots = BASE_SLOT_POINTS[color] ?: emptyList()
        return slots.getOrElse(tokenId) { BASE_CENTERS[color] ?: FloatPoint(0f, 0f) }
    }
}
