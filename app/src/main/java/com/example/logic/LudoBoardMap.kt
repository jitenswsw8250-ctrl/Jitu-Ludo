package com.example.logic

import com.example.model.PlayerColor
import com.example.model.Token
import com.example.model.TokenPosition

data class FloatPoint(val x: Float, val y: Float)

data class GridCoord(val col: Int, val row: Int)

object LudoBoardMap {
    const val GRID_SIZE: Int = 15
    const val TRACK_LENGTH: Int = 52
    const val MAX_STEPS: Int = 56

    val SAFE_TRACK_INDICES: Set<Int> = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    // Canonical 52-cell outer track in clockwise order [col, row]
    val OUTER_TRACK: List<GridCoord> = listOf(
        GridCoord(6, 13), // 0 - Red start (Bottom Arm, Left Lane, moving UP)
        GridCoord(6, 12), // 1
        GridCoord(6, 11), // 2
        GridCoord(6, 10), // 3
        GridCoord(6, 9),  // 4
        GridCoord(5, 8),  // 5
        GridCoord(4, 8),  // 6
        GridCoord(3, 8),  // 7
        GridCoord(2, 8),  // 8 - Safe star (Left Arm)
        GridCoord(1, 8),  // 9
        GridCoord(0, 8),  // 10
        GridCoord(0, 7),  // 11
        GridCoord(0, 6),  // 12
        GridCoord(1, 6),  // 13 - Green start (Left Arm, Top Lane, moving RIGHT)
        GridCoord(2, 6),  // 14
        GridCoord(3, 6),  // 15
        GridCoord(4, 6),  // 16
        GridCoord(5, 6),  // 17
        GridCoord(6, 5),  // 18
        GridCoord(6, 4),  // 19
        GridCoord(6, 3),  // 20
        GridCoord(6, 2),  // 21 - Safe star (Top Arm)
        GridCoord(6, 1),  // 22
        GridCoord(6, 0),  // 23
        GridCoord(7, 0),  // 24
        GridCoord(8, 0),  // 25
        GridCoord(8, 1),  // 26 - Yellow start (Top Arm, Right Lane, moving DOWN)
        GridCoord(8, 2),  // 27
        GridCoord(8, 3),  // 28
        GridCoord(8, 4),  // 29
        GridCoord(8, 5),  // 30
        GridCoord(9, 6),  // 31
        GridCoord(10, 6), // 32
        GridCoord(11, 6), // 33
        GridCoord(12, 6), // 34 - Safe star (Right Arm)
        GridCoord(13, 6), // 35
        GridCoord(14, 6), // 36
        GridCoord(14, 7), // 37
        GridCoord(14, 8), // 38
        GridCoord(13, 8), // 39 - Blue start (Right Arm, Bottom Lane, moving LEFT)
        GridCoord(12, 8), // 40
        GridCoord(11, 8), // 41
        GridCoord(10, 8), // 42
        GridCoord(9, 8),  // 43
        GridCoord(8, 9),  // 44
        GridCoord(8, 10), // 45
        GridCoord(8, 11), // 46
        GridCoord(8, 12), // 47 - Safe star (Bottom Arm)
        GridCoord(8, 13), // 48
        GridCoord(8, 14), // 49
        GridCoord(7, 14), // 50
        GridCoord(6, 14)  // 51
    )

    // Red home lane (Bottom arm: col 7, rows 13 down to 9 towards center)
    val RED_HOME_LANE: List<GridCoord> = listOf(
        GridCoord(7, 13),
        GridCoord(7, 12),
        GridCoord(7, 11),
        GridCoord(7, 10),
        GridCoord(7, 9)
    )

    // Green home lane (Left arm: cols 1 to 5, row 7 towards center)
    val GREEN_HOME_LANE: List<GridCoord> = listOf(
        GridCoord(1, 7),
        GridCoord(2, 7),
        GridCoord(3, 7),
        GridCoord(4, 7),
        GridCoord(5, 7)
    )

    // Yellow home lane (Top arm: col 7, rows 1 to 5 towards center)
    val YELLOW_HOME_LANE: List<GridCoord> = listOf(
        GridCoord(7, 1),
        GridCoord(7, 2),
        GridCoord(7, 3),
        GridCoord(7, 4),
        GridCoord(7, 5)
    )

    // Blue home lane (Right arm: cols 13 down to 9, row 7 towards center)
    val BLUE_HOME_LANE: List<GridCoord> = listOf(
        GridCoord(13, 7),
        GridCoord(12, 7),
        GridCoord(11, 7),
        GridCoord(10, 7),
        GridCoord(9, 7)
    )

    val BASE_CENTERS: Map<PlayerColor, FloatPoint> = mapOf(
        PlayerColor.GREEN to FloatPoint(3.0f, 3.0f),
        PlayerColor.YELLOW to FloatPoint(12.0f, 3.0f),
        PlayerColor.RED to FloatPoint(3.0f, 12.0f),
        PlayerColor.BLUE to FloatPoint(12.0f, 12.0f)
    )

    val HOME_CENTERS: Map<PlayerColor, FloatPoint> = mapOf(
        PlayerColor.RED to FloatPoint(7.5f, 8.4f),
        PlayerColor.GREEN to FloatPoint(6.6f, 7.5f),
        PlayerColor.YELLOW to FloatPoint(7.5f, 6.6f),
        PlayerColor.BLUE to FloatPoint(8.4f, 7.5f)
    )

    val BASE_SLOT_POINTS: Map<PlayerColor, List<FloatPoint>> = mapOf(
        PlayerColor.GREEN to listOf(
            FloatPoint(2.15f, 2.15f),
            FloatPoint(3.85f, 2.15f),
            FloatPoint(2.15f, 3.85f),
            FloatPoint(3.85f, 3.85f)
        ),
        PlayerColor.YELLOW to listOf(
            FloatPoint(11.15f, 2.15f),
            FloatPoint(12.85f, 2.15f),
            FloatPoint(11.15f, 3.85f),
            FloatPoint(12.85f, 3.85f)
        ),
        PlayerColor.RED to listOf(
            FloatPoint(2.15f, 11.15f),
            FloatPoint(3.85f, 11.15f),
            FloatPoint(2.15f, 12.85f),
            FloatPoint(3.85f, 12.85f)
        ),
        PlayerColor.BLUE to listOf(
            FloatPoint(11.15f, 11.15f),
            FloatPoint(12.85f, 11.15f),
            FloatPoint(11.15f, 12.85f),
            FloatPoint(12.85f, 12.85f)
        )
    )

    fun getTrackIndex(color: PlayerColor, step: Int): Int? {
        return if (step < 51) {
            (color.startTrackIndex + step) % TRACK_LENGTH
        } else {
            null
        }
    }

    fun isSafeSquare(color: PlayerColor, step: Int): Boolean {
        if (step in 51..56) return true
        val trackIndex = (color.startTrackIndex + step) % TRACK_LENGTH
        return SAFE_TRACK_INDICES.contains(trackIndex)
    }

    fun getBaseSlotCoordinate(color: PlayerColor, tokenId: Int): FloatPoint {
        val list = BASE_SLOT_POINTS[color] ?: emptyList()
        val index = (tokenId % list.size.coerceAtLeast(1)).coerceIn(0, list.lastIndex.coerceAtLeast(0))
        return list.getOrNull(index) ?: FloatPoint(0f, 0f)
    }

    fun getTokenCoordinate(token: Token): FloatPoint {
        return when (val pos = token.position) {
            is TokenPosition.InBase -> getBaseSlotCoordinate(token.color, token.id)
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
                    val coord = lane.getOrNull(homeIndex) ?: lane.last()
                    FloatPoint(coord.col + 0.5f, coord.row + 0.5f)
                } else {
                    HOME_CENTERS[token.color] ?: FloatPoint(7.5f, 7.5f)
                }
            }
            is TokenPosition.Finished -> {
                val center = HOME_CENTERS[token.color] ?: FloatPoint(7.5f, 7.5f)
                val angle = token.id * 90.0 * (Math.PI / 180.0)
                val offset = 0.22f
                FloatPoint(
                    (center.x + Math.cos(angle) * offset).toFloat(),
                    (center.y + Math.sin(angle) * offset).toFloat()
                )
            }
        }
    }
}
