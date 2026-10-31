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

enum class PlayerAvatar(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String
) {
    KING("king", "👑", "King", "Royal Monarch"),
    TIGER("tiger", "🐯", "Tiger", "Fierce Striker"),
    NINJA("ninja", "🥷", "Ninja", "Silent Master"),
    LION("lion", "🦁", "Lion", "Majestic Leader"),
    ROBOT("robot", "🤖", "Robot", "Smart Cyborg"),
    WIZARD("wizard", "🧙‍♂️", "Wizard", "Arcane Mage"),
    FALCON("falcon", "🦅", "Falcon", "Swift Scout"),
    STAR("star", "⭐", "Superstar", "Lucky Legend"),
    DRAGON("dragon", "🐲", "Dragon", "Mythic Power"),
    QUEEN("queen", "👸", "Queen", "Imperial Grace");

    companion object {
        fun fromId(id: String?): PlayerAvatar {
            return values().find { it.id.equals(id, ignoreCase = true) } ?: KING
        }

        fun defaultFor(color: PlayerColor): PlayerAvatar {
            return when (color) {
                PlayerColor.RED -> TIGER
                PlayerColor.GREEN -> NINJA
                PlayerColor.YELLOW -> LION
                PlayerColor.BLUE -> ROBOT
            }
        }
    }
}

enum class BoardTheme(
    val id: String,
    val title: String,
    val description: String,
    val boardBackground: Color,
    val trackCellFill: Color,
    val trackCellBorder: Color,
    val starColor: Color,
    val frameBorderBrushColors: List<Color>,
    val outerBackground: Color
) {
    CLASSIC(
        id = "classic",
        title = "Classic",
        description = "Traditional porcelain white & gold",
        boardBackground = Color(0xFFFAFAFA),
        trackCellFill = Color(0xFFFFFFFF),
        trackCellBorder = Color(0xFFCBD5E1),
        starColor = Color(0xFFF59E0B),
        frameBorderBrushColors = listOf(
            Color(0xFFFFFBEB), Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFF78350F), Color(0xFFFDE047)
        ),
        outerBackground = Color(0xFF150A05)
    ),
    NIGHT(
        id = "night",
        title = "Night",
        description = "Deep cosmic obsidian & glowing neon stars",
        boardBackground = Color(0xFF0F172A),
        trackCellFill = Color(0xFF1E293B),
        trackCellBorder = Color(0xFF334155),
        starColor = Color(0xFF38BDF8),
        frameBorderBrushColors = listOf(
            Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF6366F1), Color(0xFF1E1B4B), Color(0xFF38BDF8)
        ),
        outerBackground = Color(0xFF070B14)
    ),
    ROYAL_GOLD(
        id = "royal_gold",
        title = "Royal Gold",
        description = "Regal amber marble & imperial luster",
        boardBackground = Color(0xFFFFFBEB),
        trackCellFill = Color(0xFFFEF3C7),
        trackCellBorder = Color(0xFFD97706),
        starColor = Color(0xFFDC2626),
        frameBorderBrushColors = listOf(
            Color(0xFFFFFBEB), Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFB45309), Color(0xFFFFFBEB)
        ),
        outerBackground = Color(0xFF261208)
    );

    companion object {
        fun fromId(id: String?): BoardTheme {
            return values().find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: CLASSIC
        }
    }
}

data class EmojiReactionEvent(
    val id: Long = System.currentTimeMillis(),
    val emoji: String,
    val senderName: String,
    val senderColor: PlayerColor,
    val timestampMs: Long = System.currentTimeMillis()
)

data class Player(
    val color: PlayerColor,
    val name: String,
    val isBot: Boolean = false,
    val avatar: PlayerAvatar = PlayerAvatar.defaultFor(color),
    val tokens: List<Token> = List(4) { Token(id = it, color = color) },
    val rank: Int? = null, // 1st, 2nd, 3rd, 4th
    val captures: Int = 0,
    val losses: Int = 0,
    val consecutiveSixes: Int = 0,
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

enum class GameSkin(
    val title: String,
    val description: String,
    val primaryTone: Color,
    val accentTone: Color,
    val rimTone: Color
) {
    CLASSIC(
        title = "Classic",
        description = "Traditional glossy enamel",
        primaryTone = Color(0xFFF59E0B),
        accentTone = Color(0xFFFDE68A),
        rimTone = Color(0xFF1E3A8A)
    ),
    RUBY(
        title = "Ruby",
        description = "Imperial crimson gemstone",
        primaryTone = Color(0xFFE11D48),
        accentTone = Color(0xFFFDA4AF),
        rimTone = Color(0xFF881337)
    ),
    EMERALD(
        title = "Emerald",
        description = "Royal jade & green lustre",
        primaryTone = Color(0xFF059669),
        accentTone = Color(0xFF6EE7B7),
        rimTone = Color(0xFF064E3B)
    ),
    SAPPHIRE(
        title = "Sapphire",
        description = "Deep celestial cosmic blue",
        primaryTone = Color(0xFF2563EB),
        accentTone = Color(0xFF93C5FD),
        rimTone = Color(0xFF1E3A8A)
    ),
    GOLD(
        title = "Gold",
        description = "24K burnished luxury gold",
        primaryTone = Color(0xFFEAB308),
        accentTone = Color(0xFFFEF08A),
        rimTone = Color(0xFF78350F)
    ),
    SILVER(
        title = "Silver",
        description = "Mirror-finish platinum chrome",
        primaryTone = Color(0xFF94A3B8),
        accentTone = Color(0xFFF8FAFC),
        rimTone = Color(0xFF334155)
    )
}

enum class AnimationSpeed(
    val title: String,
    val stepDelayMs: Long
) {
    NORMAL("Normal", 100L),
    FAST("Fast", 60L)
}

enum class AiDifficulty(
    val title: String,
    val description: String
) {
    EASY("Easy", "Casual, relaxed moves"),
    MEDIUM("Medium", "Balanced tactical play"),
    HARD("Hard", "Aggressive & calculated moves")
}

data class MatchPlayerSummary(
    val name: String,
    val color: PlayerColor,
    val isBot: Boolean,
    val captures: Int,
    val tokensHome: Int,
    val avatar: PlayerAvatar = PlayerAvatar.defaultFor(color)
)

data class MatchHistoryRecord(
    val id: String,
    val timestampMs: Long,
    val formattedDate: String,
    val gameMode: GameMode,
    val aiDifficulty: AiDifficulty?,
    val winnerName: String,
    val winnerColor: PlayerColor,
    val winnerAvatar: PlayerAvatar = PlayerAvatar.defaultFor(winnerColor),
    val durationSeconds: Long = 0L,
    val players: List<MatchPlayerSummary>
)

data class GameState(
    val matchId: String = java.util.UUID.randomUUID().toString(),
    val players: List<Player> = emptyList(),
    val currentTurnIndex: Int = 0,
    val diceState: DiceState = DiceState(),
    val phase: TurnPhase = TurnPhase.WAITING_FOR_ROLL,
    val gameMode: GameMode = GameMode.LOCAL_MULTIPLAYER,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val movableTokenIds: Set<Int> = emptySet(),
    val statusMessage: String = "Roll the dice to start!",
    val winnerPodium: List<Player> = emptyList(),
    val isGameOver: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val autoRollBots: Boolean = true,
    val captureEvent: CaptureAnimationEvent? = null,
    val currentSkin: GameSkin = GameSkin.CLASSIC,
    val boardTheme: BoardTheme = BoardTheme.CLASSIC,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val matchStartTimeMs: Long = System.currentTimeMillis(),
    val matchDurationSeconds: Long = 0L,
    val activeReaction: EmojiReactionEvent? = null,
    val extraTurnReason: String? = null
) {
    val currentPlayer: Player?
        get() = players.getOrNull(currentTurnIndex)
}
