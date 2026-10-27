package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.logic.FloatPoint

enum class PlayerColor(
    val title: String,
    val primaryColor: Color,
    val darkColor: Color,
    val lightColor: Color,
    val startTrackIndex: Int, // Index on 0..51 outer track
    val homeEntryTrackIndex: Int // Index on 0..51 outer track just before entering home lane
) {
    RED(
        title = "Red",
        primaryColor = Color(0xFFDC2626),
        darkColor = Color(0xFF991B1B),
        lightColor = Color(0xFFFCA5A5),
        startTrackIndex = 0,
        homeEntryTrackIndex = 50
    ),
    GREEN(
        title = "Green",
        primaryColor = Color(0xFF16A34A),
        darkColor = Color(0xFF14532D),
        lightColor = Color(0xFF86EFAC),
        startTrackIndex = 13,
        homeEntryTrackIndex = 11
    ),
    YELLOW(
        title = "Yellow",
        primaryColor = Color(0xFFCA8A04),
        darkColor = Color(0xFF713F12),
        lightColor = Color(0xFFFEF08A),
        startTrackIndex = 26,
        homeEntryTrackIndex = 24
    ),
    BLUE(
        title = "Blue",
        primaryColor = Color(0xFF2563EB),
        darkColor = Color(0xFF1E3A8A),
        lightColor = Color(0xFF93C5FD),
        startTrackIndex = 39,
        homeEntryTrackIndex = 37
    )
}

sealed class TokenPosition {
    object InBase : TokenPosition()
    data class OnBoard(val step: Int) : TokenPosition() // 0..55 (0..50 outer track, 51..55 home lane)
    object Finished : TokenPosition() // Reached home (step 56)
}

data class Token(
    val id: Int, // 0..3
    val color: PlayerColor,
    val position: TokenPosition = TokenPosition.InBase
) {
    val isInBase: Boolean get() = position is TokenPosition.InBase
    val isFinished: Boolean get() = position is TokenPosition.Finished
    val step: Int? get() = (position as? TokenPosition.OnBoard)?.step
}

data class Player(
    val color: PlayerColor,
    val name: String,
    val isBot: Boolean = false,
    val tokens: List<Token> = List(4) { Token(id = it, color = color) },
    val rank: Int? = null, // 1st, 2nd, 3rd, 4th
    val captures: Int = 0,
    val losses: Int = 0,
    val liveWinChancePercent: Int = 0,
    val estimatedWinChanceText: String = ""
) {
    val isFinished: Boolean get() = tokens.all { it.isFinished }
    val finishedTokensCount: Int get() = tokens.count { it.isFinished }
    val inBaseTokensCount: Int get() = tokens.count { it.isInBase }
    val activeTokensCount: Int get() = tokens.count { it.position is TokenPosition.OnBoard }
    val formattedWinChance: String
        get() = if (estimatedWinChanceText.isNotEmpty()) estimatedWinChanceText else "$liveWinChancePercent%"
}

enum class GameMode {
    VS_COMPUTER,
    LOCAL_MULTIPLAYER
}

enum class TurnPhase {
    WAITING_FOR_ROLL,
    SELECTING_TOKEN,
    ANIMATING_MOVE,
    TURN_TRANSITION
}

data class DiceState(
    val value: Int = 1,
    val isRolling: Boolean = false,
    val consecutiveSixes: Int = 0
)

data class CaptureAnimationEvent(
    val eventId: Long = System.currentTimeMillis(),
    val capturingPlayerColor: PlayerColor,
    val capturingPlayerName: String,
    val capturingTokenId: Int,
    val capturedPlayerColor: PlayerColor,
    val capturedPlayerName: String,
    val capturedTokenId: Int,
    val captureBoardCoord: FloatPoint, // (col, row) on board
    val capturedBaseCoord: FloatPoint, // (col, row) in yard base
    val capturingEmoji: String = "😎",
    val capturedEmoji: String = "😭"
)

data class GameState(
    val players: List<Player> = emptyList(),
    val currentTurnIndex: Int = 0,
    val diceState: DiceState = DiceState(),
    val phase: TurnPhase = TurnPhase.WAITING_FOR_ROLL,
    val gameMode: GameMode = GameMode.LOCAL_MULTIPLAYER,
    val movableTokenIds: Set<Int> = emptySet(),
    val statusMessage: String = "Roll the dice to start!",
    val winnerPodium: List<Player> = emptyList(),
    val isGameOver: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val autoRollBots: Boolean = true,
    val captureEvent: CaptureAnimationEvent? = null
) {
    val currentPlayer: Player?
        get() = players.getOrNull(currentTurnIndex)
}
